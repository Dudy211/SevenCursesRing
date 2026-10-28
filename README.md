# 七咒之戒 · SevenCursesRing

> 它凉得像一枚刚从深水里捞出来的戒指。
> 戴上它的人会听见——很远的深处，有玻璃在慢慢裂开。

纯**客户端**整活模组 · Forge 1.20.1 · 无服务端依赖 · MIT

戒指一旦戴上，七咒同时上身；`/curse toggle` 关不掉，**只有创造模式的手摘得下它**。

---

## 七咒

| # | 名 | 铭文 | 真实作用 |
|---|---|---|---|
| 1 | 停钟 | 钟摆停在格与格之间 | 帧率锁到 5~15，画面像幻灯片 |
| 2 | 淤积 | 有什么在胸腔里越堆越多 | 占 25% 堆 + 持续搅动，一阵一阵地顿（GC） |
| 3 | 垂烬 | 火将熄时，画面先乱 | 16 种渲染故障随机上演：贴图、花屏、黑幕、假死机 |
| 4 | 乱指 | 手还记得路，路已被挪走 | 全键打乱，只豁免 ESC；错一阵、好一阵 |
| 5 | 沉夜 | 白天也这样黑 | gamma 压到 0 + 黑雾，世界几乎全黑 |
| 6 | 失手 | 你指东，它偏往西 | 鼠标忽然不动，或者整个反向 |
| 7 | 迟世 | 世界开始慢半拍回应你 | 服务端每 tick 睡 60~200ms，TPS 掉到 4~8 |

「垂烬」底下的 16 种故障（`/curse glitch list` 可看）：

`贴图遗失` `经纬错织` `显存碎屑` `深浅失序` `颜色叛变` `断信` `呼吸不匀` `褪色`
`停钟` `沉夜` `失手` `空间拉伤` `目眩` `假死` `迟世` `淤积`

调度规则：同时只放一个故障；每次 2~6 秒（个别 ×2）；同一种故障冷却 3 秒；
默认只放强度上限以下的（`maxIntensity=2` = 全开）。

---

## 安装

1. Minecraft **1.20.1** + **Forge 47.x**
2. 装 [Curios API](https://www.curseforge.com/minecraft/mc-mods/curios)（5.14.1+）
3. 把 `sevencurses-forge-1.20.1-*.jar` 丢进 `mods/`

创造模式物品栏里有「七咒之戒」，也可以：

```
/give @p sevencurses:seven_curses_ring
```

戴上后右侧 HUD 会显示 `受咒：…` 与当前 `异象：…`。

---

## 指令

```
/curse list                              七咒状态（带真实作用）
/curse toggle <id>                       开/关某一咒（戒指戴着时无效）
/curse glitch list                       列出 16 种渲染故障
/curse glitch now <id>                   立刻放一个（跳过随机与强度过滤）
/curse glitch stop                       停止当前故障并复原
/curse diag                              诊断：Mixin 存活 / 实测 mspt / 淤积占用
/curse panic                             一键平息全部
```

`DELETE` 键 = 紧急解除（等同 `/curse panic`）。

### `/curse diag` 怎么读

```
迟世：在放  睡掉 37 tick / 5210ms  兜底=关
tickServer 调用 1832 次（Mixin 存活）
实测 mspt 132.4  推得 TPS 7.6
淤积：在放
  held=512MB target=512MB churn=320MB (搅动中)
```

- `tickServer 调用` 不涨 → Mixin 没挂上（会自动切 `ServerTickEvent` 兜底，`兜底=开`）
- 涨了但 `睡掉 0 tick` → flag 没置起来（咒没开，或被 `maxIntensity` 过滤）
- 睡了但 TPS 还是 20 → 看实测 mspt

---

## 配置

`config/sevencurses-client-v2.toml`（客户端配置，**文件名带 v2**，防止旧默认值覆盖）

```toml
[general]
enabled = true          # 总开关
hud = true              # 右下角状态提示

[glitch]
maxIntensity = 2        # 0=只出 LIGHT，1=含 MEDIUM，2=含 HEAVY
minIntervalTicks = 10   # 两次故障间隔 0.5~2.5 秒
maxIntervalTicks = 50
minDurationTicks = 40   # 单次 2~6 秒
maxDurationTicks = 120
cooldownTicks = 60      # 同种故障冷却 3 秒

[medium.memory_pressure]
holdFraction = 0.25     # 淤积：占 maxHeap 的多少（0~0.45）
churn = true            # 搅动线程（真 GC 卡顿）；关掉就只剩占位

[heavy.tps_spike]
minSleepMs = 60         # 迟世：每 tick 睡多久（400 → TPS 2~3）
maxSleepMs = 200
```

---

## 安全设计（有意为之的取舍）

整活可以，但不能把人玩坏：

- **不真 OOM**：淤积只占 25% 堆（夹 256MB~3GB，硬保证分配后剩 ≥25%），
  搅动线程低空自动歇，撞 `OutOfMemoryError` 立刻收手。
- **不高频闪烁**：`断信` / `假死` 是盖住并保持，不是黑白高频闪（光敏性癫痫风险）。
- **成对还原**：所有改 GL 状态的地方都 HEAD/RETURN 成对注入 + `stop()` 兜底；
  `resetAll()` 会把深度测试、深度写入、shader 颜色全部归位。
- **单次上限**：单次故障最长 400 tick（20 秒），同时只放一个。
- **出口永远在**：`DELETE` 键 / `/curse panic` 随时清空全部状态；
  摘戒指（创造模式）也会立刻触发 panic。
- **会自己认错**：`tps_spike` 十 tick 没见到 Mixin 心跳就自动切兜底通道并留日志。

已知边界：

- 「迟世」只对**单机 / 集成服务器**有效（调度器在客户端；独服不跑我们的代码）。
- 「淤积」是安全的**压力**，不是真 OOM；最狠也不过占 45% 堆。
- 搅动会让 CPU 占用上一个台阶，笔记本风扇会响——设计内。

---

## 自行构建

两个**第三方 jar 不随仓库分发**（版权 + 体积），构建前手动放进去：

```
mixin-libs/mixin-processor.jar                      # Mixin 0.8.5 的 annotation processor
compile_only_depend/curios-forge-5.14.1+1.20.1.jar  # Curios API
```

细节见这两个目录里的 `README.md`。贴图与 logo 以 Base64 存在 `assets-b64/`，
首次构建前跑一次 `bash tools/restore_assets.sh` 还原。然后：

```bash
JAVA_HOME=/path/to/jdk17 ./gradlew build
# 产物 build/libs/sevencurses-forge-1.20.1-<version>.jar
```

环境：JDK 17、Gradle 8.12（`gradle-wrapper.jar` 不入库，用本机 gradle，
或先跑一次 `gradle wrapper`）。

---

## 许可

MIT，见 [LICENSE](LICENSE)。玩得开心，别拿去恶心不认识的人。
