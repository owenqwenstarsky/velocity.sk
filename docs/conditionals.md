---
layout: page
title: Conditionals
---

# Conditionals

Conditionals allow you to execute actions only when certain conditions are met. They're essential for creating dynamic, intelligent scripts.

## If Statement Syntax

```skript
if <condition>:
    <actions>
```

Example:

```skript
command /vip:
    trigger:
        if player is in server "lobby":
            send "You're in the lobby!" to player
```

## If-Else Syntax

```skript
if <condition>:
    <actions>
else:
    <actions>
```

Example:

```skript
command /check:
    trigger:
        if {coins::%player%} > "100":
            send "§aYou're rich!" to player
        else:
            send "§cYou need more coins" to player
```

## If-Else If-Else Syntax

```skript
if <condition>:
    <actions>
else if <condition>:
    <actions>
else:
    <actions>
```

Example:

```skript
command /rank:
    trigger:
        if {points::%player%} > "1000":
            send "§6Your rank: GOLD" to player
        else if {points::%player%} > "500":
            send "§7Your rank: SILVER" to player
        else if {points::%player%} > "100":
            send "§eYour rank: BRONZE" to player
        else:
            send "§fYour rank: MEMBER" to player
```

## Indentation

Conditionals **must** be properly indented:

```skript
command /test:
    trigger:                           # First level
        if player is in server "hub":  # Second level
            send "In hub!" to player   # Third level (inside if)
        else:                          # Second level
            send "Not in hub!" to player  # Third level (inside else)
```

## Available Conditions

### Variable Existence

Check if a variable is set or not:

```skript
if {variable} is set:
    send "Variable exists" to player

if {variable} is not set:
    send "Variable doesn't exist" to player
```

### Server Location

Check which server a player is on:

```skript
if player is in server "lobby":
    send "You're in the lobby!" to player

if player is not in server "minigames":
    send "You're not on the minigames server" to player
```

### Equality

Check if two values are equal:

```skript
if {rank::%player%} is "vip":
    send "You're a VIP!" to player

if %arg-1% is "test":
    send "You said test!" to player

if {language::%player%} = "english":
    send "Language is English" to player
```

### Inequality

Check if two values are not equal:

```skript
if {status} is not "banned":
    transfer player to "hub"

if %arg-1% != "cancel":
    send "Proceeding..." to player
```

### Greater Than / Less Than

Compare numeric values:

```skript
if {coins::%player%} > "100":
    send "You have more than 100 coins!" to player

if {level::%player%} < "10":
    send "You're under level 10" to player
```

### Contains

Check if text contains a substring:

```skript
if %message% contains "help":
    send "Need help? Visit /help" to player

if {player-name} contains "Steve":
    send "Steve is in your name!" to player
```

## Examples

### Permission-like System

```skript
command /admin:
    trigger:
        if {rank::%player%} is "admin":
            send "§aAccess granted!" to player
            transfer player to "admin-server"
        else:
            send "§cYou don't have permission!" to player
```

### Shop System

```skript
command /buy <item>:
    trigger:
        if %item% is "sword":
            if {coins::%player%} > "100":
                set {coins::%player%} to "0"
                send "§aPurchased a sword for 100 coins!" to player
            else:
                send "§cYou need 100 coins! You have {coins::%player%}" to player
        else if %item% is "armor":
            if {coins::%player%} > "200":
                set {coins::%player%} to "0"
                send "§aPurchased armor for 200 coins!" to player
            else:
                send "§cYou need 200 coins! You have {coins::%player%}" to player
        else:
            send "§cUnknown item: %item%" to player
```

### Server Router

```skript
command /play <gamemode>:
    trigger:
        if %gamemode% is "survival":
            if player is in server "survival":
                send "§cYou're already on Survival!" to player
            else:
                transfer player to "survival"
                send "§aTeleporting to Survival..." to player
        
        else if %gamemode% is "creative":
            if player is in server "creative":
                send "§cYou're already on Creative!" to player
            else:
                transfer player to "creative"
                send "§aTeleporting to Creative..." to player
        
        else:
            send "§cUnknown gamemode: %gamemode%" to player
            send "§7Available: survival, creative" to player
```

### Daily Reward System

```skript
command /daily:
    trigger:
        if {lastdaily::%player%} is not set:
            if {streak::%player%} is set:
                if {streak::%player%} > "5":
                    set {coins::%player%} to "500"
                    send "§6§lSTREAK BONUS! +500 coins!" to player
                else:
                    set {coins::%player%} to "100"
                    send "§a+100 daily coins!" to player
            else:
                set {coins::%player%} to "100"
                set {streak::%player%} to "1"
                send "§a+100 daily coins!" to player
            
            set {lastdaily::%player%} to "claimed"
        else:
            send "§cYou already claimed today!" to player
```

### Anti-Spam System

```skript
on chat:
    if {muted::%player%} is set:
        send "§cYou are muted!" to player
    else:
        if %message% contains "spam":
            set {warnings::%player%} to "1"
            send "§cWatch your language!" to player
            
            if {warnings::%player%} > "3":
                set {muted::%player%} to "true"
                send "§cYou have been muted!" to player
```

### Server Status Check

```skript
command /serverstatus <server>:
    trigger:
        if %server% is "lobby":
            if {status::lobby} is "online":
                send "§aLobby: ONLINE" to player
            else:
                send "§cLobby: OFFLINE" to player
        
        else if %server% is "survival":
            if {status::survival} is "online":
                send "§aSurvival: ONLINE" to player
            else:
                send "§cSurvival: OFFLINE" to player
        
        else:
            send "§cUnknown server: %server%" to player
```

### Level System

```skript
on join:
    if {level::%player%} is not set:
        set {level::%player%} to "1"
        set {xp::%player%} to "0"
    
    if {level::%player%} > "10":
        send "§6Welcome back, veteran!" to player
    else if {level::%player%} > "5":
        send "§eWelcome back, experienced player!" to player
    else:
        send "§7Welcome back!" to player
```

### VIP Benefits

```skript
on server switch:
    if {rank::%player%} is "vip":
        if %to-server% is "minigames":
            send "§dVIP: You get a speed boost on minigames!" to player
    
    if {rank::%player%} is "mvp":
        send "§6MVP: Thanks for supporting the server!" to player
        if %to-server% is "lobby":
            send "§6Visit /mvp for exclusive perks!" to player
```

### Tutorial System

```skript
on join:
    if {completed_tutorial::%player%} is not set:
        transfer player to "tutorial"
        send "§aWelcome! Please complete the tutorial." to player
        set {tutorial_step::%player%} to "1"
    else:
        if player is in server "tutorial":
            transfer player to "lobby"
            send "§eYou already completed the tutorial!" to player
```

## Nested Conditionals

You can nest if statements inside each other:

```skript
command /check:
    trigger:
        if {rank::%player%} is "admin":
            if {coins::%player%} > "1000":
                send "§aYou're a rich admin!" to player
            else:
                send "§aYou're an admin, but broke!" to player
        else:
            if {coins::%player%} > "1000":
                send "§eYou're rich, but not an admin!" to player
            else:
                send "§7You're a regular player" to player
```

## Combining with Events

Conditionals work in both commands and events:

```skript
on join:
    if {banned::%player%} is set:
        send "§cYou are banned from this server!" to player
        # Note: Kicking requires additional implementation
    else:
        if {firstjoin::%player%} is not set:
            send "§6Welcome to the server for the first time!" to player
            set {firstjoin::%player%} to "true"
            transfer player to "tutorial"
        else:
            send "§eWelcome back, %player%!" to player
```

## Multiple Conditions

Currently, you need to nest conditionals to check multiple conditions:

```skript
if {rank::%player%} is "vip":
    if {coins::%player%} > "100":
        send "VIP with lots of coins!" to player
```

## Best Practices

1. **Check existence first**: Always verify variables are set before comparing them
2. **Use else for default**: Provide fallback behavior with `else`
3. **Order matters**: Put most specific conditions first in else-if chains
4. **Keep it simple**: Break complex logic into multiple commands/events
5. **Indent consistently**: Use tabs OR spaces, not both

## Common Pitfalls

1. **Missing indentation**: Actions must be indented under their condition
2. **Comparing unset variables**: Check `is set` first
3. **Wrong quotes**: Use double quotes `"` not single quotes `'`
4. **Typos in conditions**: `player is in sever` won't work
5. **Forgetting colons**: Conditions must end with `:`

## Debugging Tips

If your conditionals don't work:

1. Check the console for parsing errors
2. Verify your indentation is consistent
3. Make sure variables are set before comparing them
4. Test with simple messages to see which branch executes
5. Use `/vsk info` to check variable values

Example debug approach:

```skript
command /debug:
    trigger:
        send "Checking coin value..." to player
        if {coins::%player%} is set:
            send "Coins: {coins::%player%}" to player
            if {coins::%player%} > "100":
                send "More than 100!" to player
            else:
                send "100 or less" to player
        else:
            send "Coins not set!" to player
```

See [Expressions](expressions.md) for more details on what can be used in conditions.

