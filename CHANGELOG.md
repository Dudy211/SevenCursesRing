# 变更日志

## v1.4.0 — 2026-10-28

**文案**

- tooltip 重写：先说物（"它凉得像一枚刚从深水里捞出来的戒指"），再刻字，最后规矩一句。
- 七咒改名：停钟 / 淤积 / 垂烬 / 乱指 / 沉夜 / 失手 / 迟世，各配一句铭文。
- **每咒两行**：上面铭文、下面真实作用（新增 `curse.sevencurses.<id>.effect` 键）。
- 16 个渲染故障一并雅化（贴图遗失 / 经纬错织 / 显存碎屑 / … / 假死）。
- HUD 改为 `受咒：%s` / `异象：%s`，`/curse list` 也带作用串。

**迟世（TPS）**

- `MinecraftServer.tickServer` Mixin 每 tick 无条件 `tickSeen++`：**Mixin 存活变成可观测事实**。
- RETURN 处记 `nanoTime` 实测 tick 周期（EMA，丢弃 >2s 的暂停/加载样本）。
- `TpsSpikeGlitch.onTick()` 观察 10 tick：心跳不动 → WARN + 自动切 `ServerTickEvent` 兜底；
  心跳恢复则关掉兜底（避免两边同时睡）。
- 睡眠区间进配置：`heavy.tps_spike.minSleepMs/maxSleepMs`（默认 60/200）。
- 正在放「迟世」时 F3 显示**实测** mspt/TPS，并标 `[七咒 · 迟世 / 实测]`。

**淤积（内存）**

- 旧版只占 20% 堆且看门狗在堆较满时**一个字节都不分配**（静默失效），重写。
- 双层：占位 `maxHeap × holdFraction`（默认 0.25，夹 256MB~3GB，硬保证留 25% 空）
  + 搅动线程 `sevencurses-memchurn`（4MB 轮转、保留 12 个 ≈48MB 活动集）。
- `hardStop()` 统一清理（池 + 线程 + flag），`GlitchRuntime.resetAll()` 改调它。
- F3 多一行 `淤积: held / target / churn`。

**新增**

- `/curse diag` 诊断面板：Mixin 存活、睡掉多少 tick/ms、实测 mspt、淤积占用、配置口径。
- `RenderGlitch.onTick()` 默认方法（播放期间的每 tick 回调）。

**修复**

- `GlitchRuntime.seed` 从未赋值 → 「贴图遗失」每轮坏的都是同一批贴图。
- `ClientBootstrap` 重复 import。

---

## v1.3.0 — 2026-10-03

- 七咒全部落地（停钟 / 内存 / 显卡 / 键盘 / 亮度 / 鼠标 / TPS）。
- 15→16 个渲染故障；调度器加权随机、冷却、强度过滤、单次上限。
- `DebugScreenOverlayMixin` 塞假 GPU / mspt / TPS 行。

## v1.1.x — 2026-10-02

- TPS 咒改走 `MinecraftServer.tickServer` Mixin（`ServerTickEvent` 不可靠）。
- 锁内存改成"安全版"（有界池 + 看门狗，不真 OOM）。

## v1.0.0 — 2026-10-01

- 戒指本体（Curios 饰品，创造模式才可取下）+ 七咒框架。

## v0.2.0 — 2026-09-30

- 第 3 咒「显卡临终」完整版（LIGHT / MEDIUM / HEAVY 全组）。
- 第 4 咒「键盘错位」：SWAP / ROTATE / DEAD 三种模式。

## v0.1.0 — 2026-09-30

- 项目起步：贴图丢失 / UV 拉花 / 假花屏。
