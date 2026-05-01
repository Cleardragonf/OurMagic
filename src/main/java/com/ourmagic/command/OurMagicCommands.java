package com.ourmagic.command;

import com.mojang.brigadier.CommandDispatcher;
import com.ourmagic.registry.ModItems;
import com.ourmagic.wand.WandTemplates;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
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
                                        .executes(context -> giveAdminWand(context.getSource(), EntityArgument.getPlayer(context, "target"))))));
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
}
