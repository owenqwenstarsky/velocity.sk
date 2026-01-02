package com.example.velocity.script.execution;

import com.example.velocity.script.variable.VariableManager;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.*;

/**
 * Holds all runtime information needed for script execution.
 * This includes the player, arguments, local variable scope, event data, etc.
 */
public class ExecutionContext {
    private final ProxyServer server;
    private final Player player;
    private final List<String> argumentNames;
    private final String[] argumentValues;
    private final VariableManager variableManager;
    private final UUID scopeId;
    private final Map<String, Object> eventData;

    private ExecutionContext(Builder builder) {
        this.server = builder.server;
        this.player = builder.player;
        this.argumentNames = builder.argumentNames;
        this.argumentValues = builder.argumentValues;
        this.variableManager = builder.variableManager;
        this.scopeId = builder.scopeId;
        this.eventData = builder.eventData;
    }

    public ProxyServer getServer() {
        return server;
    }

    public Player getPlayer() {
        return player;
    }

    public List<String> getArgumentNames() {
        return argumentNames;
    }

    public String[] getArgumentValues() {
        return argumentValues;
    }

    public String getArgument(int index) {
        if (argumentValues != null && index >= 0 && index < argumentValues.length) {
            return argumentValues[index];
        }
        return null;
    }

    public String getArgument(String name) {
        if (argumentNames != null && argumentValues != null) {
            int index = argumentNames.indexOf(name);
            if (index >= 0 && index < argumentValues.length) {
                return argumentValues[index];
            }
        }
        return null;
    }

    public VariableManager getVariableManager() {
        return variableManager;
    }

    public UUID getScopeId() {
        return scopeId;
    }

    public Map<String, Object> getEventData() {
        return eventData;
    }

    public Object getEventValue(String key) {
        return eventData != null ? eventData.get(key) : null;
    }

    public Optional<RegisteredServer> getPlayerServer() {
        if (player != null && player.getCurrentServer().isPresent()) {
            return Optional.of(player.getCurrentServer().get().getServer());
        }
        return Optional.empty();
    }

    public static class Builder {
        private ProxyServer server;
        private Player player;
        private List<String> argumentNames;
        private String[] argumentValues;
        private VariableManager variableManager;
        private UUID scopeId;
        private Map<String, Object> eventData;

        public Builder server(ProxyServer server) {
            this.server = server;
            return this;
        }

        public Builder player(Player player) {
            this.player = player;
            return this;
        }

        public Builder arguments(List<String> names, String[] values) {
            this.argumentNames = names;
            this.argumentValues = values;
            return this;
        }

        public Builder variableManager(VariableManager variableManager) {
            this.variableManager = variableManager;
            return this;
        }

        public Builder scopeId(UUID scopeId) {
            this.scopeId = scopeId;
            return this;
        }

        public Builder eventData(Map<String, Object> eventData) {
            this.eventData = eventData;
            return this;
        }

        public ExecutionContext build() {
            return new ExecutionContext(this);
        }
    }
}

