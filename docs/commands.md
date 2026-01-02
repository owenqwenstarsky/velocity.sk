# Commands

Commands allow players to execute custom actions by typing in chat.

## Basic Command Syntax

```skript
command /<name>:
    trigger:
        <actions>
```

Example:

```skript
command /heal:
    trigger:
        send "You have been healed!" to player
```

## Command Arguments

You can require players to provide arguments when using a command:

```skript
command /msg <player> <message>:
    trigger:
        send "Message from %player%: %message%" to %player%
```

Arguments are automatically available as:
- `%arg-1%`, `%arg-2%`, etc. (by position)
- `%<name>%` (by the name you specified, e.g., `%player%`, `%message%`)

### Validation

If a player doesn't provide enough arguments, they'll automatically see a usage message:

```
Usage: /msg <player> <message>
```

## Command Metadata

### Permission

Require a permission to use the command:

```skript
command /staff:
    permission: velocity.staff
    permission message: "§cYou must be a staff member to use this command!"
    trigger:
        send "Staff panel opened" to player
```

If no permission message is set, the default is: "§cYou don't have permission to use this command."

### Aliases

Add alternative names for your command:

```skript
command /teleport:
    aliases: /tp, /tele
    trigger:
        send "Teleportation system" to player
```

Players can now use `/teleport`, `/tp`, or `/tele`.

### Usage Message

Customize the usage message shown when arguments are missing:

```skript
command /warn <player> <reason>:
    usage: /warn <player> <reason> - Warn a player
    trigger:
        send "Warned %player% for: %reason%" to all players
```

### Description

Add a description (useful for help systems):

```skript
command /hub:
    description: Teleports you to the hub server
    trigger:
        transfer player to "hub"
```

## Full Example

```skript
command /broadcast <message>:
    permission: velocity.broadcast
    permission message: "§cOnly administrators can broadcast messages!"
    aliases: /bc, /announce
    usage: /broadcast <message> - Send a message to all players
    description: Broadcasts a message to everyone online
    trigger:
        send "§6[Broadcast] §f%message%" to all players
```

## Available Placeholders

Within command triggers, you can use:

- `%player%` - The name of the player who executed the command
- `%uuid%` - The UUID of the player
- `%arg-1%`, `%arg-2%`, etc. - Command arguments by position
- `%<arg-name>%` - Command arguments by name
- `{variable}` - Global variables (see [Variables](variables.md))
- `{_variable}` - Local variables

## Examples

### Simple Greeting Command

```skript
command /hello:
    trigger:
        send "Hello, %player%!" to player
```

### Command with Arguments

```skript
command /tell <player> <message>:
    trigger:
        send "§7[%player% -> You] §f%message%" to %player%
        send "§7[You -> %player%] §f%message%" to player
```

### Command with Permission

```skript
command /fly:
    permission: velocity.fly
    trigger:
        send "§aFlight mode toggled!" to player
```

### Hub Transfer Command

```skript
command /hub:
    aliases: /lobby, /spawn
    description: Return to the hub server
    trigger:
        send "§aTeleporting to hub..." to player
        transfer player to "hub"
```

### Player Management Command

```skript
command /kick <player> <reason>:
    permission: velocity.kick
    permission message: "§cYou don't have permission to kick players!"
    usage: /kick <player> <reason>
    trigger:
        send "§c%player% has been kicked for: %reason%" to all players
        send "§cYou have been kicked. Reason: %reason%" to %player%
```

## Best Practices

1. **Use descriptive names**: `/teleport` is better than `/tp1`
2. **Add permissions**: Prevent abuse of powerful commands
3. **Provide feedback**: Always send messages so players know what happened
4. **Use aliases**: Provide shortcuts for frequently-used commands
5. **Validate input**: Use conditionals to check if arguments are valid (see [Conditionals](conditionals.md))

