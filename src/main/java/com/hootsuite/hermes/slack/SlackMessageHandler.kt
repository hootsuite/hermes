package com.hootsuite.hermes.slack

import com.github.kittinunf.fuel.Fuel
import com.google.gson.Gson
import com.hootsuite.hermes.slack.models.SlackParams
import com.hootsuite.hermes.slack.models.SlackUser

/**
 * Object to handle sending various messages to Slack
 * TODO Organize the methods
 */
object SlackMessageHandler {

    /**
     * Send messages to slack with given params
     * @param params - The parameters of the message to Send to Slack
     */
    private fun sendToSlack(url: String, params: SlackParams) {
        Fuel
                .post(url)
                .body(Gson().toJson(params))
                .response { _, response, result ->
                    println(response)
                    println(result)
                    //TODO Handle Response and Result
                }
    }

    // User Messages

    /**
     * Send a Pull Request Approval Message to Slack
     * @param reviewer - The Reviewer of the Pull Request
     * @param author - The Author of the Pull Request
     * @param url - The http URL of the Pull Request
     */
    fun approval(reviewer: String, author: SlackUser, url: String) {
        val params = SlackParams.approval(reviewer, author.name, url, author.avatarUrl)
        sendToSlack(author.slackUrl, params)
    }

    /**
     * Send a Pull Request Request Changes Message to Slack
     * @param reviewer - The Reviewer of the Pull Request
     * @param author - The Author of the Pull Request
     * @param url - The http URL of the Pull Request
     * @param comment - Review
     */
    fun requestChanges(reviewer: String, author: SlackUser, url: String, comment: String) {
        val params = SlackParams.requestChanges(reviewer, author.name, url, comment, author.avatarUrl)
        sendToSlack(author.slackUrl, params)
    }

    /**
     * Send a Pull Request Review Request Message to Slack
     * @param reviewer - The Reviewer of the Pull Request
     * @param author - The Author of the Pull Request
     * @param url - The http URL of the Pull Request
     * @param title - The title of the Pull Request
     */
    fun requestReviewer(reviewer: SlackUser, author: String, url: String, title: String) {
        val params = SlackParams.requestReviewer(reviewer.name, author, url, title, reviewer.avatarUrl)
        sendToSlack(reviewer.slackUrl, params)
    }

    /**
     * Send a Pull Request Updated Review Request Message to Slack
     * @param reviewer - The Reviewer of the Pull Request
     * @param author - The Author of the Pull Request
     * @param url - The http URL of the Pull Request
     */
    fun rerequestReviewer(reviewer: SlackUser, author: String, url: String) {
        val params = SlackParams.rerequestReviewer(reviewer.name, author, url, reviewer.avatarUrl)
        sendToSlack(reviewer.slackUrl, params)
    }

    /**
     * Send the author of a failing commit a message on slack
     * @param author - The author of the Pull Request with a failing commit
     * @param targetUrl - The targetUrl from the failing status
     * @param repoName - The full repo name (Org and Repo) with the failing commit
     * @param commitUrl - The Html URL of the failing commit
     */
    fun buildFailure(author: SlackUser, targetUrl: String, repoName: String, commitUrl: String) {
        val params = SlackParams.buildFailure(author.name, targetUrl, repoName, commitUrl, author.avatarUrl)
        sendToSlack(author.slackUrl, params)
    }

    // Admin Messages

    /**
     * Send a message to slack after the creation of a Hermes User
     * @param githubName - The github login of the User
     * @param slackName - The slack name of the User
     * @param teamName - The Hermes team name of the user
     * @param adminUrl - The slack URL to send the status message to
     * @param avatarUrl - The Url of the avatar of the user or null if there is no avatar
     */
    fun createUser(githubName: String, slackName: String, teamName: String, avatarUrl: String?, adminUrl: String) {
        val params = SlackParams.createUser(githubName, slackName, teamName, avatarUrl)
        sendToSlack(adminUrl, params)
    }

    /**
     * Send a message to slack after a Hermes User is Updated
     * @param githubName - The github login of the User
     * @param slackName - The slack name of the User
     * @param teamName - The Hermes team name of the user
     * @param adminUrl - The slack URL to send the status message to
     * @param avatarUrl - The Url of the avatar of the user or null if there is no avatar
     */
    fun updateUser(githubName: String, slackName: String, teamName: String, avatarUrl: String?, adminUrl: String) {
        val params = SlackParams.updateUser(githubName, slackName, teamName, avatarUrl)
        sendToSlack(adminUrl, params)
    }

    /**
     * Send a message to slack after the creation of a Hermes Team
     * @param teamName - The Hermes team name of the user
     * @param slackUrl - The slack webhook url of the team
     * @param adminUrl - The slack URL to send the status message to
     */
    fun createTeam(teamName: String, slackUrl: String, adminUrl: String) {
        val params = SlackParams.createTeam(teamName, slackUrl)
        sendToSlack(adminUrl, params)
    }

    /**
     * Send a message to slack after a Hermes Team is updated
     * @param teamName - The Hermes team name of the user
     * @param slackUrl - The slack webhook url of the team
     * @param adminUrl - The slack URL to send the status message to
     */
    fun updateTeam(teamName: String, slackUrl: String, adminUrl: String) {
        val params = SlackParams.updateTeam(teamName, slackUrl)
        sendToSlack(adminUrl, params)
    }

    /**
     * Send a message to slack when a user who is tagged in a Pull Request is missing from the db
     * @param githubName - The Github name of the missing user
     * @param adminUrl - The slack URL to send the status message to
     */
    fun missingUser(githubName: String, adminUrl: String) {
        val params = SlackParams.missingUser(githubName)
        sendToSlack(adminUrl, params)
    }

    /**
     * Send a message to slack when a team of a user who is tagged in a Pull Request is missing from the db
     * @param githubName - The Github name of the user
     * @param slackName - The slack name of the user
     * @param teamName - The Hermes team name which is missing
     * @param adminUrl - The slack URL to send the status message to
     */
    fun missingTeam(githubName: String, slackName: String, teamName: String, adminUrl: String) {
        val params = SlackParams.missingTeam(githubName, slackName, teamName)
        sendToSlack(adminUrl, params)
    }

    /**
     * Send a message to slack when an event is sent to Hermes which is not handled. This indicates that the webhook
     * on github is registered to more events than it should be.
     * @param eventName - The name of the Unhandled Github event
     * @param adminUrl - The slack RUL to send the status message to
     */
    fun unhandledEvent(eventName: String, adminUrl: String) {
        val params = SlackParams.unhandledEvent(eventName)
        sendToSlack(adminUrl, params)
    }

    /**
     * Send a message to slack when a ping event is received. Happens when a new webhook is registered
     * @param zen - Github's short zen message.
     * @param missingEvents - The list of events that should be registered but were not
     * @param extraEvents - The list of events that were registred but should not be
     * @param repoName - The full name of the repository where the webhook was registered
     * @param sender - The person who configured the webhook
     * @param adminUrl - The Admin URL to send the message to
     */
    fun ping(zen: String,
             missingEvents: List<String>,
             extraEvents: List<String>,
             repoName: String,
             sender: String,
             adminUrl: String) {
        val params = SlackParams.ping(zen, missingEvents, extraEvents, repoName, sender)
        sendToSlack(adminUrl, params)
    }
}
