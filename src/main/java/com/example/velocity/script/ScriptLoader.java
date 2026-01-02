package com.example.velocity.script;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ScriptLoader {
    private final Logger logger;
    private final ScriptParser parser;
    private final Path scriptsDirectory;

    public ScriptLoader(Logger logger, Path dataDirectory) {
        this.logger = logger;
        this.parser = new ScriptParser(logger);
        this.scriptsDirectory = dataDirectory.resolve("scripts");
    }

    public void ensureDirectoryStructure() {
        try {
            if (!Files.exists(scriptsDirectory.getParent())) {
                Files.createDirectories(scriptsDirectory.getParent());
                logger.info("Created data directory: {}", scriptsDirectory.getParent());
            }
            
            if (!Files.exists(scriptsDirectory)) {
                Files.createDirectories(scriptsDirectory);
                logger.info("Created scripts directory: {}", scriptsDirectory);
                createExampleScript();
            }
        } catch (IOException e) {
            logger.error("Failed to create directory structure", e);
        }
    }

    private void createExampleScript() {
        Path exampleScript = scriptsDirectory.resolve("example.vsk.disabled");
        try {
            String exampleContent = """
                # Example VelocitySk Script
                # This is a comment
                # This script is disabled by default. Use /vsk enable example.vsk to enable it.
                
                # Basic command without arguments
                command /test:
                  trigger:
                    send "This is a test by %player%" to player
                
                # Command with default variables
                command /hello:
                  trigger:
                    send "Hello %player%!" to player
                    send "%player% says hello to everyone!" to all players
                
                # Command with arguments
                command /greet <name>:
                  trigger:
                    send "You greeted %name%!" to player
                    send "%player% greets you!" to %name%
                
                # Command with multiple arguments
                command /tell <player> <message>:
                  trigger:
                    send "You told %player%: %message%" to player
                    send "%player% tells you: %message%" to %player%
                
                # Available variables:
                # %player% - The player executing the command
                # %uuid% - The player's UUID
                # %arg-1%, %arg-2%, etc - Arguments by position
                # %argname% - Arguments by name (e.g., %name%, %message%)
                """;
            
            Files.writeString(exampleScript, exampleContent);
            logger.info("Created example script (disabled): {}", exampleScript.getFileName());
        } catch (IOException e) {
            logger.error("Failed to create example script", e);
        }
    }

    public LoadResult loadScripts() {
        List<Script> scripts = new ArrayList<>();
        int errorCount = 0;
        
        if (!Files.exists(scriptsDirectory)) {
            logger.warn("Scripts directory does not exist: {}", scriptsDirectory);
            return new LoadResult(scripts, false, 0);
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(scriptsDirectory, "*.vsk")) {
            for (Path scriptFile : stream) {
                try {
                    Script script = parser.parse(scriptFile);
                    scripts.add(script);
                    logger.info("Loaded script: {}", scriptFile.getFileName());
                } catch (ScriptParseException e) {
                    errorCount++;
                    logger.error("Failed to parse script: {}", scriptFile.getFileName());
                    logger.error("Script has {} error(s):", e.getErrors().size());
                    for (ScriptParseException.ParseError error : e.getErrors()) {
                        logger.error("  {}", error.toString());
                    }
                } catch (IOException e) {
                    errorCount++;
                    logger.error("Failed to read script file: {}", scriptFile.getFileName(), e);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to read scripts directory", e);
        }

        return new LoadResult(scripts, errorCount > 0, errorCount);
    }

    public Script loadSingleScript(String scriptName) {
        if (!Files.exists(scriptsDirectory)) {
            logger.warn("Scripts directory does not exist: {}", scriptsDirectory);
            return null;
        }

        Path scriptFile = scriptsDirectory.resolve(scriptName);
        
        if (!Files.exists(scriptFile)) {
            logger.warn("Script file does not exist: {}", scriptName);
            return null;
        }

        try {
            Script script = parser.parse(scriptFile);
            logger.info("Loaded script: {}", scriptName);
            return script;
        } catch (ScriptParseException e) {
            logger.error("Failed to parse script: {}", scriptName);
            logger.error("Script has {} error(s):", e.getErrors().size());
            for (ScriptParseException.ParseError error : e.getErrors()) {
                logger.error("  {}", error.toString());
            }
            return null;
        } catch (IOException e) {
            logger.error("Failed to read script file: {}", scriptName, e);
            return null;
        }
    }

    public List<String> getScriptNames() {
        List<String> scriptNames = new ArrayList<>();
        
        if (!Files.exists(scriptsDirectory)) {
            return scriptNames;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(scriptsDirectory, "*.vsk")) {
            for (Path scriptFile : stream) {
                scriptNames.add(scriptFile.getFileName().toString());
            }
        } catch (IOException e) {
            logger.error("Failed to read scripts directory", e);
        }

        return scriptNames;
    }

    public List<String> getEnabledScriptNames() {
        List<String> scriptNames = new ArrayList<>();
        
        if (!Files.exists(scriptsDirectory)) {
            return scriptNames;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(scriptsDirectory, "*.vsk")) {
            for (Path scriptFile : stream) {
                String fileName = scriptFile.getFileName().toString();
                // Only include files that don't have .disabled extension
                if (!fileName.endsWith(".disabled")) {
                    scriptNames.add(fileName);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to read scripts directory", e);
        }

        return scriptNames;
    }

    public List<String> getDisabledScriptNames() {
        List<String> scriptNames = new ArrayList<>();
        
        if (!Files.exists(scriptsDirectory)) {
            return scriptNames;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(scriptsDirectory, "*.vsk.disabled")) {
            for (Path scriptFile : stream) {
                // Return just the base name without .disabled
                String fileName = scriptFile.getFileName().toString();
                scriptNames.add(fileName.replace(".disabled", ""));
            }
        } catch (IOException e) {
            logger.error("Failed to read scripts directory", e);
        }

        return scriptNames;
    }

    public boolean disableScript(String scriptName) {
        if (!Files.exists(scriptsDirectory)) {
            logger.warn("Scripts directory does not exist: {}", scriptsDirectory);
            return false;
        }

        Path scriptFile = scriptsDirectory.resolve(scriptName);
        
        if (!Files.exists(scriptFile)) {
            logger.warn("Script file does not exist: {}", scriptName);
            return false;
        }

        Path disabledFile = scriptsDirectory.resolve(scriptName + ".disabled");
        
        try {
            Files.move(scriptFile, disabledFile);
            logger.info("Disabled script: {}", scriptName);
            return true;
        } catch (IOException e) {
            logger.error("Failed to disable script: {}", scriptName, e);
            return false;
        }
    }

    public boolean enableScript(String scriptName) {
        if (!Files.exists(scriptsDirectory)) {
            logger.warn("Scripts directory does not exist: {}", scriptsDirectory);
            return false;
        }

        // Add .disabled if not present for the source file
        String disabledFileName = scriptName.endsWith(".disabled") ? scriptName : scriptName + ".disabled";
        Path disabledFile = scriptsDirectory.resolve(disabledFileName);
        
        if (!Files.exists(disabledFile)) {
            logger.warn("Disabled script file does not exist: {}", disabledFileName);
            return false;
        }

        // Remove .disabled extension for the enabled file
        String enabledFileName = scriptName.endsWith(".disabled") ? scriptName.replace(".disabled", "") : scriptName;
        Path enabledFile = scriptsDirectory.resolve(enabledFileName);
        
        try {
            Files.move(disabledFile, enabledFile);
            logger.info("Enabled script: {}", enabledFileName);
            return true;
        } catch (IOException e) {
            logger.error("Failed to enable script: {}", scriptName, e);
            return false;
        }
    }

    public Path getScriptsDirectory() {
        return scriptsDirectory;
    }
}

