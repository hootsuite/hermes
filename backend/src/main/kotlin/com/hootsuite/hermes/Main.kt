package com.hootsuite.hermes

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.gson.responseObject
import com.github.kittinunf.fuel.httpGet
import com.google.gson.Gson
import com.hootsuite.hermes.database.DataStore
import com.hootsuite.hermes.database.model.ReviewEntity
import com.hootsuite.hermes.database.model.ReviewRequestEntity
import com.hootsuite.hermes.database.model.TeamEntity
import com.hootsuite.hermes.github.GithubEventHandler
import com.hootsuite.hermes.github.model.Events
import com.hootsuite.hermes.github.model.SupportedEvents
import com.hootsuite.hermes.model.Team
import com.hootsuite.hermes.model.User
import com.hootsuite.hermes.slack.SlashCommandHandler
import com.hootsuite.hermes.slack.model.SlackAuth
import com.hootsuite.hermes.slack.model.SlashCommand
import com.hootsuite.hermes.slack.model.SlashResponse
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.content.file
import io.ktor.content.static
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.*
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.jetbrains.exposed.sql.transactions.transaction
import java.text.DateFormat

// Dependencies exposed for testing purposes
internal var dataStore = DataStore()
internal var githubEventHandler = GithubEventHandler(dataStore)
internal var slackCommandHandler = SlashCommandHandler(dataStore)

/**
 * Main Entry Point into the ktor Application
 */
fun main(args: Array<String>) {

    dataStore.configureDatabase()

    val env = applicationEngineEnvironment {
        module {
            main()
        }
        // Private API
        connector {
            host = "127.0.0.1"
            port = Config.SERVER_PORT_PRIVATE
        }
        // Public API
        connector {
            host = "0.0.0.0"
            port = Config.SERVER_PORT
        }
    }

    embeddedServer(Netty, env).start(wait = true)
}


fun Application.main() {
        install(DefaultHeaders)
        install(Compression)
        install(CallLogging)
        install(ContentNegotiation) {
            gson {
                setDateFormat(DateFormat.LONG)
                setPrettyPrinting()
            }
        }
        routing {
            get(Config.Endpoint.ROOT) { call.respondText("Hermes") }
            // TODO Better Static Serving
            static(Config.Endpoint.ROOT) {
                file(Config.StaticPages.REGISTER_USER_PAGE)
                file(Config.StaticPages.REGISTER_TEAM_PAGE)
                file(Config.StaticPages.INSTALL)
            }

            // Webhooks from Github
            post(Config.Endpoint.WEBHOOK) { webhookPost(call) }

            // Users
            get(Config.Endpoint.USERS) { if (ensurePrivateApi(call)) { usersGet(call) } }
            post(Config.Endpoint.USERS) { if (ensurePrivateApi(call)) { usersPost(call) } }
            get(Config.Endpoint.REGISTER_USER) { if (ensurePrivateApi(call)) { registerUserGet(call) } }

            // Teams
            get(Config.Endpoint.TEAMS) { if (ensurePrivateApi(call)) { teamsGet(call) } }
            post(Config.Endpoint.TEAMS) { if (ensurePrivateApi(call)) { teamsPost(call) } }
            get(Config.Endpoint.REGISTER_TEAM) { if (ensurePrivateApi(call)) { registerTeamGet(call) } }

            // ReviewRequests
            get(Config.Endpoint.REVIEW_REQUESTS) { if (ensurePrivateApi(call)) { reviewRequestsGet(call) } }

            //Reviews
            get(Config.Endpoint.REVIEWS) { if (ensurePrivateApi(call)) { reviewsGet(call) } }

            // Install Slack App
            get(Config.Endpoint.INSTALL) { if (ensurePrivateApi(call)) { installGet(call) } }

            // Handle Slack Slash Command
            post(Config.Endpoint.SLACK) { if (ensurePrivateApi(call)) { slackPost(call) } }
        }


}

private fun ensurePrivateApi(call: ApplicationCall): Boolean {
    val isPrivateApi = call.request.local.port == Config.SERVER_PORT_PRIVATE

    if (!isPrivateApi) {
        call.response.status(HttpStatusCode.Forbidden)
    }
    return isPrivateApi
}

/**
 * Handle the POST to the /webhook Endpoint
 * @param call - The ApplicationCall for the request
 */
suspend fun webhookPost(call: ApplicationCall) {
    val eventType = call.request.header(Events.EVENT_HEADER) ?: Events.NO_EVENT
    when (eventType) {
        SupportedEvents.PULL_REQUEST_REVIEW.eventName -> githubEventHandler.pullRequestReview(call.receive())
        SupportedEvents.PULL_REQUEST.eventName -> githubEventHandler.pullRequest(call.receive())
        SupportedEvents.ISSUE_COMMENT.eventName -> githubEventHandler.issueComment(call.receive())
        SupportedEvents.STATUS.eventName -> githubEventHandler.status(call.receive())
        SupportedEvents.PING.eventName -> githubEventHandler.ping(call.receive())
        else -> githubEventHandler.unhandledEvent(eventType)
    }
    // TODO Handle Problems
    call.respond(HttpStatusCode.OK)
}

/**
 * Handle the GET to the /users Endpoint
 * @param call - The ApplicationCall of the request
 */
suspend fun usersGet(call: ApplicationCall) {
    // TODO Some sort of frontend
    val users = dataStore.getAllUsers().joinToString("<br>") { it.toString() }
    val usersHtml = StringBuilder()
    usersHtml.append("<h1>Users</h1><p>")
    usersHtml.append(users)
    usersHtml.append("</p>")
    call.respondText(usersHtml.toString(), ContentType.Text.Html, HttpStatusCode.OK)
}

/**
 * Handle the POST to the /users Endpoint
 * @param call - The ApplicationCall for the request
 */
suspend fun usersPost(call: ApplicationCall) {
    val user = call.receive<User>()
    dataStore.createOrUpdateUserByGithubName(user)
    //TODO Handle problems and Response
}

/**
 * Method to handle User Creation Form input
 * TODO Add a real front end
 */
suspend fun registerUserGet(call: ApplicationCall) {
    //TODO Fix all this
    val githubName = call.parameters["githubName"]
    if (githubName == null) {
        call.respondText("Please input your github username")
        return
    }
    val slackName = call.parameters["slackName"]
    if (slackName == null) {
        call.respondText("Please input your slack username")
        return
    }
    val teamName = call.parameters["teamName"]
    if (teamName == null) {
        call.respondText("Please input your team name")
        return
    }
    val avatarUrl = if (call.parameters["avatarUrl"].isNullOrEmpty()) null else call.parameters["avatarUrl"]
    dataStore.createOrUpdateUserByGithubName(User(githubName, slackName, teamName, avatarUrl))
    // TODO Handle Problems
    call.respondText("User Created or Updated Successfully", ContentType.Text.Plain, HttpStatusCode.OK)
}

/**
 * Handle the GET to the /teams Endpoint
 * @param call - The ApplicationCall of the request
 */
suspend fun teamsGet(call: ApplicationCall) {
    // TODO Some sort of frontend
    val teams = transaction { TeamEntity.all().joinToString("<br>") { it.toString() } }
    val teamsHtml = StringBuilder()
    teamsHtml.append("<h1>Teams</h1><p>")
    teamsHtml.append(teams)
    teamsHtml.append("</p>")
    call.respondText(teamsHtml.toString(), ContentType.Text.Html, HttpStatusCode.OK)
}

/**
 * Handle the POST to the /teams Endpoint
 * @param call - The ApplicationCall for the request
 */
suspend fun teamsPost(call: ApplicationCall) {
    val team = call.receive<Team>()
    dataStore.createOrUpdateTeam(team)
    call.respond(HttpStatusCode.OK)
}

/**
 * Method to handle Team Creation Form input
 * TODO Add a real front end
 */
suspend fun registerTeamGet(call: ApplicationCall) {
    //TODO Fix all this
    val teamName = call.parameters["teamName"]
    if (teamName == null) {
        call.respondText("Please input your team name")
        return
    }
    val slackUrl = call.parameters["slackUrl"]
    if (slackUrl == null) {
        call.respondText("Please input your slack url")
        return
    }
    // TODO When we use a POST or a Slack APP, we should be able to use the full URL or the Slack Channel
    dataStore.createOrUpdateTeam(Team(teamName, "https://hooks.slack.com/services/$slackUrl"))
    // TODO Handle Problems and Response
    call.respondText("Team Created or Updated Successfully", ContentType.Text.Plain, HttpStatusCode.OK)
}

/**
 * Handle the GET to the /reviewRequests Endpoint
 * @param call - The ApplicationCall of the request
 * TODO For Testing only
 */
suspend fun reviewRequestsGet(call: ApplicationCall) {
    val requestsString = transaction {
        val name = call.request.queryParameters["name"]
        val requests = ReviewRequestEntity.all()
        val filteredRequests = name?.let { requests.filter { it.githubName == name } } ?: requests
        // TODO Do this on the frontend when built
        filteredRequests.joinToString("<br>") { "<a href=\"${it.htmlUrl}\">$it</a>" }
    }
    call.respondText("<h1>Review Requests</h1><p>$requestsString</p>", ContentType.Text.Html, HttpStatusCode.OK)
}

/**
 * Handle the GET to the /reviews Endpoint
 * @param call - The ApplicationCall of the request
 * TODO For Testing only
 */
suspend fun reviewsGet(call: ApplicationCall) {
    val reviewsString = transaction {
        val name = call.request.queryParameters["name"]
        val reviews = ReviewEntity.all()
        val filteredReviews = name?.let { reviews.filter { it.githubName == name } } ?: reviews
        // TODO Do this on the frontend when built
        filteredReviews.joinToString("<br>") { "<a href=\"${it.htmlUrl}\">$it</a>" }
    }
    call.respondText("<h1>Reviews</h1><p>$reviewsString</p>", ContentType.Text.Html, HttpStatusCode.OK)
}

/**
 * Hande the GET to the /install Endpoint. Auth the app with a specific slack channel and store that as a team in the db
 * @param call - The ApplicationCall of the request
 */
suspend fun installGet(call: ApplicationCall) {
    val (_, response, result) = Config.SLACK_AUTH_URL
        .httpGet(createSlackQueryParams(call.request.queryParameters["code"]))
        .responseObject<SlackAuth>()

    if (response.statusCode == HttpStatusCode.OK.value) {
        val (slackAuth, _) = result
        slackAuth?.incomingWebhook?.let { webhook ->
            if (webhook.channel == Config.ADMIN_CHANNEL) {
                // TODO Should this be the only place to configure Admin Channel?
                Config.ADMIN_URL = webhook.url
                val config = Config.configData
                // TODO Should there be specific setters?
                Config.configData = ConfigData(
                    webhook.url,
                    config.serverPort,
                    config.serverPortPrivate,
                    config.adminChannel,
                    config.rereview
                )
                call.respondText("Admin Channel Registered", ContentType.Text.Plain, HttpStatusCode.OK)
            } else {
                dataStore.createOrUpdateTeam(Team(webhook.channel, webhook.url))
                call.respondText("Team Created or Updated Successfully", ContentType.Text.Plain, HttpStatusCode.OK)
                // TODO Proper Error Case
            }
        } ?: call.respondText("Didn't get the right slack object sorry", ContentType.Text.Html, HttpStatusCode.OK)
    } else {
        // TODO Handle non-200
    }
}

/**
 * Handle the POST to the /slack Endpoint. This handles all the slash commands that are supported by hermes.
 * @param call - The ApplicationCall of the request
 */
suspend fun slackPost(call: ApplicationCall) {
    val slashCommand = SlashCommand.fromParameters(call.receive())
    val splitText = slashCommand.text.split(' ')
    val command = splitText.firstOrNull()
    val parameters = splitText.drop(1)
    val responseText = slackCommandHandler.handleSlashCommand(slashCommand, command, parameters)
    call.respond(HttpStatusCode.OK)
    Fuel
        .post(slashCommand.responseUrl)
        .body(Gson().toJson(SlashResponse.ephemeral(responseText)))
        .response { _, response, result ->
            println(response)
            println(result)
            //TODO Handle Response and Result
        }
}

/**
 * Create the Query Params for authorizing with slack
 * @param code The code parameter to include in the query parameters
 */
private fun createSlackQueryParams(code: String?): List<Pair<String, String>> = listOf(
    "code" to code.toString(),
    "client_id" to Config.authData.clientId,
    "client_secret" to Config.authData.secret
)
