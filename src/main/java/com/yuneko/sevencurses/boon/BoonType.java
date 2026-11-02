package com.yuneko.sevencurses.boon;

/**
 * 七福 —— 与七咒一体两面。
 *
 * <p>规则只有一条：<b>机器越好，福越弱；机器越烂，福越强。</b>
 * 七咒惩罚所有人，七福只补偿穷人。
 *
 * <p>每一条都挂在七咒的对面（见 {@link #curseId}）：
 * <ul>
 *   <li>停钟（压帧率）↔ 缓（帧率上限按显卡反向给）</li>
 *   <li>淤积（吃内存）↔ 熄（生命上限按内存反向给）</li>
 *   <li>垂烬（渲染故障）↔ 净（护甲按实测帧率反向给，太快就倒扣）</li>
 *   <li>乱指（键盘）↔ 稳（伤害按 CPU 核心数反向给）</li>
 *   <li>沉夜（黑）↔ 明（经验按分辨率反向给）</li>
 *   <li>失手（鼠标）↔ 远（移速按视距反向给）</li>
 *   <li>迟世（TPS）↔ 谦（等级越高，七福整体衰减）</li>
 * </ul>
 */
public enum BoonType {

    /** 熄 —— 生命上限按堆内存反向给。 */
    HEALTH("health", "boon.sevencurses.health", "memory"),
    /** 缓 —— 帧率上限按显卡档位反向给。 */
    FRAMECAP("framecap", "boon.sevencurses.framecap", "framelock"),
    /** 稳 —— 攻击伤害按 CPU 核心数反向给。 */
    DAMAGE("damage", "boon.sevencurses.damage", "keyscramble"),
    /** 远 —— 移动速度按视距反向给。 */
    SPEED("speed", "boon.sevencurses.speed", "mouse"),
    /** 净 —— 护甲按实测帧率反向给（超过 120fps 会倒扣）。 */
    ARMOR("armor", "boon.sevencurses.armor", "gpu"),
    /** 明 —— 经验获取按窗口分辨率反向给。 */
    XP("xp", "boon.sevencurses.xp", "gamma"),
    /** 谦 —— 等级越高，七福整体越弱（总阀门）。 */
    HUMILITY("humility", "boon.sevencurses.humility", "tps");

    private final String id;
    private final String translationKey;
    /** 对面那个「咒」的 id，只用于文档与诊断显示。 */
    private final String curseId;

    BoonType(String id, String translationKey, String curseId) {
        this.id = id;
        this.translationKey = translationKey;
        this.curseId = curseId;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return translationKey;
    }

    public String curseId() {
        return curseId;
    }
}
