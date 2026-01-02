package com.example.velocity.script.expression;

import com.example.velocity.script.execution.ExecutionContext;

/**
 * Represents an expression that can be evaluated at runtime to produce a value.
 */
public interface Expression {
    /**
     * Evaluates this expression in the given context and returns its value.
     * @param context The execution context
     * @return The evaluated value as a String, or null if evaluation fails
     */
    String evaluate(ExecutionContext context);
}

