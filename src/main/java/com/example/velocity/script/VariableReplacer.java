package com.example.velocity.script;

import com.velocitypowered.api.proxy.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VariableReplacer {
    
    public static String replace(String text, Player executor, List<String> argNames, String[] argValues) {
        String result = text;
        
        // Replace default variables
        result = result.replace("%player%", executor.getUsername());
        result = result.replace("%uuid%", executor.getUniqueId().toString());
        
        // Replace command arguments
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
        
        return result;
    }
    
    public static Map<String, String> getAvailableVariables(Player executor, List<String> argNames, String[] argValues) {
        Map<String, String> variables = new HashMap<>();
        
        // Add default variables
        variables.put("%player%", executor.getUsername());
        variables.put("%uuid%", executor.getUniqueId().toString());
        
        // Add command arguments
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


