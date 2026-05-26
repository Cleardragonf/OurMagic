package com.ourmagic.magic;

import com.ourmagic.magic.spell.PayloadSpell;
import com.ourmagic.magic.spell.payloads.ArrowPayload;
import com.ourmagic.magic.spell.payloads.AgilityPayload;
import com.ourmagic.magic.spell.payloads.BindPayload;
import com.ourmagic.magic.spell.payloads.BlindPayload;
import com.ourmagic.magic.spell.payloads.BlastPayload;
import com.ourmagic.magic.spell.payloads.BlinkPayload;
import com.ourmagic.magic.spell.payloads.BubblePayload;
import com.ourmagic.magic.spell.payloads.AnchorPayload;
import com.ourmagic.magic.spell.payloads.CharmPayload;
import com.ourmagic.magic.spell.payloads.CleansePayload;
import com.ourmagic.magic.spell.payloads.ConjurePayload;
import com.ourmagic.magic.spell.payloads.CompositePayload;
import com.ourmagic.magic.spell.payloads.CursePayload;
import com.ourmagic.magic.spell.payloads.DisarmPayload;
import com.ourmagic.magic.spell.payloads.EchoPayload;
import com.ourmagic.magic.spell.payloads.ExplosionPayload;
import com.ourmagic.magic.spell.payloads.BlastguardPayload;
import com.ourmagic.magic.spell.payloads.FallguardPayload;
import com.ourmagic.magic.spell.payloads.FireguardPayload;
import com.ourmagic.magic.spell.payloads.FirePlacementPayload;
import com.ourmagic.magic.spell.payloads.FirePayload;
import com.ourmagic.magic.spell.payloads.FireballPayload;
import com.ourmagic.magic.spell.payloads.FortifyPayload;
import com.ourmagic.magic.spell.payloads.FreezePayload;
import com.ourmagic.magic.spell.payloads.GatherPayload;
import com.ourmagic.magic.spell.payloads.GravityPayload;
import com.ourmagic.magic.spell.payloads.HexPayload;
import com.ourmagic.magic.spell.payloads.HealPayload;
import com.ourmagic.magic.spell.payloads.IllusionPayload;
import com.ourmagic.magic.spell.payloads.LevitatePayload;
import com.ourmagic.magic.spell.payloads.LifedrainPayload;
import com.ourmagic.magic.spell.payloads.LightningPayload;
import com.ourmagic.magic.spell.payloads.LifeWardPayload;
import com.ourmagic.magic.spell.payloads.ManaBurnPayload;
import com.ourmagic.magic.spell.payloads.ManaShieldPayload;
import com.ourmagic.magic.spell.payloads.MissilePayload;
import com.ourmagic.magic.spell.payloads.NullifyPayload;
import com.ourmagic.magic.spell.payloads.OverloadPayload;
import com.ourmagic.magic.spell.payloads.PayloadEffect;
import com.ourmagic.magic.spell.payloads.PhasePayload;
import com.ourmagic.magic.spell.payloads.PhysicalShieldPayload;
import com.ourmagic.magic.spell.payloads.PushPayload;
import com.ourmagic.magic.spell.payloads.RecallPayload;
import com.ourmagic.magic.spell.payloads.RegeneratePayload;
import com.ourmagic.magic.spell.payloads.ReflectPayload;
import com.ourmagic.magic.spell.payloads.RevealPayload;
import com.ourmagic.magic.spell.payloads.RunePayload;
import com.ourmagic.magic.spell.payloads.SanctuaryPayload;
import com.ourmagic.magic.spell.payloads.ScryPayload;
import com.ourmagic.magic.spell.payloads.SilencePayload;
import com.ourmagic.magic.spell.payloads.SummonPayload;
import com.ourmagic.magic.spell.payloads.TransmutePayload;
import com.ourmagic.magic.spell.payloads.WardPayload;
import com.ourmagic.magic.spell.payloads.WardAreaPayload;
import com.ourmagic.magic.spell.runtime.SpellBuildContext;
import com.ourmagic.magic.spell.shapes.SpellShape;
import com.ourmagic.magic.spell.shapes.SpellShapes;
import com.ourmagic.magic.spell.payloads.StunPayload;
import com.ourmagic.magic.spell.payloads.WarpPayload;
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
    private static List<ArcaneKnowledgeRecipe> arcaneKnowledgeRecipes;
    private static final Set<String> SOLO_PAYLOADS = Set.of(
            "blink",
            "cleanse",
            "conjure",
            "fire_place",
            "gather",
            "illusion",
            "phase",
            "recall",
            "reflect",
            "sanctuary",
            "scry",
            "shield",
            "summon",
            "summon_random",
            "summon_undead",
            "summon_beast",
            "summon_guardian",
            "summon_arcane",
            "summon_swarm",
            "transmute",
            "warp"
    );
    private static final Map<String, Set<String>> PAYLOAD_COMBOS = payloadCombos();
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
            random("fireball", "point", 2),
            random("blind", "target", 4),
            random("blind", "target_aoe", 2),
            random("shield", "self", 3),
            random("shield", "block", 1),
            random("heal", "ally_aoe", 5),
            random("heal", "target", 3),
            random("heal", "ally_target_aoe", 3),
            random("heal", "self", 2),
            random("blink", "point", 3),
            random("levitate", "target", 3),
            random("levitate", "self", 2),
            random("lightning", "target", 3),
            random("lightning", "target_aoe", 1),
            random("regenerate", "ally_aoe", 3),
            random("regenerate", "target", 2),
            random("regenerate", "ally_target_aoe", 2),
            random("fortify", "self", 3),
            random("fortify", "ally_aoe", 3),
            random("fireguard", "self", 2),
            random("fallguard", "self", 2),
            random("blastguard", "self", 2),
            random("agility", "self", 3),
            random("mana_shield", "self", 2),
            random("life_ward", "self", 2),
            random("explode", "target", 1),
            random("explode", "target_aoe", 1),
            random("explode", "point", 1),
            random("lightning+explode", "target", 1),
            random("blind+explode", "target", 1),
            random("bind", "target", 2),
            random("nullify", "self", 3),
            random("nullify", "target", 2),
            random("stun", "target", 1),
            random("warp", "self", 1),
            random("ward", "self", 3),
            random("ward", "ally_aoe", 2),
            random("gravity", "target_aoe", 2),
            random("silence", "target", 2),
            random("charm", "target", 2),
            random("phase", "self", 2),
            random("lifedrain", "target", 2),
            random("curse", "target", 2),
            random("reveal", "target_aoe", 2),
            random("cleanse", "ally_aoe", 2),
            random("disarm", "target", 1),
            random("rune+bind", "block", 1),
            random("rune+explode", "block", 1),
            random("echo+missile", "target", 1),
            random("echo+heal", "target", 1),
            random("recall", "self", 1),
            random("illusion", "self", 2),
            random("reflect", "self", 2),
            random("transmute", "block", 2),
            random("anchor", "target", 2),
            random("manaburn", "target", 2),
            random("overload", "target", 2),
            random("conjure", "point", 2),
            random("scry", "target", 2),
            random("scry", "self", 1),
            random("hex", "target", 2),
            random("sanctuary", "aoe", 2),
            random("summon_random", "point", 2),
            random("summon_undead", "point", 2),
            random("summon_beast", "point", 2),
            random("summon_guardian", "point", 1),
            random("summon_arcane", "point", 1),
            random("summon_swarm", "point", 1)
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
        if (recipe == null || !isPayloadCombinationAllowed(recipe.payloads()) || !isShapeAllowed(recipe.payloads(), shape)) {
            return "";
        }

        String key = String.join("+", recipe.payloads()) + "@" + shape;
        return get(key) == null ? "" : key;
    }

    public static boolean isValidRecipe(String spellKey) {
        Recipe recipe = Recipe.parse(spellKey);
        return recipe != null && isPayloadCombinationAllowed(recipe.payloads()) && isShapeAllowed(recipe.payloads(), recipe.shape()) && get(spellKey) != null;
    }

    public static List<PayloadEffect> payloadEffectsExcluding(String spellKey, String excludedPayload) {
        return payloadEffectsExcluding(spellKey, Set.of(excludedPayload));
    }

    public static List<PayloadEffect> payloadEffectsExcluding(String spellKey, Set<String> excludedPayloads) {
        Recipe recipe = Recipe.parse(spellKey);
        if (recipe == null || !isValidRecipe(spellKey)) {
            return List.of();
        }

        List<PayloadEffect> payloads = new ArrayList<>();
        int payloadIndex = 0;
        for (String payloadKey : recipe.payloads()) {
            if (excludedPayloads.contains(payloadKey)) {
                continue;
            }
            SpellPart part = PARTS.get(payloadKey);
            if (part == null) {
                return List.of();
            }
            PayloadEffect payload = part.payload().get();
            SpellBuildContext buildContext = new SpellBuildContext(spellKey, payloadKey, recipe.shape(), recipe.payloads(), payloadIndex++);
            payload.particle(buildContext);
            payloads.add(payload);
        }
        return List.copyOf(payloads);
    }

    public static List<String> allowedShapes(String spellKey) {
        Recipe recipe = Recipe.parse(spellKey);
        return recipe == null ? List.of() : allowedShapesForPayloads(recipe.payloads());
    }

    public static List<String> allowedShapesForPayloads(List<String> payloads) {
        if (!isPayloadCombinationAllowed(payloads)) {
            return List.of();
        }
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

    public static Spell randomArcaneKnowledgeSpell(RandomSource random) {
        List<ArcaneKnowledgeRecipe> recipes = arcaneKnowledgeRollTable();
        if (recipes.isEmpty()) {
            return randomSpell(random);
        }

        int totalWeight = 0;
        for (ArcaneKnowledgeRecipe recipe : recipes) {
            totalWeight += recipe.weight();
        }

        int roll = random.nextInt(Math.max(1, totalWeight));
        for (ArcaneKnowledgeRecipe recipe : recipes) {
            roll -= recipe.weight();
            if (roll < 0) {
                return get(recipe.key());
            }
        }

        return randomSpell(random);
    }

    public static List<String> arcaneKnowledgeRecipes() {
        return arcaneKnowledgeRollTable().stream().map(ArcaneKnowledgeRecipe::key).toList();
    }

    private static List<ArcaneKnowledgeRecipe> arcaneKnowledgeRollTable() {
        if (arcaneKnowledgeRecipes == null) {
            arcaneKnowledgeRecipes = buildArcaneKnowledgeRecipes();
        }
        return arcaneKnowledgeRecipes;
    }

    private static void registerParts() {
        part("anchor", AnchorPayload::new, shapes("target", "target_aoe", "aoe"), mana(14, 28), cooldown(42, 84));
        part("agility", AgilityPayload::new, shapes("self", "target", "ally_aoe", "ally_target_aoe"), mana(12, 23), cooldown(42, 84));
        part("arrow", ArrowPayload::new, shapes("target", "target_aoe", "aoe"), mana(7, 14), cooldown(10, 20), true);
        part("bind", BindPayload::new, shapes("target", "target_aoe", "aoe"), mana(15, 30), cooldown(39, 77), true);
        part("blast", BlastPayload::new, shapes("target", "point", "block", "target_aoe", "aoe"), mana(17, 32), cooldown(35, 70), true);
        part("blind", BlindPayload::new, shapes("target", "target_aoe", "aoe"), mana(11, 20), cooldown(25, 49));
        part("blink", BlinkPayload::new, shapes("self", "point"), mana(20, 38), cooldown(49, 98));
        part("blastguard", BlastguardPayload::new, shapes("self", "target", "ally_aoe", "ally_target_aoe"), mana(18, 35), cooldown(56, 112));
        part("bubble", BubblePayload::new, shapes("self", "ally_aoe"), mana(7, 14), cooldown(21, 42));
        part("charm", CharmPayload::new, shapes("target", "target_aoe", "aoe"), mana(16, 31), cooldown(42, 84));
        part("cleanse", CleansePayload::new, shapes("self", "target", "ally_aoe", "ally_target_aoe"), mana(13, 25), cooldown(35, 70));
        part("conjure", ConjurePayload::new, shapes("point", "block"), mana(21, 40), cooldown(63, 126));
        part("curse", CursePayload::new, shapes("target", "target_aoe", "aoe"), mana(15, 29), cooldown(42, 84));
        part("disarm", DisarmPayload::new, shapes("target"), mana(18, 35), cooldown(56, 112), true);
        part("echo", EchoPayload::new, shapes("self", "target", "point", "block", "target_aoe", "aoe", "ally_aoe", "ally_target_aoe"), mana(8, 16), cooldown(18, 36));
        part("explode", ExplosionPayload::new, shapes("self", "target", "point", "block", "target_aoe", "aoe"), mana(21, 41), cooldown(49, 98), true);
        part("fallguard", FallguardPayload::new, shapes("self", "target", "ally_aoe", "ally_target_aoe"), mana(12, 23), cooldown(42, 84));
        part("fire", FirePayload::new, shapes("target", "target_aoe", "aoe"), mana(10, 19), cooldown(25, 49), true);
        part("fireguard", FireguardPayload::new, shapes("self", "target", "ally_aoe", "ally_target_aoe"), mana(12, 23), cooldown(42, 84));
        part("fire_place", FirePlacementPayload::new, shapes("block"), mana(8, 16), cooldown(14, 28));
        part("fireball", FireballPayload::new, shapes("self", "target", "point", "target_aoe", "aoe"), mana(13, 24), cooldown(21, 42), true);
        part("fortify", FortifyPayload::new, shapes("self", "target", "ally_aoe", "ally_target_aoe"), mana(18, 35), cooldown(56, 112));
        part("frost", FreezePayload::new, shapes("block", "target_aoe", "aoe", "water_target_aoe"), mana(10, 19), cooldown(17, 34), true);
        part("gather", GatherPayload::new, shapes("items_self_aoe"), mana(11, 20), cooldown(21, 42));
        part("gravity", GravityPayload::new, shapes("target", "point", "block", "target_aoe", "aoe"), mana(18, 35), cooldown(49, 98), true);
        part("hex", HexPayload::new, shapes("target", "target_aoe", "aoe"), mana(14, 28), cooldown(35, 70));
        part("heal", () -> new HealPayload(6.0F, 4.0F, 8), shapes("self", "target", "ally_aoe", "ally_target_aoe"), mana(14, 27), cooldown(42, 84));
        part("illusion", IllusionPayload::new, shapes("self", "point", "target_aoe", "aoe"), mana(13, 25), cooldown(35, 70));
        part("levitate", LevitatePayload::new, shapes("self", "target"), mana(15, 30), cooldown(32, 63), true);
        part("lifedrain", LifedrainPayload::new, shapes("target"), mana(18, 35), cooldown(42, 84));
        part("life_ward", LifeWardPayload::new, shapes("self", "target", "ally_aoe", "ally_target_aoe"), mana(24, 46), cooldown(84, 168));
        part("lightning", LightningPayload::new, shapes("target", "target_aoe", "aoe"), mana(25, 47), cooldown(63, 126), true);
        part("manaburn", ManaBurnPayload::new, shapes("target"), mana(18, 35), cooldown(49, 98));
        part("mana_shield", ManaShieldPayload::new, shapes("self", "target", "ally_aoe", "ally_target_aoe"), mana(16, 31), cooldown(56, 112));
        part("missile", MissilePayload::new, shapes("self", "target", "target_aoe", "aoe"), mana(6, 11), cooldown(8, 17), true);
        part("reflect", ReflectPayload::new, shapes("self", "target", "ally_aoe", "ally_target_aoe"), mana(16, 31), cooldown(56, 112));
        part("nullify", NullifyPayload::new, shapes("self", "target", "ally_aoe", "ally_target_aoe"), mana(14, 27), cooldown(42, 84));
        part("overload", OverloadPayload::new, shapes("target", "target_aoe", "aoe"), mana(20, 39), cooldown(56, 112));
        part("phase", PhasePayload::new, shapes("self", "target"), mana(20, 38), cooldown(63, 126));
        part("push", PushPayload::new, shapes("target", "target_aoe", "aoe"), mana(8, 16), cooldown(13, 25), true);
        part("recall", RecallPayload::new, shapes("self"), mana(24, 46), cooldown(70, 140));
        part("regenerate", RegeneratePayload::new, shapes("target", "ally_aoe", "ally_target_aoe"), mana(25, 47), cooldown(70, 140));
        part("reveal", RevealPayload::new, shapes("target", "target_aoe", "aoe"), mana(8, 16), cooldown(21, 42));
        part("rune", RunePayload::new, shapes("block", "point"), mana(12, 24), cooldown(42, 84));
        part("sanctuary", SanctuaryPayload::new, shapes("self", "point", "target", "aoe", "ally_aoe"), mana(25, 48), cooldown(84, 168));
        part("scry", ScryPayload::new, shapes("self", "target", "point", "block"), mana(7, 14), cooldown(14, 28));
        part("shield", PhysicalShieldPayload::new, shapes("self", "block"), mana(13, 24), cooldown(56, 112));
        part("silence", SilencePayload::new, shapes("target", "target_aoe", "aoe"), mana(17, 33), cooldown(49, 98));
        part("stun", StunPayload::new, shapes("target"), mana(25, 47), cooldown(63, 126));
        part("summon", () -> new SummonPayload(SummonPayload.Variant.RANDOM), shapes("point", "block"), mana(22, 42), cooldown(70, 140));
        part("summon_random", () -> new SummonPayload(SummonPayload.Variant.RANDOM), shapes("point", "block"), mana(22, 42), cooldown(70, 140));
        part("summon_undead", () -> new SummonPayload(SummonPayload.Variant.UNDEAD), shapes("point", "block"), mana(21, 40), cooldown(70, 140));
        part("summon_beast", () -> new SummonPayload(SummonPayload.Variant.BEAST), shapes("point", "block"), mana(19, 37), cooldown(63, 126));
        part("summon_guardian", () -> new SummonPayload(SummonPayload.Variant.GUARDIAN), shapes("point", "block"), mana(28, 54), cooldown(84, 168));
        part("summon_arcane", () -> new SummonPayload(SummonPayload.Variant.ARCANE), shapes("point", "block"), mana(30, 58), cooldown(84, 168));
        part("summon_swarm", () -> new SummonPayload(SummonPayload.Variant.SWARM), shapes("point", "block"), mana(18, 35), cooldown(56, 112));
        part("transmute", TransmutePayload::new, shapes("block", "target_aoe", "aoe"), mana(12, 24), cooldown(35, 70));
        part("ward", WardPayload::new, shapes("self", "target", "ally_aoe", "ally_target_aoe", "ward_hostile", "ward_players", "ward_allies", "ward_any"), mana(15, 29), cooldown(56, 112));
        part("anti_fire", () -> new WardAreaPayload(WardAreaPayload.Variant.ANTI_FIRE), shapes("target"), mana(12, 24), cooldown(45, 90));
        part("anti_water", () -> new WardAreaPayload(WardAreaPayload.Variant.ANTI_WATER), shapes("target"), mana(12, 24), cooldown(45, 90));
        part("anti_explosion", () -> new WardAreaPayload(WardAreaPayload.Variant.ANTI_EXPLOSION), shapes("target"), mana(16, 32), cooldown(60, 120));
        part("anti_magic", () -> new WardAreaPayload(WardAreaPayload.Variant.ANTI_MAGIC), shapes("target"), mana(22, 44), cooldown(80, 160));
        part("anti_grief", () -> new WardAreaPayload(WardAreaPayload.Variant.ANTI_GRIEF), shapes("target"), mana(18, 36), cooldown(70, 140));
        part("anti_projectile", () -> new WardAreaPayload(WardAreaPayload.Variant.ANTI_PROJECTILE), shapes("target"), mana(16, 32), cooldown(60, 120));
        part("anti_teleport", () -> new WardAreaPayload(WardAreaPayload.Variant.ANTI_TELEPORT), shapes("target"), mana(18, 36), cooldown(70, 140));
        part("anti_decay", () -> new WardAreaPayload(WardAreaPayload.Variant.ANTI_DECAY), shapes("target"), mana(12, 24), cooldown(45, 90));
        part("anti_summon", () -> new WardAreaPayload(WardAreaPayload.Variant.ANTI_SUMMON), shapes("target"), mana(18, 36), cooldown(70, 140));
        part("air", () -> new WardAreaPayload(WardAreaPayload.Variant.AIR), shapes("target"), mana(14, 28), cooldown(50, 100));
        part("air_bubble", () -> new WardAreaPayload(WardAreaPayload.Variant.AIR_BUBBLE), shapes("target"), mana(18, 36), cooldown(70, 140));
        part("alarm", () -> new WardAreaPayload(WardAreaPayload.Variant.ALARM), shapes("target"), mana(10, 20), cooldown(35, 70));
        part("entry_filter", () -> new WardAreaPayload(WardAreaPayload.Variant.ENTRY_FILTER), shapes("target"), mana(18, 36), cooldown(70, 140));
        part("weather", () -> new WardAreaPayload(WardAreaPayload.Variant.WEATHER), shapes("target"), mana(14, 28), cooldown(55, 110));
        part("lockdown", () -> new WardAreaPayload(WardAreaPayload.Variant.LOCKDOWN), shapes("target"), mana(24, 48), cooldown(90, 180));
        part("item_guard", () -> new WardAreaPayload(WardAreaPayload.Variant.ITEM_GUARD), shapes("target"), mana(12, 24), cooldown(45, 90));
        part("storage_lock", () -> new WardAreaPayload(WardAreaPayload.Variant.STORAGE_LOCK), shapes("target"), mana(14, 28), cooldown(50, 100));
        part("grow", () -> new WardAreaPayload(WardAreaPayload.Variant.GROW), shapes("target"), mana(16, 32), cooldown(60, 120));
        part("freeze", () -> new WardAreaPayload(WardAreaPayload.Variant.FREEZE), shapes("target"), mana(14, 28), cooldown(55, 110));
        part("thaw", () -> new WardAreaPayload(WardAreaPayload.Variant.THAW), shapes("target"), mana(14, 28), cooldown(55, 110));
        part("weakening", () -> new WardAreaPayload(WardAreaPayload.Variant.WEAKENING), shapes("target"), mana(16, 32), cooldown(60, 120));
        part("mana_drain", () -> new WardAreaPayload(WardAreaPayload.Variant.MANA_DRAIN), shapes("target"), mana(20, 40), cooldown(80, 160));
        part("reflect_projectile", () -> new WardAreaPayload(WardAreaPayload.Variant.REFLECT_PROJECTILE), shapes("target"), mana(22, 44), cooldown(85, 170));
        part("dispel", () -> new WardAreaPayload(WardAreaPayload.Variant.DISPEL), shapes("target"), mana(20, 40), cooldown(80, 160));
        part("light", () -> new WardAreaPayload(WardAreaPayload.Variant.LIGHT), shapes("target"), mana(10, 20), cooldown(35, 70));
        part("dark", () -> new WardAreaPayload(WardAreaPayload.Variant.DARK), shapes("target"), mana(10, 20), cooldown(35, 70));
        part("temporal", () -> new WardAreaPayload(WardAreaPayload.Variant.TEMPORAL), shapes("target"), mana(18, 36), cooldown(70, 140));
        part("stasis", () -> new WardAreaPayload(WardAreaPayload.Variant.STASIS), shapes("target"), mana(18, 36), cooldown(70, 140));
        part("fertility", () -> new WardAreaPayload(WardAreaPayload.Variant.FERTILITY), shapes("target"), mana(14, 28), cooldown(60, 120));
        part("camouflage", () -> new WardAreaPayload(WardAreaPayload.Variant.CAMOUFLAGE), shapes("target"), mana(18, 36), cooldown(70, 140));
        part("warp", WarpPayload::new, shapes("self"), mana(28, 54), cooldown(84, 168));
    }

    private static void registerShapes() {
        shape("self", particle -> SpellShapes.self());
        shape("target", particle -> SpellShapes.lookedLivingOrPoint(30, 20));
        shape("point", particle -> SpellShapes.lookedPoint(30, 20));
        shape("block", particle -> SpellShapes.lookedBlockAdjacent(18));
        shape("target_aoe", particle -> SpellShapes.livingAroundLookedPoint(30, 20, 4, 8, particle));
        shape("aoe", particle -> SpellShapes.livingAroundSelf(4, 8, false, particle));
        shape("ally_aoe", particle -> SpellShapes.playersAroundSelf(4, particle));
        shape("ally_target_aoe", particle -> SpellShapes.playersAroundLookedLivingOrSelf(18, 4, particle));
        shape("items_self_aoe", particle -> SpellShapes.itemsAroundSelf(10, particle));
        shape("water_target_aoe", particle -> SpellShapes.waterBlocksAroundLookedLivingOrPoint(18, 18, 1, 1, 0, particle));
        shape("ward_hostile", particle -> SpellShapes.lookedPoint(20, 12));
        shape("ward_players", particle -> SpellShapes.lookedPoint(20, 12));
        shape("ward_allies", particle -> SpellShapes.lookedPoint(20, 12));
        shape("ward_any", particle -> SpellShapes.lookedPoint(20, 12));
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
        registerRecipe("bind@target");
        registerRecipe("shield@self");
        registerRecipe("heal@ally_aoe");
        registerRecipe("blink@point");
        registerRecipe("levitate@target");
        registerRecipe("lightning@target");
        registerRecipe("nullify@self");
        registerRecipe("stun@target");
        registerRecipe("warp@self");
        registerRecipe("fire_place@block");
        registerRecipe("gather@items_self_aoe");
        registerRecipe("regenerate@ally_aoe");
        registerRecipe("fortify@self");
        registerRecipe("fireguard@self");
        registerRecipe("fallguard@self");
        registerRecipe("blastguard@self");
        registerRecipe("agility@self");
        registerRecipe("mana_shield@self");
        registerRecipe("life_ward@self");
        registerRecipe("ward@self");
        registerRecipe("ward+bind@ward_hostile");
        registerRecipe("ward+heal@ward_allies");
        registerRecipe("ward+anti_fire@ward_any");
        registerRecipe("ward+anti_water@ward_any");
        registerRecipe("ward+anti_explosion@ward_any");
        registerRecipe("ward+anti_magic@ward_any");
        registerRecipe("ward+anti_grief@ward_any");
        registerRecipe("ward+anti_projectile@ward_any");
        registerRecipe("ward+anti_teleport@ward_any");
        registerRecipe("ward+anti_decay@ward_any");
        registerRecipe("ward+anti_summon@ward_any");
        registerRecipe("ward+air@ward_any");
        registerRecipe("ward+air_bubble@ward_any");
        registerRecipe("ward+alarm@ward_any");
        registerRecipe("ward+entry_filter@ward_hostile");
        registerRecipe("ward+weather@ward_any");
        registerRecipe("ward+lockdown@ward_any");
        registerRecipe("ward+item_guard@ward_any");
        registerRecipe("ward+storage_lock@ward_any");
        registerRecipe("ward+grow@ward_any");
        registerRecipe("ward+freeze@ward_any");
        registerRecipe("ward+thaw@ward_any");
        registerRecipe("ward+cleanse@ward_allies");
        registerRecipe("ward+reveal@ward_any");
        registerRecipe("ward+sanctuary@ward_any");
        registerRecipe("ward+weakening@ward_hostile");
        registerRecipe("ward+mana_drain@ward_players");
        registerRecipe("ward+reflect_projectile@ward_any");
        registerRecipe("ward+dispel@ward_any");
        registerRecipe("ward+light@ward_any");
        registerRecipe("ward+temporal@ward_any");
        registerRecipe("ward+stasis@ward_hostile");
        registerRecipe("ward+fertility@ward_any");
        registerRecipe("ward+camouflage@ward_any");
        registerRecipe("gravity@target_aoe");
        registerRecipe("silence@target");
        registerRecipe("charm@target");
        registerRecipe("phase@self");
        registerRecipe("recall@self");
        registerRecipe("disarm@target");
        registerRecipe("lifedrain@target");
        registerRecipe("cleanse@ally_aoe");
        registerRecipe("curse@target");
        registerRecipe("reveal@target");
        registerRecipe("rune+bind@block");
        registerRecipe("echo+missile@target");
        registerRecipe("illusion@self");
        registerRecipe("reflect@self");
        registerRecipe("transmute@block");
        registerRecipe("anchor@target");
        registerRecipe("manaburn@target");
        registerRecipe("overload@target");
        registerRecipe("conjure@point");
        registerRecipe("scry@target");
        registerRecipe("scry@self");
        registerRecipe("hex@target");
        registerRecipe("sanctuary@aoe");
        registerRecipe("summon_random@point");
        registerRecipe("summon_undead@point");
        registerRecipe("summon_beast@point");
        registerRecipe("summon_guardian@point");
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
        if (!isPayloadCombinationAllowed(parsed.payloads())) {
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
        int minManaCost = 0;
        int maxManaCost = 0;
        int minCooldownTicks = 0;
        int maxCooldownTicks = 0;
        ParticleOptions particle = ParticleTypes.ENCHANT;
        Spell.FocusEffect focusEffect = Spell.FocusEffect.NONE;

        for (String payloadKey : parsed.payloads()) {
            SpellPart part = PARTS.get(payloadKey);
            if (part == null) {
                return null;
            }

            PayloadEffect payload = part.payload().get();
            SpellBuildContext buildContext = new SpellBuildContext(key, payloadKey, parsed.shape(), parsed.payloads(), payloads.size());
            payloads.add(payload);
            upgrades.addAll(payload.supportedUpgrades(buildContext));
            minManaCost += part.manaCost().min();
            maxManaCost += part.manaCost().max();
            minCooldownTicks += part.cooldownTicks().min();
            maxCooldownTicks += part.cooldownTicks().max();
            particle = payload.particle(buildContext);
            focusEffect = strongestFocusEffect(focusEffect, part.focusEffect());
        }

        if (parsed.shape().contains("aoe")) {
            upgrades.add(Spell.UPGRADE_RADIUS);
        }

        PayloadEffect payload = payloads.size() == 1 ? payloads.get(0) : new CompositePayload(payloads.toArray(PayloadEffect[]::new));
        SpellShape spellShape = shape.factory().apply(particle);
        boolean physical = parsed.payloads().stream().map(PARTS::get).anyMatch(SpellPart::physical);
        return new PayloadSpell(key, Math.max(1, minManaCost), Math.max(1, maxManaCost), Math.max(1, minCooldownTicks), Math.max(1, maxCooldownTicks), spellShape, payload, particle, physical, focusEffect, upgrades.toArray(String[]::new));
    }

    private static void part(String key, Supplier<PayloadEffect> payload, ShapeCompatibility shapes, CostRange manaCost, CostRange cooldownTicks) {
        part(key, payload, shapes, manaCost, cooldownTicks, false);
    }

    private static void part(String key, Supplier<PayloadEffect> payload, ShapeCompatibility shapes, CostRange manaCost, CostRange cooldownTicks, boolean physical) {
        PARTS.put(key, new SpellPart(payload, shapes, manaCost, cooldownTicks, physical, focusEffectFor(key, physical)));
    }

    private static Spell.FocusEffect focusEffectFor(String key, boolean physical) {
        return switch (key) {
            case "blink", "conjure", "fire_place", "gather", "phase", "recall", "scry", "transmute", "warp" -> Spell.FocusEffect.RANGE;
            case "cleanse", "illusion", "rune", "sanctuary" -> Spell.FocusEffect.RADIUS;
            case "agility", "anchor", "alarm", "anti_decay", "anti_explosion", "anti_fire", "anti_grief", "anti_magic", "anti_projectile", "anti_summon", "anti_teleport", "anti_water", "air", "air_bubble", "bind", "blastguard", "blind", "bubble", "camouflage", "charm", "curse", "dark", "dispel", "entry_filter", "fallguard", "fertility", "fireguard", "fortify", "freeze", "frost", "grow", "hex", "item_guard", "levitate", "life_ward", "light", "lockdown", "mana_drain", "mana_shield", "overload", "reflect", "reflect_projectile", "reveal", "shield", "silence", "stasis", "storage_lock", "stun", "temporal", "thaw", "ward", "weakening", "weather" -> Spell.FocusEffect.DURATION;
            case "nullify" -> Spell.FocusEffect.RANGE;
            case "heal", "lifedrain", "manaburn", "regenerate", "summon", "summon_random", "summon_undead", "summon_beast", "summon_guardian", "summon_arcane", "summon_swarm" -> Spell.FocusEffect.UTILITY;
            default -> physical ? Spell.FocusEffect.DAMAGE : Spell.FocusEffect.UTILITY;
        };
    }

    private static Spell.FocusEffect strongestFocusEffect(Spell.FocusEffect current, Spell.FocusEffect candidate) {
        return focusPriority(candidate) > focusPriority(current) ? candidate : current;
    }

    private static int focusPriority(Spell.FocusEffect effect) {
        return switch (effect) {
            case DAMAGE -> 5;
            case UTILITY -> 4;
            case DURATION -> 3;
            case RADIUS -> 2;
            case RANGE -> 1;
            case NONE -> 0;
        };
    }

    private static CostRange mana(int min, int max) {
        return new CostRange(min, max);
    }

    private static CostRange cooldown(int min, int max) {
        return new CostRange(min, max);
    }

    private static void shape(String key, Function<ParticleOptions, SpellShape> factory) {
        SHAPES.put(key, new ShapePart(factory));
    }

    private static RandomSpellRecipe random(String payloads, String shape, int weight) {
        return new RandomSpellRecipe(payloads + "@" + shape, weight);
    }

    private static List<ArcaneKnowledgeRecipe> buildArcaneKnowledgeRecipes() {
        Set<String> wandRecipes = new LinkedHashSet<>();
        RANDOM_RECIPES.forEach(recipe -> wandRecipes.add(recipe.key()));

        List<String> payloads = List.copyOf(PARTS.keySet());
        Set<ArcaneKnowledgeRecipe> recipes = new LinkedHashSet<>();
        addArcaneKnowledgeRecipes(recipes, wandRecipes, payloads, List.of());
        return List.copyOf(recipes);
    }

    private static void addArcaneKnowledgeRecipes(Set<ArcaneKnowledgeRecipe> recipes, Set<String> excludedRecipes, List<String> payloads, List<String> selected) {
        if (!selected.isEmpty()) {
            addArcaneKnowledgeShapes(recipes, excludedRecipes, selected);
        }
        if (selected.size() >= 3) {
            return;
        }

        int start = selected.isEmpty() ? 0 : payloads.indexOf(selected.get(selected.size() - 1)) + 1;
        for (int i = start; i < payloads.size(); i++) {
            List<String> next = new ArrayList<>(selected);
            next.add(payloads.get(i));
            if (isPayloadCombinationAllowed(next)) {
                addArcaneKnowledgeRecipes(recipes, excludedRecipes, payloads, next);
            }
        }
    }

    private static void addArcaneKnowledgeShapes(Set<ArcaneKnowledgeRecipe> recipes, Set<String> excludedRecipes, List<String> payloads) {
        for (String shape : SHAPES.keySet()) {
            if (!isShapeAllowed(payloads, shape)) {
                continue;
            }

            String key = String.join("+", payloads) + "@" + shape;
            if (!excludedRecipes.contains(key) && get(key) != null) {
                recipes.add(new ArcaneKnowledgeRecipe(key, arcaneKnowledgeWeight(payloads.size())));
            }
        }
    }

    private static int arcaneKnowledgeWeight(int payloadCount) {
        return switch (payloadCount) {
            case 1 -> 100;
            case 2 -> 16;
            case 3 -> 2;
            default -> 1;
        };
    }

    private static ShapeCompatibility shapes(String... shapes) {
        Set<String> allowedShapes = Set.of(shapes);
        return allowedShapes::contains;
    }

    private static boolean isShapeAllowed(List<String> payloads, String shape) {
        if (payloads.isEmpty() || !SHAPES.containsKey(shape)) {
            return false;
        }

        if (payloads.contains("ward") && payloads.size() > 1 && !shape.startsWith("ward_")) {
            return false;
        }

        if (shape.startsWith("ward_")) {
            return isWardShapeAllowed(payloads);
        }

        if (payloads.contains("rune") && (shape.equals("block") || shape.equals("point"))) {
            return payloads.stream().allMatch(PARTS::containsKey);
        }

        for (String payload : payloads) {
            SpellPart part = PARTS.get(payload);
            if (part == null || !part.shapes().allows(shape)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isWardShapeAllowed(List<String> payloads) {
        if (!payloads.contains("ward") || payloads.size() < 2) {
            return false;
        }
        return payloads.stream()
                .filter(payload -> !payload.equals("ward"))
                .map(PARTS::get)
                .allMatch(part -> part != null && part.shapes().allows("target"));
    }

    private static boolean isPayloadCombinationAllowed(List<String> payloads) {
        if (payloads.isEmpty() || payloads.size() > 3 || payloads.stream().anyMatch(payload -> !PARTS.containsKey(payload))) {
            return false;
        }
        if (payloads.size() == 1) {
            return true;
        }
        if (payloads.stream().anyMatch(SOLO_PAYLOADS::contains)) {
            return false;
        }
        if (payloads.size() != Set.copyOf(payloads).size()) {
            return false;
        }

        for (int i = 0; i < payloads.size(); i++) {
            for (int j = i + 1; j < payloads.size(); j++) {
                if (!canCombine(payloads.get(i), payloads.get(j))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean canCombine(String first, String second) {
        return PAYLOAD_COMBOS.getOrDefault(first, Set.of()).contains(second)
                || PAYLOAD_COMBOS.getOrDefault(second, Set.of()).contains(first);
    }

    private static Map<String, Set<String>> payloadCombos() {
        Map<String, Set<String>> combos = new LinkedHashMap<>();
        combo(combos, "explode", "arrow", "bind", "blast", "blind", "curse", "disarm", "fire", "fireball", "frost", "gravity", "hex", "levitate", "lightning", "manaburn", "missile", "overload", "push", "silence", "stun");
        combo(combos, "rune", "bind", "blast", "blind", "curse", "explode", "fire", "fireball", "frost", "gravity", "hex", "levitate", "lightning", "manaburn", "missile", "overload", "push", "silence", "stun");
        combo(combos, "echo", "arrow", "bind", "blast", "blind", "bubble", "curse", "disarm", "explode", "fire", "fireball", "frost", "gravity", "heal", "hex", "levitate", "lifedrain", "lightning", "manaburn", "missile", "nullify", "overload", "push", "regenerate", "reveal", "silence", "stun");
        combo(combos, "gravity", "arrow", "bind", "blast", "curse", "fire", "fireball", "frost", "hex", "levitate", "lightning", "missile", "overload", "push", "silence", "stun");
        combo(combos, "bind", "arrow", "blind", "curse", "fire", "fireball", "frost", "hex", "lightning", "manaburn", "missile", "overload", "silence", "stun");
        combo(combos, "curse", "blind", "hex", "lifedrain", "manaburn", "overload", "silence");
        combo(combos, "hex", "blind", "lifedrain", "manaburn", "overload", "silence");
        combo(combos, "heal", "agility", "blastguard", "bubble", "fallguard", "fireguard", "fortify", "life_ward", "mana_shield", "regenerate", "reveal");
        combo(combos, "ward", "agility", "anchor", "alarm", "anti_decay", "anti_explosion", "anti_fire", "anti_grief", "anti_magic", "anti_projectile", "anti_summon", "anti_teleport", "anti_water", "air", "air_bubble", "arrow", "bind", "blast", "blastguard", "blind", "bubble", "camouflage", "charm", "cleanse", "curse", "dark", "disarm", "dispel", "entry_filter", "fallguard", "fertility", "fire", "fireball", "fireguard", "fortify", "freeze", "frost", "gravity", "grow", "heal", "hex", "item_guard", "levitate", "life_ward", "lifedrain", "light", "lightning", "lockdown", "mana_drain", "mana_shield", "manaburn", "missile", "nullify", "overload", "push", "regenerate", "reflect", "reflect_projectile", "reveal", "sanctuary", "silence", "stasis", "storage_lock", "stun", "temporal", "thaw", "weakening", "weather");
        return combos;
    }

    private static void combo(Map<String, Set<String>> combos, String payload, String... allowed) {
        combos.put(payload, Set.of(allowed));
    }

    private interface ShapeCompatibility {
        boolean allows(String shape);
    }

    private record CostRange(int min, int max) {
        private CostRange {
            int normalizedMin = Math.min(min, max);
            int normalizedMax = Math.max(min, max);
            min = normalizedMin;
            max = normalizedMax;
        }
    }

    private record SpellPart(Supplier<PayloadEffect> payload, ShapeCompatibility shapes, CostRange manaCost, CostRange cooldownTicks, boolean physical, Spell.FocusEffect focusEffect) {
    }

    private record ShapePart(Function<ParticleOptions, SpellShape> factory) {
    }

    private record RandomSpellRecipe(String key, int weight) {
    }

    private record ArcaneKnowledgeRecipe(String key, int weight) {
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
