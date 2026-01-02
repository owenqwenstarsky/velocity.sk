package com.example.velocity;

import com.example.velocity.command.VskCommand;
import com.example.velocity.script.CommandManager;
import com.example.velocity.script.LoadResult;
import com.example.velocity.script.ScriptLoader;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
    id = "velocity-sk",
    name = "VelocitySk",
    version = "1.0.0",
    description = "A recreation of Skript for Velocity",
    authors = {"Author"}
)
public class VelocityPlugin {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    
    private ScriptLoader scriptLoader;
    private CommandManager commandManager;

    @Inject
    public VelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("VelocitySk is initializing...");

        // Create scripts/ directory structure
        scriptLoader = new ScriptLoader(logger, dataDirectory);
        scriptLoader.ensureDirectoryStructure();

        // Initialize command manager
        commandManager = new CommandManager(server, logger);

        // Load and register scripts
        LoadResult result = scriptLoader.loadScripts();
        commandManager.registerScripts(result.getScripts());

        // Register /vsk command (reload, enable, disable)
        server.getCommandManager().register(
            server.getCommandManager().metaBuilder("vsk")
                .aliases("velocitysk")
                .build(),
            new VskCommand(scriptLoader, commandManager, logger)
        );

        if (result.hadErrors()) {
            logger.warn("VelocitySk has been enabled! Loaded {} script(s) with {} error(s)", 
                result.getScripts().size(), result.getErrorCount());
        } else {
            logger.info("VelocitySk has been enabled! Loaded {} script(s)", result.getScripts().size());
        }
    }
}

