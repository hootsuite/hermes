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
    val htmlUrl: String,
    val githubName: String
)

/**
 * DTO Object for a Hermes Review
 */
class Review private constructor(
    val githubName: String,
    val htmlUrl: String,
    val reviewState: ReviewState
) {
    companion object {
        /**
         * Builder method for Approved Review
         * @param name - The Github name of the reviewer
         * @param htmlUrl - The htmlUrl of the Pull Request
         */
        fun approved(name: String, htmlUrl: String) = Review(name, htmlUrl, ReviewState.APPROVED)

        /**
         * Builder method for Commented Review
         * @param name - The Github name of the reviewer
         * @param htmlUrl - The htmlUrl of the Pull Request
         */
        fun commented(name: String, htmlUrl: String) = Review(name, htmlUrl, ReviewState.COMMENTED)

        /**
         * Builder method for Changes Requested Review
         * @param name - The Github name of the reviewer
         * @param htmlUrl - The htmlUrl of the Pull Request
         */
        fun changesRequested(name: String, htmlUrl: String) = Review(name, htmlUrl, ReviewState.CHANGES_REQUESTED)
    }
}

enum class ReviewState {
    APPROVED,
    COMMENTED,
    CHANGES_REQUESTED
}