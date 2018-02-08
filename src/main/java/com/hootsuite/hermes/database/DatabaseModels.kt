package com.hootsuite.hermes.database

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

/**
 * Users Table
 */
object Users : IntIdTable() {
    val githubName = varchar("githubName", 50).index()
    val slackName = varchar("slackName", 50)
    val teamName = varchar("teamName", 50)
}

/**
 * Teams Table
 */
object Teams : IntIdTable() {
    val teamName = varchar("teamName", 50).index()
    val slackUrl = varchar("slackUrl", 150)
}

/**
 * ReviewRequests Table
 */
object ReviewRequests : IntIdTable() {
    // TODO Key on something better than html url, issue id and pr id are different
    val htmlUrl = varchar("htmlUrl", 150).index()
    val githubName = varchar("githubName", 50)
}

/**
 * Hermes User DAO
 */
class User(id: EntityID<Int>) : IntEntity(id) {
    var githubName by Users.githubName
    var slackName by Users.slackName
    var teamName by Users.teamName

    /**
     * Format for viewing as a single String
     * TODO Fix This
     */
    override fun toString() = "$githubName : $slackName : $teamName"

    companion object : IntEntityClass<User>(Users)
}

/**
 * Hermes Team DAO
 */
class Team(id: EntityID<Int>) : IntEntity(id) {

    var teamName by Teams.teamName
    var slackUrl by Teams.slackUrl

    /**
     * Format for viewing as a single String
     * TODO Fix This
     */
    override fun toString() = "$teamName : $slackUrl"

    companion object : IntEntityClass<Team>(Teams)
}

/**
 * Hermes ReviewRequests DAO
 */
class ReviewRequest(id: EntityID<Int>) : IntEntity(id) {

    var htmlUrl by ReviewRequests.htmlUrl
    var githubName by ReviewRequests.githubName

    /**
     * Format for viewing as a single String
     * TODO Fix This
     */
    override fun toString() = "$htmlUrl : $githubName"

    companion object : IntEntityClass<ReviewRequest>(ReviewRequests)
}
