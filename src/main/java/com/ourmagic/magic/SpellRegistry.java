package com.ourmagic.magic;

import com.ourmagic.magic.spell.PayloadSpell;
import com.ourmagic.magic.spell.effect.ArrowPayload;
import com.ourmagic.magic.spell.effect.BlindPayload;
import com.ourmagic.magic.spell.effect.BlastPayload;
import com.ourmagic.magic.spell.effect.BlinkPayload;
import com.ourmagic.magic.spell.effect.BubblePayload;
import com.ourmagic.magic.spell.effect.CompositePayload;
import com.ourmagic.magic.spell.effect.ExplosionPayload;
import com.ourmagic.magic.spell.effect.FirePlacementPayload;
import com.ourmagic.magic.spell.effect.FireballPayload;
import com.ourmagic.magic.spell.effect.FreezePayload;
import com.ourmagic.magic.spell.effect.GatherPayload;
import com.ourmagic.magic.spell.effect.HealPayload;
import com.ourmagic.magic.spell.effect.LevitatePayload;
import com.ourmagic.magic.spell.effect.LightningPayload;
import com.ourmagic.magic.spell.effect.MissilePayload;
import com.ourmagic.magic.spell.effect.PayloadEffect;
import com.ourmagic.magic.spell.effect.PhysicalShieldPayload;
import com.ourmagic.magic.spell.effect.PushPayload;
import com.ourmagic.magic.spell.effect.RegeneratePayload;
import com.ourmagic.magic.spell.effect.SpellShape;
import com.ourmagic.magic.spell.effect.SpellShapes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public final class SpellRegistry {
    private static final Map<String, Spell> SPELLS = new LinkedHashMap<>();
    private static final Map<String, SpellPart> PARTS = new LinkedHashMap<>();
    private static final Map<String, ShapePart> SHAPES = new LinkedHashMap<>();
    private static final List<RandomSpellRecipe> RANDOM_RECIPES = List.of(
            random("missile", "target", 8),
            random("missile", "target_aoe", 2),
            random("arrow", "target", 7),
            random("push", "target", 6),
            random("push", "target_aoe", 2),
            random("blast", "point", 5),
            random("blast", "target_aoe", 2),
            random("frost", "water_target_aoe", 4),
            random("bubble", "self", 4),
            random("fireball", "point", 5),
            random("fireball", "target", 4),
            random("fire", "target", 4),
            random("fire", "target_aoe", 2),
            random("fire", "point", 2),
            random("blind", "target", 4),
            random("blind", "target_aoe", 2),
            random("shield", "self", 3),
            random("shield", "block", 1),
            random("heal", "ally_self_aoe", 5),
            random("heal", "target", 3),
            random("heal", "ally_target_aoe", 3),
            random("heal", "self", 2),
            random("blink", "point", 3),
            random("levitate", "target", 3),
            random("levitate", "self", 2),
            random("lightning", "target", 3),
            random("lightning", "target_aoe", 1),
            random("regenerate", "ally_self_aoe", 3),
            random("regenerate", "target", 2),
            random("regenerate", "ally_target_aoe", 2),
            random("explode", "target", 1),
            random("explode", "target_aoe", 1),
            random("explode", "point", 1),
            random("lightning+explode", "target", 1),
            random("blind+explode", "target", 1)
    );

    static {
        registerParts();
        registerShapes();
        registerDefaults();
    }

    private SpellRegistry() {
    }

    public static Spell get(String key) {
        Spell spell = SPELLS.get(key);
        if (spell != null) {
            return spell;
        }

        spell = buildDynamic(key);
        if (spell != null) {
            SPELLS.put(key, spell);
        }
        return spell;
    }

    public static Collection<Spell> all() {
        return SPELLS.values();
    }

    public static List<String> payloadKeys() {
        return List.copyOf(PARTS.keySet());
    }

    public static List<String> shapeKeys() {
        return List.copyOf(SHAPES.keySet());
    }

    public static String payloadKey(String spellKey) {
        Recipe recipe = Recipe.parse(spellKey);
        return recipe == null ? "" : String.join("+", recipe.payloads());
    }

    public static List<String> payloadParts(String spellKey) {
        Recipe recipe = Recipe.parse(spellKey);
        return recipe == null ? List.of() : recipe.payloads();
    }

    public static String shapeKey(String spellKey) {
        Recipe recipe = Recipe.parse(spellKey);
        return recipe == null ? "" : recipe.shape();
    }

    public static String withShape(String spellKey, String shape) {
        Recipe recipe = Recipe.parse(spellKey);
        if (recipe == null || !isShapeAllowed(recipe.payloads(), shape)) {
            return "";
        }

        String key = String.join("+", recipe.payloads()) + "@" + shape;
        return get(key) == null ? "" : key;
    }

    public static boolean isValidRecipe(String spellKey) {
        Recipe recipe = Recipe.parse(spellKey);
        return recipe != null && isShapeAllowed(recipe.payloads(), recipe.shape()) && get(spellKey) != null;
    }

    public static List<String> allowedShapes(String spellKey) {
        Recipe recipe = Recipe.parse(spellKey);
        return recipe == null ? List.of() : allowedShapesForPayloads(recipe.payloads());
    }

    public static List<String> allowedShapesForPayloads(List<String> payloads) {
        return SHAPES.keySet().stream()
                .filter(shape -> isShapeAllowed(payloads, shape))
                .toList();
    }

    public static Spell randomSpell(RandomSource random) {
        int totalWeight = 0;
        for (RandomSpellRecipe recipe : RANDOM_RECIPES) {
            totalWeight += recipe.weight();
        }

        int roll = random.nextInt(Math.max(1, totalWeight));
        for (RandomSpellRecipe recipe : RANDOM_RECIPES) {
            roll -= recipe.weight();
            if (roll < 0) {
                return get(recipe.key());
            }
        }

        return get("missile@self");
    }

    private static void registerParts() {
        part("arrow", ArrowPayload::new, shapes("target", "target_aoe", "target_area", "aoe"), 10, 14, ParticleTypes.ENCHANTED_HIT, Spell.UPGRADE_MULTISTRIKE);
        part("blast", BlastPayload::new, shapes("target", "point", "target_aoe", "target_area", "aoe"), 24, 50, ParticleTypes.EXPLOSION, Spell.UPGRADE_MULTISTRIKE);
        part("blind", BlindPayload::new, shapes("target", "target_aoe", "target_area", "aoe"), 15, 35, ParticleTypes.SQUID_INK, Spell.UPGRADE_DURATION, Spell.UPGRADE_CHAINING);
        part("blink", BlinkPayload::new, shapes("self", "point"), 28, 70, ParticleTypes.PORTAL, Spell.UPGRADE_RANGE);
        part("bubble", BubblePayload::new, shapes("self", "ally_self_aoe"), 10, 30, ParticleTypes.BUBBLE, Spell.UPGRADE_DURATION);
        part("explode", ExplosionPayload::new, shapes("self","target", "point", "target_aoe", "target_area", "aoe"), 30, 70, ParticleTypes.EXPLOSION, Spell.UPGRADE_MULTISTRIKE, Spell.UPGRADE_CHAINING);
        part("fire", FireballPayload::new, shapes("target", "point", "target_aoe", "target_area", "aoe"), 18, 30, ParticleTypes.FLAME, Spell.UPGRADE_MULTISTRIKE, Spell.UPGRADE_CHAINING);
        part("fire_place", FirePlacementPayload::new, shapes("block"), 12, 20, ParticleTypes.FLAME);
        part("fireball", FireballPayload::new, shapes( "target", "point", "target_aoe", "target_area", "aoe"), 18, 30, ParticleTypes.FLAME, Spell.UPGRADE_MULTISTRIKE, Spell.UPGRADE_CHAINING);
        part("frost", FreezePayload::new, shapes("target_aoe", "target_area", "aoe", "water_target_aoe"), 14, 24, ParticleTypes.SNOWFLAKE, Spell.UPGRADE_DURATION);
        part("gather", GatherPayload::new, shapes("items_self_aoe"), 15, 30, ParticleTypes.ENCHANT);
        part("heal", () -> new HealPayload(6.0F, 4.0F, 8), shapes("self", "target", "ally_self_aoe", "ally_target_aoe"), 20, 60, ParticleTypes.HAPPY_VILLAGER, Spell.UPGRADE_RANGE);
        part("levitate", LevitatePayload::new, shapes("self", "target"), 22, 45, ParticleTypes.END_ROD, Spell.UPGRADE_DURATION);
        part("lightning", LightningPayload::new, shapes("target", "target_aoe", "target_area", "aoe"), 35, 90, ParticleTypes.ELECTRIC_SPARK, Spell.UPGRADE_CHAINING, Spell.UPGRADE_MULTISTRIKE);
        part("missile", MissilePayload::new, shapes("self", "target", "target_aoe", "target_area", "aoe"), 8, 12, MissilePayload.PURPLE_PARTICLE, Spell.UPGRADE_MULTISTRIKE);
        part("push", PushPayload::new, shapes("target", "target_aoe", "target_area", "aoe"), 12, 18, ParticleTypes.CLOUD, Spell.UPGRADE_MULTISTRIKE);
        part("regenerate", RegeneratePayload::new, shapes("target", "ally_self_aoe", "ally_target_aoe"), 35, 100, ParticleTypes.HAPPY_VILLAGER, Spell.UPGRADE_RANGE, Spell.UPGRADE_DURATION);
        part("shield", PhysicalShieldPayload::new, shapes("self", "block"), 18, 80, ParticleTypes.ENCHANT, Spell.UPGRADE_DURATION);
    }

    private static void registerShapes() {
        shape("self", particle -> SpellShapes.self());
        shape("target", particle -> SpellShapes.lookedLivingOrPoint(30, 20));
        shape("point", particle -> SpellShapes.lookedPoint(30, 20));
        shape("block", particle -> SpellShapes.lookedBlockAdjacent(18));
        shape("self_aoe", particle -> SpellShapes.livingAroundSelf(4, 8, particle));
        shape("self_area", SHAPES.get("self_aoe").factory());
        shape("target_aoe", particle -> SpellShapes.livingAroundLookedPoint(30, 20, 4, 8, particle));
        shape("target_area", SHAPES.get("target_aoe").factory());
        shape("aoe", SHAPES.get("target_aoe").factory());
        shape("ally_self_aoe", particle -> SpellShapes.playersAroundSelf(4, particle));
        shape("ally_target_aoe", particle -> SpellShapes.playersAroundLookedLivingOrSelf(18, 4, particle));
        shape("items_self_aoe", particle -> SpellShapes.itemsAroundSelf(10, particle));
        shape("water_target_aoe", particle -> SpellShapes.waterBlocksAroundLookedLivingOrPoint(18, 18, 1, 1, 0, particle));
    }

    private static void registerDefaults() {
        registerRecipe("missile@self");
        registerRecipe("push@target");
        registerRecipe("arrow@target");
        registerRecipe("blast@target");
        registerRecipe("frost@water_target_aoe");
        registerRecipe("bubble@self");
        registerRecipe("fireball@self");
        registerRecipe("blind@target");
        registerRecipe("shield@self");
        registerRecipe("heal@ally_self_aoe");
        registerRecipe("blink@point");
        registerRecipe("levitate@target");
        registerRecipe("lightning@target");
        registerRecipe("fire_place@block");
        registerRecipe("gather@items_self_aoe");
        registerRecipe("regenerate@ally_self_aoe");
    }

    private static void registerRecipe(String recipe) {
        Spell spell = build(recipe, recipe);
        if (spell != null) {
            SPELLS.put(recipe, spell);
        }
    }

    private static Spell buildDynamic(String key) {
        if (!key.contains("@")) {
            return null;
        }
        return build(key, key);
    }

    private static Spell build(String key, String recipe) {
        Recipe parsed = Recipe.parse(recipe);
        if (parsed == null) {
            return null;
        }
        if (!isShapeAllowed(parsed.payloads(), parsed.shape())) {
            return null;
        }

        ShapePart shape = SHAPES.get(parsed.shape());
        if (shape == null) {
            return null;
        }

        List<PayloadEffect> payloads = new ArrayList<>();
        Set<String> upgrades = new LinkedHashSet<>();
        int manaCost = 0;
        int cooldownTicks = 0;
        ParticleOptions particle = ParticleTypes.ENCHANT;

        for (String payloadKey : parsed.payloads()) {
            SpellPart part = PARTS.get(payloadKey);
            if (part == null) {
                return null;
            }

            payloads.add(part.payload().get());
            upgrades.addAll(part.upgrades());
            manaCost += part.manaCost();
            cooldownTicks += part.cooldownTicks();
            particle = part.particle();
        }

        if (parsed.shape().contains("aoe") || parsed.shape().contains("area")) {
            upgrades.add(Spell.UPGRADE_RADIUS);
        }

        PayloadEffect payload = payloads.size() == 1 ? payloads.get(0) : new CompositePayload(payloads.toArray(PayloadEffect[]::new));
        SpellShape spellShape = shape.factory().apply(particle);
        return new PayloadSpell(key, Math.max(1, manaCost), Math.max(1, cooldownTicks), spellShape, payload, particle, upgrades.toArray(String[]::new));
    }

    private static void part(String key, Supplier<PayloadEffect> payload, ShapeCompatibility shapes, int manaCost, int cooldownTicks, ParticleOptions particle, String... upgrades) {
        PARTS.put(key, new SpellPart(payload, shapes, manaCost, cooldownTicks, particle, Set.of(upgrades)));
    }

    private static void shape(String key, Function<ParticleOptions, SpellShape> factory) {
        SHAPES.put(key, new ShapePart(factory));
    }

    private static RandomSpellRecipe random(String payloads, String shape, int weight) {
        return new RandomSpellRecipe(payloads + "@" + shape, weight);
    }

    private static ShapeCompatibility shapes(String... shapes) {
        Set<String> allowedShapes = Set.of(shapes);
        return allowedShapes::contains;
    }

    private static boolean isShapeAllowed(List<String> payloads, String shape) {
        if (payloads.isEmpty() || !SHAPES.containsKey(shape)) {
            return false;
        }

        for (String payload : payloads) {
            SpellPart part = PARTS.get(payload);
            if (part == null || !part.shapes().allows(shape)) {
                return false;
            }
        }
        return true;
    }

    private interface ShapeCompatibility {
        boolean allows(String shape);
    }

    private record SpellPart(Supplier<PayloadEffect> payload, ShapeCompatibility shapes, int manaCost, int cooldownTicks, ParticleOptions particle, Set<String> upgrades) {
    }

    private record ShapePart(Function<ParticleOptions, SpellShape> factory) {
    }

    private record RandomSpellRecipe(String key, int weight) {
    }

    private record Recipe(List<String> payloads, String shape) {
        private static Recipe parse(String recipe) {
            String[] split = recipe.split("@", 2);
            if (split.length != 2 || split[0].isBlank() || split[1].isBlank()) {
                return null;
            }

            List<String> payloads = new ArrayList<>();
            for (String payload : split[0].split("\\+")) {
                String normalized = payload.trim().toLowerCase();
                if (normalized.isEmpty()) {
                    return null;
                }
                payloads.add(normalized);
            }

            return new Recipe(payloads, split[1].trim().toLowerCase());
        }
    }
}
