---
layout: page
title: Get Started
---

# Getting Started with VelocitySk

VelocitySk is a Skript-like scripting plugin for Velocity proxy servers. It allows you to create custom commands, handle events, and manage variables using a simple, readable syntax.

## Installation

1. Download the `velocity-sk-1.0.0.jar` file
2. Place it in your Velocity proxy's `plugins/` folder
3. Restart or reload your proxy server
4. The plugin will create a `plugins/velocity-sk/` folder with a `scripts/` subdirectory

## File Structure

```
velocity-sk/
├── scripts/           # Place your .vsk script files here
│   └── example.vsk
├── variables.db       # SQLite database for persistent variables
└── logs/             # Script execution logs (if enabled)
```

## Your First Script

Create a file named `welcome.vsk` in the `plugins/velocity-sk/scripts/` folder:

```skript
command /welcome:
    trigger:
        send "Welcome to the server, %player%!" to player
```

After creating the file, reload the scripts:

```
/vsk reload all
```

Now players can use `/welcome` to receive a greeting!

## Script File Format

- Scripts must have the `.vsk` extension
- Use indentation (tabs or 4 spaces) to define structure
- Comments start with `#`
- Empty lines are ignored

Example with comments:

```skript
# This is a comment
command /greet <name>:
    # This command greets a player by name
    trigger:
        send "Hello, %arg-1%!" to player
```

## Managing Scripts

### Reload All Scripts
```
/vsk reload all
```

### Reload a Single Script
```
/vsk reload welcome.vsk
```

### Enable a Disabled Script
```
/vsk enable welcome.vsk
```

### Disable a Script
```
/vsk disable welcome.vsk
```

### View Plugin Info
```
/vsk info
```

## Next Steps

- Learn about [Commands](commands.md) - Create custom commands with arguments
- Explore [Events](events.md) - React to player actions and proxy events
- Understand [Variables](variables.md) - Store and retrieve data
- Master [Conditionals](conditionals.md) - Add logic to your scripts

## Common Pitfalls

1. **Indentation**: Make sure to use consistent indentation (tabs OR spaces, not both)
2. **Quotes**: Always use double quotes `"` for text strings
3. **Variable Syntax**: Variables must be wrapped in curly braces: `{variable}`
4. **Command Names**: Use only letters, numbers, and underscores in command names

## Getting Help

If a script fails to load, check the console for error messages. The parser will tell you exactly which line has an issue and what's wrong.

Example error message:
```
[ERROR] Script welcome.vsk has 1 parsing error(s)
Line 3: Invalid send syntax. Expected: send "message" [to <target>]
```

