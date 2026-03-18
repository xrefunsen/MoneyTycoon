# MoneyTycoon

Roblox-style Tycoon game-mode plugin for Minecraft Paper 1.21.4. Players build factories on void world islands, drop items from droppers, move them on conveyor belts, sell at the sell point, and purchase upgrades.

## Requirements

- Java 21+
- Paper 1.21.4 (or compatible fork)
- Vault + economy plugin (EssentialsX, CMI, etc.)
- PlaceholderAPI (optional)

## Build

```bash
mvn clean package
```

Output: `target/MoneyTycoon-2.0.0.jar`

## Installation

1. Place the JAR in your server's `plugins/` folder
2. Ensure Vault and an economy plugin are installed
3. Start the server — the void tycoon world is created automatically
4. Edit `plugins/MoneyTycoon/config.yml` as needed

## Commands

| Command | Description |
|--------|-------------|
| `/tycoon create` | Create tycoon |
| `/tycoon delete` | Delete tycoon |
| `/tycoon menu` | Main menu |
| `/tycoon upgrades` | Upgrades |
| `/tycoon collect` | Collect money |
| `/tycoon visit <player>` | Visit player's tycoon |
| `/tycoon tp` | Teleport to your tycoon |
| `/tycoon prestige` | Prestige |
| `/tycoon rebirth` | Rebirth |
| `/tycoon quest` | Quests |
| `/tycoon pet` | Pets |
| `/tycoon theme` | Change theme |
| `/tycoon coop invite/kick/list` | Co-op management |
| `/tycoon leaderboard` | Leaderboard |
| `/tycoon admin reload` | Reload config |
| `/tycoon admin delete <player>` | Delete tycoon (admin) |

**Aliases:** `/ty`, `/factory`

## License

Private License. All rights reserved.
