package com.example.velocity.script;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CommandManager {
    private final ProxyServer server;
    private final Logger logger;
    private final Map<String, Script.CommandScript> registeredCommands;

    public CommandManager(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        this.registeredCommands = new HashMap<>();
    }

    public void registerScripts(List<Script> scripts) {
        // Clear previous commands
        registeredCommands.clear();

        // Register all commands from all scripts
        for (Script script : scripts) {
            for (Script.CommandScript command : script.getCommands()) {
                registerCommand(command);
            }
        }

        logger.info("Registered {} command(s) from scripts", registeredCommands.size());
    }

    private void registerCommand(Script.CommandScript commandScript) {
        String commandName = commandScript.getCommandName();
        
        // Store the command
        registeredCommands.put(commandName, commandScript);

        // Register with Velocity
        server.getCommandManager().register(
            commandName,
            new ScriptCommand(commandScript),
            commandName
        );

        logger.info("Registered command: /{}", commandName);
    }

    private class ScriptCommand implements SimpleCommand {
        private final Script.CommandScript commandScript;

        public ScriptCommand(Script.CommandScript commandScript) {
            this.commandScript = commandScript;
        }

        @Override
        public void execute(Invocation invocation) {
            if (!(invocation.source() instanceof Player player)) {
                invocation.source().sendMessage(Component.text("This command can only be executed by a player."));
                return;
            }

            // Get command arguments
            String[] args = invocation.arguments();
            
            // Check if enough arguments are provided
            List<String> requiredArgs = commandScript.getArguments();
            if (requiredArgs.size() > args.length) {
                player.sendMessage(Component.text("Usage: /" + commandScript.getCommandName() + 
                    (requiredArgs.isEmpty() ? "" : " <" + String.join("> <", requiredArgs) + ">")));
                return;
            }

            // Execute all actions in the command
            for (Script.Action action : commandScript.getActions()) {
                executeAction(player, action, requiredArgs, args);
            }
        }

        private void executeAction(Player executor, Script.Action action, List<String> argNames, String[] argValues) {
            if (action.getType() == Script.ActionType.SEND_MESSAGE) {
                // Replace variables in the message
                String processedMessage = VariableReplacer.replace(
                    action.getMessage(), 
                    executor, 
                    argNames, 
                    argValues
                );
                Component message = Component.text(processedMessage);

                switch (action.getTarget()) {
                    case PLAYER -> {
                        executor.sendMessage(message);
                        logger.debug("Sent message to player: {}", executor.getUsername());
                    }
                    case ALL_PLAYERS -> {
                        server.getAllPlayers().forEach(p -> p.sendMessage(message));
                        logger.debug("Broadcast message to all players");
                    }
                    case SPECIFIC_PLAYER -> {
                        // Replace variables in target player name
                        String targetName = VariableReplacer.replace(
                            action.getTargetPlayer(),
                            executor,
                            argNames,
                            argValues
                        );
                        Optional<Player> targetPlayer = server.getPlayer(targetName);
                        if (targetPlayer.isPresent()) {
                            targetPlayer.get().sendMessage(message);
                            logger.debug("Sent message to player: {}", targetName);
                        } else {
                            logger.warn("Skipped sending message to '{}' - player is not online (executed by: {})", 
                                targetName, executor.getUsername());
                        }
                    }
                }
            }
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            // No permissions required for now
            return true;
        }
    }

    public void registerScript(Script script) {
        for (Script.CommandScript command : script.getCommands()) {
            registerCommand(command);
        }
        logger.info("Registered {} command(s) from script: {}", script.getCommands().size(), script.getName());
    }

    public void unregisterScriptCommands(Script script) {
        for (Script.CommandScript command : script.getCommands()) {
            String commandName = command.getCommandName();
            server.getCommandManager().unregister(commandName);
            registeredCommands.remove(commandName);
            logger.info("Unregistered command: /{}", commandName);
        }
    }

    public void unregisterAll() {
        for (String commandName : registeredCommands.keySet()) {
            server.getCommandManager().unregister(commandName);
        }
        registeredCommands.clear();
        logger.info("Unregistered all script commands");
    }
}

