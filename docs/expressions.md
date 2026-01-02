# Expressions

Expressions are values that can be evaluated at runtime. They're used in conditions, variable assignments, messages, and more.

## Types of Expressions

### Literal Strings

Text wrapped in double quotes:

```skript
"Hello, world!"
"This is a message"
"100"
```

### Player Information

Information about the player executing a command or triggering an event:

#### Player Name

```skript
player
player's name
```

Both return the player's username.

Example:
```skript
send "Your name is {player}" to player
```

#### Player UUID

```skript
player's uuid
```

Returns the player's unique identifier.

Example:
```skript
send "Your UUID: {player's uuid}" to player
```

#### Player's Server

```skript
player's server
```

Returns the name of the server the player is currently on.

Example:
```skript
send "You are on: {player's server}" to player
```

### Variables

Variables can be used as expressions:

```skript
{variable}          # Global variable
{_variable}         # Local variable
{list::key}         # List entry
```

Example:
```skript
send "Your coins: {coins::%player%}" to player
set {_temp} to {another-variable}
```

### Command Arguments

Access arguments passed to commands:

```skript
%arg-1%            # First argument
%arg-2%            # Second argument
%<name>%           # Argument by name
```

Example:
```skript
command /greet <name>:
    trigger:
        send "Hello, %name%!" to player
        send "Hello, %arg-1%!" to player  # Same thing
```

### Event Data

Special placeholders available in events:

#### Join Event

```skript
%player%           # Player name
%uuid%             # Player UUID
```

#### Quit Event

```skript
%player%           # Player name
%uuid%             # Player UUID
%quit-message%     # Quit message (often empty in Velocity)
```

#### Server Switch Event

```skript
%player%           # Player name
%from-server%      # Previous server
%to-server%        # New server
```

#### Chat Event

```skript
%player%           # Player who sent the message
%message%          # The chat message
```

#### Server Connect Event

```skript
%player%           # Player connecting
%target-server%    # Server they're connecting to
```

## Using Expressions

### In Messages

Expressions must be wrapped in `{}` when used inside quoted strings:

```skript
send "Welcome, {player}!" to player
send "You have {coins::%player%} coins" to player
send "Server: {player's server}" to player
```

**Note:** Without `{}`, expressions like `player` or `player's server` will be sent as literal text.

### In Conditions

Expressions can be compared:

```skript
if player's server is "lobby":
    send "You're in the lobby!" to player

if {rank::%player%} is "vip":
    send "You're VIP!" to player

if %message% contains "help":
    send "Type /help" to player
```

### In Variable Assignments

Store expression results in variables:

```skript
set {name::%player%} to player
set {current-server::%player%} to player's server
set {message-copy} to %message%
```

## Expression Examples

### Player Information

```skript
command /whoami:
    trigger:
        send "§eName: {player}" to player
        send "§eUUID: {player's uuid}" to player
        send "§eServer: {player's server}" to player
```

### Variable Expressions

```skript
command /balance:
    trigger:
        if {coins::%player%} is set:
            send "§eCoins: §6{coins::%player%}" to player
        else:
            send "§eCoins: §60" to player
```

### Copying Data

```skript
command /backup:
    trigger:
        set {backup::name::%player%} to player
        set {backup::server::%player%} to player's server
        send "§aBackup created!" to player
```

### Dynamic Messages

```skript
on server switch:
    if %from-server% is not "none":
        send "§7You moved from §e%from-server% §7to §e%to-server%" to player
```

### Argument Manipulation

```skript
command /echo <message>:
    trigger:
        send "You said: %message%" to player
        set {last-message::%player%} to "%message%"
```

## Combining Expressions

You can combine multiple expressions in a single action:

```skript
command /status:
    trigger:
        send "§6=== Player Status ===" to player
        send "§eName: {player}" to player
        send "§eServer: {player's server}" to player
        send "§eCoins: {coins::%player%}" to player
        send "§eRank: {rank::%player%}" to player
```

## Expression Evaluation Order

Expressions are evaluated when they're used:

```skript
command /test:
    trigger:
        set {_name} to player              # Stores current player name
        send "Stored name: {_name}" to player
        
        set {_server} to player's server   # Stores current server (no {} needed outside quotes)
        send "Stored server: {_server}" to player
```

## Advanced Examples

### Server-Specific Messages

```skript
on join:
    set {_current} to player's server    # No {} needed outside quotes
    
    if {_current} is "lobby":
        send "§aWelcome to the lobby!" to player
    else if {_current} is "survival":
        send "§2Welcome to Survival mode!" to player
    else:
        send "§eWelcome to {_current}!" to player
```

### Tracking Player Movement

```skript
on server switch:
    set {history::%player%::previous} to %from-server%
    set {history::%player%::current} to %to-server%

command /whereami:
    trigger:
        send "§eCurrent: {player's server}" to player
        if {history::%player%::previous} is set:
            send "§ePrevious: {history::%player%::previous}" to player
```

### Chat Logger

```skript
on chat:
    set {chat-log::%player%::last} to %message%
    send "§7[LOG] {player}: %message%" to all players in server "staff"
```

### Dynamic Variable Names

```skript
command /setdata <key> <value>:
    trigger:
        set {data::%player%::%key%} to "%value%"
        send "§aSet %key% = %value%" to player

command /getdata <key>:
    trigger:
        if {data::%player%::%key%} is set:
            send "§e%key% = {data::%player%::%key%}" to player
        else:
            send "§cKey '%key%' not found" to player
```

## Placeholder Summary

### Always Available

- `{player}` or `{player's name}` - Player username (use `{}` in strings)
- `{player's uuid}` - Player UUID (use `{}` in strings)
- `{player's server}` - Current server name (use `{}` in strings)
- `{variable}` - Any global variable
- `{_variable}` - Any local variable

**Note:** When these expressions are used outside of quoted strings (e.g., in `set` statements or as targets), you don't need `{}`:
```skript
set {_name} to player           # No {} needed
send "Hello" to player          # target, no {} needed
send "Hi {player}!" to player   # inside string, {} required
```

### In Commands Only

- `%arg-1%`, `%arg-2%`, etc. - Command arguments
- `%<name>%` - Named command arguments

### In Events Only

Event-specific placeholders like `%from-server%`, `%message%`, etc.

## Type System

All expressions evaluate to strings. Numbers, booleans, and other types are represented as text:

```skript
set {count} to "10"        # Stored as string "10"
set {status} to "true"     # Stored as string "true"
set {name} to player       # Stored as string (player name)
```

When comparing:
```skript
if {count} > "5":          # String comparison, works for numbers
if {status} is "true":     # String comparison
```

## Limitations

1. **No arithmetic**: `{coins} + 10` doesn't work
2. **No string concatenation**: Can't do `"Hello " + player`
3. **No function calls**: No `uppercase(player)` or similar
4. **String types only**: Everything is text

## Workarounds

### Multiple Variables for Calculations

Instead of:
```skript
set {total} to {coins} + {gems}  # Doesn't work
```

Use separate storage:
```skript
set {totals::%player%::coins} to {coins::%player%}
set {totals::%player%::gems} to {gems::%player%}
```

### Pre-formatted Strings

Instead of:
```skript
set {message} to "Hello " + player  # Doesn't work
```

Use expressions in braces:
```skript
set {message} to "Hello {player}"
send {message} to player  # VelocitySk evaluates {player} when sending
```

## Best Practices

1. **Use descriptive variable names**: Makes expressions readable
2. **Store intermediate results**: Use local variables for complex expressions
3. **Check before using**: Verify variables exist with `is set`
4. **Test your expressions**: Use `/echo` or send messages to verify values
5. **Document complex expressions**: Add comments explaining what they do

## Related Documentation

- [Variables](variables.md) - Learn about variable types and storage
- [Conditionals](conditionals.md) - Use expressions in if statements
- [Actions](actions.md) - Use expressions in actions
- [Commands](commands.md) - Work with command arguments
- [Events](events.md) - Event-specific placeholders

