package com.yuneko.sevencurses.curse;

import java.util.Optional;

/**
 * 七咒。
 *
 * <p>七咒如今全部落地；枚举保持完整，这样 {@code /curse list} 和 HUD 一开始就是齐的。
 *
 * <p>注意：{@code GPU} 是「垂烬」，它底下才是那 16 个渲染故障 ——
 * 其余六咒各自只有一个效果。
 */
public enum CurseType {

    FRAME_LOCK("framelock", "curse.sevencurses.framelock"),
    MEMORY("memory", "curse.sevencurses.memory"),
    GPU("gpu", "curse.sevencurses.gpu"),
    KEY_SCRAMBLE("keyscramble", "curse.sevencurses.keyscramble"),
    GAMMA("gamma", "curse.sevencurses.gamma"),
    MOUSE("mouse", "curse.sevencurses.mouse"),
    TPS("tps", "curse.sevencurses.tps");

    private final String id;
    private final String translationKey;

    CurseType(String id, String translationKey) {
        this.id = id;
        this.translationKey = translationKey;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return translationKey;
    }

    /** 这个咒当前是否已经实现（没实现的 toggle 了也不会有效果）。 */
    public boolean implemented() {
        return true;  // 七咒全部实现
    }

    public static Optional<CurseType> byId(String id) {
        for (CurseType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
