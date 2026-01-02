# Bug Fixes Summary

This document summarizes all bugs found and fixed in the VelocitySk codebase.

## Bugs Fixed

### 1. VskCommand.java - Script Name Resolution After Enabling
**File:** `src/main/java/com/example/velocity/command/VskCommand.java`
**Lines:** 185-199
**Severity:** High

**Issue:** 
When enabling a script via `/vsk enable <script.vsk.disabled>`, the enable command would successfully rename the file from `script.vsk.disabled` to `script.vsk`, but then tried to load the script using the original name with `.disabled` extension, causing a file-not-found error.

**Fix:**
Strip the `.disabled` extension from the script name before attempting to load it:
```java
String enabledScriptName = scriptName.endsWith(".disabled") ? scriptName.replace(".disabled", "") : scriptName;
Script script = scriptLoader.loadSingleScript(enabledScriptName);
```

**Impact:** Commands `/vsk enable` now work correctly.

---

### 2. CommandManager.java - Redundant Command Registration
**File:** `src/main/java/com/example/velocity/script/CommandManager.java`
**Lines:** 54-70
**Severity:** Medium

**Issue:**
When registering commands with Velocity, the code was passing the command name as both the first parameter (primary alias) and in the varargs parameter (additional aliases), potentially causing the command to be registered twice or unexpected behavior.

```java
// Before (incorrect)
server.getCommandManager().register(commandName, new ScriptCommand(commandScript), commandName);

// After (correct)
server.getCommandManager().register(commandName, new ScriptCommand(commandScript));
```

**Fix:**
Remove the redundant command name from the varargs parameter. The same fix was applied to alias registration.

**Impact:** Cleaner command registration, prevents potential duplicate registration issues.

---

### 3. ScriptLoader.java - Redundant Filter in getEnabledScriptNames()
**File:** `src/main/java/com/example/velocity/script/ScriptLoader.java`
**Lines:** 167-187
**Severity:** Low (Code Quality)

**Issue:**
The method used `DirectoryStream` with pattern `"*.vsk"` which already excludes `"*.vsk.disabled"` files, but then had an additional filter checking `!fileName.endsWith(".disabled")`. This filter was redundant since the glob pattern already excludes those files.

**Fix:**
Removed the redundant filter and added a comment explaining that the glob pattern already excludes disabled scripts:
```java
// The "*.vsk" pattern already excludes "*.vsk.disabled" files
scriptNames.add(fileName);
```

**Impact:** Cleaner, more efficient code with better documentation.

---

### 4. VariableReplacer.java - Potential NullPointerException
**File:** `src/main/java/com/example/velocity/script/VariableReplacer.java`
**Lines:** 17-35, 77-95
**Severity:** High

**Issue:**
Both the `replace()` and `getAvailableVariables()` methods called methods on the `executor` (Player) parameter without null-checking first. This would cause a NullPointerException if the executor was null, which could happen in certain event contexts or system-triggered actions.

```java
// Before (vulnerable to NPE)
result = result.replace("{player}", executor.getUsername());
result = result.replace("{player's name}", executor.getUsername());
// ...

// After (null-safe)
if (executor != null) {
    result = result.replace("{player}", executor.getUsername());
    result = result.replace("{player's name}", executor.getUsername());
    // ...
}
```

**Fix:**
Added null checks before accessing executor methods in both places.

**Impact:** Prevents crashes in edge cases where no player is associated with the execution context.

---

## Testing

All fixes have been verified to:
1. Maintain backward compatibility
2. Not introduce new compilation errors
3. Follow existing code style and patterns

## Files Modified

- `src/main/java/com/example/velocity/command/VskCommand.java`
- `src/main/java/com/example/velocity/script/CommandManager.java`
- `src/main/java/com/example/velocity/script/ScriptLoader.java`
- `src/main/java/com/example/velocity/script/VariableReplacer.java`

## Summary

- **Total bugs fixed:** 4 (5 locations)
- **High severity:** 2
- **Medium severity:** 1
- **Low severity:** 1
- **Lines changed:** ~38 insertions, ~36 deletions

All bugs have been successfully fixed and the code is now more robust and maintainable.
