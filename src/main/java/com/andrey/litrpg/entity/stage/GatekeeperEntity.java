package com.andrey.litrpg.entity.stage;

import com.andrey.litrpg.entity.BossDamage;
import com.andrey.litrpg.entity.Telegraph;
import com.andrey.litrpg.event.MobLevels;
import com.andrey.litrpg.fx.Fx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Привратник Костей (этап 1, ур. 4). Огромный костяной страж в цепях.
 * <ul>
 *   <li>«Цепной хлыст» — конус вперёд по танку;</li>
 *   <li>«Круг цепей» / «Петля» — то уходи от него, то прижмись (читайте разметку);</li>
 *   <li>«Прыжок» — к самому дальнему, удар по кругу при приземлении;</li>
 *   <li>«Залп костей» — бьёт всех, кого видит: прячьтесь за колоннами!</li>
 *   <li>с 50%: «Обрушение» — рвёт цепью колонну (полоса удара), укрытий становится меньше;
 *       «Оковы» — сковывает двоих: разойдутся дальше 9 блоков — цепь жжёт обоих.</li>
 * </ul>
 */
public class GatekeeperEntity extends StageBoss {
    private final List<BlockPos> pillars = new ArrayList<>();
    private UUID chainA, chainB;
    private int chainTicks;
    private Vec3 leapTo;
    private Telegraph leapZone;

    public GatekeeperEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.WHITE);
        skill("Цепной хлыст", 120, 16, 1).first(40)
                .when(t -> distanceTo(t) < 8)
                .begin(t -> cone(position(), dirTo(t), 8, 35, 16, 0.95f).color(Telegraph.ORANGE)
                        .then(e -> BossDamage.sunder(e, 1)))
                .act(t -> {
                    fxSlash(dirTo(t), 7, 80, 15, 0xE0D8C8A0);
                    sound(SoundEvents.CHAIN_BREAK, 2f, 0.6f);
                });
        skill("Круг цепей", 220, 26, 2).first(120)
                .begin(t -> {
                    if (random.nextBoolean()) {
                        circle(position(), 6, 26, 0.85f).color(Telegraph.RED);
                        shoutAll("«Круг цепей» — отбегите от Привратника!");
                    } else {
                        donut(position(), 4, 15, 30, 0.75f).color(Telegraph.PURPLE);
                        shoutAll("«Петля» — бегите к Привратнику, в центр!");
                    }
                })
                .act(t -> {
                    fxRing(position(), 1, 15, 14, 0xE0C8C8B0);
                    sound(SoundEvents.CHAIN_PLACE, 2.5f, 0.5f);
                });
        skill("Прыжок", 260, 30, 1).first(200)
                .begin(t -> {
                    LivingEntity far = farthest();
                    leapTo = far.position();
                    leapZone = circle(leapTo, 4.5, 30, 0.7f).color(Telegraph.RED).follow(far, 12)
                            .then(e -> knockFrom(e, leapZone.center(), 1.2, 0.5));
                })
                .act(t -> {
                    Vec3 to = leapZone != null ? leapZone.center() : leapTo != null ? leapTo : t.position();
                    teleportTo(to.x, to.y, to.z);
                    fxRing(to, 0.5, 6, 12, 0xF0FFFFFF);
                    burst(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DEEPSLATE_BRICKS.defaultBlockState()), to, 40, 1.5, 0.3);
                    sound(SoundEvents.GENERIC_EXPLODE, 1.5f, 0.6f);
                });
        skill("Залп костей", 340, 40, 2).first(160).unstoppable()
                .begin(t -> shoutAll("«Залп костей» — спрячьтесь за колонной! Видит — попадёт."))
                .during(left -> {
                    if (left % 8 == 0) fxRing(position(), 0.5, 3, 10, 0xC0FFF0D0);
                })
                .act(t -> volley());
        skill("Подъём стражи", 560, 30, 2).first(300)
                .act(t -> {
                    adds(EntityType.SKELETON, arena(), 2, 4, MobLevels.Rank.NORMAL, null);
                    shoutAll("Привратник поднимает стражу — убейте скелетов, пока их не стало больше.");
                    sound(SoundEvents.SKELETON_AMBIENT, 2f, 0.5f);
                });
        skill("Обрушение", 380, 40, 2).fromPhase(1).first(80).unstoppable()
                .begin(t -> {
                    BlockPos pl = standingPillar();
                    if (pl == null) return;
                    Vec3 from = Vec3.atBottomCenterOf(pl).add(0.5, 0, 0.5);
                    Vec3 dir = arena().subtract(from);
                    line(new Vec3(from.x, arena().y, from.z), dir, 16, 1.8, 40, 1.0f).color(Telegraph.ORANGE);
                    fxBeam(chest(), from.add(0, 6, 0), 0.25, 40, 0xC0A0A0A0);
                    shoutAll("Привратник тянет цепь — колонна рухнет! Уйдите с её пути.");
                })
                .act(t -> collapseNearest());
        skill("Оковы", 420, 20, 1).fromPhase(1).first(200)
                .act(t -> chain());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 600.0)
                .add(Attributes.ATTACK_DAMAGE, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.26)
                .add(Attributes.ARMOR, 8.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override public int getBossLevel() { return 4; }
    @Override public int getXpReward() { return 0; }
    // первый хранитель: было 4200 — группа за минуту снимала одно деление из десяти, убавлено в 1.5 раза
    @Override protected float ehp() { return 2800; }

    @Override
    protected float[] phaseThresholds() {
        return new float[]{0.5f};
    }

    @Override
    protected List<String> intro() {
        return List.of("Колонны — ваше укрытие от «Залпа костей». С половины здоровья он их обрушивает.",
                "«Круг цепей» — отбегите; «Петля» — к нему вплотную.",
                "С 50%: «Оковы» связывают двоих — держитесь ближе 9 блоков друг к другу.");
    }

    @Override
    protected void onArena() {
        pillars.clear();
        for (Vec3 v : points("pillars")) pillars.add(BlockPos.containing(v.x, v.y, v.z));
    }

    @Override
    protected void onPhase(int phase) {
        shout("Цепи… рвутся!");
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 0));
        fxPillar(position(), 2, 8, 20, 0xC0FFE0A0);
    }

    // ---------------- залп ----------------

    private void volley() {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 eye = getEyePosition();
        int hit = 0;
        for (ServerPlayer p : players()) {
            Vec3 tgt = chestOf(p);
            var res = sl.clip(new ClipContext(eye, tgt, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (res.getType() == HitResult.Type.BLOCK) {
                fxBeam(eye, res.getLocation(), 0.2, 6, 0xA0E0D8C0);
                burst(ParticleTypes.CRIT, res.getLocation(), 8, 0.2, 0.1);
                continue;
            }
            fxBeam(eye, tgt, 0.35, 8, 0xF0FFF0D0);
            if (hit(p, 0.85f)) hit++;
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }
        sound(SoundEvents.SKELETON_SHOOT, 3f, 0.5f);
        if (hit > 0) shoutAll("Залп достал " + hit + " — за колоннами вы были бы целы.");
    }

    // ---------------- колонны ----------------

    private BlockPos standingPillar() {
        List<BlockPos> ok = new ArrayList<>();
        for (BlockPos p : pillars) if (!broken.containsKey(p.above(2)) && !level().getBlockState(p.above(2)).isAir()) ok.add(p);
        if (ok.isEmpty()) return null;
        ok.sort((a, b) -> Double.compare(a.distSqr(blockPosition()), b.distSqr(blockPosition())));
        return ok.get(Math.min(ok.size() - 1, random.nextInt(Math.min(3, ok.size()))));
    }

    private void collapseNearest() {
        BlockPos pl = standingPillar();
        if (pl == null) return;
        if (!(level() instanceof ServerLevel sl)) return;
        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) {
                for (int y = 1; y < 12; y++) {
                    BlockPos b = pl.offset(dx, y, dz);
                    BlockState st = level().getBlockState(b);
                    if (st.isAir()) continue;
                    breakBlock(b);
                    if (y % 3 == 0) sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5,
                            12, 0.5, 0.5, 0.5, 0.1);
                }
            }
        }
        // обломки — короткие «пеньки» мешают ходить
        for (int i = 0; i < 6; i++) {
            Vec3 v = Vec3.atBottomCenterOf(pl).add((random.nextDouble() - 0.5) * 8, 0, (random.nextDouble() - 0.5) * 8);
            tempBlock(BlockPos.containing(v), Blocks.COBBLED_DEEPSLATE_SLAB.defaultBlockState(), 20 * 60);
        }
        fxRing(Vec3.atBottomCenterOf(pl), 1, 7, 14, 0xE0B0A090);
        sound(SoundEvents.GENERIC_EXPLODE, 2.5f, 0.5f);
        shoutAll("Колонна рухнула — укрытий меньше.");
    }

    private void restorePillars() {
        restoreBroken();
    }

    // ---------------- оковы ----------------

    private void chain() {
        List<LivingEntity> list = new ArrayList<>();
        for (ServerPlayer p : players()) if (p != getTarget()) list.add(p);
        if (list.size() < 2) list = new ArrayList<>(players());
        if (list.size() < 2) return;
        java.util.Collections.shuffle(list, new java.util.Random(random.nextLong()));
        LivingEntity a = list.get(0), b = list.get(1);
        chainA = a.getUUID();
        chainB = b.getUUID();
        chainTicks = 200;
        Fx.tether(level(), a, b, 0.25, 200, 0xE0B0B0B0);
        tip(a, "Вы скованы с " + b.getName().getString() + " — держитесь рядом!", ChatFormatting.GOLD);
        tip(b, "Вы скованы с " + a.getName().getString() + " — держитесь рядом!", ChatFormatting.GOLD);
        sound(SoundEvents.CHAIN_PLACE, 2f, 0.8f);
    }

    @Override
    protected void bossTick() {
        if (chainTicks > 0 && --chainTicks % 20 == 0 && level() instanceof ServerLevel sl) {
            if (sl.getEntity(chainA) instanceof LivingEntity a && sl.getEntity(chainB) instanceof LivingEntity b && a.isAlive() && b.isAlive()) {
                if (a.distanceTo(b) > 9) {
                    hit(a, 0.18f);
                    hit(b, 0.18f);
                    fxBeam(chestOf(a), chestOf(b), 0.4, 10, 0xF0FF4020);
                }
            } else chainTicks = 0;
        }
    }

    private LivingEntity farthest() {
        LivingEntity best = getTarget();
        double bd = -1;
        for (ServerPlayer p : players()) {
            double d = p.distanceToSqr(this);
            if (d > bd) {
                bd = d;
                best = p;
            }
        }
        return best;
    }

    @Override
    protected void onReset() {
        super.onReset();
        restorePillars();
        chainTicks = 0;
    }

    @Override
    public void die(DamageSource src) {
        restorePillars();
        super.die(src);
    }

    @Override
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        return w == BossDamage.Weapon.BLUNT || w == BossDamage.Weapon.PICKAXE ? 1.4f : w == BossDamage.Weapon.ARROW ? 0.75f : 1f;
    }

    @Override
    protected void registerGoals() {
        meleeGoals(1.0);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.SKELETON_AMBIENT; }
    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.SKELETON_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SKELETON_DEATH; }
    @Override public float getVoicePitch() { return 0.4f; }
}
