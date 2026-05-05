package com.ourmagic.network;

import com.ourmagic.magic.Spell;
import com.ourmagic.magic.SpellIngredients;
import com.ourmagic.magic.SpellRegistry;
import com.ourmagic.item.GrimoireItem;
import com.ourmagic.registry.ModItems;
import com.ourmagic.wand.WandData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CraftSpellPacket(InteractionHand hand, String spellKey, Target target) {
    public static final String TAG_SPELL_KEY = "OurMagicSpell";

    public static void encode(CraftSpellPacket packet, FriendlyByteBuf buffer) {
        buffer.writeEnum(packet.hand);
        buffer.writeUtf(packet.spellKey, 64);
        buffer.writeEnum(packet.target);
    }

    public static CraftSpellPacket decode(FriendlyByteBuf buffer) {
        return new CraftSpellPacket(buffer.readEnum(InteractionHand.class), buffer.readUtf(64), buffer.readEnum(Target.class));
    }

    public static void handle(CraftSpellPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() == null) {
                return;
            }

            Spell spell = SpellRegistry.get(packet.spellKey);
            if (spell == null) {
                context.getSender().displayClientMessage(Component.literal("Unknown spell: " + packet.spellKey).withStyle(ChatFormatting.RED), false);
                return;
            }

            ItemStack wand = context.getSender().getItemInHand(packet.hand);
            if (!wand.is(ModItems.WAND.get()) && !wand.is(ModItems.ADMIN_WAND.get())) {
                return;
            }

            WandData data = WandData.read(wand);
            CraftCheck craftCheck = canCraftRecipe(packet.spellKey);
            if (!craftCheck.allowed()) {
                context.getSender().displayClientMessage(Component.literal("Unknown spell effect recipe.").withStyle(ChatFormatting.RED), false);
                return;
            }
            if (craftCheck.effectCount() > maxCraftEffects(data)) {
                context.getSender().displayClientMessage(Component.literal("This recipe needs a stronger known spell before combining that many effects.").withStyle(ChatFormatting.RED), false);
                return;
            }
            boolean hasIngredients = SpellIngredients.has(context.getSender().getInventory(), packet.spellKey);
            if (!hasIngredients && !SpellIngredients.isUnlocked(context.getSender().getInventory(), packet.spellKey)) {
                context.getSender().displayClientMessage(Component.literal("Missing spellcraft ingredients or grimoire knowledge.").withStyle(ChatFormatting.RED), false);
                return;
            }

            if (packet.target == Target.WAND) {
                if (data.addRolledSpell(packet.spellKey, context.getSender().getRandom())) {
                    if (hasIngredients) {
                        SpellIngredients.consume(context.getSender().getInventory(), packet.spellKey);
                    }
                    data.save(wand);
                    context.getSender().getInventory().setChanged();
                    context.getSender().displayClientMessage(Component.literal("Added " + packet.spellKey + " to wand").withStyle(ChatFormatting.AQUA), false);
                } else {
                    context.getSender().displayClientMessage(Component.literal("That wand already knows " + packet.spellKey).withStyle(ChatFormatting.YELLOW), false);
                }
                return;
            }

            if (packet.target == Target.GRIMOIRE) {
                InteractionHand grimoireHand = packet.hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                ItemStack grimoire = context.getSender().getItemInHand(grimoireHand);
                if (!grimoire.is(ModItems.GRIMOIRE.get())) {
                    context.getSender().displayClientMessage(Component.literal("Hold a grimoire in your other hand.").withStyle(ChatFormatting.RED), false);
                    return;
                }
                if (GrimoireItem.addSpell(grimoire, packet.spellKey)) {
                    if (hasIngredients) {
                        SpellIngredients.consume(context.getSender().getInventory(), packet.spellKey);
                    }
                    context.getSender().getInventory().setChanged();
                    context.getSender().displayClientMessage(Component.literal("Added " + packet.spellKey + " to grimoire").withStyle(ChatFormatting.AQUA), false);
                } else {
                    context.getSender().displayClientMessage(Component.literal("That grimoire already contains " + packet.spellKey).withStyle(ChatFormatting.YELLOW), false);
                }
                return;
            }

            if (hasIngredients) {
                SpellIngredients.consume(context.getSender().getInventory(), packet.spellKey);
            }
            ItemStack paper = new ItemStack(Items.PAPER);
            CompoundTag tag = paper.getOrCreateTag();
            tag.putString(TAG_SPELL_KEY, packet.spellKey);
            paper.setHoverName(Component.literal("Spell: " + packet.spellKey).withStyle(ChatFormatting.LIGHT_PURPLE));
            if (!context.getSender().getInventory().add(paper)) {
                context.getSender().drop(paper, false);
            }
        });
        context.setPacketHandled(true);
    }

    public enum Target {
        WAND,
        PAPER,
        GRIMOIRE
    }

    private static CraftCheck canCraftRecipe(String spellKey) {
        java.util.List<String> requestedPayloads = SpellRegistry.payloadParts(spellKey);
        if (requestedPayloads.isEmpty()) {
            return new CraftCheck(false, 0);
        }

        return new CraftCheck(SpellRegistry.payloadKeys().containsAll(requestedPayloads), requestedPayloads.size());
    }

    private static int maxCraftEffects(WandData data) {
        int highestLevel = data.spells().stream().mapToInt(WandData.WandSpellData::level).max().orElse(1);
        if (highestLevel >= 50) {
            return 3;
        }
        if (highestLevel >= 20) {
            return 2;
        }
        return 1;
    }

    private record CraftCheck(boolean allowed, int effectCount) {
    }
}
