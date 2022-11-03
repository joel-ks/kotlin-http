package jks.http.server

import jks.utilities.connectionDetails
import jks.utilities.using
import java.io.IOException
import java.io.OutputStream
import java.io.Reader
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.system.exitProcess

private val CHARSET = Charsets.US_ASCII // HTTP 0.7 spec defined the message to be ASCII encoded text

// Technically should be \r\n but the spec says "A well-behaved server will not require the carriage return character."
private const val TERMINATOR = '\n'
private const val SEPARATOR = ' '

fun main(args: Array<String>) {
    val config = parseCommandLine(args)
    checkEnvironment(config)

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
                    processHttpConnection(inStream, outStream, config)
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

private fun checkEnvironment(config: AppConfiguration) {
    println("Running in ${System.getProperty("user.dir")}")

    if (!Paths.get(config.webRoot).exists()) {
        println("Could not find web root ${config.webRoot}")
        exitProcess(-1)
    }
    if (!Paths.get(config.badRequestPage).exists()) {
        println("Could not find bad request page ${config.badRequestPage}")
        exitProcess(-1)
    }
    if (!Paths.get(config.notFoundPage).exists()) {
        println("Could not find not found page ${config.notFoundPage}")
        exitProcess(-1)
    }
    if (!Paths.get(config.internalServerErrorPage).exists()) {
        println("Could not find internal server error page ${config.internalServerErrorPage}")
        exitProcess(-1)
    }
}

@Throws(IOException::class)
private fun processHttpConnection(inStream: Reader, outStream: OutputStream, config: AppConfiguration) {
    val documentPath = readHttpRequest(inStream)

    if (documentPath == null) sendFile(Paths.get(config.badRequestPage), outStream)
    else {
        var requestedDocument = Paths.get(config.webRoot, documentPath)
        if (requestedDocument.exists() && requestedDocument.isDirectory())
            requestedDocument = requestedDocument.resolve(config.defaultPage)

        if (requestedDocument.exists()) sendFile(requestedDocument, outStream)
        else sendFile(Paths.get(config.notFoundPage), outStream)
    }
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

private fun sendFile(file: Path, outputStream: OutputStream) {
    Files.newInputStream(file).buffered().use { it.transferTo(outputStream) }
}
