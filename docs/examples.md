# Complete Examples

This page contains complete, ready-to-use script examples demonstrating various features of VelocitySk.

## Welcome System

A comprehensive welcome system for new and returning players.

**File: `welcome.vsk`**

```skript
# Welcome new players
on join:
    # Check if first time joining
    if {firstjoin::%player%} is not set:
        send "§6§l✦ WELCOME TO THE SERVER ✦" to player
        send "§eThis is your first time joining!" to player
        send "§aThanks for joining! Type /help to get started." to player
        send "§7%player% joined the server for the first time!" to all players
        
        # Mark as joined and give starting coins
        set {firstjoin::%player%} to "true"
        set {coins::%player%} to "100"
        set {joincount::%player%} to "1"
        
        # Send to tutorial
        transfer player to "tutorial"
    else:
        # Returning player
        send "§eWelcome back, %player%!" to player
        send "§7You are on: {player's server}" to player
        
        if {coins::%player%} is set:
            send "§eCoins: §6{coins::%player%}" to player

# Goodbye message
on quit:
    send "§7%player% left the server" to all players
    
    # Clean up temporary data
    delete {temp::%player%}
```

## Server Hub System

Commands for navigating between servers with a hub menu.

**File: `hub.vsk`**

```skript
# Hub command
command /hub:
    aliases: /lobby, /spawn
    description: Return to the hub server
    trigger:
        if player is in server "hub":
            send "§cYou're already in the hub!" to player
        else:
            send "§aTeleporting to hub..." to player
            transfer player to "hub"

# Server menu command
command /servers:
    aliases: /serverlist, /play
    description: View available servers
    trigger:
        send "§6§l✦ SERVER MENU ✦" to player
        send "§e/hub §7- Main lobby" to player
        send "§e/survival §7- Survival server" to player
        send "§e/creative §7- Creative server" to player
        send "§e/minigames §7- Minigames server" to player
        send "§e/skyblock §7- Skyblock server" to player

# Individual server commands
command /survival:
    aliases: /surv
    description: Join the survival server
    trigger:
        if player is in server "survival":
            send "§cYou're already on Survival!" to player
        else:
            send "§aTeleporting to Survival..." to player
            transfer player to "survival"

command /creative:
    aliases: /crea
    description: Join the creative server
    trigger:
        if player is in server "creative":
            send "§cYou're already on Creative!" to player
        else:
            send "§aTeleporting to Creative..." to player
            transfer player to "creative"

command /minigames:
    aliases: /mg, /games
    description: Join the minigames server
    trigger:
        if player is in server "minigames":
            send "§cYou're already on Minigames!" to player
        else:
            send "§aTeleporting to Minigames..." to player
            transfer player to "minigames"

# Track server switches
on server switch:
    if %from-server% is not "none":
        send "§7You moved from §e%from-server% §7to §e%to-server%" to player
    
    set {lastserver::%player%} to %from-server%

# Back command
command /back:
    description: Return to your previous server
    trigger:
        if {lastserver::%player%} is set:
            send "§aReturning to {lastserver::%player%}..." to player
            transfer player to {lastserver::%player%}
        else:
            send "§cNo previous server found!" to player
```

## Economy System

A complete coin-based economy system.

**File: `economy.vsk`**

```skript
# Check balance
command /balance:
    aliases: /bal, /money, /coins
    description: Check your coin balance
    trigger:
        if {coins::%player%} is set:
            send "§eYou have §6{coins::%player%} §ecoins" to player
        else:
            set {coins::%player%} to "0"
            send "§eYou have §60 §ecoins" to player

# Pay another player
command /pay <player> <amount>:
    description: Give coins to another player
    usage: /pay <player> <amount>
    trigger:
        # Check if trying to pay themselves
        if %player% is player:
            send "§cYou can't pay yourself!" to player
        else:
            # Check if sender has coins
            if {coins::%player%} is set:
                send "§aYou sent %amount% coins to %player%!" to player
                send "§aYou received %amount% coins from %player%!" to %player%
                
                # Set recipient's coins
                set {coins::%player%} to "%amount%"
            else:
                send "§cYou don't have any coins!" to player

# Admin command to set coins
command /setcoins <player> <amount>:
    permission: velocity.economy.admin
    permission message: "§cYou don't have permission to manage coins!"
    description: Set a player's coin balance
    usage: /setcoins <player> <amount>
    trigger:
        set {coins::%player%} to "%amount%"
        send "§aSet %player%'s balance to %amount% coins" to player
        send "§aYour coin balance was set to %amount%" to %player%

# Daily reward
command /daily:
    aliases: /reward
    description: Claim your daily coin reward
    trigger:
        if {lastdaily::%player%} is not set:
            # First time claiming
            set {coins::%player%} to "100"
            set {lastdaily::%player%} to "claimed"
            set {streak::%player%} to "1"
            send "§a§l+ 100 COINS" to player
            send "§7Daily reward claimed! Come back tomorrow for more." to player
        else:
            send "§cYou already claimed your daily reward!" to player
            send "§7Come back tomorrow!" to player

# Reset daily rewards (runs automatically or via command)
on quit:
    delete {lastdaily::%player%}

# Shop system
command /shop:
    description: Open the coin shop
    trigger:
        send "§6§l✦ COIN SHOP ✦" to player
        send "§e/buy vip §7- 1000 coins" to player
        send "§e/buy prefix §7- 500 coins" to player
        send "§e/buy chatcolor §7- 250 coins" to player

command /buy <item>:
    description: Buy an item from the shop
    usage: /buy <item>
    trigger:
        if %item% is "vip":
            if {coins::%player%} > "999":
                set {rank::%player%} to "vip"
                send "§a§lPURCHASED VIP RANK!" to player
                send "§7-1000 coins" to player
            else:
                send "§cYou need 1000 coins! You have {coins::%player%}" to player
        
        else if %item% is "prefix":
            if {coins::%player%} > "499":
                send "§a§lPURCHASED CUSTOM PREFIX!" to player
                send "§7-500 coins" to player
                send "§7Use /setprefix to customize it" to player
            else:
                send "§cYou need 500 coins! You have {coins::%player%}" to player
        
        else if %item% is "chatcolor":
            if {coins::%player%} > "249":
                send "§a§lPURCHASED CHAT COLOR!" to player
                send "§7-250 coins" to player
            else:
                send "§cYou need 250 coins! You have {coins::%player%}" to player
        
        else:
            send "§cUnknown item: %item%" to player
            send "§7Use /shop to see available items" to player
```

## Moderation Tools

Staff commands for player management.

**File: `moderation.vsk`**

```skript
# Broadcast command
command /broadcast <message>:
    permission: velocity.broadcast
    permission message: "§cOnly staff can broadcast messages!"
    aliases: /bc, /announce
    description: Send a message to all players
    usage: /broadcast <message>
    trigger:
        send "§6§l[BROADCAST]" to all players
        send "§e%message%" to all players

# Staff chat
command /staffchat <message>:
    permission: velocity.staff
    permission message: "§cYou must be staff to use this!"
    aliases: /sc, /ac
    description: Send a message to online staff
    usage: /staffchat <message>
    trigger:
        send "§b[STAFF] §f%player%: %message%" to all players in server "staff"

# Send player to server
command /send <player> <server>:
    permission: velocity.send
    permission message: "§cYou don't have permission to send players!"
    description: Send a player to a server
    usage: /send <player> <server>
    trigger:
        transfer %player% to "%server%"
        send "§aSent %player% to %server%" to player
        send "§eYou were sent to %server% by a staff member" to %player%

# Warn player
command /warn <player> <reason>:
    permission: velocity.warn
    permission message: "§cYou don't have permission to warn players!"
    description: Warn a player
    usage: /warn <player> <reason>
    trigger:
        send "§c§l⚠ WARNING ⚠" to %player%
        send "§cYou have been warned by staff!" to %player%
        send "§cReason: %reason%" to %player%
        send "§a✓ Warned %player% for: %reason%" to player
        
        # Track warnings
        if {warnings::%player%} is set:
            send "§7This player has been warned before" to player
        else:
            set {warnings::%player%} to "1"

# Check player info
command /checkplayer <player>:
    permission: velocity.staff
    aliases: /check
    description: View player information
    usage: /checkplayer <player>
    trigger:
        send "§6=== Player Info: %player% ===" to player
        
        if {rank::%player%} is set:
            send "§eRank: {rank::%player%}" to player
        else:
            send "§eRank: §7Member" to player
        
        if {coins::%player%} is set:
            send "§eCoins: {coins::%player%}" to player
        else:
            send "§eCoins: §70" to player
        
        if {warnings::%player%} is set:
            send "§cWarnings: Yes" to player
        else:
            send "§aWarnings: None" to player

# Mute player
command /mute <player>:
    permission: velocity.mute
    permission message: "§cYou don't have permission to mute players!"
    description: Mute a player
    usage: /mute <player>
    trigger:
        set {muted::%player%} to "true"
        send "§c%player% has been muted" to all players
        send "§cYou have been muted by staff" to %player%

command /unmute <player>:
    permission: velocity.mute
    permission message: "§cYou don't have permission to unmute players!"
    description: Unmute a player
    usage: /unmute <player>
    trigger:
        if {muted::%player%} is set:
            delete {muted::%player%}
            send "§a%player% has been unmuted" to all players
            send "§aYou have been unmuted" to %player%
        else:
            send "§c%player% is not muted" to player

# Chat filter
on chat:
    if {muted::%player%} is set:
        send "§cYou are muted and cannot chat!" to player
```

## VIP System

VIP perks and commands.

**File: `vip.vsk`**

```skript
# VIP welcome message
on join:
    if {rank::%player%} is "vip":
        send "§d§l✦ VIP %player% joined the server! ✦" to all players
    else if {rank::%player%} is "mvp":
        send "§6§l✦ MVP %player% joined the server! ✦" to all players

# VIP menu
command /vip:
    permission: velocity.vip
    permission message: "§cYou must be VIP to use this! Visit /buy vip"
    description: Access VIP features
    trigger:
        send "§5§l✦ VIP MENU ✦" to player
        send "§d/viplobby §7- Join VIP lobby" to player
        send "§d/vipchat <msg> §7- VIP-only chat" to player
        send "§d/fly §7- Toggle flight (in lobby)" to player

# VIP lobby
command /viplobby:
    permission: velocity.vip
    aliases: /vl
    description: Join the VIP lobby
    trigger:
        if player is in server "vip-lobby":
            send "§cYou're already in the VIP lobby!" to player
        else:
            send "§dWelcome to the VIP lobby!" to player
            transfer player to "vip-lobby"

# VIP chat
command /vipchat <message>:
    permission: velocity.vip
    aliases: /vc
    description: Send a message in VIP chat
    usage: /vipchat <message>
    trigger:
        if {rank::%player%} is "mvp":
            send "§6[MVP] §f%player%: %message%" to all players in server "vip-lobby"
        else:
            send "§d[VIP] §f%player%: %message%" to all players in server "vip-lobby"

# Server-specific VIP perks
on server switch:
    if {rank::%player%} is "vip":
        if %to-server% is "minigames":
            send "§d§lVIP BONUS: §7+2x coins in minigames!" to player
    
    if {rank::%player%} is "mvp":
        if %to-server% is "survival":
            send "§6§lMVP BONUS: §7/fly enabled on survival!" to player
```

## Tutorial System

An interactive tutorial for new players.

**File: `tutorial.vsk`**

```skript
# Auto-send new players to tutorial
on join:
    if {completed_tutorial::%player%} is not set:
        send "§6Welcome! Completing the tutorial..." to player
        transfer player to "tutorial"
        set {tutorial_step::%player%} to "1"

# Tutorial progress command
command /tutorial:
    description: View tutorial progress
    trigger:
        if {completed_tutorial::%player%} is set:
            send "§aYou've already completed the tutorial!" to player
            send "§7Use /skiptutorial if you want to reset" to player
        else:
            if {tutorial_step::%player%} is set:
                send "§eTutorial Progress: Step {tutorial_step::%player%}" to player
                send "§7Follow the signs to continue!" to player
            else:
                send "§cYou haven't started the tutorial yet" to player
                send "§7Join the server to begin!" to player

# Skip tutorial (for returning players with new accounts)
command /skiptutorial:
    description: Skip the tutorial
    trigger:
        if {completed_tutorial::%player%} is not set:
            set {completed_tutorial::%player%} to "true"
            send "§aTutorial marked as complete!" to player
            transfer player to "hub"
        else:
            send "§cYou already completed the tutorial!" to player

# Complete tutorial
command /completetutorial:
    permission: velocity.tutorial
    description: Mark tutorial as complete
    trigger:
        set {completed_tutorial::%player%} to "true"
        delete {tutorial_step::%player%}
        send "§a§lTUTORIAL COMPLETE!" to player
        send "§eYou earned 500 bonus coins!" to player
        set {coins::%player%} to "500"
        transfer player to "hub"
```

## Usage Tips

1. **Create separate files**: Each example can be its own `.vsk` file
2. **Combine features**: Mix and match from different examples
3. **Test thoroughly**: Try each command before deploying
4. **Customize**: Change messages, coin amounts, server names, etc.
5. **Add permissions**: Adjust permission nodes to match your setup

## More Examples

For more specific examples, see:
- [Commands](commands.md) - Command syntax and options
- [Events](events.md) - Event triggers and handlers
- [Variables](variables.md) - Variable storage patterns
- [Conditionals](conditionals.md) - Logic and decision making
- [Actions](actions.md) - Available actions and their usage

