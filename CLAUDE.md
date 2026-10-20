# LitRPG: Система — памятка для Claude

Мод для Minecraft **1.20.1 Forge 47.3.0**, Java 17. Владелец — Андрей (GitHub `Naholde1/litrpg-mod`),
играет через **TLauncher**, общается по-русски, в программировании не разбирается.

## Как работать с владельцем
- Делать **масштабно, качественно и красиво**: детальные текстуры, продуманные механики, оптимизация.
  Не скупиться на объём, но держать баланс. Вопросы — только по-настоящему важные, в начале.
- Все тексты в игре — **на русском** (en_us.json можно оставлять частично русским).
- Предлагать свои идеи в духе литРПГ-книг; ориентир — анонсы Hytale.
- После каждого этапа: собрать, проверить в dev-клиенте, установить jar в TLauncher, локально закоммитить.
- Коммитить в ветку `develop` (git user: `Naholde1` / `arsentevandrej389@gmail.com`). Владелец просил **бэкапы в GitHub**:
  после коммита пробовать `git push origin develop`. Сейчас на компьютере сохранён вход аккаунта `Naholde` без прав на
  `Naholde1/litrpg-mod` (403) — пока доступ не починен, дополнительно класть `git bundle` в `C:\Users\arsen\litrpg-backups`.
  В конце сообщения коммита — строка Co-Authored-By от системы.
- Отчёт в конце — по-русски, коротко и по пунктам, что сделано и как это проверить командами.

## Окружение
- Проект: `C:\Users\arsen\litrpg-mod` (короткий путь без кириллицы и OneDrive — так и оставить).
- JDK 17 и Gradle: `C:\Users\arsen\.litrpg-tools\` (`jdk-17.0.20.1+1`, `gradle-8.8`). В репо есть `gradlew`.
- Сборка (PowerShell): `$env:JAVA_HOME="C:\Users\arsen\.litrpg-tools\jdk-17.0.20.1+1"; .\gradlew.bat build --no-daemon`
  → `build/libs/litrpg-system-<версия>.jar` (версия в `build.gradle`).
- Установка: заменить `litrpg-system-*.jar` в `%APPDATA%\.minecraft\mods`. Там же нужны
  `curios-forge-5.14.1+1.20.1.jar`, `Patchouli-1.20.1-84-FORGE.jar`, `jei-1.20.1-forge-15.20.0.106.jar`.
- Python 3.10 без PIL — PNG пишем сами (`tools/png.py`).
- Ловушки: bash в этом окружении ломается на **длинных многострочных heredoc** с python-кодом — такие правки
  класть в файл (`build/preview/*.py`) и запускать. Пути длиннее ~260 символов ломают git/python.

## Устройство кода (`src/main/java/com/andrey/litrpg`)
- `LitRPG` — точка входа, реестры, лут-модификатор. `registry/` — предметы, сущности, вкладка.
- `stats/` — данные игрока (`PlayerStats` в capability `StatsProvider`, резервная копия в persisted NBT),
  `StatsManager` (атрибуты, опыт, уровень, титулы, выбор расы/класса, синхронизация), `Race`, `PlayerClass`
  (31 класс, стили, скрытые), `Style` (стиль игры до 10 ур.), `ClassOffers`, `Title`, `Profession`, `Stat`.
- `ability/` — `Ability` (140 навыков данными: вид + параметры), `AbilityHandler` (исполнение по видам),
  `Minions` (призыв), `Stealth`.
- `event/` — `ServerEvents` (вход, смерть/клон, тик, опыт), `CombatEvents` (разница уровней, криты, уклонение,
  заряды при ударе, дроп), `MobLevels` (уровни/ранги/свойства мобов), `ProfessionEvents`, `StyleEvents`.
- `gear/` — предметы Системы: `GearData` (NBT `litrpg_gear`), `Rarity`, `GearBonus`, `Passive`, `GearSet`
  (3 старых набора), `ItemTemplates` (читает `/litrpg_items.json`), `TemplateItem`, `GearGen` (генерация),
  `LootRolls` (дроп), `ChestLootModifier`, `GearTrades`, `GearEvents` (пересчёт надетого, пассивки,
  способности Shift+ПКМ, наковальня), `Families` (бонусы против семейств мобов), `PlayerGear`.
- `item/` — классы предметов, `TemplateItems` (фабрика предметов по шаблонам, материалы брони из данных).
- `npc/` — жители рас: `NpcType` (раса × роль; роль ARTISAN — мастера), `Craft` (20 ремёсел: фразы, звуки работы),
  `RaceNpcEntity` (ИИ, реплики-облачка, торговля, скупка Shift+ПКМ, у мастера — доска поручений + «Торговать»),
  `NpcTrades` (товары народов и ремёсел), `NpcRegistry`.
- `rep/` — `Faction` (11 народов, стартовые значения по расе), `RepEvents` (изменения и последствия).
- `party/` — `Party`, `PartyCommand` (/party), `DamageTracker` (опыт по вкладу в урон).
- `world/` — `Zone` (уровни: в Мире Системы от городов, иначе кольца), `BloodMoon`, `Dungeon`.
- `world/map/` — **Мир Системы** (тип мира `litrpg:system_world`): `WorldMap` (читает `/litrpg_world.json`, сетка высот/земель,
  города, уровни; математика 1:1 с `tools/worldmath.py`), `MapShape` (density function), `TerritoryBiomeSource`,
  `MapTilePlacement`/`MapTileStructure`/`MapTilePiece` (постройки-плитки ≤128×128 на фиксированных местах; картины —
  через маркеры данных `painting <вариант> <сторона>`), `RoadFeature`, `SystemWorld` (граница 9500, спавн). Реестры — `registry/ModWorldgen`.
- `city/` — `CityLaw` (город ломается: блоки без добычи, −репутация, розыск за упорство; население и найм мастеров),
  `CityRepairs` (SavedData повреждений; строители чинят), `Speech` (облачка TextDisplay, имена), `Gossip` (слухи).
- `quest/CityQuests` — поручения мастеров: доставка посылки (`ModItems.PARCEL`), крысы у мастерской (`entity/RatEntity`),
  разведение животных (BREED), поход к месту (VISIT), сбор, охота. Доски в `QuestLog.offers` с ключом `npc:<uuid>`.
- `entity/Creatures` — 7 монстров регионов (модели данными через `Animated` + `DataBossRenderer`). `command/LitRpgCommand` — все /litrpg команды.
- `entity/BossBase` — каркас боссов (навыки с подготовкой, фазы, сброс боя, рост здоровья под группу), `BossProjectile`,
  `entity/boss/` — 8 боссов подземелий. `block/BossAltar*` — Печать подземелья (призыв хранителя, откат 20 мин).
  `world/Dungeon` — 10 подземелий (уровень, босс, репутация). `quest/` — задания: `Story` (15 глав), `QuestGen`
  (поручения Системы и народов), `QuestEvents` (прогресс, награды), журнал `QuestLog` в `PlayerStats`.
- `client/` — экраны (`SystemScreen`, `OriginScreen`, `ClassChoiceScreen`, `CharacterScreen`), `HudOverlay`,
  `GearTooltip`, `RaceLook` (внешность рас), рендеры. `network/` — пакеты (`ModNetwork.VERSION` поднимать при изменениях).
- `util/` — `Scheduler`, `DevCommands` (только dev). `stats/StatBreakdown` — разбор статов (вкладка «Расчёт»).
- `client/AtlasScreen` — атлас всего мира (карта → Tab).

## Генераторы (`tools/`) — данные и картинки не править руками, а перезапускать скрипт
- `gen_item_textures.py` (+`gen_extra_textures.py`) — старые текстуры предметов и брони наборов.
- `gen_gear_content.py` — **все шаблонные предметы** (украшения, броня, оружие рас, жетоны, монеты): пишет
  `src/main/resources/litrpg_items.json`, текстуры, модели, названия, теги/слоты Curios, рецепты.
- `gen_lang.py` — названия/описания классов, навыков, стилей. `gen_codex.py` — книга Patchouli «Кодекс Системы».
- `gen_bosses.py` — модели боссов (JSON для `client/model/DataModel`) + текстуры и светящийся слой, `--preview`.
- `gen_dungeons.py` — 10 подземелий (.nbt + worldgen + сундуки). Тест: `/place template litrpg:dungeon_ruins x y z`.
- `gen_npc_skins.py` — скины жителей + модели яиц. `gen_structures.py` — поселения (.nbt + worldgen JSON).
- **Мир Системы**: `gen_world.py` (земли, биомы, города, реки, дороги, noise settings, preset → `litrpg_world.json`)
  → `gen_mapbuild.py` (города/поселения/чудеса/ад: `mapbuild_lib.py`, `mapbuild_cities.py`, `mapbuild_pois.py`; дописывает pois/tiles)
  → `gen_world.py --art` (атлас и картина-карта). `worldmath.py` должен совпадать с `WorldMap.java` (проверка: dev `devmap x z`).
- `gen_bosses.py` — модели боссов и монстров регионов (и крыс).
- `mapbuild_crafts.py` — мастерские (обстановка, вывески, задние дворы-загоны), прилавки, «мелочи жизни»; список
  мастерских городов `CITY_CRAFTS` → `litrpg_world.json` `workshops`. `connect_blocks` в `mapbuild_lib` соединяет заборы/панели.
- Животных в загонах не ставить рядом с блоками в 1 высоту (сено, котёл) — запрыгивают и уходят через забор.
- `--preview build/preview/x.png` у генераторов текстур — посмотреть результат через Read.

## Проверка в игре (без участия владельца)
- Клиент: `.\gradlew.bat runClient -Pquickplay=<мир>` (мир должен существовать в `run/saves`).
  Новый мир — сначала `runServer` (в `run/server.properties` level-name, RCON 25599 / `litrpgtest`,
  `tools/rcon.py "команда"`), потом скопировать `run/<мир>` в `run/saves`.
- Мир Системы для тестов: `run/server.properties` level-name=systemtest, level-type=litrpg\:system_world;
  `bash build/preview/newworld.sh` (сервер генерирует → копия в saves), затем `runClient -Pquickplay=systemtest`.
- В dev-режиме: команды из `run/dev_commands.txt` выполняет сервер раз в секунду (вывод — `run/logs/latest.log`;
  особые строки: `devmap x z`, `devrender`, `devdump`, `devbreak x y z`/`devuse x y z` — от лица Dev,
  `devtalk <ремесло> [trade]` — ПКМ по ближайшему мастеру, `devaccept <n>` — принять n-е поручение, `devsay` — реплики),
  `run/dev_screenshot.txt` с именем → скриншот в `run/screenshots/`, `run/dev_open.txt` (`char0`..`char4`, `close`)
  открывает окно персонажа (`atlas` — атлас). Смерть на экране смерти в dev-режиме автоматически пропускается.
- Тестовый персонаж — `Dev`. Окна Минкрафта через PrintWindow дают устаревший кадр — используй dev-скриншоты.
- Перед новым запуском — убить java-процессы с `litrpg-mod` в командной строке. Принудительное закрытие
  не сохраняет мир (данные откатятся к последнему автосохранению — это не баг).

## Сделано (2.6.0) и что дальше
Сделано: расы с внешностью, 31 класс с выбором на 10 ур., 140 навыков, профессии, титулы, уровни/ранги мобов,
зоны, Кровавая луна, экипировка 6 редкостей (200+ предметов, 14 наборов, Curios-слоты), монеты и торговля,
5 народов с поселениями/жителями/репутацией, группы и честный опыт, Кодекс, JEI.
2.4.0: квесты (сюжет + поручения), репутация за врагов народов, 10 подземелий и 8 новых боссов, Искра Божества.
2.5.0: Мир Системы 10000×10000 (9 земель, ~37 биомов, 10 городов с законом и стражей, деревни/лесопилки/рыбаки/караваны/
заставы/руины, 5 чудес, подземелья на карте, Инферналис в аду, Стена Мира), атлас, вкладка «Расчёт», монстры регионов.
2.6.0: живые города — 118 мастерских 20 ремёсел, мастера с торговлей и поручениями, крысы, посылки, разведение,
реплики жителей, разрушаемый город со строителями, загоны и огороды; авто-подтверждение «экспериментального» мира.
Дальше (идеи): распорядок дня жителей (ночью по домам), поручения в деревнях, экономика (цены от спроса), события у чудес.
