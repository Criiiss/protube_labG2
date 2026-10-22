package com.tecnocampus.LS2.protube_back.profile.application.command;

/**
 * Command to change the user's username
 * @param username The new username
 */
public record ChangeUserNameCommand(String username) {
}
