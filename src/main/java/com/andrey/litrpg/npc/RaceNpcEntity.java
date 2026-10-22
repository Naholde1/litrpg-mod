package com.andrey.litrpg.npc;

import com.andrey.litrpg.LitRPG;

import com.andrey.litrpg.entity.ManaBoltEntity;
import com.andrey.litrpg.gear.GearData;
import com.andrey.litrpg.gear.GearBonus;
import com.andrey.litrpg.gear.LootRolls;
import com.andrey.litrpg.registry.ModItems;
import com.andrey.litrpg.rep.Faction;
import com.andrey.litrpg.rep.RepEvents;
import com.andrey.litrpg.stats.StatsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Житель одной из рас. Нейтрален к игрокам с нормальной репутацией,
 * нападает на тех, кого его народ ненавидит, защищает поселение от монстров.
 * Торговцы продают снаряжение народа и скупают предметы Системы (Shift+ПКМ).
 */
public class RaceNpcEntity extends PathfinderMob implements RangedAttackMob, Merchant {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> CRAFT =
            net.minecraft.network.syncher.SynchedEntityData.defineId(RaceNpcEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    private final NpcType npc;
    private boolean female;
    /** Личное имя («Гаррет Ашфорд») и занятие горожанина; полное имя собирается в refreshName(). */
    private String pname = "";
    /** Имя героя сюжета («Капитан стражи Освальд») — показывается как есть. */
    private String storyName = "";
    @Nullable private Job job;
    private int chatterCooldown = 2 + (int) (Math.random() * 12);
    @Nullable private Player tradingPlayer;
    @Nullable private MerchantOffers offers;
    /** Ключи штучных товаров текущего списка сделок (null — обычный товар). */
    private final java.util.List<String> offerKeys = new java.util.ArrayList<>();
    /** Когда продана штучная вещь сокровищницы: ключ → игровое время. */
    private CompoundTag sold = new CompoundTag();
    @Nullable private final ServerBossEvent bossEvent;
    private int spellCooldown = 60;

    public RaceNpcEntity(EntityType<? extends RaceNpcEntity> type, Level level, NpcType npc) {
        super(type, level);
        this.npc = npc;
        this.bossEvent = npc.role == NpcType.Role.CHIEF
                ? new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10) : null;
        setupGoals();
        this.xpReward = npc.role == NpcType.Role.CHIEF ? 60 : 8;
    }

    public NpcType npcType() { return npc; }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(CRAFT, -1);
    }

    /** Ремесло мастера (null у остальных жителей). */
    @Nullable
    public Craft craft() {
        int i = entityData.get(CRAFT);
        return i < 0 || i >= Craft.values().length ? null : Craft.values()[i];
    }

    public void setCraft(@Nullable Craft c) {
        entityData.set(CRAFT, c == null ? -1 : c.ordinal());
    }

    @Nullable
    public Job job() { return job; }

    public String personalName() { return pname; }

    public void setStoryName(String name) {
        storyName = name;
        pname = name;
        job = null;
        refreshName();
    }

    public boolean isFemale() { return female; }

    /** Выдать имя (и занятие горожанину), если их ещё нет. */
    public void ensureName() {
        if (!pname.isEmpty()) return;
        if (npc.role == NpcType.Role.CHIEF || npc.isGuard()) female = false;
        else female = random.nextInt(npc.role == NpcType.Role.CITIZEN || npc.role == NpcType.Role.TRADER ? 2 : npc.outlaw() ? 6 : 3) == 0;
        if (npc.key.startsWith("pirate_")) {
            pname = Names.pirate(random);
            female = false;
        } else {
            pname = Names.full(npc.race, female, random);
        }
        if (npc.role == NpcType.Role.CITIZEN && job == null) job = Job.random(npc.race, random);
        refreshName();
    }

    /** Имя без фамилии и рода: «Серый Клык», «Торин», «Аэлин». */
    private String shortName() {
        String n = pname.split(" из ")[0].split(",")[0];
        return npc.race == com.andrey.litrpg.stats.Race.BEASTKIN ? n : n.split(" ")[0];
    }

    /** Полное имя над головой: «Гаррет Ашфорд, гончар», «[Ур. 12] Гром Костолом, орк-налётчик», «Лорд-регент Аркадии Годрик». */
    public void refreshName() {
        if (pname.isEmpty()) return;
        String type = npc.title();
        String low = type.isEmpty() ? "" : Character.toLowerCase(type.charAt(0)) + type.substring(1);
        String text = !storyName.isEmpty() ? storyName : switch (npc.role) {
            case ARTISAN -> {
                Craft c = craft();
                yield pname + (c == null ? "" : ", " + (female ? c.titleF : c.title));
            }
            case CITIZEN -> pname + (job == null ? "" : ", " + job.title(female));
            case TRADER -> pname + ", " + (female ? "торговка" : "торговец");
            case CHIEF -> npc.key.startsWith("pirate_") ? "Капитан " + pname : type + " " + shortName();
            default -> pname + ", " + low;
        };
        int lvl = getPersistentData().getInt(com.andrey.litrpg.event.MobLevels.TAG);
        boolean showLevel = npc.fights() && lvl > 0;
        net.minecraft.network.chat.MutableComponent name = Component.empty();
        if (showLevel) {
            ChatFormatting color = npc.isGuard() ? ChatFormatting.DARK_RED : lvl < 10 ? ChatFormatting.GRAY : lvl < 20 ? ChatFormatting.YELLOW
                    : lvl < 35 ? ChatFormatting.GOLD : ChatFormatting.RED;
            name.append(Component.literal("[Ур. " + lvl + "] ").withStyle(color));
        }
        name.append(Component.literal(text).withStyle(npc.outlaw() ? ChatFormatting.RED : npc.isGuard() ? ChatFormatting.GOLD : ChatFormatting.WHITE));
        setCustomName(name);
        setCustomNameVisible(false);
    }

    public static AttributeSupplier.Builder createAttributes(NpcType t) {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, t.health)
                .add(Attributes.ATTACK_DAMAGE, t.damage)
                .add(Attributes.MOVEMENT_SPEED, t.speed)
                .add(Attributes.FOLLOW_RANGE, 28.0)
                .add(Attributes.ARMOR, t.isGuard() ? 30 : t.role == NpcType.Role.CHIEF ? 10 : t.race == com.andrey.litrpg.stats.Race.DWARF ? 6 : 3)
                .add(Attributes.ARMOR_TOUGHNESS, t.isGuard() ? 12 : 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, t.isGuard() ? 1.0 : t.role == NpcType.Role.CHIEF ? 0.7 : 0.1);
    }

    @Override
    protected void registerGoals() {
        // цели ставятся в setupGoals(): тип жителя ещё неизвестен в момент вызова этого метода
    }

    private void setupGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        switch (npc.role) {
            case WARRIOR, CHIEF -> goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
            case GUARD -> goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35, true));
            case CITIZEN -> {
                goalSelector.addGoal(1, new PanicGoal(this, 1.4));
                goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Monster.class, 12f, 1.0, 1.4));
            }
            case ARCHER -> goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 30, 18f));
            case MAGE -> goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 40, 16f));
            case TRADER, ARTISAN -> {
                goalSelector.addGoal(1, new PanicGoal(this, 1.3));
                goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Monster.class, 10f, 1.0, 1.3));
            }
        }
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        if (npc.isGuard()) {
            // стража: преступники, враги народа и любые монстры в городе
            targetSelector.addGoal(1, new HurtByTargetGoal(this, RaceNpcEntity.class).setAlertOthers());
            targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 5, false, false,
                    e -> e instanceof Player p && (com.andrey.litrpg.city.CityLaw.isWanted(p) || hostileTo(p))));
            targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.Mob.class, 10, false, false,
                    e -> (e instanceof Enemy && com.andrey.litrpg.ability.Minions.ownerOf(e) == null)
                            || e instanceof RaceNpcEntity o && o.npcType().outlaw()));
        } else if (npc.outlaw()) {
            targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
            targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                    e -> e instanceof Player p && hostileTo(p)));
        } else if (npc.fights()) {
            targetSelector.addGoal(1, new HurtByTargetGoal(this, RaceNpcEntity.class).setAlertOthers());
            targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                    e -> e instanceof Player p && hostileTo(p)));
            targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Monster.class, 10, true, false,
                    e -> e instanceof Enemy && com.andrey.litrpg.ability.Minions.ownerOf(e) == null));
        }
    }

    /** Народ ненавидит игрока — нападать без предупреждения. */
    public boolean hostileTo(Player p) {
        if (p.isCreative() || p.isSpectator()) return false;
        if (npc.outlaw()) return true;
        int rep = StatsManager.get(p).getRep(npc.faction);
        return rep < Faction.Tier.UNFRIENDLY.from;
    }

    // ---------- появление ----------

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        if (npc.isArtisan() && craft() == null) setCraft(Craft.values()[random.nextInt(Craft.values().length)]);
        ensureName();
        equip();
        setPersistenceRequired();
        restrictTo(blockPosition(), homeRadius());
        return result;
    }

    private int homeRadius() {
        if (npc.isArtisan()) return craft() == Craft.BUILDER ? 40 : 6;
        return npc.role == NpcType.Role.TRADER ? 10 : 22;
    }

    private void equip() {
        if (npc.isArtisan()) {
            Craft c = craft();
            ItemStack tool = c == null ? ItemStack.EMPTY : new ItemStack(switch (c) {
                case BLACKSMITH, ARMORER -> Items.IRON_PICKAXE;
                case WEAPONSMITH -> Items.IRON_SWORD;
                case JEWELER -> Items.AMETHYST_SHARD;
                case TAILOR -> Items.SHEARS;
                case HERBALIST -> Items.POPPY;
                case ALCHEMIST -> Items.POTION;
                case BAKER -> Items.BREAD;
                case BUTCHER -> Items.IRON_AXE;
                case FISHMONGER -> Items.FISHING_ROD;
                case INNKEEPER -> Items.HONEY_BOTTLE;
                case BUILDER -> Items.STONE_BRICKS;
                case LEATHERWORKER -> Items.LEATHER;
                case FLETCHER -> Items.BOW;
                case SCRIBE -> Items.WRITABLE_BOOK;
                case FARMER -> Items.IRON_HOE;
                case HEALER -> Items.GLISTERING_MELON_SLICE;
                case BANKER -> Items.GOLD_INGOT;
                case STABLEMASTER -> Items.LEAD;
                case MINER -> Items.IRON_PICKAXE;
            });
            setItemSlot(EquipmentSlot.MAINHAND, tool);
            setDropChance(EquipmentSlot.MAINHAND, 0f);
            return;
        }
        if (npc.role == NpcType.Role.CITIZEN && job != null) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(job.tool));
            setDropChance(EquipmentSlot.MAINHAND, 0f);
            return;
        }
        if (npc.outlaw()) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(switch (npc) {
                case OUTLAW_THUG -> Items.IRON_AXE;
                case OUTLAW_CULTIST -> ModItems.APPRENTICE_STAFF.get();
                case PIRATE_GUNNER -> Items.CROSSBOW;
                case PIRATE_CAPTAIN -> Items.GOLDEN_SWORD;
                default -> Items.IRON_SWORD;
            }));
            setDropChance(EquipmentSlot.MAINHAND, 0.05f);
            if (npc.key.startsWith("pirate_")) {
                ItemStack hat = new ItemStack(Items.LEATHER_HELMET);
                ((net.minecraft.world.item.DyeableLeatherItem) Items.LEATHER_HELMET).setColor(hat,
                        npc == NpcType.PIRATE_CAPTAIN ? 0x1A1A1A : random.nextBoolean() ? 0xA02020 : 0x303A60);
                setItemSlot(EquipmentSlot.HEAD, hat);
                setDropChance(EquipmentSlot.HEAD, 0f);
            }
            return;
        }
        ItemStack main = switch (npc.role) {
            case ARCHER -> new ItemStack(npc.race == com.andrey.litrpg.stats.Race.DWARF ? Items.CROSSBOW : Items.BOW);
            case MAGE -> new ItemStack(switch (npc.race) {
                case ELF -> ModItems.TEMPLATE_ITEMS.get("elven_staff").get();
                case DEMONKIN -> ModItems.TEMPLATE_ITEMS.get("demon_staff").get();
                default -> ModItems.APPRENTICE_STAFF.get();
            });
            case WARRIOR, CHIEF -> new ItemStack(switch (npc.race) {
                case ELF -> ModItems.TEMPLATE_ITEMS.get("elven_blade").get();
                case DWARF -> ModItems.TEMPLATE_ITEMS.get(npc.role == NpcType.Role.CHIEF ? "dwarven_hammer" : "dwarven_axe").get();
                case ORC -> ModItems.TEMPLATE_ITEMS.get(npc.role == NpcType.Role.CHIEF ? "orc_cleaver" : "orc_club").get();
                case BEASTKIN -> ModItems.TEMPLATE_ITEMS.get(npc.role == NpcType.Role.CHIEF ? "beast_spear" : "beast_claws").get();
                default -> ModItems.TEMPLATE_ITEMS.get("demon_blade").get();
            });
            case TRADER, CITIZEN, ARTISAN -> ItemStack.EMPTY;
            case GUARD -> new ItemStack(switch (npc.race) {
                case ELF -> ModItems.TEMPLATE_ITEMS.get("elven_blade").get();
                case DWARF -> ModItems.TEMPLATE_ITEMS.get("dwarven_hammer").get();
                case ORC -> ModItems.TEMPLATE_ITEMS.get("orc_cleaver").get();
                case BEASTKIN -> ModItems.TEMPLATE_ITEMS.get("beast_spear").get();
                case DEMONKIN -> ModItems.TEMPLATE_ITEMS.get("demon_blade").get();
                default -> Items.NETHERITE_SWORD;
            });
        };
        if (npc.race == com.andrey.litrpg.stats.Race.HUMAN && npc.role == NpcType.Role.WARRIOR) main = new ItemStack(Items.IRON_SWORD);
        if (npc.race == com.andrey.litrpg.stats.Race.HUMAN && npc.role == NpcType.Role.CHIEF) main = new ItemStack(Items.DIAMOND_SWORD);
        if (npc.isGuard()) {
            setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            setDropChance(EquipmentSlot.OFFHAND, 0f);
        }
        setItemSlot(EquipmentSlot.MAINHAND, main);
        setDropChance(EquipmentSlot.MAINHAND, npc.isGuard() ? 0f : npc.role == NpcType.Role.CHIEF ? 0.25f : 0.02f);
    }

    // ---------- бой ----------

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        if (npc.role == NpcType.Role.ARCHER) {
            Arrow arrow = new Arrow(level(), this);
            double dx = target.getX() - getX();
            double dy = target.getY(0.33) - arrow.getY();
            double dz = target.getZ() - getZ();
            double d = Math.sqrt(dx * dx + dz * dz);
            arrow.shoot(dx, dy + d * 0.2, dz, 1.6f, 6f);
            arrow.setBaseDamage(2.0 + getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.4);
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            level().addFreshEntity(arrow);
            playSound(SoundEvents.ARROW_SHOOT, 1f, 1f / (getRandom().nextFloat() * 0.4f + 0.8f));
        } else {
            castBolt(target);
        }
    }

    private void castBolt(LivingEntity target) {
        Vec3 dir = target.getEyePosition().subtract(getEyePosition()).normalize();
        ManaBoltEntity bolt = new ManaBoltEntity(level(), this, dir.x, dir.y, dir.z, npc.element());
        bolt.setPos(getX() + dir.x, getEyeY() - 0.2, getZ() + dir.z);
        bolt.setDamage((float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.1f);
        level().addFreshEntity(bolt);
        playSound(SoundEvents.EVOKER_CAST_SPELL, 1f, 1.2f);
    }

    /** Стража: урон по ней вчетверо слабее, не разбивается при падении, постепенно лечится. */
    @Override
    protected void actuallyHurt(DamageSource source, float amount) {
        super.actuallyHurt(source, npc.isGuard() ? amount / 4f : amount);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return !npc.isGuard() && super.causeFallDamage(distance, multiplier, source);
    }

    /** Нет игроков рядом — житель «спит»: не ищет путь и цели (большие города не грузят сервер). */
    private boolean idle;

    @Override
    public void aiStep() {
        if (!level().isClientSide && pname.isEmpty() && tickCount > 2) {
            ensureName();
            if (npc.role == NpcType.Role.CITIZEN && getMainHandItem().isEmpty()) equip();
        }
        if (!level().isClientSide && tickCount % 20 == 0) {
            boolean was = idle;
            idle = getTarget() == null && getLastHurtByMob() == null && bossEvent == null
                    && level().getNearestPlayer(getX(), getY(), getZ(), 64, false) == null;
            if (idle && !was) getNavigation().stop();
        }
        super.aiStep();
    }

    /** Спящий житель стоит на месте: Майнкрафт пропускает его ИИ (цели, поиск пути, взгляд). */
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || idle;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        chatter();
        if (npc.isGuard() && tickCount % 20 == 0 && getHealth() < getMaxHealth()) heal(6f);
        if (bossEvent != null) {
            bossEvent.setName(getDisplayName());
            bossEvent.setProgress(getHealth() / getMaxHealth());
            // вождь ещё и колдует
            LivingEntity t = getTarget();
            if (t != null && --spellCooldown <= 0 && hasLineOfSight(t)) {
                spellCooldown = 50 + getRandom().nextInt(40);
                castBolt(t);
                if (getHealth() < getMaxHealth() * 0.5f && level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, getX(), getY() + 2, getZ(), 6, 0.5, 0.5, 0.5, 0);
                    heal(4);
                }
            }
        }
    }

    /** Жизнь города: реплики над головой и звуки работы мастеров, когда рядом есть игрок. */
    private void chatter() {
        if (npc.role == NpcType.Role.CHIEF || getTarget() != null || tickCount % 20 != 0) return;
        if (--chatterCooldown > 0) return;
        Player near = level().getNearestPlayer(this, 12);
        if (near == null || near.isSpectator()) {
            chatterCooldown = 3;
            return;
        }
        Craft c = craft();
        if (c != null && random.nextBoolean()) {
            // работа: стук молота, бульканье зелий, шорох ткани
            playSound(c.workSound, 0.7f, 0.9f + random.nextFloat() * 0.2f);
            if (level() instanceof ServerLevel sl) {
                Vec3 hand = position().add(getLookAngle().scale(0.6)).add(0, getBbHeight() * 0.55, 0);
                sl.sendParticles(c.workParticle, hand.x, hand.y, hand.z, 6, 0.2, 0.15, 0.2, 0.02);
            }
            swing(InteractionHand.MAIN_HAND);
            chatterCooldown = 4 + random.nextInt(6);
            return;
        }
        com.andrey.litrpg.city.Speech.say(this, com.andrey.litrpg.city.Speech.line(this, random), 100);
        chatterCooldown = 10 + random.nextInt(16);
    }

    @Override
    public void remove(RemovalReason reason) {
        for (var p : getPassengers()) if (p.getTags().contains(com.andrey.litrpg.city.Speech.BUBBLE_TAG)) p.discard();
        super.remove(reason);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (bossEvent != null) bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (bossEvent != null) bossEvent.removePlayer(player);
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean hit) {
        super.dropCustomDeathLoot(source, looting, hit);
        boolean chief = npc.role == NpcType.Role.CHIEF;
        if (npc.outlaw()) {
            // разбойники: монеты, у капитана — ещё и снаряжение Системы
            for (ItemStack c : LootRolls.coinStacks((chief ? 160 : 10) + random.nextInt(chief ? 120 : 14))) spawnAtLocation(c);
            if (chief) {
                int lvl = Math.max(1, getPersistentData().getInt(com.andrey.litrpg.event.MobLevels.TAG));
                spawnAtLocation(com.andrey.litrpg.gear.GearGen.random(random, lvl,
                        random.nextFloat() < 0.3f ? com.andrey.litrpg.gear.Rarity.EPIC : com.andrey.litrpg.gear.Rarity.RARE));
            }
            return;
        }
        if (random.nextFloat() < (chief ? 1f : 0.35f)) spawnAtLocation(new ItemStack(token(), chief ? 3 + random.nextInt(3) : 1));
        for (ItemStack c : LootRolls.coinStacks((chief ? 120 : 6) + random.nextInt(chief ? 80 : 10))) spawnAtLocation(c);
    }

    public net.minecraft.world.item.Item token() {
        return switch (npc.race) {
            case HUMAN -> Items.EMERALD;
            case ELF -> ModItems.ELF_TOKEN.get();
            case DWARF -> ModItems.DWARF_TOKEN.get();
            case ORC -> ModItems.ORC_TOKEN.get();
            case BEASTKIN -> ModItems.BEAST_TOKEN.get();
            default -> ModItems.DEMON_TOKEN.get();
        };
    }

    // ---------- торговля ----------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isAlive() || getTarget() == player || npc.outlaw()) return super.mobInteract(player, hand);
        if (npc.role == NpcType.Role.CITIZEN) {
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (level().isClientSide) return InteractionResult.SUCCESS;
            ServerPlayer sp = (ServerPlayer) player;
            // слух — всегда; торговля — если есть занятие и житель не держит зла
            com.andrey.litrpg.city.Gossip.talk(sp, this);
            if (job != null && !player.isShiftKeyDown() && StatsManager.get(sp).getRep(npc.faction) >= Faction.Tier.NEUTRAL.from) {
                startTrade(sp);
            }
            return InteractionResult.CONSUME;
        }
        if (npc.isArtisan()) {
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (level().isClientSide) return InteractionResult.SUCCESS;
            ServerPlayer sp = (ServerPlayer) player;
            int rep = StatsManager.get(sp).getRep(npc.faction);
            if (rep < Faction.Tier.UNFRIENDLY.from) {
                StatsManager.message(sp, Component.literal(getName().getString() + ": «Уходи. С такими, как ты, дел не веду».").withStyle(ChatFormatting.RED));
                playSound(SoundEvents.VILLAGER_NO, 1f, 1f);
                return InteractionResult.CONSUME;
            }
            ItemStack held = player.getItemInHand(hand);
            if (player.isShiftKeyDown() && !held.isEmpty()) {
                sell(sp, held, rep);
                return InteractionResult.CONSUME;
            }
            // доска мастера: его поручения и кнопка «Торговать»
            com.andrey.litrpg.quest.CityQuests.ensureOffers(sp, this);
            StatsManager.syncNow(sp);
            Craft c = craft();
            com.andrey.litrpg.network.ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                    new com.andrey.litrpg.network.OpenQuestsPacket(com.andrey.litrpg.quest.CityQuests.boardKey(this), npc.faction.key,
                            getName().getString(), c == null ? "" : c.workshop, getId()));
            com.andrey.litrpg.city.Speech.say(this, greeting(sp), 60);
            return InteractionResult.CONSUME;
        }
        if (npc.role != NpcType.Role.TRADER) {
            // воины, маги, стража и вожди раздают поручения своего народа
            if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
            if (level().isClientSide) return InteractionResult.SUCCESS;
            ServerPlayer sp = (ServerPlayer) player;
            if (StatsManager.get(sp).getRep(npc.faction) < Faction.Tier.UNFRIENDLY.from) {
                StatsManager.message(sp, Component.literal(getName().getString() + ": «Убирайся, чужак».").withStyle(ChatFormatting.RED));
                playSound(SoundEvents.VILLAGER_NO, 1f, 1f);
                return InteractionResult.CONSUME;
            }
            com.andrey.litrpg.quest.QuestEvents.ensureFactionOffers(sp, npc.faction);
            StatsManager.syncNow(sp);
            com.andrey.litrpg.network.ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp),
                    new com.andrey.litrpg.network.OpenQuestsPacket(npc.faction.key, npc.faction.key, getName().getString(), "", -1));
            return InteractionResult.CONSUME;
        }
        if (level().isClientSide) return InteractionResult.SUCCESS;
        ServerPlayer sp = (ServerPlayer) player;
        int rep = StatsManager.get(sp).getRep(npc.faction);
        if (rep < Faction.Tier.NEUTRAL.from) {
            StatsManager.message(sp, Component.literal(getName().getString() + ": «Мы не торгуем с такими, как ты».").withStyle(ChatFormatting.RED));
            playSound(SoundEvents.VILLAGER_NO, 1f, 1f);
            return InteractionResult.CONSUME;
        }
        ItemStack held = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && !held.isEmpty()) {
            sell(sp, held, rep);
            return InteractionResult.CONSUME;
        }
        startTrade(sp);
        return InteractionResult.CONSUME;
    }

    /** Список сделок под игрока (репутация, уровень, что уже раскуплено) и окно с отношением и скидкой в заголовке. */
    private void startTrade(ServerPlayer sp) {
        int rep = StatsManager.get(sp).getRep(npc.faction);
        long now = level().getGameTime();
        offers = NpcTrades.offers(this, rep, StatsManager.get(sp).getLevel(),
                k -> sold.contains(k) && now - sold.getLong(k) < NpcTrades.RESTOCK, offerKeys);
        if (offers.isEmpty()) return;
        Faction.Tier tier = Faction.Tier.of(rep);
        int d = NpcTrades.discountPercent(rep);
        // коротко, чтобы влезло в окно: имя без ремесла и скидка цветом отношения (подробности — в окне персонажа)
        String name = getName().getString();
        if (name.indexOf(',') > 0) name = name.substring(0, name.indexOf(','));
        Component title = Component.literal(name + " ").append(Component.literal(d == 0 ? "±0%" : d > 0 ? "−" + d + "%" : "+" + (-d) + "%")
                .withStyle(tier.color));
        if (!net.minecraftforge.fml.loading.FMLLoader.isProduction()) {
            LitRPG.LOGGER.info("[dev] trade {}: {} offers, tier {}, last: {}", name, offers.size(), tier,
                    offers.stream().skip(Math.max(0, offers.size() - 6)).map(o -> o.getResult().getHoverName().getString() + " "
                            + com.andrey.litrpg.gear.GearData.coins(o.getCostA().getCount() * (o.getCostA().is(com.andrey.litrpg.registry.ModItems.GOLD_COIN.get()) ? 81 : 9))
                            + (o.isOutOfStock() ? " (распродано)" : "")).toList());
        }
        setTradingPlayer(sp);
        openTradingScreen(sp, title, 1);
    }

    private String greeting(ServerPlayer p) {
        int rep = StatsManager.get(p).getRep(npc.faction);
        String[] g = rep >= Faction.Tier.HONORED.from
                ? new String[]{"О, мой лучший покупатель!", "Рад тебя видеть, друг!", "Для тебя — лучшее!"}
                : rep >= Faction.Tier.FRIENDLY.from
                ? new String[]{"Добро пожаловать!", "Чем могу помочь?", "Заходи, смотри товар."}
                : new String[]{"Чего тебе?", "Смотри, но руками не трогай.", "Покупай или проходи."};
        return g[random.nextInt(g.length)];
    }

    /** Открыть торговлю (из доски мастера). */
    public void openTrade(ServerPlayer sp) {
        if (!isAlive() || distanceToSqr(sp) > 64) return;
        int rep = StatsManager.get(sp).getRep(npc.faction);
        if (rep < Faction.Tier.NEUTRAL.from) {
            StatsManager.message(sp, Component.literal(getName().getString() + ": «Мы не торгуем с такими, как ты».").withStyle(ChatFormatting.RED));
            return;
        }
        startTrade(sp);
    }

    /** Скупка: предметы Системы, жетоны народов и ценная добыча (трофеи, камни, свитки) за монеты. */
    private void sell(ServerPlayer p, ItemStack held, int rep) {
        int price;
        GearData d = GearData.get(held);
        if (d != null) {
            price = d.value();
        } else if (NpcTrades.isToken(held)) {
            price = 20 * held.getCount();
        } else if (com.andrey.litrpg.gear.SellPrices.unit(held) > 0) {
            price = com.andrey.litrpg.gear.SellPrices.stack(held);
            if (price < 1) {
                StatsManager.message(p, Component.literal(getName().getString() + ": «За такую мелочь и медяка не дам. Принеси побольше».")
                        .withStyle(ChatFormatting.GRAY));
                return;
            }
        } else {
            StatsManager.message(p, Component.literal(getName().getString() + ": «Это мне не нужно. Неси снаряжение Системы, трофеи с чудовищ или ценности».")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        float mult = NpcTrades.repPriceMult(rep);
        float bonus = 1 + StatsManager.get(p).gear().get(GearBonus.SELL_PRICE) / 100f;
        int paid = Math.max(1, Math.round(price / mult * bonus * 0.6f));
        held.setCount(0);
        for (ItemStack c : LootRolls.coinStacks(paid)) {
            if (!p.getInventory().add(c)) p.drop(c, false);
        }
        StatsManager.message(p, Component.literal("Продано за " + GearData.coins(paid)).withStyle(ChatFormatting.YELLOW));
        if (paid >= 5) RepEvents.change(p, npc.faction, 1);
        playSound(SoundEvents.VILLAGER_YES, 1f, 1f);
    }

    @Override public void setTradingPlayer(@Nullable Player player) { tradingPlayer = player; }
    @Nullable @Override public Player getTradingPlayer() { return tradingPlayer; }

    @Override
    public MerchantOffers getOffers() {
        if (offers == null) offers = new MerchantOffers();
        return offers;
    }

    @Override public void overrideOffers(MerchantOffers o) { offers = o; }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        int i = offers == null ? -1 : offers.indexOf(offer);
        if (i >= 0 && i < offerKeys.size() && offerKeys.get(i) != null) {
            // штучная вещь сокровищницы — до нового завоза её нет
            sold.putLong(offerKeys.get(i), level().getGameTime());
            for (String k : java.util.List.copyOf(sold.getAllKeys())) {
                if (level().getGameTime() - sold.getLong(k) >= NpcTrades.RESTOCK) sold.remove(k);
            }
        }
        ambientSoundTime = -getAmbientSoundInterval();
        if (tradingPlayer instanceof ServerPlayer sp) RepEvents.change(sp, npc.faction, 2);
    }

    @Override public void notifyTradeUpdated(ItemStack stack) {}
    @Override public int getVillagerXp() { return 0; }
    @Override public void overrideXp(int xp) {}
    @Override public boolean showProgressBar() { return false; }
    @Override public SoundEvent getNotifyTradeSound() { return SoundEvents.VILLAGER_YES; }
    @Override public boolean isClientSide() { return level().isClientSide; }

    // ---------- звуки и прочее ----------

    @Override
    protected SoundEvent getAmbientSound() {
        return switch (npc.race) {
            case ORC -> SoundEvents.PIGLIN_BRUTE_AMBIENT;
            case BEASTKIN -> SoundEvents.WOLF_AMBIENT;
            case DEMONKIN -> SoundEvents.BLAZE_AMBIENT;
            default -> SoundEvents.VILLAGER_AMBIENT;
        };
    }

    @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.PLAYER_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.PLAYER_DEATH; }

    @Override
    public float getVoicePitch() {
        return switch (npc.race) {
            case DWARF -> 0.75f;
            case ORC -> 0.7f;
            case ELF -> 1.25f;
            default -> 1f;
        } * (0.9f + getRandom().nextFloat() * 0.2f);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (craft() != null) tag.putString("Craft", craft().key);
        tag.putBoolean("Female", female);
        if (!pname.isEmpty()) tag.putString("PName", pname);
        if (!storyName.isEmpty()) tag.putString("SName", storyName);
        if (job != null) tag.putString("Job", job.name());
        if (!sold.isEmpty()) tag.put("Sold", sold);
        if (hasRestriction()) {
            tag.putInt("HomeX", getRestrictCenter().getX());
            tag.putInt("HomeY", getRestrictCenter().getY());
            tag.putInt("HomeZ", getRestrictCenter().getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Craft")) setCraft(Craft.byKey(tag.getString("Craft")));
        female = tag.getBoolean("Female");
        pname = tag.getString("PName");
        sold = tag.getCompound("Sold");
        storyName = tag.getString("SName");
        job = tag.contains("Job") ? Job.byKey(tag.getString("Job")) : null;
        if (tag.contains("HomeX")) {
            restrictTo(new net.minecraft.core.BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ")), homeRadius());
        }
        if (getMainHandItem().isEmpty() && npc.role != NpcType.Role.TRADER) equip();
    }
}
