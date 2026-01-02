package com.example.velocity.script;

import com.example.velocity.script.execution.ActionExecutor;
import com.example.velocity.script.execution.ExecutionContext;
import com.example.velocity.script.variable.VariableManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CommandManager {
    private final ProxyServer server;
    private final Logger logger;
    private final VariableManager variableManager;
    private final ActionExecutor actionExecutor;
    private final Map<String, Script.CommandScript> registeredCommands;

    public CommandManager(ProxyServer server, Logger logger, VariableManager variableManager) {
        this.server = server;
        this.logger = logger;
        this.variableManager = variableManager;
        this.actionExecutor = new ActionExecutor(server, logger);
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

        // Register with Velocity - don't pass commandName in aliases parameter
        server.getCommandManager().register(
            commandName,
            new ScriptCommand(commandScript)
        );

        logger.info("Registered command: /{}", commandName);

        // Register aliases
        for (String alias : commandScript.getAliases()) {
            server.getCommandManager().register(
                alias,
                new ScriptCommand(commandScript)
            );
            logger.info("Registered alias: /{} -> /{}", alias, commandName);
        }
    }

    private class ScriptCommand implements SimpleCommand {
        private final Script.CommandScript commandScript;

        public ScriptCommand(Script.CommandScript commandScript) {
            this.commandScript = commandScript;
        }

        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();

            // Check if source is a player
            if (!(source instanceof Player player)) {
                source.sendMessage(Component.text("This command can only be executed by a player."));
                return;
            }

            // Check permission
            if (commandScript.getPermission() != null && !commandScript.getPermission().isEmpty()) {
                if (!player.hasPermission(commandScript.getPermission())) {
                    String message = commandScript.getPermissionMessage();
                    if (message == null || message.isEmpty()) {
                        message = "§cYou don't have permission to use this command.";
                    }
                    player.sendMessage(Component.text(message));
                    return;
                }
            }

            // Get command arguments
            String[] args = invocation.arguments();
            
            // Check if enough arguments are provided
            List<String> requiredArgs = commandScript.getArguments();
            if (requiredArgs.size() > args.length) {
                String usage = commandScript.getUsage();
                if (usage == null || usage.isEmpty()) {
                    usage = "/" + commandScript.getCommandName() + 
                        (requiredArgs.isEmpty() ? "" : " <" + String.join("> <", requiredArgs) + ">");
                }
                player.sendMessage(Component.text("§cUsage: " + usage));
                return;
            }

            // Create execution context
            UUID scopeId = variableManager.createScope();
            
            try {
                ExecutionContext context = new ExecutionContext.Builder()
                    .server(server)
                    .player(player)
                    .arguments(requiredArgs, args)
                    .variableManager(variableManager)
                    .scopeId(scopeId)
                    .build();

                // Execute all actions in the command
                actionExecutor.executeActions(commandScript.getActions(), context);
            } catch (Exception e) {
                logger.error("Error executing command /{} for player {}", 
                            commandScript.getCommandName(), player.getUsername(), e);
                player.sendMessage(Component.text("§cAn error occurred while executing this command."));
            } finally {
                // Clean up local variables
                variableManager.destroyScope(scopeId);
            }
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            // We handle permission checking in execute() to send custom messages
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

            // Unregister aliases
            for (String alias : command.getAliases()) {
                server.getCommandManager().unregister(alias);
                logger.info("Unregistered alias: /{}", alias);
            }
        }
    }

    public void unregisterAll() {
        for (String commandName : registeredCommands.keySet()) {
            Script.CommandScript command = registeredCommands.get(commandName);
            server.getCommandManager().unregister(commandName);
            
            // Unregister aliases
            for (String alias : command.getAliases()) {
                server.getCommandManager().unregister(alias);
            }
        }
        registeredCommands.clear();
        logger.info("Unregistered all script commands");
    }
}
