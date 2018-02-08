package com.hootsuite.hermes.slack.models

import com.google.gson.annotations.SerializedName

/**
 * Attachment for formatting Slack Messages
 */
data class Attachment(
        val fallback: String,
        val color: String? = null,
        val author_name: String? = null,
        val title: String,
        val title_link: String? = null,
        val text: String
)

/**
 * Combination of a Slack hook url and a slack username
 */
data class SlackUser(
        val name: String,
        val slackUrl: String
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
        val incomingWebhook: IncomingWebhook)

/**
 * Incoming Webhooks Configuration
 */
data class IncomingWebhook(
        val channel: String,
        @SerializedName("configuration_url")
        val configurationUrl: String,
        val url: String)
