package jks.http.server

import joptsimple.OptionException
import joptsimple.OptionParser
import kotlin.system.exitProcess

data class AppConfiguration(
    val port: Int,
    val timeout: Int,
    val webRoot: String,
    val defaultPage: String,
    val badRequestPage: String,
    val notFoundPage: String,
    val internalServerErrorPage: String
)

private const val ARG_HELP_SHORT = "h"
private const val ARG_HELP_LONG = "help"
private const val ARG_HELP_DESC = "show this help page"

private const val ARG_PORT_SHORT = "p"
private const val ARG_PORT_LONG = "port"
private const val ARG_PORT_DESC = "the TCP port to listen on"
private const val DEFAULT_PORT = 80

private const val ARG_TIMEOUT_SHORT = "t"
private const val ARG_TIMEOUT_LONG = "timeout"
private const val ARG_TIMEOUT_TYPE_DESC = "milliseconds"
private const val ARG_TIMEOUT_DESC = "timeout for all socket operations (0 for no timeout)"
private const val DEFAULT_TIMEOUT = 0

private const val ARG_WEB_ROOT_SHORT = "w"
private const val ARG_WEB_ROOT_LONG = "web-root"
private const val ARG_WEB_ROOT_DESC = "the root directory to serve files from"
private const val DEFAULT_WEB_ROOT = "webroot"

private const val ARG_DEFAULT_PAGE_LONG = "default"
private const val ARG_DEFAULT_PAGE_DESC = "default page to serve from directories"
private const val DEFAULT_DEFAULT_PAGE = "index.html"

private const val ARG_BAD_REQ_PAGE_LONG = "error-bad-request"
private const val ARG_BAD_REQ_PAGE_DESC = "page to serve on a bad request"
private const val DEFAULT_BAD_REQ_PAGE = "pages/BadRequest.html"

private const val ARG_NOT_FOUND_PAGE_LONG = "error-not-found"
private const val ARG_NOT_FOUND_PAGE_DESC = "page to serve when a requested resource doesn't exist"
private const val DEFAULT_NOT_FOUND_PAGE = "pages/NotFound.html"

private const val ARG_SERVER_ERROR_PAGE_LONG = "error-server"
private const val ARG_SERVER_ERROR_PAGE_DESC = "page to serve when an error occurs processing a request"
private const val DEFAULT_SERVER_ERROR_PAGE = "pages/InternalServerError.html"
fun parseCommandLine(args: Array<String>): AppConfiguration {
    val parser = OptionParser(false)

    val helpSpec = parser.acceptsAll(listOf(ARG_HELP_SHORT, ARG_HELP_LONG), ARG_HELP_DESC).forHelp()

    val portSpec = parser.acceptsAll(listOf(ARG_PORT_SHORT, ARG_PORT_LONG), ARG_PORT_DESC)
        .withRequiredArg()
        .ofType(Int::class.java)
        .defaultsTo(DEFAULT_PORT)

    val timeoutSpec = parser.acceptsAll(listOf(ARG_TIMEOUT_SHORT, ARG_TIMEOUT_LONG), ARG_TIMEOUT_DESC)
        .withRequiredArg()
        .ofType(Int::class.java)
        .defaultsTo(DEFAULT_TIMEOUT)
        .describedAs(ARG_TIMEOUT_TYPE_DESC)

    val webRootSpec = parser.acceptsAll(listOf(ARG_WEB_ROOT_SHORT, ARG_WEB_ROOT_LONG), ARG_WEB_ROOT_DESC)
        .withRequiredArg()
        .ofType(String::class.java)
        .defaultsTo(DEFAULT_WEB_ROOT)

    val badRequestPageSpec = parser.accepts(ARG_BAD_REQ_PAGE_LONG, ARG_BAD_REQ_PAGE_DESC)
        .withRequiredArg()
        .ofType(String::class.java)
        .defaultsTo(DEFAULT_BAD_REQ_PAGE)

    val defaultPageSpec = parser.accepts(ARG_DEFAULT_PAGE_LONG, ARG_DEFAULT_PAGE_DESC)
        .withRequiredArg()
        .ofType(String::class.java)
        .defaultsTo(DEFAULT_DEFAULT_PAGE)

    val notFoundPageSpec = parser.accepts(ARG_NOT_FOUND_PAGE_LONG, ARG_NOT_FOUND_PAGE_DESC)
        .withRequiredArg()
        .ofType(String::class.java)
        .defaultsTo(DEFAULT_NOT_FOUND_PAGE)

    val serverErrorPageSpec = parser.accepts(ARG_SERVER_ERROR_PAGE_LONG, ARG_SERVER_ERROR_PAGE_DESC)
        .withRequiredArg()
        .ofType(String::class.java)
        .defaultsTo(DEFAULT_SERVER_ERROR_PAGE)

    try {
        val opts = parser.parse(*args)

        if (opts.has(helpSpec)) {
            parser.printHelpOn(System.out)
            exitProcess(0)
        }

        return AppConfiguration(
            port = opts.valueOf(portSpec),
            timeout = opts.valueOf(timeoutSpec),
            webRoot = opts.valueOf(webRootSpec),
            defaultPage = opts.valueOf(defaultPageSpec),
            badRequestPage = opts.valueOf(badRequestPageSpec),
            notFoundPage = opts.valueOf(notFoundPageSpec),
            internalServerErrorPage = opts.valueOf(serverErrorPageSpec),
        )
    } catch (ex: OptionException) {
        println(ex.message)
        parser.printHelpOn(System.out)
        exitProcess(-1)
    }
}
