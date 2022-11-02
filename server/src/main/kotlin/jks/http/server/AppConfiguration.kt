package jks.http.server

import joptsimple.OptionException
import joptsimple.OptionParser
import kotlin.system.exitProcess

data class AppConfiguration(
    val port: Int,
    val timeout: Int
)

private const val ARG_PORT = "port"
private const val ARG_PORT_DESC = "the TCP port to listen on"
private const val DEFAULT_PORT = 80

private const val ARG_TIMEOUT = "timeout"
private const val ARG_TIMEOUT_TYPE_DESC = "milliseconds"
private const val ARG_TIMEOUT_DESC = "timeout for all socket operations (0 for no timeout)"
private const val DEFAULT_TIMEOUT = 0

private const val ARG_HELP = "help"
private const val ARG_HELP_DESC = "show this help page"

fun parseCommandLine(args: Array<String>): AppConfiguration {
    val parser = OptionParser()

    val helpSpec = parser.accepts(ARG_HELP, ARG_HELP_DESC).forHelp()

    val portSpec = parser.accepts(ARG_PORT, ARG_PORT_DESC)
        .withRequiredArg()
        .ofType(Int::class.java)
        .defaultsTo(DEFAULT_PORT)

    val timeoutSpec = parser.accepts(ARG_TIMEOUT, ARG_TIMEOUT_DESC)
        .withRequiredArg()
        .ofType(Int::class.java)
        .defaultsTo(DEFAULT_TIMEOUT)
        .describedAs(ARG_TIMEOUT_TYPE_DESC)

    try {
        val opts = parser.parse(*args)

        if (opts.has(helpSpec)) {
            parser.printHelpOn(System.out)
            exitProcess(0)
        }

        return AppConfiguration(
            port = opts.valueOf(portSpec),
            timeout = opts.valueOf(timeoutSpec)
        )
    } catch (ex: OptionException) {
        println(ex.message)
        parser.printHelpOn(System.out)
        exitProcess(-1)
    }
}
