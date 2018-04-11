package com.hootsuite.hermes.slack

import com.hootsuite.hermes.database.DatabaseUtils
import com.hootsuite.hermes.model.User
import com.hootsuite.hermes.slack.model.SlashCommand

/**
 * Object to parse the incoming slash commands from slack
 */
object SlashCommandHandler {

    /**
     * Handle an incoming Slash Command from slack and return a response String to send to the user
     * @param slashCommand - The incoming Slash Command parsed from slack
     * @param controlCommand - The command that the user is trying to invoke (register, avatar, etc.)
     * @param parameters - The arguments that the user is passing to the command (their username, avatar url, etc.)
     * @return String - A response string to send back to slack.
     */
    fun handleSlashCommand(slashCommand: SlashCommand, controlCommand: String?, parameters: List<String>): String =
        when (controlCommand) {
            SlashCommand.REGISTER -> {
                val team = if (slashCommand.channel == DIRECT_MESSAGE) {
                    "@${slashCommand.username}"
                } else {
                    "#${slashCommand.channel}"
                }
                if (parameters.size == 1) {
                    DatabaseUtils.createOrUpdateUser(User(parameters.first(), slashCommand.username, team))
                    if (DatabaseUtils.getTeamOrNull(team) != null) {
                        "You have successfully registered to $team"
                    } else {
                        "$team is not registered, please register that team with Hermes."
                    }
                } else {
                    REGISTER_HELP
                }
            }
            SlashCommand.AVATAR -> {
                if (parameters.size == 1) {
                    DatabaseUtils.updateAvatar(slashCommand.username, parameters[0])
                    AVATAR_UPDATED
                } else {
                    AVATAR_HELP
                }
            }
            else -> {
                HELP_TEXT
            }
        }

    private val HELP_TEXT = """Please use one of the following slash commands:
        ```/hermes register <your github username>
        /hermes avatar <your chosen avatar URL>```""".trimIndent()

    private const val REGISTER_HELP = "Register with your github username: `/hermes register <your github username>`"

    private const val AVATAR_UPDATED = "Your Avatar has been updated."
    private const val AVATAR_HELP = "Update your avatar with a URL: `/hermes avatar <your chosen avatar URL>`"

    private const val DIRECT_MESSAGE = "directmessage"
}