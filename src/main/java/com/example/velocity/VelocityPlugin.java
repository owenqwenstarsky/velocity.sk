package com.example.velocity;

import com.example.velocity.command.VskCommand;
import com.example.velocity.script.CommandManager;
import com.example.velocity.script.LoadResult;
import com.example.velocity.script.ScriptLoader;
import com.example.velocity.script.event.EventManager;
import com.example.velocity.script.variable.VariableManager;
import com.example.velocity.script.variable.VariableStorage;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.sql.SQLException;

@Plugin(
    id = "velocity-sk",
    name = "VelocitySk",
    version = "1.0.0",
    description = "A recreation of Skript for Velocity",
    authors = {"owenmakesmistakes"}
)
public class VelocityPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    
    private ScriptLoader scriptLoader;
    private CommandManager commandManager;
    private EventManager eventManager;
    private VariableStorage variableStorage;
    private VariableManager variableManager;

    @Inject
    public VelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("VelocitySk is initializing...");

        try {
            // Initialize variable storage
            File dbFile = dataDirectory.resolve("variables.db").toFile();
            variableStorage = new VariableStorage(logger, dbFile);
            variableStorage.initialize();

            // Initialize variable manager
            variableManager = new VariableManager(logger, variableStorage);
            variableManager.loadGlobalVariables();

            // Create scripts/ directory structure
            scriptLoader = new ScriptLoader(logger, dataDirectory);
            scriptLoader.ensureDirectoryStructure();

            // Initialize command manager
            commandManager = new CommandManager(server, logger, variableManager);

            // Initialize event manager
            eventManager = new EventManager(server, logger, variableManager);

            // Register event manager with Velocity
            server.getEventManager().register(this, eventManager);

            // Load and register scripts
            LoadResult result = scriptLoader.loadScripts();
            commandManager.registerScripts(result.getScripts());
            eventManager.registerScripts(result.getScripts());

            // Register /vsk command (reload, enable, disable)
            server.getCommandManager().register(
                server.getCommandManager().metaBuilder("vsk")
                    .aliases("velocitysk")
                    .build(),
                new VskCommand(scriptLoader, commandManager, eventManager, variableManager, logger)
            );

            if (result.hadErrors()) {
                logger.warn("VelocitySk has been enabled! Loaded {} script(s) with {} error(s)", 
                    result.getScripts().size(), result.getErrorCount());
            } else {
                logger.info("VelocitySk has been enabled! Loaded {} script(s)", result.getScripts().size());
            }
            
            logger.info("Variable stats: {}", variableManager.getStats());
            logger.info("Event stats: {}", eventManager.getStats());

        } catch (SQLException e) {
            logger.error("Failed to initialize variable storage", e);
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("VelocitySk is shutting down...");
        
        // Close variable storage
        if (variableStorage != null) {
            variableStorage.close();
        }
        
        logger.info("VelocitySk has been disabled.");
    }
}
