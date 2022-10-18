package jks.http.server

import jks.utilities.using
import joptsimple.OptionParser
import java.net.ServerSocket

const val ARG_PORT = "port"

fun main(args: Array<String>) {
    val opts = buildOptionsParser().parse(*args)
    val port = opts.valueOf(ARG_PORT) as Int

    ServerSocket(port).use { serverSocket ->
        println("Server running on port $port...")

        while (true) {
            try {
                using {
                    val socket = serverSocket.accept().closeWhenDone()

                    println("Connection accepted from ${socket.inetAddress} on port ${socket.port}")

                    val inStream = socket.getInputStream().buffered().closeWhenDone()
                    val outStream = socket.getOutputStream().buffered().closeWhenDone()

                    var read: Int
                    var count = 0
                    do {
                        read = inStream.read()
                        if (read == -1 || read == ('\n'.code)) break

                        outStream.write(read)
                        ++count
                    } while (true)

                    outStream.flush()
                    println("Echoed $count bytes")
                }
            } catch (e: Exception) {
                print("Exception occurred: ")
                e.printStackTrace()
            }
        }
    }
}

private fun buildOptionsParser(): OptionParser {
    val parser = OptionParser()
    parser.accepts(ARG_PORT).withRequiredArg().ofType(Int::class.java)

    return parser
}
