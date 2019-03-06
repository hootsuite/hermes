package com.hootsuite.hermes

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * Configuration for the Hermes Server
 */
object Config {

    private const val CONFIG_PATH = "config.json"
    private const val SECRETS_PATH = "secrets.json"

    const val SLACK_AUTH_URL = "https://slack.com/api/oauth.access"

    var configData: ConfigData = initDataFromFile(CONFIG_PATH)
        set(value) {
            File(CONFIG_PATH).writeText(GsonBuilder().setPrettyPrinting().create().toJson(value))
            field = value
        }

    val authData: AuthData = initDataFromFile(SECRETS_PATH)

    // TODO We should only need one of these, either register admin channel or configure via file
    // Admin slack webhook to send Hermes status messages to
    var ADMIN_URL = configData.adminUrl

    // Admin Channel to send Hermes Status messages to
    val ADMIN_CHANNEL = configData.adminChannel ?: "#hermes-admin"

    // Port for the Server to run on
    val SERVER_PORT = configData.serverPort ?: 8080

    // Port for the private APIs to run on
    val SERVER_PORT_PRIVATE = configData.serverPortPrivate ?: 9090

    // Trigger Comment for sending review request updates
    val REREVIEW = configData.rereview?.command ?: "!hermes"

    // Parameter passed to rereview command to only notify people who have requested changes to the pull request
    val REJECTED = configData.rereview?.rejected ?: "rejected"

    // Parameter passed to rereview command to only notify people who have not approved the pull request
    val UNAPPROVED = configData.rereview?.unapproved ?: "unapproved"


    private inline fun <reified T>initDataFromFile(path: String): T =
        if (File(path).exists()) {
            Gson().fromJson<T>(File(path).readText(), object: TypeToken<T>() {}.type)
        } else {
            throw IllegalStateException("File $path does not exist")
        }

    /**
     * Supported Endpoints
     */
    object Endpoint {
        const val ROOT = "/"
        const val WEBHOOK = "/webhook"
        const val USERS = "/users"
        const val REGISTER_USER = "/registerUser"
        const val TEAMS = "/teams"
        const val REGISTER_TEAM = "/registerTeam"
        const val REVIEW_REQUESTS = "/reviewRequests"
        const val REVIEWS = "/reviews"
        const val INSTALL = "/install"
        const val SLACK = "/slack"
    }

    /**
     * Static pages being served
     */
    object StaticPages {
        const val REGISTER_USER_PAGE = "registerUser.html"
        const val REGISTER_TEAM_PAGE = "registerTeam.html"
        const val INSTALL = "install.html"
    }
}

/**
 * Data class for the config.json file
 */
data class ConfigData(
        val adminUrl: String,
        val serverPort: Int? = null,
        val serverPortPrivate: Int? = null,
        val adminChannel: String? = null,
        val rereview: RereviewCommand? = null
)

data class RereviewCommand(
        val command: String? = null,
        val rejected: String? = null,
        val unapproved: String? = null
)

/**
 * Auth Data for authorizing with slack
 */
data class AuthData(
        val clientId: String,
        val secret: String
)