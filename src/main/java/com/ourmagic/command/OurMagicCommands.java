package com.ourmagic.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.ourmagic.item.WardDiagramItem;
import com.ourmagic.magic.ArcaneKnowledgeBook;
import com.ourmagic.magic.Spell;
import com.ourmagic.magic.SpellInstance;
import com.ourmagic.magic.SpellRegistry;
import com.ourmagic.magic.spell.runtime.MagicAllies;
import com.ourmagic.magic.ward.WorldWards;
import com.ourmagic.block.entity.WardStoneBlockEntity;
import com.ourmagic.registry.ModItems;
import com.ourmagic.wand.WandData;
import com.ourmagic.wand.WandTemplates;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class OurMagicCommands {
    private static final String WARD_PAYLOAD = "ward";
    private static final String DEFAULT_WARD_SHAPE = "ward_any";

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
                                .executes(context -> dropArcaneKnowledgeBook(context.getSource(), context.getSource().getPlayerOrException(), BoolArgumentType.getBool(context, "strong")))))
                .then(Commands.literal("ally")
                        .executes(context -> listAllies(context.getSource(), context.getSource().getPlayerOrException()))
                        .then(Commands.literal("list")
                                .executes(context -> listAllies(context.getSource(), context.getSource().getPlayerOrException())))
                        .then(Commands.literal("add")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> addAlly(context.getSource(), context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player")))))
                        .then(Commands.literal("mark")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> addAlly(context.getSource(), context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player")))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> removeAlly(context.getSource(), context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player")))))
                        .then(Commands.literal("unmark")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> removeAlly(context.getSource(), context.getSource().getPlayerOrException(), EntityArgument.getPlayer(context, "player")))))
                        .then(Commands.literal("clear")
                                .executes(context -> clearAllies(context.getSource(), context.getSource().getPlayerOrException()))))
                .then(Commands.literal("ward")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("stone")
                                .executes(context -> giveItem(context.getSource(), context.getSource().getPlayerOrException(), new ItemStack(ModItems.WARD_STONE.get()), "Ward Stone"))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> giveItem(context.getSource(), EntityArgument.getPlayer(context, "target"), new ItemStack(ModItems.WARD_STONE.get()), "Ward Stone"))))
                        .then(Commands.literal("converter")
                                .executes(context -> giveItem(context.getSource(), context.getSource().getPlayerOrException(), new ItemStack(ModItems.MAGIC_FLOW_CONVERTER.get()), "Magic Flow Converter"))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> giveItem(context.getSource(), EntityArgument.getPlayer(context, "target"), new ItemStack(ModItems.MAGIC_FLOW_CONVERTER.get()), "Magic Flow Converter"))))
                        .then(Commands.literal("rf_generator")
                                .executes(context -> giveItem(context.getSource(), context.getSource().getPlayerOrException(), new ItemStack(ModItems.CREATIVE_RF_GENERATOR.get()), "Creative RF Generator"))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> giveItem(context.getSource(), EntityArgument.getPlayer(context, "target"), new ItemStack(ModItems.CREATIVE_RF_GENERATOR.get()), "Creative RF Generator"))))
                        .then(Commands.literal("tuner")
                                .executes(context -> giveItem(context.getSource(), context.getSource().getPlayerOrException(), new ItemStack(ModItems.WARD_TUNER.get()), "Ward Tuner"))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> giveItem(context.getSource(), EntityArgument.getPlayer(context, "target"), new ItemStack(ModItems.WARD_TUNER.get()), "Ward Tuner"))))
                        .then(Commands.literal("perimeter")
                                .executes(context -> giveItem(context.getSource(), context.getSource().getPlayerOrException(), new ItemStack(ModItems.WARD_PERIMETER_STONE.get(), 32), "Ward Perimeter Stones"))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 6400))
                                        .executes(context -> giveItem(context.getSource(), context.getSource().getPlayerOrException(), new ItemStack(ModItems.WARD_PERIMETER_STONE.get(), IntegerArgumentType.getInteger(context, "count")), "Ward Perimeter Stones"))
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(context -> giveItem(context.getSource(), EntityArgument.getPlayer(context, "target"), new ItemStack(ModItems.WARD_PERIMETER_STONE.get(), IntegerArgumentType.getInteger(context, "count")), "Ward Perimeter Stones")))))
                        .then(Commands.literal("paper")
                                .then(Commands.argument("spell", StringArgumentType.greedyString())
                                        .executes(context -> giveWardPaper(context.getSource(), context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "spell")))))
                        .then(Commands.literal("diagram")
                                .then(Commands.argument("spell", StringArgumentType.greedyString())
                                        .executes(context -> giveWardPaper(context.getSource(), context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "spell")))))
                        .then(Commands.literal("apply")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .then(Commands.argument("spell", StringArgumentType.greedyString())
                                                .executes(context -> applyWard(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"), context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "spell"))))))
                        .then(Commands.literal("clear")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(context -> clearWard(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos")))))
                        .then(Commands.literal("info")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(context -> wardInfo(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos")))))
                        .then(Commands.literal("outline")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(context -> wardOutline(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos")))))
                        .then(Commands.literal("biome")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .then(Commands.argument("biome", ResourceLocationArgument.id())
                                                .executes(context -> wardBiome(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"), ResourceLocationArgument.getId(context, "biome"))))))
                        .then(Commands.literal("charge")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> chargeWardStone(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "pos"), IntegerArgumentType.getInteger(context, "amount"))))))
                        .then(Commands.literal("link")
                                .then(Commands.argument("ward", BlockPosArgument.blockPos())
                                        .then(Commands.argument("perimeter", BlockPosArgument.blockPos())
                                                .executes(context -> linkWardPerimeter(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "ward"), BlockPosArgument.getLoadedBlockPos(context, "perimeter"))))))
                        .then(Commands.literal("unlink")
                                .then(Commands.argument("perimeter", BlockPosArgument.blockPos())
                                        .executes(context -> unlinkWardPerimeter(context.getSource(), BlockPosArgument.getLoadedBlockPos(context, "perimeter")))))
                        .then(Commands.literal("count")
                                .executes(context -> wardCount(context.getSource())))));
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
        if (SpellRegistry.isWardRecipe(spell.key())) {
            source.sendFailure(Component.literal("Ward recipes cannot be placed on wands. Use a Ward Diagram on a Ward Stone instead."));
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

    private static int addAlly(CommandSourceStack source, ServerPlayer owner, ServerPlayer ally) {
        if (MagicAllies.addAlly(owner, ally)) {
            source.sendSuccess(() -> Component.literal("Added " + ally.getGameProfile().getName() + " to your OurMagic ally list."), false);
            return 1;
        }
        source.sendFailure(Component.literal(ally.getGameProfile().getName() + " is already in your OurMagic ally list."));
        return 0;
    }

    private static int removeAlly(CommandSourceStack source, ServerPlayer owner, ServerPlayer ally) {
        if (MagicAllies.removeAlly(owner, ally)) {
            source.sendSuccess(() -> Component.literal("Removed " + ally.getGameProfile().getName() + " from your OurMagic ally list."), false);
            return 1;
        }
        source.sendFailure(Component.literal(ally.getGameProfile().getName() + " is not in your OurMagic ally list."));
        return 0;
    }

    private static int clearAllies(CommandSourceStack source, ServerPlayer owner) {
        int removed = MagicAllies.clearAllies(owner);
        source.sendSuccess(() -> Component.literal("Cleared " + removed + " OurMagic allies."), false);
        return removed;
    }

    private static int listAllies(CommandSourceStack source, ServerPlayer owner) {
        List<MagicAllies.AllyEntry> allies = MagicAllies.allies(owner);
        if (allies.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Your OurMagic ally list is empty."), false);
            return 0;
        }
        String names = String.join(", ", allies.stream().map(MagicAllies.AllyEntry::name).toList());
        source.sendSuccess(() -> Component.literal("OurMagic allies: " + names), false);
        return allies.size();
    }

    private static int giveItem(CommandSourceStack source, ServerPlayer target, ItemStack stack, String name) {
        int remaining = stack.getCount();
        while (remaining > 0) {
            ItemStack chunk = stack.copy();
            chunk.setCount(Math.min(remaining, stack.getMaxStackSize()));
            remaining -= chunk.getCount();
            if (!target.getInventory().add(chunk)) {
                target.drop(chunk, false);
            }
        }
        source.sendSuccess(() -> Component.literal("Gave " + name + " to " + target.getGameProfile().getName()), true);
        return 1;
    }

    private static int giveWardPaper(CommandSourceStack source, ServerPlayer target, String spellKey) {
        String resolvedSpellKey = normalizeWardSpellKey(spellKey);
        Spell spell = SpellRegistry.get(resolvedSpellKey);
        if (spell == null || !SpellRegistry.payloadParts(resolvedSpellKey).contains(WARD_PAYLOAD)) {
            source.sendFailure(Component.literal("Unknown ward spell recipe: " + spellKey + " (resolved to " + resolvedSpellKey + ")"));
            return 0;
        }

        ItemStack diagram = WardDiagramItem.create(SpellInstance.fixed(resolvedSpellKey));
        if (!target.getInventory().add(diagram)) {
            target.drop(diagram, false);
        }
        source.sendSuccess(() -> Component.literal("Gave ward diagram " + resolvedSpellKey + " to " + target.getGameProfile().getName()), true);
        return 1;
    }

    private static int applyWard(CommandSourceStack source, BlockPos pos, ServerPlayer owner, String spellKey) {
        ServerLevel level = source.getLevel();
        String resolvedSpellKey = normalizeWardSpellKey(spellKey);
        WorldWards.ApplyResult result = WorldWards.apply(level, pos, owner, SpellInstance.fixed(resolvedSpellKey));
        if (!result.success()) {
            source.sendFailure(Component.literal(result.message() + " (" + spellKey + " resolved to " + resolvedSpellKey + ")"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(result.message()), true);
        return 1;
    }

    private static String normalizeWardSpellKey(String input) {
        String trimmed = input.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }

        String[] split = trimmed.split("@");
        String payloadSpec;
        String shapeSpec;
        if (split.length >= 3 && normalizeRecipeToken(split[0]).equals(WARD_PAYLOAD)) {
            payloadSpec = split[1];
            shapeSpec = joinRecipeTokens(split, 2);
        } else {
            payloadSpec = split[0];
            shapeSpec = split.length > 1 ? joinRecipeTokens(split, 1) : "";
        }

        List<String> payloads = new ArrayList<>();
        for (String rawPayload : payloadSpec.split("\\+")) {
            String payload = normalizeRecipeToken(rawPayload);
            if (!payload.isEmpty()) {
                payloads.add(payload);
            }
        }

        payloads.remove(WARD_PAYLOAD);
        payloads.add(0, WARD_PAYLOAD);

        String shape = shapeSpec.isEmpty() ? DEFAULT_WARD_SHAPE : normalizeWardShape(shapeSpec);
        return String.join("+", payloads) + "@" + shape;
    }

    private static String joinRecipeTokens(String[] tokens, int start) {
        List<String> parts = new ArrayList<>();
        for (int i = start; i < tokens.length; i++) {
            if (!tokens[i].isBlank()) {
                parts.add(tokens[i]);
            }
        }
        return String.join("_", parts);
    }

    private static String normalizeWardShape(String input) {
        String shape = normalizeRecipeToken(input);
        if (shape.isEmpty()) {
            return DEFAULT_WARD_SHAPE;
        }
        if (shape.startsWith("ward_")) {
            return shape;
        }
        if (shape.startsWith("player_") && shape.length() > "player_".length()) {
            return "ward_player_" + shape.substring("player_".length());
        }
        if (shape.startsWith("mob_") && shape.length() > "mob_".length()) {
            return "ward_mob_" + shape.substring("mob_".length());
        }
        if (shape.startsWith("entity_type_") && shape.length() > "entity_type_".length()) {
            return "ward_entity_type_" + shape.substring("entity_type_".length());
        }
        if (shape.startsWith("entity_") && shape.length() > "entity_".length()) {
            return "ward_entity_" + shape.substring("entity_".length());
        }
        return switch (shape) {
            case "any", "all", "everyone", "everything" -> "ward_any";
            case "non_allied", "non_ally", "nonallied", "not_allied", "enemy", "enemies" -> "ward_non_allied";
            case "hostile", "hostiles", "monster", "monsters" -> "ward_hostile";
            case "player", "players" -> "ward_players";
            case "ally", "allies", "friend", "friends", "friendly" -> "ward_allies";
            case "mob", "mobs" -> "ward_mobs";
            case "passive", "passives", "creature", "creatures" -> "ward_passive";
            case "animal", "animals" -> "ward_animals";
            default -> "ward_" + shape;
        };
    }

    private static String normalizeRecipeToken(String input) {
        return input.trim().toLowerCase(Locale.ROOT).replaceAll("[\\s-]+", "_");
    }

    private static int clearWard(CommandSourceStack source, BlockPos pos) {
        boolean removed = WorldWards.clear(source.getLevel(), pos);
        if (!removed) {
            source.sendFailure(Component.literal("No ward is anchored at that Ward Stone."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Cleared ward at " + pos.toShortString()), true);
        return 1;
    }

    private static int wardInfo(CommandSourceStack source, BlockPos pos) {
        String message = WorldWards.describe(source.getLevel(), pos).orElse("No ward is anchored at that Ward Stone.");
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }

    private static int wardOutline(CommandSourceStack source, BlockPos pos) {
        if (!WorldWards.showOutline(source.getLevel(), pos)) {
            source.sendFailure(Component.literal("No ward is anchored at that Ward Stone."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Displayed ward outline at " + pos.toShortString()), false);
        return 1;
    }

    private static int wardBiome(CommandSourceStack source, BlockPos pos, ResourceLocation biomeId) {
        WorldWards.ApplyResult result = WorldWards.setBiome(source.getLevel(), pos, biomeId);
        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(result.message()), true);
        return 1;
    }

    private static int chargeWardStone(CommandSourceStack source, BlockPos pos, int amount) {
        ServerLevel level = source.getLevel();
        java.util.Optional<WardStoneBlockEntity> wardStone = WardStoneBlockEntity.getOrCreate(level, pos);
        if (wardStone.isEmpty()) {
            source.sendFailure(Component.literal("There is no Ward Stone at " + pos.toShortString() + "."));
            return 0;
        }
        int accepted = wardStone.get().receiveMagicFlow(amount, false);
        source.sendSuccess(() -> Component.literal("Added " + accepted + " MF to Ward Stone. Stored "
                + wardStone.get().magicFlow() + "/" + wardStone.get().magicFlowCapacity() + " MF."), true);
        return accepted;
    }

    private static int linkWardPerimeter(CommandSourceStack source, BlockPos ward, BlockPos perimeter) {
        WorldWards.ApplyResult result = WorldWards.linkPerimeter(source.getLevel(), ward, perimeter);
        if (!result.success()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(result.message()), true);
        return 1;
    }

    private static int unlinkWardPerimeter(CommandSourceStack source, BlockPos perimeter) {
        if (!WorldWards.unlinkPerimeter(source.getLevel(), perimeter)) {
            source.sendFailure(Component.literal("That Ward Perimeter Stone was not linked."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Unlinked Ward Perimeter Stone at " + perimeter.toShortString()), true);
        return 1;
    }

    private static int wardCount(CommandSourceStack source) {
        int count = WorldWards.count(source.getLevel());
        source.sendSuccess(() -> Component.literal("Active world wards: " + count), false);
        return count;
    }
}
