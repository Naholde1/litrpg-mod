package com.andrey.litrpg.client.music;

import com.andrey.litrpg.LitRPG;
import com.andrey.litrpg.entity.LitBoss;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.BufferUtils;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Музыка боя с боссом: когда рядом босс, обычная музыка смолкает и вступает фонк
 * (у каждого босса — свой трек из {@link PhonkSynth}). Громкость — ползунок «Музыка».
 * Босс пал или вы ушли — трек плавно затихает.
 */
@Mod.EventBusSubscriber(modid = LitRPG.MODID, value = Dist.CLIENT)
public final class BossMusic {
    private BossMusic() {}

    private static final int START = 30, KEEP = 64;
    private static final Map<Long, CompletableFuture<short[]>> TRACKS = new LinkedHashMap<>();
    private static Track playing;
    /** Трек из файла (музыка владельца), если у хранителя он есть. */
    private static FileTrack file;
    private static int lostFor;

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            if (playing != null) playing.fadeOut();
            playing = null;
            return;
        }
        LivingEntity boss = nearestBoss(mc, playing == null && file == null ? START : KEEP);
        if (file != null) {
            if (file.isStopped() || !mc.getSoundManager().isActive(file) && file.ticks > 40) file = null;
            else {
                mc.getMusicManager().stopPlaying();
                if (boss == null || !mc.player.isAlive()) {
                    if (++lostFor > 60) {
                        file.fadeOut();
                        file = null;
                    }
                } else lostFor = 0;
                return;
            }
        }
        if (playing == null && boss != null && mc.player.isAlive()) {
            var key = ForgeRegistries.ENTITY_TYPES.getKey(boss.getType());
            String name = key == null ? null : Tracks.BOSS.get(key.getPath());
            net.minecraft.sounds.SoundEvent ev = name == null ? null : com.andrey.litrpg.registry.ModSounds.music(name);
            if (ev != null && mc.getSoundManager().getSoundEvent(ev.getLocation()) != null) {
                mc.getMusicManager().stopPlaying();
                file = new FileTrack(ev);
                lostFor = 0;
                mc.getSoundManager().play(file);
                LitRPG.LOGGER.info("Boss music (file) {} for {}", name, key);
                return;
            }
        }
        if (playing != null && (playing.isStopped() || !mc.getSoundManager().isActive(playing) && playing.started)) playing = null;
        if (playing != null) {
            mc.getMusicManager().stopPlaying(); // обычная музыка ждёт, пока идёт бой
            if (boss == null || !mc.player.isAlive()) {
                if (++lostFor > 60) {
                    playing.fadeOut();
                    playing = null;
                }
            } else {
                lostFor = 0;
            }
            return;
        }
        if (boss == null || !mc.player.isAlive()) return;
        long seed = seed(boss);
        CompletableFuture<short[]> track = track(seed);
        if (!track.isDone()) return; // ещё собирается — начнём через тик-другой
        short[] pcm = track.getNow(null);
        if (pcm == null) return;
        mc.getMusicManager().stopPlaying();
        playing = new Track(pcm);
        lostFor = 0;
        mc.getSoundManager().play(playing);
        LitRPG.LOGGER.info("Boss music started for {} (seed {})", boss.getType(), seed);
    }

    private static LivingEntity nearestBoss(Minecraft mc, double r) {
        LivingEntity best = null;
        double bd = r * r;
        for (Entity en : mc.level.entitiesForRendering()) {
            if (!(en instanceof LivingEntity le) || !le.isAlive() || !(en instanceof LitBoss || en instanceof WitherBoss)) continue;
            double d = en.distanceToSqr(mc.player);
            if (d < bd) {
                bd = d;
                best = le;
            }
        }
        return best;
    }

    private static long seed(Entity boss) {
        var key = ForgeRegistries.ENTITY_TYPES.getKey(boss.getType());
        return key == null ? 0 : key.toString().hashCode() * 0x9E3779B97F4A7C15L;
    }

    private static synchronized CompletableFuture<short[]> track(long seed) {
        CompletableFuture<short[]> f = TRACKS.get(seed);
        if (f == null) {
            if (TRACKS.size() >= 3) TRACKS.remove(TRACKS.keySet().iterator().next());
            f = CompletableFuture.supplyAsync(() -> PhonkSynth.render(seed));
            TRACKS.put(seed, f);
        }
        return f;
    }

    /** Звук, который играет готовую петлю из памяти. */
    private static final class Track extends AbstractTickableSoundInstance {
        private static final SoundEvent EVENT = SoundEvent.createVariableRangeEvent(LitRPG.id("music.boss_phonk"));
        private final short[] pcm;
        private float target = 1f;
        private int fadeIn;
        boolean started;

        Track(short[] pcm) {
            super(EVENT, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.pcm = pcm;
            this.looping = false;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
            this.volume = 0.01f;
        }

        @Override
        public WeighedSoundEvents resolve(SoundManager manager) {
            // звука нет в sounds.json — описываем его сами: поток, который мы же и наполняем
            this.sound = new Sound(location.toString(), ConstantFloat.of(1f), ConstantFloat.of(1f), 1, Sound.Type.FILE, true, false, 16);
            WeighedSoundEvents w = new WeighedSoundEvents(location, null);
            w.addSound(sound);
            return w;
        }

        @Override
        public CompletableFuture<AudioStream> getStream(SoundBufferLibrary lib, Sound s, boolean loop) {
            started = true;
            LitRPG.LOGGER.debug("Boss music stream opened");
            return CompletableFuture.completedFuture(new Loop(pcm));
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        void fadeOut() {
            target = 0f;
        }

        @Override
        public void tick() {
            // вступление за 1.5 с, затихание за 2 с
            if (target > 0) {
                fadeIn = Math.min(30, fadeIn + 1);
                volume = Math.max(0.01f, fadeIn / 30f);
            } else {
                volume -= 0.025f;
                if (volume <= 0.01f) stop();
            }
        }
    }

    /** Трек из файла: поток по кругу, плавное вступление и затихание. */
    private static final class FileTrack extends AbstractTickableSoundInstance {
        private float target = 1f;
        int ticks;

        FileTrack(SoundEvent ev) {
            super(ev, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.relative = true;
            this.attenuation = Attenuation.NONE;
            this.volume = 0.01f;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        void fadeOut() {
            target = 0f;
        }

        @Override
        public void tick() {
            ticks++;
            if (target > 0) volume = Math.min(1f, Math.max(0.01f, ticks / 30f));
            else {
                volume -= 0.025f;
                if (volume <= 0.01f) stop();
            }
        }
    }

    /** Бесконечная петля PCM (стерео, 16 бит). */
    private static final class Loop implements AudioStream {
        private static final AudioFormat FORMAT = new AudioFormat(PhonkSynth.RATE, 16, 2, true, false);
        private final short[] pcm;
        private int pos, reads;

        Loop(short[] pcm) {
            this.pcm = pcm;
        }

        @Override
        public AudioFormat getFormat() {
            return FORMAT;
        }

        @Override
        public ByteBuffer read(int size) {
            int frames = Math.max(1, size / 4);
            ByteBuffer buf = BufferUtils.createByteBuffer(frames * 4).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < frames * 2; i++) {
                buf.putShort(pcm[pos]);
                if (++pos >= pcm.length) pos = 0;
            }
            buf.flip();
            if (reads++ == 2) LitRPG.LOGGER.info("Boss music streaming ({} bytes per buffer)", frames * 4);
            return buf;
        }

        @Override
        public void close() {}
    }
}
