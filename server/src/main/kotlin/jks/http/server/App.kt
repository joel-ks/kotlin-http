package jks.http.server

import jks.utilities.connectionDetails
import jks.utilities.using
import joptsimple.OptionParser
import java.io.IOException
import java.io.Reader
import java.io.Writer
import java.net.ServerSocket

private const val ARG_PORT = "port"
private const val TIMEOUT_MS = 30 * 1000

// Technically should be \r\n but the spec says "A well-behaved server will not require the carriage return character."
private const val TERMINATOR = '\n'
private const val SEPARATOR = ' '

private val DOCUMENTS = mapOf(
    "/" to "index",
    "/greeting" to "hello world"
)
private const val ERROR_BAD_REQUEST = "Bad request"
private const val ERROR_NOT_FOUND = "Not found"

fun main(args: Array<String>) {
    val charset = Charsets.US_ASCII // HTTP 0.7 spec defined the message to be ASCII encoded text

    val opts = buildOptionsParser().parse(*args)
    val port = opts.valueOf(ARG_PORT) as Int

    ServerSocket(port).use { serverSocket ->
        println("Listening on ${serverSocket.inetAddress}:${serverSocket.localPort}...")

        while (true) {
            using {
                try {
                    val socket = serverSocket.accept().closeWhenDone()
                    println("Connection accepted from ${socket.connectionDetails()} on port ${socket.localPort}")

                    socket.soTimeout = TIMEOUT_MS

                    val inStream = socket.getInputStream().reader(charset).buffered().closeWhenDone()
                    val outStream = socket.getOutputStream().writer(charset).buffered().closeWhenDone()
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
private fun processHttpConnection(inStream: Reader, outStream: Writer) {
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

    if (hitTerminator || sb.toString() != "GET") {
        outStream.write(ERROR_BAD_REQUEST)
        return
    }

    sb.clear()
    var hitSeparator = false
    while (true) {
        val next = inStream.read()

        if (next == -1) {
            outStream.write(ERROR_BAD_REQUEST)
            return
        } else if (next == TERMINATOR.code) break
        else if (next == SEPARATOR.code) hitSeparator = true // We want to consume, but ignore, any extraneous content up to the terminator
        else if (!hitSeparator) sb.append(next.toChar())
    }

    val documentPath = sb.toString().trim() // remove possible \r

    val document = DOCUMENTS[documentPath]
    if (document == null) {
        outStream.write(ERROR_NOT_FOUND)
        return
    }

    outStream.write(document)
}

private fun buildOptionsParser(): OptionParser {
    val parser = OptionParser()
    parser.accepts(ARG_PORT).withRequiredArg().ofType(Int::class.java)

    return parser
}
