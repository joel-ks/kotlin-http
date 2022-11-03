package jks.http.server

import java.nio.file.Path

data class HttpRoutingConfig(var webRoot: Path, var defaultFileName: String)
