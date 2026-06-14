package com.ourmagic.magic;

import com.ourmagic.network.CraftSpellPacket;
import com.ourmagic.wand.WandData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

public record SpellInstance(String key, String displayName, int manaCost, int cooldownTicks, int level, int xp, int attributePoints, int chaining, int chainingEntities, int chainingRadius, int chainingDamage, int damage, int healing, int radius, int multistrike, int multistrikeCasts, int range, int duration, int summonHealth, int summonDefense, int summonArmor, int summonAttack, int summonSpeed) {
    public static final String TAG_DISPLAY_NAME = "OurMagicSpellName";
    public static final String TAG_MANA_COST = "OurMagicSpellCost";
    public static final String TAG_COOLDOWN = "OurMagicSpellCooldown";
    private static final String TAG_LEVEL = "OurMagicSpellLevel";
    private static final String TAG_XP = "OurMagicSpellXp";
    private static final String TAG_POINTS = "OurMagicSpellPoints";
    private static final String TAG_UPGRADES = "OurMagicSpellUpgrades";

    public SpellInstance(String key, String displayName, int manaCost, int cooldownTicks) {
        this(key, displayName, manaCost, cooldownTicks, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public static SpellInstance roll(String key, RandomSource random) {
        Spell spell = SpellRegistry.get(key);
        if (spell == null) {
            return new SpellInstance(key, titleCase(key), 0, 0);
        }
        return new SpellInstance(
                key,
                SpellNames.roll(key, random),
                ranged(random, spell.minManaCost(), spell.maxManaCost()),
                ranged(random, spell.minCooldownTicks(), spell.maxCooldownTicks())
        );
    }

    public static SpellInstance fixed(String key) {
        Spell spell = SpellRegistry.get(key);
        if (spell == null) {
            return new SpellInstance(key, titleCase(key), 0, 0);
        }
        return new SpellInstance(key, SpellNames.fallback(key), spell.manaCost(), spell.cooldownTicks());
    }

    public static SpellInstance fromWandSpell(WandData.WandSpellData spell) {
        return new SpellInstance(
                spell.key(),
                spell.displayName(),
                spell.manaCost(),
                spell.cooldownTicks(),
                spell.level(),
                spell.xp(),
                spell.attributePoints(),
                spell.chaining(),
                spell.chainingEntities(),
                spell.chainingRadius(),
                spell.chainingDamage(),
                spell.damage(),
                spell.healing(),
                spell.radius(),
                spell.multistrike(),
                spell.multistrikeCasts(),
                spell.range(),
                spell.duration(),
                spell.summonHealth(),
                spell.summonDefense(),
                spell.summonArmor(),
                spell.summonAttack(),
                spell.summonSpeed()
        );
    }

    public static SpellInstance fromTag(CompoundTag tag) {
        String key = tag.getString(CraftSpellPacket.TAG_SPELL_KEY);
        Spell spell = SpellRegistry.get(key);
        String name = tag.contains(TAG_DISPLAY_NAME) ? tag.getString(TAG_DISPLAY_NAME) : SpellNames.fallback(key);
        int cost = tag.contains(TAG_MANA_COST) ? tag.getInt(TAG_MANA_COST) : spell == null ? 0 : spell.manaCost();
        int cooldown = tag.contains(TAG_COOLDOWN) ? tag.getInt(TAG_COOLDOWN) : spell == null ? 0 : spell.cooldownTicks();
        CompoundTag upgrades = tag.getCompound(TAG_UPGRADES);
        boolean summonSpell = SpellRegistry.payloadParts(key).stream().anyMatch(payload -> payload.startsWith("summon"));
        int damage = upgrades.getInt("damage");
        int summonDefense = upgrades.contains("summon_defense") ? upgrades.getInt("summon_defense") : summonSpell ? damage / 2 : 0;
        int summonAttack = upgrades.contains("summon_attack") ? upgrades.getInt("summon_attack") : summonSpell ? damage : 0;
        return new SpellInstance(
                key,
                name,
                cost,
                cooldown,
                tag.contains(TAG_LEVEL) ? tag.getInt(TAG_LEVEL) : 1,
                tag.getInt(TAG_XP),
                tag.getInt(TAG_POINTS),
                upgrades.getInt("chaining"),
                upgrades.getInt("chaining_entities"),
                upgrades.getInt("chaining_radius"),
                upgrades.getInt("chaining_damage"),
                damage,
                upgrades.getInt("healing"),
                upgrades.getInt("radius"),
                upgrades.getInt("multistrike"),
                upgrades.getInt("multistrike_casts"),
                upgrades.getInt("range"),
                upgrades.getInt("duration"),
                upgrades.getInt("summon_health"),
                summonDefense,
                upgrades.getInt("summon_armor"),
                summonAttack,
                upgrades.getInt("summon_speed")
        );
    }

    public static SpellInstance fromItem(ItemStack stack) {
        return fromTag(stack.getOrCreateTag());
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        writeTo(tag);
        return tag;
    }

    public void writeTo(CompoundTag tag) {
        tag.putString(CraftSpellPacket.TAG_SPELL_KEY, key);
        tag.putString(TAG_DISPLAY_NAME, displayName);
        tag.putInt(TAG_MANA_COST, manaCost);
        tag.putInt(TAG_COOLDOWN, cooldownTicks);
        tag.putInt(TAG_LEVEL, level);
        tag.putInt(TAG_XP, xp);
        tag.putInt(TAG_POINTS, attributePoints);

        CompoundTag upgrades = new CompoundTag();
        upgrades.putInt("chaining", chaining);
        upgrades.putInt("chaining_entities", chainingEntities);
        upgrades.putInt("chaining_radius", chainingRadius);
        upgrades.putInt("chaining_damage", chainingDamage);
        upgrades.putInt("damage", damage);
        upgrades.putInt("healing", healing);
        upgrades.putInt("radius", radius);
        upgrades.putInt("multistrike", multistrike);
        upgrades.putInt("multistrike_casts", multistrikeCasts);
        upgrades.putInt("range", range);
        upgrades.putInt("duration", duration);
        upgrades.putInt("summon_health", summonHealth);
        upgrades.putInt("summon_defense", summonDefense);
        upgrades.putInt("summon_armor", summonArmor);
        upgrades.putInt("summon_attack", summonAttack);
        upgrades.putInt("summon_speed", summonSpeed);
        tag.put(TAG_UPGRADES, upgrades);
    }

    public void writeToItem(ItemStack stack) {
        writeTo(stack.getOrCreateTag());
        stack.setHoverName(Component.literal(displayName).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    public WandData.WandSpellData toWandSpellData() {
        return new WandData.WandSpellData(
                key,
                displayName,
                manaCost,
                cooldownTicks,
                level,
                xp,
                attributePoints,
                chaining,
                chainingEntities,
                chainingRadius,
                chainingDamage,
                damage,
                healing,
                radius,
                multistrike,
                multistrikeCasts,
                range,
                duration,
                summonHealth,
                summonDefense,
                summonArmor,
                summonAttack,
                summonSpeed,
                0L
        );
    }

    private static int ranged(RandomSource random, int min, int max) {
        min = Math.max(0, min);
        max = Math.max(min, max);
        return min + random.nextInt(max - min + 1);
    }

    private static String titleCase(String key) {
        String clean = key.replace('@', ' ').replace('+', ' ').replace('_', ' ');
        StringBuilder builder = new StringBuilder();
        for (String part : clean.split(" ")) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT)).append(part.substring(1));
        }
        return builder.length() == 0 ? key : builder.toString();
    }

    private static final class SpellNames {
        private SpellNames() {
        }

        private static String roll(String key, RandomSource random) {
            List<String> payloads = SpellRegistry.payloadParts(key);
            if (payloads.isEmpty()) {
                return fallback(key);
            }

            String primary = pick(names(payloads.get(0)), random);
            if (payloads.size() == 1) {
                return primary + " " + shapeNoun(SpellRegistry.shapeKey(key));
            }

            StringBuilder name = new StringBuilder(possessive(primary));
            for (int i = 1; i < payloads.size(); i++) {
                name.append(' ').append(pick(adjectives(payloads.get(i)), random));
            }
            name.append(' ').append(shapeNoun(SpellRegistry.shapeKey(key)));
            return name.toString();
        }

        private static String fallback(String key) {
            List<String> payloads = SpellRegistry.payloadParts(key);
            if (payloads.isEmpty()) {
                return titleCase(key);
            }
            StringBuilder name = new StringBuilder(names(payloads.get(0)).get(0));
            for (int i = 1; i < payloads.size(); i++) {
                name.append(' ').append(adjectives(payloads.get(i)).get(0));
            }
            name.append(' ').append(shapeNoun(SpellRegistry.shapeKey(key)));
            return name.toString();
        }

        private static String pick(List<String> values, RandomSource random) {
            return values.get(random.nextInt(values.size()));
        }

        private static String possessive(String value) {
            return value.endsWith("s") ? value + "'" : value + "'s";
        }

        private static String shapeNoun(String shape) {
            return switch (shape) {
                case "self" -> "Charm";
                case "target" -> "Spell";
                case "point" -> "Cast";
                case "block" -> "Rune";
                case "aoe", "ally_aoe" -> "Aura";
                case "target_aoe", "ally_target_aoe" -> "Burst";
                default -> "Spell";
            };
        }

        private static List<String> names(String payload) {
            return switch (payload) {
                case "lightning" -> List.of("Lightning", "Flash", "Thor", "Stormcall", "Volt");
                case "explode" -> List.of("Explosion", "Rupture", "Detonation", "Nova", "Shatter");
                case "fire", "fireball" -> List.of("Flame", "Ember", "Inferno", "Cinder", "Pyre");
                case "frost" -> List.of("Frost", "Rime", "Glacier", "Winter", "Ice");
                case "arrow" -> List.of("Arrow", "Bolt", "Volley", "Quill", "Piercer");
                case "bind" -> List.of("Binding", "Snare", "Chain", "Tether", "Lock");
                case "heal" -> List.of("Mending", "Grace", "Renewal", "Mercy", "Bloom");
                case "summon", "summon_random" -> List.of("Summoning", "Calling", "Conjuration", "Pact", "Muster");
                case "scry" -> List.of("Scrying", "Sight", "Watcher", "Omen", "Farseeing");
                case "reflect" -> List.of("Reflect", "Reversal", "Aegis", "Reprisal", "Turnspell");
                default -> List.of(titleCase(payload));
            };
        }

        private static List<String> adjectives(String payload) {
            return switch (payload) {
                case "lightning" -> List.of("Storming", "Flashing", "Voltaic", "Thundering");
                case "explode" -> List.of("Explosive", "Shattering", "Rupturing", "Volatile");
                case "fire", "fireball" -> List.of("Burning", "Infernal", "Embered", "Scorching");
                case "frost" -> List.of("Frozen", "Glacial", "Rimebound", "Chilling");
                case "arrow" -> List.of("Piercing", "Volleying", "Barbed", "Seeking");
                case "bind" -> List.of("Binding", "Chaining", "Snaring", "Tethering");
                case "heal" -> List.of("Mending", "Restoring", "Renewing", "Merciful");
                case "gravity" -> List.of("Crushing", "Heavy", "Gravitic", "Falling");
                case "hex" -> List.of("Hexed", "Ominous", "Marked", "Dire");
                case "silence" -> List.of("Silent", "Muted", "Quieting", "Still");
                default -> List.of(titleCase(payload));
            };
        }
    }
}
