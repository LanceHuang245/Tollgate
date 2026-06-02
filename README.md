[English](README.md) | [简体中文](README_CN.md)

# Tollgate

A Minecraft Paper plugin that adds toll gates to your server. Players place an iron door with a sign above it to create a toll gate — sneaking and right-clicking the door deducts a fee via Vault economy and teleports the player through.

## Requirements

- **Paper 26.1.x**
- **Java 25+**
- **Vault** plugin
- A Vault-compatible economy plugin (e.g. [EssentialsX](https://essentialsx.net/))

## Features

- Iron door + sign above = toll gate
- Sign text sets the price (first line, e.g. `50` or `$50`)
- Players **sneak + right-click** the iron door to pay and pass through
- Fee deduction via Vault economy — works with EssentialsX, CMI, or any Vault-based economy
- Automatic tollgate registration when a valid sign is placed above an iron door
- Automatic deregistration when the sign or door is broken
- Configurable messages
- Admin commands for management

## Installation

1. Download `Tollgate-x.x.x.jar` from [Releases](https://github.com/eworld/Tollgate/releases)
2. Place the `.jar` in your server's `plugins/` folder
3. Ensure **Vault** and an economy plugin (e.g. EssentialsX) are installed and running
4. Restart the server (or use a plugin manager to load it)
5. Edit `plugins/Tollgate/config.yml` if you want to customize messages

## How to Use

### Setting up a toll gate

1. Place an **iron door** block
2. Place a **sign** on the block **directly above** the iron door
3. Write a **number** as the price on the sign (first line). Currency symbols are optional and stripped automatically.

```
┌──────────┐
│   50     │  ← sign with price
├──────────┤
│ IRON     │  ← iron door (top half)
│ DOOR     │
│          │
│ (bottom) │  ← iron door (bottom half)
└──────────┘
```

### Using a toll gate

- Hold **Sneak** (Shift) and **right-click** the iron door
- If you have enough money, the fee is deducted and you are teleported **1 block past the door**
- If you don't have enough, you'll see an "insufficient funds" message

## Configuration

`plugins/Tollgate/config.yml`:

```yaml
# Currency symbol displayed in messages
currency-symbol: "$"

# Payment cooldown per player (seconds, 0 = disabled)
cooldown: 0

messages:
  payment-success: "&aYou paid &6%price% &ato pass through the toll gate."
  insufficient-funds: "&cYou don't have enough money! Need &6%price%&c, you have &6%balance%&c."
  cooldown: "&cPlease wait &6%seconds% &cseconds before using this gate again."
  tollgate-created: "&aToll gate created with price &6%price%"
  tollgate-removed: "&cToll gate removed."
  plugin-reloaded: "&aTollgate config reloaded."
```

### Placeholders

| Placeholder | Description |
|-------------|-------------|
| `%price%` | The toll fee formatted with currency symbol |
| `%balance%` | The player's current balance after payment |
| `%seconds%` | Remaining cooldown seconds |

## Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/tollgate reload` | `tollgate.admin` | Reload `config.yml` |
| `/tollgate list` | `tollgate.admin` | List all active toll gates |
| `/tollgate remove` | `tollgate.admin` | Remove the toll gate at your current location |

## Permissions

| Permission | Default | Description |
|-----------|---------|-------------|
| `tollgate.admin` | OP | Access to all `/tollgate` subcommands |
| `tollgate.use` | true | Allows using toll gates (paying and passing) |

## How It Works

### Economy Integration

Tollgate uses the **Vault** API to interact with the server economy. This means it works with any Vault-compatible economy plugin — EssentialsX, CMI, TheNewEconomy, etc.

The plugin does **not** use EssentialsX's deprecated direct API (`com.earth2me.essentials.api.Economy`). Instead, it hooks into Vault's `Economy` interface:

```java
RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
Economy economy = rsp.getProvider();
```

Key Vault methods used:

| Method | Purpose |
|--------|---------|
| `Economy.has(OfflinePlayer, double)` | Check if player can afford the fee |
| `Economy.withdrawPlayer(OfflinePlayer, double)` | Deduct the fee |
| `Economy.getBalance(OfflinePlayer)` | Get player balance for messages |
| `Economy.format(double)` | Format amounts with currency symbol |

### Toll Gate Validation

A sign is recognized as a toll gate when:
1. It is placed on the block **directly above** an iron door
2. Its first line contains a valid number (positive, with or without `$`/`¢`/`€`/`¥` prefix)

## Building from Source

```bash
git clone https://github.com/eworld/Tollgate.git
cd Tollgate
mvn clean package
```

The compiled `.jar` will be in `target/`.

### Dependencies

```xml
<dependency>
    <groupId>io.papermc.paper</groupId>
    <artifactId>paper-api</artifactId>
    <version>26.1.2</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>com.github.MilkBowl</groupId>
    <artifactId>VaultAPI</artifactId>
    <version>1.7</version>
    <scope>provided</scope>
</dependency>
```

## License

MIT
