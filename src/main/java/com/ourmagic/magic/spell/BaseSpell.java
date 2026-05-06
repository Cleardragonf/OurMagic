package com.ourmagic.magic.spell;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.spell.effect.SpellContext;
import com.ourmagic.magic.spell.effect.SpellEffect;
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
    private final int manaCost;
    private final int cooldownTicks;
    private final SpellEffect effect;
    private final Set<String> supportedUpgrades;

    protected BaseSpell(String key, int manaCost, int cooldownTicks, SpellEffect effect, String... supportedUpgrades) {
        this.key = key;
        this.manaCost = manaCost;
        this.cooldownTicks = cooldownTicks;
        this.effect = effect;
        this.supportedUpgrades = Set.of(supportedUpgrades);
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public int manaCost() {
        return manaCost;
    }

    @Override
    public int cooldownTicks() {
        return cooldownTicks;
    }

    @Override
    public boolean cast(Level level, ServerPlayer player, ItemStack wand, WandData data) {
        return cast(level, player, wand, data, 1.0F);
    }

    @Override
    public boolean cast(Level level, ServerPlayer player, ItemStack wand, WandData data, float chantMultiplier) {
        SpellEffect castEffect = effect;
        for (SpellModifier modifier : MODIFIERS) {
            castEffect = modifier.wrap(this, castEffect);
        }
        float multiplier = Math.max(1.0F, Math.min(100.0F, chantMultiplier));
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
