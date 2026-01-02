---
layout: page
title: Actions
---

# Actions

Actions are the things your scripts actually do. They're the lines that execute when a command is run or an event is triggered.

## Send Message

Send text to players.

### Send to Command Executor/Event Player

```skript
send "Hello!" to player
```

### Send to All Players

```skript
send "§aServer restarting in 5 minutes!" to all players
```

### Send to Specific Player

```skript
send "You received a gift from %player%!" to %arg-1%
```

You can use variables and placeholders:

```skript
send "Gift sent to {target}!" to player
```

### Send to All Players on a Server

```skript
send "§cMinigames server restarting soon!" to all players in server "minigames"
```

### Formatting

Use Minecraft color codes with `§`:

```skript
send "§aGreen §bBlue §cRed §6Gold §fWhite" to player
send "§l§nBold and Underlined" to player
```

## Transfer Player

Move a player to a different backend server.

### Transfer the Command Executor

```skript
transfer player to "hub"
```

### Transfer a Specific Player

```skript
transfer %arg-1% to "survival"
```

### Transfer Using Variables

```skript
set {target} to "minigames"
transfer player to {target}
```

### Examples

```skript
command /hub:
    trigger:
        send "§aTeleporting to hub..." to player
        transfer player to "hub"

command /send <player> <server>:
    permission: velocity.send
    trigger:
        transfer %player% to "%server%"
        send "§aSent %player% to %server%" to player
```

## Variable Operations

### Set Variable

Assign a value to a variable:

```skript
set {coins::%player%} to "100"
set {_local} to "temporary value"
set {server::hub} to "lobby1"
```

Variables can contain:
- Player placeholders: `set {name} to %player%`
- Text: `set {status} to "online"`
- Numbers: `set {count} to "42"`
- Other variables: `set {copy} to {original}`

### Delete Variable

Remove a variable:

```skript
delete {temp::%player%}
delete {_local}
```

### Examples

```skript
command /balance:
    trigger:
        if {coins::%player%} is set:
            send "§eYou have {coins::%player%} coins" to player
        else:
            send "§eYou have 0 coins" to player

command /pay <player> <amount>:
    trigger:
        set {coins::%player%} to %amount%
        send "§aYou gave %player% %amount% coins!" to player
        send "§aYou received %amount% coins from %player%!" to %player%
```

## Conditional Actions

Execute actions only when certain conditions are met. See [Conditionals](conditionals.md) for full details.

```skript
if {coins::%player%} > "100":
    send "You're rich!" to player
else:
    send "You need more coins" to player
```

## Combining Actions

You can combine multiple actions in sequence:

```skript
command /welcome:
    trigger:
        send "§6§lWELCOME!" to player
        send "§eYou are on the {player's server} server" to player
        set {welcomed::%player%} to "true"
        send "§a%player% was welcomed!" to all players in server "hub"
```

## Action Examples by Use Case

### Player Management

```skript
command /warn <player> <reason>:
    permission: velocity.warn
    trigger:
        send "§cYou have been warned by staff!" to %player%
        send "§cReason: %reason%" to %player%
        send "§7[Staff] %player% warned %player% for: %reason%" to all players in server "staff"
        set {warnings::%player%} to "warned"
```

### Server Navigation

```skript
command /servers:
    trigger:
        send "§6§lAvailable Servers:" to player
        send "§e/hub §7- Return to hub" to player
        send "§e/survival §7- Survival server" to player
        send "§e/creative §7- Creative server" to player

command /survival:
    trigger:
        send "§aTeleporting to Survival..." to player
        transfer player to "survival"

command /creative:
    trigger:
        send "§aTeleporting to Creative..." to player
        transfer player to "creative"
```

### Coin System

```skript
command /daily:
    trigger:
        if {lastdaily::%player%} is not set:
            set {coins::%player%} to "100"
            set {lastdaily::%player%} to "claimed"
            send "§aYou claimed 100 daily coins!" to player
        else:
            send "§cYou already claimed your daily coins!" to player

command /coins:
    aliases: /balance, /money
    trigger:
        if {coins::%player%} is set:
            send "§eYou have §6{coins::%player%} §ecoins" to player
        else:
            send "§eYou have §60 §ecoins" to player
```

### Announcement System

```skript
command /announce <message>:
    permission: velocity.announce
    aliases: /broadcast
    trigger:
        send "§6§l[ANNOUNCEMENT]" to all players
        send "§e%message%" to all players

command /serverannounce <server> <message>:
    permission: velocity.announce
    trigger:
        send "§6§l[ANNOUNCEMENT]" to all players in server "%server%"
        send "§e%message%" to all players in server "%server%"
```

### VIP System

```skript
command /vip:
    permission: velocity.vip
    trigger:
        send "§5§l✦ VIP MENU ✦" to player
        send "§d/vipchat - Access VIP chat" to player
        send "§d/viplobby - Join VIP lobby" to player

command /viplobby:
    permission: velocity.vip
    aliases: /vl
    trigger:
        transfer player to "vip-lobby"
        send "§dWelcome to the VIP lobby!" to player
```

## Advanced: Nested Conditionals

You can nest actions within conditionals:

```skript
command /promote <player>:
    permission: velocity.admin
    trigger:
        if {rank::%player%} is "member":
            set {rank::%player%} to "vip"
            send "§aPromoted %player% to VIP!" to player
        else:
            if {rank::%player%} is "vip":
                set {rank::%player%} to "mvp"
                send "§aPromoted %player% to MVP!" to player
            else:
                send "§c%player% is already max rank!" to player
```

## Best Practices

1. **Always provide feedback**: Send messages so players know their action worked
2. **Check before you set**: Use conditionals to validate before setting variables
3. **Clean up variables**: Delete temporary variables when done
4. **Use clear messages**: Include color codes and formatting for readability
5. **Handle errors gracefully**: Check if variables exist before using them

## Limitations

- Variable values are stored as strings
- Mathematical operations require custom implementation
- Some actions (like kicking players) are not yet available

See [Variables](variables.md) for more information on variable types and storage.

