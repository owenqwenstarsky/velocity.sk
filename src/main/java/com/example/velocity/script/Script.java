package com.example.velocity.script;

import com.example.velocity.script.event.EventTrigger;
import com.example.velocity.script.expression.Condition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Script {
    private final String name;
    private final List<CommandScript> commands;
    private final List<EventTrigger> eventTriggers;

    public Script(String name) {
        this.name = name;
        this.commands = new ArrayList<>();
        this.eventTriggers = new ArrayList<>();
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

    public List<EventTrigger> getEventTriggers() {
        return eventTriggers;
    }

    public void addEventTrigger(EventTrigger trigger) {
        this.eventTriggers.add(trigger);
    }

    public static class CommandScript {
        private final String commandName;
        private final List<String> arguments;
        private final List<Action> actions;
        private String permission;
        private String permissionMessage;
        private List<String> aliases;
        private String usage;
        private String description;

        public CommandScript(String commandName, List<String> arguments) {
            this.commandName = commandName;
            this.arguments = arguments;
            this.actions = new ArrayList<>();
            this.aliases = new ArrayList<>();
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

        public String getPermission() {
            return permission;
        }

        public void setPermission(String permission) {
            this.permission = permission;
        }

        public String getPermissionMessage() {
            return permissionMessage;
        }

        public void setPermissionMessage(String permissionMessage) {
            this.permissionMessage = permissionMessage;
        }

        public List<String> getAliases() {
            return aliases;
        }

        public void setAliases(List<String> aliases) {
            this.aliases = aliases;
        }

        public String getUsage() {
            return usage;
        }

        public void setUsage(String usage) {
            this.usage = usage;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class Action {
        private final ActionType type;
        private final Map<String, String> data;

        public Action(ActionType type) {
            this.type = type;
            this.data = new HashMap<>();
        }

        public Action(ActionType type, Map<String, String> data) {
            this.type = type;
            this.data = new HashMap<>(data);
        }

        public ActionType getType() {
            return type;
        }

        public String getData(String key) {
            return data.get(key);
        }

        public void setData(String key, String value) {
            data.put(key, value);
        }

        public Map<String, String> getAllData() {
            return new HashMap<>(data);
        }

        // Convenience methods for backward compatibility
        public String getMessage() {
            return data.get("message");
        }

        public MessageTarget getTarget() {
            String target = data.get("target");
            if (target == null) return MessageTarget.PLAYER;
            return MessageTarget.valueOf(target);
        }

        public String getTargetPlayer() {
            return data.get("targetPlayer");
        }

        public String getServerName() {
            return data.get("serverName");
        }

        public String getPlayerExpr() {
            return data.get("playerExpr");
        }

        public String getVariableName() {
            return data.get("variableName");
        }

        public String getVariableValue() {
            return data.get("variableValue");
        }
    }

    public static class ConditionalAction extends Action {
        private final Condition condition;
        private final List<Action> ifActions;
        private final List<Action> elseActions;

        public ConditionalAction(Condition condition, List<Action> ifActions, List<Action> elseActions) {
            super(ActionType.CONDITIONAL);
            this.condition = condition;
            this.ifActions = ifActions;
            this.elseActions = elseActions;
        }

        public Condition getCondition() {
            return condition;
        }

        public List<Action> getIfActions() {
            return ifActions;
        }

        public List<Action> getElseActions() {
            return elseActions;
        }
    }

    public enum ActionType {
        SEND_MESSAGE,
        TRANSFER_PLAYER,
        SEND_TO_SERVER_PLAYERS,
        SET_VARIABLE,
        DELETE_VARIABLE,
        CONDITIONAL
    }

    public enum MessageTarget {
        PLAYER,
        ALL_PLAYERS,
        SPECIFIC_PLAYER,
        SERVER_PLAYERS
    }
}

