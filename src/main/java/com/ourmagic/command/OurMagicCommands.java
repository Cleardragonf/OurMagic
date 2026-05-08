package com.ourmagic.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.ourmagic.magic.ArcaneKnowledgeBook;
import com.ourmagic.magic.Spell;
import com.ourmagic.magic.SpellInstance;
import com.ourmagic.magic.SpellRegistry;
import com.ourmagic.registry.ModItems;
import com.ourmagic.wand.WandData;
import com.ourmagic.wand.WandTemplates;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public final class OurMagicCommands {
    private OurMagicCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ourmagic")
                .then(Commands.literal("wand")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> giveWand(context.getSource(), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> giveWand(context.getSource(), EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("adminwand")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> giveAdminWand(context.getSource(), context.getSource().getPlayerOrException()))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> giveAdminWand(context.getSource(), EntityArgument.getPlayer(context, "target")))))
                .then(Commands.literal("spellwand")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("spell", StringArgumentType.greedyString())
                                .executes(context -> giveSpellWand(context.getSource(), context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "spell")))))
                .then(Commands.literal("arcane_book")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> dropArcaneKnowledgeBook(context.getSource(), context.getSource().getPlayerOrException(), false))
                        .then(Commands.argument("strong", BoolArgumentType.bool())
                                .executes(context -> dropArcaneKnowledgeBook(context.getSource(), context.getSource().getPlayerOrException(), BoolArgumentType.getBool(context, "strong"))))));
    }

    private static int giveWand(CommandSourceStack source, ServerPlayer target) {
        ItemStack stack = new ItemStack(ModItems.WAND.get());
        WandTemplates.applyRandom(stack, RandomSource.create());
        target.getInventory().add(stack);
        source.sendSuccess(() -> Component.translatable("command.ourmagic.wand.given", target.getDisplayName()), true);
        return 1;
    }

    private static int giveAdminWand(CommandSourceStack source, ServerPlayer target) {
        ItemStack stack = new ItemStack(ModItems.ADMIN_WAND.get());
        WandTemplates.applyAdmin(stack);
        target.getInventory().add(stack);
        source.sendSuccess(() -> Component.translatable("command.ourmagic.admin_wand.given", target.getDisplayName()), true);
        return 1;
    }

    private static int giveSpellWand(CommandSourceStack source, ServerPlayer target, String spellKey) {
        Spell spell = SpellRegistry.get(spellKey);
        if (spell == null) {
            source.sendFailure(Component.literal("Unknown spell recipe: " + spellKey));
            return 0;
        }

        ItemStack stack = new ItemStack(ModItems.WAND.get());
        SpellInstance instance = SpellInstance.roll(spell.key(), target.getRandom());
        new WandData("custom", "Custom Wand", 1.0F, java.util.List.of(new WandData.WandSpellData(instance.key(), instance.displayName(), instance.manaCost(), instance.cooldownTicks())), 0, 0).save(stack);
        target.getInventory().add(stack);
        source.sendSuccess(() -> Component.literal("Gave " + target.getGameProfile().getName() + " a wand with " + instance.displayName()), true);
        return 1;
    }

    private static int dropArcaneKnowledgeBook(CommandSourceStack source, ServerPlayer target, boolean strong) {
        ItemStack stack = ArcaneKnowledgeBook.create(target.getRandom(), strong);
        ItemEntity item = new ItemEntity(target.level(), target.getX(), target.getY() + 0.25D, target.getZ(), stack);
        item.setDefaultPickUpDelay();
        target.level().addFreshEntity(item);
        source.sendSuccess(() -> Component.literal("Dropped " + stack.getHoverName().getString() + " for " + target.getGameProfile().getName()), true);
        return 1;
    }
}
