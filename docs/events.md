# Events

Events allow your scripts to automatically react to things that happen on your proxy, such as players joining, quitting, or switching servers.

## Event Syntax

```skript
on <event>:
    <actions>
```

Unlike commands, events don't need a `trigger:` section. Actions go directly under the event declaration.

## Available Events

### On Join

Triggered when a player successfully logs into the proxy:

```skript
on join:
    send "§eWelcome to the network, %player%!" to player
    send "§7%player% joined the network" to all players
```

**Available Variables:**
- `%player%` - The player's name
- `%uuid%` - The player's UUID

### On Quit

Triggered when a player disconnects from the proxy:

```skript
on quit:
    send "§7%player% left the network" to all players
    delete {playtime::%player%}
```

**Available Variables:**
- `%player%` - The player's name
- `%uuid%` - The player's UUID

### On Server Switch

Triggered when a player successfully switches from one backend server to another:

```skript
on server switch:
    send "§eYou moved from %from-server% to %to-server%" to player
    if %to-server% is "minigames":
        send "§aWelcome to the minigames server!" to player
```

**Available Variables:**
- `%player%` - The player's name
- `%from-server%` - The server they came from
- `%to-server%` - The server they're now on

### On Chat

Triggered when a player sends a chat message:

```skript
on chat:
    if %message% contains "discord":
        send "§bJoin our Discord: discord.gg/example" to player
```

**Available Variables:**
- `%player%` - The player who sent the message
- `%message%` - The chat message content

### On Server Connect

Triggered **before** a player connects to a backend server (can be cancelled in the future):

```skript
on server connect:
    send "§7Connecting you to %target-server%..." to player
```

**Available Variables:**
- `%player%` - The player connecting
- `%target-server%` - The server they're connecting to

## Examples

### Welcome Message with First Join Detection

```skript
on join:
    if {firstjoin::%player%} is not set:
        send "§6§lWELCOME!" to player
        send "§eThis is your first time joining the server!" to player
        send "§aWelcome %player% to the server for the first time!" to all players
        set {firstjoin::%player%} to "true"
    else:
        send "§eWelcome back, %player%!" to player
```

### Server-Specific Welcome Messages

```skript
on server switch:
    if %to-server% is "survival":
        send "§2Welcome to Survival!" to player
        send "§7Type /help for a list of commands" to player
    
    if %to-server% is "creative":
        send "§bWelcome to Creative!" to player
        send "§7Build whatever you can imagine!" to player
```

### Chat Filter

```skript
on chat:
    if %message% contains "badword":
        send "§cPlease watch your language!" to player
        # Note: This doesn't cancel the message in current implementation
        # Future updates will add cancellation support
```

### Auto-Redirect from Full Server

```skript
on server connect:
    if %target-server% is "lobby1":
        # Check if lobby is full (requires additional implementation)
        send "§eConnecting to %target-server%..." to player
```

### Track Play Time

```skript
on join:
    set {jointime::%player%} to "now"

on quit:
    # This is a simplified example
    # Full implementation would require time calculation
    delete {jointime::%player%}
```

### Broadcast Server Changes

```skript
on server switch:
    if %from-server% is not "none":
        send "§7[§e%player%§7] %from-server% → %to-server%" to all players in server "staff"
```

### First-Time Orientation

```skript
on join:
    if {completed_tutorial::%player%} is not set:
        transfer player to "tutorial"
        send "§aWelcome! Complete the tutorial to access the full server." to player
```

## Combining Events with Variables

Events become powerful when combined with variables:

```skript
on join:
    if {banned::%player%} is set:
        send "§cYour account has been suspended." to player
        # Note: Kicking requires additional implementation
    
    set {online::%player%} to "true"

on quit:
    delete {online::%player%}
```

## Event + Command Integration

```skript
on join:
    set {lastjoin::%player%} to "current time"

command /lastseen <player>:
    trigger:
        if {lastjoin::%player%} is set:
            send "%player% was last seen: {lastjoin::%player%}" to player
        else:
            send "Player not found or never joined." to player
```

## Best Practices

1. **Keep events fast**: Events are triggered frequently, so avoid heavy operations
2. **Use conditionals**: Check conditions before performing actions (see [Conditionals](conditionals.md))
3. **Clean up variables**: Delete temporary variables in quit events to save memory
4. **Test thoroughly**: Events can affect all players, so test carefully
5. **Avoid spam**: Don't send too many messages in high-frequency events like `on chat`

## Event Order

Events are processed in the order they appear in your scripts. If you have multiple scripts with the same event, they'll all execute in the order the scripts were loaded.

## Common Pitfalls

1. **Using command placeholders in events**: `%arg-1%` doesn't exist in events, only `%player%` and event-specific variables
2. **Forgetting to clean up**: Variables set in `on join` should usually be cleaned in `on quit`
3. **Message spam**: Be careful with `send` in `on chat` - it can create message loops

