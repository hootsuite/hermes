package com.hootsuite.hermes.database.model

import org.jetbrains.exposed.dao.IntIdTable

/**
 * Users Table
 */
object Users : IntIdTable() {
    val githubName = varchar("githubName", 50).index()
    val slackName = varchar("slackName", 50)
    val teamName = varchar("teamName", 50)
    val avatarUrl = varchar("avatarUrl", 200).nullable()
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
 * Reviews Table
 */
object Reviews : IntIdTable() {
    val githubName = varchar("githubName", 50).index()
    val htmlUrl = varchar("htmlUrl", 150)
    val reviewState = varchar("reviewState", 50)
}
