package com.example.velocity.script.expression;

import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and creates Condition objects from text.
 */
public class ConditionParser {
    
    // Patterns for different condition types
    private static final Pattern IS_SET_PATTERN = Pattern.compile("(.+?)\\s+is\\s+set", Pattern.CASE_INSENSITIVE);
    private static final Pattern IS_NOT_SET_PATTERN = Pattern.compile("(.+?)\\s+is\\s+not\\s+set", Pattern.CASE_INSENSITIVE);
    private static final Pattern IN_SERVER_PATTERN = Pattern.compile("(.+?)\\s+is\\s+in\\s+server\\s+\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern NOT_IN_SERVER_PATTERN = Pattern.compile("(.+?)\\s+is\\s+not\\s+in\\s+server\\s+\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern EQUALS_PATTERN = Pattern.compile("(.+?)\\s+(?:is|=|==)\\s+(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern NOT_EQUALS_PATTERN = Pattern.compile("(.+?)\\s+(?:is\\s+not|!=)\\s+(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTAINS_PATTERN = Pattern.compile("(.+?)\\s+contains\\s+(.+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern GREATER_THAN_PATTERN = Pattern.compile("(.+?)\\s+>\\s+(.+)");
    private static final Pattern LESS_THAN_PATTERN = Pattern.compile("(.+?)\\s+<\\s+(.+)");
    
    /**
     * Parses a condition from text.
     */
    public static Condition parse(String text) {
        text = text.trim();
        
        // Variable is set
        Matcher isSetMatcher = IS_SET_PATTERN.matcher(text);
        if (isSetMatcher.matches()) {
            String varExpr = isSetMatcher.group(1).trim();
            return context -> {
                if (context.getVariableManager() != null && context.getScopeId() != null) {
                    String evaluatedVar = evaluateVariableName(varExpr, context);
                    return context.getVariableManager().isSet(context.getScopeId(), evaluatedVar);
                }
                return false;
            };
        }
        
        // Variable is not set
        Matcher isNotSetMatcher = IS_NOT_SET_PATTERN.matcher(text);
        if (isNotSetMatcher.matches()) {
            String varExpr = isNotSetMatcher.group(1).trim();
            return context -> {
                if (context.getVariableManager() != null && context.getScopeId() != null) {
                    String evaluatedVar = evaluateVariableName(varExpr, context);
                    return !context.getVariableManager().isSet(context.getScopeId(), evaluatedVar);
                }
                return true;
            };
        }
        
        // Player is in server
        Matcher inServerMatcher = IN_SERVER_PATTERN.matcher(text);
        if (inServerMatcher.matches()) {
            String playerExpr = inServerMatcher.group(1).trim();
            String serverName = inServerMatcher.group(2).trim();
            
            return context -> {
                if (playerExpr.equals("player") && context.getPlayer() != null) {
                    Optional<RegisteredServer> currentServer = context.getPlayerServer();
                    if (currentServer.isPresent()) {
                        return currentServer.get().getServerInfo().getName().equalsIgnoreCase(serverName);
                    }
                }
                return false;
            };
        }
        
        // Player is not in server
        Matcher notInServerMatcher = NOT_IN_SERVER_PATTERN.matcher(text);
        if (notInServerMatcher.matches()) {
            String playerExpr = notInServerMatcher.group(1).trim();
            String serverName = notInServerMatcher.group(2).trim();
            
            return context -> {
                if (playerExpr.equals("player") && context.getPlayer() != null) {
                    Optional<RegisteredServer> currentServer = context.getPlayerServer();
                    if (currentServer.isPresent()) {
                        return !currentServer.get().getServerInfo().getName().equalsIgnoreCase(serverName);
                    }
                }
                return true;
            };
        }
        
        // Contains
        Matcher containsMatcher = CONTAINS_PATTERN.matcher(text);
        if (containsMatcher.matches()) {
            String leftExpr = containsMatcher.group(1).trim();
            String rightExpr = containsMatcher.group(2).trim();
            
            return context -> {
                Expression left = ExpressionParser.parse(leftExpr);
                Expression right = ExpressionParser.parse(rightExpr);
                
                String leftValue = left.evaluate(context);
                String rightValue = right.evaluate(context);
                
                if (leftValue != null && rightValue != null) {
                    return leftValue.contains(rightValue);
                }
                return false;
            };
        }
        
        // Not equals (check before equals since it's more specific)
        Matcher notEqualsMatcher = NOT_EQUALS_PATTERN.matcher(text);
        if (notEqualsMatcher.matches()) {
            String leftExpr = notEqualsMatcher.group(1).trim();
            String rightExpr = notEqualsMatcher.group(2).trim();
            
            return context -> {
                Expression left = ExpressionParser.parse(leftExpr);
                Expression right = ExpressionParser.parse(rightExpr);
                
                String leftValue = left.evaluate(context);
                String rightValue = right.evaluate(context);
                
                if (leftValue == null && rightValue == null) return false;
                if (leftValue == null || rightValue == null) return true;
                
                return !leftValue.equals(rightValue);
            };
        }
        
        // Equals
        Matcher equalsMatcher = EQUALS_PATTERN.matcher(text);
        if (equalsMatcher.matches()) {
            String leftExpr = equalsMatcher.group(1).trim();
            String rightExpr = equalsMatcher.group(2).trim();
            
            return context -> {
                Expression left = ExpressionParser.parse(leftExpr);
                Expression right = ExpressionParser.parse(rightExpr);
                
                String leftValue = left.evaluate(context);
                String rightValue = right.evaluate(context);
                
                if (leftValue == null && rightValue == null) return true;
                if (leftValue == null || rightValue == null) return false;
                
                return leftValue.equals(rightValue);
            };
        }
        
        // Greater than
        Matcher greaterMatcher = GREATER_THAN_PATTERN.matcher(text);
        if (greaterMatcher.matches()) {
            String leftExpr = greaterMatcher.group(1).trim();
            String rightExpr = greaterMatcher.group(2).trim();
            
            return context -> {
                try {
                    Expression left = ExpressionParser.parse(leftExpr);
                    Expression right = ExpressionParser.parse(rightExpr);
                    
                    String leftValue = left.evaluate(context);
                    String rightValue = right.evaluate(context);
                    
                    if (leftValue != null && rightValue != null) {
                        double leftNum = Double.parseDouble(leftValue);
                        double rightNum = Double.parseDouble(rightValue);
                        return leftNum > rightNum;
                    }
                } catch (NumberFormatException e) {
                    // Not numbers, can't compare
                }
                return false;
            };
        }
        
        // Less than
        Matcher lessMatcher = LESS_THAN_PATTERN.matcher(text);
        if (lessMatcher.matches()) {
            String leftExpr = lessMatcher.group(1).trim();
            String rightExpr = lessMatcher.group(2).trim();
            
            return context -> {
                try {
                    Expression left = ExpressionParser.parse(leftExpr);
                    Expression right = ExpressionParser.parse(rightExpr);
                    
                    String leftValue = left.evaluate(context);
                    String rightValue = right.evaluate(context);
                    
                    if (leftValue != null && rightValue != null) {
                        double leftNum = Double.parseDouble(leftValue);
                        double rightNum = Double.parseDouble(rightValue);
                        return leftNum < rightNum;
                    }
                } catch (NumberFormatException e) {
                    // Not numbers, can't compare
                }
                return false;
            };
        }
        
        // Default: always true
        return context -> true;
    }
    
    /**
     * Checks if a condition string is valid.
     */
    public static boolean isValidCondition(String text) {
        text = text.trim();
        
        return IS_SET_PATTERN.matcher(text).matches() ||
               IS_NOT_SET_PATTERN.matcher(text).matches() ||
               IN_SERVER_PATTERN.matcher(text).matches() ||
               NOT_IN_SERVER_PATTERN.matcher(text).matches() ||
               CONTAINS_PATTERN.matcher(text).matches() ||
               NOT_EQUALS_PATTERN.matcher(text).matches() ||
               EQUALS_PATTERN.matcher(text).matches() ||
               GREATER_THAN_PATTERN.matcher(text).matches() ||
               LESS_THAN_PATTERN.matcher(text).matches();
    }
    
    /**
     * Evaluates placeholders within a variable name (e.g., {coins::%player%} -> {coins::PlayerName})
     */
    private static String evaluateVariableName(String variableName, com.example.velocity.script.execution.ExecutionContext context) {
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

