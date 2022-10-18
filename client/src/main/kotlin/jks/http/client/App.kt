package jks.http.client

import jks.utilities.using
import java.net.Socket

fun main() {
    println("Client running...")

    using {
        val socket = Socket("localhost", 8080).closeWhenDone()

        println("Connected to ${socket.inetAddress}:${socket.port}")

        val socketIn = socket.getInputStream().reader().buffered().closeWhenDone()
        val socketOut = socket.getOutputStream().writer().buffered().closeWhenDone()

        print("Type message and press enter to send: ")
        val line = readLine()
        socketOut.write(line ?: "")
        socketOut.write("\n")
        socketOut.flush()

        val response = socketIn.readText()
        println(response)
    }
}
