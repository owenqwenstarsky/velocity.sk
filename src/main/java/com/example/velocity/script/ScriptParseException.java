package com.example.velocity.script;

import java.util.ArrayList;
import java.util.List;

public class ScriptParseException extends Exception {
    private final List<ParseError> errors;

    public ScriptParseException(String message) {
        super(message);
        this.errors = new ArrayList<>();
    }

    public ScriptParseException(List<ParseError> errors) {
        super("Script parsing failed with " + errors.size() + " error(s)");
        this.errors = errors;
    }

    public List<ParseError> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public static class ParseError {
        private final int lineNumber;
        private final String line;
        private final String message;
        private final ErrorType type;

        public ParseError(int lineNumber, String line, String message, ErrorType type) {
            this.lineNumber = lineNumber;
            this.line = line;
            this.message = message;
            this.type = type;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getLine() {
            return line;
        }

        public String getMessage() {
            return message;
        }

        public ErrorType getType() {
            return type;
        }

        @Override
        public String toString() {
            return String.format("[Line %d] %s: %s\n    %s", lineNumber, type, message, line.trim());
        }
    }

    public enum ErrorType {
        SYNTAX_ERROR,
        INVALID_COMMAND,
        INVALID_ARGUMENT,
        ORPHANED_TRIGGER,
        ORPHANED_ACTION,
        EMPTY_COMMAND,
        INVALID_TARGET
    }
}


