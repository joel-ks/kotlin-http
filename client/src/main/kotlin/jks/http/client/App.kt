package jks.http.client

import jks.utilities.connectionDetails
import jks.utilities.using
import java.net.Socket

private const val TERMINATOR = "\r\n"
private const val TIMEOUT_MS = 30 * 1000

fun main() {
    val charset = Charsets.US_ASCII // HTTP 0.7 spec defined the message to be ASCII encoded text

    println("Client running...")

    using {
        val socket = Socket("localhost", 8080).closeWhenDone()
        println("Connected to ${socket.connectionDetails()}")

        socket.soTimeout = TIMEOUT_MS

        val socketIn = socket.getInputStream().reader(charset).buffered().closeWhenDone()
        val socketOut = socket.getOutputStream().writer(charset).buffered().closeWhenDone()

        print("Type message and press enter to send: ")
        val line = readLine()
        socketOut.write(line ?: "")
        socketOut.write(TERMINATOR)
        socketOut.flush()

        val response = socketIn.readText()
        println(response)
    }
}
