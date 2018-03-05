package com.hootsuite.hermes.github

import com.hootsuite.hermes.Config
import com.hootsuite.hermes.database.DatabaseUtils
import com.hootsuite.hermes.github.models.ApprovalState
import com.hootsuite.hermes.github.models.IssueCommentAction
import com.hootsuite.hermes.github.models.IssueCommentEvent
import com.hootsuite.hermes.github.models.PingEvent
import com.hootsuite.hermes.github.models.PullRequestAction
import com.hootsuite.hermes.github.models.PullRequestEvent
import com.hootsuite.hermes.github.models.PullRequestReviewAction
import com.hootsuite.hermes.github.models.PullRequestReviewEvent
import com.hootsuite.hermes.github.models.StatusEvent
import com.hootsuite.hermes.github.models.StatusState
import com.hootsuite.hermes.github.models.SupportedEvents
import com.hootsuite.hermes.models.ReviewRequestDTO
import com.hootsuite.hermes.slack.SlackMessageHandler

/**
 * Object to handle events from Github webhooks
 */
object GithubEventHandler {

    /**
     * Route Pull Request Reviews to Slack
     * @param pullRequestReviewEvent - The Event from the Github webhook
     */
    fun pullRequestReview(pullRequestReviewEvent: PullRequestReviewEvent) {
        DatabaseUtils.getSlackUserOrNull(pullRequestReviewEvent.pullRequest.user.login)?.let { slackUser ->
            when (pullRequestReviewEvent.action) {
                PullRequestReviewAction.SUBMITTED -> when (pullRequestReviewEvent.review.state) {
                    ApprovalState.APPROVED -> SlackMessageHandler.approval(
                            reviewer = pullRequestReviewEvent.review.user.login,
                            author = slackUser,
                            url = pullRequestReviewEvent.pullRequest.htmlUrl)
                    ApprovalState.CHANGES_REQUESTED -> SlackMessageHandler.requestChanges(
                            reviewer = pullRequestReviewEvent.review.user.login,
                            author = slackUser,
                            url = pullRequestReviewEvent.pullRequest.htmlUrl,
                            comment = pullRequestReviewEvent.review.body)
                    ApprovalState.COMMENTED -> {
                        // TODO What do we want to do here?
                    }
                }
                else -> {
                    // TODO Handle other actions?
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
                        // TODO Need to clean these up when we get a PR closed event
                        DatabaseUtils.createOrUpdateReviewRequest(ReviewRequestDTO(
                                pullRequestEvent.pullRequest.htmlUrl,
                                pullRequestEvent.requestedReviewer.login))
                        SlackMessageHandler.requestReviewer(
                                reviewer = slackUser,
                                author = pullRequestEvent.pullRequest.user.login,
                                url = pullRequestEvent.pullRequest.htmlUrl,
                                title = pullRequestEvent.pullRequest.title)
                    }
            PullRequestAction.CLOSED -> DatabaseUtils.deleteReviewRequest(pullRequestEvent.pullRequest.htmlUrl)
            else -> {
                // TODO Handle Other actions?
            }
        }
    }

    /**
     * Route Issue Comment Events to Slack. Gets a list of reviewers from the database.
     * @param issueCommentEvent - The Event from the Github Webhook
     */
    fun issueComment(issueCommentEvent: IssueCommentEvent) {
        if (issueCommentEvent.action == IssueCommentAction.CREATED &&
                issueCommentEvent.comment.body == Config.REREVIEW) {
            DatabaseUtils.getRereviewers(issueCommentEvent.issue.htmlUrl).forEach {
                SlackMessageHandler.rerequestReviewer(
                        reviewer = it,
                        author = issueCommentEvent.issue.user.login,
                        url = issueCommentEvent.issue.htmlUrl)
            }
        } else {
            // TODO Handle Other Actions?
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
                            commitUrl = statusEvent.commit.htmlUrl)
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
                adminUrl = Config.SLACK_ADMIN_URL)
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