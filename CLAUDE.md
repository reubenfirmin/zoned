# Zoned Framework - Claude Code Guidelines

## Overview

Zoned is a Kotlin Multiplatform framework for building server-rendered web applications with client-side enhancements. It follows a "server renders everything, client enhances" architecture.

## Golden Rule

**The frontend is as dumb as possible. Render as much as possible into the markup on the server.**

The server renders complete HTML with data attributes. The client-side JavaScript only adds interactivity (event handlers, animations) - it never fetches data or makes decisions about what to display.

## Build Workflow

- **Watch process**: `./watch.sh` runs continuously and rebuilds on changes
- **Check errors**: Look at `.build_errors` file (appears when build fails, disappears on success)
- **Publish zoned**: After editing zoned files, run `./gradlew publishToMavenLocal` so dependent projects pick up changes

## FORBIDDEN Patterns

These patterns are strictly prohibited in the codebase:

```kotlin
// NEVER use these:
asDynamic()           // Type-unsafe, breaks tooling
unsafeCast<T>()       // Use proper type conversions
js("{}")              // No inline JavaScript
js("...")             // No inline JavaScript
eval()                // Security risk
innerHTML = "..."     // Use DSL to build content
document.getElementById()  // Use Ref<T> pattern instead
element.style.x = "..." // Use css {} DSL instead (except for dynamic values)
```

## Enhancement System Architecture

### What is an Enhancement?

An enhancement adds client-side interactivity to server-rendered content. Examples: tooltips, context menus, sortable lists, WYSIWYG editors.

### Three Parts of an Enhancement

1. **Common Definition** (`src/commonMain/.../FooEnhancement.kt`)
   - Enhancement object with name and config serializer
   - Config data class with serializable properties
   - Marked with `@ClientEnhancement` annotation

2. **Server-Side DSL** (auto-generated in `build/generated/kotlin/.../EnhancementDSL.kt`)
   - `fun FlowContent.foo(configure: FooConfig.() -> Unit, content: FlowContent.() -> Unit)`
   - Creates wrapper div with `data-enhancement` and `data-enhancement-config` attributes

3. **Client-Side Implementation** (`src/jsMain/.../FooEnhancementImpl.kt`)
   - Function that receives element and config, adds event handlers
   - Marked with `@EnhancementImpl(FooEnhancement::class)` annotation

### Enhancement Wrapper Pattern

**Enhancements WRAP their content.** The DSL creates a container div, and content goes inside:

```kotlin
// Server-side usage:
tooltip({ text = "Helpful info" }) {
    span { +"Hover me" }
}

// Renders as:
// <div data-enhancement="tooltip" data-enhancement-config='{"text":"Helpful info"}'>
//     <span>Hover me</span>
// </div>
```

### Client-Side Implementation Patterns

#### Pattern 1: Direct Element Enhancement (for simple enhancements)
```kotlin
@EnhancementImpl(SortableEnhancement::class)
fun makeSortableEnhancement(element: Element, config: SortableConfig) {
    val htmlElement = element as HTMLElement
    // Add event handlers directly to element
}
```

#### Pattern 2: TagConsumer Wrapper (for enhancements that rebuild content)
```kotlin
@EnhancementImpl(TooltipEnhancement::class)
fun TagConsumer<HTMLElement>.initTooltipEnhancement(config: TooltipConfig, children: List<Node>) {
    val triggerRef = Ref<HTMLElement>()

    div {
        ref(triggerRef)
        css { display = Display.inlineBlock }

        // Re-insert server-rendered children
        insertChildren(children)

        onMouseEnter { TooltipManager.show(triggerRef.element, config.text) }
        onMouseLeave { TooltipManager.hide() }
    }
}
```

### Event Delegation for Nested Content

When an enhancement wraps content with many interactive elements (like a table with rows), use event delegation:

```kotlin
// Server renders data attributes on each row:
contextMenu({ menuUrl = "...", dataAttributes = listOf("propertyId") }) {
    table {
        tbody {
            items.forEach { item ->
                tr {
                    attributes["data-propertyId"] = item.id  // Data on each row
                    // row content
                }
            }
        }
    }
}

// Client-side walks up from click target to find data attributes:
container.onContextMenu { event ->
    val targetElement = findElementWithDataAttributes(event.target, config.dataAttributes)
    // Read attributes from targetElement, not container
}
```

### Marking Implementations with Annotations

Use `@EnhancementImpl` annotation instead of relying on function naming conventions:

```kotlin
@EnhancementImpl(TooltipEnhancement::class)
fun TagConsumer<HTMLElement>.initTooltipEnhancement(config: TooltipConfig, children: List<Node>) {
    // ...
}
```

The Gradle plugin scans for these annotations to generate the registry.

## Typed CSS DSL

Use the `css {}` DSL from kotlinx.css for inline styles:

```kotlin
div {
    css {
        display = Display.inlineBlock
        backgroundColor = Color("rgba(17, 24, 39, 0.95)")
        padding = Padding(4.px, 8.px)
        borderRadius = 4.px
        zIndex = 10000

        // Transitions and shadows
        transition += Transition("opacity", 150.ms, Timing.easeInOut)
        boxShadow += BoxShadow(Color("rgba(0,0,0,0.3)"), 0.px, 2.px, 8.px)
    }
}
```

Required imports:
```kotlin
import kotlinx.css.*
import kotlinx.css.properties.BoxShadow
import kotlinx.css.properties.Timing
import kotlinx.css.properties.Transition
import kotlinx.css.properties.ms
```

## Ref Pattern for Element References

Instead of `document.getElementById()`, use the typed `Ref<T>` pattern:

```kotlin
val buttonRef = Ref<HTMLButtonElement>()

div {
    button {
        ref(buttonRef)  // Captures reference during the DSL pass
        +"Click me"
        onMount {
            // Access the element ONLY after it is in the DOM (see "Rendering Cycle" below)
            buttonRef.element.disabled = true
        }
    }
}
```

Do **not** read `ref.element` synchronously after the DSL block — at that point the
element may not be in the live document yet. Use it inside `onMount { }`.

## Rendering Cycle: the DSL is pre-DOM, `onMount` is post-DOM

When you build client UI with the DSL (`HTMLElement.appendTo().div { }`, `addToBody { }`,
any `ElementTrackingConsumer`), the DSL pass runs **before** the element is attached to the
live document. The element is only appended (and measurable) in `finalize()`, which then
fires the queued `onMount` callbacks. Two hard rules follow:

1. **Bind events and do DOM work in `onMount`, on the real element** — not via the
   `on*Function` DSL setters (which are pre-DOM and don't bind a live listener), and not by
   reading `ref.element` synchronously after the block. Capture a `Ref` in the DSL, then in
   `onMount` use `ref.element.addEventListener(...)`, `getBoundingClientRect()`, positioning,
   focus, measuring, etc. (`onMount` runs after `appendChild`, so the element is live.)

2. **Build children on the `FlowContent`/`Tag` receiver, never as `TagConsumer` extensions.**
   A `TagConsumer<HTMLElement>`-level `div { }` runs `visitAndFinalize`, which appends the
   element to the consumer's **root** (e.g. `<body>`) as a *sibling* and pops the tracker —
   breaking `ref`/`onMount` nesting and scattering content. To add children, call builders on
   the enclosing element's block receiver:

   ```kotlin
   // WRONG — appends siblings to <body>, pops the tracker (onMount/ref break)
   fun TagConsumer<HTMLElement>.renderItems(items: List<X>) { items.forEach { div { ... } } }
   addToBody { div { renderItems(items) } }

   // CORRECT — items are children of the div
   fun FlowContent.renderItems(items: List<X>) { items.forEach { div { ... } } }
   addToBody { div { renderItems(items) } }
   ```

A floating element (menu/tooltip/modal) is therefore: one container built with `addToBody { div { ref(r); /* children on FlowContent */; onMount { position + addEventListener(...) } } }`, removed on dismiss.

## DOM Helpers

### Adding Elements to Body
```kotlin
addToBody {
    div {
        // Modal, tooltip, or other body-level content
    }
}
```

### Re-inserting Server-Rendered Children
```kotlin
fun TagConsumer<HTMLElement>.initFooEnhancement(config: FooConfig, children: List<Node>) {
    div {
        // ... wrapper content
        insertChildren(children)  // Re-insert original content
    }
}
```

### Event Handlers

The kotlinx-html DSL is **pre-DOM** (see "Rendering Cycle" below): the `on*Function`
DSL setters (`onClick { }`, `onContextMenu { }`, …) do **not** reliably produce a live
listener on the element that ends up in the document. Bind listeners in `onMount { }` on
the real element instead:

```kotlin
val rootRef = Ref<HTMLElement>()
div {
    ref(rootRef)
    onMount {
        rootRef.element.addEventListener(EventType("click")) { e: MouseEvent -> /* handle */ }
    }
}
```

## File Organization

```
src/
├── commonMain/kotlin/zoned/framework/
│   └── ui/enhancements/
│       ├── Enhancement.kt           # Base interfaces
│       ├── EnhancementImpl.kt       # @EnhancementImpl annotation
│       ├── TooltipEnhancement.kt    # Enhancement + Config definition
│       └── ...
├── jvmMain/kotlin/zoned/framework/
│   └── ...                          # Server-side components
└── jsMain/kotlin/zoned/framework/
    ├── dom/
    │   ├── Ref.kt                   # Ref<T> pattern
    │   ├── DomHelpers.kt            # insertChildren, etc.
    │   └── Events.kt                # Event handler extensions
    ├── interop/
    │   ├── Css.kt                   # css {} DSL
    │   └── Connect.kt               # addToBody, etc.
    └── ui/enhancements/
        ├── TooltipEnhancementImpl.kt
        └── ...

build/generated/
├── kotlin/zoned/enhancements/
│   └── ZonedEnhancementDSL.kt       # Generated server DSL
└── kotlin-js/zoned/enhancements/
    └── ZonedEnhancementRegistry.kt  # Generated client registry
```

## Gradle Plugin

The `zoned-gradle-plugin` provides:

1. **Enhancement Scanner** - Finds `@ClientEnhancement` objects in commonMain
2. **Impl Scanner** - Finds `@EnhancementImpl` functions in jsMain
3. **Code Generator** - Generates DSL functions and client registry

Run `./gradlew generate-enhancements` to regenerate (usually automatic).

## Testing Changes

1. Make changes to zoned
2. Run `./gradlew publishToMavenLocal`
3. Check dependent project's `.build_errors` file
4. Test in browser

## Common Mistakes to Avoid

1. **Using enhancement as attribute instead of wrapper**
   ```kotlin
   // WRONG - trying to apply to existing element
   div {
       tooltip { text = "..." }  // This creates a div INSIDE, not on the div
       +"Content"
   }

   // CORRECT - wrapper pattern
   tooltip({ text = "..." }) {
       div { +"Content" }
   }
   ```

2. **Inline styles instead of css DSL**
   ```kotlin
   // WRONG
   element.style.backgroundColor = "red"

   // CORRECT
   css { backgroundColor = Color.red }
   ```

3. **getElementById instead of Ref**
   ```kotlin
   // WRONG
   val el = document.getElementById("my-id")

   // CORRECT
   val myRef = Ref<HTMLElement>()
   div { ref(myRef); id = "my-id" }
   // then use myRef.element
   ```

4. **Missing annotation on impl**
   ```kotlin
   // WRONG - scanner might miss this
   fun initFooEnhancement(...)

   // CORRECT
   @EnhancementImpl(FooEnhancement::class)
   fun initFooEnhancement(...)
   ```
