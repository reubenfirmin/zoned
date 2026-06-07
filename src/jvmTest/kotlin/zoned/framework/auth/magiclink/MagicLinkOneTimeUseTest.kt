package zoned.framework.auth.magiclink

import okhttp3.OkHttpClient
import okhttp3.Request
import zoned.framework.api.Method
import zoned.framework.api.BaseRoute
import zoned.framework.api.Route
import zoned.framework.api.Zoned
import zoned.framework.auth.Auth
import zoned.framework.auth.JWTAuthentication
import zoned.framework.auth.Person
import zoned.framework.auth.Role
import zoned.framework.email.PostmarkEmailer
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val PORT = 11092
private const val BASE = "http://localhost:$PORT"

private data class TestPerson(
    override val id: UUID,
    override val email: String,
    override val role: Role,
    override val accountId: UUID
) : Person

/**
 * In-memory provider. The auth key hash starts at [INITIAL_HASH]; calling
 * [updateAuthKey] rotates it, which is what makes a magic link one-time-use.
 */
private class FakeProvider(private val user: TestPerson) : MagicLinkProvider {
    var currentHash: String = INITIAL_HASH
        private set

    override fun findUserByEmail(email: String): Person = user
    override fun findUserById(id: UUID): Person = user
    override fun updateAuthKey(id: UUID, key: String) { currentHash = "rotated-${key.hashCode()}" }
    override fun getAuthKeyHash(id: UUID): String = currentHash
    override fun fromEmail(): String = "noreply@example.com"
    override fun appName(): String = "TestApp"
    override fun loginSuccessRoute(user: Person): Route = BaseRoute("/done", Method.GET)
    override fun newUserRegistrationRoute(email: String, token: String): String? = null

    companion object {
        const val INITIAL_HASH = "hash-v1"
    }
}

class MagicLinkOneTimeUseTest {

    private val userId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val user = TestPerson(userId, "user@example.com", Role.USER, UUID.randomUUID())
    private val jwt = JWTAuthentication("test-secret")
    private val provider = FakeProvider(user)

    private val app: Zoned = Zoned.create {
        auth(Auth(jwt))
        bindings { }
        apis(MagicLinkAuth(provider, jwt, PostmarkEmailer("fake-secret"), BASE))
        accessLog(false)
    }

    // Don't auto-follow the success redirect; we want to inspect the verify response itself.
    private val client = OkHttpClient.Builder().followRedirects(false).build()

    @BeforeTest fun start() { app.start(PORT) }
    @AfterTest fun stop() { app.stop() }

    private fun freshLinkToken(keyHash: String): String = jwt.createExpiringToken(
        mapOf(
            MAGIC_LINK_EMAIL to user.email,
            MAGIC_LINK_USER_ID to userId.toString(),
            MAGIC_LINK_KEY_HASH to keyHash
        )
    )

    private fun verify(token: String) =
        client.newCall(Request.Builder().url("$BASE/magiclink/verify?token=$token").get().build()).execute()

    @Test
    fun `first click logs in and second click reports link already used`() {
        val token = freshLinkToken(FakeProvider.INITIAL_HASH)

        // First click: key hash matches -> success redirect, and the key rotates.
        verify(token).use { resp ->
            assertEquals(302, resp.code, "first click should redirect to the success route")
            assertEquals("/done", resp.header("Location"))
        }
        assertTrue(provider.currentHash != FakeProvider.INITIAL_HASH, "auth key should have rotated")

        // Second click with the same token: key hash no longer matches -> already used.
        verify(token).use { resp ->
            assertEquals(200, resp.code)
            assertTrue(
                resp.body.string().contains("Already Used"),
                "second click should render the 'link already used' page"
            )
        }
    }

    @Test
    fun `a link minted against a stale key hash is rejected as already used`() {
        // Simulates a second outstanding link minted against a key hash that no longer matches.
        val staleToken = freshLinkToken("some-old-hash")

        verify(staleToken).use { resp ->
            assertEquals(200, resp.code)
            assertTrue(resp.body.string().contains("Already Used"))
        }
    }
}
