# Changelog

## Current Branch

### Added
- Added a typed magic energy system with Arcane, Fire, Water, Earth, Life, and Storm energy.
- Added magic accumulator crystal blocks for each energy type.
  - Accumulators harvest energy from nearby thematic blocks.
  - Accumulators store their own energy type and output through adjacent receivers or linked receivers.
  - Output is split across all valid receivers from one shared per-tick budget.
- Added a multiblock Magic Battery.
  - Stores all magic energy types.
  - Connected battery blocks consolidate into one shared storage.
  - Capacity scales per block.
- Added a Magic Linker item.
  - Click an accumulator to set it as a source.
  - Click a receiver to toggle a link.
  - Sneak-click an accumulator to cycle transfer mode.
- Added a Magic Linker HUD.
  - Shows accumulator storage, output, mode, and link count.
  - Shows Magic Battery multiblock size, capacity, and stored energy by type.
  - Shows Magic Flow Converter role and linked ward summary.
- Added mana-sight-only transfer particles.
  - Visible to admins, creative players, and spectators for now.
  - Particle thickness scales with transferred energy.
- Added a Grimoire spell viewer with search.
  - Right-click opens the spell index.
  - Sneak-right-click cycles the selected spell.
  - Shows spell details, payloads, shape, mana, cooldown, and recipe requirements.
- Added Warlock Hunt events.
  - Eligible players can be hunted by warlock groups.
  - Hunts spawn near the player and immediately target them.
  - Hunt size scales with difficulty.
  - Hunts are throttled to roughly hourly pressure.
- Added a custom warlock texture.

### Changed
- Magic Flow Converter now accepts Arcane magic energy in addition to RF.
  - Arcane ME converts into ward MF.
  - Existing RF to MF behavior remains.
- Spellcraft now supports selecting and crafting multi-payload spells up to the registry-supported limit.
- Arcane Knowledge books now act as real recipe knowledge sources.
  - Exact known recipes can unlock crafting.
  - Payload knowledge can contribute to combined spell recipes.
  - Arcane Knowledge is consumed when used.
- Grimoire knowledge now spends grimoire magic charge when used for spellcraft or when copying a grimoire spell into a wand.
- Arcane Knowledge books now reliably fill their intended spell count.
- Arcane Knowledge tooltips are compact by default and detailed while holding Shift.
  - Default view shows stored spell recipes.
  - Shift view shows full ingredient requirements.
- Warlock AI was expanded.
  - Warlocks can roll support spells such as heal and regenerate.
  - Support warlocks heal injured hostile mobs and other warlocks.
  - Warlocks exclude player-owned summons from ally healing.
  - Warlocks target players and player-owned summons.
  - Revenge targeting is restored.
  - Warlocks can roll shield and dodge incoming projectiles.

### Fixed
- Fixed Magic Linker interactions being swallowed by accumulator, battery, and converter block right-click handlers.
- Fixed Arcane Knowledge tooltip ingredient truncation for multi-payload spells.
- Fixed spellcraft not recognizing all payload knowledge from multi-spell Arcane Knowledge books.
- Fixed multi-payload spellcraft being blocked by wand spell level.
- Fixed accumulator output multiplying per linked target.
- Fixed grimoire charge not being consumed consistently when used as spellcraft knowledge.

### Assets and Data
- Added blockstates, block models, item models, recipes, and language entries for:
  - Arcane Accumulator
  - Fire Accumulator
  - Water Accumulator
  - Earth Accumulator
  - Life Accumulator
  - Storm Accumulator
  - Magic Battery
  - Magic Linker
- Added `assets/ourmagic/textures/entity/warlock.png`.

### Verification
- `.\gradlew.bat build` passes.
