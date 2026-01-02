package com.example.velocity.script.event;

import com.example.velocity.script.Script;
import com.example.velocity.script.execution.ActionExecutor;
import com.example.velocity.script.execution.ExecutionContext;
import com.example.velocity.script.variable.VariableManager;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.util.*;

/**
 * Manages event triggers from scripts and routes Velocity events to them.
 */
public class EventManager {
    private final ProxyServer server;
    private final Logger logger;
    private final VariableManager variableManager;
    private final ActionExecutor actionExecutor;
    private final Map<EventTrigger.EventType, List<EventTrigger>> eventTriggers;

    public EventManager(ProxyServer server, Logger logger, VariableManager variableManager) {
        this.server = server;
        this.logger = logger;
        this.variableManager = variableManager;
        this.actionExecutor = new ActionExecutor(server, logger);
        this.eventTriggers = new EnumMap<>(EventTrigger.EventType.class);
        
        // Initialize empty lists for each event type
        for (EventTrigger.EventType type : EventTrigger.EventType.values()) {
            eventTriggers.put(type, new ArrayList<>());
        }
    }

    /**
     * Registers all event triggers from loaded scripts.
     */
    public void registerScripts(List<Script> scripts) {
        // Clear previous triggers
        for (List<EventTrigger> triggers : eventTriggers.values()) {
            triggers.clear();
        }

        // Register new triggers
        for (Script script : scripts) {
            for (EventTrigger trigger : script.getEventTriggers()) {
                eventTriggers.get(trigger.getEventType()).add(trigger);
                logger.debug("Registered {} trigger from script: {}", 
                            trigger.getEventType(), script.getName());
            }
        }

        int totalTriggers = eventTriggers.values().stream().mapToInt(List::size).sum();
        logger.info("Registered {} event trigger(s) from scripts", totalTriggers);
    }

    /**
     * Executes all triggers for a specific event type.
     */
    private void executeTriggers(EventTrigger.EventType eventType, ExecutionContext context) {
        List<EventTrigger> triggers = eventTriggers.get(eventType);
        if (triggers == null || triggers.isEmpty()) {
            return;
        }

        for (EventTrigger trigger : triggers) {
            try {
                actionExecutor.executeActions(trigger.getActions(), context);
            } catch (Exception e) {
                logger.error("Error executing {} trigger from script {}", 
                            eventType, trigger.getScriptName(), e);
            }
        }
    }

    // Velocity Event Listeners

    @Subscribe(order = PostOrder.NORMAL)
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        UUID scopeId = variableManager.createScope();

        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("player", player.getUsername());
            eventData.put("uuid", player.getUniqueId().toString());

            ExecutionContext context = new ExecutionContext.Builder()
                .server(server)
                .player(player)
                .variableManager(variableManager)
                .scopeId(scopeId)
                .eventData(eventData)
                .build();

            executeTriggers(EventTrigger.EventType.JOIN, context);
        } finally {
            variableManager.destroyScope(scopeId);
        }
    }

    @Subscribe(order = PostOrder.NORMAL)
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        UUID scopeId = variableManager.createScope();

        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("player", player.getUsername());
            eventData.put("uuid", player.getUniqueId().toString());
            
            // Note: DisconnectEvent doesn't provide a quit message in Velocity
            eventData.put("quit-message", "");

            ExecutionContext context = new ExecutionContext.Builder()
                .server(server)
                .player(player)
                .variableManager(variableManager)
                .scopeId(scopeId)
                .eventData(eventData)
                .build();

            executeTriggers(EventTrigger.EventType.QUIT, context);
        } finally {
            variableManager.destroyScope(scopeId);
        }
    }

    @Subscribe(order = PostOrder.NORMAL)
    public void onServerSwitch(ServerPostConnectEvent event) {
        Player player = event.getPlayer();
        UUID scopeId = variableManager.createScope();

        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("player", player.getUsername());
            eventData.put("uuid", player.getUniqueId().toString());
            
            String fromServer = event.getPreviousServer() != null ? 
                event.getPreviousServer().getServerInfo().getName() : "none";
            String toServer = player.getCurrentServer().isPresent() ?
                player.getCurrentServer().get().getServerInfo().getName() : "unknown";
            
            eventData.put("from-server", fromServer);
            eventData.put("to-server", toServer);

            ExecutionContext context = new ExecutionContext.Builder()
                .server(server)
                .player(player)
                .variableManager(variableManager)
                .scopeId(scopeId)
                .eventData(eventData)
                .build();

            executeTriggers(EventTrigger.EventType.SERVER_SWITCH, context);
        } finally {
            variableManager.destroyScope(scopeId);
        }
    }

    @Subscribe(order = PostOrder.NORMAL)
    public void onChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID scopeId = variableManager.createScope();

        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("player", player.getUsername());
            eventData.put("uuid", player.getUniqueId().toString());
            eventData.put("message", event.getMessage());

            ExecutionContext context = new ExecutionContext.Builder()
                .server(server)
                .player(player)
                .variableManager(variableManager)
                .scopeId(scopeId)
                .eventData(eventData)
                .build();

            executeTriggers(EventTrigger.EventType.CHAT, context);
        } finally {
            variableManager.destroyScope(scopeId);
        }
    }

    @Subscribe(order = PostOrder.NORMAL)
    public void onServerConnect(ServerPreConnectEvent event) {
        Player player = event.getPlayer();
        UUID scopeId = variableManager.createScope();

        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("player", player.getUsername());
            eventData.put("uuid", player.getUniqueId().toString());
            
            String targetServer = event.getResult().getServer().isPresent() ?
                event.getResult().getServer().get().getServerInfo().getName() : "unknown";
            
            eventData.put("target-server", targetServer);

            ExecutionContext context = new ExecutionContext.Builder()
                .server(server)
                .player(player)
                .variableManager(variableManager)
                .scopeId(scopeId)
                .eventData(eventData)
                .build();

            executeTriggers(EventTrigger.EventType.SERVER_CONNECT, context);
        } finally {
            variableManager.destroyScope(scopeId);
        }
    }

    /**
     * Unregisters all event triggers.
     */
    public void unregisterAll() {
        for (List<EventTrigger> triggers : eventTriggers.values()) {
            triggers.clear();
        }
        logger.info("Unregistered all event triggers");
    }

    /**
     * Gets statistics about registered events.
     */
    public String getStats() {
        StringBuilder sb = new StringBuilder();
        for (EventTrigger.EventType type : EventTrigger.EventType.values()) {
            int count = eventTriggers.get(type).size();
            if (count > 0) {
                sb.append(type.name()).append(": ").append(count).append(" ");
            }
        }
        return sb.toString().trim();
    }
}

