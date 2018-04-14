import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

const val ROOT_FOLDER = "content"

fun findFile(path: String): File {
    return if (path == "/") Paths.get(ROOT_FOLDER, "index.html").toFile()
    else Paths.get(ROOT_FOLDER, path.substring(1)).toFile()
}

fun getContentType(extension: String): String {
    return when (extension) {
        "css" -> "text/css"
        "js" -> "application/javascript"
        else -> "text/html"
    }
}

fun main(args: Array<String>) {
    val socket = ServerSocket(4242)
    println("Listening for connection")
    while (true) {

        val inc = socket.accept()

        val header = ArrayList<String>(10)

        val bufferedReader = BufferedReader(InputStreamReader(inc.getInputStream()))

        var next = bufferedReader.readLine()
        var isWebSocket = false
        while (next.isNotEmpty()) {
            if (next == "Upgrade: websocket") {
                isWebSocket = true
            }
            header.add(next)
            next = bufferedReader.readLine()
        }

        val out = inc.getOutputStream()
        if (isWebSocket) {
            println("Handling web socket")
            respondWS(header,inc,out)
        } else {
            respondHTTP(header, inc, out)
        }

        out.flush()
        // todo when moving the socket into a thread, do NOT close immediately after response is sent
        out.close()
        inc.close()
    }
}

private fun respondHTTP(header: ArrayList<String>, inc: Socket, out: OutputStream) {
    val file = findFile(header[0].split(' ')[1])
    if (!file.exists()) {
        HeaderWriter.status(out, HTTPStatus.NOT_FOUND_404)
        HeaderWriter.finalize(out)
    } else {
        HeaderWriter.all(out, HTTPStatus.OK_200, getContentType(file.extension), file)

        Files.copy(file.toPath(), inc.getOutputStream())
    }
}

private fun respondWS(header: ArrayList<String>, inc: Socket, out: OutputStream){
    HeaderWriter.status(out, HTTPStatus.SWITCHING_PROTOCOLS)
    val wsSecurityKeyValue = header.find { x->x.contains("Sec-WebSocket-Key:") }!!.substring(19)
    println("Web socket key $wsSecurityKeyValue")
    HeaderWriter.wsResponse(out)
    HeaderWriter.wsAccept(out, wsSecurityKeyValue)
}