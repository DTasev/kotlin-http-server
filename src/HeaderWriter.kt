import java.io.File
import java.io.OutputStream
import java.security.MessageDigest
import java.util.*
import java.math.BigInteger


const val WEBSOCKET_MAGIC = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"

enum class HTTPStatus(val message: String) {
    OK_200("200 OK"),
    NOT_FOUND_404("404 Not found"),
    SWITCHING_PROTOCOLS("101 Switching Protocols")
}

class HeaderWriter {
    companion object {
        fun all(out: OutputStream, status: HTTPStatus, content: String, file: File) {
            HeaderWriter.status(out, status)
            HeaderWriter.content(out, content)
            HeaderWriter.contentLength(out, file)
            HeaderWriter.finalize(out)
        }

        fun status(out: OutputStream, status: HTTPStatus) {
            out.write("HTTP/1.1 ${status.message}\r\n".toByteArray())
        }

        fun content(out: OutputStream, content: String) {
            out.write("Content-Type: $content\r\n".toByteArray())
        }

        fun contentLength(out: OutputStream, file: File) {
            out.write("Content-Length: ${file.length()}\r\n".toByteArray())
        }

        fun finalize(out: OutputStream) {
            out.write("Accept: text/plain, text/html, text/*\r\n\r\n".toByteArray())
        }

        fun wsAccept(out: OutputStream, wsSecurityKeyValue: String) {
            val magicString = wsSecurityKeyValue + WEBSOCKET_MAGIC
            val crypt = MessageDigest.getInstance("SHA-1")

            crypt.update(magicString.toByteArray())

            val sha1 = BigInteger(1, crypt.digest()).toString(16)

            println("SHA-1: $sha1")
            val result = "Sec-WebSocket-Accept: ${Base64.getEncoder().encodeToString(sha1.toByteArray())}\r\n"
            println("Result of WS bullshit: $result")
            out.write(result.toByteArray())
            out.write("\r\n\r\n".toByteArray())
        }

        fun wsResponse(out: OutputStream) {
            out.write("Upgrade: websocket\r\n".toByteArray())
            out.write("Connection: Upgrade\r\n".toByteArray())
        }
    }
}