package com.example.velocity.script;

import java.util.ArrayList;
import java.util.List;

public class Script {
    private final String name;
    private final List<CommandScript> commands;

    public Script(String name) {
        this.name = name;
        this.commands = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public List<CommandScript> getCommands() {
        return commands;
    }

    public void addCommand(CommandScript command) {
        this.commands.add(command);
    }

    public static class CommandScript {
        private final String commandName;
        private final List<String> arguments;
        private final List<Action> actions;

        public CommandScript(String commandName, List<String> arguments) {
            this.commandName = commandName;
            this.arguments = arguments;
            this.actions = new ArrayList<>();
        }

        public String getCommandName() {
            return commandName;
        }

        public List<String> getArguments() {
            return arguments;
        }

        public List<Action> getActions() {
            return actions;
        }

        public void addAction(Action action) {
            this.actions.add(action);
        }
    }

    public static class Action {
        private final ActionType type;
        private final String message;
        private final MessageTarget target;
        private final String targetPlayer;

        public Action(ActionType type, String message, MessageTarget target, String targetPlayer) {
            this.type = type;
            this.message = message;
            this.target = target;
            this.targetPlayer = targetPlayer;
        }

        public ActionType getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }

        public MessageTarget getTarget() {
            return target;
        }

        public String getTargetPlayer() {
            return targetPlayer;
        }
    }

    public enum ActionType {
        SEND_MESSAGE
    }

    public enum MessageTarget {
        PLAYER,
        ALL_PLAYERS,
        SPECIFIC_PLAYER
    }
}

