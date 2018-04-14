import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
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
    val connection = socket.accept()
    connection.receiveBufferSize = 4096
    val input = BufferedReader(InputStreamReader(connection.getInputStream()))
    val out = connection.getOutputStream()
    val header = ArrayList<String>(10)

    println("Reading input...")
    var next = input.readLine()
    var isWebSocket = false
    while (!next.isNullOrEmpty()) {
        if (next == "Upgrade: websocket") {
            isWebSocket = true
        }
        header.add(next)
        next = input.readLine()
        println("Read from input $next")
    }

    if (isWebSocket) {
        println("Handling web socket")
        respondWS(header, out)
    } else {
        respondHTTP(header, out)
    }
    out.flush()
    var totalBits = ""
    while (true) {
        val nin = input.read()
        if (nin == -1) {
            break
        }
        println("Input int: $nin, bits: ${nin.toString(2)}")
        nin.toString(2).forEachIndexed { index, c ->
            print("$index $c, ")
        }
        println()
        // bitwise & with 0x7f (1111111) removes all bits except the last ones
        println("Shifted ${nin.and(0x7f)}, bits: ${nin.and(0x7f).toString(2)}")
        totalBits += nin.toString(2)
        println("All bits so far: $totalBits")
    }
    out.close()
    // todo when moving the socket into a thread, do NOT close immediately after response is sent
    connection.close()
}

private fun respondHTTP(header: ArrayList<String>, out: OutputStream) {
    val file = findFile(header[0].split(' ')[1])
    if (!file.exists()) {
        HeaderWriter.status(out, HTTPStatus.NOT_FOUND_404)
        HeaderWriter.finalize(out)
    } else {
        HeaderWriter.all(out, HTTPStatus.OK_200, getContentType(file.extension), file)

        Files.copy(file.toPath(), out)
    }
}

private fun respondWS(header: ArrayList<String>, out: OutputStream) {
    HeaderWriter.status(out, HTTPStatus.SWITCHING_PROTOCOLS)
    val wsSecurityKeyValue = header.find { x -> x.contains("Sec-WebSocket-Key:") }!!.substring(19)
    println("Web socket key $wsSecurityKeyValue")
    HeaderWriter.wsResponse(out)
    HeaderWriter.wsAccept(out, wsSecurityKeyValue)
}