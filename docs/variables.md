---
layout: page
title: Variables
---

# Variables

Variables allow you to store and retrieve data in your scripts. VelocitySk supports three types of variables: global, local, and lists.

## Variable Syntax

All variables are wrapped in curly braces `{}`:

```skript
{variable}        # Global variable
{_variable}       # Local variable
{list::key}       # List entry (global)
{_list::key}      # List entry (local)
```

## Global Variables

Global variables persist across script executions and server restarts. They're stored in an SQLite database.

### Setting Global Variables

```skript
set {coins::%player%} to "100"
set {server-status} to "online"
set {motd} to "Welcome to the server!"
```

### Reading Global Variables

```skript
send "You have {coins::%player%} coins" to player

if {server-status} is "online":
    send "Server is operational" to player
```

### Deleting Global Variables

```skript
delete {temp-data}
delete {session::%player%}
```

### Persistence

Global variables are automatically saved to `plugins/velocity-sk/variables.db` and will persist even after server restarts.

## Local Variables

Local variables only exist during a single command execution or event trigger. They're perfect for temporary calculations.

### Setting Local Variables

```skript
set {_temp} to "hello"
set {_player-name} to %player%
set {_count} to "5"
```

### Using Local Variables

```skript
command /test:
    trigger:
        set {_message} to "Hello, %player%!"
        send {_message} to player
        # {_message} is automatically deleted after the command finishes
```

### Advantages

- **Fast**: Stored in memory only
- **Clean**: Automatically deleted when execution ends
- **Safe**: Can't conflict with other scripts or executions

## List Variables

Lists allow you to store multiple related values under one name.

### Global Lists

```skript
set {players::notch} to "admin"
set {players::steve} to "member"
set {players::alex} to "vip"
```

### Local Lists

```skript
set {_temp::1} to "first"
set {_temp::2} to "second"
set {_temp::3} to "third"
```

### Using Lists

```skript
command /rank <player>:
    trigger:
        if {ranks::%player%} is set:
            send "%player%'s rank: {ranks::%player%}" to player
        else:
            send "%player% has no rank" to player
```

### Deleting Lists

To delete an entire list (all entries with the same prefix):

```skript
delete {list::*}
delete {_temp::*}
```

To delete a specific entry:

```skript
delete {list::key}
```

## Variable Naming

### Best Practices

1. **Use descriptive names**: `{coins::%player%}` is better than `{c::%player%}`
2. **Use player names in keys**: `{data::%player%}` prevents conflicts between players
3. **Use prefixes for organization**: `{shop::item::diamond}`, `{stats::kills::%player%}`
4. **Lowercase**: `{player-level}` is more readable than `{PLAYERLEVEL}`

### Valid Names

- Letters, numbers, underscores, hyphens: `{my-variable_123}`
- Colons for lists: `{list::key}`
- Player names: `{coins::%player%}`

### Invalid Names

- Spaces: `{my variable}` ❌
- Special characters: `{my@variable}` ❌
- Starting with numbers: `{123variable}` ❌

## Using Variables with Placeholders

You can embed player names and other placeholders in variable names:

```skript
command /setcoins <player> <amount>:
    permission: velocity.admin
    trigger:
        set {coins::%player%} to "%amount%"
        send "Set %player%'s coins to %amount%" to player
```

This creates separate variables for each player:
- `{coins::Steve}`
- `{coins::Alex}`
- `{coins::Notch}`

## Examples

### Simple Coin System

```skript
command /balance:
    aliases: /bal, /money
    trigger:
        if {coins::%player%} is set:
            send "§eYou have §6{coins::%player%} §ecoins" to player
        else:
            set {coins::%player%} to "0"
            send "§eYou have §60 §ecoins" to player

command /pay <player> <amount>:
    trigger:
        if {coins::%player%} is set:
            set {coins::%player%} to "%amount%"
            send "§aTransferred %amount% coins to %player%" to player
            send "§aYou received %amount% coins from %player%!" to %player%
        else:
            send "§cYou don't have enough coins!" to player
```

### Player Preferences

```skript
command /language <lang>:
    trigger:
        if %lang% is "english":
            set {language::%player%} to "en"
            send "Language set to English" to player
        
        if %lang% is "spanish":
            set {language::%player%} to "es"
            send "Idioma establecido en Español" to player

on join:
    if {language::%player%} is "es":
        send "§eBienvenido, %player%!" to player
    else:
        send "§eWelcome, %player%!" to player
```

### Last Location Tracker

```skript
on server switch:
    set {lastserver::%player%} to %from-server%

command /back:
    trigger:
        if {lastserver::%player%} is set:
            transfer player to {lastserver::%player%}
            send "§aTeleporting to {lastserver::%player%}..." to player
        else:
            send "§cNo previous server found!" to player
```

### Vote Tracking

```skript
command /vote:
    trigger:
        if {voted::%player%} is set:
            send "§cYou already voted today!" to player
        else:
            set {voted::%player%} to "true"
            set {votes::%player%} to "1"
            send "§aThank you for voting!" to player
            send "§eThanks for voting at: vote.server.com" to player

on quit:
    delete {voted::%player%}
```

### Role System

```skript
command /setrole <player> <role>:
    permission: velocity.admin
    trigger:
        set {role::%player%} to "%role%"
        send "§aSet %player%'s role to %role%" to player
        send "§aYour role has been set to %role%!" to %player%

command /myrole:
    trigger:
        if {role::%player%} is set:
            send "§eYour role: §6{role::%player%}" to player
        else:
            send "§eYou have no assigned role" to player
```

### Complex List Example: Homes

```skript
command /sethome <name>:
    trigger:
        set {homes::%player%::%name%} to player's server
        send "§aHome '%name%' set on server {player's server}!" to player

command /home <name>:
    trigger:
        if {homes::%player%::%name%} is set:
            transfer player to {homes::%player%::%name%}
            send "§aTeleporting to home '%name%'..." to player
        else:
            send "§cHome '%name%' not found!" to player

command /delhome <name>:
    trigger:
        if {homes::%player%::%name%} is set:
            delete {homes::%player%::%name%}
            send "§aDeleted home '%name%'" to player
        else:
            send "§cHome '%name%' doesn't exist!" to player
```

## Variable Storage

### Database Location

Global variables are stored in:
```
plugins/velocity-sk/variables.db
```

This is an SQLite database file. You can:
- Back it up regularly
- View/edit it with SQLite tools
- Restore it from backups

### Performance

- **Global variables**: Cached in memory, synced to disk
- **Local variables**: Memory only, no disk I/O
- **Lists**: Each entry is stored separately

### Viewing Statistics

Use the command:
```
/vsk info
```

This shows:
- Number of global variables stored
- Cache status
- Active execution scopes (local variables)

## Common Patterns

### Checking if Set

```skript
if {variable} is set:
    send "Variable exists" to player
else:
    send "Variable doesn't exist" to player
```

### Checking if Not Set

```skript
if {variable} is not set:
    set {variable} to "default value"
```

### Using with Conditionals

```skript
if {coins::%player%} > "100":
    send "You're rich!" to player

if {rank::%player%} is "vip":
    send "Welcome, VIP member!" to player
```

## Limitations

1. **Type System**: All values are stored as strings
2. **No Arrays**: Use lists with numeric keys instead
3. **No Math**: `{coins} + 10` doesn't work (yet)
4. **String Values**: Numbers are stored as text

## Best Practices

1. **Initialize variables**: Always check `is set` before reading
2. **Clean up**: Delete temporary variables when done
3. **Use local for temp data**: Don't pollute global namespace
4. **Namespace your globals**: Use prefixes like `{system::...}`
5. **Document your variables**: Add comments explaining what they store

## Migration and Backup

To backup all variables:
1. Stop the server
2. Copy `plugins/velocity-sk/variables.db`
3. Store it safely

To restore:
1. Stop the server
2. Replace `variables.db` with your backup
3. Start the server

## Advanced: Variable Wildcards

To work with all entries in a list:

```skript
delete {temp::*}          # Deletes all {temp::...} variables
delete {coins::%player%::*}  # Deletes all coin types for a player
```

See [Expressions](expressions.md) for more ways to use variables in conditions.

