package com.hootsuite.hermes

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

/**
 * Configuration for the Hermes Server
 */
object Config {

    private const val CONFIG_PATH = "config.json"
    private const val SECRETS_PATH = "secrets.json"

    const val SLACK_AUTH_URL = "https://slack.com/api/oauth.access"

    var configData: ConfigData = Gson().fromJson<ConfigData>(File(CONFIG_PATH).readText(), ConfigData::class.java)
    set(value) {
        File(CONFIG_PATH).writeText(GsonBuilder().setPrettyPrinting().create().toJson(value))
        field = value
    }

    val authData: AuthData = Gson().fromJson<AuthData>(File(SECRETS_PATH).readText(), AuthData::class.java)

    // TODO We should only need one of these, either register admin channel or configure via file
    // Admin slack webhook to send Hermes status messages to
    var SLACK_ADMIN_URL = configData.adminUrl
    // Admin Channel to send Hermes Status messages to
    val ADMIN_CHANNEL = configData.adminChannel

    // Port for the Server to run on
    val SERVER_PORT = configData.serverPort

    // Trigger Comment for sending review request updates
    val REREVIEW = configData.rereviewCommand

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
        const val INSTALL = "/install"
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
        val serverPort: Int,
        val adminChannel: String ,
        val rereviewCommand: String)

/**
 * Auth Data for authorizing with slack
 */
data class AuthData(
        val clientId: String,
        val secret: String)