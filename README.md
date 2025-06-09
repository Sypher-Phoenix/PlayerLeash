# PlayerLeash for PaperMC

![Java Version](https://img.shields.io/badge/Java-17+-blue.svg)
![API Version](https://img.shields.io/badge/API-Paper_1.17+-green.svg)
![License](https://img.shields.io/badge/License-MIT-brightgreen.svg)
[![GitHub issues](https://img.shields.io/github/issues/YourUsername/PlayerLeash)](https://github.com/YourUsername/PlayerLeash/issues)

A lightweight, standalone Minecraft plugin for Paper/Spigot servers that allows players to leash other players with a invisible rope.

## Features

- **Leash Players:** Right-click a player while holding a lead to leash them.
- **Physics-Based Tug:** When players move too far apart, they are gently pulled toward each other.
- **Configurable Distances:** Customize the "tug" and "break" distances.
- **Permission-Based:** Control who can use leashes with a simple permission node.
- **Standalone:** No dependencies on any other plugin.
- **Highly Performant:** Built with modern Kotlin and optimized for performance, running its main task every tick for smooth visuals and physics.

## Installation

1.  Download the latest release from the [Releases](https://github.com/SypherPhoenix/PlayerLeash/releases) page.
2.  Place the `PlayerLeash-vX.X.X.jar` file into your server's `plugins/` directory.
3.  Restart your server. The plugin will generate a default `config.yml` file.
4.  (Optional) Grant permissions to your players/groups.

## Usage

### Commands

This plugin has no commands. All interactions are done in-game.

### Permissions

-   `playerleash.use` - Allows a player to right-click another player with a lead to create a leash.
    -   **Default:** `op`

### How to Leash

1.  Hold a **Lead** in your main hand.
2.  Make sure you have the `playerleash.use` permission.
3.  Right-click on the player you wish to leash.
4.  To unleash, right-click the same player again.

The leash will also break if the players get too far apart, one of them disconnects, or (by default) if one of them dies.

## Configuration

The configuration can be edited in `plugins/PlayerLeash/config.yml`.

```yaml
# PlayerLeash Configuration

leash:
  # The strength of the "tug" when players move too far apart.
  # Higher values mean a stronger pull. (Default: 0.4)
  strength: 0.4

  # The distance (in blocks) at which players will start being pulled towards each other.
  # (Default: 8.0)
  distance:
    tug: 8.0

    # The maximum distance (in blocks) before the leash snaps.
    # (Default: 12.0)
    max: 12.0
    
  # If true, the leash will break if either leashed player dies.
  # (Default: true)
  break-on-death: true
```

---

## For Developers

### Building from Source

This project uses Gradle for building.

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/SypherPhoenix/PlayerLeash.git
    cd PlayerLeash
    ```

2.  **Build the project:**
    -   On Windows: `gradlew build`
    -   On Linux/macOS: `./gradlew build`

3.  The compiled JAR file will be located in the `build/libs/` directory.

### Contributing

Contributions are welcome! If you have a feature request, bug report, or pull request, please feel free to [open an issue](https://github.com/YourUsername/PlayerLeash/issues) or submit a pull request.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
