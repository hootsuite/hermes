package com.hootsuite.hermes.slack.model

import com.google.gson.annotations.SerializedName
import java.util.Arrays

/**
 * Parameters to Format Slack Messages
 */
data class SlackParams(
    @SerializedName("icon_emoji")
    val iconEmoji: String? = null,
    // TODO Override hashcode and equals?
    val attachments: Array<Attachment>,
    @SerializedName("link_names")
    val linkNames: Int = 0
) {
    companion object {

        /**
         * Format a Pull Request Approval Message for Slack
         * @param reviewer - The Reviewer of the Pull Request
         * @param author - The Author of the Pull Request
         * @param url - The http URL of the Pull Request
         * @param avatarUrl - The Url of the avatar of the user or null if there is no avatar
         * @return SlackParams - The params of the formatted slack message
         */
        fun approved(reviewer: String, author: String, url: String, avatarUrl: String?) = SlackParams(
            iconEmoji = ":thumbsup:",
            attachments = arrayOf(
                Attachment(
                    fallback = "$reviewer}approved PR authored by $author.",
                    color = "#36a64f",
                    author_name = reviewer,
                    title = "Pull Request Approved: ${formatUrl(url)}",
                    title_link = url,
                    text = "<$author>, your PR has been approved.",
                    thumb_url = avatarUrl
                )
            ),
            linkNames = 1
        )

        /**
         * Format a Pull Request Request Changes Message for Slack
         * @param reviewer - The Reviewer of the Pull Request
         * @param author - The Author of the Pull Request
         * @param url - The http URL of the Pull Request
         * @param comment - The comment on the Review
         * @param avatarUrl - The Url of the avatar of the user or null if there is no avatar
         * @return SlackParams - The params of the formatted slack message
         */
        fun changesRequested(reviewer: String, author: String, url: String, comment: String?, avatarUrl: String?) =
            SlackParams(
                iconEmoji = ":no_entry:",
                attachments = arrayOf(
                    Attachment(
                        fallback = "$reviewer - Changes Requested for $author.",
                        color = "danger",
                        author_name = reviewer,
                        title = "PR Changes Requested: ${formatUrl(url)}",
                        title_link = url,
                        text = "<$author>, changes are requested to your PR.${comment?.let { "\n$comment" } ?: ""}",
                        thumb_url = avatarUrl
                    )
                ),
                linkNames = 1
            )

        /**
         * Format a Pull Request Commented Message for Slack
         * @param reviewer - The Reviewer of the Pull Request
         * @param author - The Author of the Pull Request
         * @param url - The http URL of the Pull Request
         * @param comment - The comment on the Review
         * @param avatarUrl - The Url of the avatar of the user or null if there is no avatar
         * @return SlackParams - The params of the formatted slack message
         */
        fun commented(reviewer: String, author: String, url: String, comment: String?, avatarUrl: String?) =
            SlackParams(
                iconEmoji = ":eyes:",
                attachments = arrayOf(
                    Attachment(
                        fallback = "$reviewer - Comments left for $author.",
                        color = "warning",
                        author_name = reviewer,
                        title = "PR Commented: ${formatUrl(url)}",
                        title_link = url,
                        text = "<$author>, comments have been left on your PR.${comment?.let { "\n$comment" } ?: ""}",
                        thumb_url = avatarUrl
                    )
                ),
                linkNames = 1
            )

        /**
         * Format a Pull Request Commented Message for Slack
         * @param dismisser - The person who dismissed the Pull Request Review
         * @param reviewer - The Author of the Pull Request Review
         * @param url - The http URL of the Pull Request
         * @param avatarUrl - The Url of the avatar of the user or null if there is no avatar
         * @return SlackParams - The params of the formatted slack message
         */
        fun reviewDismissed(dismisser: String, reviewer: String, url: String, avatarUrl: String?) = SlackParams(
            iconEmoji = ":cyclone:",
            attachments = arrayOf(
                Attachment(
                    fallback = "$reviewer: $dismisser has dismissed your Pull Request.",
                    color = "warning",
                    author_name = dismisser,
                    title = "Review Dismissed: ${formatUrl(url)}",
                    title_link = url,
                    text = "<$reviewer>, one of your reviews have been dismissed by $dismisser.",
                    thumb_url = avatarUrl
                )
            ),
            linkNames = 1
        )

        /**
         * Format a Review Request Message for Slack
         * @param reviewer - The Requested Reviewer of the Pull Request
         * @param author - The Author of the Pull Request
         * @param sender - The User who send the review Request
         * @param url - The http URL of the Pull Request
         * @param title - The title of the Pull Request
         * @param avatarUrl - The Url of the avatar of the user or null if there is no avatar
         * @return SlackParams - The params of the formatted slack message
         */
        fun requestReviewer(
            reviewer: String,
            author: String,
            sender: String?,
            url: String,
            title: String,
            avatarUrl: String?
        ) = SlackParams(
            attachments = arrayOf(
                Attachment(
                    fallback = "$reviewer: $author has requested your review on a Pull Request.",
                    color = "#439FE0",
                    author_name = author,
                    title = "Review Requested: ${formatUrl(url)}",
                    title_link = url,
                    text = "<$reviewer>: ${sender ?: author} has requested your review on a Pull Request.\n$title",
                    thumb_url = avatarUrl
                )
            ),
            linkNames = 1
        )

        /**
         * Format a Review Request Message for Slack
         * @param reviewer - The Requested Reviewer of the Pull Request
         * @param author - The Author of the Pull Request
         * @param url - The http URL of the Pull Request
         * @param avatarUrl - The Url of the avatar of the user or null if there is no avatar
         * @return SlackParams - The params of the formatted slack message
         */
        fun rerequestReviewer(reviewer: String, requester: String, author: String, url: String, avatarUrl: String?) =
            SlackParams(
                attachments = arrayOf(
                    Attachment(
                        fallback = "$reviewer: $author has updated their Pull Request.",
                        color = "#439FE0",
                        author_name = author,
                        title = "Please take another look: ${formatUrl(url)}",
                        title_link = url,
                        text = "<$reviewer>: $requester has requested another look at the Pull Request.",
                        thumb_url = avatarUrl
                    )
                ),
                linkNames = 1
            )

        fun unhandledReview(commenter: String, url: String, arguments: String, avatarUrl: String?) = SlackParams(
            attachments = arrayOf(
                Attachment(
                    fallback = "$commenter: Your rereview command failed.",
                    color = "warning",
                    author_name = commenter,
                    title = "Your rereview command failed: ${formatUrl(url)}",
                    title_link = url,
                    text = "<$commenter>: Please use /help rereview to see accepted arguments\nYou tried: $arguments",
                    thumb_url = avatarUrl
                )
            ),
            linkNames = 1
        )

        /**
         * Format a Build Failure Message for Slack
         * @param author - The author of the Pull Request with a failing commit
         * @param targetUrl - The targetUrl from the failing status
         * @param repoName - The full repo name (Org and Repo) with the failing commit
         * @param commitUrl - The Html URL of the failing commit
         * @param avatarUrl - The Url of the avatar of the user or null if there is no avatar
         * @return SlackParams - The params of the formatted slack message
         */
        fun buildFailure(
            author: String,
            targetUrl: String,
            repoName: String,
            commitUrl: String,
            avatarUrl: String?
        ) = SlackParams(
            iconEmoji = ":boom:",
            attachments = arrayOf(
                Attachment(
                    fallback = "$author: Your commit failed to build",
                    color = "danger",
                    title = "$author: Your Commit Failed to Build - $repoName",
                    title_link = commitUrl,
                    text = "<$author>: Your commit failed to build.\n$targetUrl",
                    thumb_url = avatarUrl
                )
            ),
            linkNames = 1
        )

        /**
         * Format a User Created Message for Slack
         * @param githubName - The github login of the User
         * @param slackName - The slack name of the User
         * @param teamName - The Hermes team name of the user
         * @param avatarUrl - The Url of the avatar of the user or null if there is no avatar
         * @return SlackParams - The params of the formatted slack message
         */
        fun createUser(githubName: String, slackName: String, teamName: String, avatarUrl: String?) = SlackParams(
            attachments = arrayOf(
                Attachment(
                    fallback = "$githubName has registered.",
                    color = "#00FF00",
                    title = "$githubName has registered.",
                    text = "$githubName: Slack: $slackName, Team: $teamName",
                    thumb_url = avatarUrl
                )
            )
        )

        /**
         * Format a User Updated Message for Slack
         * @param githubName - The github login of the User
         * @param slackName - The slack name of the User
         * @param teamName - The Hermes team name of the user
         * * @param avatarUrl - The Url of the avatar of the user or null if there is no avatar
         * @return SlackParams - The params of the formatted slack message
         */
        fun updateUser(githubName: String, slackName: String, teamName: String, avatarUrl: String?) = SlackParams(
            attachments = arrayOf(
                Attachment(
                    fallback = "$githubName has updated their information",
                    color = "#0000FF",
                    title = "$githubName has updated their information",
                    text = "$githubName: Slack: $slackName, Team: $teamName",
                    thumb_url = avatarUrl
                )
            )
        )

        /**
         * Format a Team Created Message for Slack
         * @param teamName - The Hermes team name of the user
         * @param slackUrl - The slack webhook url of the team
         * @return SlackParams - The params of the formatted slack message
         */
        fun createTeam(teamName: String, slackUrl: String) = SlackParams(
            attachments = arrayOf(
                Attachment(
                    fallback = "$teamName has been registered",
                    color = "#00FF00",
                    title = "$teamName has been registered",
                    text = "$teamName has registered to Hermes\n$slackUrl."
                )
            )
        )

        /**
         * Format a Team Updated Message for Slack
         * @param teamName - The Hermes team name of the user
         * @param slackUrl - The slack webhook url of the team
         * @return SlackParams - The params of the formatted slack message
         */
        fun updateTeam(teamName: String, slackUrl: String) = SlackParams(
            attachments = arrayOf(
                Attachment(
                    fallback = "$teamName has been updated",
                    color = "#0000FF",
                    title = "$teamName has been updated",
                    text = "$teamName has been updated\n$slackUrl."
                )
            )
        )

        /**
         * Format a missing User message for Slack
         * @param githubName - The github name of the missing user
         * @return SlackParams - The params of the formatted slack message
         */
        fun missingUser(githubName: String) = SlackParams(
            attachments = arrayOf(
                Attachment(
                    fallback = "$githubName is not registered with Hermes",
                    color = "#FF0000",
                    title = "$githubName is not Registered",
                    text = "$githubName is not Registered."
                )
            )
        )

        /**
         * Format a missing team message for Slack
         * @param githubName - The github name of the user with the missing team
         * @param slackName - The slack name of the user with the missing team
         * @param teamName - The name of the missing team
         * @return SlackParams - The params of the formatted slack message
         */
        fun missingTeam(githubName: String, slackName: String, teamName: String) = SlackParams(
            attachments = arrayOf(
                Attachment(
                    fallback = "$teamName is not registered with Hermes",
                    color = "#FF0000",
                    title = "$teamName is not Registered",
                    text = "$teamName is not Registered. User: $githubName, Slack: $slackName"
                )
            )
        )

        /**
         * Format an unhandled Event message for Slack
         * @param eventName - The name of the unhandled event
         * @return SlackParams - The params of the formatted slack message
         */
        fun unhandledEvent(eventName: String) = SlackParams(
            attachments = arrayOf(
                Attachment(
                    fallback = "Received Unhandled $eventName event.",
                    color = "#FF0000",
                    title = "Unhandled $eventName",
                    text = "Received Unhandled $eventName event."
                )
            )
        )

        /**
         * Format a ping (new webhook registered) message for slack
         * @param zen - Github's short zen message.
         * @param missingEvents - The list of events that should be registered but were not
         * @param extraEvents - The list of events that were registred but should not be
         * @param repoName - The full name of the repository where the webhook was registered
         * @param sender - The person who configured the webhook
         */
        fun ping(
            zen: String,
            missingEvents: List<String>,
            extraEvents: List<String>,
            repoName: String,
            sender: String
        ): SlackParams {
            val eventsString = when {
                missingEvents.isEmpty() && extraEvents.isEmpty() -> "Events registered Correctly\n$zen."
                missingEvents.isEmpty() && extraEvents.isNotEmpty() ->
                    "Webhook misconfigured - Extra Events: ${extraEvents.joinToString(", ")}"
                missingEvents.isNotEmpty() && extraEvents.isEmpty() ->
                    "Webhook misconfigured - Missing Events: ${missingEvents.joinToString(", ")}"
                else ->
                    """Webhook misconfigured
                        Missing Events: ${missingEvents.joinToString(", ")}
                        Extra Events: ${extraEvents.joinToString(", ")}"""
            }
            return SlackParams(
                attachments = arrayOf(
                    Attachment(
                        fallback = "New webhook registered for $repoName",
                        color = "0000FF",
                        title = "New Webhook Registered for $repoName",
                        text = "$sender registered a webhook\n$eventsString"
                    )
                )
            )
        }

        /**
         * Formats a Github URL to only include the org or user name, repo name, and PR number
         * @param url - The Github URL to format
         * @return String - The formatted URL
         */
        private fun formatUrl(url: String) = url.split('/').takeLast(4).joinToString("/")

    }

    /**
     * Generated equals method
     * @param other - The object to compare this to
     * @return Boolean - True if the objects are equal, false otherwise
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SlackParams

        if (iconEmoji != other.iconEmoji) return false
        if (!Arrays.equals(attachments, other.attachments)) return false
        if (linkNames != other.linkNames) return false

        return true
    }

    /**
     * Generated hashCode Method
     * @return Int - The hashcode of the object
     */
    override fun hashCode(): Int {
        var result = iconEmoji?.hashCode() ?: 0
        result = 31 * result + Arrays.hashCode(attachments)
        result = 31 * result + linkNames
        return result
    }
}