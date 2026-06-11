package zoned.framework.routing

import zoned.framework.interop.decodeURIComponent

object RouteTrie {

    private sealed class TrieNode {
        val children = mutableListOf<TrieNode>()
    }

    private class RootNode : TrieNode()
    private class StringNode(val value: String) : TrieNode()
    private class ParamNode(val paramName: String) : TrieNode()
    private class WildcardNode(val paramName: String) : TrieNode()  // Matches remaining path
    private class RouteNode(val route: Route) : TrieNode()

    private val root = RootNode()

    /** Reset all registered routes. Intended for test isolation. */
    internal fun clear() {
        root.children.clear()
    }

    fun isEmpty(): Boolean = root.children.isEmpty()

    fun addRoute(route: Route) {
        var current: TrieNode = root

        route.segments.forEach { segment ->
            current = when (segment) {
                is RouteSegment.Static -> current.children.find {
                    it is StringNode && it.value == segment.value
                } ?: StringNode(segment.value).also {
                    current.children.add(it)
                }
                is RouteSegment.Parameter -> current.children.find {
                    it is ParamNode && it.paramName == segment.name
                } ?: ParamNode(segment.name).also {
                    current.children.add(it)
                }
                is RouteSegment.Wildcard -> current.children.find {
                    it is WildcardNode && it.paramName == segment.name
                } ?: WildcardNode(segment.name).also {
                    current.children.add(it)
                }
            }
        }

        if (current.children.any { it is RouteNode }) {
            throw IllegalStateException("Route already exists")
        }
        current.children.add(RouteNode(route))
    }

    /**
     * Matches [path] using static → parameter → wildcard priority.
     * This trie does **not** backtrack: once a static segment is matched, the search continues
     * only within that subtree. A path that partially matches a static prefix but has no terminal
     * route returns null, even if a sibling wildcard would otherwise match the full path.
     */
    fun findRoute(path: String): Pair<Route, Map<String, String>>? {
        var current: TrieNode = root
        // The browser hands us percent-encoded pathnames; routes are declared (and params consumed)
        // in decoded form, so decode once at the boundary.
        val segments = path.split("/").filter { it.isNotEmpty() }.map { decodeURIComponent(it) }
        val params = mutableMapOf<String, String>()

        var segmentIndex = 0
        while (segmentIndex < segments.size) {
            val segment = segments[segmentIndex]

            // 1. Static match (most specific) wins.
            val staticChild = current.children.find { it is StringNode && it.value == segment }
            if (staticChild != null) {
                current = staticChild
                segmentIndex++
                continue
            }

            // 2. Then a single-segment parameter.
            val paramChild = current.children.find { it is ParamNode }
            if (paramChild != null) {
                params[(paramChild as ParamNode).paramName] = segment
                current = paramChild
                segmentIndex++
                continue
            }

            // 3. Finally fall back to a wildcard, which consumes the rest of the path.
            val wildcardChild = current.children.find { it is WildcardNode }
            if (wildcardChild != null) {
                val remainingSegments = segments.subList(segmentIndex, segments.size)
                params[(wildcardChild as WildcardNode).paramName] = remainingSegments.joinToString("/")
                current = wildcardChild
                break
            }

            return null
        }

        // After processing all segments, check if we've reached a RouteNode
        return current.children.find { it is RouteNode }?.let { (it as RouteNode).route to params }
    }

    fun visualize(): String {
        return buildString {
            appendLine("Routes:")
            visualizeNode(root, "", true)
        }
    }

    private fun StringBuilder.visualizeNode(node: TrieNode, prefix: String, isLast: Boolean) {
        val nodePrefix = if (isLast) "└── " else "├── "
        val childPrefix = if (isLast) "    " else "│   "

        append(prefix)
        append(nodePrefix)
        when (node) {
            is RootNode -> appendLine("Root")
            is StringNode -> appendLine(node.value)
            is ParamNode -> appendLine("{${node.paramName}}")
            is WildcardNode -> appendLine("{${node.paramName}...}")
            is RouteNode -> appendLine("Route: ${node.route.pattern}")
        }

        node.children.forEachIndexed { index, child ->
            visualizeNode(child, prefix + childPrefix, index == node.children.lastIndex)
        }
    }
}