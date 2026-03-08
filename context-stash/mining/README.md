# Overview

The mining package is a complex and lightweight mining system using minecraft packets for mining render, coding over
1.8.9 protocols.

## Understanding the mining structure

The mining package is structured as follows:

- `common`: The **common** package for the mining package, the common folder is required for mining
  work.
- `common/api`: The **API** for third-party services to interact, this module is interface-based.
- `common/configuration`: The **configuration** for the mining package, this module is responsible for configuration files.
- `common/core`: The **core** and private structure of the mining package.
  Generally this module is responsible for implement the services and default implementations.
- `common/plugin`: The **plugin** package for the mining package.
  Compiles everything of mining package into a single package.
- `common/shared`: The **shared** package for the mining package.
  Contains shared code used by all other mining packages.
- `common/eventbus`: The **eventbus** package for the mining package.
  Contains the event bus used by the mining package.
- `common/services`: The **services** package for the mining package.
  Contains all the services (interfaces) used by the mining package.
- `features/gui`: The **GUI** (Graphical User Interface) for the mining package, this includes menus and
  GUI interactions.
- `features/enchantments`: The **enchants** package for the mining package, this includes all the
  enchantments used by the mining package.
- `features/themes`: The **themes** package for the mining package, this includes all the area themes used by the
  mining package.

## API

The API is the interface for third-party services to interact with the mining package. The API is interface-based
and does not contain any default implementations.

## Configuration

The configuration package is the package that contains all the configuration files used by the mining package.
This package is responsible for the configuration files; the configuration files are used all modules like enchantments,
handlers, etc.

## Core

The core package is the private structure of the mining package. This package is responsible for implementing the
factories, services and default implementations. Everything needs the core package to work.

## Shared

The shared package is the package that contains shared code used by all other mining packages. This package is
responsible for the shared code used by all other packages, generally is used for models, utils, etc.

## Services

The service package is the package that contains all the services used by the mining package. This package is
responsible for the services, the services are used in the core package. A service is a class that provides a
specific functionality, generally a service is a singleton by default implementation and an interface for third-party
services to interact.

## Plugin

The plugin package is the package that compiles everything of the mining package into a bukkit plugin, including the gui
enchantments, etc.

### Event Bus

The event bus is a simple event bus system that allows for the registration of listeners and the firing of events.
The event bus is responsible for default subscribers and event handling. For example, the event bus is used to
handle when the player joins the server the event bus will fire the `MiningUserLoadEvent` because of the
default login phase (see MiningEventBusLoginSubscriber to understand).

## GUI

The GUI package is the package that contains all the GUI (Graphical User Interface) for the mining package. This
package is responsible for the menus and GUI interactions, this package is used only in the plugin package.

## Enchantments

The enchantment package is the package that contains all the enchantments used by the mining package. This package
is responsible for the enchantments, the enchantments are used in the gui and plugin. An enchantment is a special
property that can be applied to pickaxe tool and can be activated by the player when mines a block.

## Themes

The themes package is the package that represents the themes can be used in mining areas.
A theme is a set of details that can be used in a mining area like builds, blocks, fade, styles etc.

That's all for the mining package, if you have any questions or need help, please contact the author of the package.