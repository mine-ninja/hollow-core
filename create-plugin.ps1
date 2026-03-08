param(
    [Parameter(Mandatory=$true)]
    [string]$ModuleName
)

function ToPascalCase($str) {
    return ($str -split '[^a-zA-Z0-9]+' | ForEach-Object { $_.Substring(0,1).ToUpper() + $_.Substring(1).ToLower() }) -join ''
}

$PascalName = ToPascalCase $ModuleName
$root = "plugins/$ModuleName"
$src = "$root/src/main/java/io/github/minehollow/$ModuleName"

New-Item -ItemType Directory -Force -Path $src | Out-Null
New-Item -ItemType Directory -Force -Path "$root/src/main/resources" | Out-Null

$baseGradle = "plugins/bestiary/build.gradle.kts"
$targetGradle = "$root/build.gradle.kts"
if (Test-Path $baseGradle) {
    Copy-Item $baseGradle $targetGradle
    (Get-Content $targetGradle) -replace 'bestiary', $ModuleName | Set-Content $targetGradle
} else {
    $defaultGradle = @"
plugins {
    id("java")
}

group = "io.github.minehollow.$ModuleName"
version = "1.0.0"

repositories {
    mavenCentral()
    maven {
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
"@
    Set-Content $targetGradle -Value $defaultGradle
}

$mainClassPath = "$src/${PascalName}Plugin.java"
$mainClassContent = @"
package io.github.minehollow.$ModuleName;

import org.bukkit.plugin.java.JavaPlugin;

public class ${PascalName}Plugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getLogger().info(\"$ModuleName enabled!\");
    }
}
"@
Set-Content -Path $mainClassPath -Value $mainClassContent

$pluginYmlPath = "$root/src/main/resources/plugin.yml"
$pluginYmlContent = @"
name: $ModuleName
main: io.github.minehollow.$ModuleName.${PascalName}Plugin
version: 1.0.0
author: minehollow
api-version: 1.20
"@
Set-Content -Path $pluginYmlPath -Value $pluginYmlContent

Write-Host "Module '$ModuleName' created at $root"

$settingsPath = "settings.gradle.kts"
if (Test-Path $settingsPath) {
    $settings = Get-Content $settingsPath
    $includeIdx = ($settings | Select-String -Pattern '^include\(').LineNumber - 1
    if ($includeIdx -ge 0) {
        $moduleLine = '    "plugins:' + $ModuleName + '",' # with indentation
        $endIdx = ($settings | Select-String -Pattern '^\)').LineNumber | Where-Object { $_ -gt $includeIdx } | Select-Object -First 1
        if ($endIdx) {
            $alreadyIncluded = $settings -match [regex]::Escape($moduleLine.Trim())
            if (-not $alreadyIncluded) {
                $settings = $settings[0..($endIdx-2)] + $moduleLine + $settings[($endIdx-1)..($settings.Length-1)]
                Set-Content $settingsPath -Value $settings
                Write-Host "Added 'plugins:$ModuleName' to settings.gradle.kts."
            } else {
                Write-Host "Module 'plugins:$ModuleName' already present in settings.gradle.kts."
            }
        }
    }
}
