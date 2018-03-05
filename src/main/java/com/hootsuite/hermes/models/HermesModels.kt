package com.hootsuite.hermes.models

/**
 * DTO Object for a Hermes User
 */
data class UserDTO(
        val githubName: String,
        val slackName: String,
        val teamName: String,
        val avatarUrl: String? = null
)

/**
 * DTO Object for a Hermes Team
 */
data class TeamDTO(
        val teamName: String,
        val slackUrl: String
)

/**
 * DTO Object for a Hermes Review Request
 */
data class ReviewRequestDTO(
        var htmlUrl: String,
        var githubName: String
)