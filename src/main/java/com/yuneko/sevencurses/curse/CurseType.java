package com.yuneko.sevencurses.curse;

import java.util.Optional;

/**
 * 七咒。
 *
 * <p>当前只有 GPU（第 3 咒）真正落地，其余六个是预留开关位 ——
 * 保持枚举完整，这样 {@code /curse list} 和 HUD 从一开始就是完整的。
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
