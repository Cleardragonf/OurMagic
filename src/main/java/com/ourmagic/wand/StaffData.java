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

public final class StaffData {
    public static final String TAG_TEMPLATE = "OurMagicStaffTemplate";
    private static final String TAG_NAME = "OurMagicStaffName";
    private static final String TAG_POWER = "OurMagicStaffPower";
    private static final String TAG_SPELLS = "OurMagicStaffSpells";
    private static final String TAG_ACTIVE = "OurMagicStaffActive";
    private static final String TAG_ASSIGNED_SPELLS = "OurMagicStaffAssignedSpells";
    private static final String TAG_COOLDOWN = "OurMagicStaffCooldown";
    private static final String TAG_STAFF_LEVEL = "OurMagicStaffLevel";
    private static final String TAG_STAFF_XP = "OurMagicStaffXp";
    private static final String TAG_STAFF_MODIFIERS = "OurMagicStaffModifiers";
    private static final String TAG_SPELL_KEY = "Key";
    private static final String TAG_SPELL_NAME = "Name";
    private static final String TAG_SPELL_COST = "Cost";
    private static final String TAG_SPELL_COOLDOWN = "Cooldown";
    private static final String TAG_SPELL_LEVEL = "Level";
    private static final String TAG_SPELL_XP = "Xp";
    private static final String TAG_SPELL_POINTS = "AttributePoints";
    private static final String TAG_SPELL_UPGRADES = "Upgrades";
    private static final int MAX_SPELL_LEVEL = 100;
    private static final int MAX_STAFF_LEVEL = 20;
    private static final int ASSIGNED_SLOTS = 6;
    private static final String SHAPE_UPGRADE_PREFIX = "shape:";
    private static final float BASE_MELEE_DAMAGE = 6.0F;

    private final String displayName;
    private final float power;
    private final List<WandSpellData> spells;
    private final Map<String, Integer> modifiers;
    private int staffLevel;
    private int staffXp;
    private int activeIndex;
    private final int[] assignedSpells;
    private long cooldownUntil;

    public StaffData(String displayName, float power, List<WandSpellData> spells, int activeIndex, long cooldownUntil) {
        this(displayName, power, spells, activeIndex, defaultAssignments(spells.size()), cooldownUntil, 1, 0, Map.of());
    }

    public StaffData(String displayName, float power, List<WandSpellData> spells, int activeIndex, long cooldownUntil, int staffLevel, int staffXp, Map<String, Integer> modifiers) {
        this(displayName, power, spells, activeIndex, defaultAssignments(spells.size()), cooldownUntil, staffLevel, staffXp, modifiers);
    }

    public StaffData(String displayName, float power, List<WandSpellData> spells, int activeIndex, int[] assignedSpells, long cooldownUntil, int staffLevel, int staffXp, Map<String, Integer> modifiers) {
        this.displayName = displayName;
        this.power = power;
        this.spells = new ArrayList<>(spells);
        this.staffLevel = Math.max(1, Math.min(MAX_STAFF_LEVEL, staffLevel));
        this.staffXp = Math.max(0, staffXp);
        this.modifiers = new LinkedHashMap<>(modifiers);
        this.activeIndex = Math.max(0, Math.min(activeIndex, Math.max(0, spells.size() - 1)));
        this.assignedSpells = normalizeAssignments(assignedSpells, spells.size());
        this.cooldownUntil = cooldownUntil;
    }

    public static StaffData createNew() {
        return new StaffData("Custom Staff", 1.0F, new ArrayList<>(), 0, 0);
    }

    public static StaffData read(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        String name = tag.getString(TAG_NAME);
        if (name.isEmpty()) {
            name = "Staff";
        }

        List<WandSpellData> spells = new ArrayList<>();
        ListTag spellTags = tag.getList(TAG_SPELLS, 8);
        for (int i = 0; i < spellTags.size(); i++) {
            spells.add(WandSpellData.load(spellTags.getCompound(i)));
        }

        int[] assignedSpells = normalizeAssignments(tag.getLongArray(TAG_ASSIGNED_SPELLS), spells.size());
        long cooldownUntil = tag.getLong(TAG_COOLDOWN);
        int staffLevel = tag.getInt(TAG_STAFF_LEVEL);
        if (staffLevel == 0) staffLevel = 1;
        int staffXp = tag.getInt(TAG_STAFF_XP);

        Map<String, Integer> modifiers = new LinkedHashMap<>();
        CompoundTag modifierTag = tag.getCompound(TAG_STAFF_MODIFIERS);
        for (String key : modifierTag.getAllKeys()) {
            modifiers.put(key, modifierTag.getInt(key));
        }

        return new StaffData(name, tag.getFloat(TAG_POWER), spells, tag.getInt(TAG_ACTIVE), assignedSpells, cooldownUntil, staffLevel, staffXp, modifiers);
    }

    public void save(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_NAME, displayName);
        tag.putFloat(TAG_POWER, power);
        tag.putInt(TAG_STAFF_LEVEL, staffLevel);
        tag.putInt(TAG_STAFF_XP, staffXp);
        tag.putInt(TAG_ACTIVE, activeIndex);
        tag.putLongArray(TAG_ASSIGNED_SPELLS, java.util.Arrays.stream(assignedSpells).asLongStream().toArray());
        tag.putLong(TAG_COOLDOWN, cooldownUntil);

        ListTag spellTags = new ListTag();
        for (WandSpellData spell : spells) {
            spellTags.add(spell.save());
        }
        tag.put(TAG_SPELLS, spellTags);

        CompoundTag modifierTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : modifiers.entrySet()) {
            modifierTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put(TAG_STAFF_MODIFIERS, modifierTag);
    }

    public String displayName() {
        return displayName;
    }

    public float power() {
        return power;
    }

    public int staffLevel() {
        return staffLevel;
    }

    public int staffXp() {
        return staffXp;
    }

    public int staffXpToNextLevel() {
        if (staffLevel >= MAX_STAFF_LEVEL) {
            return 0;
        }
        return 80 + staffLevel * 20;
    }

    public int staffModifierSlots() {
        // Linear growth: 1 slot per 7 levels
        return (staffLevel / 7) + 1;
    }

    public int usedStaffModifierSlots() {
        return modifiers.values().stream().mapToInt(Integer::intValue).sum();
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

    public int addActiveSpellXp(int amount) {
        var active = activeSpellData();
        if (active.isEmpty()) {
            return 0;
        }
        int levelsGained = active.get().addXp(amount);
        return levelsGained;
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
        float multiplier = Math.max(0.35F, 1.0F - modifierLevel(WandModifier.HASTE.key()) * 0.08F);
        int woodCount = getWoodModifierCount();
        multiplier *= Math.max(0.35F, 1.0F - (woodCount * 0.05F));
        if (hasModifier(WandMaterialModifier.STRING.key())) {
            multiplier *= 0.975F;
        }
        if (hasModifier(WandMaterialModifier.GUNPOWDER.key())) {
            multiplier *= 1.025F;
        }
        return multiplier;
    }

    public float damageMultiplier() {
        float multiplier = 1.0F + modifierLevel(WandModifier.POTENCY.key()) * 0.12F;
        if (hasModifier(WandMaterialModifier.ROTTEN_FLESH.key())) {
            multiplier *= 1.05F;
        }
        if (hasModifier(WandMaterialModifier.SPIDER_EYE.key())) {
            multiplier *= 1.03F;
        }
        return multiplier;
    }

    public float rangeMultiplier() {
        float multiplier = 1.0F + modifierLevel(WandModifier.ELASTICITY.key()) * 0.08F;
        if (hasModifier(WandMaterialModifier.ENDER_PEARL.key())) {
            multiplier *= 1.05F;
        }
        return multiplier;
    }

    public float durationMultiplier() {
        float multiplier = 1.0F + modifierLevel(WandModifier.ENDURANCE.key()) * 0.10F;
        if (hasModifier(WandMaterialModifier.BONE.key())) {
            multiplier *= 1.10F;
        }
        return multiplier;
    }

    public float damageMultiplier(int spellLevel) {
        return 1.0F + spellLevel * 0.05F;
    }

    public float utilityMultiplier(int spellLevel) {
        return 1.0F + spellLevel * 0.03F;
    }

    public float xpMultiplier() {
        return 1.0F + modifierLevel(WandModifier.WISDOM.key()) * 0.04F;
    }

    public float meleeDamage() {
        float damage = BASE_MELEE_DAMAGE;
        damage += modifierLevel(WandModifier.POTENCY.key()) * 0.6F;
        if (hasModifier(WandMaterialModifier.ROTTEN_FLESH.key())) {
            damage *= 1.05F;
        }
        return damage;
    }

    public long cooldownUntil() {
        return cooldownUntil;
    }

    public void setCooldownUntil(long cooldownUntil) {
        this.cooldownUntil = cooldownUntil;
    }

    public Optional<WandSpellData> activeSpellData() {
        return Optional.ofNullable(spells.size() > activeIndex && activeIndex >= 0 ? spells.get(activeIndex) : null);
    }

    public List<WandSpellData> spells() {
        return List.copyOf(spells);
    }

    public boolean canAddStaffModifier(String key) {
        Optional<WandModifier> leveledModifier = WandModifier.byKey(key);
        if (leveledModifier.isPresent()) {
            // For staffs, leveled modifiers have no cap (infinite)
            return usedStaffModifierSlots() < staffModifierSlots();
        }
        
        Optional<WandMaterialModifier> materialModifier = WandMaterialModifier.byKey(key);
        if (materialModifier.isPresent()) {
            return !modifiers.containsKey(key) && usedStaffModifierSlots() < staffModifierSlots();
        }
        
        return false;
    }

    public boolean addStaffModifier(String key) {
        if (!canAddStaffModifier(key)) {
            return false;
        }
        modifiers.merge(key, 1, Integer::sum);
        return true;
    }

    public int addStaffXp(int amount) {
        if (amount <= 0 || staffLevel >= MAX_STAFF_LEVEL) {
            return 0;
        }
        int oldLevel = staffLevel;
        staffXp += amount;
        while (staffLevel < MAX_STAFF_LEVEL && staffXp >= staffXpToNextLevel()) {
            staffXp -= staffXpToNextLevel();
            staffLevel++;
        }
        if (staffLevel >= MAX_STAFF_LEVEL) {
            staffXp = 0;
        }
        return staffLevel - oldLevel;
    }

    public boolean addSpell(String key) {
        Spell spell = SpellRegistry.get(key);
        if (spell == null || spells.stream().anyMatch(existing -> existing.key().equals(key))) {
            return false;
        }

        spells.add(new WandSpellData(key, spell.manaCost(), spell.cooldownTicks()));
        return true;
    }

    public boolean addSpell(SpellInstance instance) {
        if (SpellRegistry.get(instance.key()) == null || spells.stream().anyMatch(existing -> existing.key().equals(instance.key()))) {
            return false;
        }

        spells.add(new WandSpellData(instance.key(), instance.displayName(), instance.manaCost(), instance.cooldownTicks()));
        return true;
    }

    private boolean hasModifier(String key) {
        return modifiers.containsKey(key);
    }

    private int getWoodModifierCount() {
        int count = 0;
        for (WandMaterialModifier wood : WandMaterialModifier.WOODS) {
            if (hasModifier(wood.key())) {
                count++;
            }
        }
        return count;
    }

    private static int[] normalizeAssignments(int[] assignments, int spellCount) {
        int[] normalized = new int[ASSIGNED_SLOTS];
        for (int i = 0; i < ASSIGNED_SLOTS; i++) {
            if (i < assignments.length) {
                normalized[i] = Math.max(0, Math.min(Math.toIntExact(assignments[i]), Math.max(0, spellCount - 1)));
            } else {
                normalized[i] = 0;
            }
        }
        return normalized;
    }

    private static int[] normalizeAssignments(long[] assignments, int spellCount) {
        int[] normalized = new int[ASSIGNED_SLOTS];
        for (int i = 0; i < ASSIGNED_SLOTS; i++) {
            if (i < assignments.length) {
                normalized[i] = Math.max(0, Math.min(Math.toIntExact(assignments[i]), Math.max(0, spellCount - 1)));
            } else {
                normalized[i] = 0;
            }
        }
        return normalized;
    }

    private static int[] defaultAssignments(int spellCount) {
        int[] assignments = new int[ASSIGNED_SLOTS];
        for (int i = 0; i < Math.min(spellCount, ASSIGNED_SLOTS); i++) {
            assignments[i] = i;
        }
        return assignments;
    }
}
