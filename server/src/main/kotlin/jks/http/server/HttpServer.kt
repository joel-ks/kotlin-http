package jks.http.server

import jks.utilities.connectionDetails
import jks.utilities.using
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.Reader
import java.net.ServerSocket
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.system.exitProcess

// Technically should be \r\n but the spec says "A well-behaved server will not require the carriage return character."
private const val TERMINATOR = '\n'
private const val SEPARATOR = ' '

class HttpServer private constructor(
    private val port: Int,
    var timeout: Int,
    private val routingConfig: HttpRoutingConfig,
    private val badRequestPage: Path,
    private val notFoundPage: Path,
    private val internalServerErrorPage: Path,
) {
    private val charset: Charset = Charsets.US_ASCII // HTTP 0.7 spec defined the message to be ASCII encoded text

    init {
        println("Running in ${System.getProperty("user.dir")}")

        if (!routingConfig.webRoot.exists()) {
            println("Could not find web root ${routingConfig.webRoot}")
            exitProcess(-1)
        }
        if (!badRequestPage.exists()) {
            println("Could not find bad request page $badRequestPage")
            exitProcess(-1)
        }
        if (!notFoundPage.exists()) {
            println("Could not find not found page $notFoundPage")
            exitProcess(-1)
        }
        if (!internalServerErrorPage.exists()) {
            println("Could not find internal server error page $internalServerErrorPage")
            exitProcess(-1)
        }
    }

    class Builder(var port: Int) {
        var timeout: Int = 0
        val routingConfig = HttpRoutingConfig(Path("webroot"), "index.html")
        var badRequestPage: Path = getResourcePath("BadRequest.html")
        var notFoundPage: Path = getResourcePath("NotFound.html")
        var internalServerErrorPage: Path = getResourcePath("InternalServerError.html")

        fun build(): HttpServer = HttpServer(port, timeout, routingConfig, badRequestPage, notFoundPage, internalServerErrorPage)
    }

    companion object {
        inline fun build(port: Int, block: Builder.() -> Unit): HttpServer = Builder(port).apply(block).build()

        private fun getResourcePath(resource: String): Path {
            val uri = HttpServer::class.java.classLoader.getResource(resource)?.toURI() ?: throw Exception("Resource not found: $resource")
            return Path.of(uri)
        }
    }

    fun listen() {
        ServerSocket(port).use { serverSocket ->
            println("Listening on ${serverSocket.inetAddress}:${serverSocket.localPort}...")

            while (true) {
                using {
                    try {
                        val socket = serverSocket.accept().closeWhenDone()
                        println("Connection accepted from ${socket.connectionDetails()} on port ${socket.localPort}")

                        socket.soTimeout = timeout

                        val inStream = socket.getInputStream().reader(charset).buffered().closeWhenDone()
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

    private fun processHttpConnection(inStream: Reader, outStream: OutputStream) {
        val resourcePath = readHttpRequest(inStream)

        try {
            val resourceStream = if (resourcePath == null) Files.newInputStream(badRequestPage) else getResource(resourcePath)
            resourceStream.buffered().transferTo(outStream)
        } catch (ex: InvalidPathException) {
            Files.newInputStream(internalServerErrorPage).transferTo(outStream)
            throw ex
        }
    }

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
        while (true) {
            val next = inStream.read()

            if (next == -1) return null
            else if (next == TERMINATOR.code || next == SEPARATOR.code) break
            else sb.append(next.toChar())
        }

        return sb.toString().trim() // remove possible '\r'
    }

    private fun getResource(resourcePath: String): InputStream {
        // make sure the document path is relative, so it can be resolved against webRoot
        val resourcePathSanitised = if (resourcePath[0] == '/') resourcePath.substring(1) else resourcePath

        var requestedDocument = routingConfig.webRoot.resolve(resourcePathSanitised)
        if (requestedDocument.exists() && requestedDocument.isDirectory())
            requestedDocument = requestedDocument.resolve(routingConfig.defaultFileName)

        return if (requestedDocument.exists()) Files.newInputStream(requestedDocument)
        else Files.newInputStream(notFoundPage)
    }
}
