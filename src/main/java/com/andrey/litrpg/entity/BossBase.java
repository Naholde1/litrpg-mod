package com.andrey.litrpg.entity;

import com.andrey.litrpg.event.MobLevels;
import com.andrey.litrpg.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Основа боссов. Даёт полосу здоровья, фазы, навыки с подготовкой
 * (босс замирает, светится и выкрикивает название — у игрока есть время
 * отбежать), привязку к алтарю подземелья и сброс боя, если все ушли.
 * <p>
 * Бой «как в рейде»: урон навыков — в долях здоровья бойца ({@link BossDamage}), танк держит,
 * остальные — нет; опасные зоны на земле ({@link Telegraph}); щит, который надо сбить;
 * слабости к роду оружия; узлы силы на арене (блок {@code litrpg:relic}), без разрушения которых
 * хранитель неуязвим или воскресает; оглушение; неистовство после затяжного боя.
 */
public abstract class BossBase extends Monster implements LitBoss, Animated {
    /** Метка призванных боссом существ (UUID босса). */
    public static final String MINION_TAG = "litrpg_minion_of";
    private static final EntityDataAccessor<Integer> CAST = SynchedEntityData.defineId(BossBase.class, EntityDataSerializers.INT);
    private static final UUID PARTY_HP_ID = UUID.fromString("5b1e3c9a-1f2d-4e8a-9c7b-2a6d4f8e0b11");

    protected final ServerBossEvent bossEvent;
    protected final List<Skill> skills = new ArrayList<>();
    /** Место хранителя на арене (может отсутствовать — призыв командой). */
    protected BlockPos home;
    /** Границы арены этапа: за них хранитель не уходит, в них считаются бойцы. */
    protected com.andrey.litrpg.run.Layout.Box arenaBox;
    /** Доп. параметры арены из схемы (точки механик, залы второй фазы…). */
    protected com.google.gson.JsonObject arenaExtra = new com.google.gson.JsonObject();
    protected int phase = 0;
    private Skill casting;
    private int castLeft;
    private LivingEntity castTarget;
    private int lonelyTicks;
    private int globalCooldown = 40;
    /** Зоны, размеченные текущей подготовкой (пропадают при прерывании). */
    private final List<Telegraph> castZones = new ArrayList<>();
    private long interruptLock;
    /** Для плавной анимации подготовки на клиенте. */
    public float castAnim, castAnimO;

    protected final List<Telegraph> zones = new ArrayList<>();
    /** Щит: поглощает урон, пока не сбит. */
    protected float barrier;
    protected float barrierMax;
    /** Тики боя (для неистовства). */
    protected int fightTicks;
    protected boolean enraged;
    /** Оглушён (тики): не двигается, не колдует, получает +50% урона. */
    protected int stagger;
    /** Неуязвим (ушёл под землю, на потолок…) — текст подсказки; null — уязвим. */
    protected String immune;
    private Vec3 spawnPos;
    private boolean engaged;
    /** Узлы силы на арене: позиция → исходный блок (чтобы вернуть после сброса). */
    protected final Map<BlockPos, BlockState> relics = new HashMap<>();
    private boolean relicsScanned;
    private int relicsAlive = -1;
    private final Map<UUID, Long> hinted = new HashMap<>();

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
        Consumer<LivingEntity> begin;
        Consumer<Integer> tick;
        boolean shout = true;
        /** Можно ли сбить подготовку (оглушение, прерывание). Механики арены — нельзя. */
        boolean interruptible = true;
        int minPhase = 0;
        int maxPhase = 99;

        Skill(String name, int cooldown, int windup, int anim) {
            this.name = name;
            this.cooldown = cooldown;
            this.windup = windup;
            this.anim = anim;
            this.left = cooldown / 2;
        }

        public Skill when(Predicate<LivingEntity> p) { this.when = p; return this; }
        public Skill act(Consumer<LivingEntity> a) { this.act = a; return this; }
        /** В момент начала подготовки (обычно — разметить зоны с задержкой = подготовке). */
        public Skill begin(Consumer<LivingEntity> b) { this.begin = b; return this; }
        /** Каждый тик подготовки (оставшиеся тики). */
        public Skill during(Consumer<Integer> t) { this.tick = t; return this; }
        public Skill silent() { this.shout = false; return this; }
        /** Механику арены не прервать. */
        public Skill unstoppable() { this.interruptible = false; return this; }
        public Skill fromPhase(int p) { this.minPhase = p; return this; }
        public Skill untilPhase(int p) { this.maxPhase = p; return this; }
        /** Первое применение — через столько тиков после начала боя. */
        public Skill first(int ticks) { this.left = ticks; return this; }
    }

    /**
     * @param anim номер позы для модели: 1 — замах/руки вверх, 2 — руки в стороны
     */
    protected Skill skill(String name, int cooldownTicks, int windupTicks, int anim) {
        Skill s = new Skill(name, cooldownTicks, windupTicks, anim);
        skills.add(s);
        return s;
    }

    @Override
    public int castPose() {
        return entityData.get(CAST);
    }

    @Override
    public boolean isCasting() {
        return casting != null;
    }

    @Override
    public float castAnim(float partialTick) {
        return Mth.lerp(partialTick, castAnimO, castAnim);
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

    // ================= рейдовые механики: что переопределяют хранители =================

    /** Подсказки Системы в начале боя: в чём идея хранителя. */
    protected List<String> intro() {
        return List.of();
    }

    /** Множитель урона по хранителю от рода удара (слабости и стойкости). */
    protected float weaponMult(BossDamage.Weapon w, DamageSource src) {
        return 1f;
    }

    /** Свой множитель урона хранителя (ярость, накопленная сила…). */
    protected float powerMult() {
        return 1f;
    }

    /** Сила обычного удара (доля «единицы»). */
    protected float meleePower() {
        return 0.28f;
    }

    /** Через сколько тиков боя хранитель впадает в неистовство. */
    protected int enrageTicks() {
        return 20 * 60 * 5;
    }

    /** Смертельный удар: true — хранитель не погибает (воскрешение, вторая жизнь); здоровье выставить самому. */
    protected boolean onLethal(DamageSource src) {
        return false;
    }

    /** Разрушен узел силы на арене (осталось alive). */
    protected void onRelicBroken(int alive) {}

    /** Щит сбит. */
    protected void onBarrierBroken() {}

    /** Начало боя. */
    protected void onEngage() {}

    /** Может ли сейчас выбирать навыки (по умолчанию — пока уязвим). */
    protected boolean canCast() {
        return immune == null;
    }

    /** Свой ход хранителя каждый тик (вне оглушения), до выбора навыка. */
    protected void bossTick() {}

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
        Component bar = getDisplayName();
        if (barrier > 0) bar = bar.copy().append(Component.literal("  ⛨ " + (int) Math.ceil(barrier)).withStyle(ChatFormatting.AQUA));
        if (immune != null) bar = bar.copy().append(Component.literal("  ✖ неуязвим").withStyle(ChatFormatting.GRAY));
        if (enraged) bar = bar.copy().append(Component.literal("  ☠ неистовство ×" + enrageStacks()).withStyle(ChatFormatting.DARK_RED));
        bossEvent.setName(bar);
        bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (!(level() instanceof ServerLevel sl)) return;

        // привязка к арене
        if (tickCount % 20 == 0 && leashed()) {
            teleportTo(home.getX() + 0.5, home.getY() + 0.1, home.getZ() + 0.5);
            setTarget(null);
        }
        // все ушли — бой сбрасывается, босс восстанавливается
        if (tickCount % 20 == 0) {
            boolean anyone = !players().isEmpty();
            lonelyTicks = anyone ? 0 : lonelyTicks + 20;
            if (lonelyTicks >= 600 && (getHealth() < getMaxHealth() || phase > 0 || engaged)) resetFight();
        }

        if (!relicsScanned && tickCount > 5) scanRelics();
        else if (relicsScanned && relicsAlive < 0) relicsAlive = relicsAlive();
        if (relicsAlive >= 0 && tickCount % 10 == 0) {
            int alive = relicsAlive();
            if (alive < relicsAlive) {
                relicsAlive = alive;
                onRelicBroken(alive);
            }
        }
        Telegraph.tick(this, zones);
        tickTemp();

        // бой: начало и неистовство
        LivingEntity tgt = getTarget();
        if (tgt != null && tgt.isAlive()) {
            if (!engaged) {
                engaged = true;
                fightTicks = 0;
                List<String> lines = intro();
                for (ServerPlayer p : players()) {
                    p.sendSystemMessage(Component.literal("━━ Система: «" + getName().getString() + "» ━━").withStyle(ChatFormatting.GOLD));
                    for (String l : lines) p.sendSystemMessage(Component.literal(" • " + l).withStyle(ChatFormatting.GRAY));
                    p.sendSystemMessage(Component.literal(" • Неистовство через " + enrageTicks() / 1200 + " мин. боя.").withStyle(ChatFormatting.DARK_GRAY));
                }
                onEngage();
            }
            fightTicks++;
            if (fightTicks >= enrageTicks() && (fightTicks - enrageTicks()) % 200 == 0) {
                int st = enrageStacks();
                if (!enraged) {
                    enraged = true;
                    addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 3600, 1));
                    shoutAll("Бой затянулся — " + getName().getString() + " впадает в НЕИСТОВСТВО: урон растёт ×1,3 каждые 10 с!");
                } else {
                    shoutAll("Неистовство ×" + st + ": урон ×" + String.format("%.1f", enrageMult()));
                }
                sound(SoundEvents.WITHER_SPAWN, 1.5f, 0.6f + st * 0.05f);
            }
            if (enraged && tickCount % 10 == 0) burst(ParticleTypes.ANGRY_VILLAGER, position().add(0, getBbHeight() + 0.3, 0), 1, 0.5, 0);
        }
        if (barrier > 0 && tickCount % 6 == 0) {
            ring(ParticleTypes.ENCHANTED_HIT, position().add(0, getBbHeight() * 0.5, 0), getBbWidth() * 0.9 + 0.4, 10);
        }
        if (stagger > 0) {
            stagger--;
            getNavigation().stop();
            setDeltaMovement(0, getDeltaMovement().y, 0);
            if (stagger % 5 == 0) burst(ParticleTypes.CRIT, position().add(0, getBbHeight() + 0.3, 0), 6, 0.5, 0.1);
            return;
        }

        // фазы
        float[] th = phaseThresholds();
        while (phase < th.length && getHealth() < getMaxHealth() * th[phase]) {
            phase++;
            onPhase(phase);
        }
        bossTick();

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
        if (target == null || !target.isAlive() || !canCast()) return;
        List<Skill> ready = new ArrayList<>();
        for (Skill s : skills) if (s.left <= 0 && phase >= s.minPhase && phase <= s.maxPhase && s.when.test(target)) ready.add(s);
        if (ready.isEmpty()) return;
        Skill s = ready.get(random.nextInt(ready.size()));
        s.left = phase > 0 ? (int) (s.cooldown * 0.85f) : s.cooldown;
        castTarget = target;
        int zonesBefore = zones.size();
        if (s.begin != null) s.begin.accept(target);
        castZones.clear();
        if (zones.size() > zonesBefore) castZones.addAll(zones.subList(zonesBefore, zones.size()));
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

    /** Полный сброс боя: здоровье, фазы, щиты, узлы силы. */
    protected void resetFight() {
        setHealth(getMaxHealth());
        phase = 0;
        casting = null;
        entityData.set(CAST, 0);
        removeAllEffects();
        zones.clear();
        barrier = 0;
        stagger = 0;
        immune = null;
        enraged = false;
        engaged = false;
        fightTicks = 0;
        restoreRelics();
        clearTemp();
        for (Skill sk : skills) sk.left = sk.cooldown / 2;
        if (home != null) teleportTo(home.getX() + 0.5, home.getY() + 0.1, home.getZ() + 0.5);
        onReset();
    }

    // ================= урон «по-рейдовому» =================

    /** Неистовство «по-вовски»: каждые 10 с сверх таймера урон ×1,3 (геометрически) — без урона группы не победить. */
    public int enrageStacks() {
        return !enraged ? 0 : 1 + Math.max(0, fightTicks - enrageTicks()) / 200;
    }

    public float enrageMult() {
        return (float) Math.pow(1.3, enrageStacks());
    }

    /** «Единица» урона хранителя — примерное здоровье бойца его уровня. */
    public float unit() {
        return BossDamage.unit(getBossLevel());
    }

    /** Удар навыка: power — доля «единицы» (0.3 — ощутимо, 0.6 — тяжело, 1+ — смертельно для не-танка). */
    public boolean hit(LivingEntity e, float power) {
        if (e == null || !e.isAlive() || e == this || isMinion(e)) return false;
        float dmg = unit() * power * enrageMult() * powerMult();
        e.invulnerableTime = 0;
        boolean ok = e.hurt(BossDamage.source(this), dmg);
        e.invulnerableTime = Math.min(e.invulnerableTime, 4);
        return ok;
    }

    /** Удар навыка по области. */
    protected int hitAll(Vec3 center, double radius, float power, Consumer<LivingEntity> extra) {
        int n = 0;
        for (LivingEntity e : enemiesAround(center, radius)) {
            if (hit(e, power)) {
                n++;
                if (extra != null) extra.accept(e);
            }
        }
        return n;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!(target instanceof LivingEntity le)) return super.doHurtTarget(target);
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        boolean ok = hit(le, meleePower());
        if (ok) {
            double kb = getAttributeValue(Attributes.ATTACK_KNOCKBACK);
            if (kb > 0) knockFrom(le, position(), kb * 0.35, 0.1);
            setLastHurtMob(le);
        }
        return ok;
    }

    // ---------- опасные зоны ----------

    protected Telegraph zone(Telegraph.Shape shape, Vec3 c, double r1, double r2, int delay, float power) {
        Telegraph t = new Telegraph(shape, c, r1, r2, delay, power);
        zones.add(t);
        return t;
    }

    protected Telegraph circle(Vec3 c, double r, int delay, float power) {
        return zone(Telegraph.Shape.CIRCLE, c, r, 0, delay, power);
    }

    /** Кольцо: бьёт всех от inner до outer, в центре безопасно. */
    protected Telegraph donut(Vec3 c, double inner, double outer, int delay, float power) {
        return zone(Telegraph.Shape.RING, c, outer, inner, delay, power);
    }

    protected Telegraph cone(Vec3 c, Vec3 dir, double len, double halfAngle, int delay, float power) {
        return zone(Telegraph.Shape.CONE, c, len, halfAngle, delay, power).dir(dir);
    }

    protected Telegraph line(Vec3 c, Vec3 dir, double len, double halfWidth, int delay, float power) {
        return zone(Telegraph.Shape.LINE, c, len, halfWidth, delay, power).dir(dir);
    }

    void detonate(Telegraph t) {
        List<LivingEntity> in = new ArrayList<>();
        for (LivingEntity e : enemiesAround(t.center, Math.max(t.r1, 2) + 2)) {
            if (t.contains(e.position()) && (t.spare == null || !t.spare.test(e))) in.add(e);
        }
        if (t.soak > 0) {
            if (in.size() < t.soak) {
                for (LivingEntity e : fighters()) if (hit(e, t.power) && t.extra != null) t.extra.accept(e);
                shoutAll("Удар не разделили — нужно " + t.soak + " в круге! Досталось всем.");
            } else {
                float each = t.power * t.soak / in.size();
                for (LivingEntity e : in) if (hit(e, each) && t.extra != null) t.extra.accept(e);
            }
        } else if (t.power > 0 || t.extra != null) {
            for (LivingEntity e : in) {
                if (t.power > 0) {
                    if (hit(e, t.power) && t.extra != null) t.extra.accept(e);
                } else if (t.extra != null) t.extra.accept(e);
            }
        }
        if (t.fx != null) t.fx.accept(t.center);
    }

    // ---------- арена ----------

    /** Ушёл с арены (столкнули, увёл кайт) — вернуть. */
    protected boolean leashed() {
        if (home == null) return false;
        if (arenaBox != null) return !arenaBox.aabb().inflate(2).contains(position());
        return distanceToSqr(Vec3.atBottomCenterOf(home)) > 30 * 30;
    }

    /** Центр арены: место хранителя или место появления. */
    public Vec3 arena() {
        if (arenaBox != null) return new Vec3(arenaBox.center().x, home != null ? home.getY() : arenaBox.y1(), arenaBox.center().z);
        if (home != null) return Vec3.atBottomCenterOf(home);
        if (spawnPos == null) spawnPos = position();
        return spawnPos;
    }

    /** Игроки и их союзники на арене. */
    protected List<LivingEntity> fighters() {
        if (arenaBox != null) {
            return level().getEntitiesOfClass(LivingEntity.class, arenaBox.aabb().inflate(1, 2, 1),
                    e -> e != this && e.isAlive() && !isMinion(e) && !(e instanceof BossBase)
                            && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))
                            && !(e instanceof net.minecraft.world.entity.decoration.ArmorStand)
                            && !(e instanceof com.andrey.litrpg.run.TraderEntity));
        }
        return enemiesAround(arena(), 30);
    }

    protected List<ServerPlayer> players() {
        if (!(level() instanceof ServerLevel sl)) return List.of();
        AABB box = arenaBox != null ? arenaBox.aabb().inflate(1, 2, 1) : new AABB(arena(), arena()).inflate(34, 16, 34);
        return sl.getEntitiesOfClass(ServerPlayer.class, box, p -> p.isAlive() && !p.isCreative() && !p.isSpectator());
    }

    /** Случайный боец, кроме того, кто держит хранителя (обычно — не танк). */
    protected LivingEntity randomOther() {
        List<LivingEntity> all = new ArrayList<>(fighters());
        if (all.size() > 1) all.remove(getTarget());
        return all.isEmpty() ? getTarget() : all.get(random.nextInt(all.size()));
    }

    /** Ищет узлы силы (litrpg:relic) вокруг центра арены. */
    protected void scanRelics() {
        relicsScanned = true;
        if (relics.isEmpty()) {
            BlockPos c = BlockPos.containing(arena());
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-26, -6, -26), c.offset(26, 12, 26))) {
                BlockState st = level().getBlockState(p);
                if (st.is(ModBlocks.RELIC.get())) relics.put(p.immutable(), st);
            }
        }
        relicsAlive = relicsAlive();
    }

    public int relicsAlive() {
        int n = 0;
        for (BlockPos p : relics.keySet()) if (level().getBlockState(p).is(ModBlocks.RELIC.get())) n++;
        return n;
    }

    protected List<BlockPos> liveRelics() {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : relics.keySet()) if (level().getBlockState(p).is(ModBlocks.RELIC.get())) out.add(p);
        return out;
    }

    protected void restoreRelics() {
        relics.forEach((p, st) -> {
            if (!level().getBlockState(p).is(ModBlocks.RELIC.get())) level().setBlock(p, st, 3);
        });
        relicsAlive = relics.size();
    }

    // ---------- щит, оглушение, подсказки ----------

    protected void setBarrier(float amount) {
        barrierMax = amount;
        barrier = amount;
    }

    public boolean isCastingPublic() {
        return casting != null;
    }

    /**
     * Прерывание (как «пинок» в MMO): сбивает подготовку навыка, его зоны гаснут.
     * После этого 8 с хранителя не прервать. Механики арены ({@link Skill#unstoppable()}) не сбиваются.
     */
    public boolean interrupt(Entity by) {
        if (casting == null || casting.windup < 12 || !casting.interruptible || level().getGameTime() < interruptLock) return false;
        String name = casting.name;
        zones.removeAll(castZones);
        castZones.clear();
        casting = null;
        entityData.set(CAST, 0);
        globalCooldown = 40;
        interruptLock = level().getGameTime() + 160;
        burst(ParticleTypes.ENCHANTED_HIT, position().add(0, getBbHeight() * 0.8, 0), 30, 0.6, 0.2);
        sound(SoundEvents.SHIELD_BLOCK, 2f, 0.6f);
        shoutAll((by == null ? "Кто-то" : by.getName().getString()) + " прерывает «" + name + "»!");
        return true;
    }

    /** Оглушить хранителя: стоит, не колдует, получает +50% урона. */
    public void stagger(int ticks, String why) {
        stagger = Math.max(stagger, ticks);
        casting = null;
        entityData.set(CAST, 0);
        getNavigation().stop();
        if (why != null) shoutAll(why);
    }

    public boolean isStaggered() {
        return stagger > 0;
    }

    /** Подсказка Системы всем на арене. */
    protected void shoutAll(String text) {
        for (ServerPlayer p : players()) {
            p.sendSystemMessage(Component.literal("◆ ").withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(text).withStyle(ChatFormatting.WHITE)));
        }
    }

    /** Подсказка над хотбаром одному игроку (не чаще раза в 4 с). */
    protected void tip(Entity e, String text, ChatFormatting color) {
        if (!(e instanceof ServerPlayer p)) return;
        long now = level().getGameTime();
        Long last = hinted.get(p.getUUID());
        if (last != null && now - last < 80) return;
        hinted.put(p.getUUID(), now);
        p.displayClientMessage(Component.literal(text).withStyle(color), true);
    }

    // ================= помощники для навыков =================

    /** Живые цели рядом: игроки и всё, что не на стороне босса. */
    protected List<LivingEntity> enemiesAround(Vec3 center, double radius) {
        return level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius, Math.max(6, radius / 2), radius),
                e -> e != this && e.isAlive() && !isMinion(e) && !(e instanceof BossBase)
                        && !(e instanceof Player p && (p.isCreative() || p.isSpectator()))
                        && !(e instanceof net.minecraft.world.entity.decoration.ArmorStand)
                        && e.position().distanceToSqr(center) <= radius * radius + 36);
    }

    protected List<LivingEntity> enemiesAround(double radius) {
        return enemiesAround(position(), radius);
    }

    public boolean isMinion(Entity e) {
        return e.getPersistentData().hasUUID(MINION_TAG) && e.getPersistentData().getUUID(MINION_TAG).equals(getUUID());
    }

    protected void knockFrom(LivingEntity e, Vec3 from, double strength, double up) {
        Vec3 d = e.position().subtract(from);
        d = new Vec3(d.x, 0, d.z);
        if (d.lengthSqr() < 1.0E-4) d = new Vec3(random.nextDouble() - 0.5, 0, random.nextDouble() - 0.5);
        d = d.normalize().scale(strength);
        e.push(d.x, up, d.z);
        e.hurtMarked = true;
    }

    /** Притянуть к точке. */
    protected void pullTo(LivingEntity e, Vec3 to, double strength) {
        Vec3 d = to.subtract(e.position());
        d = new Vec3(d.x, 0, d.z);
        if (d.lengthSqr() < 1.0E-4) return;
        d = d.normalize().scale(strength);
        e.push(d.x, 0.15, d.z);
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

    protected void soundAt(Vec3 at, SoundEvent s, float volume, float pitch) {
        level().playSound(null, at.x, at.y, at.z, s, SoundSource.HOSTILE, volume, pitch);
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

    /** Снаряд в цель; power — доля «единицы» урона. */
    protected BossProjectile shoot(BossProjectile.Kind kind, LivingEntity target, float power, float speed, float spreadDeg) {
        return shootAt(kind, target.position().add(0, target.getBbHeight() * 0.5, 0), power, speed, spreadDeg);
    }

    protected BossProjectile shootAt(BossProjectile.Kind kind, Vec3 at, float power, float speed, float spreadDeg) {
        BossProjectile p = new BossProjectile(level(), this, kind, 1f).power(power);
        double sx = getX(), sy = getEyeY() - 0.3, sz = getZ();
        p.setPos(sx, sy, sz);
        double dx = at.x - sx, dy = at.y - sy, dz = at.z - sz;
        double h = Math.sqrt(dx * dx + dz * dz);
        if (kind.gravity > 0) dy += h * kind.gravity * 2.2;
        p.shoot(dx, dy, dz, speed, spreadDeg);
        level().addFreshEntity(p);
        return p;
    }

    /** Призвать существ вокруг босса (не больше max живых рядом). */
    protected <M extends Mob> int summon(EntityType<M> type, int count, int max, int level, Consumer<M> setup) {
        return summonAt(type, position(), count, max, level, setup);
    }

    protected <M extends Mob> int summonAt(EntityType<M> type, Vec3 at, int count, int max, int level, Consumer<M> setup) {
        if (!(this.level() instanceof ServerLevel sl)) return 0;
        long alive = sl.getEntitiesOfClass(Mob.class, getBoundingBox().inflate(40), this::isMinion).size();
        int done = 0;
        for (int i = 0; i < count && alive + done < max; i++) {
            M m = type.create(sl);
            if (m == null) continue;
            Vec3 pos = null;
            for (int t = 0; t < 10 && pos == null; t++) {
                double a = random.nextDouble() * Math.PI * 2;
                double r = 1.5 + random.nextDouble() * 3;
                Vec3 c = new Vec3(at.x + Math.cos(a) * r, at.y, at.z + Math.sin(a) * r);
                if (sl.noCollision(m, type.getAABB(c.x, c.y, c.z))) pos = c;
            }
            if (pos == null) pos = at;
            m.moveTo(pos.x, pos.y, pos.z, random.nextFloat() * 360, 0);
            m.finalizeSpawn(sl, sl.getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
            m.getPersistentData().putUUID(MINION_TAG, getUUID());
            m.setPersistenceRequired();
            MobLevels.apply(m, Math.max(1, level), MobLevels.Rank.NORMAL);
            if (setup != null) setup.accept(m);
            LivingEntity tg = getTarget() != null ? randomOther() : null;
            if (tg != null) m.setTarget(tg);
            sl.addFreshEntity(m);
            sl.sendParticles(ParticleTypes.POOF, pos.x, pos.y + 0.5, pos.z, 8, 0.3, 0.3, 0.3, 0.02);
            done++;
        }
        return done;
    }

    protected List<Mob> minions() {
        return level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(48), this::isMinion);
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

    // ---------- временные блоки, темницы ----------

    private record TempBlock(BlockPos pos, BlockState was, long until) {}
    private record Tomb(UUID who, List<BlockPos> blocks, long until, float dot) {}
    private final List<TempBlock> temp = new ArrayList<>();
    private final List<Tomb> tombs = new ArrayList<>();

    /** Поставить блок на время (только на месте воздуха/травы); вернётся сам. */
    protected boolean tempBlock(BlockPos p, BlockState st, int ticks) {
        BlockState was = level().getBlockState(p);
        if (!was.canBeReplaced() || was.is(st.getBlock())) return false;
        level().setBlock(p, st, 3);
        temp.add(new TempBlock(p.immutable(), was, level().getGameTime() + ticks));
        return true;
    }

    /**
     * Темница: кокон из паутины, ледяная гробница… Цель заперта в клетке из блоков и получает урон,
     * пока союзники не разобьют клетку (или пока не истечёт время).
     */
    protected void entomb(LivingEntity e, BlockState st, int ticks, float dotPerSecond, boolean cage) {
        BlockPos at = e.blockPosition();
        e.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        e.setDeltaMovement(Vec3.ZERO);
        List<BlockPos> placed = new ArrayList<>();
        List<BlockPos> spots = new ArrayList<>();
        if (cage) {
            for (int y = 0; y < 2; y++) {
                spots.add(at.offset(1, y, 0));
                spots.add(at.offset(-1, y, 0));
                spots.add(at.offset(0, y, 1));
                spots.add(at.offset(0, y, -1));
            }
            spots.add(at.above(2));
        } else {
            spots.add(at);
            spots.add(at.above());
        }
        for (BlockPos p : spots) if (tempBlock(p, st, ticks)) placed.add(p.immutable());
        if (!placed.isEmpty()) tombs.add(new Tomb(e.getUUID(), placed, level().getGameTime() + ticks, dotPerSecond));
        if (e instanceof ServerPlayer p) {
            p.displayClientMessage(Component.literal("Вы в ловушке! Союзники — разбейте клетку!").withStyle(ChatFormatting.RED), true);
        }
    }

    private void tickTemp() {
        long now = level().getGameTime();
        temp.removeIf(t -> {
            if (now < t.until) return false;
            if (level().getBlockState(t.pos) != t.was) level().setBlock(t.pos, t.was, 3);
            return true;
        });
        if (now % 20 != 0 || !(level() instanceof ServerLevel sl)) return;
        tombs.removeIf(t -> {
            Entity e = sl.getEntity(t.who);
            if (!(e instanceof LivingEntity le) || !le.isAlive() || now >= t.until) return true;
            boolean held = false;
            for (BlockPos p : t.blocks) if (!level().getBlockState(p).isAir()) held = true;
            if (!held) {
                tip(le, "Свобода!", ChatFormatting.GREEN);
                return true;
            }
            if (t.dot > 0) hit(le, t.dot);
            return false;
        });
    }

    protected void clearTemp() {
        for (TempBlock t : temp) level().setBlock(t.pos, t.was, 3);
        temp.clear();
        tombs.clear();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && reason.shouldDestroy()) clearTemp();
        super.remove(reason);
    }

    /** Блок под ногами (для механик «встаньте на…»). */
    protected BlockState under(LivingEntity e) {
        return level().getBlockState(e.blockPosition().below());
    }

    /** Обычный набор целей ближнего боя. */
    protected void meleeGoals(double speed) {
        goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, speed, true));
        goalSelector.addGoal(6, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(8, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));
        targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    /** Направление от хранителя к цели (по земле). */
    protected Vec3 dirTo(Entity e) {
        Vec3 d = e.position().subtract(position());
        d = new Vec3(d.x, 0, d.z);
        return d.lengthSqr() < 1e-4 ? getLookAngle() : d.normalize();
    }

    /** Случайная точка на арене (в радиусе r от центра). */
    protected Vec3 randomSpot(double r) {
        Vec3 c = arena();
        double a = random.nextDouble() * Math.PI * 2, d = Math.sqrt(random.nextDouble()) * r;
        Vec3 p = new Vec3(c.x + Math.cos(a) * d, c.y, c.z + Math.sin(a) * d);
        // опустить на пол
        BlockPos b = BlockPos.containing(p);
        for (int i = 0; i < 6 && level().getBlockState(b.below()).isAir(); i++) b = b.below();
        return new Vec3(p.x, b.getY(), p.z);
    }

    // ================= арена и группа =================

    public void setHome(BlockPos pos) {
        this.home = pos;
        restrictTo(pos, 24);
    }

    /** Арена этапа (из схемы подземелья). */
    public void setArena(com.andrey.litrpg.run.Layout.Box box, BlockPos at, com.google.gson.JsonObject extra) {
        this.arenaBox = box;
        this.arenaExtra = extra == null ? new com.google.gson.JsonObject() : extra;
        this.home = at;
        int r = Math.max(box.x2() - box.x1(), box.z2() - box.z1()) / 2 + 2;
        restrictTo(at, r);
        onArena();
    }

    /** Арена задана (прочитать точки механик из arenaExtra). */
    protected void onArena() {}

    /** Точка из доп. параметров арены: "name": [x, y, z]. */
    protected Vec3 point(String name) {
        if (!arenaExtra.has(name)) return arena();
        var a = arenaExtra.getAsJsonArray(name);
        return new Vec3(a.get(0).getAsDouble() + 0.5, a.get(1).getAsDouble(), a.get(2).getAsDouble() + 0.5);
    }

    /** Список точек: "name": [[x,y,z], ...]. */
    protected List<Vec3> points(String name) {
        List<Vec3> out = new ArrayList<>();
        if (!arenaExtra.has(name)) return out;
        for (var e : arenaExtra.getAsJsonArray(name)) {
            var a = e.getAsJsonArray();
            out.add(new Vec3(a.get(0).getAsDouble() + 0.5, a.get(1).getAsDouble(), a.get(2).getAsDouble() + 0.5));
        }
        return out;
    }

    public BlockPos getHome() {
        return home;
    }

    /**
     * Размер группы: здоровье не может быть выше 1024, поэтому группа даёт «запас прочности» —
     * входящий урон делится (выверено под троих; четвёртый и пятый — +30% каждый).
     */
    protected float partyScale = 1f;
    protected int partySize = 3;

    public void scaleForParty(int players) {
        var inst = getAttribute(Attributes.MAX_HEALTH);
        if (inst != null) inst.removeModifier(PARTY_HP_ID);
        partySize = players;
        partyScale = (float) com.andrey.litrpg.run.Run.hpScale(players);
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
        amount /= toughness() * partyScale;
        if (stagger > 0) amount *= 1.5f;
        BossDamage.Weapon w = BossDamage.weapon(source);
        float mult = weaponMult(w, source);
        if (mult != 1f) {
            amount *= mult;
            Entity who = source.getEntity();
            if (mult >= 1.4f) {
                tip(who, "Уязвимость: " + w.title + " ×" + String.format("%.1f", mult), ChatFormatting.GREEN);
                burst(ParticleTypes.CRIT, position().add(0, getBbHeight() * 0.6, 0), 10, 0.5, 0.2);
            } else if (mult <= 0.6f) {
                tip(who, "Стойкость: " + w.title + " почти не ранят (×" + String.format("%.1f", mult) + ")", ChatFormatting.GRAY);
            }
        }
        if (barrier > 0 && !source.is(DamageTypes.GENERIC_KILL)) {
            float take = Math.min(barrier, amount);
            barrier -= take;
            amount -= take;
            if (barrier <= 0) {
                barrier = 0;
                burst(ParticleTypes.FLASH, position().add(0, getBbHeight() * 0.5, 0), 1, 0, 0);
                sound(SoundEvents.SHIELD_BREAK, 2f, 0.6f);
                onBarrierBroken();
            }
            if (amount <= 0) return;
        }
        super.actuallyHurt(source, amount);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() != null && isMinion(source.getEntity())) return false;
        if (source.getDirectEntity() instanceof BossProjectile bp && bp.getOwner() == this) return false;
        if (source.getEntity() instanceof BossBase) return false;
        if (immune != null && !source.is(DamageTypes.GENERIC_KILL) && !source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            tip(source.getEntity(), immune, ChatFormatting.GRAY);
            return false;
        }
        return super.hurt(source, amount);
    }

    /** Смертельный удар (зовёт BossFight из события смерти): true — хранитель выжил. */
    public boolean tryLethal(DamageSource source) {
        if (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) return false;
        if (!onLethal(source)) return false;
        if (getHealth() <= 0) setHealth(1f);
        return true;
    }

    /** Для dev-команды devboss. */
    public String debugState() {
        return String.format("hp=%.0f/%.0f party=%.1f barrier=%.0f phase=%d relics=%d/%d zones=%d immune=%s stagger=%d fight=%ds enrage=%d target=%s home=%s",
                getHealth(), getMaxHealth(), partyScale, barrier, phase, relicsAlive(), relics.size(), zones.size(), immune, stagger,
                fightTicks / 20, enrageStacks(), getTarget() == null ? "-" : getTarget().getName().getString(), home);
    }

    /** Идёт ли бой (хранитель кого-то атакует). */
    public boolean inCombat() {
        return engaged;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return isMinion(other) || other instanceof BossBase || super.isAlliedTo(other);
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
        tag.putFloat("Party", partyScale);
        tag.putInt("PartyN", partySize);
        if (arenaBox != null) tag.putIntArray("Arena", new int[]{arenaBox.x1(), arenaBox.y1(), arenaBox.z1(), arenaBox.x2(), arenaBox.y2(), arenaBox.z2()});
        tag.putString("ArenaExtra", arenaExtra.toString());
        tag.putFloat("Barrier", barrier);
        tag.putFloat("BarrierMax", barrierMax);
        tag.putInt("Fight", fightTicks);
        tag.putBoolean("Enraged", enraged);
        ListTag rl = new ListTag();
        relics.forEach((p, st) -> {
            CompoundTag t = NbtUtils.writeBlockPos(p);
            t.put("S", NbtUtils.writeBlockState(st));
            rl.add(t);
        });
        tag.put("Relics", rl);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Home")) setHome(NbtUtils.readBlockPos(tag.getCompound("Home")));
        phase = tag.getInt("Phase");
        partyScale = tag.contains("Party") ? Math.max(0.3f, tag.getFloat("Party")) : 1f;
        partySize = tag.contains("PartyN") ? tag.getInt("PartyN") : 3;
        if (tag.contains("Arena")) {
            int[] a = tag.getIntArray("Arena");
            arenaBox = new com.andrey.litrpg.run.Layout.Box(a[0], a[1], a[2], a[3], a[4], a[5]);
        }
        if (tag.contains("ArenaExtra")) {
            try {
                arenaExtra = com.google.gson.JsonParser.parseString(tag.getString("ArenaExtra")).getAsJsonObject();
            } catch (Exception ignored) {
                arenaExtra = new com.google.gson.JsonObject();
            }
        }
        if (arenaBox != null) onArena();
        barrier = tag.getFloat("Barrier");
        barrierMax = tag.getFloat("BarrierMax");
        fightTicks = tag.getInt("Fight");
        enraged = tag.getBoolean("Enraged");
        relics.clear();
        ListTag rl = tag.getList("Relics", Tag.TAG_COMPOUND);
        for (int i = 0; i < rl.size(); i++) {
            CompoundTag t = rl.getCompound(i);
            relics.put(NbtUtils.readBlockPos(t), NbtUtils.readBlockState(level().holderLookup(Registries.BLOCK), t.getCompound("S")));
        }
        if (!relics.isEmpty()) relicsScanned = true;
        bossEvent.setName(getDisplayName());
    }
}
