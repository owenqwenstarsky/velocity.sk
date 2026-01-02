package com.example.velocity.script.variable;

import org.slf4j.Logger;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all variables: global (persistent), local (temporary), and list variables.
 * Global variables are stored in SQLite and cached in memory.
 * Local variables are stored only in memory per execution scope.
 */
public class VariableManager {
    private final Logger logger;
    private final VariableStorage storage;
    private final VariableRegistry registry;
    private final Map<String, String> globalCache;

    public VariableManager(Logger logger, VariableStorage storage) {
        this.logger = logger;
        this.storage = storage;
        this.registry = new VariableRegistry();
        this.globalCache = new ConcurrentHashMap<>();
    }

    /**
     * Loads all global variables from storage into cache.
     */
    public void loadGlobalVariables() {
        try {
            Map<String, String> variables = storage.loadAllVariables();
            globalCache.clear();
            globalCache.putAll(variables);
            logger.info("Loaded {} global variable(s) from storage", variables.size());
        } catch (SQLException e) {
            logger.error("Failed to load global variables from storage", e);
        }
    }

    /**
     * Creates a new execution scope for local variables.
     */
    public UUID createScope() {
        return registry.createScope();
    }

    /**
     * Destroys an execution scope and cleans up its local variables.
     */
    public void destroyScope(UUID scopeId) {
        registry.destroyScope(scopeId);
    }

    /**
     * Sets a variable (global or local depending on the name).
     * Global variables: {name}
     * Local variables: {_name}
     * List variables: {list::key} or {_list::key}
     */
    public void setVariable(UUID scopeId, String name, String value) {
        if (name.startsWith("{_")) {
            // Local variable
            String cleanName = name.substring(1, name.length() - 1); // Remove { and }
            registry.setLocal(scopeId, cleanName, value);
        } else if (name.startsWith("{")) {
            // Global variable
            String cleanName = name.substring(1, name.length() - 1); // Remove { and }
            globalCache.put(cleanName, value);
            
            // Persist to database
            try {
                storage.saveVariable(cleanName, value, "string");
            } catch (SQLException e) {
                logger.error("Failed to save global variable '{}' to storage", cleanName, e);
            }
        }
    }

    /**
     * Gets a variable value (global or local depending on the name).
     */
    public String getVariable(UUID scopeId, String name) {
        if (name.startsWith("{_")) {
            // Local variable
            String cleanName = name.substring(1, name.length() - 1);
            return registry.getLocal(scopeId, cleanName);
        } else if (name.startsWith("{")) {
            // Global variable
            String cleanName = name.substring(1, name.length() - 1);
            return globalCache.get(cleanName);
        }
        return null;
    }

    /**
     * Checks if a variable exists and is set.
     */
    public boolean isSet(UUID scopeId, String name) {
        if (name.startsWith("{_")) {
            // Local variable
            String cleanName = name.substring(1, name.length() - 1);
            return registry.hasLocal(scopeId, cleanName);
        } else if (name.startsWith("{")) {
            // Global variable
            String cleanName = name.substring(1, name.length() - 1);
            return globalCache.containsKey(cleanName);
        }
        return false;
    }

    /**
     * Deletes a variable.
     */
    public void deleteVariable(UUID scopeId, String name) {
        if (name.startsWith("{_")) {
            // Local variable
            String cleanName = name.substring(1, name.length() - 1);
            registry.deleteLocal(scopeId, cleanName);
        } else if (name.startsWith("{")) {
            // Global variable
            String cleanName = name.substring(1, name.length() - 1);
            globalCache.remove(cleanName);
            
            // Delete from database
            try {
                storage.deleteVariable(cleanName);
            } catch (SQLException e) {
                logger.error("Failed to delete global variable '{}' from storage", cleanName, e);
            }
        }
    }

    /**
     * Gets all list entries for a list variable.
     * Example: {list::*} returns all entries matching {list::...}
     */
    public Map<String, String> getListEntries(UUID scopeId, String listName) {
        Map<String, String> entries = new HashMap<>();
        
        // Remove {, }, and ::*
        String prefix = listName.replace("{", "").replace("}", "").replace("::*", "");
        
        if (listName.startsWith("{_")) {
            // Local list - not implemented in registry yet, would need extension
            logger.warn("Local list iteration not yet fully supported");
        } else {
            // Global list
            for (Map.Entry<String, String> entry : globalCache.entrySet()) {
                if (entry.getKey().startsWith(prefix + "::")) {
                    entries.put(entry.getKey(), entry.getValue());
                }
            }
        }
        
        return entries;
    }

    /**
     * Deletes all entries in a list variable.
     */
    public void deleteList(UUID scopeId, String listName) {
        String prefix = listName.replace("{", "").replace("}", "").replace("::*", "");
        
        if (listName.startsWith("{_")) {
            // Local list
            logger.warn("Local list deletion not yet fully supported");
        } else {
            // Global list
            try {
                storage.deleteVariablesByPrefix(prefix + "::");
                globalCache.keySet().removeIf(key -> key.startsWith(prefix + "::"));
            } catch (SQLException e) {
                logger.error("Failed to delete list '{}' from storage", listName, e);
            }
        }
    }

    /**
     * Gets statistics about variables.
     */
    public String getStats() {
        try {
            int dbCount = storage.getVariableCount();
            int cacheCount = globalCache.size();
            int scopeCount = registry.getScopeCount();
            return String.format("Global: %d (DB: %d, Cache: %d), Active Scopes: %d", 
                                 cacheCount, dbCount, cacheCount, scopeCount);
        } catch (SQLException e) {
            return "Error getting stats: " + e.getMessage();
        }
    }
}

