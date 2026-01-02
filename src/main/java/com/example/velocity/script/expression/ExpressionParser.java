package com.example.velocity.script.expression;

import com.example.velocity.script.execution.ExecutionContext;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and creates Expression objects from text.
 */
public class ExpressionParser {
    
    private static final Pattern QUOTED_STRING = Pattern.compile("^\"(.*)\"$");
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("^\\{[^}]+\\}$");
    
    /**
     * Parses an expression from text.
     */
    public static Expression parse(String text) {
        final String trimmedText = text.trim();
        
        // Quoted string literal
        Matcher quotedMatcher = QUOTED_STRING.matcher(trimmedText);
        if (quotedMatcher.matches()) {
            String value = quotedMatcher.group(1);
            return context -> value;
        }
        
        // Variable reference
        if (VARIABLE_PATTERN.matcher(trimmedText).matches()) {
            return context -> {
                if (context.getVariableManager() != null && context.getScopeId() != null) {
                    // Evaluate placeholders in variable name (e.g., {coins::%player%} -> {coins::PlayerName})
                    String evaluatedName = evaluateVariableName(trimmedText, context);
                    return context.getVariableManager().getVariable(context.getScopeId(), evaluatedName);
                }
                return null;
            };
        }
        
        // Player expressions
        if (trimmedText.equals("player")) {
            return context -> context.getPlayer() != null ? context.getPlayer().getUsername() : null;
        }
        
        if (trimmedText.equals("player's name")) {
            return context -> context.getPlayer() != null ? context.getPlayer().getUsername() : null;
        }
        
        if (trimmedText.equals("player's uuid")) {
            return context -> context.getPlayer() != null ? context.getPlayer().getUniqueId().toString() : null;
        }
        
        if (trimmedText.equals("player's server")) {
            return context -> {
                Optional<RegisteredServer> server = context.getPlayerServer();
                return server.map(s -> s.getServerInfo().getName()).orElse(null);
            };
        }
        
        // Event data variables (e.g., %from-server%, %to-server%, %message%)
        if (trimmedText.startsWith("%") && trimmedText.endsWith("%")) {
            String key = trimmedText.substring(1, trimmedText.length() - 1);
            return context -> {
                Object value = context.getEventValue(key);
                return value != null ? value.toString() : null;
            };
        }
        
        // Command argument by name (arg-1, arg-2, etc. or custom names)
        if (trimmedText.startsWith("%") && trimmedText.endsWith("%")) {
            String argName = trimmedText.substring(1, trimmedText.length() - 1);
            return context -> context.getArgument(argName);
        }
        
        // Plain text literal (no quotes)
        return context -> trimmedText;
    }
    
    /**
     * Evaluates an expression with variable/placeholder replacement.
     */
    public static String evaluateWithReplacements(String text, ExecutionContext context) {
        String result = text;
        
        // Replace player expressions in braces: {player}, {player's name}, {player's uuid}, {player's server}
        // These MUST be in {} when inside strings, otherwise they are sent as literal text
        if (context.getPlayer() != null) {
            result = result.replace("{player}", context.getPlayer().getUsername());
            result = result.replace("{player's name}", context.getPlayer().getUsername());
            result = result.replace("{player's uuid}", context.getPlayer().getUniqueId().toString());
            
            // Also support %player% and %uuid% for backwards compatibility
            result = result.replace("%player%", context.getPlayer().getUsername());
            result = result.replace("%uuid%", context.getPlayer().getUniqueId().toString());
        }
        
        // Replace player's server expression
        Optional<RegisteredServer> server = context.getPlayerServer();
        if (server.isPresent()) {
            result = result.replace("{player's server}", server.get().getServerInfo().getName());
        }
        
        // Replace event data placeholders (%player%, %from-server%, %message%, etc.)
        if (context.getEventData() != null) {
            for (var entry : context.getEventData().entrySet()) {
                String key = "%" + entry.getKey() + "%";
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                result = result.replace(key, value);
            }
        }
        
        // Replace command arguments (%arg-1%, %argname%, etc.)
        if (context.getArgumentNames() != null && context.getArgumentValues() != null) {
            for (int i = 0; i < context.getArgumentNames().size() && i < context.getArgumentValues().length; i++) {
                result = result.replace("%" + context.getArgumentNames().get(i) + "%", context.getArgumentValues()[i]);
                result = result.replace("%arg-" + (i + 1) + "%", context.getArgumentValues()[i]);
            }
        }
        
        // Replace script variables {varname}, {list::key}, etc.
        // This must come AFTER player expressions to avoid conflicts
        if (context.getVariableManager() != null && context.getScopeId() != null) {
            Pattern varPattern = Pattern.compile("\\{[^}]+\\}");
            Matcher matcher = varPattern.matcher(result);
            StringBuffer sb = new StringBuffer();
            
            while (matcher.find()) {
                String varName = matcher.group();
                // Skip already-replaced player expressions (they won't match anymore anyway)
                // Evaluate any placeholders in the variable name first (e.g., {coins::%player%})
                String evaluatedVarName = evaluateVariableName(varName, context);
                String value = context.getVariableManager().getVariable(context.getScopeId(), evaluatedVarName);
                if (value != null) {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
                } else {
                    // Keep the original variable placeholder if not set
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(varName));
                }
            }
            matcher.appendTail(sb);
            result = sb.toString();
        }
        
        return result;
    }
    
    /**
     * Evaluates placeholders within a variable name (e.g., {coins::%player%} -> {coins::PlayerName})
     */
    private static String evaluateVariableName(String variableName, ExecutionContext context) {
        String result = variableName;
        
        // Replace player placeholders
        if (context.getPlayer() != null) {
            result = result.replace("%player%", context.getPlayer().getUsername());
            result = result.replace("%uuid%", context.getPlayer().getUniqueId().toString());
        }
        
        // Replace command arguments
        if (context.getArgumentNames() != null && context.getArgumentValues() != null) {
            for (int i = 0; i < context.getArgumentNames().size() && i < context.getArgumentValues().length; i++) {
                result = result.replace("%" + context.getArgumentNames().get(i) + "%", context.getArgumentValues()[i]);
            }
        }
        
        // Replace event data
        if (context.getEventData() != null) {
            for (var entry : context.getEventData().entrySet()) {
                String key = "%" + entry.getKey() + "%";
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                result = result.replace(key, value);
            }
        }
        
        return result;
    }
}

