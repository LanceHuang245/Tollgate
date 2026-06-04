[English](README.md) | [简体中文](README_CN.md)

# Tollgate

A Minecraft Paper plugin that adds toll gates to your server. Players place an iron door with a sign above it to create a toll gate — sneaking and right-clicking the door deducts a fee via Vault economy and teleports the player through.

## Requirements

- **Paper 1.20.6+**
- **Java 21+**
- **Vault** plugin
- A Vault-compatible economy plugin (e.g. [EssentialsX](https://essentialsx.net/))

## Features

- Iron door + sign above = toll gate
- Write `[Tollgate]` on the sign to start creation, then enter a custom title and price via chat
- Players must be within **2 blocks** (horizontal + vertical) of the door to interact
- Players **sneak + right-click** the iron door to pay and pass through
- Fee deduction via Vault economy — works with EssentialsX, CMI, or any Vault-based economy
- Automatic tollgate registration upon creation completion, automatic deregistration on break
- Custom title support via chat input during creation
- Particle burst effect (END_ROD) on passage, configurable and visible only to the passer
- Persisted tollgate data survives server restarts, including in multi-world environments
- Configurable messages and cooldowns
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
2. Place a **wall sign** on the block **directly above** the iron door
3. Write `[Tollgate]` on the sign's first line — this starts the creation flow
4. Type a **custom title** in chat when prompted
5. Type a **price** in chat when prompted (supports currency symbols like `$50` or plain numbers like `50`)

Type `cancel` at any prompt to abort.

```
┌──────────┐
│[Tollgate]│  ← sign with creation tag
├──────────┤
│ IRON     │  ← iron door (top half)
│ DOOR     │
│          │
│ (bottom) │  ← iron door (bottom half)
└──────────┘
```

### Using a toll gate

- Hold **Sneak** (Shift) and **right-click** the iron door **from within 2 blocks**
- If you have enough money, the fee is deducted and you are teleported past the door
- If you don't have enough, you'll see an "insufficient funds" message
- A 3-second cooldown prevents rapid reuse per tollgate

## Configuration

`plugins/Tollgate/config.yml`:

```yaml
# Payment cooldown per tollgate per player (seconds, 0 = disabled)
cooldown: 3

# Particle effects when a player passes through a tollgate (visible to the passing player only)
particles:
  enabled: true
  count: 25
  radius: 1.2

messages:
  enter-title: "&aPlease enter a custom title:"
  enter-price: "&aPlease enter the price:"
  enter-timeout: "&cTollgate creation timed out. Cancelled."
  tollgate-created: "&aTollgate created! Title: &6%title%&a, Price: &6%price%"
  tollgate-create-failed: "&cTollgate creation failed! Price must be positive."
  invalid-price: "&cInvalid price. Please enter a positive number."
  payment-success: "&aYou paid &6%price% &ato pass through the toll gate."
  insufficient-funds: "&cYou don't have enough money! Need &6%price%&c, you have &6%balance%&c."
  cooldown: "&cPlease wait &6%seconds% &cseconds before using this gate again."
  tollgate-removed: "&cToll gate removed."
  tollgate-not-found: "&cNo toll gate found here."
  no-permission: "&cYou don't have permission to use this command."
  plugin-reloaded: "&aTollgate config reloaded."
  title-cancelled: "&cTollgate creation cancelled."
  payment-received: "&a%player% &7passed your tollgate &6%title%&7. You received &6%price%&7. Total: &6%revenue%"
  tollgate-invalid-door: "&cThe iron door was removed. Creation cancelled."
  tollgate-invalid-sign: "&cThe sign was removed. Creation cancelled."
```

### Placeholders

| Placeholder | Description |
|-------------|-------------|
| `%price%` | The toll fee formatted with currency symbol |
| `%balance%` | The player's current balance after payment |
| `%seconds%` | Remaining cooldown seconds |
| `%title%` | The custom title of the tollgate |
| `%player%` | The name of the player who passed through |
| `%revenue%` | The tollgate's total lifetime revenue |

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
1. It is a **wall sign** placed on the block **directly above** an iron door
2. Its first line is exactly `[Tollgate]`
3. The sign text triggers a chat input flow: the player types a title, then a price (positive number, with or without `$`/`¢`/`€`/`¥` prefix)

### Interaction Limits

Players must be within 2 blocks of the door horizontally and vertically to interact. Interactions beyond this range are silently ignored to prevent accidental remote triggering.

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
    <version>1.20.6-R0.1-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>com.github.MilkBowl</groupId>
    <artifactId>VaultAPI</artifactId>
    <version>1.7</version>
    <scope>provided</scope>
</dependency>
```

### Toll Gate Persistence

All tollgate data is saved to `data.yml` and restored on server restart. For worlds that load lazily (e.g., multiverse, BskyBlock, or custom world generators), tollgates are automatically registered when the world becomes available — no manual intervention required.

## License

MIT
