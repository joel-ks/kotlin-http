package jks.http.server

import joptsimple.OptionParser

data class AppConfiguration(
    val port: Int,
    val timeout: Int
)

private const val ARG_PORT = "port"
private const val DEFAULT_PORT = 80

private const val ARG_TIMEOUT = "timeout"
private const val DEFAULT_TIMEOUT = 0

fun parseCommandLine(args: Array<String>): AppConfiguration {
    val parser = OptionParser()
    parser.accepts(ARG_PORT).withOptionalArg().ofType(Int::class.java)
    parser.accepts(ARG_TIMEOUT).withOptionalArg().ofType(Int::class.java)

    val opts = parser.parse(*args)
    return AppConfiguration(
        port = opts.valueOf(ARG_PORT) as Int? ?: DEFAULT_PORT,
        timeout = opts.valueOf(ARG_TIMEOUT) as Int? ?: DEFAULT_TIMEOUT
    )
}
