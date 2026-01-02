package com.example.velocity.script.execution;

import com.example.velocity.script.Script;
import com.example.velocity.script.expression.ExpressionParser;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

/**
 * Executes script actions in a given execution context.
 */
public class ActionExecutor {
    private final ProxyServer server;
    private final Logger logger;

    public ActionExecutor(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    /**
     * Executes a list of actions in order.
     */
    public void executeActions(List<Script.Action> actions, ExecutionContext context) {
        for (Script.Action action : actions) {
            executeAction(action, context);
        }
    }

    /**
     * Executes a single action.
     */
    public void executeAction(Script.Action action, ExecutionContext context) {
        switch (action.getType()) {
            case SEND_MESSAGE -> executeSendMessage(action, context);
            case TRANSFER_PLAYER -> executeTransferPlayer(action, context);
            case SEND_TO_SERVER_PLAYERS -> executeSendToServerPlayers(action, context);
            case SET_VARIABLE -> executeSetVariable(action, context);
            case DELETE_VARIABLE -> executeDeleteVariable(action, context);
            case CONDITIONAL -> executeConditional((Script.ConditionalAction) action, context);
        }
    }

    private void executeSendMessage(Script.Action action, ExecutionContext context) {
        String message = action.getMessage();
        if (message == null) return;

        // Replace variables and placeholders
        String processedMessage = ExpressionParser.evaluateWithReplacements(message, context);
        // Translate & color codes to §
        processedMessage = translateColorCodes(processedMessage);
        Component component = Component.text(processedMessage);

        Script.MessageTarget target = action.getTarget();
        Player executor = context.getPlayer();

        switch (target) {
            case PLAYER -> {
                if (executor != null) {
                    executor.sendMessage(component);
                    logger.debug("Sent message to player: {}", executor.getUsername());
                }
            }
            case ALL_PLAYERS -> {
                server.getAllPlayers().forEach(p -> p.sendMessage(component));
                logger.debug("Broadcast message to all players");
            }
            case SPECIFIC_PLAYER -> {
                String targetName = ExpressionParser.evaluateWithReplacements(
                    action.getTargetPlayer(), context);
                Optional<Player> targetPlayer = server.getPlayer(targetName);
                if (targetPlayer.isPresent()) {
                    targetPlayer.get().sendMessage(component);
                    logger.debug("Sent message to player: {}", targetName);
                } else {
                    logger.warn("Cannot send message to '{}' - player not online", targetName);
                }
            }
            case SERVER_PLAYERS -> {
                // This is handled by executeSendToServerPlayers
                logger.warn("SERVER_PLAYERS target used in SEND_MESSAGE action, should use SEND_TO_SERVER_PLAYERS");
            }
        }
    }

    private void executeTransferPlayer(Script.Action action, ExecutionContext context) {
        String playerExpr = action.getPlayerExpr();
        String serverName = action.getServerName();

        if (playerExpr == null || serverName == null) {
            logger.warn("Transfer action missing required data");
            return;
        }

        // Evaluate expressions
        String evaluatedServerName = ExpressionParser.evaluateWithReplacements(serverName, context);
        
        // Get target player
        Player targetPlayer = null;
        if (playerExpr.equals("player") && context.getPlayer() != null) {
            targetPlayer = context.getPlayer();
        } else {
            String playerName = ExpressionParser.evaluateWithReplacements(playerExpr, context);
            Optional<Player> player = server.getPlayer(playerName);
            if (player.isPresent()) {
                targetPlayer = player.get();
            }
        }

        if (targetPlayer == null) {
            logger.warn("Cannot transfer player - player not found: {}", playerExpr);
            return;
        }

        // Get target server
        Optional<RegisteredServer> targetServer = server.getServer(evaluatedServerName);
        if (targetServer.isEmpty()) {
            logger.warn("Cannot transfer player - server not found: {}", evaluatedServerName);
            targetPlayer.sendMessage(Component.text("§cServer not found: " + evaluatedServerName));
            return;
        }

        // Transfer player
        targetPlayer.createConnectionRequest(targetServer.get()).fireAndForget();
        logger.debug("Transferring player {} to server {}", targetPlayer.getUsername(), evaluatedServerName);
    }

    private void executeSendToServerPlayers(Script.Action action, ExecutionContext context) {
        String message = action.getMessage();
        String serverName = action.getServerName();

        if (message == null || serverName == null) {
            logger.warn("Send to server players action missing required data");
            return;
        }

        // Evaluate expressions
        String processedMessage = ExpressionParser.evaluateWithReplacements(message, context);
        // Translate & color codes to §
        processedMessage = translateColorCodes(processedMessage);
        String evaluatedServerName = ExpressionParser.evaluateWithReplacements(serverName, context);
        Component component = Component.text(processedMessage);

        // Get target server
        Optional<RegisteredServer> targetServer = server.getServer(evaluatedServerName);
        if (targetServer.isEmpty()) {
            logger.warn("Cannot send message to server players - server not found: {}", evaluatedServerName);
            return;
        }

        // Send to all players on that server
        int count = 0;
        for (Player player : server.getAllPlayers()) {
            Optional<ServerConnection> connection = player.getCurrentServer();
            if (connection.isPresent() && 
                connection.get().getServerInfo().getName().equals(evaluatedServerName)) {
                player.sendMessage(component);
                count++;
            }
        }

        logger.debug("Sent message to {} player(s) on server {}", count, evaluatedServerName);
    }

    private void executeSetVariable(Script.Action action, ExecutionContext context) {
        String variableName = action.getVariableName();
        String variableValue = action.getVariableValue();

        if (variableName == null || variableValue == null) {
            logger.warn("Set variable action missing required data");
            return;
        }

        // Evaluate the variable name (for placeholders like %player% in {coins::%player%})
        String evaluatedName = evaluateVariableName(variableName, context);
        // Evaluate the value expression
        String evaluatedValue = ExpressionParser.evaluateWithReplacements(variableValue, context);

        // Set the variable
        if (context.getVariableManager() != null && context.getScopeId() != null) {
            context.getVariableManager().setVariable(context.getScopeId(), evaluatedName, evaluatedValue);
            logger.debug("Set variable {} = {}", evaluatedName, evaluatedValue);
        } else {
            logger.warn("Cannot set variable - variable manager not available");
        }
    }

    private void executeDeleteVariable(Script.Action action, ExecutionContext context) {
        String variableName = action.getVariableName();

        if (variableName == null) {
            logger.warn("Delete variable action missing variable name");
            return;
        }

        // Evaluate the variable name (for placeholders like %player% in {coins::%player%})
        String evaluatedName = evaluateVariableName(variableName, context);

        // Delete the variable
        if (context.getVariableManager() != null && context.getScopeId() != null) {
            context.getVariableManager().deleteVariable(context.getScopeId(), evaluatedName);
            logger.debug("Deleted variable {}", evaluatedName);
        } else {
            logger.warn("Cannot delete variable - variable manager not available");
        }
    }

    /**
     * Evaluates placeholders within a variable name (e.g., {coins::%player%} -> {coins::PlayerName})
     */
    private String evaluateVariableName(String variableName, ExecutionContext context) {
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

    private void executeConditional(Script.ConditionalAction action, ExecutionContext context) {
        // Evaluate the condition
        boolean conditionMet = action.getCondition().evaluate(context);

        if (conditionMet) {
            // Execute if actions
            executeActions(action.getIfActions(), context);
        } else {
            // Execute else actions
            executeActions(action.getElseActions(), context);
        }
    }

    /**
     * Translates '&' color codes to '§' (section sign) for Minecraft formatting.
     * Supports colors (0-9, a-f), formatting (k-o, r), and hex colors (&#RRGGBB).
     */
    private String translateColorCodes(String text) {
        if (text == null) return null;
        
        // First handle hex colors: &#RRGGBB -> §x§R§R§G§G§B§B
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            if (i + 8 <= text.length() && text.charAt(i) == '&' && text.charAt(i + 1) == '#') {
                String hex = text.substring(i + 2, i + 8);
                if (hex.matches("[0-9A-Fa-f]{6}")) {
                    result.append("§x");
                    for (char c : hex.toCharArray()) {
                        result.append("§").append(Character.toLowerCase(c));
                    }
                    i += 8;
                    continue;
                }
            }
            result.append(text.charAt(i));
            i++;
        }
        
        // Then handle standard codes: &X -> §X
        String processed = result.toString();
        char[] chars = processed.toCharArray();
        StringBuilder finalResult = new StringBuilder();
        
        for (int j = 0; j < chars.length; j++) {
            if (chars[j] == '&' && j + 1 < chars.length) {
                char code = Character.toLowerCase(chars[j + 1]);
                // Valid color/format codes: 0-9, a-f, k-o, r
                if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f') || 
                    (code >= 'k' && code <= 'o') || code == 'r') {
                    finalResult.append('§').append(chars[j + 1]);
                    j++; // Skip the next character
                    continue;
                }
            }
            finalResult.append(chars[j]);
        }
        
        return finalResult.toString();
    }
}

