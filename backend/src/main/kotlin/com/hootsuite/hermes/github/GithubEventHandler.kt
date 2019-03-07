package com.hootsuite.hermes.github

import com.hootsuite.hermes.Config
import com.hootsuite.hermes.database.DataStore
import com.hootsuite.hermes.github.model.ApprovalState
import com.hootsuite.hermes.github.model.IssueCommentAction
import com.hootsuite.hermes.github.model.IssueCommentEvent
import com.hootsuite.hermes.github.model.PingEvent
import com.hootsuite.hermes.github.model.PullRequestAction
import com.hootsuite.hermes.github.model.PullRequestEvent
import com.hootsuite.hermes.github.model.PullRequestReviewAction
import com.hootsuite.hermes.github.model.PullRequestReviewEvent
import com.hootsuite.hermes.github.model.StatusEvent
import com.hootsuite.hermes.github.model.StatusState
import com.hootsuite.hermes.github.model.SupportedEvents
import com.hootsuite.hermes.model.Review
import com.hootsuite.hermes.model.ReviewRequest
import com.hootsuite.hermes.model.ReviewState
import com.hootsuite.hermes.slack.SlackMessageHandler

/**
 * Class to handle events from Github webhooks
 */
class GithubEventHandler(private val dataStore: DataStore) {

    /**
     * Route Pull Request Reviews to Slack
     * @param reviewEvent - The Event from the Github webhook
     */
    fun pullRequestReview(reviewEvent: PullRequestReviewEvent) {
        val reviewer = reviewEvent.review.user.login
        val prUrl = reviewEvent.pullRequest.htmlUrl
        val author = reviewEvent.pullRequest.user.login
        val reviewBody = reviewEvent.review.body
        dataStore.getSlackUserOrNull(author)?.also { prAuthor ->
            when (reviewEvent.action) {
                PullRequestReviewAction.SUBMITTED -> if (reviewer != author) {
                    dataStore.createOrUpdateReviewRequest(ReviewRequest(prUrl, reviewer))
                    when (reviewEvent.review.state) {
                        ApprovalState.APPROVED -> {
                            dataStore.createOrUpdateReview(Review.approved(reviewer, prUrl))
                            SlackMessageHandler.onApproved(reviewer, prAuthor, prUrl)
                        }
                        ApprovalState.CHANGES_REQUESTED -> {
                            dataStore.createOrUpdateReview(Review.changesRequested(reviewer, prUrl))
                            SlackMessageHandler.onChangesRequested(reviewer, prAuthor, prUrl, reviewBody)
                        }
                        ApprovalState.COMMENTED -> {
                            dataStore.createOrUpdateReview(Review.commented(reviewer, prUrl))
                            SlackMessageHandler.onCommented(reviewer, prAuthor, prUrl, reviewBody)
                        }
                    }
                }
                PullRequestReviewAction.DISMISSED -> {
                    dataStore.deleteReview(prUrl, reviewer)
                    val sender = reviewEvent.sender.login
                    if (sender != reviewer) {
                        dataStore.getSlackUserOrNull(reviewer)?.let { slackUser ->
                            SlackMessageHandler.onReviewDismissed(sender, slackUser, prUrl)
                        }
                    }
                }
                else -> {
                    // Do Nothing
                }
            }
        }
    }

    /**
     * Route Pull Request Events to Slack. Stores a Pull Request Review in the database
     * @param pullRequestEvent - The Event from the Github webhook
     */
    fun pullRequest(pullRequestEvent: PullRequestEvent) {
        when (pullRequestEvent.action) {
            PullRequestAction.REVIEW_REQUESTED -> pullRequestEvent.requestedReviewer
                ?.let { dataStore.getSlackUserOrNull(it.login) }
                ?.let { slackUser ->
                    val requestedReviewer = pullRequestEvent.requestedReviewer.login
                    val requestSender = pullRequestEvent.sender?.login
                    dataStore.createOrUpdateReviewRequest(
                        ReviewRequest(
                            pullRequestEvent.pullRequest.htmlUrl,
                            requestedReviewer
                        )
                    )
                    if (requestSender != requestedReviewer) {
                        SlackMessageHandler.onRequestReviewer(
                            reviewer = slackUser,
                            author = pullRequestEvent.pullRequest.user.login,
                            sender = requestSender,
                            url = pullRequestEvent.pullRequest.htmlUrl,
                            title = pullRequestEvent.pullRequest.title
                        )
                    }
                }
            PullRequestAction.CLOSED -> {
                pullRequestEvent.pullRequest.htmlUrl.let {
                    dataStore.deleteReviewRequests(it)
                    dataStore.deleteReviews(it)
                }
            }
            else -> {
                // TODO Handle Other actions?
            }
        }
    }

    /**
     * Route Issue Comment Events to Slack. Gets a list of reviewers from the database.
     * @param commentEvent - The Event from the Github Webhook
     */
    fun issueComment(commentEvent: IssueCommentEvent) {
        val commentBody = commentEvent.comment.body.trim()
        if (commentEvent.action == IssueCommentAction.CREATED && commentBody.startsWith(Config.REREVIEW)) {
            // Extract the parameters passed to the rereview command
            val argumentList = commentBody.split(' ').drop(1)
            parseRereview(
                commentBody,
                commentEvent.issue.htmlUrl,
                commentEvent.comment.user.login,
                commentEvent.issue.user.login,
                argumentList
            )
        } else {
            // TODO Handle Other Actions?
        }
    }

    private fun parseRereview(
        commentBody: String,
        issueUrl: String,
        requester: String,
        author: String,
        argumentList: List<String>
    ) {
        when {
            commentBody == Config.REREVIEW -> dataStore.getRereviewers(issueUrl).forEach {
                SlackMessageHandler.onRerequestReviewer(it, requester, author, issueUrl)
            }
            argumentList.all { it.startsWith(Companion.GITHUB_MENTION) } -> {
                argumentList.mapNotNull { dataStore.getSlackUserOrNull(it.removePrefix(Companion.GITHUB_MENTION)) }.forEach {
                    SlackMessageHandler.onRerequestReviewer(it, requester, author, issueUrl)
                }
            }
            argumentList.size == 1 && argumentList.first() == Config.REJECTED -> {
                dataStore.getReviewsByState(issueUrl, setOf(ReviewState.CHANGES_REQUESTED)).forEach {
                    SlackMessageHandler.onRerequestReviewer(it, requester, author, issueUrl)
                }

            }
            argumentList.size == 1 && argumentList.first() == Config.UNAPPROVED -> {
                dataStore.getRereviewers(issueUrl)
                    .minus(dataStore.getReviewsByState(issueUrl, setOf(ReviewState.APPROVED)))
                    .forEach { SlackMessageHandler.onRerequestReviewer(it, requester, author, issueUrl) }
            }
            else -> {
                dataStore.getSlackUserOrNull(author)?.let {
                    SlackMessageHandler.onUnhandledRereview(it, issueUrl, argumentList.joinToString())
                }

            }
        }
    }

    /**
     * Send a message to slack when a status event is received such as a commit failing to build.
     * @param statusEvent - The Status Event from Github
     */
    fun status(statusEvent: StatusEvent) {
        when (statusEvent.state) {
            StatusState.FAILURE, StatusState.ERROR -> {
                dataStore.getSlackUserOrNull(statusEvent.commit.author.login)?.let { slackUser ->
                    SlackMessageHandler.onBuildFailure(
                        author = slackUser,
                        targetUrl = statusEvent.targetUrl,
                        repoName = statusEvent.repository.fullName,
                        commitUrl = statusEvent.commit.htmlUrl
                    )
                }
            }
            else -> {
                // TODO Handle Other States?
            }
        }
    }

    /**
     * Send a message to slack on a ping event. The event is sent when a new webhook is registered
     * @param pingEvent - The Ping Event from Github
     */
    fun ping(pingEvent: PingEvent) {
        val extraEvents = pingEvent.hook.events
            .filterNot { it in SupportedEvents.values().map { it.eventName } }
        val missingEvents = SupportedEvents.values()
            .filterNot { it == SupportedEvents.PING }
            .map { it.eventName }
            .filterNot { it in pingEvent.hook.events }
        SlackMessageHandler.onPing(
            zen = pingEvent.zen,
            missingEvents = missingEvents,
            extraEvents = extraEvents,
            repoName = pingEvent.repository?.fullName ?: "Organization",
            sender = pingEvent.sender.login,
            adminUrl = Config.ADMIN_URL
        )
    }

    /**
     * Send a message to slack when an unhandled event is receieved.
     * @param eventType - The name of the event
     */
    fun unhandledEvent(eventType: String) {
        //TODO Add repo / user information for unhandled events?
        SlackMessageHandler.onUnhandledEvent(eventType, Config.ADMIN_URL)
    }

    companion object {
        private const val GITHUB_MENTION = "@"
    }
}