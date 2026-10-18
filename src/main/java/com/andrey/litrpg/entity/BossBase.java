package com.andrey.litrpg.entity;

import com.andrey.litrpg.event.MobLevels;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Основа боссов. Даёт полосу здоровья, фазы, навыки с подготовкой
 * (босс замирает, светится и выкрикивает название — у игрока есть время
 * отбежать), привязку к алтарю подземелья и сброс боя, если все ушли.
 */
public abstract class BossBase extends Monster implements LitBoss {
    /** Метка призванных боссом существ (UUID босса). */
    public static final String MINION_TAG = "litrpg_minion_of";
    private static final EntityDataAccessor<Integer> CAST = SynchedEntityData.defineId(BossBase.class, EntityDataSerializers.INT);
    private static final UUID PARTY_HP_ID = UUID.fromString("5b1e3c9a-1f2d-4e8a-9c7b-2a6d4f8e0b11");

    protected final ServerBossEvent bossEvent;
    protected final List<Skill> skills = new ArrayList<>();
    /** Алтарь, у которого босс появился (может отсутствовать — призыв предметом или командой). */
    protected BlockPos home;
    protected int phase = 0;
    private Skill casting;
    private int castLeft;
    private LivingEntity castTarget;
    private int lonelyTicks;
    private int globalCooldown = 40;
    /** Для плавной анимации подготовки на клиенте. */
    public float castAnim, castAnimO;

    protected BossBase(EntityType<? extends Monster> type, Level level, BossEvent.BossBarColor color) {
        super(type, level);
        this.bossEvent = new ServerBossEvent(Component.empty(), color, BossEvent.BossBarOverlay.NOTCHED_10);
        this.bossEvent.setDarkenScreen(true);
        this.xpReward = 100;
        this.setPersistenceRequired();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CAST, 0);
    }

    // ================= навыки =================

    /** Навык босса: перезарядка, подготовка (тики), условие и действие. */
    public static final class Skill {
        final String name;
        final int cooldown;
        final int windup;
        final int anim;
        int left;
        Predicate<LivingEntity> when = t -> true;
        Consumer<LivingEntity> act = t -> {};
        Consumer<Integer> tick;
        boolean shout = true;
        int minPhase = 0;

        Skill(String name, int cooldown, int windup, int anim) {
            this.name = name;
            this.cooldown = cooldown;
            this.windup = windup;
            this.anim = anim;
            this.left = cooldown / 2;
        }

        public Skill when(Predicate<LivingEntity> p) { this.when = p; return this; }
        public Skill act(Consumer<LivingEntity> a) { this.act = a; return this; }
        /** Каждый тик подготовки (оставшиеся тики). */
        public Skill during(Consumer<Integer> t) { this.tick = t; return this; }
        public Skill silent() { this.shout = false; return this; }
        public Skill fromPhase(int p) { this.minPhase = p; return this; }
    }

    /**
     * @param anim номер позы для модели: 1 — замах/руки вверх, 2 — руки в стороны
     */
    protected Skill skill(String name, int cooldownTicks, int windupTicks, int anim) {
        Skill s = new Skill(name, cooldownTicks, windupTicks, anim);
        skills.add(s);
        return s;
    }

    public int castPose() {
        return entityData.get(CAST);
    }

    public boolean isCasting() {
        return casting != null;
    }

    /** Пороги здоровья для фаз (по убыванию), например {0.6f, 0.3f}. */
    protected float[] phaseThresholds() {
        return new float[]{0.5f};
    }

    /** Вызывается при переходе в новую фазу (1, 2...). */
    protected void onPhase(int phase) {}

    /** Сброс боя: все игроки ушли. */
    protected void onReset() {}

    /** Множитель, на который делится входящий урон (запас прочности сверх 1024 ед. здоровья). */
    protected float toughness() {
        return 1f;
    }

    // ================= основной цикл =================

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            castAnimO = castAnim;
            castAnim = Mth.approach(castAnim, castPose() > 0 ? 1f : 0f, 0.15f);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setName(getDisplayName());
        bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (!(level() instanceof ServerLevel sl)) return;

        // привязка к арене
        if (home != null && tickCount % 20 == 0 && distanceToSqr(Vec3.atBottomCenterOf(home)) > 28 * 28) {
            teleportTo(home.getX() + 0.5, home.getY() + 1, home.getZ() + 0.5);
            setTarget(null);
        }
        // все ушли — бой сбрасывается, босс восстанавливается
        if (tickCount % 20 == 0) {
            boolean anyone = !sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(40),
                    p -> !p.isSpectator() && !p.isCreative()).isEmpty();
            lonelyTicks = anyone ? 0 : lonelyTicks + 20;
            if (lonelyTicks >= 600 && (getHealth() < getMaxHealth() || phase > 0)) {
                setHealth(getMaxHealth());
                phase = 0;
                casting = null;
                entityData.set(CAST, 0);
                removeAllEffects();
                onReset();
            }
        }

        // фазы
        float[] th = phaseThresholds();
        while (phase < th.length && getHealth() < getMaxHealth() * th[phase]) {
            phase++;
            onPhase(phase);
        }

        // подготовка навыка
        if (casting != null) {
            castLeft--;
            getNavigation().stop();
            if (castTarget != null && castTarget.isAlive()) getLookControl().setLookAt(castTarget, 30, 30);
            if (casting.tick != null) casting.tick.accept(castLeft);
            if (castLeft <= 0) {
                Skill s = casting;
                casting = null;
                entityData.set(CAST, 0);
                if (castTarget != null && castTarget.isAlive()) s.act.accept(castTarget);
                globalCooldown = 30;
            }
            return;
        }
        for (Skill s : skills) if (s.left > 0) s.left--;
        if (globalCooldown > 0) {
            globalCooldown--;
            return;
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        List<Skill> ready = new ArrayList<>();
        for (Skill s : skills) if (s.left <= 0 && phase >= s.minPhase && s.when.test(target)) ready.add(s);
        if (ready.isEmpty()) return;
        Skill s = ready.get(random.nextInt(ready.size()));
        s.left = phase > 0 ? (int) (s.cooldown * 0.8f) : s.cooldown;
        castTarget = target;
        if (s.windup <= 0) {
            s.act.accept(target);
            globalCooldown = 20;
            return;
        }
        casting = s;
        castLeft = s.windup;
        entityData.set(CAST, Math.max(1, s.anim));
        if (s.shout) {
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(40))) {
                p.displayClientMessage(Component.empty().append(getName().copy().withStyle(ChatFormatting.GOLD))
                        .append(Component.literal(" готовит «" + s.name + "»!").withStyle(ChatFormatting.RED)), true);
            }
        }
    }

    // ================= помощники для навыков =================

    /** Живые цели рядом: игроки и всё, что не на стороне босса. */
    protected List<LivingEntity> enemiesAround(Vec3 center, double radius) {
        return level().getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(center, center).inflate(radius),
                e -> e != this && e.isAlive() && !isMinion(e) && !(e instanceof BossBase)
                        && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))
                        && e.position().distanceToSqr(center) <= radius * radius);
    }

    protected List<LivingEntity> enemiesAround(double radius) {
        return enemiesAround(position(), radius);
    }

    public boolean isMinion(Entity e) {
        return e.getPersistentData().hasUUID(MINION_TAG) && e.getPersistentData().getUUID(MINION_TAG).equals(getUUID());
    }

    /** Удар по области. */
    protected void aoe(Vec3 center, double radius, float damage, Consumer<LivingEntity> extra) {
        for (LivingEntity e : enemiesAround(center, radius)) {
            if (e.hurt(damageSources().mobAttack(this), damage) && extra != null) extra.accept(e);
        }
    }

    protected void knockFrom(LivingEntity e, Vec3 from, double strength, double up) {
        Vec3 d = e.position().subtract(from);
        d = new Vec3(d.x, 0, d.z);
        if (d.lengthSqr() < 1.0E-4) d = new Vec3(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5);
        d = d.normalize().scale(strength);
        e.push(d.x, up, d.z);
        e.hurtMarked = true;
    }

    /** Кольцо частиц на земле (предупреждение или эффект). */
    protected void ring(ParticleOptions p, Vec3 c, double radius, int count) {
        if (!(level() instanceof ServerLevel sl)) return;
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2 * i / count;
            sl.sendParticles(p, c.x + Math.cos(a) * radius, c.y + 0.15, c.z + Math.sin(a) * radius, 1, 0, 0.05, 0, 0);
        }
    }

    protected void burst(ParticleOptions p, Vec3 c, int count, double spread, double speed) {
        if (level() instanceof ServerLevel sl) sl.sendParticles(p, c.x, c.y, c.z, count, spread, spread * 0.5, spread, speed);
    }

    protected void sound(SoundEvent s, float volume, float pitch) {
        level().playSound(null, getX(), getY(), getZ(), s, SoundSource.HOSTILE, volume, pitch);
    }

    /** Реплика босса для всех рядом. */
    protected void shout(String text) {
        if (!(level() instanceof ServerLevel sl)) return;
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(48))) {
            p.sendSystemMessage(Component.literal("[").withStyle(ChatFormatting.DARK_GRAY)
                    .append(getName().copy().withStyle(ChatFormatting.GOLD))
                    .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(text).withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC)));
        }
    }

    /** Снаряд в цель. */
    protected BossProjectile shoot(BossProjectile.Kind kind, LivingEntity target, float damage, float speed, float spreadDeg) {
        BossProjectile p = new BossProjectile(level(), this, kind, damage);
        double sx = getX(), sy = getEyeY() - 0.3, sz = getZ();
        p.setPos(sx, sy, sz);
        double dx = target.getX() - sx, dy = target.getY(0.5) - sy, dz = target.getZ() - sz;
        double h = Math.sqrt(dx * dx + dz * dz);
        if (kind.gravity > 0) dy += h * kind.gravity * 2.2;
        p.shoot(dx, dy, dz, speed, spreadDeg);
        level().addFreshEntity(p);
        return p;
    }

    /** Призвать существ вокруг босса (не больше max живых рядом). */
    protected <M extends Mob> int summon(EntityType<M> type, int count, int max, int level, Consumer<M> setup) {
        if (!(this.level() instanceof ServerLevel sl)) return 0;
        long alive = sl.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(32), this::isMinion).size();
        int done = 0;
        for (int i = 0; i < count && alive + done < max; i++) {
            M m = type.create(sl);
            if (m == null) continue;
            Vec3 pos = null;
            for (int t = 0; t < 8 && pos == null; t++) {
                double a = random.nextDouble() * Math.PI * 2;
                double r = 2.5 + random.nextDouble() * 3;
                Vec3 c = new Vec3(getX() + Math.cos(a) * r, getY(), getZ() + Math.sin(a) * r);
                if (sl.noCollision(m, type.getAABB(c.x, c.y, c.z))) pos = c;
            }
            if (pos == null) pos = position();
            m.moveTo(pos.x, pos.y, pos.z, random.nextFloat() * 360, 0);
            m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            m.getPersistentData().putUUID(MINION_TAG, getUUID());
            MobLevels.apply(m, Math.max(1, level), MobLevels.Rank.NORMAL);
            if (setup != null) setup.accept(m);
            if (getTarget() != null) m.setTarget(getTarget());
            sl.addFreshEntity(m);
            done++;
        }
        return done;
    }

    /** Свободное место рядом с точкой (для телепорта). */
    protected boolean blinkNear(Vec3 c, double radius) {
        for (int i = 0; i < 16; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = radius * (0.4 + random.nextDouble() * 0.6);
            if (randomTeleport(c.x + Math.cos(a) * r, c.y + random.nextInt(3) - 1, c.z + Math.sin(a) * r, false)) return true;
        }
        return false;
    }

    // ================= арена и группа =================

    public void setHome(BlockPos pos) {
        this.home = pos;
        restrictTo(pos, 22);
    }

    public BlockPos getHome() {
        return home;
    }

    /** Чем больше игроков пришло, тем крепче босс (+60% здоровья за каждого сверх первого). */
    public void scaleForParty(int players) {
        var inst = getAttribute(Attributes.MAX_HEALTH);
        if (inst == null) return;
        inst.removeModifier(PARTY_HP_ID);
        if (players > 1) {
            inst.addPermanentModifier(new AttributeModifier(PARTY_HP_ID, "litrpg_party", 0.6 * (players - 1),
                    AttributeModifier.Operation.MULTIPLY_BASE));
        }
        setHealth(getMaxHealth());
    }

    /** Ключ сущности, например "ruin_golem". */
    public String bossId() {
        var key = ForgeRegistries.ENTITY_TYPES.getKey(getType());
        return key == null ? "" : key.getPath();
    }

    // ================= стандартное поведение =================

    @Override
    public Component getDisplayName() {
        return Component.literal("[Ур. " + getBossLevel() + "] ").withStyle(ChatFormatting.RED)
                .append(Component.empty().append(super.getDisplayName()).withStyle(ChatFormatting.GOLD));
    }

    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        super.actuallyHurt(source, amount / toughness());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() != null && isMinion(source.getEntity())) return false;
        if (source.getDirectEntity() instanceof BossProjectile bp && bp.getOwner() == this) return false;
        return super.hurt(source, amount);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return isMinion(other) || super.isAlliedTo(other);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    protected boolean canRide(Entity entity) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) tag.put("Home", NbtUtils.writeBlockPos(home));
        tag.putInt("Phase", phase);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Home")) setHome(NbtUtils.readBlockPos(tag.getCompound("Home")));
        phase = tag.getInt("Phase");
        bossEvent.setName(getDisplayName());
    }
}
