package com.example.velocity.script.expression;

import com.example.velocity.script.execution.ExecutionContext;

/**
 * Represents a condition that can be evaluated to true or false.
 */
public interface Condition {
    /**
     * Evaluates this condition in the given context.
     * @param context The execution context
     * @return true if the condition is met, false otherwise
     */
    boolean evaluate(ExecutionContext context);
}

