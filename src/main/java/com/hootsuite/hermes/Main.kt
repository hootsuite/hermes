package com.hootsuite.hermes

import com.github.kittinunf.fuel.gson.responseObject
import com.github.kittinunf.fuel.httpGet
import com.hootsuite.hermes.database.DatabaseUtils
import com.hootsuite.hermes.database.ReviewRequest
import com.hootsuite.hermes.database.Team
import com.hootsuite.hermes.database.User
import com.hootsuite.hermes.github.GithubEventHandler
import com.hootsuite.hermes.github.models.Events
import com.hootsuite.hermes.github.models.SupportedEvents
import com.hootsuite.hermes.models.TeamDTO
import com.hootsuite.hermes.models.UserDTO
import com.hootsuite.hermes.slack.models.SlackAuth
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
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.jetbrains.exposed.sql.transactions.transaction
import java.text.DateFormat

/**
 * Main Entry Point into the ktor Application
 */
fun main(args: Array<String>) {

    DatabaseUtils.configureDatabase()

    val server = embeddedServer(Netty, Config.SERVER_PORT) {
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
            get(Config.Endpoint.USERS) { usersGet(call) }
            post(Config.Endpoint.USERS) { usersPost(call) }
            get(Config.Endpoint.REGISTER_USER) { registerUserGet(call) }

            // Teams
            get(Config.Endpoint.TEAMS) { teamsGet(call) }
            post(Config.Endpoint.TEAMS) { teamsPost(call) }
            get(Config.Endpoint.REGISTER_TEAM) { registerTeamGet(call) }

            // ReviewRequests
            get(Config.Endpoint.REVIEW_REQUESTS) { reviewRequestsGet(call) }

            // Install Slack App
            get(Config.Endpoint.INSTALL) { installGet(call) }
        }
    }
    server.start(wait = true)
}

/**
 * Handle the POST to the /webhook Endpoint
 * @param call - The ApplicationCall for the request
 */
suspend fun webhookPost(call: ApplicationCall) {
    val eventType = call.request.header(Events.EVENT_HEADER) ?: Events.NO_EVENT
    when (eventType) {
        SupportedEvents.PULL_REQUEST_REVIEW.eventName -> GithubEventHandler.pullRequestReview(call.receive())
        SupportedEvents.PULL_REQUEST.eventName -> GithubEventHandler.pullRequest(call.receive())
        SupportedEvents.ISSUE_COMMENT.eventName -> GithubEventHandler.issueComment(call.receive())
        SupportedEvents.STATUS.eventName -> GithubEventHandler.status(call.receive())
        SupportedEvents.PING.eventName -> GithubEventHandler.ping(call.receive())
        else -> GithubEventHandler.unhandledEvent(eventType)
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
    val users = transaction { User.all().joinToString("<br>") { it.toString() } }
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
    val user = call.receive<UserDTO>()
    DatabaseUtils.createOrUpdateUser(user)
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
    DatabaseUtils.createOrUpdateUser(UserDTO(githubName, slackName, teamName))
    // TODO Handle Problems
    call.respondText("User Created or Updated Successfully", ContentType.Text.Plain, HttpStatusCode.OK)
}

/**
 * Handle the GET to the /teams Endpoint
 * @param call - The ApplicationCall of the request
 */
suspend fun teamsGet(call: ApplicationCall) {
    // TODO Some sort of frontend
    val teams = transaction { Team.all().joinToString("<br>") { it.toString() } }
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
    val team = call.receive<TeamDTO>()
    DatabaseUtils.createOrUpdateTeam(team)
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
    DatabaseUtils.createOrUpdateTeam(TeamDTO(teamName, "https://hooks.slack.com/services/$slackUrl"))
    // TODO Handle Problems and Response
    call.respondText("Team Created or Updated Successfully", ContentType.Text.Plain, HttpStatusCode.OK)
}

/**
 * Handle the GET to the /reviewRequests Endpoint
 * @param call - The ApplicationCall of the request
 * TODO For Testing only
 */
suspend fun reviewRequestsGet(call: ApplicationCall) {
    val reviewRequests = transaction { ReviewRequest.all().joinToString("<br>") { it.toString() } }
    val teamsHtml = StringBuilder()
    teamsHtml.append("<h1>Review Requests</h1><p>")
    teamsHtml.append(reviewRequests)
    teamsHtml.append("</p>")
    call.respondText(teamsHtml.toString(), ContentType.Text.Html, HttpStatusCode.OK)
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
                Config.SLACK_ADMIN_URL = webhook.url
                val config = Config.configData
                // TODO Should there be specific setters?
                Config.configData = ConfigData(
                        webhook.url,
                        config.serverPort,
                        config.adminChannel,
                        config.rereviewCommand)
                call.respondText("Admin Channel Registered", ContentType.Text.Plain, HttpStatusCode.OK)
            } else {
                DatabaseUtils.createOrUpdateTeam(TeamDTO(webhook.channel, webhook.url))
                call.respondText("Team Created or Updated Successfully", ContentType.Text.Plain, HttpStatusCode.OK)
                // TODO Proper Error Case
            }
        } ?: call.respondText("Didn't get the right slack object sorry", ContentType.Text.Html, HttpStatusCode.OK)
    } else {
        // TODO Handle non-200
    }
}

/**
 * Create the Query Params for authorizing with slack
 * @param code The code parameter to include in the query parameters
 */
private fun createSlackQueryParams(code: String?): List<Pair<String, String>> = listOf(
        "code" to code.toString(),
        "client_id" to Config.authData.clientId,
        "client_secret" to Config.authData.secret)
