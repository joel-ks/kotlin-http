package jks.utilities

import java.net.Socket

fun Socket.connectionDetails(): String = "${this.inetAddress}:${this.port}"
