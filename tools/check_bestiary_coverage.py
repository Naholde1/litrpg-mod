import sys
sys.path.insert(0, 'tools')
from bestiary_data import SPECIES, EXTERNAL, BANDS
rows = [dict(id=s['id'], level=s['level'], lands=s['lands'], time=s['time'], place=s['place']) for s in SPECIES] + EXTERNAL
lands = ['kingdom', 'elves', 'dwarves', 'orcs', 'beastkin', 'demonkin', 'goblins', 'frost', 'deadlands', 'wild', 'nether']
rng = {'kingdom': (1, 38), 'elves': (8, 38), 'dwarves': (10, 44), 'orcs': (12, 50), 'beastkin': (9, 42), 'demonkin': (22, 58),
       'goblins': (14, 48), 'frost': (24, 55), 'deadlands': (22, 56), 'wild': (15, 60), 'nether': (26, 60)}
van = {'kingdom': ['zombie', 'skeleton', 'spider', 'creeper', 'witch', 'enderman'], }
for land in lands:
    for when in ('day', 'night'):
        gaps = []
        for L in range(rng[land][0], rng[land][1] + 1):
            ok = [r['id'] for r in rows if (land in r['lands'] or 'any' in r['lands']) and r['place'] != 'under'
                  and abs(r['level'] - L) <= 5 and (r['time'] == 'any' or r['time'] == when)]
            if len(ok) < 2:
                gaps.append((L, len(ok)))
        print(land, when, 'thin:', gaps[:40])
