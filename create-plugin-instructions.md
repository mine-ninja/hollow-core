# Plugin Module Creation Script: `create-plugin.ps1`

This PowerShell script automates the creation of a new Spigot plugin module in your workspace, following the same package and folder structure as your existing modules.

## How It Works
- Prompts for a module name (e.g., `dungeons`)
- Creates the directory structure: `plugins/<modulename>/src/main/java/io/github/minehollow/<modulename>`
- Generates a PascalCase main class (e.g., `DungeonesPlugin.java`) in the correct package
- Copies and adapts `build.gradle.kts` from the `bestiary` plugin (if available)
- Creates a basic `plugin.yml` file

## Usage Example
Open PowerShell in your workspace root and run:

```powershell
.\create-plugin.ps1 -ModuleName dungeons
```

This will create:
- `plugins/dungeons/build.gradle.kts`
- `plugins/dungeons/src/main/java/io/github/minehollow/dungeons/DungeonesPlugin.java`
- `plugins/dungeons/src/main/resources/plugin.yml`

## Main Class Example
```java
package io.github.minehollow.dungeons;

import org.bukkit.plugin.java.JavaPlugin;

public class DungeonesPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info("dungeons enabled!");
    }
}
```

## plugin.yml Example
```yaml
name: dungeons
main: io.github.minehollow.dungeons.DungeonesPlugin
version: 1.0.0
author: minehollow
api-version: 1.20
```

## Notes
- The main class is always PascalCase (e.g., `DungeonesPlugin`) regardless of the module name casing.
- The package name remains lowercase (e.g., `io.github.minehollow.dungeons`).
- You can customize the generated files as needed after creation.

