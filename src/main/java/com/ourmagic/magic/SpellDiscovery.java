package com.ourmagic.magic;

import com.ourmagic.wand.WandData;

import java.util.List;
import java.util.Optional;

public record SpellDiscovery(String parentSpell, int requiredLevel, String resultSpell, String displayName, String description) {
    private static final List<SpellDiscovery> DISCOVERIES = List.of(
            new SpellDiscovery("missile@self", 5, "missile@target", "Guided Missile", "A missile shaped by aim instead of instinct."),
            new SpellDiscovery("push@target", 10, "blast@target", "Blast", "Force condensed into a sharper impact."),
            new SpellDiscovery("arrow@target", 15, "arrow@target_aoe", "Volley", "A shot that splinters across nearby targets."),
            new SpellDiscovery("blast@target", 25, "explode@target", "Detonate", "A volatile blast focused on one target."),
            new SpellDiscovery("frost@water_target_aoe", 20, "frost@target_aoe", "Frost Field", "Cold that spreads from the target point."),
            new SpellDiscovery("bubble@self", 15, "bubble@ally_self_aoe", "Bubble Ward", "Protective bubbles shared with nearby allies."),
            new SpellDiscovery("fireball@self", 10, "fireball@target", "Hurled Fireball", "A fireball directed at a distant target."),
            new SpellDiscovery("fireball@self", 35, "fireball@target_aoe", "Flame Burst", "Fire that erupts around impact."),
            new SpellDiscovery("blind@target", 10, "blind@target_aoe", "Darkness Cloud", "Blinding magic that blooms over an area."),
            new SpellDiscovery("heal@ally_self_aoe", 15, "heal@target", "Mend", "Healing directed to a single target."),
            new SpellDiscovery("blink@point", 20, "blink@self", "Recall Step", "Blinking magic folded back onto the caster."),
            new SpellDiscovery("levitate@target", 20, "levitate@self", "Lift", "Levitation turned inward."),
            new SpellDiscovery("lightning@target", 20, "lightning@target_aoe", "Forked Lightning", "Lightning that searches around the struck target."),
            new SpellDiscovery("lightning@target", 50, "lightning+explode@target_aoe", "Storm", "A violent storm burst born from mastered lightning."),
            new SpellDiscovery("regenerate@ally_self_aoe", 25, "regenerate@target", "Renew", "Regeneration focused into one target.")
    );

    public static List<SpellDiscovery> all() {
        return DISCOVERIES;
    }

    public static List<SpellDiscovery> forParent(String parentSpell) {
        return DISCOVERIES.stream()
                .filter(discovery -> discovery.parentSpell().equals(parentSpell))
                .toList();
    }

    public static Optional<SpellDiscovery> byResult(String resultSpell) {
        return DISCOVERIES.stream()
                .filter(discovery -> discovery.resultSpell().equals(resultSpell))
                .findFirst();
    }

    public boolean isUnlocked(WandData data) {
        return data.spells().stream()
                .anyMatch(spell -> spell.key().equals(parentSpell) && spell.level() >= requiredLevel);
    }
}
