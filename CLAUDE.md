# LitRPG: Система — памятка для Claude

Мод для Minecraft **1.20.1 Forge 47.3.0**, Java 17. Владелец — Андрей (GitHub `Naholde1/litrpg-mod`),
играет через **TLauncher**, общается по-русски, в программировании не разбирается.

## Как работать с владельцем
- Делать **масштабно, качественно и красиво**: детальные текстуры, продуманные механики, оптимизация.
  Не скупиться на объём, но держать баланс. Вопросы — только по-настоящему важные, в начале.
- Все тексты в игре — **на русском** (en_us.json можно оставлять частично русским).
- Предлагать свои идеи в духе литРПГ-книг; ориентир — анонсы Hytale.
- После каждого этапа: собрать, проверить в dev-клиенте, установить jar в TLauncher, закоммитить.
- Коммитить в ветку `develop` (git user: `Naholde1` / `arsentevandrej389@gmail.com`). Владелец просил **бэкапы в GitHub**:
  после коммита делать `git push origin develop` (доступ работает с 20.10.2026). Если push снова не пройдёт —
  класть `git bundle` в `C:\Users\arsen\litrpg-backups` и сказать владельцу.
  В конце сообщения коммита — строка Co-Authored-By от системы.
- Отчёт в конце — по-русски, коротко и по пунктам, что сделано и как это проверить командами.

## Передача работы
`HANDOVER.md` в корне — что сделано, что осталось (баги с указанием файла и строки, обещанное
и не сделанное, таблица 27 классов), как поднять проект на другом ПК и как писать способности
со своей механикой. Обновлять этот файл после каждого крупного этапа.

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

## Суть игры (3.0)
Вся игра — **прохождение подземелья** группой 3–5 игроков: Преддверие → 5 этапов по 10 уровней (1–50) → титры.
Нет открытого мира, рас, профессий, строительства и выживания. Старый мир (2.x) — тег `v2.17.1` в git.
Решения владельца по 3.0 — в памяти Claude (`dungeon-rework-3.0`): личная добыча, вайп = этап заново (вещи остаются),
павший — дух до воскрешения, класс на 10 ур. (50 классов × 16 навыков), мана у всех, зелья слабее лекаря, растущее неистовство.

## Устройство кода (`src/main/java/com/andrey/litrpg`)
- `LitRPG` — точка входа, реестры. `registry/` — `ModItems`, `ModBlocks` (RELIC, RUNE_DIAL, MIRROR, CACHE, RIFT), `ModEntities`
  (WILD — бестиарий, BOSSES — хранители), `ModWorldgen` (плитки и `StageBiomeSource`), `ModSounds` (музыка), `ModEffects`.
- **`run/`** — прохождение: `Layout` (чтение `/litrpg_dungeon.json`: этапы, отряды, хранители, преграды, механизмы, тайники),
  `RunState` (SavedData `litrpg_run`: этап 0 — Преддверие, 1–5, 6 — пройдено), `Run` (старт с печати, отряды по близости, привязка,
  хранители, вайп, выход, спасение из пропасти и лавы `solidNear`, правила мира), `Spirits` (духи и воскрешение), `Rewards` (точный опыт
  этапа: 50% отряды, 45% хранители, 5% загадки; `claims`), `RunLoot` (личная добыча; трофеи хранителя — шаблоны с `boss` = id и `roles`),
  `Mechanisms` (загадки и ловушки: plates, sequence, dials, beam, bells, memory, riddle, split, braziers, switch, area, clear, valves,
  current, portal, spikes, flames, crusher, blades, arrows, collapse, gas, rhythm), `Minibosses`, `Protect` (ломать можно только тег
  `litrpg:breakable`), `Shop`/`TraderEntity`/`Wallet` (торговец привала, смена класса), `Lore`, `Credits`.
- `world/` — `TilePlacement`/`TileStructure`/`TilePiece` (плитки 32×32 на фиксированных чанках), `StageBiomeSource` (биом по этапу).
  Измерение `litrpg:dungeon` (пустота, свет 0.12).
- `entity/` — `BossBase` (навыки с подготовкой, разметка `Telegraph`, узлы силы, щит, оглушение, неистовство, арена `setArena`/`point`/
  `points`, `partySize` → `Run.hpScale`), `BossDamage` (урон в долях «единицы»), `BossProjectile`, `Telegraph` (круг/кольцо/конус/полоса,
  `soak`, `follow`, `spare`), `wild/` (45 видов бестиария), `Creatures`.
  **`entity/stage/`** — 15 хранителей: `StageBoss` (ehp, `moveGroup` — смена локации, `hazard` — лужи, `adds`), `Bosses` (реестр и модели).
  Этап 1: `GatekeeperEntity`, `TwinNecromancerEntity` (Морра/Весса), `LichKingEntity`. Этап 2: `SpiderQueenEntity`, `RotEntEntity`,
  `DarkWitchEntity` (Изнанка). Этап 3: `TideWardenEntity` (волна-укрытие, затопление), `LeviathanEntity` (нырки, отлив/прилив),
  `DeepPriestessEntity` (купол на дне). Этап 4: `SmelterGolemEntity` (жар и закалка), `FireTwinEntity` (Игнис/Синдер: нить, слияние,
  тление), `AshLordEntity` (обвал пола, кальдера). Этап 5: `ChronophageEntity` (откат и песочные часы), `WorldEaterEntity` (осколки
  льда и огня, голод), `XalatharEntity` (крах края, Сердце Бездны, «Конец всего»).
- `ability/` — `Ability` (навыки из `litrpg_classes.json`), `AbilityHandler` (`tryUse` → `Casting` → исполнение; общий откат),
  `Dynamic` (прицельное/читаемое лечение, HoT, цепное; самонаводящиеся и читаемые снаряды, DoT), `SkillFx`, `SkillEffects`, `Minions`.
- `stats/` — `PlayerStats` (MAX_LEVEL 50, роль, класс, панель 8 + рывок, `claims`), `StatsManager` (опыт, выбор роли со стартовым
  набором, классы, `resetClass`), `Role`, `PlayerClass`, `ClassOffers` (стиль 0.7 + очки 0.3), `Style`.
- `gear/` — `GearData`, `Rarity`, `GearGen`, `ItemTemplates` (`/litrpg_items.json`), `GearEvents`, `SellPrices`.
- `client/` — `HudOverlay` (рамки группы, панель 8 + рывок, полоса чтения), `ShopScreen`, `OriginScreen` (роль), `ClassChoiceScreen`,
  `CharacterScreen`, `ClientHooks` (отложенное окно роли при входе), `fx/FxRenderer` (геометрия эффектов и разметки), `music/`
  (`Tracks` — трек хранителя, `BossMusic`, `PhonkSynth`), `Minimap`.
- `network/` — пакеты (`ModNetwork.VERSION` поднимать при изменениях), `fx/Fx` — серверная отправка эффектов.

## Генераторы (`tools/`) — данные и картинки не править руками, а перезапускать скрипт
- **Подземелье**: `gen_dungeon.py [--only 1,2] [--preview dir]` → плитки `structures/dungeon/*.nbt` + `litrpg_dungeon.json`.
  Строитель — `tools/dungeon/build.py` (Map: carve/room/round_room/cave/corridor/pit, `shell` — стены по темам, `auto_lights`, `glow`;
  Stage: pack/boss/gate/mech/lore). Карты — `dungeon/lobby.py`, `stage1.py` … `stage5.py` (OX = 1024·n).
  **После правок карты — `python tools/check_dungeon.py`**: схема (преграды, засады, типы мобов) и проходимость пешком от привала
  до всех хранителей/тайников/рычагов/выхода, плюс проверка обхода дверей. Должно быть «Всё в порядке».
- `gen_dimension.py` — измерение, биомы этапов (туман, вода, частицы, звуки). `gen_dungeon_blocks.py` — текстуры блоков подземелья.
- `gen_stage_bosses.py [--only id] [--preview png]` — модели 15 хранителей (каркас `gen_bosses.py`) + названия и яйца.
- **`gen_classes.py`** — 50 классов × 16 навыков: данные `tools/classes/*.py`, слияние пар и динамические навыки — `classes/v3.py`
  → `litrpg_classes.json`, lang. Названия навыков внутри класса не должны повторяться. После — `gen_codex.py`.
- `gen_gear_content.py` — шаблонные предметы (украшения, оружие, броня, **трофеи хранителей `BOSS_LOOT`**: 4 на хранителя, `roles`).
- `gen_codex.py` — Кодекс (Patchouli): начало, подземелье, этапы и хранители (`BOSS_GUIDE`), классы из json, бестиарий по этапам.
- `gen_bestiary.py` + `bestiary_data.py` — 45 существ. `gen_music.py` — музыка (ffmpeg, громкость −16 LUFS) → `sounds/music/*.ogg`
  (**папка в .gitignore** — треки владельца не публикуются; на GitHub их нет, в jar — есть).
- `--preview build/preview/x.png` у генераторов картинок — посмотреть результат через Read.

## Проверка в игре (без участия владельца)
- Новый мир: `bash build/preview/newdungeon.sh` (сервер генерирует `run/dungeontest` → копия в `run/saves`), затем
  `.\gradlew.bat runClient -Pquickplay=dungeontest`. После изменения карты — обязательно новый мир (плитки ставятся при генерации чанков).
- Dev-команды: `run/dev_commands.txt` (сервер выполняет раз в секунду; лог — `run/logs/latest.log`), `run/dev_screenshot.txt` с именем →
  `run/screenshots/`, `run/dev_open.txt` (`close`). Особые: `devboss` (состояние хранителей), `devuse x y z`, `devbreak x y z`, `devmobs`.
- В одиночку: `litrpg run minplayers 1`, `litrpg role Dev <tank|damage|support>`, `litrpg level set Dev N`, `litrpg run stage N`,
  `effect give Dev minecraft:resistance 3000 4 true` (неуязвимость), фаза хранителя — `execute in litrpg:dungeon as @e[type=litrpg:ID]
  run data merge entity @s {Health:N}`. Телепорт — только `execute in litrpg:dungeon run tp …`. `/kill` обходит механику гибели.
- Хранитель отмечается побеждённым при смерти сущности типа из схемы (у близнецов — первая сестра/Игнис; вторая умирает вместе).
- Перед новым запуском — убить java-процессы с `litrpg-mod` в командной строке.

## Сделано
2.x (до тега `v2.17.1`): открытый Мир Системы, расы, города, профессии, квесты, рейды — удалено в 3.0.
**3.0.0** — подземелье: Преддверие и 5 этапов (Склеп Забытых 1–10, Паучья чаща 10–20, Затопленный храм 20–30, Кузни Пепла 30–40,
Шпиль Бездны 40–50), 15 хранителей со сменой локаций, загадки/ловушки/тайники/мини-хранители, торговец привала, личная добыча и
трофеи хранителей под роль, духи и вайпы, 50 классов × 16 навыков с прицельным лечением и читаемыми чарами, рамки группы, музыка
владельца у хранителей, титры. Проверка проходимости — `tools/check_dungeon.py`.
Дальше: баланс цифр по отзывам группы, «героическая» сложность уже одна — подкручивать ehp/урон в `entity/stage/*`.
