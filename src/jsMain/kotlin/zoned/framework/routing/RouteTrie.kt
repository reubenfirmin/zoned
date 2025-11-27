package zoned.framework.routing

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

    fun findRoute(path: String): Pair<Route, Map<String, String>>? {
        var current: TrieNode = root
        val segments = path.split("/").filter { it.isNotEmpty() }
        val params = mutableMapOf<String, String>()

        var segmentIndex = 0
        while (segmentIndex < segments.size) {
            val segment = segments[segmentIndex]

            // Check for wildcard first (it captures all remaining segments)
            val wildcardChild = current.children.find { it is WildcardNode }
            if (wildcardChild != null) {
                current = wildcardChild
                val remainingSegments = segments.subList(segmentIndex, segments.size)
                params[(wildcardChild as WildcardNode).paramName] = remainingSegments.joinToString("/")
                break  // Wildcard consumes rest of path
            }

            // Otherwise try to match single segment
            current = current.children.find { child ->
                when (child) {
                    is StringNode -> child.value == segment
                    is ParamNode -> true
                    else -> false
                }
            } ?: return null

            if (current is ParamNode) {
                params[current.paramName] = segment
            }

            segmentIndex++
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