package com.ourmagic.magic.spell;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.runtime.SpellEffect;
import com.ourmagic.magic.spell.modifier.ChainingModifier;
import com.ourmagic.magic.spell.modifier.MultistrikeModifier;
import com.ourmagic.magic.spell.modifier.SpellModifier;
import com.ourmagic.wand.WandData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Set;

public class BaseSpell implements Spell {
    private static final List<SpellModifier> MODIFIERS = List.of(
            new MultistrikeModifier(),
            new ChainingModifier()
    );

    private final String key;
    private final int minManaCost;
    private final int maxManaCost;
    private final int minCooldownTicks;
    private final int maxCooldownTicks;
    private final SpellEffect effect;
    private final Set<String> supportedUpgrades;
    private final boolean physical;
    private final Spell.FocusEffect focusEffect;

    protected BaseSpell(String key, int manaCost, int cooldownTicks, SpellEffect effect, String... supportedUpgrades) {
        this(key, manaCost, manaCost, cooldownTicks, cooldownTicks, effect, false, supportedUpgrades);
    }

    protected BaseSpell(String key, int minManaCost, int maxManaCost, int minCooldownTicks, int maxCooldownTicks, SpellEffect effect, String... supportedUpgrades) {
        this(key, minManaCost, maxManaCost, minCooldownTicks, maxCooldownTicks, effect, false, supportedUpgrades);
    }

    protected BaseSpell(String key, int minManaCost, int maxManaCost, int minCooldownTicks, int maxCooldownTicks, SpellEffect effect, boolean physical, String... supportedUpgrades) {
        this(key, minManaCost, maxManaCost, minCooldownTicks, maxCooldownTicks, effect, physical, Spell.FocusEffect.UTILITY, supportedUpgrades);
    }

    protected BaseSpell(String key, int minManaCost, int maxManaCost, int minCooldownTicks, int maxCooldownTicks, SpellEffect effect, boolean physical, Spell.FocusEffect focusEffect, String... supportedUpgrades) {
        this.key = key;
        this.minManaCost = Math.max(0, Math.min(minManaCost, maxManaCost));
        this.maxManaCost = Math.max(this.minManaCost, Math.max(minManaCost, maxManaCost));
        this.minCooldownTicks = Math.max(1, Math.min(minCooldownTicks, maxCooldownTicks));
        this.maxCooldownTicks = Math.max(this.minCooldownTicks, Math.max(minCooldownTicks, maxCooldownTicks));
        this.effect = effect;
        this.physical = physical;
        this.focusEffect = focusEffect;
        this.supportedUpgrades = Set.of(supportedUpgrades);
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public int manaCost() {
        return Math.round((minManaCost + maxManaCost) / 2.0F);
    }

    @Override
    public int minManaCost() {
        return minManaCost;
    }

    @Override
    public int maxManaCost() {
        return maxManaCost;
    }

    @Override
    public int cooldownTicks() {
        return Math.round((minCooldownTicks + maxCooldownTicks) / 2.0F);
    }

    @Override
    public int minCooldownTicks() {
        return minCooldownTicks;
    }

    @Override
    public int maxCooldownTicks() {
        return maxCooldownTicks;
    }

    @Override
    public boolean isPhysical() {
        return physical;
    }

    @Override
    public Spell.FocusEffect focusEffect() {
        return focusEffect;
    }

    @Override
    public boolean cast(Level level, ServerPlayer player, ItemStack wand, WandData data) {
        return cast(level, player, wand, data, 1.0F);
    }

    @Override
    public boolean cast(Level level, ServerPlayer player, ItemStack wand, WandData data, float focusMultiplier) {
        SpellEffect castEffect = effect;
        for (SpellModifier modifier : MODIFIERS) {
            castEffect = modifier.wrap(this, castEffect);
        }
        float multiplier = Math.max(1.0F, Math.min(100.0F, focusMultiplier));
        return castEffect.cast(new SpellContext(level, player, wand, data, this, 0, 1, multiplier, true, false, java.util.Optional.empty()));
    }

    @Override
    public boolean supportsUpgrade(String upgrade) {
        return supportedUpgrades.contains(upgrade)
                || supportedUpgrades.contains(Spell.UPGRADE_CHAINING) && upgrade.startsWith(Spell.UPGRADE_CHAINING + ".")
                || supportedUpgrades.contains(Spell.UPGRADE_MULTISTRIKE) && upgrade.startsWith(Spell.UPGRADE_MULTISTRIKE + ".");
    }

    @Override
    public boolean supportsUpgradeFamily(String upgrade) {
        return supportedUpgrades.contains(upgrade);
    }
}
