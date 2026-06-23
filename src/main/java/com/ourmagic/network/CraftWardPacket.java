package com.ourmagic.network;

import com.ourmagic.advancement.ModCriteriaTriggers;
import com.ourmagic.item.GrimoireItem;
import com.ourmagic.item.WardDiagramItem;
import com.ourmagic.magic.Spell;
import com.ourmagic.magic.SpellIngredients;
import com.ourmagic.magic.SpellInstance;
import com.ourmagic.magic.SpellRegistry;
import com.ourmagic.registry.ModItems;
import com.ourmagic.ui.WardCrafterMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CraftWardPacket(String spellKey, Target target) {
    public static void encode(CraftWardPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.spellKey, 96);
        buffer.writeEnum(packet.target);
    }

    public static CraftWardPacket decode(FriendlyByteBuf buffer) {
        return new CraftWardPacket(buffer.readUtf(96), buffer.readEnum(Target.class));
    }

    public static void handle(CraftWardPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || !(player.containerMenu instanceof WardCrafterMenu)) {
                return;
            }

            Spell spell = SpellRegistry.get(packet.spellKey);
            if (spell == null || !SpellRegistry.isValidRecipe(packet.spellKey) || !SpellRegistry.isWardRecipe(packet.spellKey)) {
                player.displayClientMessage(Component.literal("Unsupported ward recipe.").withStyle(ChatFormatting.RED), false);
                return;
            }

            boolean hasIngredients = SpellIngredients.has(player.getInventory(), packet.spellKey);
            if (!hasIngredients) {
                player.displayClientMessage(Component.literal("Missing ward ingredients.").withStyle(ChatFormatting.RED), false);
                return;
            }

            SpellInstance ward = SpellInstance.roll(packet.spellKey, player.getRandom());
            if (packet.target == Target.GRIMOIRE) {
                ItemStack grimoire = findGrimoire(player.getInventory());
                if (grimoire.isEmpty()) {
                    player.displayClientMessage(Component.literal("Put a grimoire in your inventory.").withStyle(ChatFormatting.RED), false);
                    return;
                }
                if (!GrimoireItem.addSpell(grimoire, ward)) {
                    player.displayClientMessage(Component.literal("That grimoire already contains " + ward.displayName()).withStyle(ChatFormatting.YELLOW), false);
                    return;
                }
                consumeWardCost(player.getInventory(), packet.spellKey, hasIngredients);
                player.getInventory().setChanged();
                ModCriteriaTriggers.awardSpell(player, ward.key());
                player.displayClientMessage(Component.literal("Added " + ward.displayName() + " to grimoire").withStyle(ChatFormatting.AQUA), false);
                return;
            }

            consumeWardCost(player.getInventory(), packet.spellKey, hasIngredients);
            ItemStack diagram = WardDiagramItem.create(ward);
            ModCriteriaTriggers.awardSpell(player, ward.key());
            if (!player.getInventory().add(diagram)) {
                player.drop(diagram, false);
            }
            player.displayClientMessage(Component.literal("Created " + diagram.getHoverName().getString()).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        });
        context.setPacketHandled(true);
    }

    private static ItemStack findGrimoire(Container inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.GRIMOIRE.get())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void consumeWardCost(Container inventory, String spellKey, boolean hasIngredients) {
        if (hasIngredients) {
            SpellIngredients.consume(inventory, spellKey);
        }
    }

    public enum Target {
        DIAGRAM,
        GRIMOIRE
    }
}
