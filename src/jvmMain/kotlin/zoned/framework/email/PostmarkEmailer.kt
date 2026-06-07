package zoned.framework.email

import com.google.inject.Inject
import com.google.inject.name.Named
import com.postmarkapp.postmark.Postmark
import com.postmarkapp.postmark.client.data.model.message.Message
import com.postmarkapp.postmark.client.data.model.message.MessageResponse
import org.slf4j.LoggerFactory
import zoned.framework.config.Bindings
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

class PostmarkEmailer @Inject constructor(@Named(Bindings.POSTMARK_SECRET) val postmarkSecret: String) {

    private val client = Postmark.getApiClient(postmarkSecret)
    private val threadPool = Executors.newCachedThreadPool()
    private val logger = LoggerFactory.getLogger(javaClass)

    fun sendSimpleEmail(
        from: String,
        to: String,
        subject: String,
        body: String,
        textBody: String? = null
    ): Future<Boolean> {
        return threadPool.submit(Callable {
            val message = Message(from, to, subject, body)
            message.setMessageStream("outbound")
            // Always set TextBody explicitly. Without one, Postmark auto-derives a
            // plain-text alternative from the HTML which renders <a href="…">CTA</a>
            // as a visible bare URL ("Or paste this link into your browser: …").
            // Fall back to subject if no caller-supplied text — never the body.
            message.setTextBody(textBody ?: subject)
            val response: MessageResponse = client.deliverMessage(message)
            if (response.errorCode != 0) {
                logger.error("Failed sending signup email to $to with body $body - details $response")
                // will go nowhere at the moment since we're not handling futures
                throw Exception("Failed sending email: $response")
            }
            true
        })
    }
}