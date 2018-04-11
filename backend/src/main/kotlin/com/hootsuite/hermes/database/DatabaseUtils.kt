package com.hootsuite.hermes.database

import com.hootsuite.hermes.Config
import com.hootsuite.hermes.database.model.ReviewEntity
import com.hootsuite.hermes.database.model.ReviewRequestEntity
import com.hootsuite.hermes.database.model.ReviewRequests
import com.hootsuite.hermes.database.model.Reviews
import com.hootsuite.hermes.database.model.TeamEntity
import com.hootsuite.hermes.database.model.Teams
import com.hootsuite.hermes.database.model.UserEntity
import com.hootsuite.hermes.database.model.Users
import com.hootsuite.hermes.model.Review
import com.hootsuite.hermes.model.ReviewRequest
import com.hootsuite.hermes.model.ReviewState
import com.hootsuite.hermes.model.Team
import com.hootsuite.hermes.model.User
import com.hootsuite.hermes.slack.SlackMessageHandler
import com.hootsuite.hermes.slack.model.SlackUser
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * Object to wrap database calls
 */
object DatabaseUtils {

    /**
     * Configure the Database for storing user and team information
     */
    fun configureDatabase() {
        // TODO Set up properly
        Database.connect("jdbc:h2:./test", driver = "org.h2.Driver")

        transaction {
            logger.addLogger(StdOutSqlLogger)
            SchemaUtils.create(Users)
            SchemaUtils.create(Teams)
            SchemaUtils.create(ReviewRequests)
            SchemaUtils.create(Reviews)
        }
    }

    /**
     * Gets a slack user from the database from a given github name. If either the user or the team doesn't exist
     * returns null. Sends an admin message to slack if either is missing.
     * @param githubName - The github login for the user to lookup.
     * @return SlackUser? The slack user from the database or null if the user or team is missing
     */
    fun getSlackUserOrNull(githubName: String): SlackUser? = transaction {
        val user = UserEntity.find { Users.githubName eq githubName }.firstOrNull()
        if (user == null) {
            SlackMessageHandler.onMissingUser(githubName, Config.ADMIN_URL)
            null
        } else {
            val team = TeamEntity.find { Teams.teamName eq user.teamName }.firstOrNull()
            if (team == null) {
                SlackMessageHandler.onMissingTeam(user.githubName, user.slackName, user.teamName, Config.ADMIN_URL)
                null
            } else {
                // TODO Where should we handle '@'? Currently it's on registration
                // TODO could be used to disable notifications
                SlackUser(user.slackName, team.slackUrl, user.avatarUrl)
            }
        }
    }

    /**
     * Create or update a User in the Database
     * TODO Handle Problems storing
     * @param user - The User Model Object to be stored
     */
    fun createOrUpdateUser(user: User) = transaction {
        // TODO Extract and make multiple transactions
        val existingUser = UserEntity.find { Users.githubName eq user.githubName }.firstOrNull()
        if (existingUser != null) {
            existingUser.slackName = formatSlackHandle(user.slackName)
            existingUser.teamName = user.teamName
            existingUser.avatarUrl = user.avatarUrl
            SlackMessageHandler.onUpdateUser(
                user.githubName,
                user.slackName,
                user.teamName,
                user.avatarUrl,
                Config.ADMIN_URL
            )
        } else {
            UserEntity.new {
                githubName = user.githubName
                slackName = formatSlackHandle(user.slackName)
                teamName = user.teamName
                avatarUrl = user.avatarUrl
            }
            SlackMessageHandler.onCreateUser(
                user.githubName,
                user.slackName,
                user.teamName,
                user.avatarUrl,
                Config.ADMIN_URL
            )
        }
    }

    /**
     * Update a Users Avatar based on a slack handle
     * @param slackHandle - The slack handle of the user (including the mention character)
     * @param avatarString - The string of the User's avatar
     */
    fun updateAvatar(slackHandle: String, avatarString: String) = transaction {
        val existingUser = UserEntity.find { Users.slackName eq formatSlackHandle(slackHandle) }.firstOrNull()
        if (existingUser != null) {
            existingUser.avatarUrl = avatarString
            SlackMessageHandler.onUpdateUser(
                existingUser.githubName,
                existingUser.slackName,
                existingUser.teamName,
                existingUser.avatarUrl,
                Config.ADMIN_URL
            )
        }
    }

    /**
     * Create or update a Team in the Database
     * TODO Handle Problems storing
     * @param team - The Team Model Object to be stored
     */
    fun createOrUpdateTeam(team: Team) {
        // TODO Extract and make multiple Transactions
        transaction {
            val existingTeam = TeamEntity.find { Teams.teamName eq team.teamName }.firstOrNull()
            if (existingTeam != null) {
                existingTeam.slackUrl = team.slackUrl
                SlackMessageHandler.onUpdateTeam(
                    team.teamName,
                    team.slackUrl,
                    Config.ADMIN_URL
                )
            } else {
                TeamEntity.new {
                    teamName = team.teamName
                    slackUrl = team.slackUrl
                }
                SlackMessageHandler.onCreateTeam(
                    team.teamName,
                    team.slackUrl,
                    Config.ADMIN_URL
                )
            }
        }
    }

    /**
     * Create or update a Review Request in the database
     * TODO Handle Problems storing
     * @param request - The Review Request to be stored in the database
     */
    fun createOrUpdateReviewRequest(request: ReviewRequest) = transaction {
        ReviewRequestEntity.find {
            (ReviewRequests.htmlUrl eq request.htmlUrl).and(ReviewRequests.githubName eq request.githubName)
        }.firstOrNull() ?: ReviewRequestEntity.new {
            githubName = request.githubName
            htmlUrl = request.htmlUrl
        }
    }

    fun createOrUpdateReview(review: Review) = transaction {
        val existingReview = ReviewEntity.find {
            (Reviews.htmlUrl eq review.htmlUrl).and(Reviews.githubName eq review.githubName)
        }.firstOrNull()
        if (existingReview != null) {
            existingReview.reviewState = review.reviewState.name
        } else {
            ReviewEntity.new {
                githubName = review.githubName
                htmlUrl = review.htmlUrl
                reviewState = review.reviewState.name
            }
        }
    }

    /**
     * Delete review requests for the given Pull Request from the database
     * @param url - The Url of the Pull Request
     */
    fun deleteReviewRequests(url: String) = transaction {
        ReviewRequestEntity.find { ReviewRequests.htmlUrl eq url }.forEach { it.delete() }
    }

    /**
     * Delete reviews for the given Pull Request from the database
     * @param url - The Url of the Pull Request
     */
    fun deleteReviews(url: String) = transaction {
        ReviewEntity.find { Reviews.htmlUrl eq url }.forEach { it.delete() }
    }

    /**
     * Delete the review for the given Pull Request and Github User from the database
     * @param url - The Url of the Pull Request
     */
    fun deleteReview(url: String, githubName: String) = transaction {
        ReviewEntity.find {
            (Reviews.htmlUrl eq url).and(Reviews.githubName eq githubName)
        }.forEach { it.delete() }
    }

    /**
     * Get a list of rereviewers from the database based on a key for the Pull Request
     * @param htmlUrl - The Html URL of the Pull Request
     * @return List<SlackUser> - A list of slack users reviewing the pull request
     */
    fun getRereviewers(htmlUrl: String): List<SlackUser> = transaction {
        ReviewRequestEntity.find { ReviewRequests.htmlUrl eq htmlUrl }.mapNotNull { getSlackUserOrNull(it.githubName) }
    }

    /**
     * Get a list of Reviewers from the database based on a key for the Pull Request and a Review State
     * @param htmlUrl - The Html URL of the Pull Request
     * @param reviewState - The state of review to get reviews for
     * @return List<SlackUser> - A list of slack users who have reviewed the Pull Request with a given state
     */
    fun getReviewsByState(htmlUrl: String, reviewState: Set<ReviewState>): List<SlackUser> = transaction {
        ReviewEntity.find { Reviews.htmlUrl eq htmlUrl }
            .filter { it.reviewState in reviewState.map { it.name } }
            .mapNotNull { getSlackUserOrNull(it.githubName) }
    }

    /**
     * Format a handle for tagging in Slack
     * TODO Should the handle formatting be handled here?
     * @param name - The slack name of the user
     * @return String - The slack formatted handle
     */
    private fun formatSlackHandle(name: String) = "@$name"
}