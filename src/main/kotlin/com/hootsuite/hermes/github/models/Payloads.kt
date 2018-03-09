package com.hootsuite.hermes.github.models

import com.google.gson.annotations.SerializedName
import java.util.Arrays

/**
 * Github Comment Model Object
 */
data class Comment(
    val body: String
)

/**
 * Github Commit Model Object
 */
data class Commit(
    @SerializedName("html_url")
    val htmlUrl: String,
    val author: User
)

/**
 * Github Issue Model Object
 */
data class Issue(
    @SerializedName("html_url")
    val htmlUrl: String,
    val user: User
)

/**
 * Github Pull Request Model Object
 */
data class PullRequest(
    @SerializedName("html_url")
    val htmlUrl: String,
    val user: User,
    val title: String,
    val id: Int
)

data class Repository(
    @SerializedName("full_name")
    val fullName: String
)

/**
 * Github Pull Request Review Model Object
 */
data class Review(
    val user: User,
    val state: ApprovalState,
    val body: String
)

/**
 * Github User Model Object
 */
data class User(
    val login: String
)

/**
 * Github Webhook Model Object
 */
data class Webhook(
    val name: String,
    val events: Array<String>
) {
    /**
     * Generated equals Method
     * @param other - The object to compare to
     * @return Boolean - True if the objects are equal, false otherwise
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Webhook

        if (name != other.name) return false
        if (!Arrays.equals(events, other.events)) return false

        return true
    }

    /**
     * Generated hashCode Method
     * @return Int - Hashcode of the object
     */
    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + Arrays.hashCode(events)
        return result
    }
}

/**
 * Possible States for Pull Request Approval Status
 */
enum class ApprovalState {
    @SerializedName("approved")
    APPROVED,
    @SerializedName("changes_requested")
    CHANGES_REQUESTED,
    @SerializedName("commented")
    COMMENTED
}


