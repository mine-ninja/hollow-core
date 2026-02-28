plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "hollow-core"

include(
    "core-sdk",
    "core-proxy",
    "core-minecraft",
    "plugins:skills",
    "plugins:kits",
    "plugins:mines",
    "plugins:rank",
    "plugins:lobby",
    "plugins:bestiary",
    "plugins:quests",
    "plugins:mailbox",
    "plugins:clans",
    "plugins:essentials",
    "plugins:npc",
)
