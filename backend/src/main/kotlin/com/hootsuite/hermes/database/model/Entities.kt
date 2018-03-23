package com.hootsuite.hermes.database.model

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass

/**
 * Hermes User DAO
 */
class UserEntity(id: EntityID<Int>) : IntEntity(id) {
    var githubName by Users.githubName
    var slackName by Users.slackName
    var teamName by Users.teamName
    var avatarUrl by Users.avatarUrl

    /**
     * Format for viewing as a single String
     * TODO Fix This
     */
    override fun toString() = "$githubName : $slackName : $teamName : ${avatarUrl ?: "No Avatar"}"

    companion object : IntEntityClass<UserEntity>(Users)
}

/**
 * Hermes Team DAO
 */
class TeamEntity(id: EntityID<Int>) : IntEntity(id) {

    var teamName by Teams.teamName
    var slackUrl by Teams.slackUrl

    /**
     * Format for viewing as a single String
     * TODO Fix This
     */
    override fun toString() = "$teamName : $slackUrl"

    companion object : IntEntityClass<TeamEntity>(Teams)
}

/**
 * Hermes ReviewRequests DAO
 */
class ReviewRequestEntity(id: EntityID<Int>) : IntEntity(id) {

    var htmlUrl by ReviewRequests.htmlUrl
    var githubName by ReviewRequests.githubName

    /**
     * Format for viewing as a single String
     * TODO Fix This
     */
    override fun toString() = "$htmlUrl : $githubName"

    companion object : IntEntityClass<ReviewRequestEntity>(ReviewRequests)
}