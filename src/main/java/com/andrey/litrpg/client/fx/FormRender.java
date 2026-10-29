package com.andrey.litrpg.client.fx;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.client.ClientData;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * Облик меняет не только цифры, но и то, кого видят вокруг: пока он надет, вместо игрока
 * рисуется зверь. Своей модели не делаем — берём ванильную и рисуем её на месте игрока,
 * с его поворотом и его шагом.
 * <p>
 * Звери-«болванки» не добавлены в мир и ничего не делают: они существуют только затем,
 * чтобы их нарисовал обычный отрисовщик существ со всей анимацией и текстурами.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID, value = Dist.CLIENT)
public final class FormRender {
    private static final Map<String, LivingEntity> DUMMIES = new HashMap<>();

    private FormRender() {}

    /** Какой зверь соответствует облику. Ключ облика оканчивается названием зверя. */
    private static EntityType<?> beast(String formKey) {
        String kind = formKey.substring(formKey.lastIndexOf('_') + 1);
        return switch (kind) {
            case "wolf" -> EntityType.WOLF;
            case "bear" -> EntityType.POLAR_BEAR;
            case "raven" -> EntityType.PARROT;
            case "primal" -> EntityType.RAVAGER;
            default -> null;
        };
    }

    private static LivingEntity dummy(String formKey, Level level) {
        LivingEntity made = DUMMIES.get(formKey);
        if (made != null && made.level() == level) return made;
        EntityType<?> type = beast(formKey);
        if (type == null) return null;
        Entity raw = type.create(level);
        if (!(raw instanceof LivingEntity le)) return null;
        if (le instanceof Wolf wolf) {
            wolf.setTame(false);
            wolf.setBaby(false);
        }
        if (le instanceof Parrot parrot) parrot.setVariant(Parrot.Variant.GRAY);
        DUMMIES.put(formKey, le);
        return le;
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player p = event.getEntity();
        String form = ClientData.FORMS.get(p.getUUID());
        if (form == null) return;
        LivingEntity beast = dummy(form, p.level());
        if (beast == null) return;

        float partial = event.getPartialTick();
        // зверь встаёт ровно туда же, куда смотрит и где стоит игрок
        double x = Mth.lerp(partial, p.xOld, p.getX());
        double y = Mth.lerp(partial, p.yOld, p.getY());
        double z = Mth.lerp(partial, p.zOld, p.getZ());
        beast.setPos(x, y, z);
        beast.xOld = x;
        beast.yOld = y;
        beast.zOld = z;
        beast.setYRot(p.getYRot());
        beast.yRotO = p.getYRot();
        beast.setXRot(p.getXRot());
        beast.xRotO = p.getXRot();
        beast.yBodyRot = p.yBodyRot;
        beast.yBodyRotO = p.yBodyRot;
        beast.yHeadRot = p.yHeadRot;
        beast.yHeadRotO = p.yHeadRot;
        beast.tickCount = p.tickCount;
        beast.setOnGround(p.onGround());
        beast.setShiftKeyDown(p.isShiftKeyDown());
        beast.setSwimming(p.isSwimming());
        // шаг: ноги зверя двигаются ровно настолько, насколько бежит игрок
        beast.walkAnimation.update(p.walkAnimation.speed(), 1f);
        // имя над зверем — чтобы товарища не потеряли из виду
        beast.setCustomName(p.getName());
        beast.setCustomNameVisible(!p.isInvisible());

        event.setCanceled(true);
        float yaw = Mth.lerp(partial, p.yRotO, p.getYRot());
        Minecraft.getInstance().getEntityRenderDispatcher().render(beast, 0, 0, 0, yaw, partial,
                event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight());
    }

    /** Смена мира — болванки прежнего мира больше не годятся. */
    public static void clear() {
        DUMMIES.clear();
    }
}
