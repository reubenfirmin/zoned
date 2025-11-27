package zoned.framework.api

import kotlin.test.Test
import kotlin.test.assertEquals

class RouteTest {

    @Test
    fun `param substitutes path parameter`() {
        val route = BaseRoute("/api/users/{userId}", Method.GET)
        val parameterized = route.param("userId", "123")
        assertEquals("/api/users/123", parameterized.url())
    }

    @Test
    fun `param adds query parameter when not in path`() {
        val route = BaseRoute("/api/users", Method.GET)
        val parameterized = route.param("page", 1)
        assertEquals("/api/users?page=1", parameterized.url())
    }

    @Test
    fun `param handles mixed path and query parameters`() {
        val route = BaseRoute("/api/users/{userId}/posts", Method.GET)
        val parameterized = route
            .param("userId", "abc-123")
            .addParam("page", 2)
            .addParam("limit", 10)
        assertEquals("/api/users/abc-123/posts?page=2&limit=10", parameterized.url())
    }

    @Test
    fun `param substitutes multiple path parameters`() {
        val route = BaseRoute("/api/{accountId}/users/{userId}", Method.DELETE)
        val parameterized = route
            .param("accountId", "acct-1")
            .addParam("userId", "user-2")
        assertEquals("/api/acct-1/users/user-2", parameterized.url())
    }

    @Test
    fun `params map substitutes path parameters`() {
        val route = BaseRoute("/team/invitation/{invitationId}", Method.DELETE)
        val parameterized = route.params(mapOf("invitationId" to "inv-uuid-123"))
        assertEquals("/team/invitation/inv-uuid-123", parameterized.url())
    }

    @Test
    fun `params map handles mixed path and query parameters`() {
        val route = BaseRoute("/api/items/{itemId}", Method.GET)
        val parameterized = route.params(mapOf(
            "itemId" to "item-1",
            "expand" to "details",
            "format" to "json"
        ))
        // itemId should be in path, others as query params
        val url = parameterized.url()
        assert(url.startsWith("/api/items/item-1?")) { "Expected path param substitution, got: $url" }
        assert(url.contains("expand=details")) { "Expected expand query param, got: $url" }
        assert(url.contains("format=json")) { "Expected format query param, got: $url" }
    }

    @Test
    fun `enum values are converted to name`() {
        val route = BaseRoute("/api/items", Method.GET)
        val parameterized = route.param("method", Method.POST)
        assertEquals("/api/items?method=POST", parameterized.url())
    }

    @Test
    fun `path with no params returns unchanged`() {
        val route = BaseRoute("/api/simple", Method.GET)
        assertEquals("/api/simple", route.url())
    }

    @Test
    fun `urlWithHost prepends base url`() {
        val route = BaseRoute("/api/users/{userId}", Method.GET)
        val parameterized = route.param("userId", "123")
        assertEquals("https://example.com/api/users/123", parameterized.urlWithHost("https://example.com"))
    }
}
