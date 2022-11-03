package jks.http.server.app

data class AppConfiguration(
    val port: Int,
    val timeout: Int,
    val webRoot: String,
    val defaultPage: String,
    val badRequestPage: String,
    val notFoundPage: String,
    val internalServerErrorPage: String
)
