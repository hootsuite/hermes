package com.hootsuite.hermes.database

import com.hootsuite.hermes.Config
import com.hootsuite.hermes.database.model.ReviewRequestEntity
import com.hootsuite.hermes.database.model.ReviewRequests
import com.hootsuite.hermes.database.model.TeamEntity
import com.hootsuite.hermes.database.model.Teams
import com.hootsuite.hermes.database.model.UserEntity
import com.hootsuite.hermes.database.model.Users
import com.hootsuite.hermes.model.ReviewRequest
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
            SlackMessageHandler.missingUser(githubName, Config.SLACK_ADMIN_URL)
            null
        } else {
            val team = TeamEntity.find { Teams.teamName eq user.teamName }.firstOrNull()
            if (team == null) {
                SlackMessageHandler.missingTeam(
                    user.githubName,
                    user.slackName,
                    user.teamName,
                    Config.SLACK_ADMIN_URL
                )
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
    fun createOrUpdateUser(user: User) {
        // TODO Extract and make multiple transactions
        transaction {
            val existingUser = UserEntity.find { Users.githubName eq user.githubName }.firstOrNull()
            if (existingUser != null) {
                existingUser.slackName = formatSlackHandle(user.slackName)
                existingUser.teamName = user.teamName
                existingUser.avatarUrl = user.avatarUrl
                SlackMessageHandler.updateUser(
                    user.githubName,
                    user.slackName,
                    user.teamName,
                    user.avatarUrl,
                    Config.SLACK_ADMIN_URL
                )
            } else {
                UserEntity.new {
                    githubName = user.githubName
                    slackName = formatSlackHandle(user.slackName)
                    teamName = user.teamName
                    avatarUrl = user.avatarUrl
                }
                SlackMessageHandler.createUser(
                    user.githubName,
                    user.slackName,
                    user.teamName,
                    user.avatarUrl,
                    Config.SLACK_ADMIN_URL
                )
            }
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
                SlackMessageHandler.updateTeam(
                    team.teamName,
                    team.slackUrl,
                    Config.SLACK_ADMIN_URL
                )
            } else {
                TeamEntity.new {
                    teamName = team.teamName
                    slackUrl = team.slackUrl
                }
                SlackMessageHandler.createTeam(
                    team.teamName,
                    team.slackUrl,
                    Config.SLACK_ADMIN_URL
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

    /**
     * Delete a review request from the database
     * @param htmlUrl - The Url of the Pull Request to be deleted
     */
    fun deleteReviewRequest(htmlUrl: String) = transaction {
        ReviewRequestEntity.find { ReviewRequests.htmlUrl eq htmlUrl }.map { it.delete() }
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
     * Format a handle for tagging in Slack
     * TODO Should the handle formatting be handled here?
     * @param name - The slack name of the user
     * @return String - The slack formatted handle
     */
    private fun formatSlackHandle(name: String) = "@$name"
}