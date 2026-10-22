package com.andrey.litrpg.rep;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.entity.GoblinEntity;
import com.andrey.litrpg.party.DamageTracker;
import com.andrey.litrpg.stats.PlayerClass;
import com.andrey.litrpg.stats.PlayerStats;
import com.andrey.litrpg.stats.StatsManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Изменение репутации и её последствия. */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public final class RepEvents {
    /** Моб помнит, что игрок напал на него, и перестаёт быть дружелюбным. */
    private static final String PROVOKED_TAG = "litrpg_provoked";

    private RepEvents() {}

    public static void change(ServerPlayer p, Faction f, int delta) {
        if (delta == 0) return;
        PlayerStats s = StatsManager.get(p);
        Faction.Tier before = Faction.Tier.of(s.getRep(f));
        s.addRep(f, delta);
        Faction.Tier after = Faction.Tier.of(s.getRep(f));
        p.displayClientMessage(Component.literal(f.title + " " + (delta > 0 ? "+" : "") + delta)
                .withStyle(delta > 0 ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        if (before != after) {
            StatsManager.message(p, Component.literal("Отношение народа «" + f.title + "»: ")
                    .append(Component.literal(after.title).withStyle(after.color)));
            if (after.ordinal() >= Faction.Tier.REVERED.ordinal() && before.ordinal() < Faction.Tier.REVERED.ordinal()) {
                com.andrey.litrpg.stats.Title t = switch (f) {
                    case ELVES -> com.andrey.litrpg.stats.Title.ELF_FRIEND;
                    case DWARVES -> com.andrey.litrpg.stats.Title.DWARF_FRIEND;
                    case ORCS -> com.andrey.litrpg.stats.Title.ORC_FRIEND;
                    case BEASTKIN -> com.andrey.litrpg.stats.Title.BEAST_FRIEND;
                    case DEMONKIN -> com.andrey.litrpg.stats.Title.DEMON_FRIEND;
                    case KINGDOM -> com.andrey.litrpg.stats.Title.KINGDOM_FRIEND;
                    default -> null;
                };
                if (t != null) StatsManager.grantTitle(p, t);
            }
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(),
                    after.ordinal() > before.ordinal() ? SoundEvents.VILLAGER_CELEBRATE : SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8f, 1f);
        }
        StatsManager.sync(p);
    }

    /** Начальная репутация при выборе расы. */
    public static void initFor(ServerPlayer p) {
        PlayerStats s = StatsManager.get(p);
        for (Faction f : Faction.values()) s.setRep(f, f.start(s.getRace()));
    }

    /** Враги народа: убийство его жителей радует этих соседей. */
    public static Faction[] rivals(Faction f) {
        return switch (f) {
            case ELVES -> new Faction[]{Faction.ORCS, Faction.DEMONKIN};
            case DWARVES -> new Faction[]{Faction.ORCS, Faction.GOBLINS};
            case ORCS -> new Faction[]{Faction.ELVES, Faction.DWARVES, Faction.KINGDOM};
            case DEMONKIN -> new Faction[]{Faction.ELVES, Faction.KINGDOM};
            case BEASTKIN -> new Faction[]{Faction.ORCS};
            default -> new Faction[0];
        };
    }

    public static Faction factionOf(Entity e) {
        if (e instanceof com.andrey.litrpg.npc.RaceNpcEntity npc) return npc.npcType().faction;
        if (e instanceof AbstractVillager || e instanceof IronGolem) return Faction.KINGDOM;
        if (e instanceof GoblinEntity) return Faction.GOBLINS;
        if (e instanceof AbstractPiglin) return Faction.PIGLINS;
        if (e instanceof AbstractIllager || e instanceof Ravager || e instanceof Witch) return Faction.ILLAGERS;
        if (e instanceof Monster && ((Monster) e).getMobType() == MobType.UNDEAD) return Faction.UNDEAD;
        if (e instanceof Wolf || e instanceof PolarBear || e instanceof Bee) return Faction.WILDS;
        return null;
    }

    // ---------------- изменение репутации ----------------

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide || dead instanceof Player) return;
        ServerPlayer p = DamageTracker.topContributor(dead, event.getSource());
        if (p == null) return;
        Faction f = factionOf(dead);
        if (dead instanceof com.andrey.litrpg.npc.RaceNpcEntity npc && npc.npcType().outlaw()) {
            // разбойники и пираты: людям — спокойнее, разбойному люду — обида
            change(p, Faction.KINGDOM, npc.npcType().role == com.andrey.litrpg.npc.NpcType.Role.CHIEF ? 15 : 2);
            change(p, Faction.ILLAGERS, -4);
            return;
        }
        if (dead instanceof com.andrey.litrpg.npc.RaceNpcEntity npc) {
            boolean chief = npc.npcType().role == com.andrey.litrpg.npc.NpcType.Role.CHIEF;
            change(p, f, chief ? -120 : -25);
            for (Faction rival : rivals(f)) change(p, rival, chief ? 25 : 5);
            return;
        }
        if (dead instanceof com.andrey.litrpg.entity.GoblinEntity) change(p, Faction.DWARVES, 2);
        rewardEnemyKill(p, dead);
        // защита поселений рас: монстр убит рядом с их жителями
        if (dead instanceof net.minecraft.world.entity.monster.Enemy) {
            var near = dead.level().getEntitiesOfClass(com.andrey.litrpg.npc.RaceNpcEntity.class, dead.getBoundingBox().inflate(32));
            if (!near.isEmpty()) change(p, near.get(0).npcType().faction, 2);
        }
        if (dead instanceof Villager) {
            change(p, Faction.KINGDOM, -60);
            change(p, Faction.ILLAGERS, 15);
            change(p, Faction.UNDEAD, 10);
        } else if (dead instanceof IronGolem) {
            change(p, Faction.KINGDOM, -30);
        } else if (f == Faction.ILLAGERS) {
            change(p, Faction.ILLAGERS, -12);
            change(p, Faction.KINGDOM, 8);
        } else if (f == Faction.GOBLINS) {
            change(p, Faction.GOBLINS, -6);
            if (p.getRandom().nextInt(3) == 0) change(p, Faction.KINGDOM, 1);
        } else if (f == Faction.PIGLINS) {
            change(p, Faction.PIGLINS, -12);
        } else if (dead instanceof Hoglin) {
            change(p, Faction.PIGLINS, 4);
        } else if (f == Faction.UNDEAD) {
            if (p.getRandom().nextInt(2) == 0) change(p, Faction.UNDEAD, -1);
            // защита деревни: нежить рядом с жителями
            if (!dead.level().getEntitiesOfClass(Villager.class, dead.getBoundingBox().inflate(32)).isEmpty()) change(p, Faction.KINGDOM, 2);
        } else if (f == Faction.WILDS) {
            change(p, Faction.WILDS, -15);
        } else if (dead instanceof Animal) {
            if (p.getRandom().nextInt(4) == 0) change(p, Faction.WILDS, -1);
        } else if (dead instanceof Monster && !dead.level().getEntitiesOfClass(Villager.class, dead.getBoundingBox().inflate(32)).isEmpty()) {
            change(p, Faction.KINGDOM, 1);
        }
    }

    /**
     * У каждого народа есть свои враги среди монстров. Их убийство повышает
     * репутацию: элиты ценятся вдвое, чемпионы — впятеро.
     */
    private static void rewardEnemyKill(ServerPlayer p, LivingEntity dead) {
        if (dead instanceof com.andrey.litrpg.npc.RaceNpcEntity || dead instanceof com.andrey.litrpg.entity.LitBoss) return;
        var rank = com.andrey.litrpg.event.MobLevels.getRank(dead);
        int mult = rank == com.andrey.litrpg.event.MobLevels.Rank.CHAMPION ? 5 : rank == com.andrey.litrpg.event.MobLevels.Rank.ELITE ? 2 : 1;
        java.util.Map<Faction, Integer> gain = new java.util.EnumMap<>(Faction.class);
        var rnd = p.getRandom();
        if (dead instanceof Monster m && m.getMobType() == MobType.UNDEAD) {
            // нежить — общий враг живых
            gain.merge(Faction.KINGDOM, 1, Integer::sum);
            if (rnd.nextBoolean()) gain.merge(Faction.ELVES, 1, Integer::sum);
            if (rnd.nextInt(3) == 0) gain.merge(Faction.BEASTKIN, 1, Integer::sum);
            if (dead instanceof net.minecraft.world.entity.monster.WitherSkeleton) gain.merge(Faction.PIGLINS, 3, Integer::sum);
        }
        if (dead instanceof net.minecraft.world.entity.monster.CaveSpider || dead instanceof net.minecraft.world.entity.monster.Silverfish) {
            gain.merge(Faction.DWARVES, 2, Integer::sum); // вредители шахт
        } else if (dead instanceof net.minecraft.world.entity.monster.Spider) {
            gain.merge(Faction.ELVES, 2, Integer::sum); // пауки губят леса
            if (rnd.nextBoolean()) gain.merge(Faction.BEASTKIN, 1, Integer::sum);
        }
        if (dead instanceof net.minecraft.world.entity.monster.Creeper) {
            gain.merge(Faction.DWARVES, 1, Integer::sum);
            gain.merge(Faction.KINGDOM, 1, Integer::sum);
        }
        if (dead instanceof net.minecraft.world.entity.raid.Raider && !(dead instanceof Witch)) {
            // разбойники и браконьеры
            gain.merge(Faction.BEASTKIN, 3, Integer::sum);
            gain.merge(Faction.ELVES, 2, Integer::sum);
            gain.merge(Faction.DWARVES, 1, Integer::sum);
            if (dead instanceof net.minecraft.world.entity.monster.Evoker) gain.merge(Faction.DEMONKIN, 4, Integer::sum);
        }
        if (dead instanceof Witch) {
            gain.merge(Faction.BEASTKIN, 2, Integer::sum);
            gain.merge(Faction.ELVES, 1, Integer::sum);
        }
        if (dead instanceof GoblinEntity) {
            gain.merge(Faction.ELVES, 1, Integer::sum);
            if (rnd.nextBoolean()) gain.merge(Faction.ORCS, -1, Integer::sum); // гоблины — родня орков
        }
        if (dead instanceof net.minecraft.world.entity.monster.Phantom) {
            gain.merge(Faction.BEASTKIN, 2, Integer::sum);
            gain.merge(Faction.ELVES, 1, Integer::sum);
        }
        if (dead instanceof net.minecraft.world.entity.monster.EnderMan || dead instanceof net.minecraft.world.entity.monster.Shulker) {
            gain.merge(Faction.DEMONKIN, 2, Integer::sum); // Пустота — соперница Пепла
        }
        if (dead instanceof net.minecraft.world.entity.monster.Blaze || dead instanceof net.minecraft.world.entity.monster.Ghast
                || dead instanceof net.minecraft.world.entity.monster.MagmaCube) {
            gain.merge(Faction.DWARVES, 1, Integer::sum);
        }
        if (dead instanceof net.minecraft.world.entity.monster.Slime && !(dead instanceof net.minecraft.world.entity.monster.MagmaCube)
                && rnd.nextBoolean()) {
            gain.merge(Faction.WILDS, 1, Integer::sum);
        }
        // орки уважают силу: победа над элитой или чемпионом
        if (mult > 1 && dead instanceof net.minecraft.world.entity.monster.Enemy) gain.merge(Faction.ORCS, mult, Integer::sum);
        for (var e : gain.entrySet()) change(p, e.getKey(), e.getValue() * (e.getValue() > 0 ? mult : 1));
    }

    /** Победа над боссом подземелья: благодарность одних народов и гнев других. */
    public static void bossKilled(ServerPlayer p, java.util.Map<Faction, Integer> rep) {
        for (var e : rep.entrySet()) change(p, e.getKey(), e.getValue());
    }

    @SubscribeEvent
    public static void onTrade(TradeWithVillagerEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) change(p, Faction.KINGDOM, 3);
    }

    @SubscribeEvent
    public static void onBreed(BabyEntitySpawnEvent event) {
        if (event.getCausedByPlayer() instanceof ServerPlayer p) change(p, Faction.WILDS, 4);
    }

    @SubscribeEvent
    public static void onTame(AnimalTameEvent event) {
        if (event.getTamer() instanceof ServerPlayer p) change(p, Faction.WILDS, 20);
    }

    /** Подарки: золотой слиток гоблину или пиглину. И синхронизация репутации с ценами жителей. */
    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        Entity t = event.getTarget();
        ItemStack hand = event.getItemStack();
        if (t instanceof GoblinEntity && hand.is(Items.GOLD_INGOT)) {
            if (!p.getAbilities().instabuild) hand.shrink(1);
            change(p, Faction.GOBLINS, 15);
            p.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, t.getX(), t.getY() + 1.2, t.getZ(), 8, 0.3, 0.3, 0.3, 0);
            t.getPersistentData().remove(PROVOKED_TAG);
            event.setCanceled(true);
            return;
        }
        if (t instanceof AbstractPiglin && hand.is(Items.GOLD_INGOT)) {
            change(p, Faction.PIGLINS, 3);
            return;
        }
        if (t instanceof Villager v) syncGossip(p, v);
    }

    /**
     * Цены жителей в ванилле зависят от «сплетен» о игроке. Переводим нашу
     * репутацию в сплетни: высокая — скидки, низкая — наценки, а при ненависти
     * железные големы нападают сами.
     */
    private static void syncGossip(ServerPlayer p, Villager v) {
        int rep = StatsManager.get(p).getRep(Faction.KINGDOM);
        var gossip = v.getGossips();
        gossip.remove(p.getUUID(), GossipType.MAJOR_POSITIVE);
        gossip.remove(p.getUUID(), GossipType.MAJOR_NEGATIVE);
        if (rep > 0) gossip.add(p.getUUID(), GossipType.MAJOR_POSITIVE, Math.min(20, rep / 40));
        else if (rep < 0) gossip.add(p.getUUID(), GossipType.MAJOR_NEGATIVE, Math.min(100, -rep / 10));
    }

    // ---------------- последствия ----------------

    /** Дружелюбные народы не выбирают игрока целью, пока он их не тронул. */
    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewTarget() instanceof ServerPlayer p)) return;
        LivingEntity mob = event.getEntity();
        if (mob.getPersistentData().getBoolean(PROVOKED_TAG)) return;
        Faction f = factionOf(mob);
        if (f == null || f == Faction.KINGDOM) return;
        PlayerStats s = StatsManager.get(p);
        int rep = s.getRep(f);
        boolean friendly = rep >= Faction.Tier.FRIENDLY.from;
        if (f == Faction.GOBLINS && s.gear().hasSet(com.andrey.litrpg.gear.GearSet.GOBLIN)) friendly = true;
        if (f == Faction.UNDEAD && (s.getPlayerClass() != null && s.getPlayerClass().styles.getOrDefault(com.andrey.litrpg.stats.Style.UNDEAD, 0) >= 3
                && s.getPlayerClass().isHidden())
                && rep >= 0) friendly = true;
        if (friendly) event.setCanceled(true);
    }

    /** Нападение на моба делает его (и его соседей) враждебным, несмотря на репутацию. */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer)) return;
        LivingEntity target = event.getEntity();
        Faction f = factionOf(target);
        if (f == null) return;
        if (target instanceof com.andrey.litrpg.npc.RaceNpcEntity o && o.npcType().outlaw()) return;
        if (target instanceof com.andrey.litrpg.npc.RaceNpcEntity && event.getSource().getEntity() instanceof ServerPlayer sp
                && !target.getPersistentData().getBoolean(PROVOKED_TAG)) {
            change(sp, f, -10); // нападение на жителя
        }
        if (target.getPersistentData().getBoolean(PROVOKED_TAG)) return; // соседи уже предупреждены
        target.getPersistentData().putBoolean(PROVOKED_TAG, true);
        for (LivingEntity e : target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(12),
                e -> factionOf(e) == f)) {
            e.getPersistentData().putBoolean(PROVOKED_TAG, true);
        }
    }

    /** Дети природы мстят тем, кого ненавидят: волки нападают сами. */
    @SubscribeEvent
    public static void onWolfTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Wolf wolf) || wolf.level().isClientSide || wolf.isTame() || wolf.tickCount % 40 != 0) return;
        if (wolf.getTarget() != null) return;
        for (ServerPlayer p : wolf.level().getEntitiesOfClass(ServerPlayer.class, wolf.getBoundingBox().inflate(16))) {
            if (!p.isCreative() && StatsManager.get(p).getRep(Faction.WILDS) <= Faction.Tier.HOSTILE.from) {
                wolf.setTarget(p);
                wolf.setRemainingPersistentAngerTime(400);
                break;
            }
        }
    }
}
