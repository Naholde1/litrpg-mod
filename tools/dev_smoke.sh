#!/bin/bash
# Прогон навыков класса в запущенном dev-клиенте (мир dungeontest): bash tools/dev_smoke.sh <роль> <класс> <ключ> ...
# Ставит класс, трёх зомби с NoAI перед игроком и по очереди жмёт devability. Ошибки — в run/logs/latest.log.
# Перед первым прогоном: echo "litrpg run minplayers 1" > run/dev_commands.txt
cd /c/Users/arsen/litrpg-mod
role=$1; cls=$2; shift 2
printf 'litrpg role Dev %s\nlitrpg level set Dev 40\nlitrpg class set Dev %s\neffect give Dev minecraft:resistance 3000 4 true\nexecute in litrpg:dungeon as Dev at @s run tp @s 0 64 8 180 15\n' $role $cls > run/dev_commands.txt
sleep 3
printf 'kill @e[type=minecraft:zombie]\nexecute at Dev run summon minecraft:zombie ~2 ~ ~-6 {NoAI:1b,Health:300f,Attributes:[{Name:"generic.max_health",Base:300d}]}\nexecute at Dev run summon minecraft:zombie ~-2 ~ ~-7 {NoAI:1b,Health:300f,Attributes:[{Name:"generic.max_health",Base:300d}]}\nexecute at Dev run summon minecraft:zombie ~0 ~ ~-9 {NoAI:1b,Health:300f,Attributes:[{Name:"generic.max_health",Base:300d}]}\n' > run/dev_commands.txt
sleep 2
for k in "$@"; do echo "devability $k" > run/dev_commands.txt; sleep 1.2; done
