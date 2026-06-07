package zoned.framework.auth.magiclink

import okhttp3.OkHttpClient
import okhttp3.Request
import zoned.framework.api.BaseRoute
import zoned.framework.api.Method
import zoned.framework.api.Route
import zoned.framework.api.Zoned
import zoned.framework.auth.Auth
import zoned.framework.auth.JWTAuthentication
import zoned.framework.auth.Person
import zoned.framework.auth.Role
import zoned.framework.email.PostmarkEmailer
import java.util.UUID
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val PORT = 11093
private const val BASE = "http://localhost:$PORT"
private const val INITIAL_HASH = "hash-v1"

/**
 * Provider whose [consumeAuthKey] is a genuine atomic compare-and-swap (what a real app
 * implements as a conditional UPDATE). The barriers force both concurrent verifies to meet
 * inside the key-management methods at the same time, so the test deterministically exercises
 * the race rather than hoping for an unlucky interleaving.
 */
private class AtomicProvider(private val user: Person) : MagicLinkProvider {
    private val hash = AtomicReference(INITIAL_HASH)
    private val barrier = CyclicBarrier(2)
    val consumeCalls = AtomicInteger(0)
    val getHashCallsDuringVerify = AtomicInteger(0)

    override fun findUserByEmail(email: String): Person = user
    override fun findUserById(id: UUID): Person = user
    override fun updateAuthKey(id: UUID, key: String) { hash.set("rotated-${key.hashCode()}") }

    // Verify must NOT read the hash separately anymore; if it does, this trips the revert guard.
    override fun getAuthKeyHash(id: UUID): String {
        getHashCallsDuringVerify.incrementAndGet()
        return hash.get()
    }

    // The atomic framework path: both requests rendezvous (generous timeout — it's a deadlock
    // safety net, not a load-bearing deadline), then contend on a single value-based
    // compare-and-swap (what a SQL `UPDATE ... WHERE hash = :expected` does). The barrier is
    // released BEFORE the lock so the threads genuinely contend; comparison is by value.
    override fun consumeAuthKey(id: UUID, expectedKeyHash: String, newKey: String): Boolean {
        consumeCalls.incrementAndGet()
        barrier.await(30, TimeUnit.SECONDS)
        synchronized(this) {
            if (hash.get() != expectedKeyHash) return false
            hash.set("rotated-${newKey.hashCode()}")
            return true
        }
    }

    override fun fromEmail(): String = "noreply@example.com"
    override fun appName(): String = "TestApp"
    override fun loginSuccessRoute(user: Person): Route = BaseRoute("/done", Method.GET)
    override fun newUserRegistrationRoute(email: String, token: String): String? = null
}

private data class CcPerson(
    override val id: UUID,
    override val email: String,
    override val role: Role,
    override val accountId: UUID
) : Person

class MagicLinkConcurrencyTest {

    private val user = CcPerson(
        UUID.fromString("00000000-0000-0000-0000-000000000009"),
        "user@example.com", Role.USER, UUID.randomUUID()
    )
    private val jwt = JWTAuthentication("test-secret")
    private val provider = AtomicProvider(user)

    private val app: Zoned = Zoned.create {
        auth(Auth(jwt))
        bindings { }
        apis(MagicLinkAuth(provider, jwt, PostmarkEmailer("fake-secret"), BASE))
        accessLog(false)
    }

    private val client = OkHttpClient.Builder().followRedirects(false).build()

    @BeforeTest fun start() { app.start(PORT) }
    @AfterTest fun stop() { app.stop() }

    @Test
    fun `two simultaneous clicks of the same link log in exactly once`() {
        val token = jwt.createExpiringToken(
            mapOf(
                MAGIC_LINK_EMAIL to user.email,
                MAGIC_LINK_USER_ID to user.id.toString(),
                MAGIC_LINK_KEY_HASH to INITIAL_HASH
            )
        )

        val pool = Executors.newFixedThreadPool(2)
        val codes = try {
            (1..2).map {
                pool.submit<Int> {
                    client.newCall(
                        Request.Builder().url("$BASE/magiclink/verify?token=$token").get().build()
                    ).execute().use { it.code }
                }
            }.map { it.get(10, TimeUnit.SECONDS) }
        } finally {
            pool.shutdownNow()
        }

        // Exactly one request wins the swap (302 -> /done); the other sees the burned link (200 page).
        assertEquals(1, codes.count { it == 302 }, "exactly one click should establish a session; got codes=$codes")
        assertEquals(1, codes.count { it == 200 }, "the other click should be rejected as already-used; got codes=$codes")

        // Revert guard: verify must route through the atomic gate, not a separate read-then-write.
        assertEquals(2, provider.consumeCalls.get(), "verify should consume the link via the atomic compare-and-swap")
        assertEquals(0, provider.getHashCallsDuringVerify.get(), "verify must not read the hash separately (non-atomic check-then-act)")
    }
}
