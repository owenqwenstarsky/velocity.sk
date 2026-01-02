package com.example.velocity.script.variable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages local variable scopes for script executions.
 * Each execution context gets a unique ID and its own local variable space.
 */
public class VariableRegistry {
    private final Map<UUID, Map<String, String>> localScopes;

    public VariableRegistry() {
        this.localScopes = new ConcurrentHashMap<>();
    }

    /**
     * Creates a new local variable scope and returns its ID.
     */
    public UUID createScope() {
        UUID scopeId = UUID.randomUUID();
        localScopes.put(scopeId, new HashMap<>());
        return scopeId;
    }

    /**
     * Sets a local variable in a specific scope.
     */
    public void setLocal(UUID scopeId, String name, String value) {
        Map<String, String> scope = localScopes.get(scopeId);
        if (scope != null) {
            scope.put(name, value);
        }
    }

    /**
     * Gets a local variable from a specific scope.
     */
    public String getLocal(UUID scopeId, String name) {
        Map<String, String> scope = localScopes.get(scopeId);
        if (scope != null) {
            return scope.get(name);
        }
        return null;
    }

    /**
     * Checks if a local variable exists in a specific scope.
     */
    public boolean hasLocal(UUID scopeId, String name) {
        Map<String, String> scope = localScopes.get(scopeId);
        return scope != null && scope.containsKey(name);
    }

    /**
     * Deletes a local variable from a specific scope.
     */
    public void deleteLocal(UUID scopeId, String name) {
        Map<String, String> scope = localScopes.get(scopeId);
        if (scope != null) {
            scope.remove(name);
        }
    }

    /**
     * Destroys a scope and all its local variables.
     */
    public void destroyScope(UUID scopeId) {
        localScopes.remove(scopeId);
    }

    /**
     * Gets the count of active scopes.
     */
    public int getScopeCount() {
        return localScopes.size();
    }
}

