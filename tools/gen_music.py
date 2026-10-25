"""
Музыка Андрея → assets/litrpg/sounds/music/*.ogg с выровненной громкостью (EBU R128, два прохода loudnorm)
и sounds.json. Треки — личные файлы владельца, в git не попадают (.gitignore), в jar — попадают.
Запуск: python tools/gen_music.py [папка-с-mp3]   (по умолчанию C:/Users/arsen/Downloads)
"""
import json
import os
import re
import subprocess
import sys

FFMPEG = r'C:\Users\arsen\.litrpg-tools\ffmpeg_dl\ffmpeg-master-latest-win64-gpl\bin\ffmpeg.exe'
ASSETS = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'litrpg')
OUT = os.path.join(ASSETS, 'sounds', 'music')

# имя файла (начало) → имя звука
TRACKS = {
    'Vessel_-_Red_Sex': 'red',
    'Qura_-_Jane_Hoodtrap': 'jane_hoodtrap',
    'blud (Ultra Slowed)': 'blud',
    'Dj Univxrsel Kurayami': 'kurayami',
    'Tys_Music_-_As_Time_Flies': 'as_time_flies',
    'wutiwant': 'wutiwant',
    'ugolki_soznaniya': 'next_opponent',
    'Hans_Zimmer': 'no_time_for_caution',
    'Ennio_Morricone': 'ecstasy_of_gold',
    'Eru Flow Flame': 'aura_of_glory',
    'Fight Club Soundtrack': 'fight_club',
    'SLXUGHTER_-_fragment': 'fragment',
    'Bountyhunter_-_Woops': 'woops',
    'Hensonn_-_Fear': 'fear',
    'NUEKI_TOLCHONOV': 'blue_lock',
}
# громкость: -16 LUFS (как музыка игр), пики не выше -1.5 dBTP
TARGET = 'I=-16:TP=-1.5:LRA=11'


def measure(path):
    r = subprocess.run([FFMPEG, '-hide_banner', '-i', path, '-af', f'loudnorm={TARGET}:print_format=json', '-f', 'null', '-'],
                       capture_output=True, text=True, encoding='utf-8', errors='replace')
    m = re.search(r'\{[^{}]*"input_i"[^{}]*\}', r.stderr, re.S)
    return json.loads(m.group(0)) if m else None


def convert(src, dst):
    meas = measure(src)
    if meas:
        af = (f'loudnorm={TARGET}:measured_I={meas["input_i"]}:measured_TP={meas["input_tp"]}:measured_LRA={meas["input_lra"]}'
              f':measured_thresh={meas["input_thresh"]}:offset={meas["target_offset"]}:linear=true')
    else:
        af = f'loudnorm={TARGET}'
    subprocess.run([FFMPEG, '-hide_banner', '-loglevel', 'error', '-y', '-i', src, '-vn', '-af', af, '-ar', '44100', '-ac', '2',
                    '-c:a', 'libvorbis', '-q:a', '4', dst], check=True)
    return meas


def main():
    src_dir = sys.argv[1] if len(sys.argv) > 1 else r'C:\Users\arsen\Downloads'
    os.makedirs(OUT, exist_ok=True)
    files = [f for f in os.listdir(src_dir) if f.lower().endswith('.mp3')]
    sounds = {}
    for prefix, name in TRACKS.items():
        match = [f for f in files if f.startswith(prefix)]
        if not match:
            print('нет файла для', prefix)
            continue
        dst = os.path.join(OUT, name + '.ogg')
        meas = convert(os.path.join(src_dir, match[0]), dst)
        print(f'{name}: {match[0]} (было {meas["input_i"] if meas else "?"} LUFS) -> {os.path.getsize(dst) // 1024} КБ')
        sounds['music.' + name] = {'category': 'music', 'sounds': [{'name': 'litrpg:music/' + name, 'stream': True}]}
    path = os.path.join(ASSETS, 'sounds.json')
    old = json.load(open(path, encoding='utf-8')) if os.path.exists(path) else {}
    old.update(sounds)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(old, f, ensure_ascii=False, indent=1)
    print('sounds.json:', len(old), 'звуков')


if __name__ == '__main__':
    main()
