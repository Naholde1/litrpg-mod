package com.andrey.litrpg.entity.stage;

import com.andrey.litrpg.entity.BossDamage;
import com.andrey.litrpg.entity.Telegraph;
import com.andrey.litrpg.event.MobLevels;
import com.andrey.litrpg.fx.Fx;
import com.andrey.litrpg.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Древень-гниль (этап 2, ур. 17). Роща из мёртвых деревьев.
 * <ul>
 *   <li>«Удар ветвями» — широкий конус по танку; «Хватка корней» — двое прикованы к месту и теряют здоровье;</li>
 *   <li>«Ростки» — в лунках вокруг поднимаются древни-ростки: кто проживёт 20 с — врастает в Древня и лечит его на 8%;</li>
 *   <li>«Гнилые споры» — облака гнили остаются на полу;</li>
 *   <li>с 50%: «Корни рощи» — стены корней делят арену на коридоры (на 20 с), меняя поле боя;</li>
 *   <li>с 20%: «Последний рост» — ростки во всех лунках разом. Огонь жжёт его кору (×1,5).</li>
 * </ul>
 */
public class RotEntEntity extends StageBoss {
    private final Map<UUID, Long> sprouts = new HashMap<>();

    public RotEntEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.GREEN);
        skill("Удар ветвями", 110, 18, 1).first(40)
                .when(t -> distanceTo(t) < 9)
                .begin(t -> cone(position(), dirTo(t), 9, 60, 18, 1.0f).color(Telegraph.GREEN).then(e -> BossDamage.sunder(e, 1)))
                .act(t -> {
                    fxSlash(dirTo(t), 8, 120, 10, 0xE0A08040);
                    sound(SoundEvents.WOOD_BREAK, 2f, 0.5f);
                });
        skill("Хватка корней", 300, 20, 2).first(120)
                .begin(t -> {
                    List<ServerPlayer> ps = new ArrayList<>(players());
                    java.util.Collections.shuffle(ps, new java.util.Random(random.nextLong()));
                    for (int i = 0; i < Math.min(2, ps.size()); i++) circle(ps.get(i).position(), 2, 20, 0.2f).color(Telegraph.GREEN)
                            .then(e -> {
                                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 7));
                                e.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 1));
                                Fx.spiral(level(), e, 0.7, 1.5, 3, 120, 0xC0806030);
                                tip(e, "Корни держат вас — лекарь, сюда!", ChatFormatting.GOLD);
                            });
                })
                .act(t -> sound(SoundEvents.ROOTED_DIRT_BREAK, 2f, 0.5f));
        skill("Ростки", 600, 30, 2).first(240)
                .act(t -> sprout(2));
        skill("Гнилые споры", 260, 24, 2).first(160)
                .begin(t -> {
                    for (int i = 0; i < 3; i++) {
                        Vec3 at = i == 0 ? randomOther().position() : randomSpot(16);
                        circle(at, 3, 24, 0.3f).color(Telegraph.GREEN).fx(c -> hazard(c, 3, 20 * 15, 0.08f, 0xA0808020,
                                e -> e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0))));
                    }
                })
                .act(t -> sound(SoundEvents.FUNGUS_BREAK, 2f, 0.5f));
        skill("Корни рощи", 700, 40, 2).fromPhase(1).first(40).unstoppable()
                .begin(t -> shoutAll("Корни рощи прорастают — арену делят стены!"))
                .act(t -> rootWalls());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 900.0)
                .add(Attributes.ATTACK_DAMAGE, 11.0)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override public int getBossLevel() { return 17; }
    @Override public int getXpReward() { return 0; }
    @Override protected float ehp() { return 19200; }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.5f, 0.2f};
    }

    @Override
    protected List<String> intro() {
        return List.of("Ростки в лунках — рубите их за 20 с, иначе они врастут в Древня и вылечат его.",
                "«Хватка корней» держит двоих — лекарю их лечить, остальным не стоять в спорах.",
                "Огонь жжёт его кору: урон огнём ×1,5.");
    }

    private void sprout(int count) {
        List<Vec3> spots = new ArrayList<>(points("saplings"));
        java.util.Collections.shuffle(spots, new java.util.Random(random.nextLong()));
        for (int i = 0; i < Math.min(count, spots.size()); i++) {
            Vec3 at = spots.get(i);
            var t = ModEntities.WILD.get("treant");
            if (t == null) continue;
            adds(t.get(), at, 1, 12, MobLevels.Rank.NORMAL, m -> {
                sprouts.put(m.getUUID(), level().getGameTime() + 400);
                Fx.mark(level(), m, 0.45, 2, 400, 0xD080FF60);
            });
        }
        shoutAll("Ростки поднимаются! Срубите их за 20 секунд.");
        sound(SoundEvents.BIG_DRIPLEAF_TILT_DOWN, 2f, 0.5f);
    }

    @Override
    protected void bossTick() {
        if (sprouts.isEmpty() || tickCount % 10 != 0) return;
        long now = level().getGameTime();
        for (Mob m : minions()) {
            Long due = sprouts.get(m.getUUID());
            if (due == null || now < due) continue;
            sprouts.remove(m.getUUID());
            fxBeam(m.position().add(0, 1, 0), chest(), 0.4, 14, 0xE060FF40);
            m.discard();
            heal(getMaxHealth() * 0.08f);
            shoutAll("Росток врос в Древня — он лечится!");
        }
        sprouts.keySet().removeIf(id -> minions().stream().noneMatch(m -> m.getUUID().equals(id)));
    }

    /** Стены корней: 3 полосы по направлению к случайной стороне, с проходами. */
    private void rootWalls() {
        Vec3 c = arena();
        boolean alongX = random.nextBoolean();
        for (int k = -1; k <= 1; k++) {
            int off = k * 7;
            int gap = random.nextInt(9) - 4;
            for (int i = -18; i <= 18; i++) {
                if (Math.abs(i - gap) <= 1) continue;
                for (int y = 0; y < 3; y++) {
                    BlockPos p = alongX ? BlockPos.containing(c.x + i, c.y + y, c.z + off) : BlockPos.containing(c.x + off, c.y + y, c.z + i);
                    tempBlock(p, Blocks.MANGROVE_ROOTS.defaultBlockState(), 20 * 20);
                }
            }
        }
        fxRing(c, 1, 18, 16, 0xC0806030);
        sound(SoundEvents.ROOTS_BREAK, 3f, 0.4f);
    }

    @Override
    protected void onPhase(int phase) {
        if (phase == 2) {
            shout("Роща… вечна!");
            sprout(6);
        } else shout("Вы рубите корни — я пущу новые.");
    }

    @Override
    protected void onReset() {
        super.onReset();
        sprouts.clear();
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        return w == BossDamage.Weapon.FIRE ? 1.5f : w == BossDamage.Weapon.AXE ? 1.25f : w == BossDamage.Weapon.ARROW ? 0.7f : 1f;
    }

    @Override
    protected void registerGoals() {
        meleeGoals(0.9);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.WOOD_STEP; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.WOOD_HIT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.WOOD_BREAK; }
    @Override public float getVoicePitch() { return 0.4f; }
}
