[English](README.md) | [简体中文](README_CN.md)

# Tollgate

一款 Minecraft Paper 插件，为服务器添加收费门功能。玩家在铁门上方放置告示牌即可创建收费门——按住潜行键右键铁门将通过 Vault 经济系统扣款，并将玩家传送到门后方。

## 运行要求

- **Paper 26.1.x**
- **Java 25+**
- **Vault** 插件
- 任意 Vault 兼容的经济插件（推荐 [EssentialsX](https://essentialsx.net/)）

## 功能特性

- 铁门 + 上方告示牌 = 收费门
- 告示牌第一行文字即为价格（例如 `50` 或 `$50`）
- 玩家**潜行（Shift）+ 右键点击**铁门即可付费通行
- 通过 Vault 经济 API 扣款——兼容 EssentialsX、CMI 等所有 Vault 经济插件
- 在铁门上方放置有效告示牌时自动注册收费门
- 告示牌或铁门被破坏时自动注销收费门
- 创建时通过聊天输入自定义标题
- 通行时播撒粒子迸发特效（END_ROD），可配置且仅对通行玩家可见
- 收费站数据在服务器重启后持久化，支持多世界环境
- 可自定义消息文本与冷却时间
- 管理员命令支持

## 安装方法

1. 从 [Releases](https://github.com/eworld/Tollgate/releases) 下载 `Tollgate-x.x.x.jar`
2. 将 `.jar` 文件放入服务器的 `plugins/` 目录
3. 确保已安装并启用 **Vault** 和一款经济插件（如 EssentialsX）
4. 重启服务器（或使用插件管理器热加载）
5. 如需自定义消息，编辑 `plugins/Tollgate/config.yml`

## 使用说明

### 创建收费门

1. 放置一个**铁门**方块
2. 在铁门**正上方**的方块上放置一个**告示牌**
3. 在告示牌第一行写下**价格数字**。货币符号可选，插件会自动去除。

```
┌──────────┐
│   50     │  ← 告示牌，标注价格
├──────────┤
│   铁门   │  ← 铁门（上半部分）
│          │
│   铁门   │  ← 铁门（下半部分）
└──────────┘
```

### 使用收费门

- 按住**潜行键（Shift）**并**右键点击**铁门
- 余额足够时，扣除费用并传送到**门后 1 格**
- 余额不足时，显示"余额不足"提示

## 配置文件

`plugins/Tollgate/config.yml`：

```yaml
# 消息中显示的货币符号
currency-symbol: "$"

# 每位玩家的付费冷却时间（秒，0 = 禁用）
cooldown: 0

# 玩家通行时的粒子特效（仅对通行玩家可见）
particles:
  enabled: true
  count: 25
  radius: 1.2

messages:
  payment-success: "&a你已支付 &6%price% &a通过收费门。"
  insufficient-funds: "&c余额不足！需要 &6%price%&c，你只有 &6%balance%&c。"
  cooldown: "&c请等待 &6%seconds% &c秒后再使用此收费门。"
  tollgate-created: "&a收费门已创建，价格 &6%price%"
  tollgate-removed: "&c收费门已移除。"
  plugin-reloaded: "&aTollgate 配置已重载。"
```

### 占位符

| 占位符 | 说明 |
|--------|------|
| `%price%` | 通行费用（带货币符号） |
| `%balance%` | 玩家扣款后的余额 |
| `%seconds%` | 剩余冷却秒数 |

## 命令

| 命令 | 权限 | 说明 |
|------|------|------|
| `/tollgate reload` | `tollgate.admin` | 重载 `config.yml` |
| `/tollgate list` | `tollgate.admin` | 列出所有活跃的收费门 |
| `/tollgate remove` | `tollgate.admin` | 移除当前位置的收费门 |

## 权限

| 权限节点 | 默认 | 说明 |
|---------|------|------|
| `tollgate.admin` | OP | 可使用所有 `/tollgate` 子命令 |
| `tollgate.use` | true | 允许使用收费门（付费通行） |

## 工作原理

### 经济系统对接

Tollgate 通过 **Vault** API 与服务器经济系统交互，兼容所有 Vault 经济插件——EssentialsX、CMI、TheNewEconomy 等。

插件**不使用** EssentialsX 已废弃的直连 API（`com.earth2me.essentials.api.Economy`），而是通过 Vault 的 `Economy` 接口：

```java
RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
Economy economy = rsp.getProvider();
```

使用的核心 Vault 方法：

| 方法 | 用途 |
|------|------|
| `Economy.has(OfflinePlayer, double)` | 检查玩家余额是否足够 |
| `Economy.withdrawPlayer(OfflinePlayer, double)` | 从玩家账户扣款 |
| `Economy.getBalance(OfflinePlayer)` | 获取玩家余额用于消息显示 |
| `Economy.format(double)` | 格式化金额为带货币符号的字符串 |

### 收费门验证

告示牌被识别为收费门需满足：
1. 告示牌放置于铁门**正上方**的方块
2. 告示牌第一行包含有效数字（正数，可带 `$`/`¢`/`€`/`¥` 前缀）

## 从源码构建

```bash
git clone https://github.com/eworld/Tollgate.git
cd Tollgate
mvn clean package
```

编译后的 `.jar` 文件位于 `target/` 目录。

### Maven 依赖

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

### 收费站数据持久化

所有收费站数据保存至 `data.yml`，服务器重启后自动恢复。对于延迟加载的世界（如多世界插件、空岛世界等），收费站会在世界加载完成时自动注册，无需手动干预。

## 开源协议

MIT
