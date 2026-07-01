package zoned.framework.api

import io.javalin.Javalin
import org.slf4j.LoggerFactory
import java.io.File

/**
 * zoned-owned web server. Hides the underlying Javalin instance: callers configure routes,
 * auth, and middleware via [create]'s builder, then [start] it.
 */
class Zoned private constructor(private val javalin: Javalin) {

    fun start(port: Int): Zoned {
        javalin.start(port)
        installWatchSupervisorGuard(port)
        return this
    }

    fun stop(): Zoned {
        javalin.stop()
        return this
    }

    /**
     * Dev hot-reload safety net. DO NOT REMOVE without understanding why it exists.
     *
     * The generated `watch.sh` starts this server via `gradle run`, which forks the app JVM
     * under the long-lived Gradle daemon. That fork is NOT a child of the terminal or of
     * watch.sh, so when the terminal is closed the app is orphaned and keeps holding the port
     * (this actually happened: a server survived 16h after its terminal was gone). Bash-level
     * fixes (traps, PDEATHSIG, cgroups) are either un-trappable (SIGKILL/OOM) or Linux-only, so
     * they can't be the guarantee for a framework that also ships to macOS.
     *
     * The portable guarantee lives HERE, in the one process we fully own. watch.sh stamps a
     * heartbeat file every poll tick; while it's alive the file stays fresh. If watch.sh dies by
     * ANY means the stamps stop, this guard notices the file going stale, and halts the JVM,
     * which lets the OS reclaim the port. No process parentage and no shared env var required
     * (env doesn't reliably survive the daemon fork), so path agreement with watch.sh is derived
     * purely from (user, port) — see [watchHeartbeatFile]; keep the two ends in lockstep.
     *
     * Fail-safe by construction: in production nobody stamps the file, so it's absent or long
     * stale, the freshness gate below never arms, and the guard can never self-kill a real server.
     */
    private fun installWatchSupervisorGuard(port: Int) {
        val heartbeat = watchHeartbeatFile(port)
        // Generous default: must tolerate GC pauses and whole-box swap thrash without a false
        // kill. watch.sh stamps ~1x/sec; 20s of silence means it's genuinely gone, not just slow.
        val staleMs = System.getenv("ZONED_WATCH_STALE_MS")?.toLongOrNull() ?: 20_000L
        val checkMs = System.getenv("ZONED_WATCH_CHECK_MS")?.toLongOrNull() ?: 3_000L

        // Arm ONLY if a supervisor is stamping the file right now. This is the production guard:
        // no fresh heartbeat -> not launched under watch.sh -> never arm.
        val freshAtStart = heartbeat.exists() &&
            System.currentTimeMillis() - heartbeat.lastModified() <= staleMs
        if (!freshAtStart) return

        log.info("watch.sh supervisor detected ({}); arming parent-liveness guard (halt if stale > {} ms)", heartbeat, staleMs)
        Thread {
            while (true) {
                try {
                    Thread.sleep(checkMs)
                } catch (_: InterruptedException) {
                    return@Thread
                }
                val age = System.currentTimeMillis() - heartbeat.lastModified()
                if (!heartbeat.exists() || age > staleMs) {
                    // halt(), not exit(): the whole point is to die reliably when the supervisor is
                    // gone. Shutdown hooks could wedge; process death frees the port regardless.
                    log.warn("watch.sh heartbeat stale ({} ms) — supervisor gone, halting to free port {}", age, port)
                    Runtime.getRuntime().halt(0)
                }
            }
        }.apply {
            isDaemon = true
            name = "zoned-watch-guard"
            start()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(Zoned::class.java)

        /**
         * MUST compute the identical path to watch.sh's `$HEARTBEAT` (ZonedPlugin.kt). Constraints
         * that dictate this exact form:
         *  - No shared env var: the app JVM is a Gradle-daemon fork, so env set by watch.sh does
         *    not reliably reach it. Both sides instead derive the path from (user, port), which
         *    both already know independently.
         *  - Deliberately /tmp, not XDG_RUNTIME_DIR or TMPDIR: those can differ between the bash
         *    supervisor and a daemon-forked JVM (different sessions), which would silently break
         *    the rendezvous. /tmp is agreed by both with zero coordination.
         *  - Per-user subdir: avoids collisions/permission clashes on multi-user machines.
         * If you change this, change watch.sh's RUNTIME_DIR/HEARTBEAT to match or the guard
         * silently disarms.
         */
        internal fun watchHeartbeatFile(port: Int): File {
            val user = System.getProperty("user.name") ?: "nobody"
            return File("/tmp/zoned-$user", "watch-$port.heartbeat")
        }

        fun create(block: ZonedSpec.() -> Unit): Zoned {
            val spec = ZonedSpec().apply(block)
            val javalin = Javalin.create { config -> spec.applyTo(config) }
            return Zoned(javalin)
        }
    }
}
