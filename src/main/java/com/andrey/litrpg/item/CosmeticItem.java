package com.andrey.litrpg.item;

import com.andrey.litrpg.LitRPG;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Косметика из лавки привала: свиток следа (за игроком тянутся искры, пламя, снег…) и свиток
 * титула (приставка к имени над головой и в списке игроков). Силы не даёт — только вид.
 * Выбор хранится в сохраняемых после смерти данных игрока; свиток «Без следа» снимает след.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID)
public class CosmeticItem extends Item {
    public static final String TRAIL = "litrpg_trail";
    public static final String TITLE = "litrpg_title";
    private final boolean title;

    /** Следы: ключ, название, частица. */
    public enum Trail {
        EMBER("ember", "Искры костра", ParticleTypes.SMALL_FLAME),
        SOUL("soul", "Пламя душ", ParticleTypes.SOUL_FIRE_FLAME),
        SNOW("snow", "Иней", ParticleTypes.SNOWFLAKE),
        PETALS("petals", "Лепестки", ParticleTypes.CHERRY_LEAVES),
        STARS("stars", "Звёздная пыль", ParticleTypes.END_ROD),
        SPORES("spores", "Споры чащи", ParticleTypes.SPORE_BLOSSOM_AIR),
        NOTES("notes", "Ноты", ParticleTypes.NOTE),
        VOID("void", "Пыль Бездны", ParticleTypes.REVERSE_PORTAL);

        public final String key, title;
        final ParticleOptions particle;

        Trail(String key, String title, ParticleOptions particle) {
            this.key = key;
            this.title = title;
            this.particle = particle;
        }

        static Trail byKey(String k) {
            for (Trail t : values()) if (t.key.equals(k)) return t;
            return null;
        }
    }

    /** Титулы по этапам: чем глубже лавка, тем громче титул. */
    public static final String[][] TITLES = {
            {"Странник", "Новичок Системы"},
            {"Гроза склепов", "Костолом"},
            {"Паучья смерть", "Лесной призрак"},
            {"Ныряльщик", "Сухой в воде"},
            {"Закалённый", "Пепельный"},
            {"Легенда Шпиля", "Видевший Конец"},
    };

    public CosmeticItem(Properties props, boolean title) {
        super(props);
        this.title = title;
    }

    public static ItemStack trail(Item item, Trail t) {
        ItemStack st = new ItemStack(item);
        st.getOrCreateTag().putString("kind", t.key);
        st.setHoverName(Component.literal("Свиток следа: " + t.title).withStyle(ChatFormatting.LIGHT_PURPLE));
        return st;
    }

    public static ItemStack title(Item item, String text) {
        ItemStack st = new ItemStack(item);
        st.getOrCreateTag().putString("kind", text);
        st.setHoverName(Component.literal("Свиток титула: «" + text + "»").withStyle(ChatFormatting.GOLD));
        return st;
    }

    private static CompoundTag kept(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        String kind = stack.hasTag() ? stack.getTag().getString("kind") : "";
        if (player instanceof ServerPlayer sp && !kind.isEmpty()) {
            if (title) {
                kept(sp).putString(TITLE, kind);
                sp.refreshDisplayName();
                sp.refreshTabListName();
                sp.displayClientMessage(Component.literal("Теперь вы — «" + kind + "»").withStyle(ChatFormatting.GOLD), false);
            } else {
                kept(sp).putString(TRAIL, kind);
                Trail t = Trail.byKey(kind);
                sp.displayClientMessage(Component.literal("След: " + (t == null ? kind : t.title)).withStyle(ChatFormatting.LIGHT_PURPLE), false);
            }
            level.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 1.4f);
            if (!sp.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(title ? "ПКМ — взять титул: он встанет перед именем над головой."
                : "ПКМ — за вами потянется след. Силы не даёт, только вид.").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @SubscribeEvent
    public static void onName(PlayerEvent.NameFormat e) {
        String t = kept(e.getEntity()).getString(TITLE);
        if (!t.isEmpty()) e.setDisplayname(Component.literal("[" + t + "] ").withStyle(ChatFormatting.GOLD).append(e.getDisplayname()));
    }

    @SubscribeEvent
    public static void onTabName(PlayerEvent.TabListNameFormat e) {
        String t = kept(e.getEntity()).getString(TITLE);
        if (!t.isEmpty()) e.setDisplayName(Component.literal("[" + t + "] ").withStyle(ChatFormatting.GOLD).append(e.getEntity().getName()));
    }

    @SubscribeEvent
    public static void onTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p) || p.tickCount % 3 != 0 || p.isSpectator()) return;
        Trail t = Trail.byKey(kept(p).getString(TRAIL));
        if (t == null) return;
        double dx = p.getX() - p.xo, dz = p.getZ() - p.zo;
        if (dx * dx + dz * dz < 0.002) return;
        ((ServerLevel) p.level()).sendParticles(t.particle, p.getX(), p.getY() + 0.15, p.getZ(), 2, 0.2, 0.05, 0.2, 0.01);
    }
}
