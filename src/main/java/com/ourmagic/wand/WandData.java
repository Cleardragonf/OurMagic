package com.ourmagic.wand;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.SpellInstance;
import com.ourmagic.magic.SpellRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class WandData {
    public static final String TAG_TEMPLATE = "OurMagicTemplate";
    private static final String TAG_NAME = "OurMagicName";
    private static final String TAG_POWER = "OurMagicPower";
    private static final String TAG_SPELLS = "OurMagicSpells";
    private static final String TAG_ACTIVE = "OurMagicActive";
    private static final String TAG_ASSIGNED_SPELLS = "OurMagicAssignedSpells";
    private static final String TAG_COOLDOWN = "OurMagicCooldown";
    private static final String TAG_WAND_LEVEL = "OurMagicWandLevel";
    private static final String TAG_WAND_XP = "OurMagicWandXp";
    private static final String TAG_WAND_MODIFIERS = "OurMagicWandModifiers";
    private static final String TAG_SPELL_KEY = "Key";
    private static final String TAG_SPELL_NAME = "Name";
    private static final String TAG_SPELL_COST = "Cost";
    private static final String TAG_SPELL_COOLDOWN = "Cooldown";
    private static final String TAG_SPELL_COOLDOWN_UNTIL = "CooldownUntil";
    private static final String TAG_SPELL_LEVEL = "Level";
    private static final String TAG_SPELL_XP = "Xp";
    private static final String TAG_SPELL_POINTS = "AttributePoints";
    private static final String TAG_SPELL_UPGRADES = "Upgrades";
    private static final int MAX_SPELL_LEVEL = 100;
    private static final int MAX_WAND_MODIFIER_LEVEL = 25;
    private static final int ASSIGNED_SLOTS = 6;
    private static final String SHAPE_UPGRADE_PREFIX = "shape:";

    private final String template;
    private final String displayName;
    private final float power;
    private final List<WandSpellData> spells;
    private final Map<String, Integer> modifiers;
    private int wandLevel;
    private int wandXp;
    private int activeIndex;
    private final int[] assignedSpells;
    private long cooldownUntil;

    public WandData(String template, String displayName, float power, List<WandSpellData> spells, int activeIndex, long cooldownUntil) {
        this(template, displayName, power, spells, activeIndex, defaultAssignments(spells.size()), cooldownUntil, 1, 0, Map.of());
    }

    public WandData(String template, String displayName, float power, List<WandSpellData> spells, int activeIndex, long cooldownUntil, int wandLevel, int wandXp, Map<String, Integer> modifiers) {
        this(template, displayName, power, spells, activeIndex, defaultAssignments(spells.size()), cooldownUntil, wandLevel, wandXp, modifiers);
    }

    public WandData(String template, String displayName, float power, List<WandSpellData> spells, int activeIndex, int[] assignedSpells, long cooldownUntil, int wandLevel, int wandXp, Map<String, Integer> modifiers) {
        this.template = template;
        this.displayName = displayName;
        this.power = power;
        this.spells = new ArrayList<>(spells);
        this.wandLevel = Math.max(1, Math.min(MAX_SPELL_LEVEL, wandLevel));
        this.wandXp = Math.max(0, wandXp);
        this.modifiers = sanitizeModifiers(modifiers);
        this.activeIndex = Math.max(0, Math.min(activeIndex, Math.max(0, spells.size() - 1)));
        this.assignedSpells = normalizeAssignments(assignedSpells, spells.size());
        this.cooldownUntil = cooldownUntil;
        if (cooldownUntil > 0 && !this.spells.isEmpty()) {
            this.spells.set(this.activeIndex, this.spells.get(this.activeIndex).withCooldownUntil(cooldownUntil));
        }
    }

    public static WandData fromTemplate(WandTemplate template) {
        return new WandData(template.key(), template.displayName(), template.power(), template.spells(), 0, 0);
    }

    public static WandData combine(WandData primary, WandData secondary) {
        Map<String, WandSpellData> merged = new LinkedHashMap<>();
        for (WandSpellData spell : primary.spells) {
            merged.put(spell.key(), spell);
        }
        for (WandSpellData spell : secondary.spells) {
            merged.merge(spell.key(), spell, WandSpellData::merge);
        }

        List<WandSpellData> spells = new ArrayList<>(merged.values());
        float power = Math.min(2.0F, Math.max(primary.power, secondary.power) + Math.min(primary.power, secondary.power) * 0.10F);
        Map<String, Integer> modifiers = new LinkedHashMap<>(primary.modifiers);
        secondary.modifiers.forEach((key, level) -> modifiers.merge(key, level, Math::max));
        return new WandData(primary.template, "Combined Wand", power, spells, 0, 0, Math.max(primary.wandLevel, secondary.wandLevel), Math.max(primary.wandXp, secondary.wandXp), modifiers);
    }

    public static WandData read(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        String templateKey = tag.getString(TAG_TEMPLATE);
        WandTemplate fallback = WandTemplates.get(templateKey).orElseGet(() -> WandTemplates.get(WandTemplates.DEFAULT_TEMPLATE).orElseThrow());

        List<WandSpellData> spells = new ArrayList<>();
        ListTag spellTags = tag.getList(TAG_SPELLS, 8);
        if (!spellTags.isEmpty()) {
            for (int i = 0; i < spellTags.size(); i++) {
                spells.add(WandSpellData.fromLegacyKey(spellTags.getString(i)));
            }
        } else {
            ListTag compoundSpellTags = tag.getList(TAG_SPELLS, 10);
            for (int i = 0; i < compoundSpellTags.size(); i++) {
                CompoundTag spellTag = compoundSpellTags.getCompound(i);
                spells.add(WandSpellData.read(spellTag));
            }
        }
        if (spells.isEmpty()) {
            spells.addAll(fallback.spells());
        }

        Map<String, Integer> modifiers = new LinkedHashMap<>();
        CompoundTag modifierTags = tag.getCompound(TAG_WAND_MODIFIERS);
        for (String key : modifierTags.getAllKeys()) {
            int level = modifierTags.getInt(key);
            if (level > 0) {
                modifiers.put(key, Math.min(level, MAX_WAND_MODIFIER_LEVEL));
            }
        }

        return new WandData(
                tag.contains(TAG_TEMPLATE) ? templateKey : fallback.key(),
                tag.contains(TAG_NAME) ? tag.getString(TAG_NAME) : fallback.displayName(),
                tag.contains(TAG_POWER) ? tag.getFloat(TAG_POWER) : fallback.power(),
                spells,
                tag.getInt(TAG_ACTIVE),
                readAssignments(tag, spells.size()),
                tag.getLong(TAG_COOLDOWN),
                tag.contains(TAG_WAND_LEVEL) ? tag.getInt(TAG_WAND_LEVEL) : 1,
                tag.getInt(TAG_WAND_XP),
                modifiers
        );
    }

    public void save(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_TEMPLATE, template);
        tag.putString(TAG_NAME, displayName);
        tag.putFloat(TAG_POWER, power);
        tag.putInt(TAG_ACTIVE, activeIndex);
        ListTag assignedTags = new ListTag();
        for (int assignedSpell : assignedSpells) {
            assignedTags.add(net.minecraft.nbt.IntTag.valueOf(assignedSpell));
        }
        tag.put(TAG_ASSIGNED_SPELLS, assignedTags);
        tag.putLong(TAG_COOLDOWN, activeCooldownUntil());
        tag.putInt(TAG_WAND_LEVEL, wandLevel);
        tag.putInt(TAG_WAND_XP, wandXp);
        CompoundTag modifierTags = new CompoundTag();
        modifiers.forEach(modifierTags::putInt);
        tag.put(TAG_WAND_MODIFIERS, modifierTags);

        ListTag spellTags = new ListTag();
        for (WandSpellData spell : spells) {
            CompoundTag spellTag = new CompoundTag();
            spellTag.putString(TAG_SPELL_KEY, spell.key());
            spellTag.putString(TAG_SPELL_NAME, spell.displayName());
            spellTag.putInt(TAG_SPELL_COST, spell.manaCost());
            spellTag.putInt(TAG_SPELL_COOLDOWN, spell.cooldownTicks());
            spellTag.putLong(TAG_SPELL_COOLDOWN_UNTIL, spell.cooldownUntil());
            spellTag.putInt(TAG_SPELL_LEVEL, spell.level());
            spellTag.putInt(TAG_SPELL_XP, spell.xp());
            spellTag.putInt(TAG_SPELL_POINTS, spell.attributePoints());
            CompoundTag upgrades = new CompoundTag();
            upgrades.putInt("chaining", spell.chaining());
            upgrades.putInt("chaining_entities", spell.chainingEntities());
            upgrades.putInt("chaining_radius", spell.chainingRadius());
            upgrades.putInt("chaining_damage", spell.chainingDamage());
            upgrades.putInt("damage", spell.damage());
            upgrades.putInt("healing", spell.healing());
            upgrades.putInt("radius", spell.radius());
            upgrades.putInt("multistrike", spell.multistrike());
            upgrades.putInt("multistrike_casts", spell.multistrikeCasts());
            upgrades.putInt("range", spell.range());
            upgrades.putInt("duration", spell.duration());
            upgrades.putInt("summon_health", spell.summonHealth());
            upgrades.putInt("summon_defense", spell.summonDefense());
            upgrades.putInt("summon_armor", spell.summonArmor());
            upgrades.putInt("summon_attack", spell.summonAttack());
            upgrades.putInt("summon_speed", spell.summonSpeed());
            spellTag.put(TAG_SPELL_UPGRADES, upgrades);
            spellTags.add(spellTag);
        }
        tag.put(TAG_SPELLS, spellTags);
    }

    public String displayName() {
        return displayName;
    }

    public String template() {
        return template;
    }

    public float power() {
        return power;
    }

    public int wandLevel() {
        return wandLevel;
    }

    public int wandXp() {
        return wandXp;
    }

    public int wandXpToNextLevel() {
        if (wandLevel >= MAX_SPELL_LEVEL) {
            return 0;
        }
        return 80 + wandLevel * 20;
    }

    public int wandModifierSlots() {
        int milestones = Math.max(0, wandLevel / 5);
        return 1 + milestones * (milestones + 1) / 2;
    }

    public int usedWandModifierSlots() {
        return modifiers.size();
    }

    public Map<String, Integer> modifiers() {
        return Map.copyOf(modifiers);
    }

    public int modifierLevel(String key) {
        return modifiers.getOrDefault(key, 0);
    }

    public int activeIndex() {
        return activeIndex;
    }

    public List<Integer> assignedSpells() {
        return java.util.Arrays.stream(assignedSpells).boxed().toList();
    }

    public String activeSpell() {
        return activeSpellData().map(WandSpellData::key).orElse("");
    }

    public String activeSpellName() {
        return activeSpellData().map(WandSpellData::displayName).orElse("None");
    }

    public int activeManaCost() {
        int cost = activeSpellData().map(WandSpellData::manaCost).orElse(0);
        return Math.max(0, Math.round(cost * manaCostMultiplier()));
    }

    public int activeCooldownTicks() {
        int cooldown = activeSpellData().map(WandSpellData::cooldownTicks).orElse(0);
        return Math.max(1, Math.round(cooldown * cooldownMultiplier()));
    }

    public int activeSpellLevel() {
        return activeSpellData().map(WandSpellData::level).orElse(1);
    }

    public int activeSpellXp() {
        return activeSpellData().map(WandSpellData::xp).orElse(0);
    }

    public int activeSpellXpToNextLevel() {
        return activeSpellData().map(WandSpellData::xpToNextLevel).orElse(0);
    }

    public float activeDamageMultiplier() {
        return damageMultiplier(activeSpellLevel()) * damageMultiplier();
    }

    public float activeUtilityMultiplier() {
        return utilityMultiplier(activeSpellLevel());
    }

    public float manaCostMultiplier() {
        return Math.max(0.35F, 1.0F - modifierLevel(WandModifier.FOCUS.key()) * 0.06F);
    }

    public float cooldownMultiplier() {
        return Math.max(0.35F, 1.0F - modifierLevel(WandModifier.HASTE.key()) * 0.08F);
    }

    public float damageMultiplier() {
        return 1.0F + modifierLevel(WandModifier.POTENCY.key()) * 0.12F;
    }

    public float durationMultiplier() {
        return 1.0F + modifierLevel(WandModifier.HARDNESS.key()) * 0.10F;
    }

    public float radiusMultiplierFromWand() {
        return 1.0F + modifierLevel(WandModifier.ELASTICITY.key()) * 0.10F;
    }

    public float rangeMultiplierFromWand() {
        return 1.0F + modifierLevel(WandModifier.FOCUS.key()) * 0.05F;
    }

    public float xpMultiplier() {
        return 1.0F + modifierLevel(WandModifier.WISDOM.key()) * 0.15F;
    }

    public int activeUpgradeLevel(String upgrade) {
        return activeSpellData().map(spell -> spell.upgradeLevel(upgrade)).orElse(0);
    }

    public int addActiveSpellXp(int amount) {
        if (spells.isEmpty() || amount <= 0) {
            return 0;
        }

        WandSpellData current = spells.get(activeIndex);
        WandSpellData updated = current.addXp(amount);
        spells.set(activeIndex, updated);
        return Math.max(0, updated.level() - current.level());
    }

    public boolean upgradeSpell(int spellIndex, String upgrade) {
        if (spellIndex < 0 || spellIndex >= spells.size()) {
            return false;
        }

        WandSpellData current = spells.get(spellIndex);
        if (upgrade.startsWith(SHAPE_UPGRADE_PREFIX)) {
            WandSpellData retargeted = current.changeShape(upgrade.substring(SHAPE_UPGRADE_PREFIX.length()));
            if (retargeted.equals(current)) {
                return false;
            }
            spells.set(spellIndex, retargeted);
            return true;
        }

        Spell spell = SpellRegistry.get(current.key());
        if (spell == null || !spell.supportsUpgrade(upgrade)) {
            return false;
        }
        WandSpellData upgraded = current.upgrade(upgrade);
        if (upgraded.equals(current)) {
            return false;
        }
        spells.set(spellIndex, upgraded);
        return true;
    }

    public boolean canUpgradeSpell(int spellIndex, String upgrade) {
        if (spellIndex < 0 || spellIndex >= spells.size()) {
            return false;
        }

        WandSpellData current = spells.get(spellIndex);
        Spell spell = SpellRegistry.get(current.key());
        return spell != null
                && spell.supportsUpgrade(upgrade)
                && current.canUpgrade(upgrade);
    }

    public int addWandXp(int amount) {
        if (amount <= 0 || wandLevel >= MAX_SPELL_LEVEL) {
            return 0;
        }
        int oldLevel = wandLevel;
        wandXp += amount;
        while (wandLevel < MAX_SPELL_LEVEL && wandXp >= wandXpToNextLevel()) {
            wandXp -= wandXpToNextLevel();
            wandLevel++;
        }
        if (wandLevel >= MAX_SPELL_LEVEL) {
            wandXp = 0;
        }
        return wandLevel - oldLevel;
    }

    public boolean canAddWandModifier(String key) {
        if (WandModifier.byKey(key).isEmpty()) {
            return false;
        }

        int currentLevel = modifierLevel(key);
        if (currentLevel > 0) {
            return currentLevel < MAX_WAND_MODIFIER_LEVEL;
        }

        return usedWandModifierSlots() < wandModifierSlots();
    }

    public boolean addWandModifier(String key) {
        if (!canAddWandModifier(key)) {
            return false;
        }
        modifiers.merge(key, 1, Integer::sum);
        return true;
    }

    public boolean addSpell(String key) {
        Spell spell = SpellRegistry.get(key);
        if (spell == null || spells.stream().anyMatch(existing -> existing.key().equals(key))) {
            return false;
        }

        spells.add(new WandSpellData(key, spell.manaCost(), spell.cooldownTicks()));
        return true;
    }

    public boolean addRolledSpell(String key, RandomSource random) {
        return addSpell(SpellInstance.roll(key, random));
    }

    public boolean addSpell(SpellInstance instance) {
        if (SpellRegistry.get(instance.key()) == null || spells.stream().anyMatch(existing -> existing.key().equals(instance.key()))) {
            return false;
        }

        spells.add(instance.toWandSpellData());
        return true;
    }

    public List<WandSpellData> spells() {
        return List.copyOf(spells);
    }

    public boolean removeSpell(int spellIndex) {
        if (spellIndex < 0 || spellIndex >= spells.size() || spells.size() <= 1) {
            return false;
        }

        spells.remove(spellIndex);
        activeIndex = Math.max(0, Math.min(activeIndex >= spellIndex ? activeIndex - 1 : activeIndex, spells.size() - 1));
        for (int i = 0; i < assignedSpells.length; i++) {
            if (assignedSpells[i] == spellIndex) {
                assignedSpells[i] = activeIndex;
            } else if (assignedSpells[i] > spellIndex) {
                assignedSpells[i]--;
            }
            assignedSpells[i] = Math.max(0, Math.min(assignedSpells[i], spells.size() - 1));
        }
        return true;
    }

    public long cooldownUntil() {
        return activeCooldownUntil();
    }

    public void setCooldownUntil(long cooldownUntil) {
        setActiveCooldownUntil(cooldownUntil);
    }

    public long activeCooldownUntil() {
        return activeSpellData().map(WandSpellData::cooldownUntil).orElse(0L);
    }

    public void setActiveCooldownUntil(long cooldownUntil) {
        if (spells.isEmpty()) {
            this.cooldownUntil = cooldownUntil;
            return;
        }
        spells.set(activeIndex, spells.get(activeIndex).withCooldownUntil(cooldownUntil));
        this.cooldownUntil = cooldownUntil;
    }

    public void cycleSpell(int offset) {
        if (!spells.isEmpty()) {
            activeIndex = Math.floorMod(activeIndex + offset, spells.size());
        }
    }

    public boolean setActiveSpellIndex(int spellIndex) {
        if (spellIndex < 0 || spellIndex >= spells.size()) {
            return false;
        }
        activeIndex = spellIndex;
        return true;
    }

    public boolean selectAssignedSpell(int slot) {
        if (slot < 0 || slot >= ASSIGNED_SLOTS) {
            return false;
        }
        return setActiveSpellIndex(assignedSpells[slot]);
    }

    public boolean assignSpellSlot(int slot, int spellIndex) {
        if (slot < 0 || slot >= ASSIGNED_SLOTS || spellIndex < 0 || spellIndex >= spells.size()) {
            return false;
        }
        assignedSpells[slot] = spellIndex;
        return true;
    }

    private Optional<WandSpellData> activeSpellData() {
        if (spells.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(spells.get(activeIndex));
    }

    private static int[] defaultAssignments(int spellCount) {
        int[] assignments = new int[ASSIGNED_SLOTS];
        for (int i = 0; i < assignments.length; i++) {
            assignments[i] = Math.max(0, Math.min(i, Math.max(0, spellCount - 1)));
        }
        return assignments;
    }

    private static int[] readAssignments(CompoundTag tag, int spellCount) {
        if (!tag.contains(TAG_ASSIGNED_SPELLS, 9)) {
            return defaultAssignments(spellCount);
        }

        ListTag assignedTags = tag.getList(TAG_ASSIGNED_SPELLS, 3);
        int[] assignments = defaultAssignments(spellCount);
        for (int i = 0; i < Math.min(assignments.length, assignedTags.size()); i++) {
            assignments[i] = assignedTags.getInt(i);
        }
        return normalizeAssignments(assignments, spellCount);
    }

    private static int[] normalizeAssignments(int[] input, int spellCount) {
        int[] assignments = defaultAssignments(spellCount);
        for (int i = 0; i < assignments.length && i < input.length; i++) {
            assignments[i] = Math.max(0, Math.min(input[i], Math.max(0, spellCount - 1)));
        }
        return assignments;
    }

    private static Map<String, Integer> sanitizeModifiers(Map<String, Integer> input) {
        Map<String, Integer> sanitized = new LinkedHashMap<>();
        input.forEach((key, level) -> {
            if (WandModifier.byKey(key).isPresent() && level != null && level > 0) {
                sanitized.put(key, Math.min(level, MAX_WAND_MODIFIER_LEVEL));
            }
        });
        return sanitized;
    }

    public static float damageMultiplier(int level) {
        return 1.0F + (Math.max(1, Math.min(MAX_SPELL_LEVEL, level)) - 1) * 0.015F;
    }

    public static float utilityMultiplier(int level) {
        return 1.0F + (Math.max(1, Math.min(MAX_SPELL_LEVEL, level)) - 1) * 0.01F;
    }

    public record WandSpellData(String key, String displayName, int manaCost, int cooldownTicks, int level, int xp, int attributePoints, int chaining, int chainingEntities, int chainingRadius, int chainingDamage, int damage, int healing, int radius, int multistrike, int multistrikeCasts, int range, int duration, int summonHealth, int summonDefense, int summonArmor, int summonAttack, int summonSpeed, long cooldownUntil) {
        public WandSpellData(String key, int manaCost, int cooldownTicks) {
            this(key, manaCost, cooldownTicks, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        public WandSpellData(String key, String displayName, int manaCost, int cooldownTicks) {
            this(key, displayName, manaCost, cooldownTicks, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0L);
        }

        public WandSpellData(String key, int manaCost, int cooldownTicks, int level, int xp, int attributePoints, int chaining, int chainingEntities, int chainingRadius, int chainingDamage, int damage, int healing, int radius, int multistrike, int multistrikeCasts, int range, int duration) {
            this(key, SpellInstance.fixed(key).displayName(), manaCost, cooldownTicks, level, xp, attributePoints, chaining, chainingEntities, chainingRadius, chainingDamage, damage, healing, radius, multistrike, multistrikeCasts, range, duration, 0, 0, 0, 0, 0, 0L);
        }

        private static WandSpellData fromLegacyKey(String key) {
            Spell spell = SpellRegistry.get(key);
            if (spell == null) {
                return new WandSpellData(key, 0, 0);
            }
            return new WandSpellData(key, SpellInstance.fixed(key).displayName(), spell.manaCost(), spell.cooldownTicks());
        }

        private static WandSpellData read(CompoundTag tag) {
            CompoundTag upgrades = tag.getCompound(TAG_SPELL_UPGRADES);
            String key = tag.getString(TAG_SPELL_KEY);
            String name = tag.contains(TAG_SPELL_NAME) ? tag.getString(TAG_SPELL_NAME) : SpellInstance.fixed(key).displayName();
            Spell spell = SpellRegistry.get(key);
            int chainingRadius = upgrades.getInt("chaining_radius");
            int chainingDamage = upgrades.getInt("chaining_damage");
            int damage = upgrades.getInt("damage");
            int healing = upgrades.getInt("healing");
            int radius = upgrades.getInt("radius");
            boolean summonSpell = SpellRegistry.payloadParts(key).stream().anyMatch(payload -> payload.startsWith("summon"));
            int summonDefense = upgrades.contains("summon_defense") ? upgrades.getInt("summon_defense") : summonSpell ? damage / 2 : 0;
            int summonAttack = upgrades.contains("summon_attack") ? upgrades.getInt("summon_attack") : summonSpell ? damage : 0;

            if (spell != null && spell.supportsUpgrade(Spell.UPGRADE_CHAINING)) {
                if (!upgrades.contains("chaining_radius") && !spell.supportsUpgrade(Spell.UPGRADE_RADIUS)) {
                    chainingRadius = radius;
                    radius = 0;
                }
                if (!upgrades.contains("chaining_damage") && !spell.supportsUpgrade(Spell.UPGRADE_DAMAGE)) {
                    chainingDamage = damage;
                    damage = 0;
                }
            }

            return new WandSpellData(
                    key,
                    name,
                    tag.getInt(TAG_SPELL_COST),
                    tag.getInt(TAG_SPELL_COOLDOWN),
                    tag.contains(TAG_SPELL_LEVEL) ? Math.max(1, Math.min(MAX_SPELL_LEVEL, tag.getInt(TAG_SPELL_LEVEL))) : 1,
                    tag.getInt(TAG_SPELL_XP),
                    tag.getInt(TAG_SPELL_POINTS),
                    upgrades.getInt("chaining"),
                    upgrades.getInt("chaining_entities"),
                    chainingRadius,
                    chainingDamage,
                    damage,
                    healing,
                    radius,
                    upgrades.getInt("multistrike"),
                    upgrades.getInt("multistrike_casts"),
                    upgrades.getInt("range"),
                    upgrades.getInt("duration"),
                    upgrades.getInt("summon_health"),
                    summonDefense,
                    upgrades.getInt("summon_armor"),
                    summonAttack,
                    upgrades.getInt("summon_speed"),
                    tag.getLong(TAG_SPELL_COOLDOWN_UNTIL)
            );
        }

        public int xpToNextLevel() {
            if (level >= MAX_SPELL_LEVEL) {
                return 0;
            }
            return 25 + level * 10;
        }

        private WandSpellData addXp(int amount) {
            if (level >= MAX_SPELL_LEVEL) {
                return new WandSpellData(key, displayName, manaCost, cooldownTicks, MAX_SPELL_LEVEL, 0, attributePoints, chaining, chainingEntities, chainingRadius, chainingDamage, damage, healing, radius, multistrike, multistrikeCasts, range, duration, summonHealth, summonDefense, summonArmor, summonAttack, summonSpeed, cooldownUntil);
            }

            int nextLevel = level;
            int nextXp = xp + amount;
            int earnedPoints = 0;
            while (nextLevel < MAX_SPELL_LEVEL) {
                int needed = 25 + nextLevel * 10;
                if (nextXp < needed) {
                    break;
                }
                nextXp -= needed;
                nextLevel++;
                earnedPoints++;
            }
            if (nextLevel >= MAX_SPELL_LEVEL) {
                nextXp = 0;
            }
            return new WandSpellData(key, displayName, manaCost, cooldownTicks, nextLevel, nextXp, attributePoints + earnedPoints, chaining, chainingEntities, chainingRadius, chainingDamage, damage, healing, radius, multistrike, multistrikeCasts, range, duration, summonHealth, summonDefense, summonArmor, summonAttack, summonSpeed, cooldownUntil);
        }

        private WandSpellData merge(WandSpellData other) {
            return new WandSpellData(
                    key,
                    displayName,
                    Math.min(manaCost, other.manaCost),
                    Math.min(cooldownTicks, other.cooldownTicks),
                    Math.max(level, other.level),
                    Math.max(xp, other.xp),
                    Math.max(attributePoints, other.attributePoints),
                    Math.max(chaining, other.chaining),
                    Math.max(chainingEntities, other.chainingEntities),
                    Math.max(chainingRadius, other.chainingRadius),
                    Math.max(chainingDamage, other.chainingDamage),
                    Math.max(damage, other.damage),
                    Math.max(healing, other.healing),
                    Math.max(radius, other.radius),
                    Math.max(multistrike, other.multistrike),
                    Math.max(multistrikeCasts, other.multistrikeCasts),
                    Math.max(range, other.range),
                    Math.max(duration, other.duration),
                    Math.max(summonHealth, other.summonHealth),
                    Math.max(summonDefense, other.summonDefense),
                    Math.max(summonArmor, other.summonArmor),
                    Math.max(summonAttack, other.summonAttack),
                    Math.max(summonSpeed, other.summonSpeed),
                    Math.max(cooldownUntil, other.cooldownUntil)
            );
        }

        public int upgradeLevel(String upgrade) {
            return switch (upgrade) {
                case "chaining" -> chaining;
                case "chaining.entities" -> chainingEntities;
                case "chaining.radius" -> chainingRadius;
                case "chaining.damage" -> chainingDamage;
                case "damage" -> damage;
                case "healing" -> healing;
                case "radius" -> radius;
                case "multistrike" -> multistrike;
                case "multistrike.casts" -> multistrikeCasts;
                case "range" -> range;
                case "duration" -> duration;
                case "summon.health" -> summonHealth;
                case "summon.defense" -> summonDefense;
                case "summon.armor" -> summonArmor;
                case "summon.attack" -> summonAttack;
                case "summon.speed" -> summonSpeed;
                default -> 0;
            };
        }

        public int upgradeCost(String upgrade) {
            return 1;
        }

        public boolean canUpgrade(String upgrade) {
            return attributePoints >= upgradeCost(upgrade)
                    && (!upgrade.equals(Spell.UPGRADE_ENTITIES) || !isSummonSpell() || chainingEntities < level / 5);
        }

        private boolean isSummonSpell() {
            return SpellRegistry.payloadParts(key).stream().anyMatch(payload -> payload.startsWith("summon"));
        }

        private WandSpellData changeShape(String shape) {
            String key = SpellRegistry.withShape(this.key, shape);
            if (key.isEmpty() || key.equals(this.key)) {
                return this;
            }

            Spell spell = SpellRegistry.get(key);
            if (spell == null) {
                return this;
            }

            return new WandSpellData(key, displayName, manaCost, cooldownTicks, level, xp, attributePoints, chaining, chainingEntities, chainingRadius, chainingDamage, damage, healing, radius, multistrike, multistrikeCasts, range, duration, summonHealth, summonDefense, summonArmor, summonAttack, summonSpeed, cooldownUntil);
        }

        private WandSpellData withCooldownUntil(long cooldownUntil) {
            return new WandSpellData(key, displayName, manaCost, cooldownTicks, level, xp, attributePoints, chaining, chainingEntities, chainingRadius, chainingDamage, damage, healing, radius, multistrike, multistrikeCasts, range, duration, summonHealth, summonDefense, summonArmor, summonAttack, summonSpeed, Math.max(0L, cooldownUntil));
        }

        private WandSpellData upgrade(String upgrade) {
            if (!canUpgrade(upgrade)) {
                return this;
            }

            int nextPoints = attributePoints - upgradeCost(upgrade);
            return switch (upgrade) {
                case "chaining" -> new WandSpellData(key, displayName, manaCost + Math.max(1, Math.round(manaCost * 0.15F)), cooldownTicks, level, xp, nextPoints, chaining + 1, chainingEntities, chainingRadius, chainingDamage, damage, healing, radius, multistrike, multistrikeCasts, range, duration, summonHealth, summonDefense, summonArmor, summonAttack, summonSpeed, cooldownUntil);
                case "chaining.entities" -> new WandSpellData(key, displayName, manaCost + Math.max(1, Math.round(manaCost * 0.15F)), cooldownTicks, level, xp, nextPoints, chaining, chainingEntities + 1, chainingRadius, chainingDamage, damage, healing, radius, multistrike, multistrikeCasts, range, duration, summonHealth, summonDefense, summonArmor, summonAttack, summonSpeed, cooldownUntil);
                case "chaining.radius" -> new WandSpellData(key, displayName, manaCost + Math.max(1, Math.round(manaCost * 0.10F)), cooldownTicks + Math.max(1, Math.round(cooldownTicks * 0.05F)), level, xp, nextPoints, chaining, chainingEntities, chainingRadius + 1, chainingDamage, damage, healing, radius, multistrike, multistrikeCasts, range, duration, summonHealth, summonDefense, summonArmor, summonAttack, summonSpeed, cooldownUntil);
                case "chaining.damage" -> new WandSpellData(key, displayName, manaCost + Math.max(1, Math.round(manaCost * 0.15F)), cooldownTicks, level, xp, nextPoints, chaining, chainingEntities, chainingRadius, chainingDamage + 1, damage, healing, radius, multistrike, multistrikeCasts, range, duration, summonHealth, summonDefense, summonArmor, summonAttack, summonSpeed, cooldownUntil);
                case "damage" -> new WandSpellData(key, displayName, manaCost + Math.max(1, Math.round(manaCost * 0.15F)), cooldownTicks, level, xp, nextPoints, chaining, chainingEntities, chainingRadius, chainingDamage, damage + 1, healing, radius, multistrike, multistrikeCasts, range, duration, summonHealth, summonDefense, summonArmor, summonAttack, summonSpeed, cooldownUntil);
                case "healing" -> new WandSpellData(key, displayName, manaCost + Math.max(1, Math.round(manaCost * 0.12F)), cooldownTicks, level, xp, nextPoints, chaining, chainingEntities, chainingRadius, chainingDamage, damage, healing + 1, radius, multistrike, multistrikeCasts, range, duration, summonHealth, summonDefense, summonArmor, summonAttack, summonSpeed, cooldownUntil);
                case "multistrike" -> new WandSpellData(key, displayName, manaCost + Math.max(1, Math.round(manaCost * 0.35F)), cooldownTicks + Math.max(1, Math.round(cooldownTicks * 0.10F)), level, xp, nextPoints, chaining, chainingEntities, chainingRadius, chainingDamage, damage, healing, radius, multistrike + 1, multistrikeCasts, range, duration, summonHealth, summonDefense, summonArmor, summonAttack, summonSpeed, cooldownUntil);
                case "multistrike.casts" -> new WandSpellData(key, displayName, manaCost + Math.max(1, Math.round(manaCost * 0.25F)), cooldownTicks + Math.max(1, Math.round(cooldownTicks * 0.08F)), level, xp, nextPoints, chaining, chainingEntities, chainingRadius, chainingDamage, damage, healing, radius, multistrike, multistrikeCasts + 1, range, duration, summonHealth, summonDefense, summonArmor, summonAttack, summonSpeed, cooldownUntil);
                case "range" -> new WandSpellData(key, displayName, manaCost + Math.max(1, Math.round(manaCost * 0.20F)), cooldownTicks + Math.max(1, Math.round(cooldownTicks * 0.05F)), level, xp, nextPoints, chaining, chainingEntities, chainingRadius, chainingDamage, damage, healing, radius, multistrike, multistrikeCasts, range + 1, duration, summonHealth, summonDefense, summonArmor, summonAttack, summonSpeed, cooldownUntil);
                case "radius" -> new WandSpellData(key, displayName, manaCost + Math.max(1, Math.round(manaCost * 0.10F)), cooldownTicks + Math.max(1, Math.round(cooldownTicks * 0.05F)), level, xp, nextPoints, chaining, chainingEntities, chainingRadius, chainingDamage, damage, healing, radius + 1, multistrike, multistrikeCasts, range, duration, summonHealth, summonDefense, summonArmor, summonAttack, summonSpeed, cooldownUntil);
                case "duration" -> new WandSpellData(key, displayName, manaCost + Math.max(1, Math.round(manaCost * 0.15F)), cooldownTicks + Math.max(1, Math.round(cooldownTicks * 0.05F)), level, xp, nextPoints, chaining, chainingEntities, chainingRadius, chainingDamage, damage, healing, radius, multistrike, multistrikeCasts, range, duration + 1, summonHealth, summonDefense, summonArmor, summonAttack, summonSpeed, cooldownUntil);
                case "summon.health" -> new WandSpellData(key, displayName, manaCost + Math.max(1, Math.round(manaCost * 0.15F)), cooldownTicks, level, xp, nextPoints, chaining, chainingEntities, chainingRadius, chainingDamage, damage, healing, radius, multistrike, multistrikeCasts, range, duration, summonHealth + 1, summonDefense, summonArmor, summonAttack, summonSpeed, cooldownUntil);
                case "summon.defense" -> new WandSpellData(key, displayName, manaCost + Math.max(1, Math.round(manaCost * 0.12F)), cooldownTicks, level, xp, nextPoints, chaining, chainingEntities, chainingRadius, chainingDamage, damage, healing, radius, multistrike, multistrikeCasts, range, duration, summonHealth, summonDefense + 1, summonArmor, summonAttack, summonSpeed, cooldownUntil);
                case "summon.armor" -> new WandSpellData(key, displayName, manaCost + Math.max(1, Math.round(manaCost * 0.12F)), cooldownTicks, level, xp, nextPoints, chaining, chainingEntities, chainingRadius, chainingDamage, damage, healing, radius, multistrike, multistrikeCasts, range, duration, summonHealth, summonDefense, summonArmor + 1, summonAttack, summonSpeed, cooldownUntil);
                case "summon.attack" -> new WandSpellData(key, displayName, manaCost + Math.max(1, Math.round(manaCost * 0.18F)), cooldownTicks, level, xp, nextPoints, chaining, chainingEntities, chainingRadius, chainingDamage, damage, healing, radius, multistrike, multistrikeCasts, range, duration, summonHealth, summonDefense, summonArmor, summonAttack + 1, summonSpeed, cooldownUntil);
                case "summon.speed" -> new WandSpellData(key, displayName, manaCost + Math.max(1, Math.round(manaCost * 0.10F)), cooldownTicks, level, xp, nextPoints, chaining, chainingEntities, chainingRadius, chainingDamage, damage, healing, radius, multistrike, multistrikeCasts, range, duration, summonHealth, summonDefense, summonArmor, summonAttack, summonSpeed + 1, cooldownUntil);
                default -> this;
            };
        }
    }
}
