package com.andrey.litrpg.gear;

import com.andrey.litrpg.world.Zone;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

/**
 * Добавляет предметы Системы в сундуки мира (данжи, храмы, крепости…).
 * Уровень предмета зависит от зоны, где стоит сундук.
 */
public class ChestLootModifier extends LootModifier {
    public static final Codec<ChestLootModifier> CODEC = RecordCodecBuilder.create(inst -> codecStart(inst)
            .and(Codec.FLOAT.fieldOf("chance").forGetter(m -> m.chance))
            .apply(inst, ChestLootModifier::new));

    private final float chance;

    public ChestLootModifier(LootItemCondition[] conditions, float chance) {
        super(conditions);
        this.chance = chance;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext ctx) {
        if (!ctx.getQueriedLootTableId().getPath().startsWith("chests/")) return loot;
        var rnd = ctx.getRandom();
        String table = ctx.getQueriedLootTableId().getPath();
        if (table.startsWith("chests/dungeon_")) {
            // сундуки подземелий: предметы уровня подземелья, всегда и щедрее
            var d = com.andrey.litrpg.world.Dungeon.byKey(table.substring("chests/dungeon_".length()));
            if (d != null) {
                int n = 1 + rnd.nextInt(2);
                for (int i = 0; i < n; i++) {
                    loot.add(GearGen.random(rnd, Math.max(1, d.level + rnd.nextInt(5) - 2), GearGen.rollRarity(rnd, 0.8f + d.level * 0.015f)));
                }
                loot.addAll(LootRolls.coinStacks(20 + d.level * (6 + rnd.nextInt(6))));
                if (rnd.nextFloat() < 0.07f) loot.add(new ItemStack(com.andrey.litrpg.registry.ModItems.ASCENSION_STONE.get()));
                if (rnd.nextFloat() < 0.25f) loot.add(new ItemStack(com.andrey.litrpg.registry.ModItems.REFORGE_STONE.get()));
                return loot;
            }
        }
        if (table.startsWith("chests/city_")) {
            // городские сундуки: снаряжение Системы только у оружейников и в арсенале замка
            if (!(table.contains("armory") || table.contains("smith")) || rnd.nextFloat() >= 0.3f) return loot;
        } else if (rnd.nextFloat() >= chance) return loot;
        Vec3 origin = ctx.getParamOrNull(LootContextParams.ORIGIN);
        int level = 3;
        if (origin != null) {
            int[] z = Zone.levels(ctx.getLevel(), BlockPos.containing(origin));
            level = z[0] + rnd.nextInt(Math.max(1, z[1] - z[0] + 1));
        }
        String path = ctx.getQueriedLootTableId().getPath();
        // особые места — добыча богаче
        float boost = path.contains("end_city") || path.contains("ancient_city") || path.contains("bastion") ? 1.5f
                : path.contains("stronghold") || path.contains("woodland") || path.contains("nether_bridge") ? 0.9f : 0.4f;
        loot.add(GearGen.random(rnd, level, GearGen.rollRarity(rnd, boost)));
        if (rnd.nextFloat() < 0.2f) loot.add(GearGen.random(rnd, level, GearGen.rollRarity(rnd, boost)));
        return loot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
