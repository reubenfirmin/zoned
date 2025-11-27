@file:JsModule("flowbite")
@file:JsNonModule
package zoned.framework.libs

// see https://flowbite.com/docs/getting-started/quickstart/#init-functions
external fun initFlowbite()

// Selective initialization functions to avoid initializing components we don't need
// Specifically, we skip initCollapses() to avoid resize event listeners that interfere with CSS responsive utilities
external fun initDropdowns()
external fun initModals()
external fun initTooltips()
external fun initAccordions()

