package com.example.velocity.script.event;

import com.example.velocity.script.Script;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an event trigger in a script (e.g., "on join:", "on quit:").
 */
public class EventTrigger {
    private final EventType eventType;
    private final List<Script.Action> actions;
    private final String scriptName;

    public EventTrigger(EventType eventType, String scriptName) {
        this.eventType = eventType;
        this.scriptName = scriptName;
        this.actions = new ArrayList<>();
    }

    public EventType getEventType() {
        return eventType;
    }

    public List<Script.Action> getActions() {
        return actions;
    }

    public void addAction(Script.Action action) {
        this.actions.add(action);
    }

    public String getScriptName() {
        return scriptName;
    }

    public enum EventType {
        JOIN,              // Player joins proxy (PostLoginEvent)
        QUIT,              // Player leaves proxy (DisconnectEvent)
        SERVER_SWITCH,     // Player switches servers (ServerPostConnectEvent)
        CHAT,              // Player sends chat message (PlayerChatEvent)
        SERVER_CONNECT     // Before player connects to server (ServerPreConnectEvent)
    }
}

