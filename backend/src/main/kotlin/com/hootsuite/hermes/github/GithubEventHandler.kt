package com.hootsuite.hermes.github

import com.hootsuite.hermes.Config
import com.hootsuite.hermes.database.DatabaseUtils
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
 * Object to handle events from Github webhooks
 */
object GithubEventHandler {

    private const val GITHUB_MENTION = "@"

    /**
     * Route Pull Request Reviews to Slack
     * @param reviewEvent - The Event from the Github webhook
     */
    fun pullRequestReview(reviewEvent: PullRequestReviewEvent) {
        val reviewer = reviewEvent.review.user.login
        val prUrl = reviewEvent.pullRequest.htmlUrl
        DatabaseUtils.getSlackUserOrNull(reviewEvent.pullRequest.user.login)?.let { prAuthor ->
            when (reviewEvent.action) {
                PullRequestReviewAction.SUBMITTED -> when (reviewEvent.review.state) {
                    ApprovalState.APPROVED -> {
                        DatabaseUtils.createOrUpdateReview(Review.approved(reviewer, prUrl))
                        SlackMessageHandler.approved(reviewer, prAuthor, prUrl)
                    }
                    ApprovalState.CHANGES_REQUESTED -> {
                        DatabaseUtils.createOrUpdateReview(Review.changesRequested(reviewer, prUrl))
                        SlackMessageHandler.changesRequested(reviewer, prAuthor, prUrl, reviewEvent.review.body)
                    }
                    ApprovalState.COMMENTED -> {
                        DatabaseUtils.createOrUpdateReview(Review.commented(reviewer, prUrl))
                        SlackMessageHandler.commented(reviewer, prAuthor, prUrl, reviewEvent.review.body)
                    }
                }
                PullRequestReviewAction.DISMISSED -> {
                    DatabaseUtils.deleteReview(prUrl, reviewer)
                    val dismisser = reviewEvent.sender.login
                    // Only notify a dismissed review when a review is dismissed by someone other than the review author
                    if (reviewer != dismisser) {
                        DatabaseUtils.getSlackUserOrNull(reviewer)?.let { slackUser ->
                            SlackMessageHandler.reviewDismissed(dismisser, slackUser, prUrl)
                        }
                    }
                }
                PullRequestReviewAction.EDITED -> {
                    // TODO Should we do anything here?
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
                ?.let { DatabaseUtils.getSlackUserOrNull(it.login) }
                ?.let { slackUser ->
                    DatabaseUtils.createOrUpdateReviewRequest(
                        ReviewRequest(
                            pullRequestEvent.pullRequest.htmlUrl,
                            pullRequestEvent.requestedReviewer.login
                        )
                    )
                    SlackMessageHandler.requestReviewer(
                        reviewer = slackUser,
                        author = pullRequestEvent.pullRequest.user.login,
                        sender = pullRequestEvent.sender?.login,
                        url = pullRequestEvent.pullRequest.htmlUrl,
                        title = pullRequestEvent.pullRequest.title
                    )
                }
            PullRequestAction.CLOSED -> {
                pullRequestEvent.pullRequest.htmlUrl.let {
                    DatabaseUtils.deleteReviewRequests(it)
                    DatabaseUtils.deleteReviews(it)
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
            parseRereview(commentBody, commentEvent.issue.htmlUrl, commentEvent.comment.user.login, argumentList)
        } else {
            // TODO Handle Other Actions?
        }
    }

    private fun parseRereview(commentBody: String, issueUrl: String, author: String, argumentList: List<String>) {
        when {
            commentBody == Config.REREVIEW -> DatabaseUtils.getRereviewers(issueUrl).forEach {
                SlackMessageHandler.rerequestReviewer(it, author, issueUrl)
            }
            argumentList.all { it.startsWith(GITHUB_MENTION) } -> {
                argumentList.mapNotNull { DatabaseUtils.getSlackUserOrNull(it.removePrefix(GITHUB_MENTION)) }.forEach {
                    SlackMessageHandler.rerequestReviewer(it, author, issueUrl)
                }
            }
            argumentList.size == 1 && argumentList.first() == Config.REJECTED -> {
                DatabaseUtils.getReviewsByState(issueUrl, setOf(ReviewState.CHANGES_REQUESTED)).forEach {
                    SlackMessageHandler.rerequestReviewer(it, author, issueUrl)
                }

            }
            argumentList.size == 1 && argumentList.first() == Config.UNAPPROVED -> {
                DatabaseUtils.getRereviewers(issueUrl)
                    .minus(DatabaseUtils.getReviewsByState(issueUrl, setOf(ReviewState.APPROVED)))
                    .forEach { SlackMessageHandler.rerequestReviewer(it, author, issueUrl) }
            }
            else -> {
                DatabaseUtils.getSlackUserOrNull(author)?.let {
                    SlackMessageHandler.unhandledRereview(it, issueUrl, argumentList.joinToString())
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
                DatabaseUtils.getSlackUserOrNull(statusEvent.commit.author.login)?.let { slackUser ->
                    SlackMessageHandler.buildFailure(
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
        SlackMessageHandler.ping(
            zen = pingEvent.zen,
            missingEvents = missingEvents,
            extraEvents = extraEvents,
            repoName = pingEvent.repository?.fullName ?: "Organization",
            sender = pingEvent.sender.login,
            adminUrl = Config.SLACK_ADMIN_URL
        )
    }

    /**
     * Send a message to slack when an unhandled event is receieved.
     * @param eventType - The name of the event
     */
    fun unhandledEvent(eventType: String) {
        //TODO Add repo / user information for unhandled events?
        SlackMessageHandler.unhandledEvent(eventType, Config.SLACK_ADMIN_URL)
    }
}