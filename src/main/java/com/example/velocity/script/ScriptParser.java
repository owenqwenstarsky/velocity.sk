package com.example.velocity.script;

import com.example.velocity.script.event.EventTrigger;
import com.example.velocity.script.expression.Condition;
import com.example.velocity.script.expression.ConditionParser;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptParser {
    private final Logger logger;
    
    // Patterns
    private static final Pattern COMMAND_PATTERN = Pattern.compile("^command\\s+/([a-zA-Z0-9_]+)(?:\\s+(.*))?:");
    private static final Pattern ARG_PATTERN = Pattern.compile("<([a-zA-Z0-9_]+)>");
    private static final Pattern EVENT_PATTERN = Pattern.compile("^on\\s+(join|quit|server\\s+switch|chat|server\\s+connect):");
    private static final Pattern TRIGGER_PATTERN = Pattern.compile("^\\s+trigger:");
    private static final Pattern METADATA_PATTERN = Pattern.compile("^\\s+(permission|permission message|aliases|usage|description):\\s*(.*)");
    private static final Pattern SEND_PATTERN = Pattern.compile("^send\\s+\"([^\"]+)\"(?:\\s+to\\s+(.+))?");
    private static final Pattern TRANSFER_PATTERN = Pattern.compile("^transfer\\s+(.+?)\\s+to\\s+\"([^\"]+)\"");
    private static final Pattern SET_VAR_PATTERN = Pattern.compile("^set\\s+(\\{[^}]+\\})\\s+to\\s+(.+)");
    private static final Pattern DELETE_VAR_PATTERN = Pattern.compile("^delete\\s+(\\{[^}]+\\})");
    private static final Pattern IF_PATTERN = Pattern.compile("^if\\s+(.+):");
    private static final Pattern ELSE_IF_PATTERN = Pattern.compile("^else\\s+if\\s+(.+):");
    private static final Pattern ELSE_PATTERN = Pattern.compile("^else:");

    public ScriptParser(Logger logger) {
        this.logger = logger;
    }

    public Script parse(Path scriptFile) throws IOException, ScriptParseException {
        Script script = new Script(scriptFile.getFileName().toString());
        List<ScriptParseException.ParseError> errors = new ArrayList<>();
        
        try (BufferedReader reader = Files.newBufferedReader(scriptFile)) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            
            parseLines(script, lines, errors);
        }

        if (!errors.isEmpty()) {
            logger.error("Script {} has {} parsing error(s)", scriptFile.getFileName(), errors.size());
            throw new ScriptParseException(errors);
        }

        logger.info("Parsed script: {} with {} command(s) and {} event(s)", 
                    script.getName(), script.getCommands().size(), script.getEventTriggers().size());
        return script;
    }

    private void parseLines(Script script, List<String> lines, List<ScriptParseException.ParseError> errors) {
        int lineNumber = 0;
        Script.CommandScript currentCommand = null;
        EventTrigger currentEvent = null;
        boolean inTriggerSection = false;

        while (lineNumber < lines.size()) {
            String line = lines.get(lineNumber);
            String trimmedLine = line.trim();
            int currentLineNumber = lineNumber + 1;
            lineNumber++;

            // Skip empty lines and comments
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                continue;
            }

            // Check for command definition
            Matcher commandMatcher = COMMAND_PATTERN.matcher(line);
            if (commandMatcher.matches()) {
                if (currentCommand != null && currentCommand.getActions().isEmpty()) {
                    errors.add(new ScriptParseException.ParseError(
                        currentLineNumber - 1,
                        "command /" + currentCommand.getCommandName(),
                        "Command has no actions defined",
                        ScriptParseException.ErrorType.EMPTY_COMMAND
                    ));
                }

                String commandName = commandMatcher.group(1);
                String argsSection = commandMatcher.group(2);

                List<String> arguments = parseArguments(argsSection, currentLineNumber, line, errors);
                currentCommand = new Script.CommandScript(commandName, arguments);
                script.addCommand(currentCommand);
                currentEvent = null;
                inTriggerSection = false;
                logger.debug("Found command: /{} with {} argument(s)", commandName, arguments.size());
                continue;
            }

            // Check for event definition
            Matcher eventMatcher = EVENT_PATTERN.matcher(line);
            if (eventMatcher.matches()) {
                if (currentEvent != null && currentEvent.getActions().isEmpty()) {
                    errors.add(new ScriptParseException.ParseError(
                        currentLineNumber - 1,
                        "on " + currentEvent.getEventType().name().toLowerCase(),
                        "Event has no actions defined",
                        ScriptParseException.ErrorType.EMPTY_COMMAND
                    ));
                }

                String eventName = eventMatcher.group(1).trim().toLowerCase();
                EventTrigger.EventType eventType = parseEventType(eventName);
                
                if (eventType != null) {
                    currentEvent = new EventTrigger(eventType, script.getName());
                    script.addEventTrigger(currentEvent);
                    currentCommand = null;
                    inTriggerSection = true;
                    logger.debug("Found event trigger: {}", eventType);
                } else {
                    errors.add(new ScriptParseException.ParseError(
                        currentLineNumber,
                        line,
                        "Unknown event type: " + eventName,
                        ScriptParseException.ErrorType.SYNTAX_ERROR
                    ));
                }
                continue;
            }

            // Check for trigger section
            Matcher triggerMatcher = TRIGGER_PATTERN.matcher(line);
            if (triggerMatcher.matches()) {
                if (currentCommand == null) {
                    errors.add(new ScriptParseException.ParseError(
                        currentLineNumber,
                        line,
                        "Trigger section found without a command definition",
                        ScriptParseException.ErrorType.ORPHANED_TRIGGER
                    ));
                    continue;
                }
                inTriggerSection = true;
                logger.debug("Found trigger for command: /{}", currentCommand.getCommandName());
                continue;
            }

            // Check for command metadata
            if (currentCommand != null && !inTriggerSection) {
                Matcher metadataMatcher = METADATA_PATTERN.matcher(line);
                if (metadataMatcher.matches()) {
                    String metadataType = metadataMatcher.group(1).trim();
                    String metadataValue = metadataMatcher.group(2).trim();
                    parseMetadata(currentCommand, metadataType, metadataValue);
                    continue;
                }
            }

            // Parse actions (must be indented)
            if (line.startsWith("\t") || line.startsWith("    ")) {
                if (!inTriggerSection && currentEvent == null) {
                    errors.add(new ScriptParseException.ParseError(
                        currentLineNumber,
                        line,
                        "Action found outside of trigger section",
                        ScriptParseException.ErrorType.ORPHANED_ACTION
                    ));
                    continue;
                }

                // Parse action and add to current command or event
                List<Script.Action> targetActions = currentCommand != null ? 
                    currentCommand.getActions() : 
                    (currentEvent != null ? currentEvent.getActions() : null);

                if (targetActions != null) {
                    Script.Action action = parseAction(trimmedLine, lines, lineNumber, currentLineNumber, errors);
                    if (action != null) {
                        targetActions.add(action);
                        
                        // If it's a conditional, skip the lines it consumed
                        if (action instanceof Script.ConditionalAction) {
                            int consumed = countConditionalLines(lines, lineNumber - 1);
                            lineNumber += consumed;
                        }
                    }
                }
            } else if (!trimmedLine.isEmpty()) {
                errors.add(new ScriptParseException.ParseError(
                    currentLineNumber,
                    line,
                    "Unrecognized syntax or missing indentation",
                    ScriptParseException.ErrorType.SYNTAX_ERROR
                ));
            }
        }

        // Validate last command/event had actions
        if (currentCommand != null && currentCommand.getActions().isEmpty()) {
            errors.add(new ScriptParseException.ParseError(
                lines.size(),
                "command /" + currentCommand.getCommandName(),
                "Command has no actions defined",
                ScriptParseException.ErrorType.EMPTY_COMMAND
            ));
        }
        if (currentEvent != null && currentEvent.getActions().isEmpty()) {
            errors.add(new ScriptParseException.ParseError(
                lines.size(),
                "on " + currentEvent.getEventType().name().toLowerCase(),
                "Event has no actions defined",
                ScriptParseException.ErrorType.EMPTY_COMMAND
            ));
        }
    }

    private List<String> parseArguments(String argsSection, int lineNumber, String line, 
                                       List<ScriptParseException.ParseError> errors) {
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
                            "Invalid argument name: " + argName,
                            ScriptParseException.ErrorType.INVALID_ARGUMENT
                        ));
                    }
                    arguments.add(argName);
                }
            }
        }
        return arguments;
    }

    private EventTrigger.EventType parseEventType(String eventName) {
        return switch (eventName) {
            case "join" -> EventTrigger.EventType.JOIN;
            case "quit" -> EventTrigger.EventType.QUIT;
            case "server switch" -> EventTrigger.EventType.SERVER_SWITCH;
            case "chat" -> EventTrigger.EventType.CHAT;
            case "server connect" -> EventTrigger.EventType.SERVER_CONNECT;
            default -> null;
        };
    }

    private void parseMetadata(Script.CommandScript command, String type, String value) {
        switch (type) {
            case "permission" -> command.setPermission(value);
            case "permission message" -> command.setPermissionMessage(value);
            case "aliases" -> {
                List<String> aliases = new ArrayList<>();
                for (String alias : value.split(",")) {
                    String cleaned = alias.trim();
                    if (cleaned.startsWith("/")) {
                        cleaned = cleaned.substring(1);
                    }
                    if (!cleaned.isEmpty()) {
                        aliases.add(cleaned);
                    }
                }
                command.setAliases(aliases);
            }
            case "usage" -> command.setUsage(value);
            case "description" -> command.setDescription(value);
        }
    }

    private Script.Action parseAction(String trimmedLine, List<String> allLines, int currentIndex, 
                                      int lineNumber, List<ScriptParseException.ParseError> errors) {
        // Skip else/else-if lines - they're handled by parseConditional
        if (ELSE_PATTERN.matcher(trimmedLine).matches() || ELSE_IF_PATTERN.matcher(trimmedLine).matches()) {
            return null;
        }

        // Check for if statement
        Matcher ifMatcher = IF_PATTERN.matcher(trimmedLine);
        if (ifMatcher.matches()) {
            return parseConditional(allLines, currentIndex, lineNumber, errors);
        }

        // Check for send action
        Matcher sendMatcher = SEND_PATTERN.matcher(trimmedLine);
        if (sendMatcher.matches()) {
            return parseSendAction(sendMatcher);
        }

        // Check for transfer action
        Matcher transferMatcher = TRANSFER_PATTERN.matcher(trimmedLine);
        if (transferMatcher.matches()) {
            return parseTransferAction(transferMatcher);
        }

        // Check for set variable action
        Matcher setVarMatcher = SET_VAR_PATTERN.matcher(trimmedLine);
        if (setVarMatcher.matches()) {
            return parseSetVariableAction(setVarMatcher);
        }

        // Check for delete variable action
        Matcher deleteVarMatcher = DELETE_VAR_PATTERN.matcher(trimmedLine);
        if (deleteVarMatcher.matches()) {
            return parseDeleteVariableAction(deleteVarMatcher);
        }

        // Unrecognized action
        errors.add(new ScriptParseException.ParseError(
            lineNumber,
            trimmedLine,
            "Unrecognized action syntax",
            ScriptParseException.ErrorType.SYNTAX_ERROR
        ));

        return null;
    }

    private Script.Action parseSendAction(Matcher matcher) {
        String message = matcher.group(1);
        String target = matcher.group(2);

        Map<String, String> data = new HashMap<>();
        data.put("message", message);

        if (target == null || target.trim().equals("player")) {
            data.put("target", Script.MessageTarget.PLAYER.name());
        } else if (target.trim().equals("all players")) {
            data.put("target", Script.MessageTarget.ALL_PLAYERS.name());
        } else if (target.trim().startsWith("all players in server")) {
            // Extract server name: "all players in server \"name\""
            Pattern serverPattern = Pattern.compile("all players in server \"([^\"]+)\"");
            Matcher serverMatcher = serverPattern.matcher(target.trim());
            if (serverMatcher.matches()) {
                data.put("target", Script.MessageTarget.SERVER_PLAYERS.name());
                data.put("serverName", serverMatcher.group(1));
                return new Script.Action(Script.ActionType.SEND_TO_SERVER_PLAYERS, data);
            }
        } else {
            data.put("target", Script.MessageTarget.SPECIFIC_PLAYER.name());
            data.put("targetPlayer", target.trim());
        }

        return new Script.Action(Script.ActionType.SEND_MESSAGE, data);
    }

    private Script.Action parseTransferAction(Matcher matcher) {
        String playerExpr = matcher.group(1).trim();
        String serverName = matcher.group(2).trim();

        Map<String, String> data = new HashMap<>();
        data.put("playerExpr", playerExpr);
        data.put("serverName", serverName);

        return new Script.Action(Script.ActionType.TRANSFER_PLAYER, data);
    }

    private Script.Action parseSetVariableAction(Matcher matcher) {
        String variableName = matcher.group(1).trim();
        String variableValue = matcher.group(2).trim();
        
        // Strip quotes from string literals
        if (variableValue.startsWith("\"") && variableValue.endsWith("\"") && variableValue.length() >= 2) {
            variableValue = variableValue.substring(1, variableValue.length() - 1);
        }

        Map<String, String> data = new HashMap<>();
        data.put("variableName", variableName);
        data.put("variableValue", variableValue);

        return new Script.Action(Script.ActionType.SET_VARIABLE, data);
    }

    private Script.Action parseDeleteVariableAction(Matcher matcher) {
        String variableName = matcher.group(1).trim();

        Map<String, String> data = new HashMap<>();
        data.put("variableName", variableName);

        return new Script.Action(Script.ActionType.DELETE_VARIABLE, data);
    }

    private Script.ConditionalAction parseConditional(List<String> lines, int startIndex, int lineNumber,
                                                     List<ScriptParseException.ParseError> errors) {
        String firstLine = lines.get(startIndex - 1).trim();
        Matcher ifMatcher = IF_PATTERN.matcher(firstLine);
        
        if (!ifMatcher.matches()) {
            return null;
        }

        String conditionText = ifMatcher.group(1).trim();
        Condition condition = ConditionParser.parse(conditionText);

        List<Script.Action> ifActions = new ArrayList<>();
        List<Script.Action> elseActions = new ArrayList<>();

        // Parse if block
        int baseIndent = getIndentLevel(lines.get(startIndex - 1));
        int i = startIndex;
        
        while (i < lines.size()) {
            String line = lines.get(i);
            int indent = getIndentLevel(line);
            String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                i++;
                continue;
            }

            // Check for else or else if at same indent level
            if (indent == baseIndent) {
                if (ELSE_PATTERN.matcher(trimmed).matches() || ELSE_IF_PATTERN.matcher(trimmed).matches()) {
                    break;
                }
            }

            // If indent is less than or equal to base, we're done with this conditional
            if (indent <= baseIndent && i > startIndex) {
                break;
            }

            // Parse action at one level deeper than the if statement
            if (indent == baseIndent + 1) {
                Script.Action action = parseAction(trimmed, lines, i + 1, lineNumber + i - startIndex + 1, errors);
                if (action != null) {
                    ifActions.add(action);
                }
            }

            i++;
        }

        // Check for else block
        if (i < lines.size()) {
            String line = lines.get(i);
            int indent = getIndentLevel(line);
            String trimmed = line.trim();

            if (indent == baseIndent && ELSE_PATTERN.matcher(trimmed).matches()) {
                i++; // Move past else line
                
                // Parse else block
                while (i < lines.size()) {
                    line = lines.get(i);
                    indent = getIndentLevel(line);
                    trimmed = line.trim();

                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        i++;
                        continue;
                    }

                    if (indent <= baseIndent) {
                        break;
                    }

                    if (indent == baseIndent + 1) {
                        Script.Action action = parseAction(trimmed, lines, i + 1, lineNumber + i - startIndex + 1, errors);
                        if (action != null) {
                            elseActions.add(action);
                        }
                    }

                    i++;
                }
            }
        }

        return new Script.ConditionalAction(condition, ifActions, elseActions);
    }

    private int getIndentLevel(String line) {
        int indent = 0;
        for (char c : line.toCharArray()) {
            if (c == '\t') {
                indent++;
            } else if (c == ' ') {
                // Count 4 spaces as 1 indent level
                indent += 0.25;
            } else {
                break;
            }
        }
        return (int) indent;
    }

    private int countConditionalLines(List<String> lines, int startIndex) {
        int baseIndent = getIndentLevel(lines.get(startIndex));
        int count = 0;

        for (int i = startIndex + 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();
            
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                count++;
                continue;
            }

            int indent = getIndentLevel(line);
            
            // Check for else/else-if at the same indent level (part of this conditional)
            if (indent == baseIndent) {
                if (ELSE_PATTERN.matcher(trimmed).matches() || ELSE_IF_PATTERN.matcher(trimmed).matches()) {
                    count++;
                    continue;
                }
                // Different statement at same indent - we're done
                break;
            }
            
            if (indent < baseIndent) {
                break;
            }

            count++;
        }

        return count;
    }
}
