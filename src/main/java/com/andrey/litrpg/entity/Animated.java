package com.andrey.litrpg.entity;

/** Существо с позой «подготовки навыка» для моделей из данных. */
public interface Animated {
    /** 0 — нет, 1 — замах, 2 — руки в стороны. */
    int castPose();

    /** Плавная анимация подготовки на клиенте (0..1). */
    float castAnim(float partialTick);

    /** Проснулся ли (маскировка: мимик-сундук, пугало, трясинник): 0 — притворяется, 1 — живой. */
    default float awakeAnim(float partialTick) {
        return 1f;
    }

    default boolean isCasting() {
        return castPose() > 0;
    }
}
