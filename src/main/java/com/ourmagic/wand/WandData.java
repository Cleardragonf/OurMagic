package com.ourmagic.wand;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.SpellRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class WandData {
    public static final String TAG_TEMPLATE = "OurMagicTemplate";
    private static final String TAG_NAME = "OurMagicName";
    private static final String TAG_POWER = "OurMagicPower";
    private static final String TAG_SPELLS = "OurMagicSpells";
    private static final String TAG_ACTIVE = "OurMagicActive";
    private static final String TAG_COOLDOWN = "OurMagicCooldown";
    private static final String TAG_SPELL_KEY = "Key";
    private static final String TAG_SPELL_COST = "Cost";
    private static final String TAG_SPELL_COOLDOWN = "Cooldown";
    private static final String TAG_SPELL_LEVEL = "Level";
    private static final String TAG_SPELL_XP = "Xp";
    private static final String TAG_SPELL_POINTS = "AttributePoints";
    private static final String TAG_SPELL_UPGRADES = "Upgrades";
    private static final int MAX_SPELL_LEVEL = 100;

    private final String template;
    private final String displayName;
    private final float power;
    private final List<WandSpellData> spells;
    private int activeIndex;
    private long cooldownUntil;

    public WandData(String template, String displayName, float power, List<WandSpellData> spells, int activeIndex, long cooldownUntil) {
        this.template = template;
        this.displayName = displayName;
        this.power = power;
        this.spells = new ArrayList<>(spells);
        this.activeIndex = Math.max(0, Math.min(activeIndex, Math.max(0, spells.size() - 1)));
        this.cooldownUntil = cooldownUntil;
    }

    public static WandData fromTemplate(WandTemplate template) {
        return new WandData(template.key(), template.displayName(), template.power(), template.spells(), 0, 0);
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

        return new WandData(
                tag.contains(TAG_TEMPLATE) ? templateKey : fallback.key(),
                tag.contains(TAG_NAME) ? tag.getString(TAG_NAME) : fallback.displayName(),
                tag.contains(TAG_POWER) ? tag.getFloat(TAG_POWER) : fallback.power(),
                spells,
                tag.getInt(TAG_ACTIVE),
                tag.getLong(TAG_COOLDOWN)
        );
    }

    public void save(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_TEMPLATE, template);
        tag.putString(TAG_NAME, displayName);
        tag.putFloat(TAG_POWER, power);
        tag.putInt(TAG_ACTIVE, activeIndex);
        tag.putLong(TAG_COOLDOWN, cooldownUntil);

        ListTag spellTags = new ListTag();
        for (WandSpellData spell : spells) {
            CompoundTag spellTag = new CompoundTag();
            spellTag.putString(TAG_SPELL_KEY, spell.key());
            spellTag.putInt(TAG_SPELL_COST, spell.manaCost());
            spellTag.putInt(TAG_SPELL_COOLDOWN, spell.cooldownTicks());
            spellTag.putInt(TAG_SPELL_LEVEL, spell.level());
            spellTag.putInt(TAG_SPELL_XP, spell.xp());
            spellTag.putInt(TAG_SPELL_POINTS, spell.attributePoints());
            CompoundTag upgrades = new CompoundTag();
            upgrades.putInt("chaining", spell.chaining());
            upgrades.putInt("chaining_damage", spell.chainingDamage());
            upgrades.putInt("chaining_entities", spell.chainingEntities());
            upgrades.putInt("chaining_radius", spell.chainingRadius());
            upgrades.putInt("multistrike", spell.multistrike());
            upgrades.putInt("multistrike_casts", spell.multistrikeCasts());
            upgrades.putInt("multistrike_power", spell.multistrikePower());
            upgrades.putInt("range", spell.range());
            upgrades.putInt("duration", spell.duration());
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

    public int activeIndex() {
        return activeIndex;
    }

    public String activeSpell() {
        return activeSpellData().map(WandSpellData::key).orElse("");
    }

    public String activeSpellName() {
        String spell = activeSpell();
        return spell.isEmpty() ? "None" : spell.substring(0, 1).toUpperCase() + spell.substring(1);
    }

    public int activeManaCost() {
        return activeSpellData().map(WandSpellData::manaCost).orElse(0);
    }

    public int activeCooldownTicks() {
        return activeSpellData().map(WandSpellData::cooldownTicks).orElse(0);
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
        return damageMultiplier(activeSpellLevel());
    }

    public float activeUtilityMultiplier() {
        return utilityMultiplier(activeSpellLevel());
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
                && current.attributePoints() >= current.upgradeCost(upgrade);
    }

    public List<WandSpellData> spells() {
        return List.copyOf(spells);
    }

    public long cooldownUntil() {
        return cooldownUntil;
    }

    public void setCooldownUntil(long cooldownUntil) {
        this.cooldownUntil = cooldownUntil;
    }

    public void cycleSpell(int offset) {
        if (!spells.isEmpty()) {
            activeIndex = Math.floorMod(activeIndex + offset, spells.size());
        }
    }

    private Optional<WandSpellData> activeSpellData() {
        if (spells.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(spells.get(activeIndex));
    }

    public static float damageMultiplier(int level) {
        return 1.0F + (Math.max(1, Math.min(MAX_SPELL_LEVEL, level)) - 1) * 0.015F;
    }

    public static float utilityMultiplier(int level) {
        return 1.0F + (Math.max(1, Math.min(MAX_SPELL_LEVEL, level)) - 1) * 0.01F;
    }

    public record WandSpellData(String key, int manaCost, int cooldownTicks, int level, int xp, int attributePoints, int chaining, int chainingDamage, int chainingEntities, int chainingRadius, int multistrike, int multistrikeCasts, int multistrikePower, int range, int duration) {
        public WandSpellData(String key, int manaCost, int cooldownTicks) {
            this(key, manaCost, cooldownTicks, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        private static WandSpellData fromLegacyKey(String key) {
            Spell spell = SpellRegistry.get(key);
            if (spell == null) {
                return new WandSpellData(key, 0, 0);
            }
            return new WandSpellData(key, spell.manaCost(), spell.cooldownTicks());
        }

        private static WandSpellData read(CompoundTag tag) {
            CompoundTag upgrades = tag.getCompound(TAG_SPELL_UPGRADES);
            return new WandSpellData(
                    tag.getString(TAG_SPELL_KEY),
                    tag.getInt(TAG_SPELL_COST),
                    tag.getInt(TAG_SPELL_COOLDOWN),
                    tag.contains(TAG_SPELL_LEVEL) ? Math.max(1, Math.min(MAX_SPELL_LEVEL, tag.getInt(TAG_SPELL_LEVEL))) : 1,
                    tag.getInt(TAG_SPELL_XP),
                    tag.getInt(TAG_SPELL_POINTS),
                    upgrades.getInt("chaining"),
                    upgrades.getInt("chaining_damage"),
                    upgrades.getInt("chaining_entities"),
                    upgrades.getInt("chaining_radius"),
                    upgrades.getInt("multistrike"),
                    upgrades.getInt("multistrike_casts"),
                    upgrades.getInt("multistrike_power"),
                    upgrades.getInt("range"),
                    upgrades.getInt("duration")
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
                return new WandSpellData(key, manaCost, cooldownTicks, MAX_SPELL_LEVEL, 0, attributePoints, chaining, chainingDamage, chainingEntities, chainingRadius, multistrike, multistrikeCasts, multistrikePower, range, duration);
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
            return new WandSpellData(key, manaCost, cooldownTicks, nextLevel, nextXp, attributePoints + earnedPoints, chaining, chainingDamage, chainingEntities, chainingRadius, multistrike, multistrikeCasts, multistrikePower, range, duration);
        }

        public int upgradeLevel(String upgrade) {
            return switch (upgrade) {
                case "chaining" -> chaining;
                case "chaining.damage" -> chainingDamage;
                case "chaining.entities" -> chainingEntities;
                case "chaining.radius" -> chainingRadius;
                case "multistrike" -> multistrike;
                case "multistrike.casts" -> multistrikeCasts;
                case "multistrike.power" -> multistrikePower;
                case "range" -> range;
                case "duration" -> duration;
                default -> 0;
            };
        }

        public int upgradeCost(String upgrade) {
            return 1;
        }

        public boolean canUpgrade(String upgrade) {
            return attributePoints >= upgradeCost(upgrade);
        }

        private WandSpellData upgrade(String upgrade) {
            if (!canUpgrade(upgrade)) {
                return this;
            }

            int nextPoints = attributePoints - upgradeCost(upgrade);
            return switch (upgrade) {
                case "chaining" -> new WandSpellData(key, manaCost + Math.max(1, Math.round(manaCost * 0.15F)), cooldownTicks, level, xp, nextPoints, chaining + 1, chainingDamage, chainingEntities, chainingRadius, multistrike, multistrikeCasts, multistrikePower, range, duration);
                case "chaining.damage" -> new WandSpellData(key, manaCost + Math.max(1, Math.round(manaCost * 0.10F)), cooldownTicks, level, xp, nextPoints, chaining, chainingDamage + 1, chainingEntities, chainingRadius, multistrike, multistrikeCasts, multistrikePower, range, duration);
                case "chaining.entities" -> new WandSpellData(key, manaCost + Math.max(1, Math.round(manaCost * 0.15F)), cooldownTicks, level, xp, nextPoints, chaining, chainingDamage, chainingEntities + 1, chainingRadius, multistrike, multistrikeCasts, multistrikePower, range, duration);
                case "chaining.radius" -> new WandSpellData(key, manaCost + Math.max(1, Math.round(manaCost * 0.10F)), cooldownTicks + Math.max(1, Math.round(cooldownTicks * 0.05F)), level, xp, nextPoints, chaining, chainingDamage, chainingEntities, chainingRadius + 1, multistrike, multistrikeCasts, multistrikePower, range, duration);
                case "multistrike" -> new WandSpellData(key, manaCost + Math.max(1, Math.round(manaCost * 0.35F)), cooldownTicks + Math.max(1, Math.round(cooldownTicks * 0.10F)), level, xp, nextPoints, chaining, chainingDamage, chainingEntities, chainingRadius, multistrike + 1, multistrikeCasts, multistrikePower, range, duration);
                case "multistrike.casts" -> new WandSpellData(key, manaCost + Math.max(1, Math.round(manaCost * 0.25F)), cooldownTicks + Math.max(1, Math.round(cooldownTicks * 0.08F)), level, xp, nextPoints, chaining, chainingDamage, chainingEntities, chainingRadius, multistrike, multistrikeCasts + 1, multistrikePower, range, duration);
                case "multistrike.power" -> new WandSpellData(key, manaCost + Math.max(1, Math.round(manaCost * 0.15F)), cooldownTicks, level, xp, nextPoints, chaining, chainingDamage, chainingEntities, chainingRadius, multistrike, multistrikeCasts, multistrikePower + 1, range, duration);
                case "range" -> new WandSpellData(key, manaCost + Math.max(1, Math.round(manaCost * 0.20F)), cooldownTicks + Math.max(1, Math.round(cooldownTicks * 0.05F)), level, xp, nextPoints, chaining, chainingDamage, chainingEntities, chainingRadius, multistrike, multistrikeCasts, multistrikePower, range + 1, duration);
                case "duration" -> new WandSpellData(key, manaCost + Math.max(1, Math.round(manaCost * 0.15F)), cooldownTicks + Math.max(1, Math.round(cooldownTicks * 0.05F)), level, xp, nextPoints, chaining, chainingDamage, chainingEntities, chainingRadius, multistrike, multistrikeCasts, multistrikePower, range, duration + 1);
                default -> this;
            };
        }
    }
}
