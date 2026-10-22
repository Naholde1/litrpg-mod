package com.andrey.litrpg.client.music;

import java.util.Random;

/**
 * Синтезатор дрифт-фонка для боя с боссом — музыка собирается прямо в игре, без файлов:
 * перегруженный 808 с глайдами, мелодия ковбелла (два квадрата 808-го ковбелла через
 * полосовой фильтр), клэп, трэп-хэты с роллами, тёмный пэд и «насос» от бочки.
 * У каждого босса своё зерно: тональность, темп, рифф и линия баса.
 * Результат — стерео 16 бит, 44100 Гц, бесшовная петля в 28 тактов (~50 с).
 */
public final class PhonkSynth {
    private PhonkSynth() {}

    public static final int RATE = 44100;
    private static final int BARS = 28;

    /** Рифф ковбелла на 2 такта по шестнадцатым: ступень лада (7 = октава), -1 — пауза. */
    private static final int[][] RIFFS = {
            {7, -1, 7, -1, 6, -1, 7, -1, 4, -1, 4, 3, -1, 4, -1, -1, 7, -1, 7, -1, 6, -1, 7, -1, 9, -1, 8, -1, 7, -1, 6, 4},
            {7, 7, -1, 7, 7, -1, 7, 8, 7, -1, 6, -1, 4, -1, 6, -1, 7, 7, -1, 7, 7, -1, 7, 8, 9, -1, 8, -1, 6, -1, 4, -1},
            {4, -1, 7, -1, 4, 7, -1, 6, -1, 4, -1, 3, 4, -1, -1, -1, 4, -1, 7, -1, 4, 7, -1, 8, -1, 7, -1, 6, 7, -1, 9, -1},
            {7, -1, -1, 7, -1, -1, 7, -1, 8, -1, -1, 7, -1, -1, 6, -1, 4, -1, -1, 4, -1, -1, 4, -1, 6, -1, 4, -1, 3, -1, 2, -1},
    };
    /** Ступени лада для баса по тактам четырёхтактового круга. */
    private static final int[][] PROGS = {{0, 0, 5, 4}, {0, 5, 3, 4}, {0, 3, 5, 6}, {0, 6, 5, 4}};
    /** Удары 808 в такте: шаг (из 16) и длина в шагах. */
    private static final int[][] BASS = {{0, 6}, {6, 4}, {10, 3}, {13, 3}};
    private static final int[] MINOR = {0, 2, 3, 5, 7, 8, 10};
    private static final int[] PHRYGIAN = {0, 1, 3, 5, 7, 8, 10};

    private static float[] L, R;
    private static float[] duck;

    /** Отрисовать петлю: чередующиеся L/R сэмплы. */
    public static synchronized short[] render(long seed) {
        Random rnd = new Random(seed);
        int bpm = 126 + rnd.nextInt(4) * 4;
        int root = 36 + rnd.nextInt(7);                 // C2..F#2 — нота 808
        int[] scale = rnd.nextBoolean() ? MINOR : PHRYGIAN;
        int[] riffA = RIFFS[rnd.nextInt(RIFFS.length)];
        int[] riffB = RIFFS[rnd.nextInt(RIFFS.length)];
        int[] prog = PROGS[rnd.nextInt(PROGS.length)];

        double step = 60.0 / bpm / 4;                    // шестнадцатая, с
        int n = (int) Math.round(BARS * 16 * step * RATE);
        L = new float[n];
        R = new float[n];
        duck = new float[n];
        java.util.Arrays.fill(duck, 1f);

        float[] cowL = new float[n], cowR = new float[n];
        float[] padL = new float[n], padR = new float[n];

        for (int bar = 0; bar < BARS; bar++) {
            // форма: 0–1 вступление, 2–9 дроп A, 10–17 дроп B, 18–19 провал, 20–27 дроп A'
            boolean intro = bar < 2, brk = bar == 18 || bar == 19;
            boolean dropB = bar >= 10 && bar < 18;
            int deg = prog[bar % 4];
            double barT = bar * 16 * step;

            // тёмный пэд: минорное трезвучие по ступени
            int chordRoot = root + 24 + note(scale, deg);
            int third = root + 24 + note(scale, deg + 2), fifth = root + 24 + note(scale, deg + 4);
            pad(padL, padR, barT, 16 * step, new int[]{chordRoot, third, fifth}, intro || brk ? 0.11f : 0.07f);

            // ковбелл
            int[] riff = dropB ? riffB : riffA;
            for (int s = 0; s < 16; s++) {
                int d = riff[(bar % 2) * 16 + s];
                if (d < 0) continue;
                int midi = root + 36 + note(scale, d);
                if (dropB && bar % 4 == 3 && s >= 12) midi += 12;
                float vel = (s % 4 == 0 ? 1f : 0.8f) * (intro ? 0.55f : 1f);
                float pan = ((s * 5 + bar) % 3 - 1) * 0.25f;
                cowbell(cowL, cowR, barT + s * step, midi, vel, pan);
                // дроп A': вторая партия октавой выше, тише
                if (bar >= 20 && s % 2 == 0) cowbell(cowL, cowR, barT + s * step + step * 0.5, midi + 12, vel * 0.35f, -pan);
            }
            if (intro) {
                if (bar == 1) riser(barT, 16 * step);
                continue;
            }

            // клэп на 2 и 4
            if (!brk) {
                clap(barT + 4 * step, 1f);
                clap(barT + 12 * step, 1f);
                if (bar % 4 == 3) clap(barT + 15 * step, 0.5f);
            }
            // хэты: восьмые, в дропе B — шестнадцатые, в конце круга — ролл
            if (!brk) {
                for (int s = 0; s < 16; s++) {
                    boolean on = s % 2 == 0 || dropB || (s % 4 == 3 && rnd.nextFloat() < 0.4f);
                    if (bar % 4 == 3 && s >= 12) {
                        int k = s == 12 || s == 14 ? 3 : 2;      // триоли и тридцать вторые
                        for (int j = 0; j < k; j++) hat(barT + (s + j / (double) k) * step, 0.55f + 0.15f * j, false);
                        continue;
                    }
                    if (on) hat(barT + s * step, s % 4 == 0 ? 0.9f : 0.6f, false);
                    if (s == 6 || s == 14) hat(barT + s * step, 0.35f, true);
                }
            }
            // 808: в провале — одна длинная нота с глайдом вниз
            if (brk) {
                if (bar == 18) bass808(barT, 28 * step, root + note(scale, deg), root + note(scale, deg) - 12, 1f);
                if (bar == 19) riser(barT, 16 * step);
                continue;
            }
            for (int[] b : BASS) {
                int s = b[0];
                int midi = root + note(scale, deg) + (s == 13 && bar % 2 == 1 ? 12 : 0);
                Integer glide = s == 10 && bar % 4 == 3 ? root + note(scale, prog[(bar + 1) % 4]) : null;
                bass808(barT + s * step, b[1] * step, midi, glide == null ? midi : glide, 1f);
                kick(barT + s * step, s == 0 ? 1f : 0.75f);
            }
        }

        // сведение: пэд и ковбелл «дышат» от бочки, ковбелл — с эхом и перегрузом
        echo(cowL, cowR, 3 * step, 0.33f);
        double lp = 0, rp = 0;
        for (int i = 0; i < n; i++) {
            float dk = duck[i];
            float cl = (float) Math.tanh(cowL[i] * 1.8) * 0.42f * (0.55f + 0.45f * dk);
            float cr = (float) Math.tanh(cowR[i] * 1.8) * 0.42f * (0.55f + 0.45f * dk);
            L[i] += cl + padL[i] * dk;
            R[i] += cr + padR[i] * dk;
        }
        short[] out = new short[n * 2];
        float peak = 0;
        for (int i = 0; i < n; i++) {
            L[i] = (float) Math.tanh(L[i] * 1.15);
            R[i] = (float) Math.tanh(R[i] * 1.15);
            peak = Math.max(peak, Math.max(Math.abs(L[i]), Math.abs(R[i])));
        }
        float g = peak > 0 ? 0.92f / peak : 1f;
        for (int i = 0; i < n; i++) {
            out[i * 2] = (short) Math.round(L[i] * g * 32767);
            out[i * 2 + 1] = (short) Math.round(R[i] * g * 32767);
        }
        L = R = duck = null;
        return out;
    }

    private static int note(int[] scale, int deg) {
        int oct = Math.floorDiv(deg, 7);
        return scale[Math.floorMod(deg, 7)] + 12 * oct;
    }

    private static double hz(double midi) {
        return 440.0 * Math.pow(2, (midi - 69) / 12.0);
    }

    private static int at(double t) {
        return (int) Math.round(t * RATE);
    }

    // ---------------- голоса ----------------

    /** 808: синус с «щелчком» по высоте, долгий хвост, перегруз; глайд к следующей ноте. */
    private static void bass808(double t0, double len, double midi, double glideTo, float vel) {
        int i0 = at(t0), m = at(len + 0.12);
        double phase = 0;
        for (int k = 0; k < m && i0 + k < L.length; k++) {
            double t = k / (double) RATE;
            double p = Math.min(1, t / Math.max(0.01, len));
            double note = midi + (glideTo - midi) * (p > 0.55 ? Math.min(1, (p - 0.55) / 0.3) : 0);
            double f = hz(note) * (1 + 1.8 * Math.exp(-t * 55));
            phase += 2 * Math.PI * f / RATE;
            double env = Math.min(1, t / 0.004) * Math.exp(-t * 1.1) * (t > len ? Math.exp(-(t - len) * 40) : 1);
            double s = Math.tanh(Math.sin(phase) * 2.6) * 0.62 * env * vel;
            L[i0 + k] += s;
            R[i0 + k] += s;
        }
    }

    /** Бочка: короткий удар, на время которого приглушаются пэд и ковбелл. */
    private static void kick(double t0, float vel) {
        int i0 = at(t0), m = at(0.32);
        double phase = 0;
        for (int k = 0; k < m && i0 + k < L.length; k++) {
            double t = k / (double) RATE;
            double f = 48 + 110 * Math.exp(-t * 38);
            phase += 2 * Math.PI * f / RATE;
            double s = Math.sin(phase) * Math.exp(-t * 9) * 0.45 * vel;
            if (k < 60) s += (Math.random() * 2 - 1) * 0.15 * (1 - k / 60.0) * vel;
            L[i0 + k] += s;
            R[i0 + k] += s;
            duck[i0 + k] = (float) Math.min(duck[i0 + k], 0.35 + 0.65 * Math.min(1, t / 0.28));
        }
    }

    /** Клэп: три коротких всплеска шума через полосовой фильтр и хвост. */
    private static void clap(double t0, float vel) {
        int i0 = at(t0), m = at(0.3);
        Biquad bp = Biquad.bandpass(1300, 1.1);
        Random r = new Random(i0);
        for (int k = 0; k < m && i0 + k < L.length; k++) {
            double t = k / (double) RATE;
            double env = t < 0.03 ? (Math.exp(-(t % 0.01) * 300)) : Math.exp(-(t - 0.03) * 16);
            double s = bp.f(r.nextDouble() * 2 - 1) * env * 0.9 * vel;
            s += Math.sin(2 * Math.PI * 190 * t) * Math.exp(-t * 30) * 0.15 * vel;
            L[i0 + k] += s * 0.95;
            R[i0 + k] += s;
        }
    }

    /** Хэт: шум через фильтр высоких частот, закрытый — коротко, открытый — длиннее. */
    private static void hat(double t0, float vel, boolean open) {
        int i0 = at(t0), m = at(open ? 0.22 : 0.05);
        Biquad hp = Biquad.highpass(7500, 0.8);
        Random r = new Random(i0 * 31L + 7);
        float pan = open ? -0.3f : 0.25f;
        for (int k = 0; k < m && i0 + k < L.length; k++) {
            double t = k / (double) RATE;
            double s = hp.f(r.nextDouble() * 2 - 1) * Math.exp(-t * (open ? 14 : 75)) * 0.22 * vel;
            L[i0 + k] += s * (1 - pan);
            R[i0 + k] += s * (1 + pan);
        }
    }

    /** Ковбелл 808: два квадрата (1 : 1.48) через полосовой фильтр, щелчок и затухание. */
    private static void cowbell(float[] cl, float[] cr, double t0, int midi, float vel, float pan) {
        int i0 = at(t0), m = at(0.32);
        double f1 = hz(midi), f2 = f1 * 1.48;
        Biquad bp = Biquad.bandpass(f1 * 2.2, 1.4);
        double p1 = 0, p2 = 0;
        for (int k = 0; k < m && i0 + k < cl.length; k++) {
            double t = k / (double) RATE;
            p1 += f1 / RATE;
            p2 += f2 / RATE;
            double sq = ((p1 % 1) < 0.5 ? 1 : -1) * 0.6 + ((p2 % 1) < 0.5 ? 1 : -1) * 0.4;
            double env = Math.min(1, t / 0.002) * (Math.exp(-t * 28) * 0.7 + Math.exp(-t * 7) * 0.3);
            double s = (bp.f(sq) * 1.6 + sq * 0.12) * env * vel * 0.5;
            cl[i0 + k] += s * (1 - pan);
            cr[i0 + k] += s * (1 + pan);
        }
    }

    /** Пэд: расстроенные пилы через фильтр низких частот, медленная атака. */
    private static void pad(float[] pl, float[] pr, double t0, double len, int[] notes, float vol) {
        int i0 = at(t0), m = at(len);
        Biquad lpL = Biquad.lowpass(900, 0.7), lpR = Biquad.lowpass(900, 0.7);
        double[] ph = new double[notes.length * 2];
        for (int k = 0; k < m && i0 + k < pl.length; k++) {
            double t = k / (double) RATE;
            double sl = 0, sr = 0;
            for (int j = 0; j < notes.length; j++) {
                double f = hz(notes[j]);
                ph[j * 2] += f * 1.004 / RATE;
                ph[j * 2 + 1] += f * 0.996 / RATE;
                sl += (ph[j * 2] % 1) * 2 - 1;
                sr += (ph[j * 2 + 1] % 1) * 2 - 1;
            }
            double env = Math.min(1, t / 0.25) * Math.min(1, (len - t) / 0.08);
            pl[i0 + k] += lpL.f(sl) * env * vol;
            pr[i0 + k] += lpR.f(sr) * env * vol;
        }
    }

    /** Нарастающий шум перед дропом. */
    private static void riser(double t0, double len) {
        int i0 = at(t0), m = at(len);
        Random r = new Random(i0);
        Biquad bp = Biquad.bandpass(800, 1.5);
        for (int k = 0; k < m && i0 + k < L.length; k++) {
            double p = k / (double) m;
            if (k % 256 == 0) bp = Biquad.bandpass(600 + 7000 * p * p, 1.5);
            double s = bp.f(r.nextDouble() * 2 - 1) * p * p * 0.35;
            L[i0 + k] += s;
            R[i0 + k] += s;
        }
    }

    /** Пинг-понг эхо с затуханием. */
    private static void echo(float[] l, float[] r, double delay, float fb) {
        int d = at(delay);
        for (int i = d; i < l.length; i++) {
            float a = l[i - d], b = r[i - d];
            l[i] += b * fb;
            r[i] += a * fb;
        }
    }

    /** Двухполюсный фильтр (RBJ). */
    private static final class Biquad {
        final double b0, b1, b2, a1, a2;
        double x1, x2, y1, y2;

        Biquad(double b0, double b1, double b2, double a0, double a1, double a2) {
            this.b0 = b0 / a0;
            this.b1 = b1 / a0;
            this.b2 = b2 / a0;
            this.a1 = a1 / a0;
            this.a2 = a2 / a0;
        }

        static Biquad bandpass(double f, double q) {
            double w = 2 * Math.PI * Math.min(f, RATE * 0.45) / RATE, al = Math.sin(w) / (2 * q);
            return new Biquad(al, 0, -al, 1 + al, -2 * Math.cos(w), 1 - al);
        }

        static Biquad lowpass(double f, double q) {
            double w = 2 * Math.PI * f / RATE, al = Math.sin(w) / (2 * q), c = Math.cos(w);
            return new Biquad((1 - c) / 2, 1 - c, (1 - c) / 2, 1 + al, -2 * c, 1 - al);
        }

        static Biquad highpass(double f, double q) {
            double w = 2 * Math.PI * f / RATE, al = Math.sin(w) / (2 * q), c = Math.cos(w);
            return new Biquad((1 + c) / 2, -(1 + c), (1 + c) / 2, 1 + al, -2 * c, 1 - al);
        }

        double f(double x) {
            double y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            x2 = x1;
            x1 = x;
            y2 = y1;
            y1 = y;
            return y;
        }
    }
}
