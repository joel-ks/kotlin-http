package jks.http.server

import jks.utilities.connectionDetails
import jks.utilities.using
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.net.ServerSocket

private val CHARSET = Charsets.US_ASCII // HTTP 0.7 spec defined the message to be ASCII encoded text

// Technically should be \r\n but the spec says "A well-behaved server will not require the carriage return character."
private const val TERMINATOR = '\n'
private const val SEPARATOR = ' '

private val MESSAGES = mapOf(
    "/" to "index",
    "/greeting" to "hello world"
)

private const val ERROR_BAD_REQUEST = "BadRequest.html"
private const val ERROR_NOT_FOUND = "NotFound.html"

private object ResourceOpener {
    fun getResourceAsStream(name: String): InputStream = javaClass.classLoader.getResourceAsStream(name) ?: throw Exception("Could not find resource $name")
}

fun main(args: Array<String>) {
    val config = parseCommandLine(args)

    ServerSocket(config.port).use { serverSocket ->
        println("Listening on ${serverSocket.inetAddress}:${serverSocket.localPort}...")

        while (true) {
            using {
                try {
                    val socket = serverSocket.accept().closeWhenDone()
                    println("Connection accepted from ${socket.connectionDetails()} on port ${socket.localPort}")

                    socket.soTimeout = config.timeout

                    val inStream = socket.getInputStream().reader(CHARSET).buffered().closeWhenDone()
                    val outStream = socket.getOutputStream().buffered().closeWhenDone()
                    processHttpConnection(inStream, outStream)
                    outStream.flush()

                    println("Ending connection with ${socket.connectionDetails()} on port ${socket.localPort}")
                } catch (ioe: IOException) {
                    println("Communication error: ${ioe.message}")
                } catch (e: Exception) {
                    print("Exception occurred: ")
                    e.printStackTrace()
                }
            }
        }
    }
}

@Throws(IOException::class)
private fun processHttpConnection(inStream: Reader, outStream: OutputStream) {
    val documentPath = readHttpRequest(inStream)

    if (documentPath == null) {
        ResourceOpener.getResourceAsStream(ERROR_BAD_REQUEST).transferTo(outStream)
        return
    }

    val message = MESSAGES[documentPath]
    if (message == null) {
        ResourceOpener.getResourceAsStream(ERROR_NOT_FOUND).transferTo(outStream)
        return
    }

    val document = """
        <!DOCTYPE html>
        <html lang="en">
            <head>
                <meta charset="${CHARSET.name()}">
                <title>Success!</title>
            </head>
            <body>
                $message
            </body>
        </html> 
    """.trimIndent()

    document.byteInputStream(CHARSET).transferTo(outStream)
}

@Throws(IOException::class)
private fun readHttpRequest(inStream: Reader): String? {
    // Request format:
    // GET <address>\r\n
    // - address must not contain whitespace
    // - further content must be ignored
    //
    // Also note:
    //  - The TCP-IP connection is broken by the server when the whole document has been transferred.
    //  - The client may abort the transfer by breaking the connection before this, in which case the server shall not record any error condition.

    // Tokenize and lex
    val sb = StringBuilder()

    var hitTerminator = false
    while (true) {
        val next = inStream.read()

        if (next == -1) {
            hitTerminator = true
            break
        } else if (next == TERMINATOR.code) {
            hitTerminator = true
            break
        } else if (next == SEPARATOR.code) break
        else sb.append(next.toChar())
    }

    if (hitTerminator || sb.toString() != "GET") return null

    sb.clear()
    var hitSeparator = false
    while (true) {
        val next = inStream.read()

        if (next == -1) return null
        else if (next == TERMINATOR.code) break
        else if (next == SEPARATOR.code) hitSeparator = true // We want to consume, but ignore, any extraneous content up to the terminator
        else if (!hitSeparator) sb.append(next.toChar())
    }

    return sb.toString().trim() // remove possible '\r'
}
