package zoned.framework.api

import io.javalin.http.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import zoned.framework.auth.Auth
import zoned.framework.auth.JWTAuthentication
import zoned.framework.auth.Role
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val PORT = 11091
private const val BASE = "http://localhost:$PORT"

class PublicApi : Api {
    override val basePath = "/test"
    override val baseRoles = emptyList<Role>()

    @GET("/hello", Role.ANON)
    fun hello(ctx: Context): Response = response { "hi" }

    @GET("/secret", Role.USER)
    fun secret(ctx: Context): Response = response { "secret" }
}

class ZonedServerTest {

    private val app: Zoned = Zoned.create {
        auth(Auth(JWTAuthentication("test-secret")))
        bindings { }
        apis(PublicApi())
        accessLog(false)
    }

    private val client = OkHttpClient()

    @BeforeTest fun start() { app.start(PORT) }
    @AfterTest fun stop() { app.stop() }

    @Test
    fun `anon-allowed route returns 200`() {
        client.newCall(Request.Builder().url("$BASE/test/hello").get().build()).execute().use { resp ->
            assertEquals(200, resp.code)
            assertEquals("hi", resp.body.string())
        }
    }

    @Test
    fun `protected route returns 401 for anon`() {
        client.newCall(Request.Builder().url("$BASE/test/secret").get().build()).execute().use { resp ->
            assertEquals(401, resp.code)
        }
    }
}
