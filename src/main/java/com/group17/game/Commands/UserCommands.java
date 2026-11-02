package com.group17.game.Commands;

import org.springframework.shell.core.command.annotation.Command;
import org.springframework.stereotype.Component;

import com.group17.game.Utilities.AccessService;

@Component
public class UserCommands {
    private final AccessService accessService;

    public UserCommands(AccessService accessService) {
        this.accessService = accessService;
    }

    @Command(name = "user-upgrade", description = "Change a user's role")
    public String userUpgrade(long id, String role) {
        String result = this.accessService.upgrade(id, role);
        return result;
    }
}
