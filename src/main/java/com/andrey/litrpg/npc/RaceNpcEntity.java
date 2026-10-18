package com.andrey.litrpg.npc;

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
    private final NpcType npc;
    @Nullable private Player tradingPlayer;
    @Nullable private MerchantOffers offers;
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

    public static AttributeSupplier.Builder createAttributes(NpcType t) {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, t.health)
                .add(Attributes.ATTACK_DAMAGE, t.damage)
                .add(Attributes.MOVEMENT_SPEED, t.speed)
                .add(Attributes.FOLLOW_RANGE, 28.0)
                .add(Attributes.ARMOR, t.role == NpcType.Role.CHIEF ? 10 : t.race == com.andrey.litrpg.stats.Race.DWARF ? 6 : 3)
                .add(Attributes.KNOCKBACK_RESISTANCE, t.role == NpcType.Role.CHIEF ? 0.7 : 0.1);
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
            case ARCHER -> goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 30, 18f));
            case MAGE -> goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 40, 16f));
            case TRADER -> {
                goalSelector.addGoal(1, new PanicGoal(this, 1.3));
                goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Monster.class, 10f, 1.0, 1.3));
            }
        }
        goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.8));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        if (npc.fights()) {
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
        int rep = StatsManager.get(p).getRep(npc.faction);
        return rep < Faction.Tier.UNFRIENDLY.from;
    }

    // ---------- появление ----------

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data, tag);
        equip();
        setPersistenceRequired();
        restrictTo(blockPosition(), npc.role == NpcType.Role.TRADER ? 10 : 22);
        return result;
    }

    private void equip() {
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
            case TRADER -> ItemStack.EMPTY;
        };
        setItemSlot(EquipmentSlot.MAINHAND, main);
        setDropChance(EquipmentSlot.MAINHAND, npc.role == NpcType.Role.CHIEF ? 0.25f : 0.02f);
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

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
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
        if (random.nextFloat() < (chief ? 1f : 0.35f)) spawnAtLocation(new ItemStack(token(), chief ? 3 + random.nextInt(3) : 1));
        for (ItemStack c : LootRolls.coinStacks((chief ? 120 : 6) + random.nextInt(chief ? 80 : 10))) spawnAtLocation(c);
    }

    public net.minecraft.world.item.Item token() {
        return switch (npc.race) {
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
        if (!isAlive() || getTarget() == player) return super.mobInteract(player, hand);
        if (npc.role != NpcType.Role.TRADER) {
            // воины, маги и вожди раздают поручения своего народа
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
                    new com.andrey.litrpg.network.OpenQuestsPacket(npc.faction.key, getName().getString()));
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
        offers = NpcTrades.offers(this, rep);
        setTradingPlayer(player);
        openTradingScreen(player, getDisplayName(), 1);
        return InteractionResult.CONSUME;
    }

    /** Скупка: предметы Системы и жетоны народов за монеты. */
    private void sell(ServerPlayer p, ItemStack held, int rep) {
        int price;
        GearData d = GearData.get(held);
        if (d != null) {
            price = d.value();
        } else if (NpcTrades.isToken(held)) {
            price = 20 * held.getCount();
        } else {
            StatsManager.message(p, Component.literal(getName().getString() + ": «Это мне не нужно. Неси снаряжение Системы или жетоны народов».")
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
        RepEvents.change(p, npc.faction, 1);
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
        if (hasRestriction()) {
            tag.putInt("HomeX", getRestrictCenter().getX());
            tag.putInt("HomeY", getRestrictCenter().getY());
            tag.putInt("HomeZ", getRestrictCenter().getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HomeX")) {
            restrictTo(new net.minecraft.core.BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ")),
                    npc.role == NpcType.Role.TRADER ? 10 : 22);
        }
        if (getMainHandItem().isEmpty() && npc.role != NpcType.Role.TRADER) equip();
    }
}
