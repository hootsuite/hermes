package com.hootsuite.hermes

import com.google.gson.Gson
import com.hootsuite.hermes.database.DataStore
import com.hootsuite.hermes.github.GithubEventHandler
import com.hootsuite.hermes.github.model.*
import io.ktor.application.Application
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ApplicationTest {

    private val gson = Gson()
    @MockK(relaxed = true)
    private lateinit var mockGithubEventHandler: GithubEventHandler
    @MockK(relaxed = true)
    private lateinit var mockDataStore: DataStore

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        githubEventHandler = mockGithubEventHandler
        dataStore = mockDataStore
    }

    @Test
    fun `verify root request returns Hermes`() = withTestApplication(Application::main) {
        with(handleRequest(HttpMethod.Get, Config.Endpoint.ROOT)) {
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals("Hermes", response.content)
        }
    }

    @Test
    fun `verify public webhook API returns expected result`() = withTestApplication(Application::main) {
        with(handleRequest(HttpMethod.Post, Config.Endpoint.WEBHOOK, {
            addHeader(HttpHeaders.Accept, ContentType.Application.Json.toString())
            addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            addHeader(Events.EVENT_HEADER, SupportedEvents.PULL_REQUEST_REVIEW.eventName)
            body = gson.toJson(Companion.testPREvent)

        })) {
            verify { githubEventHandler.pullRequestReview(Companion.testPREvent) }
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }

    @Test
    fun `ensure private API request on private port return expected result`() = withTestApplication(Application::main) {
        every { mockDataStore.getAllUsers() } returns emptyList()
        with(handleRequest(method = HttpMethod.Get,
                port = Config.SERVER_PORT_PRIVATE,
                uri = Config.Endpoint.USERS)) {
            verify { mockDataStore.getAllUsers() }
            assertEquals(HttpStatusCode.OK, response.status())
            assertEquals("<h1>Users</h1><p></p>", response.content)
        }
    }

    @Test
    fun `ensure private API request on public port are forbidden`() = withTestApplication(Application::main) {
        with(handleRequest(method = HttpMethod.Get,
                port = Config.SERVER_PORT,
                uri = Config.Endpoint.USERS)) {
            assertEquals(HttpStatusCode.Forbidden, response.status())
        }
    }

    companion object {
        private val testPREvent = PullRequestReviewEvent(
                action = PullRequestReviewAction.SUBMITTED,
                review = Review(user = User("some user"),
                        state = ApprovalState.COMMENTED,
                        body = null),
                pullRequest = PullRequest(
                        htmlUrl = "http://www.someurl.com/review",
                        user = User("some user"),
                        title = "some title",
                        id = 123),
                sender = User("some user")
        )
    }
}
