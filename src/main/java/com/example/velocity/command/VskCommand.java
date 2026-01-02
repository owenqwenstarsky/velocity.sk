package com.example.velocity.command;

import com.example.velocity.script.CommandManager;
import com.example.velocity.script.LoadResult;
import com.example.velocity.script.Script;
import com.example.velocity.script.ScriptLoader;
import com.example.velocity.script.event.EventManager;
import com.example.velocity.script.variable.VariableManager;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class VskCommand implements SimpleCommand {
    private final ScriptLoader scriptLoader;
    private final CommandManager commandManager;
    private final EventManager eventManager;
    private final VariableManager variableManager;
    private final Logger logger;

    public VskCommand(ScriptLoader scriptLoader, CommandManager commandManager, EventManager eventManager, 
                      VariableManager variableManager, Logger logger) {
        this.scriptLoader = scriptLoader;
        this.commandManager = commandManager;
        this.eventManager = eventManager;
        this.variableManager = variableManager;
        this.logger = logger;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0) {
            showUsage(invocation);
            return;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "reload" -> handleReload(invocation, args);
            case "enable" -> handleEnable(invocation, args);
            case "disable" -> handleDisable(invocation, args);
            case "info" -> handleInfo(invocation);
            default -> showUsage(invocation);
        }
    }

    private void showUsage(Invocation invocation) {
        invocation.source().sendMessage(Component.text("VelocitySk Commands:", NamedTextColor.GOLD));
        invocation.source().sendMessage(Component.text("  /vsk reload <all|script.vsk>", NamedTextColor.YELLOW));
        invocation.source().sendMessage(Component.text("  /vsk enable <script.vsk>", NamedTextColor.YELLOW));
        invocation.source().sendMessage(Component.text("  /vsk disable <script.vsk>", NamedTextColor.YELLOW));
        invocation.source().sendMessage(Component.text("  /vsk info - Show plugin info and stats", NamedTextColor.YELLOW));
    }

    private void handleInfo(Invocation invocation) {
        invocation.source().sendMessage(Component.text("=== VelocitySk Info ===", NamedTextColor.GOLD));
        invocation.source().sendMessage(Component.text("Version: 1.0.0", NamedTextColor.YELLOW));
        invocation.source().sendMessage(Component.text("Variables: " + variableManager.getStats(), NamedTextColor.YELLOW));
        invocation.source().sendMessage(Component.text("Events: " + eventManager.getStats(), NamedTextColor.YELLOW));
    }

    // ========== RELOAD ==========

    private void handleReload(Invocation invocation, String[] args) {
        if (args.length < 2) {
            invocation.source().sendMessage(
                Component.text("Usage: /vsk reload <all|script.vsk>", NamedTextColor.RED)
            );
            return;
        }

        String target = args[1];

        if (target.equalsIgnoreCase("all")) {
            reloadAllScripts(invocation);
        } else if (target.endsWith(".vsk")) {
            reloadSingleScript(invocation, target);
        } else {
            invocation.source().sendMessage(
                Component.text("Invalid argument. Use 'all' or a script filename ending with .vsk", NamedTextColor.RED)
            );
        }
    }

    private void reloadAllScripts(Invocation invocation) {
        invocation.source().sendMessage(
            Component.text("Reloading all scripts...", NamedTextColor.YELLOW)
        );

        try {
            commandManager.unregisterAll();
            eventManager.unregisterAll();
            LoadResult result = scriptLoader.loadScripts();
            commandManager.registerScripts(result.getScripts());
            eventManager.registerScripts(result.getScripts());

            if (result.getScripts().isEmpty()) {
                invocation.source().sendMessage(
                    Component.text("No scripts were loaded. Check console for syntax errors.", NamedTextColor.RED)
                );
            } else {
                invocation.source().sendMessage(
                    Component.text("Successfully reloaded " + result.getScripts().size() + " script(s)!", NamedTextColor.GREEN)
                );
                if (result.hadErrors()) {
                    invocation.source().sendMessage(
                        Component.text("Warning: " + result.getErrorCount() + " script(s) failed to load. Check console for syntax errors.", NamedTextColor.YELLOW)
                    );
                }
            }
            logger.info("Reloaded all scripts ({} loaded successfully)", result.getScripts().size());
        } catch (Exception e) {
            invocation.source().sendMessage(
                Component.text("Failed to reload scripts. Check console for errors.", NamedTextColor.RED)
            );
            logger.error("Failed to reload all scripts", e);
        }
    }

    private void reloadSingleScript(Invocation invocation, String scriptName) {
        invocation.source().sendMessage(
            Component.text("Reloading script: " + scriptName + "...", NamedTextColor.YELLOW)
        );

        try {
            Script script = scriptLoader.loadSingleScript(scriptName);

            if (script == null) {
                invocation.source().sendMessage(
                    Component.text("Failed to load script: " + scriptName, NamedTextColor.RED)
                );
                invocation.source().sendMessage(
                    Component.text("Check console for syntax errors.", NamedTextColor.GRAY)
                );
                return;
            }

            commandManager.unregisterScriptCommands(script);
            commandManager.registerScript(script);
            // Note: Event triggers are reloaded with full reload only
            // Individual script event reloading is not yet supported

            invocation.source().sendMessage(
                Component.text("Successfully reloaded script: " + scriptName, NamedTextColor.GREEN)
            );
            logger.info("Reloaded script: {}", scriptName);
        } catch (Exception e) {
            invocation.source().sendMessage(
                Component.text("Failed to reload script: " + scriptName + ". Check console for errors.", NamedTextColor.RED)
            );
            logger.error("Failed to reload script: {}", scriptName, e);
        }
    }

    // ========== ENABLE ==========

    private void handleEnable(Invocation invocation, String[] args) {
        if (args.length < 2) {
            invocation.source().sendMessage(
                Component.text("Usage: /vsk enable <script.vsk>", NamedTextColor.RED)
            );
            return;
        }

        String scriptName = args[1];
        invocation.source().sendMessage(
            Component.text("Enabling script: " + scriptName + "...", NamedTextColor.YELLOW)
        );

        boolean success = scriptLoader.enableScript(scriptName);
        
        if (!success) {
            invocation.source().sendMessage(
                Component.text("Failed to enable script: " + scriptName + ". Check console for errors.", NamedTextColor.RED)
            );
            return;
        }

        // Remove .disabled extension if present for loading
        String enabledScriptName = scriptName.endsWith(".disabled") ? scriptName.replace(".disabled", "") : scriptName;
        Script script = scriptLoader.loadSingleScript(enabledScriptName);
        
        if (script == null) {
            invocation.source().sendMessage(
                Component.text("Script enabled but has syntax errors. Check console for details.", NamedTextColor.RED)
            );
            return;
        }

        commandManager.registerScript(script);

        invocation.source().sendMessage(
            Component.text("Successfully enabled script: " + enabledScriptName, NamedTextColor.GREEN)
        );
        logger.info("Enabled and loaded script: {}", enabledScriptName);
    }

    // ========== DISABLE ==========

    private void handleDisable(Invocation invocation, String[] args) {
        if (args.length < 2) {
            invocation.source().sendMessage(
                Component.text("Usage: /vsk disable <script.vsk>", NamedTextColor.RED)
            );
            return;
        }

        String scriptName = args[1];
        invocation.source().sendMessage(
            Component.text("Disabling script: " + scriptName + "...", NamedTextColor.YELLOW)
        );

        Script script = scriptLoader.loadSingleScript(scriptName);
        
        if (script == null) {
            invocation.source().sendMessage(
                Component.text("Script not found: " + scriptName, NamedTextColor.RED)
            );
            return;
        }

        commandManager.unregisterScriptCommands(script);

        boolean success = scriptLoader.disableScript(scriptName);
        
        if (!success) {
            commandManager.registerScript(script);
            invocation.source().sendMessage(
                Component.text("Failed to disable script: " + scriptName + ". Check console for errors.", NamedTextColor.RED)
            );
            return;
        }

        invocation.source().sendMessage(
            Component.text("Successfully disabled script: " + scriptName, NamedTextColor.GREEN)
        );
        logger.info("Disabled and unloaded script: {}", scriptName);
    }

    // ========== TAB COMPLETION ==========

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        
        // First argument: subcommands
        if (args.length == 0 || args.length == 1) {
            List<String> suggestions = List.of("reload", "enable", "disable", "info");
            
            if (args.length == 1) {
                String input = args[0].toLowerCase();
                return suggestions.stream()
                    .filter(s -> s.startsWith(input))
                    .toList();
            }
            
            return suggestions;
        }
        
        // Second argument: depends on subcommand
        if (args.length == 2) {
            String action = args[0].toLowerCase();
            List<String> suggestions = new ArrayList<>();
            
            switch (action) {
                case "reload" -> {
                    suggestions.add("all");
                    suggestions.addAll(scriptLoader.getEnabledScriptNames());
                }
                case "enable" -> suggestions.addAll(scriptLoader.getDisabledScriptNames());
                case "disable" -> suggestions.addAll(scriptLoader.getEnabledScriptNames());
                case "info" -> {} // No suggestions for info
            }
            
            String input = args[1].toLowerCase();
            return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(input))
                .toList();
        }

        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        // TODO: Add permission check if needed
        return true;
    }
}

