package com.hootsuite.hermes.slack.model

import com.google.gson.annotations.SerializedName
import io.ktor.http.Parameters

/**
 * Attachment for formatting Slack Messages
 */
data class Attachment(
    val fallback: String,
    val color: String? = null,
    val author_name: String? = null,
    val title: String,
    val title_link: String? = null,
    val text: String,
    val thumb_url: String? = null
)

/**
 * Combination of a Slack hook url, slack username, and an optional Avatar
 */
data class SlackUser(
    val name: String,
    val slackUrl: String,
    val avatarUrl: String? = null
)

/**
 * Auth Object which is recieved when an App is authed with slack
 */
data class SlackAuth(
    @SerializedName("ok")
    val isOk: Boolean,
    @SerializedName("team_name")
    val teamName: String,
    @SerializedName("incoming_webhook")
    val incomingWebhook: IncomingWebhook
)

/**
 * Incoming Webhooks Configuration
 */
data class IncomingWebhook(
    val channel: String,
    @SerializedName("configuration_url")
    val configurationUrl: String,
    val url: String
)

data class SlashCommand(
    val username: String,
    val channel: String,
    val responseUrl: String,
    val text: String
) {
    companion object {
        const val REGISTER = "register"
        const val UNREGISTER = "unregister"
        const val AVATAR = "avatar"
        const val REVIEWS = "reviews"

        fun fromParameters(parameters: Parameters) = SlashCommand(
            parameters["user_name"] ?: "",
            parameters["channel_name"] ?: "",
            parameters["response_url"] ?: "",
            parameters["text"] ?: ""

        )
    }
}

data class SlashResponse(
    @SerializedName("response_type")
    val responseType: ResponseType,
    val text: String
) {
    companion object {
        fun ephemeral(text: String) = SlashResponse(ResponseType.EPHEMERAL, text)
    }

    enum class ResponseType {
        @SerializedName("in_channel")
        IN_CHANNEL,
        @SerializedName("ephemeral")
        EPHEMERAL
    }
}
