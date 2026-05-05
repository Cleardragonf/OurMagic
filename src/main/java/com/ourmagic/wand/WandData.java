package com.ourmagic.wand;

import com.ourmagic.magic.Spell;
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
    private static final String TAG_COOLDOWN = "OurMagicCooldown";
    private static final String TAG_SPELL_KEY = "Key";
    private static final String TAG_SPELL_COST = "Cost";
    private static final String TAG_SPELL_COOLDOWN = "Cooldown";
    private static final String TAG_SPELL_LEVEL = "Level";
    private static final String TAG_SPELL_XP = "Xp";
    private static final String TAG_SPELL_POINTS = "AttributePoints";
    private static final String TAG_SPELL_UPGRADES = "Upgrades";
    private static final int MAX_SPELL_LEVEL = 100;
    private static final String SHAPE_UPGRADE_PREFIX = "shape:";

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

    public static WandData combine(WandData primary, WandData secondary, int maxSpells) {
        Map<String, WandSpellData> merged = new LinkedHashMap<>();
        for (WandSpellData spell : primary.spells) {
            merged.put(spell.key(), spell);
        }
        for (WandSpellData spell : secondary.spells) {
            merged.merge(spell.key(), spell, WandSpellData::merge);
        }

        List<WandSpellData> spells = merged.values().stream()
                .limit(Math.max(1, maxSpells))
                .toList();
        float power = Math.min(2.0F, Math.max(primary.power, secondary.power) + Math.min(primary.power, secondary.power) * 0.10F);
        return new WandData(primary.template, "Combined Wand", power, spells, 0, 0);
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
            upgrades.putInt("chaining_entities", spell.chainingEntities());
            upgrades.putInt("chaining_radius", spell.chainingRadius());
            upgrades.putInt("chaining_damage", spell.chainingDamage());
            upgrades.putInt("damage", spell.damage());
            upgrades.putInt("radius", spell.radius());
            upgrades.putInt("multistrike", spell.multistrike());
            upgrades.putInt("multistrike_casts", spell.multistrikeCasts());
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
                && current.attributePoints() >= current.upgradeCost(upgrade);
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
        Spell spell = SpellRegistry.get(key);
        if (spell == null || spells.stream().anyMatch(existing -> existing.key().equals(key))) {
            return false;
        }

        int cost = ranged(random, spell.manaCost(), 0.70F, 1.35F);
        int cooldown = ranged(random, spell.cooldownTicks(), 0.70F, 1.40F);
        spells.add(new WandSpellData(key, cost, cooldown));
        return true;
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

    private static int ranged(RandomSource random, int base, float minMultiplier, float maxMultiplier) {
        int min = Math.max(0, Math.round(base * minMultiplier));
        int max = Math.max(min, Math.round(base * maxMultiplier));
        return min + random.nextInt(max - min + 1);
    }

    public record WandSpellData(String key, int manaCost, int cooldownTicks, int level, int xp, int attributePoints, int chaining, int chainingEntities, int chainingRadius, int chainingDamage, int damage, int radius, int multistrike, int multistrikeCasts, int range, int duration) {
        public WandSpellData(String key, int manaCost, int cooldownTicks) {
            this(key, manaCost, cooldownTicks, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
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
            String key = tag.getString(TAG_SPELL_KEY);
            Spell spell = SpellRegistry.get(key);
            int chainingRadius = upgrades.getInt("chaining_radius");
            int chainingDamage = upgrades.getInt("chaining_damage");
            int damage = upgrades.getInt("damage");
            int radius = upgrades.getInt("radius");

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
                    radius,
                    upgrades.getInt("multistrike"),
                    upgrades.getInt("multistrike_casts"),
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
                return new WandSpellData(key, manaCost, cooldownTicks, MAX_SPELL_LEVEL, 0, attributePoints, chaining, chainingEntities, chainingRadius, chainingDamage, damage, radius, multistrike, multistrikeCasts, range, duration);
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
            return new WandSpellData(key, manaCost, cooldownTicks, nextLevel, nextXp, attributePoints + earnedPoints, chaining, chainingEntities, chainingRadius, chainingDamage, damage, radius, multistrike, multistrikeCasts, range, duration);
        }

        private WandSpellData merge(WandSpellData other) {
            return new WandSpellData(
                    key,
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
                    Math.max(radius, other.radius),
                    Math.max(multistrike, other.multistrike),
                    Math.max(multistrikeCasts, other.multistrikeCasts),
                    Math.max(range, other.range),
                    Math.max(duration, other.duration)
            );
        }

        public int upgradeLevel(String upgrade) {
            return switch (upgrade) {
                case "chaining" -> chaining;
                case "chaining.entities" -> chainingEntities;
                case "chaining.radius" -> chainingRadius;
                case "chaining.damage" -> chainingDamage;
                case "damage" -> damage;
                case "radius" -> radius;
                case "multistrike" -> multistrike;
                case "multistrike.casts" -> multistrikeCasts;
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

        private WandSpellData changeShape(String shape) {
            String key = SpellRegistry.withShape(this.key, shape);
            if (key.isEmpty() || key.equals(this.key)) {
                return this;
            }

            Spell spell = SpellRegistry.get(key);
            if (spell == null) {
                return this;
            }

            return new WandSpellData(key, spell.manaCost(), spell.cooldownTicks(), level, xp, attributePoints, chaining, chainingEntities, chainingRadius, chainingDamage, damage, radius, multistrike, multistrikeCasts, range, duration);
        }

        private WandSpellData upgrade(String upgrade) {
            if (!canUpgrade(upgrade)) {
                return this;
            }

            int nextPoints = attributePoints - upgradeCost(upgrade);
            return switch (upgrade) {
                case "chaining" -> new WandSpellData(key, manaCost + Math.max(1, Math.round(manaCost * 0.15F)), cooldownTicks, level, xp, nextPoints, chaining + 1, chainingEntities, chainingRadius, chainingDamage, damage, radius, multistrike, multistrikeCasts, range, duration);
                case "chaining.entities" -> new WandSpellData(key, manaCost + Math.max(1, Math.round(manaCost * 0.15F)), cooldownTicks, level, xp, nextPoints, chaining, chainingEntities + 1, chainingRadius, chainingDamage, damage, radius, multistrike, multistrikeCasts, range, duration);
                case "chaining.radius" -> new WandSpellData(key, manaCost + Math.max(1, Math.round(manaCost * 0.10F)), cooldownTicks + Math.max(1, Math.round(cooldownTicks * 0.05F)), level, xp, nextPoints, chaining, chainingEntities, chainingRadius + 1, chainingDamage, damage, radius, multistrike, multistrikeCasts, range, duration);
                case "chaining.damage" -> new WandSpellData(key, manaCost + Math.max(1, Math.round(manaCost * 0.15F)), cooldownTicks, level, xp, nextPoints, chaining, chainingEntities, chainingRadius, chainingDamage + 1, damage, radius, multistrike, multistrikeCasts, range, duration);
                case "damage" -> new WandSpellData(key, manaCost + Math.max(1, Math.round(manaCost * 0.15F)), cooldownTicks, level, xp, nextPoints, chaining, chainingEntities, chainingRadius, chainingDamage, damage + 1, radius, multistrike, multistrikeCasts, range, duration);
                case "multistrike" -> new WandSpellData(key, manaCost + Math.max(1, Math.round(manaCost * 0.35F)), cooldownTicks + Math.max(1, Math.round(cooldownTicks * 0.10F)), level, xp, nextPoints, chaining, chainingEntities, chainingRadius, chainingDamage, damage, radius, multistrike + 1, multistrikeCasts, range, duration);
                case "multistrike.casts" -> new WandSpellData(key, manaCost + Math.max(1, Math.round(manaCost * 0.25F)), cooldownTicks + Math.max(1, Math.round(cooldownTicks * 0.08F)), level, xp, nextPoints, chaining, chainingEntities, chainingRadius, chainingDamage, damage, radius, multistrike, multistrikeCasts + 1, range, duration);
                case "range" -> new WandSpellData(key, manaCost + Math.max(1, Math.round(manaCost * 0.20F)), cooldownTicks + Math.max(1, Math.round(cooldownTicks * 0.05F)), level, xp, nextPoints, chaining, chainingEntities, chainingRadius, chainingDamage, damage, radius, multistrike, multistrikeCasts, range + 1, duration);
                case "radius" -> new WandSpellData(key, manaCost + Math.max(1, Math.round(manaCost * 0.10F)), cooldownTicks + Math.max(1, Math.round(cooldownTicks * 0.05F)), level, xp, nextPoints, chaining, chainingEntities, chainingRadius, chainingDamage, damage, radius + 1, multistrike, multistrikeCasts, range, duration);
                case "duration" -> new WandSpellData(key, manaCost + Math.max(1, Math.round(manaCost * 0.15F)), cooldownTicks + Math.max(1, Math.round(cooldownTicks * 0.05F)), level, xp, nextPoints, chaining, chainingEntities, chainingRadius, chainingDamage, damage, radius, multistrike, multistrikeCasts, range, duration + 1);
                default -> this;
            };
        }
    }
}
