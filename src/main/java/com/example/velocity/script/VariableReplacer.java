package com.example.velocity.script;

import com.example.velocity.script.variable.VariableManager;
import com.velocitypowered.api.proxy.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VariableReplacer {
    
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{[^}]+\\}");
    
    public static String replace(String text, Player executor, List<String> argNames, String[] argValues, 
                                  VariableManager variableManager, UUID scopeId) {
        String result = text;
        
        // Replace player expressions in braces: {player}, {player's name}, {player's uuid}, {player's server}
        // These MUST be in {} when inside strings, otherwise they are sent as literal text
        if (executor != null) {
            result = result.replace("{player}", executor.getUsername());
            result = result.replace("{player's name}", executor.getUsername());
            result = result.replace("{player's uuid}", executor.getUniqueId().toString());
            
            // Also support %player% and %uuid% for backwards compatibility and event contexts
            result = result.replace("%player%", executor.getUsername());
            result = result.replace("%uuid%", executor.getUniqueId().toString());
            
            // Replace player's server if available
            if (executor.getCurrentServer().isPresent()) {
                result = result.replace("{player's server}", 
                    executor.getCurrentServer().get().getServerInfo().getName());
            }
        }
        
        // Replace command arguments (%argname%, %arg-1%, etc.)
        if (argNames != null && argValues != null) {
            int minLength = Math.min(argNames.size(), argValues.length);
            for (int i = 0; i < minLength; i++) {
                String argName = argNames.get(i);
                String argValue = argValues[i];
                // Replace both %argname% and %arg-1%, %arg-2% style
                result = result.replace("%" + argName + "%", argValue);
                result = result.replace("%arg-" + (i + 1) + "%", argValue);
            }
        }
        
        // Replace script variables {var}, {_var}, {list::key}
        // This must come AFTER player expressions to avoid conflicts
        if (variableManager != null && scopeId != null) {
            Matcher matcher = VARIABLE_PATTERN.matcher(result);
            StringBuffer sb = new StringBuffer();
            
            while (matcher.find()) {
                String varName = matcher.group();
                String value = variableManager.getVariable(scopeId, varName);
                if (value != null) {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
                } else {
                    // Keep the variable placeholder if not set
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(varName));
                }
            }
            matcher.appendTail(sb);
            result = sb.toString();
        }
        
        return result;
    }
    
    // Overload for backward compatibility
    public static String replace(String text, Player executor, List<String> argNames, String[] argValues) {
        return replace(text, executor, argNames, argValues, null, null);
    }
    
    public static Map<String, String> getAvailableVariables(Player executor, List<String> argNames, String[] argValues) {
        Map<String, String> variables = new HashMap<>();
        
        // Add player expression variables (use {} syntax inside strings)
        if (executor != null) {
            variables.put("{player}", executor.getUsername());
            variables.put("{player's name}", executor.getUsername());
            variables.put("{player's uuid}", executor.getUniqueId().toString());
            
            // Also add %player% and %uuid% for backwards compatibility
            variables.put("%player%", executor.getUsername());
            variables.put("%uuid%", executor.getUniqueId().toString());
            
            // Add player's server if available
            if (executor.getCurrentServer().isPresent()) {
                variables.put("{player's server}", 
                    executor.getCurrentServer().get().getServerInfo().getName());
            }
        }
        
        // Add command arguments (%argname%, %arg-1%, etc.)
        if (argNames != null && argValues != null) {
            int minLength = Math.min(argNames.size(), argValues.length);
            for (int i = 0; i < minLength; i++) {
                String argName = argNames.get(i);
                String argValue = argValues[i];
                variables.put("%" + argName + "%", argValue);
                variables.put("%arg-" + (i + 1) + "%", argValue);
            }
        }
        
        return variables;
    }
}


