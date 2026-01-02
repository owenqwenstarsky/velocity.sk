package com.example.velocity.script;

import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptParser {
    private final Logger logger;
    
    private static final Pattern COMMAND_PATTERN = Pattern.compile("^command\\s+/([a-zA-Z0-9_]+)(?:\\s+(.*))?:");
    private static final Pattern ARG_PATTERN = Pattern.compile("<([a-zA-Z0-9_]+)>");
    private static final Pattern TRIGGER_PATTERN = Pattern.compile("^\\s+trigger:");
    private static final Pattern SEND_PATTERN = Pattern.compile("^\\s+send\\s+\"([^\"]+)\"(?:\\s+to\\s+(.+))?");

    public ScriptParser(Logger logger) {
        this.logger = logger;
    }

    public Script parse(Path scriptFile) throws IOException, ScriptParseException {
        Script script = new Script(scriptFile.getFileName().toString());
        List<ScriptParseException.ParseError> errors = new ArrayList<>();
        
        try (BufferedReader reader = Files.newBufferedReader(scriptFile)) {
            String line;
            Script.CommandScript currentCommand = null;
            boolean inTrigger = false;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmedLine = line.trim();
                
                // Skip empty lines and comments
                if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                    continue;
                }

                // Check for command definition
                Matcher commandMatcher = COMMAND_PATTERN.matcher(line);
                if (commandMatcher.find()) {
                    // Validate previous command had actions
                    if (currentCommand != null && currentCommand.getActions().isEmpty()) {
                        errors.add(new ScriptParseException.ParseError(
                            lineNumber - 1,
                            "command /" + currentCommand.getCommandName(),
                            "Command has no actions defined",
                            ScriptParseException.ErrorType.EMPTY_COMMAND
                        ));
                    }
                    
                    String commandName = commandMatcher.group(1);
                    String argsSection = commandMatcher.group(2);
                    
                    // Validate command name
                    if (!commandName.matches("[a-zA-Z0-9_]+")) {
                        errors.add(new ScriptParseException.ParseError(
                            lineNumber,
                            line,
                            "Invalid command name. Use only letters, numbers, and underscores",
                            ScriptParseException.ErrorType.INVALID_COMMAND
                        ));
                    }
                    
                    // Parse arguments
                    List<String> arguments = new ArrayList<>();
                    if (argsSection != null && !argsSection.trim().isEmpty()) {
                        String cleanArgs = argsSection.trim();
                        if (!cleanArgs.isEmpty() && !cleanArgs.equals(":")) {
                            Matcher argMatcher = ARG_PATTERN.matcher(argsSection);
                            while (argMatcher.find()) {
                                String argName = argMatcher.group(1);
                                if (!argName.matches("[a-zA-Z0-9_]+")) {
                                    errors.add(new ScriptParseException.ParseError(
                                        lineNumber,
                                        line,
                                        "Invalid argument name: " + argName + ". Use only letters, numbers, and underscores",
                                        ScriptParseException.ErrorType.INVALID_ARGUMENT
                                    ));
                                }
                                arguments.add(argName);
                            }
                        }
                    }
                    
                    currentCommand = new Script.CommandScript(commandName, arguments);
                    script.addCommand(currentCommand);
                    inTrigger = false;
                    logger.debug("Found command: /{} with {} argument(s)", commandName, arguments.size());
                    continue;
                }

                // Check for trigger section
                Matcher triggerMatcher = TRIGGER_PATTERN.matcher(line);
                if (triggerMatcher.find()) {
                    if (currentCommand == null) {
                        errors.add(new ScriptParseException.ParseError(
                            lineNumber,
                            line,
                            "Trigger section found without a command definition",
                            ScriptParseException.ErrorType.ORPHANED_TRIGGER
                        ));
                        continue;
                    }
                    inTrigger = true;
                    logger.debug("Found trigger for command: /{}", currentCommand.getCommandName());
                    continue;
                }

                // Check for send action
                Matcher sendMatcher = SEND_PATTERN.matcher(line);
                if (sendMatcher.find()) {
                    if (currentCommand == null) {
                        errors.add(new ScriptParseException.ParseError(
                            lineNumber,
                            line,
                            "Action found without a command definition",
                            ScriptParseException.ErrorType.ORPHANED_ACTION
                        ));
                        continue;
                    }
                    
                    if (!inTrigger) {
                        errors.add(new ScriptParseException.ParseError(
                            lineNumber,
                            line,
                            "Action found outside of trigger section",
                            ScriptParseException.ErrorType.ORPHANED_ACTION
                        ));
                        continue;
                    }

                    String message = sendMatcher.group(1);
                    String target = sendMatcher.group(2);

                    Script.MessageTarget messageTarget;
                    String targetPlayer = null;

                    if (target == null || target.trim().equals("player")) {
                        messageTarget = Script.MessageTarget.PLAYER;
                    } else if (target.trim().equals("all players")) {
                        messageTarget = Script.MessageTarget.ALL_PLAYERS;
                    } else {
                        messageTarget = Script.MessageTarget.SPECIFIC_PLAYER;
                        targetPlayer = target.trim();
                    }

                    Script.Action action = new Script.Action(
                        Script.ActionType.SEND_MESSAGE,
                        message,
                        messageTarget,
                        targetPlayer
                    );
                    currentCommand.addAction(action);
                    logger.debug("Added send action: \"{}\" to {}", message, target == null ? "player" : target);
                    continue;
                }
                
                // If we got here, the line didn't match any pattern
                if (trimmedLine.startsWith("send ")) {
                    errors.add(new ScriptParseException.ParseError(
                        lineNumber,
                        line,
                        "Invalid send syntax. Expected: send \"message\" [to <target>]",
                        ScriptParseException.ErrorType.SYNTAX_ERROR
                    ));
                } else if (!trimmedLine.isEmpty()) {
                    errors.add(new ScriptParseException.ParseError(
                        lineNumber,
                        line,
                        "Unrecognized syntax",
                        ScriptParseException.ErrorType.SYNTAX_ERROR
                    ));
                }
            }
            
            // Validate last command had actions
            if (currentCommand != null && currentCommand.getActions().isEmpty()) {
                errors.add(new ScriptParseException.ParseError(
                    lineNumber,
                    "command /" + currentCommand.getCommandName(),
                    "Command has no actions defined",
                    ScriptParseException.ErrorType.EMPTY_COMMAND
                ));
            }
        }

        // If there were errors, throw exception
        if (!errors.isEmpty()) {
            logger.error("Script {} has {} parsing error(s)", scriptFile.getFileName(), errors.size());
            throw new ScriptParseException(errors);
        }

        logger.info("Parsed script: {} with {} command(s)", script.getName(), script.getCommands().size());
        return script;
    }
}

