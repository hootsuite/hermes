package com.hootsuite.hermes.model

/**
 * DTO Object for a Hermes User
 */
data class User(
    val githubName: String,
    val slackName: String,
    val teamName: String,
    val avatarUrl: String? = null
)

/**
 * DTO Object for a Hermes Team
 */
data class Team(
    val teamName: String,
    val slackUrl: String
)

/**
 * DTO Object for a Hermes Review Request
 */
data class ReviewRequest(
    var htmlUrl: String,
    var githubName: String
)