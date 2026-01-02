package com.example.velocity.script;

import java.util.List;

public class LoadResult {
    private final List<Script> scripts;
    private final boolean hadErrors;
    private final int errorCount;

    public LoadResult(List<Script> scripts, boolean hadErrors, int errorCount) {
        this.scripts = scripts;
        this.hadErrors = hadErrors;
        this.errorCount = errorCount;
    }

    public List<Script> getScripts() {
        return scripts;
    }

    public boolean hadErrors() {
        return hadErrors;
    }

    public int getErrorCount() {
        return errorCount;
    }
}


