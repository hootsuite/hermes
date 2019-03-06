package com.hootsuite.hermes

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationRequest

/**
 * Extension on [TestApplicationEngine] that allows for specifying a port
 */
fun TestApplicationEngine.handleRequest(
        method: HttpMethod,
        port: Int = 80,
        uri: String,
        setup: TestApplicationRequest.() -> Unit = {}
): TestApplicationCall = handleRequest {
    this.uri = uri
    this.method = method
    this.addHeader(HttpHeaders.Host, "localhost:$port")
    setup()
}
