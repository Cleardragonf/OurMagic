package com.ourmagic.magic.ward;

import com.ourmagic.OurMagic;
import com.ourmagic.block.WardBoundaryBlock;
import com.ourmagic.block.WardBlock;
import com.ourmagic.block.entity.WardCamouflageBlockEntity;
import com.ourmagic.block.entity.WardStoneBlockEntity;
import com.ourmagic.magic.Spell;
import com.ourmagic.magic.SpellInstance;
import com.ourmagic.magic.SpellRegistry;
import com.ourmagic.magic.energy.MagicEnergyType;
import com.ourmagic.magic.spell.payloads.PayloadEffect;
import com.ourmagic.magic.spell.runtime.MagicAllies;
import com.ourmagic.magic.spell.runtime.MagicStatusEffects;
import com.ourmagic.magic.spell.runtime.SpellContext;
import com.ourmagic.magic.spell.shapes.SpellTarget;
import com.ourmagic.mana.PlayerMana;
import com.ourmagic.network.ModNetwork;
import com.ourmagic.registry.ModBlocks;
import com.ourmagic.registry.ModItems;
import com.ourmagic.util.PlayerTitles;
import com.ourmagic.wand.WandData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID)
public class WorldWards extends SavedData {
    private static final String DATA_NAME = OurMagic.MOD_ID + "_world_wards";
    private static final int PERIMETER_SCAN_RADIUS = 48;
    private static final int MAX_SPACES = 32768;
    private static final int TRIGGER_COOLDOWN_TICKS = 20;
    private static final int ENVIRONMENT_SCAN_PER_TICK = 256;
    private static final int CAMOUFLAGE_BLEND_RADIUS = 5;
    private static final int CAMOUFLAGE_MAX_RISE = 12;
    private static final int PASSIVE_DRAIN_INTERVAL_TICKS = 20;
    private static final int BASE_PASSIVE_MAGIC_FLOW = 2;
    private static final int BASE_ACTIVE_MAGIC_FLOW = 8;
    private static final int VOLUME_PASSIVE_DIVISOR = 4096;
    private static final String ANTI_EXPLOSION_PAYLOAD = "anti_explosion";
    private static final String ANTI_MAGIC_PAYLOAD = "anti_magic";
    private static final String AIR_BUBBLE_PAYLOAD = "air_bubble";
    private static final String ANTI_GRIEF_PAYLOAD = "anti_grief";
    private static final String ANTI_PROJECTILE_PAYLOAD = "anti_projectile";
    private static final String ANTI_TELEPORT_PAYLOAD = "anti_teleport";
    private static final String ANTI_SUMMON_PAYLOAD = "anti_summon";
    private static final String ITEM_GUARD_PAYLOAD = "item_guard";
    private static final String STORAGE_LOCK_PAYLOAD = "storage_lock";
    private static final String WEATHER_PAYLOAD = "weather";
    private static final Map<String, Integer> PASSIVE_MAGIC_FLOW_COSTS = Map.ofEntries(
            Map.entry("alarm", 1),
            Map.entry("anti_decay", 3),
            Map.entry("anti_explosion", 3),
            Map.entry("anti_fire", 4),
            Map.entry("anti_grief", 3),
            Map.entry("anti_magic", 6),
            Map.entry("anti_projectile", 4),
            Map.entry("anti_summon", 4),
            Map.entry("anti_teleport", 4),
            Map.entry("anti_water", 5),
            Map.entry("air", 4),
            Map.entry("air_bubble", 10),
            Map.entry("camouflage", 8),
            Map.entry("cleanse", 4),
            Map.entry("dark", 4),
            Map.entry("dispel", 5),
            Map.entry("entry_filter", 4),
            Map.entry("fertility", 5),
            Map.entry("freeze", 5),
            Map.entry("grow", 4),
            Map.entry("item_guard", 2),
            Map.entry("light", 8),
            Map.entry("lockdown", 6),
            Map.entry("mana_drain", 5),
            Map.entry("reflect_projectile", 5),
            Map.entry("reveal", 4),
            Map.entry("sanctuary", 6),
            Map.entry("stasis", 5),
            Map.entry("storage_lock", 2),
            Map.entry("temporal", 7),
            Map.entry("thaw", 4),
            Map.entry("weakening", 4),
            Map.entry("weather", 4)
    );
    private static final Map<String, Integer> ACTIVE_MAGIC_FLOW_COSTS = Map.ofEntries(
            Map.entry("alarm", 10),
            Map.entry("anti_decay", 10),
            Map.entry("anti_explosion", 18),
            Map.entry("anti_grief", 12),
            Map.entry("anti_magic", 24),
            Map.entry("anti_projectile", 14),
            Map.entry("anti_summon", 18),
            Map.entry("anti_teleport", 18),
            Map.entry("bind", 18),
            Map.entry("cleanse", 12),
            Map.entry("dark", 12),
            Map.entry("dispel", 18),
            Map.entry("entry_filter", 16),
            Map.entry("heal", 14),
            Map.entry("item_guard", 8),
            Map.entry("lockdown", 18),
            Map.entry("mana_drain", 14),
            Map.entry("reflect_projectile", 18),
            Map.entry("reveal", 10),
            Map.entry("sanctuary", 16),
            Map.entry("stasis", 14),
            Map.entry("storage_lock", 8),
            Map.entry("weakening", 12),
            Map.entry("weather", 12)
    );
    private static final Set<String> PASSIVE_WARD_PAYLOADS = Set.of(
            "ward",
            "alarm",
            "anti_decay",
            "anti_explosion",
            "anti_fire",
            "anti_grief",
            "anti_magic",
            "anti_projectile",
            "anti_summon",
            "anti_teleport",
            "anti_water",
            "air",
            "air_bubble",
            "camouflage",
            "cleanse",
            "dark",
            "dispel",
            "entry_filter",
            "fertility",
            "freeze",
            "grow",
            "item_guard",
            "light",
            "lockdown",
            "mana_drain",
            "reflect_projectile",
            "reveal",
            "sanctuary",
            "stasis",
            "storage_lock",
            "temporal",
            "thaw",
            "weakening",
            "weather"
    );
    private static final String PROJECTILE_REFLECT_TAG = "OurMagicReflectedWard";
    private final List<ActiveWard> wards = new ArrayList<>();
    private final Map<Long, Long> perimeterLinks = new HashMap<>();
    private final Set<UUID> playersInLightWards = new HashSet<>();

    public static WorldWards get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(WorldWards::load, WorldWards::new, DATA_NAME);
    }

    public static ApplyResult apply(ServerLevel level, BlockPos anchor, ServerPlayer owner, SpellInstance spell) {
        Spell registered = SpellRegistry.get(spell.key());
        if (registered == null || !SpellRegistry.payloadParts(spell.key()).contains("ward")) {
            return new ApplyResult(false, "That spell is not a ward.");
        }
        if (SpellRegistry.payloadEffectsExcluding(spell.key(), "ward").isEmpty()) {
            return new ApplyResult(false, "World wards need a ward spell combined with another effect.");
        }
        Optional<WardStoneBlockEntity.WardMultiblock> core = WardStoneBlockEntity.findMultiblock(level, anchor);
        if (core.isEmpty()) {
            return new ApplyResult(false, "There is no Ward Stone at that position.");
        }

        WorldWards data = get(level);
        data.migrateWardCore(level, core.get());
        BlockPos masterAnchor = core.get().master();
        Optional<ComputedArea> area = computeArea(level, masterAnchor);
        if (area.isEmpty()) {
            return new ApplyResult(false, "The Ward Stone core is not inside a valid 3D perimeter anchor volume.");
        }

        int wardSlots = core.get().size();
        int activeAtAnchor = data.countWardsAt(masterAnchor);
        List<String> payloadKeys = SpellRegistry.payloadParts(spell.key());
        ActiveWard appliedWard = new ActiveWard(masterAnchor.immutable(), owner.getUUID(), spell.key(), spell.displayName(), area.get().spaces(), area.get().bounds(), new HashSet<>());
        Map<MagicEnergyType, Integer> requiredPassive = passiveWardCostsPerSecond(appliedWard, payloadKeys);
        Optional<WardStoneBlockEntity> wardStone = WardStoneBlockEntity.getOrCreate(level, masterAnchor);
        if (wardStone.isEmpty()) {
            return new ApplyResult(false, "There is no Ward Stone at that position.");
        }
        Optional<String> missingRequirement = missingMagicFlowRequirement(wardStone.get(), requiredPassive);
        if (missingRequirement.isPresent()) {
            return new ApplyResult(false, "This ward requires at least " + missingRequirement.get() + " in the Ward Stone core.");
        }
        for (int i = 0; i < data.wards.size(); i++) {
            ActiveWard ward = data.wards.get(i);
            if (!ward.anchor.equals(masterAnchor)) {
                continue;
            }
            if (ward.spellKey.equals(spell.key())) {
                if (usesGeneratedShell(ward.spellKey)) {
                    removeWardInfrastructure(level, ward);
                }
                data.wards.set(i, appliedWard);
                data.setDirty();
                return new ApplyResult(true, "Updated " + spell.displayName() + " on " + area.get().spaces().size() + " ward spaces. Active wards on this core: " + activeAtAnchor + "/" + wardSlots + ". " + costSummary(appliedWard, payloadKeys));
            }
        }
        if (activeAtAnchor >= wardSlots) {
            return new ApplyResult(false, "This Ward Stone core has " + wardSlots + " ward slots and they are all full. Add another touching Ward Stone to expand it.");
        }
        data.wards.add(appliedWard);
        data.setDirty();
        return new ApplyResult(true, "Applied " + spell.displayName() + " to " + area.get().spaces().size() + " ward spaces. Active wards on this core: " + (activeAtAnchor + 1) + "/" + wardSlots + ". " + costSummary(appliedWard, payloadKeys));
    }

    public static boolean clear(ServerLevel level, BlockPos anchor) {
        WorldWards data = get(level);
        BlockPos masterAnchor = data.normalizeAnchorOrSelf(level, anchor);
        boolean removed = data.wards.removeIf(ward -> {
            boolean matched = ward.anchor.equals(masterAnchor);
            if (matched && usesGeneratedShell(ward.spellKey)) {
                removeWardInfrastructure(level, ward);
            }
            return matched;
        });
        if (removed) {
            data.setDirty();
        }
        return removed;
    }

    public static ApplyResult linkPerimeter(ServerLevel level, BlockPos anchor, BlockPos perimeter) {
        Optional<WardStoneBlockEntity.WardMultiblock> core = WardStoneBlockEntity.findMultiblock(level, anchor);
        if (core.isEmpty()) {
            return new ApplyResult(false, "There is no Ward Stone at " + anchor.toShortString() + ".");
        }
        WorldWards data = get(level);
        data.migrateWardCore(level, core.get());
        BlockPos masterAnchor = core.get().master();
        if (!level.getBlockState(perimeter).is(ModBlocks.WARD_PERIMETER_STONE.get()) && !data.perimeterLinks.containsKey(perimeter.asLong())) {
            return new ApplyResult(false, "There is no Ward Perimeter Stone at " + perimeter.toShortString() + ".");
        }

        Long previousAnchor = data.perimeterLinks.get(perimeter.asLong());
        if (previousAnchor != null && previousAnchor != masterAnchor.asLong()) {
            BlockPos previousMaster = data.normalizeAnchorOrSelf(level, BlockPos.of(previousAnchor));
            data.wards.stream()
                    .filter(ward -> ward.anchor.equals(previousMaster) && usesGeneratedShell(ward.spellKey))
                    .forEach(ward -> removeWardInfrastructure(level, ward));
        }
        data.perimeterLinks.put(perimeter.asLong(), masterAnchor.asLong());
        if (previousAnchor != null && previousAnchor != masterAnchor.asLong()) {
            data.refreshActiveWardVolume(level, data.normalizeAnchorOrSelf(level, BlockPos.of(previousAnchor)));
        }
        data.refreshActiveWardVolume(level, masterAnchor);
        data.setDirty();
        Optional<ComputedArea> area = computeArea(level, masterAnchor);
        String suffix = area.map(computedArea -> " Current linked volume: " + computedArea.spaces().size() + " spaces.").orElse(" Link saved, but the linked anchors do not form a valid volume yet.");
        return new ApplyResult(true, "Linked perimeter stone to Ward Stone core." + suffix);
    }

    public static boolean unlinkPerimeter(ServerLevel level, BlockPos perimeter) {
        WorldWards data = get(level);
        Long anchor = data.perimeterLinks.get(perimeter.asLong());
        boolean removed = anchor != null;
        if (removed) {
            BlockPos masterAnchor = data.normalizeAnchorOrSelf(level, BlockPos.of(anchor));
            data.wards.stream()
                    .filter(ward -> ward.anchor.equals(masterAnchor) && usesGeneratedShell(ward.spellKey))
                    .forEach(ward -> removeWardInfrastructure(level, ward));
            data.perimeterLinks.remove(perimeter.asLong());
            data.refreshActiveWardVolume(level, masterAnchor);
            data.setDirty();
        }
        return removed;
    }

    public static Optional<String> describe(ServerLevel level, BlockPos anchor) {
        WorldWards data = get(level);
        BlockPos masterAnchor = data.normalizeAnchorOrSelf(level, anchor);
        List<ActiveWard> anchoredWards = data.wards.stream()
                .filter(ward -> ward.anchor.equals(masterAnchor))
                .toList();
        if (anchoredWards.isEmpty()) {
            return Optional.empty();
        }
        int spaces = anchoredWards.get(0).spaces.size();
        List<String> names = anchoredWards.stream().map(ward -> ward.displayName).toList();
        int passiveCost = anchoredWards.stream()
                .mapToInt(ward -> passiveWardCostPerSecond(ward, SpellRegistry.payloadParts(ward.spellKey)))
                .sum();
        int activeCost = anchoredWards.stream()
                .mapToInt(ward -> activeWardCost(SpellRegistry.payloadParts(ward.spellKey)))
                .sum();
        String charge = WardStoneBlockEntity.getOrCreate(level, masterAnchor)
                .map(wardStone -> "MF " + wardStone.magicFlow() + "/" + wardStone.magicFlowCapacity() + ", " + wardStone.multiblockSize() + " stones, ")
                .orElse("");
        int slots = WardStoneBlockEntity.getOrCreate(level, masterAnchor).map(WardStoneBlockEntity::wardSlots).orElse(anchoredWards.size());
        return Optional.of(charge + anchoredWards.size() + "/" + slots + " wards / " + spaces + " spaces, passive " + passiveCost + " MF/s, active up to " + activeCost + " MF/use: " + String.join(", ", names));
    }

    public static Optional<HudSummary> hudSummary(ServerLevel level, BlockPos target, Optional<BlockPos> selectedAnchor) {
        WorldWards data = get(level);
        boolean wardStone = level.getBlockState(target).is(ModBlocks.WARD_STONE.get());
        boolean perimeterStone = level.getBlockState(target).is(ModBlocks.WARD_PERIMETER_STONE.get());
        Long linkedAnchor = data.perimeterLinks.get(target.asLong());

        if (wardStone) {
            Optional<WardStoneBlockEntity.WardMultiblock> core = WardStoneBlockEntity.findMultiblock(level, target);
            if (core.isEmpty()) {
                return Optional.empty();
            }
            data.migrateWardCore(level, core.get());
            BlockPos master = core.get().master();
            int active = data.countWardsAt(master);
            List<String> lines = new ArrayList<>();
            List<String> names = data.wardNamesAt(master);
            if (names.isEmpty()) {
                lines.add("Active wards: none");
            } else {
                lines.add("Active wards: " + active);
                names.stream().limit(5).forEach(name -> lines.add("- " + name));
            }
            return Optional.of(new HudSummary("Ward Stone", List.copyOf(lines), Optional.of(master)));
        }

        if (perimeterStone || linkedAnchor != null) {
            List<String> lines = new ArrayList<>();
            if (linkedAnchor == null) {
                lines.add("Linked: no");
            } else {
                BlockPos anchor = data.normalizeAnchorOrSelf(level, BlockPos.of(linkedAnchor));
                lines.add("Linked: " + anchor.toShortString());
                computeArea(level, anchor).ifPresentOrElse(
                        area -> lines.add("Volume: " + area.spaces().size() + " blocks"),
                        () -> lines.add("Volume: incomplete"));
            }
            selectedAnchor.ifPresent(anchor -> {
                BlockPos normalized = data.normalizeAnchorOrSelf(level, anchor);
                lines.add("Selected: " + normalized.toShortString());
                boolean linkedToSelected = linkedAnchor != null && data.normalizeAnchorOrSelf(level, BlockPos.of(linkedAnchor)).equals(normalized);
                lines.add(linkedToSelected ? "Sneak-click to unlink" : "Click to link selected core");
            });
            return Optional.of(new HudSummary("Ward Perimeter", List.copyOf(lines), linkedAnchor == null ? selectedAnchor : Optional.of(BlockPos.of(linkedAnchor))));
        }

        if (selectedAnchor.isPresent()) {
            BlockPos anchor = data.normalizeAnchorOrSelf(level, selectedAnchor.get());
            Optional<WardStoneBlockEntity.WardMultiblock> core = WardStoneBlockEntity.findMultiblock(level, anchor);
            if (core.isEmpty()) {
                return Optional.of(new HudSummary("Ward Tuner", List.of("Stored Ward Stone is missing", "Click a Ward Stone to bind"), Optional.empty()));
            }
            Optional<ComputedArea> area = computeArea(level, core.get().master());
            List<String> lines = new ArrayList<>();
            lines.add("Selected: " + core.get().master().toShortString());
            lines.add("Perimeters: " + linkedPerimeters(level, core.get().master()).size() + " linked");
            lines.add(area.map(computedArea -> "Volume: " + computedArea.spaces().size() + " blocks").orElse("Volume: incomplete"));
            lines.add("Click Ward Perimeter Stone to link");
            return Optional.of(new HudSummary("Ward Tuner", List.copyOf(lines), Optional.of(core.get().master())));
        }

        return Optional.empty();
    }

    public static Optional<HudSummary> magicFlowHudSummary(ServerLevel level, BlockPos target) {
        WorldWards data = get(level);
        Optional<WardStoneBlockEntity.WardMultiblock> core = WardStoneBlockEntity.findMultiblock(level, target);
        if (core.isEmpty()) {
            return Optional.empty();
        }
        data.migrateWardCore(level, core.get());
        BlockPos master = core.get().master();
        Optional<WardStoneBlockEntity> wardStoneEntity = WardStoneBlockEntity.getOrCreate(level, master);
        if (wardStoneEntity.isEmpty()) {
            return Optional.empty();
        }

        List<ActiveWard> activeWards = data.wards.stream()
                .filter(ward -> ward.anchor.equals(master))
                .toList();
        List<String> lines = new ArrayList<>();
        WardStoneBlockEntity wardStone = wardStoneEntity.get();
        for (MagicEnergyType type : MagicEnergyType.values()) {
            lines.add(progressLine(type.displayName() + " MF", wardStone.magicFlow(type), wardStone.magicFlowCapacity(type)));
        }
        lines.add("Core: " + wardStone.multiblockSize() + " stones, " + activeWards.size() + "/" + wardStone.wardSlots() + " wards");

        if (activeWards.isEmpty()) {
            lines.add("No active ward drain");
            return Optional.of(new HudSummary("WardStone MF", List.copyOf(lines), Optional.empty()));
        }

        int totalPassive = 0;
        int totalActive = 0;
        for (ActiveWard ward : activeWards) {
            List<String> payloads = SpellRegistry.payloadParts(ward.spellKey);
            totalPassive += passiveWardCostPerSecond(ward, payloads);
            totalActive += activeWardCost(payloads);
        }
        lines.add("Total drain: " + totalPassive + " MF/s");
        lines.add("Active use: up to " + totalActive + " MF");
        return Optional.of(new HudSummary("WardStone MF", List.copyOf(lines), Optional.empty()));
    }

    public static boolean showOutline(ServerLevel level, BlockPos anchor) {
        BlockPos masterAnchor = get(level).normalizeAnchorOrSelf(level, anchor);
        Optional<ComputedArea> area = computeArea(level, masterAnchor);
        if (area.isPresent()) {
            get(level).refreshActiveWardVolume(level, masterAnchor);
            drawOutline(level, area.get().bounds());
            return true;
        }

        Optional<ActiveWard> ward = get(level).wards.stream()
                .filter(activeWard -> activeWard.anchor.equals(masterAnchor))
                .findFirst();
        ward.ifPresent(activeWard -> drawOutline(level, activeWard.bounds));
        return ward.isPresent();
    }

    public static ApplyResult setBiome(ServerLevel level, BlockPos anchor, ResourceLocation biomeId) {
        BlockPos masterAnchor = get(level).normalizeAnchorOrSelf(level, anchor);
        Optional<ActiveWard> ward = get(level).wards.stream()
                .filter(activeWard -> activeWard.anchor.equals(masterAnchor))
                .findFirst();
        Optional<ComputedArea> computedArea = Optional.empty();
        if (ward.isEmpty()) {
            if (!level.getBlockState(masterAnchor).is(ModBlocks.WARD_STONE.get())) {
                return new ApplyResult(false, "There is no Ward Stone at that position.");
            }
            computedArea = computeArea(level, masterAnchor);
            if (computedArea.isEmpty()) {
                return new ApplyResult(false, "The Ward Stone core is not inside a valid 3D perimeter anchor volume.");
            }
        }

        ResourceKey<Biome> biomeKey = ResourceKey.create(Registries.BIOME, biomeId);
        Optional<Holder.Reference<Biome>> biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolder(biomeKey);
        if (biome.isEmpty()) {
            return new ApplyResult(false, "Unknown biome: " + biomeId);
        }

        AABB bounds = ward.isPresent() ? ward.get().bounds : computedArea.orElseThrow().bounds();
        int changed = setBiomeInBounds(level, bounds, biome.get());
        return new ApplyResult(changed > 0, "Changed " + changed + " biome cells inside the ward to " + biomeId + ".");
    }

    public static int count(ServerLevel level) {
        return get(level).wards.size();
    }

    private BlockPos normalizeAnchorOrSelf(ServerLevel level, BlockPos anchor) {
        Optional<WardStoneBlockEntity.WardMultiblock> core = WardStoneBlockEntity.findMultiblock(level, anchor);
        if (core.isEmpty()) {
            return anchor;
        }
        migrateWardCore(level, core.get());
        return core.get().master();
    }

    private void migrateWardCore(ServerLevel level, WardStoneBlockEntity.WardMultiblock core) {
        long masterKey = core.master().asLong();
        boolean changed = false;
        for (int i = 0; i < wards.size(); i++) {
            ActiveWard ward = wards.get(i);
            if (!core.contains(ward.anchor) || ward.anchor.asLong() == masterKey) {
                continue;
            }
            wards.set(i, new ActiveWard(core.master(), ward.owner, ward.spellKey, ward.displayName, ward.spaces, ward.bounds, new HashSet<>(ward.camouflageSpaces)));
            changed = true;
        }

        for (Map.Entry<Long, Long> link : List.copyOf(perimeterLinks.entrySet())) {
            if (link.getValue() != masterKey && core.stones().contains(link.getValue())) {
                perimeterLinks.put(link.getKey(), masterKey);
                changed = true;
            }
        }

        WardStoneBlockEntity.getOrCreate(level, core.master());
        if (changed) {
            setDirty();
        }
    }

    private int countWardsAt(BlockPos anchor) {
        int count = 0;
        for (ActiveWard ward : wards) {
            if (ward.anchor.equals(anchor)) {
                count++;
            }
        }
        return count;
    }

    private List<String> wardNamesAt(BlockPos anchor) {
        return wards.stream()
                .filter(ward -> ward.anchor.equals(anchor))
                .map(ward -> ward.displayName)
                .toList();
    }

    private static String progressLine(String label, int value, int max) {
        return "@bar|" + label + "|" + Math.max(0, value) + "|" + Math.max(1, max);
    }

    private static String wardCostBreakdown(ActiveWard ward, List<String> payloadKeys) {
        int volumeCost = Math.max(1, ward.spaces.size() / VOLUME_PASSIVE_DIVISOR);
        List<String> parts = new ArrayList<>();
        parts.add("base " + BASE_PASSIVE_MAGIC_FLOW);
        parts.add("volume " + volumeCost);
        parts.add("types " + wardMagicTypes(payloadKeys));
        for (String payloadKey : payloadKeys) {
            int passive = PASSIVE_MAGIC_FLOW_COSTS.getOrDefault(payloadKey, 0);
            int active = ACTIVE_MAGIC_FLOW_COSTS.getOrDefault(payloadKey, 0);
            if (passive > 0 || active > 0) {
                parts.add(payloadKey + " " + passive + "/s+" + active + "/use");
            }
        }
        return String.join(", ", parts);
    }

    private static String compactHudLine(String line, int maxLength) {
        if (line.length() <= maxLength) {
            return line;
        }
        return line.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    public static boolean isLightWardAt(ServerLevel level, BlockPos pos) {
        WorldWards data = get(level);
        return data.wards.stream()
                .anyMatch(ward -> ward.contains(pos)
                        && SpellRegistry.payloadParts(ward.spellKey).contains("light")
                        && data.isWardOnline(level, ward));
    }

    public static boolean isExplosionProtected(ServerLevel level, BlockPos pos) {
        return get(level).consumePayloadAt(level, ANTI_EXPLOSION_PAYLOAD, pos);
    }

    public static boolean isMagicSuppressedAt(ServerLevel level, BlockPos pos) {
        return get(level).consumePayloadAt(level, ANTI_MAGIC_PAYLOAD, pos);
    }

    private static boolean consumePayloadAt(ServerLevel level, String payloadKey, BlockPos pos) {
        return get(level).consumePayloadAtInternal(level, payloadKey, pos);
    }

    private boolean consumePayloadAtInternal(ServerLevel level, String payloadKey, BlockPos pos) {
        Optional<ActiveWard> ward = matchingPayloadWard(level, payloadKey, pos);
        return ward.isPresent() && consumeWardActiveCost(level, ward.get(), SpellRegistry.payloadParts(ward.get().spellKey));
    }

    private Optional<ActiveWard> matchingPayloadWard(ServerLevel level, String payloadKey, BlockPos pos) {
        for (ActiveWard ward : wards) {
            if (!ward.contains(pos)) {
                continue;
            }
            List<String> payloads = SpellRegistry.payloadParts(ward.spellKey);
            if (payloads.contains(payloadKey) && isWardOnline(level, ward, payloads)) {
                return Optional.of(ward);
            }
        }
        return Optional.empty();
    }

    private static boolean hasRestrictedWardAt(ServerLevel level, String payloadKey, BlockPos pos, Entity actor) {
        WorldWards data = get(level);
        for (ActiveWard ward : data.wards) {
            if (!ward.contains(pos) || data.isOwnerOrAlly(level, ward, actor)) {
                continue;
            }
            List<String> payloads = SpellRegistry.payloadParts(ward.spellKey);
            if (payloads.contains(payloadKey)
                    && data.isWardOnline(level, ward, payloads)
                    && data.consumeWardActiveCost(level, ward, payloads)) {
                return true;
            }
        }
        return false;
    }

    private boolean isOwnerOrAlly(ServerLevel level, ActiveWard ward, Entity actor) {
        if (actor == null) {
            return false;
        }
        if (actor.getUUID().equals(ward.owner)) {
            return true;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ward.owner);
        return owner != null && MagicAllies.isAlly(owner, actor);
    }

    private boolean consumeWardPassiveCost(ServerLevel level, ActiveWard ward, List<String> payloadKeys) {
        Optional<WardStoneBlockEntity> wardStone = WardStoneBlockEntity.getOrCreate(level, ward.anchor);
        if (wardStone.isEmpty() || !hasMagicFlowCosts(wardStone.get(), passiveWardCostsPerSecond(ward, payloadKeys))) {
            return false;
        }
        return level.getGameTime() % PASSIVE_DRAIN_INTERVAL_TICKS != 0
                || consumeMagicFlowCosts(wardStone.get(), passiveWardCostsPerSecond(ward, payloadKeys));
    }

    private boolean consumeWardActiveCost(ServerLevel level, ActiveWard ward, List<String> payloadKeys) {
        return WardStoneBlockEntity.getOrCreate(level, ward.anchor)
                .map(wardStone -> consumeMagicFlowCosts(wardStone, activeWardCosts(payloadKeys)))
                .orElse(false);
    }

    private boolean isWardOnline(ServerLevel level, ActiveWard ward) {
        return isWardOnline(level, ward, SpellRegistry.payloadParts(ward.spellKey));
    }

    private boolean isWardOnline(ServerLevel level, ActiveWard ward, List<String> payloadKeys) {
        return WardStoneBlockEntity.getOrCreate(level, ward.anchor)
                .map(wardStone -> hasMagicFlowCosts(wardStone, passiveWardCostsPerSecond(ward, payloadKeys)))
                .orElse(false);
    }

    private static int passiveWardCostPerSecond(ActiveWard ward, List<String> payloadKeys) {
        return passiveWardCostsPerSecond(ward, payloadKeys).values().stream().mapToInt(Integer::intValue).sum();
    }

    private static int activeWardCost(List<String> payloadKeys) {
        return activeWardCosts(payloadKeys).values().stream().mapToInt(Integer::intValue).sum();
    }

    private static String costSummary(ActiveWard ward, List<String> payloadKeys) {
        return "Requires " + formatMagicFlowCosts(passiveWardCostsPerSecond(ward, payloadKeys)) + ". Costs: " + passiveWardCostPerSecond(ward, payloadKeys) + " MF/s passive, " + activeWardCost(payloadKeys) + " MF/use active.";
    }

    private static Map<MagicEnergyType, Integer> passiveWardCostsPerSecond(ActiveWard ward, List<String> payloadKeys) {
        Map<MagicEnergyType, Integer> costs = new java.util.EnumMap<>(MagicEnergyType.class);
        MagicEnergyType overheadType = firstWardPayloadMagicType(payloadKeys);
        addMagicFlowCost(costs, overheadType, BASE_PASSIVE_MAGIC_FLOW + Math.max(1, ward.spaces.size() / VOLUME_PASSIVE_DIVISOR));
        for (String payloadKey : payloadKeys) {
            if (payloadKey.equals("ward")) {
                continue;
            }
            addMagicFlowCost(costs, wardPayloadMagicType(payloadKey), PASSIVE_MAGIC_FLOW_COSTS.getOrDefault(payloadKey, 0));
        }
        return costs;
    }

    private static Map<MagicEnergyType, Integer> activeWardCosts(List<String> payloadKeys) {
        Map<MagicEnergyType, Integer> costs = new java.util.EnumMap<>(MagicEnergyType.class);
        addMagicFlowCost(costs, firstWardPayloadMagicType(payloadKeys), BASE_ACTIVE_MAGIC_FLOW);
        for (String payloadKey : payloadKeys) {
            if (payloadKey.equals("ward")) {
                continue;
            }
            addMagicFlowCost(costs, wardPayloadMagicType(payloadKey), ACTIVE_MAGIC_FLOW_COSTS.getOrDefault(payloadKey, 0));
        }
        return costs;
    }

    private static MagicEnergyType firstWardPayloadMagicType(List<String> payloadKeys) {
        return payloadKeys.stream()
                .filter(payloadKey -> !payloadKey.equals("ward"))
                .findFirst()
                .map(WorldWards::wardPayloadMagicType)
                .orElse(MagicEnergyType.ARCANE);
    }

    private static String wardMagicTypes(List<String> payloadKeys) {
        return payloadKeys.stream()
                .filter(payloadKey -> !payloadKey.equals("ward"))
                .map(WorldWards::wardPayloadMagicType)
                .distinct()
                .map(MagicEnergyType::displayName)
                .collect(java.util.stream.Collectors.joining("+"));
    }

    private static void addMagicFlowCost(Map<MagicEnergyType, Integer> costs, MagicEnergyType type, int amount) {
        if (amount > 0) {
            costs.merge(type, amount, Integer::sum);
        }
    }

    private static boolean hasMagicFlowCosts(WardStoneBlockEntity wardStone, Map<MagicEnergyType, Integer> costs) {
        for (Map.Entry<MagicEnergyType, Integer> cost : costs.entrySet()) {
            if (!wardStone.hasMagicFlow(cost.getKey(), cost.getValue())) {
                return false;
            }
        }
        return true;
    }

    private static boolean consumeMagicFlowCosts(WardStoneBlockEntity wardStone, Map<MagicEnergyType, Integer> costs) {
        if (!hasMagicFlowCosts(wardStone, costs)) {
            return false;
        }
        for (Map.Entry<MagicEnergyType, Integer> cost : costs.entrySet()) {
            wardStone.consumeMagicFlow(cost.getKey(), cost.getValue(), false);
        }
        return true;
    }

    private static Optional<String> missingMagicFlowRequirement(WardStoneBlockEntity wardStone, Map<MagicEnergyType, Integer> costs) {
        return costs.entrySet().stream()
                .filter(cost -> !wardStone.hasMagicFlow(cost.getKey(), cost.getValue()))
                .map(cost -> cost.getValue() + " " + cost.getKey().displayName() + " MF")
                .findFirst();
    }

    private static String formatMagicFlowCosts(Map<MagicEnergyType, Integer> costs) {
        return costs.entrySet().stream()
                .map(cost -> cost.getValue() + " " + cost.getKey().displayName() + " MF")
                .collect(java.util.stream.Collectors.joining(" + "));
    }

    private static MagicEnergyType wardPayloadMagicType(String payloadKey) {
        return switch (payloadKey) {
            case "anti_fire", "anti_water", "air", "air_bubble", "bubble", "freeze", "frost", "thaw" -> MagicEnergyType.WATER;
            case "fire", "fireball", "fire_place", "fireguard" -> MagicEnergyType.FIRE;
            case "agility", "blastguard", "charm", "cleanse", "fallguard", "fertility", "fortify", "grow", "heal", "life_ward", "mana_shield", "regenerate", "sanctuary" -> MagicEnergyType.LIFE;
            case "anti_projectile", "levitate", "lightning", "reflect_projectile", "weather" -> MagicEnergyType.STORM;
            case "anchor", "anti_explosion", "anti_grief", "gravity", "item_guard", "lockdown", "storage_lock" -> MagicEnergyType.EARTH;
            case "anti_decay", "anti_summon", "anti_teleport", "arrow", "bind", "blast", "blind", "curse", "dark", "disarm", "entry_filter", "explode", "hex", "lifedrain", "mana_drain", "manaburn", "missile", "overload", "push", "silence", "stasis", "stun", "weakening" -> MagicEnergyType.DARK;
            case "alarm", "anti_magic", "camouflage", "dispel", "light", "nullify", "reflect", "reveal", "temporal" -> MagicEnergyType.ARCANE;
            default -> MagicEnergyType.ARCANE;
        };
    }

    private static Optional<ComputedArea> computeArea(ServerLevel level, BlockPos anchor) {
        Set<Long> linkedPerimeters = linkedPerimeters(level, anchor);
        int minBuildY = level.getMinBuildHeight();
        int maxBuildY = level.getMaxBuildHeight() - 1;
        BlockPos from = new BlockPos(anchor.getX() - PERIMETER_SCAN_RADIUS, Math.max(minBuildY, anchor.getY() - PERIMETER_SCAN_RADIUS), anchor.getZ() - PERIMETER_SCAN_RADIUS);
        BlockPos to = new BlockPos(anchor.getX() + PERIMETER_SCAN_RADIUS, Math.min(maxBuildY, anchor.getY() + PERIMETER_SCAN_RADIUS), anchor.getZ() + PERIMETER_SCAN_RADIUS);
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        int anchors = 0;
        if (linkedPerimeters.isEmpty()) {
            for (BlockPos pos : BlockPos.betweenClosed(from, to)) {
                if (!level.getBlockState(pos).is(ModBlocks.WARD_PERIMETER_STONE.get())) {
                    continue;
                }
                anchors++;
                minX = Math.min(minX, pos.getX());
                minY = Math.min(minY, pos.getY());
                minZ = Math.min(minZ, pos.getZ());
                maxX = Math.max(maxX, pos.getX());
                maxY = Math.max(maxY, pos.getY());
                maxZ = Math.max(maxZ, pos.getZ());
            }
        } else {
            for (long linkedPerimeter : linkedPerimeters) {
                BlockPos pos = BlockPos.of(linkedPerimeter);
                if (!level.getBlockState(pos).is(ModBlocks.WARD_PERIMETER_STONE.get()) && !get(level).isCamouflagedPerimeter(anchor, pos)) {
                    continue;
                }
                anchors++;
                minX = Math.min(minX, pos.getX());
                minY = Math.min(minY, pos.getY());
                minZ = Math.min(minZ, pos.getZ());
                maxX = Math.max(maxX, pos.getX());
                maxY = Math.max(maxY, pos.getY());
                maxZ = Math.max(maxZ, pos.getZ());
            }
        }

        if (anchors < 2
                || anchor.getX() < minX || anchor.getX() > maxX
                || anchor.getY() < minY || anchor.getY() > maxY
                || anchor.getZ() < minZ || anchor.getZ() > maxZ) {
            return Optional.empty();
        }

        Set<Long> spaces = new HashSet<>();
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(minX, minY, minZ), new BlockPos(maxX, maxY, maxZ))) {
            if (spaces.size() >= MAX_SPACES) {
                return Optional.empty();
            }
            spaces.add(pos.asLong());
        }

        if (spaces.isEmpty()) {
            return Optional.empty();
        }
        AABB bounds = new AABB(minX, minY, minZ, maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D);
        return Optional.of(new ComputedArea(Set.copyOf(spaces), bounds));
    }

    private void refreshActiveWardVolume(ServerLevel level, BlockPos anchor) {
        anchor = normalizeAnchorOrSelf(level, anchor);
        Optional<ComputedArea> area = computeArea(level, anchor);
        if (area.isEmpty()) {
            return;
        }

        boolean refreshed = false;
        for (int i = 0; i < wards.size(); i++) {
            ActiveWard ward = wards.get(i);
            if (ward.anchor.equals(anchor)) {
                if (usesGeneratedShell(ward.spellKey)) {
                    removeWardInfrastructure(level, ward);
                }
                wards.set(i, new ActiveWard(ward.anchor, ward.owner, ward.spellKey, ward.displayName, area.get().spaces(), area.get().bounds(), new HashSet<>()));
                refreshed = true;
            }
        }
        if (refreshed) {
            setDirty();
        }
    }

    private static Set<Long> linkedPerimeters(ServerLevel level, BlockPos anchor) {
        long anchorKey = anchor.asLong();
        Set<Long> linked = new HashSet<>();
        WorldWards data = get(level);
        data.perimeterLinks.forEach((perimeter, linkedAnchor) -> {
            if (linkedAnchor == anchorKey) {
                linked.add(perimeter);
            }
        });
        return linked;
    }

    public static boolean isLinkedPerimeter(ServerLevel level, BlockPos perimeter) {
        return get(level).perimeterLinks.containsKey(perimeter.asLong());
    }

    public record HudSummary(String title, List<String> lines, Optional<BlockPos> outlineAnchor) {
    }

    private boolean isCamouflagedPerimeter(BlockPos anchor, BlockPos perimeter) {
        return wards.stream()
                .filter(ward -> ward.anchor.equals(anchor))
                .anyMatch(ward -> ward.camouflageSpaces.contains(perimeter.asLong()));
    }

    private static boolean usesGeneratedShell(String spellKey) {
        List<String> payloads = SpellRegistry.payloadParts(spellKey);
        return payloads.contains("light") || payloads.contains("camouflage") || payloads.contains(AIR_BUBBLE_PAYLOAD);
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        for (ServerLevel level : event.getServer().getAllLevels()) {
            get(level).tick(level);
        }
    }

    @SubscribeEvent
    public static void spawnPlacement(MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getLevel() instanceof ServerLevel level && consumePayloadAt(level, ANTI_SUMMON_PAYLOAD, event.getPos())) {
            event.setResult(Event.Result.DENY);
            return;
        }
        if (event.getEntityType().getCategory() != MobCategory.MONSTER || !isDaylightControlledSpawn(event.getSpawnType())) {
            return;
        }
        if (event.getLevel() instanceof ServerLevel level && isLightWardAt(level, event.getPos())) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void finalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        if (event.getLevel() instanceof ServerLevel level && consumePayloadAt(level, ANTI_SUMMON_PAYLOAD, BlockPos.containing(event.getX(), event.getY(), event.getZ()))) {
            event.setSpawnCancelled(true);
            return;
        }
        if (event.getEntity().getType().getCategory() != MobCategory.MONSTER || !isDaylightControlledSpawn(event.getSpawnType())) {
            return;
        }
        if (event.getLevel() instanceof ServerLevel level && isLightWardAt(level, BlockPos.containing(event.getX(), event.getY(), event.getZ()))) {
            event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent
    public static void blockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && hasRestrictedWardAt(level, ANTI_GRIEF_PAYLOAD, event.getPos(), event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void blockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level && hasRestrictedWardAt(level, ANTI_GRIEF_PAYLOAD, event.getPos(), event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void blockMultiPlace(BlockEvent.EntityMultiPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getReplacedBlockSnapshots().stream().anyMatch(snapshot -> hasRestrictedWardAt(level, ANTI_GRIEF_PAYLOAD, snapshot.getPos(), event.getEntity()))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = event.getPos();
        Player player = event.getEntity();
        boolean storageLocked = level.getBlockEntity(pos) instanceof Container && hasRestrictedWardAt(level, STORAGE_LOCK_PAYLOAD, pos, player);
        boolean griefLocked = hasRestrictedWardAt(level, ANTI_GRIEF_PAYLOAD, pos, player)
                || hasRestrictedWardAt(level, ANTI_GRIEF_PAYLOAD, pos.relative(event.getFace() == null ? Direction.UP : event.getFace()), player);
        if (storageLocked || griefLocked) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void fluidSource(BlockEvent.CreateFluidSourceEvent event) {
        if (event.getLevel() instanceof ServerLevel level && consumePayloadAt(level, AIR_BUBBLE_PAYLOAD, event.getPos())) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void itemPickup(EntityItemPickupEvent event) {
        ItemEntity item = event.getItem();
        if (item.level() instanceof ServerLevel level && hasRestrictedWardAt(level, ITEM_GUARD_PAYLOAD, item.blockPosition(), event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void mobGriefing(EntityMobGriefingEvent event) {
        if (event.getEntity().level() instanceof ServerLevel level
                && (consumePayloadAt(level, ANTI_GRIEF_PAYLOAD, event.getEntity().blockPosition())
                || consumePayloadAt(level, "anti_decay", event.getEntity().blockPosition()))) {
            event.setResult(Event.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void teleport(EntityTeleportEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos from = event.getEntity().blockPosition();
        BlockPos to = BlockPos.containing(event.getTarget());
        if (consumePayloadAt(level, ANTI_TELEPORT_PAYLOAD, from) || consumePayloadAt(level, ANTI_TELEPORT_PAYLOAD, to)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void entityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.loadedFromDisk()) {
            return;
        }
        Entity entity = event.getEntity();
        BlockPos pos = entity.blockPosition();
        if (entity instanceof LightningBolt && consumePayloadAt(level, WEATHER_PAYLOAD, pos)) {
            event.setCanceled(true);
            return;
        }
        if (entity instanceof Projectile && consumePayloadAt(level, ANTI_PROJECTILE_PAYLOAD, pos)) {
            event.setCanceled(true);
            return;
        }
        if (entity instanceof Mob && consumePayloadAt(level, ANTI_SUMMON_PAYLOAD, pos)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void explosionDetonate(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        event.getAffectedBlocks().removeIf(pos -> isExplosionProtected(level, pos));
        event.getAffectedEntities().removeIf(entity -> isExplosionProtected(level, entity.blockPosition()));
    }

    @SubscribeEvent
    public static void livingAttack(LivingAttackEvent event) {
        if (!isExplosionDamage(event.getSource()) || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        if (isExplosionProtected(level, event.getEntity().blockPosition())) {
            event.setCanceled(true);
        }
    }

    private static boolean isExplosionDamage(DamageSource source) {
        return source.is(DamageTypes.EXPLOSION)
                || source.is(DamageTypes.PLAYER_EXPLOSION)
                || source.is(DamageTypes.BAD_RESPAWN_POINT);
    }

    private static boolean isDaylightControlledSpawn(MobSpawnType spawnType) {
        return spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.PATROL || spawnType == MobSpawnType.REINFORCEMENT;
    }

    private void tick(ServerLevel level) {
        if (wards.isEmpty()) {
            return;
        }

        boolean changed = false;
        Set<ServerPlayer> currentLightPlayers = new HashSet<>();
        Map<Long, Integer> activeSlotsUsed = new HashMap<>();
        Iterator<ActiveWard> iterator = wards.iterator();
        while (iterator.hasNext()) {
            ActiveWard ward = iterator.next();
            if (!level.getBlockState(ward.anchor).is(ModBlocks.WARD_STONE.get())) {
                if (usesGeneratedShell(ward.spellKey)) {
                    removeWardInfrastructure(level, ward);
                }
                iterator.remove();
                changed = true;
                continue;
            }

            Optional<WardStoneBlockEntity.WardMultiblock> core = WardStoneBlockEntity.findMultiblock(level, ward.anchor);
            int wardSlots = core.map(WardStoneBlockEntity.WardMultiblock::size).orElse(1);
            long slotKey = core.map(wardMultiblock -> wardMultiblock.master().asLong()).orElse(ward.anchor.asLong());
            int usedSlots = activeSlotsUsed.getOrDefault(slotKey, 0);
            if (usedSlots >= wardSlots) {
                if (usesGeneratedShell(ward.spellKey)) {
                    removeWardInfrastructure(level, ward);
                }
                continue;
            }
            activeSlotsUsed.put(slotKey, usedSlots + 1);

            List<String> payloadKeys = SpellRegistry.payloadParts(ward.spellKey);
            if (!consumeWardPassiveCost(level, ward, payloadKeys)) {
                if (usesGeneratedShell(ward.spellKey)) {
                    removeWardInfrastructure(level, ward);
                }
                continue;
            }
            applyEnvironmentalEffects(level, ward, payloadKeys, currentLightPlayers);
            ward.tickCooldowns();
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ward.owner);
            if (owner == null) {
                continue;
            }

            List<PayloadEffect> payloads = SpellRegistry.payloadEffectsExcluding(ward.spellKey, PASSIVE_WARD_PAYLOADS);
            if (payloads.isEmpty()) {
                continue;
            }

            TargetRule targetRule = TargetRule.fromShape(SpellRegistry.shapeKey(ward.spellKey));
            List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, ward.bounds, entity -> entity.isAlive() && ward.contains(entity.blockPosition()) && targetRule.matches(owner, entity));
            for (LivingEntity entity : candidates) {
                if (ward.cooldown(entity) > 0) {
                    continue;
                }
                Spell spell = SpellRegistry.get(ward.spellKey);
                if (spell == null) {
                    continue;
                }
                WandData data = new WandData("ward_stone", "Ward Stone", 1.0F, List.of(new WandData.WandSpellData(ward.spellKey, ward.displayName, spell.manaCost(), spell.cooldownTicks())), 0, 0);
                SpellContext context = new SpellContext(level, owner, new ItemStack(ModItems.WARD_STONE.get()), data, spell).withoutBeams().asAreaCast(ward.anchor.getCenter());
                SpellTarget target = SpellTarget.living(entity);
                boolean applied = false;
                if (!consumeWardActiveCost(level, ward, payloadKeys)) {
                    continue;
                }
                for (PayloadEffect payload : payloads) {
                    applied |= payload.apply(context, target);
                }
                if (applied) {
                    ward.setCooldown(entity, TRIGGER_COOLDOWN_TICKS);
                }
            }
        }
        if (changed) {
            setDirty();
        }
        syncLightWardState(level, currentLightPlayers);
    }

    private void syncLightWardState(ServerLevel level, Set<ServerPlayer> currentLightPlayers) {
        Set<UUID> currentIds = new HashSet<>();
        for (ServerPlayer player : currentLightPlayers) {
            currentIds.add(player.getUUID());
            if (!playersInLightWards.contains(player.getUUID())) {
                ModNetwork.syncWardLight(player, true);
            }
        }

        playersInLightWards.removeIf(playerId -> {
            if (currentIds.contains(playerId)) {
                return false;
            }
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
            if (player != null) {
                ModNetwork.syncWardLight(player, false);
            }
            return true;
        });
        playersInLightWards.addAll(currentIds);
    }

    private static void applyEnvironmentalEffects(ServerLevel level, ActiveWard ward, List<String> payloadKeys, Set<ServerPlayer> currentLightPlayers) {
        boolean antiFire = payloadKeys.contains("anti_fire");
        boolean airBubble = payloadKeys.contains(AIR_BUBBLE_PAYLOAD);
        boolean antiWater = payloadKeys.contains("anti_water") || payloadKeys.contains("air") || airBubble;
        boolean light = payloadKeys.contains("light");
        boolean dark = payloadKeys.contains("dark");
        boolean temporal = payloadKeys.contains("temporal");
        boolean stasis = payloadKeys.contains("stasis");
        boolean fertility = payloadKeys.contains("fertility");
        boolean camouflage = payloadKeys.contains("camouflage");
        boolean alarm = payloadKeys.contains("alarm");
        boolean antiDecay = payloadKeys.contains("anti_decay");
        boolean antiProjectile = payloadKeys.contains(ANTI_PROJECTILE_PAYLOAD);
        boolean cleanse = payloadKeys.contains("cleanse");
        boolean dispel = payloadKeys.contains("dispel");
        boolean entryFilter = payloadKeys.contains("entry_filter");
        boolean freeze = payloadKeys.contains("freeze");
        boolean grow = payloadKeys.contains("grow");
        boolean lockdown = payloadKeys.contains("lockdown");
        boolean manaDrain = payloadKeys.contains("mana_drain");
        boolean reflectProjectile = payloadKeys.contains("reflect_projectile");
        boolean reveal = payloadKeys.contains("reveal");
        boolean sanctuary = payloadKeys.contains("sanctuary");
        boolean thaw = payloadKeys.contains("thaw");
        boolean weakening = payloadKeys.contains("weakening");
        boolean weather = payloadKeys.contains(WEATHER_PAYLOAD);
        if (!antiFire && !antiWater && !light && !dark && !temporal && !stasis && !fertility && !camouflage
                && !alarm && !antiDecay && !antiProjectile && !cleanse && !dispel && !entryFilter && !freeze
                && !grow && !lockdown && !manaDrain && !reflectProjectile && !reveal && !sanctuary && !thaw
                && !weakening && !weather) {
            return;
        }

        if (airBubble) {
            maintainBoundaryShell(level, ward, airBubble);
        }
        if (camouflage) {
            maintainCamouflage(level, ward);
        }
        if (antiProjectile || reflectProjectile || dispel) {
            handleProjectiles(level, ward, antiProjectile || dispel, reflectProjectile);
        }

        for (BlockPos pos : ward.nextScanBatch(ENVIRONMENT_SCAN_PER_TICK)) {
            BlockState state = level.getBlockState(pos);
            if (antiWater && clearWater(level, pos, state)) {
                continue;
            }
            if (freeze && freezeBlock(level, pos, state)) {
                continue;
            }
            if (thaw && thawBlock(level, pos, state)) {
                continue;
            }
            if ((antiDecay || weather) && preserveEnvironment(level, pos, state, antiDecay, weather)) {
                continue;
            }
            if (antiFire) {
                if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    continue;
                }
                if (state.is(Blocks.LAVA)) {
                    level.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
                    continue;
                }
            }
            if (light && !airBubble && state.is(ModBlocks.WARD_BOUNDARY.get())) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                continue;
            }
            if (light && state.is(ModBlocks.WARD.get())) {
                if (!shouldPlaceWardInterior(ward, pos)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                } else if (state.getValue(WardBlock.LEVEL) != 15) {
                    level.setBlock(pos, state.setValue(WardBlock.LEVEL, 15), 3);
                }
                continue;
            }
            if (light && shouldPlaceWardInterior(ward, pos) && state.isAir()) {
                level.setBlock(pos, ModBlocks.WARD.get().defaultBlockState().setValue(WardBlock.LEVEL, 15), 3);
                continue;
            }
            if ((fertility || light || grow) && level.getGameTime() % growthInterval(light, fertility, grow) == 0 && state.getBlock() instanceof BonemealableBlock growable && growable.isValidBonemealTarget(level, pos, state, false)) {
                growable.performBonemeal(level, level.random, pos, state);
            }
        }

        boolean needsFastEntityTick = entryFilter || lockdown;
        if (!needsFastEntityTick && level.getGameTime() % 10 != 0) {
            return;
        }

        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, ward.bounds, entity -> entity.isAlive() && ward.contains(entity.blockPosition()));
        for (LivingEntity entity : entities) {
            if (antiFire) {
                entity.clearFire();
            }
            boolean targeted = wardTargetsEntity(level, ward, entity);
            boolean alarmReady = alarm && targeted && ward.alarmCooldown(entity) <= 0;
            boolean needsActiveEntityCharge = alarmReady
                    || entryFilter && targeted
                    || lockdown && targeted
                    || light && entity.getType().getCategory() == MobCategory.MONSTER && entity.isInvertedHealAndHarm()
                    || dark
                    || reveal
                    || cleanse
                    || dispel
                    || sanctuary
                    || weakening && targeted
                    || manaDrain && targeted && entity instanceof ServerPlayer && level.getGameTime() % 20 == 0
                    || temporal
                    || stasis;
            if (needsActiveEntityCharge && !get(level).consumeWardActiveCost(level, ward, payloadKeys)) {
                continue;
            }
            if (alarmReady) {
                notifyWardAlarm(level, ward, entity);
                ward.setAlarmCooldown(entity, 200);
            }
            if (entryFilter && targeted) {
                ejectFromWard(ward, entity);
                continue;
            }
            if (lockdown && targeted) {
                ward.lockdownEntities.add(entity.getUUID());
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, true, false));
            }
            if (antiWater || payloadKeys.contains("air") || airBubble) {
                entity.setAirSupply(entity.getMaxAirSupply());
                entity.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 60, 0, true, false));
            }
            if (light && entity instanceof ServerPlayer player) {
                currentLightPlayers.add(player);
            }
            if (light && entity.getType().getCategory() == MobCategory.MONSTER && entity.isInvertedHealAndHarm()) {
                entity.setSecondsOnFire(6);
            }
            if (dark) {
                entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, true, false));
            }
            if (reveal) {
                entity.removeEffect(MobEffects.INVISIBILITY);
                entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0, true, false));
            }
            if (cleanse) {
                cleanseEntity(entity);
            }
            if (dispel) {
                MagicStatusEffects.nullify(entity);
            }
            if (sanctuary) {
                applySanctuary(level, ward, entity);
            }
            if (weakening && targeted) {
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, true, false));
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0, true, false));
            }
            if (manaDrain && targeted && entity instanceof ServerPlayer player && level.getGameTime() % 20 == 0) {
                PlayerMana mana = PlayerMana.get(player);
                mana.spend(Math.min(2, mana.mana()));
                ModNetwork.syncMana(player, mana);
            }
            if (temporal) {
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 0, true, false));
                entity.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 80, 0, true, false));
            }
            if (stasis) {
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2, true, false));
                entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 80, 1, true, false));
            }
        }
        if (lockdown) {
            enforceLockdown(level, ward);
        }
    }

    private static boolean clearWater(ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.getFluidState().is(FluidTags.WATER)) {
            return false;
        }
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            level.setBlock(pos, state.setValue(BlockStateProperties.WATERLOGGED, false), 3);
        } else {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
        return true;
    }

    private static boolean freezeBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getFluidState().is(FluidTags.WATER)) {
            level.setBlock(pos, Blocks.ICE.defaultBlockState(), 3);
            return true;
        }
        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return true;
        }
        return false;
    }

    private static boolean thawBlock(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.FROSTED_ICE)) {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
            return true;
        }
        if (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return true;
        }
        return false;
    }

    private static boolean preserveEnvironment(ServerLevel level, BlockPos pos, BlockState state, boolean antiDecay, boolean weather) {
        if (antiDecay && state.hasProperty(LeavesBlock.PERSISTENT) && !state.getValue(LeavesBlock.PERSISTENT)) {
            level.setBlock(pos, state.setValue(LeavesBlock.PERSISTENT, true), 3);
            return true;
        }
        if (antiDecay && state.hasProperty(FarmBlock.MOISTURE) && state.getValue(FarmBlock.MOISTURE) < FarmBlock.MAX_MOISTURE) {
            level.setBlock(pos, state.setValue(FarmBlock.MOISTURE, FarmBlock.MAX_MOISTURE), 3);
            return true;
        }
        if (weather && (state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            return true;
        }
        return false;
    }

    private static void cleanseEntity(LivingEntity entity) {
        MagicStatusEffects.cleanse(entity);
    }

    private static void applySanctuary(ServerLevel level, ActiveWard ward, LivingEntity entity) {
        if (entity instanceof ServerPlayer player && get(level).isOwnerOrAlly(level, ward, player)) {
            if (level.getGameTime() % 40 == 0) {
                player.heal(1.0F);
            }
            return;
        }
        if (entity.getType().getCategory() == MobCategory.MONSTER) {
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1, true, false));
            ejectFromWard(ward, entity);
        }
    }

    private static boolean wardTargetsEntity(ServerLevel level, ActiveWard ward, LivingEntity entity) {
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ward.owner);
        TargetRule targetRule = TargetRule.fromShape(SpellRegistry.shapeKey(ward.spellKey));
        if (owner != null) {
            return targetRule.matches(owner, entity);
        }
        return switch (targetRule.mode()) {
            case NON_ALLIED -> true;
            case HOSTILE -> entity.getType().getCategory() == MobCategory.MONSTER;
            case PLAYERS -> entity instanceof ServerPlayer;
            case ALLIES -> false;
            case MOBS -> entity instanceof Mob;
            case MONSTERS -> entity.getType().getCategory() == MobCategory.MONSTER;
            case PASSIVE -> TargetRule.isPassiveCategory(entity.getType().getCategory());
            case ANIMALS -> TargetRule.isAnimalCategory(entity.getType().getCategory());
            case PLAYER_NAME -> targetRule.matches(null, entity);
            case ENTITY_TYPE -> targetRule.matches(null, entity);
            case ANY -> true;
        };
    }

    private static void ejectFromWard(ActiveWard ward, LivingEntity entity) {
        Vec3 center = ward.bounds.getCenter();
        Vec3 delta = entity.position().subtract(center);
        double ax = Math.abs(delta.x);
        double ay = Math.abs(delta.y);
        double az = Math.abs(delta.z);
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        if (ax >= ay && ax >= az) {
            x = delta.x >= 0.0D ? ward.bounds.maxX + 0.35D : ward.bounds.minX - 0.35D;
        } else if (az >= ax && az >= ay) {
            z = delta.z >= 0.0D ? ward.bounds.maxZ + 0.35D : ward.bounds.minZ - 0.35D;
        } else {
            y = delta.y >= 0.0D ? ward.bounds.maxY + 0.35D : ward.bounds.minY - 0.35D;
        }
        entity.teleportTo(x, y, z);
        Vec3 push = entity.position().subtract(center).normalize().scale(0.45D);
        entity.setDeltaMovement(push.x, Math.max(0.08D, push.y), push.z);
        entity.hurtMarked = true;
    }

    private static void handleProjectiles(ServerLevel level, ActiveWard ward, boolean discard, boolean reflect) {
        List<Projectile> projectiles = level.getEntitiesOfClass(Projectile.class, ward.bounds.inflate(1.0D), projectile -> projectile.isAlive() && ward.contains(projectile.blockPosition()));
        for (Projectile projectile : projectiles) {
            if (reflect) {
                reflectProjectile(ward, projectile);
            } else if (discard) {
                projectile.discard();
            }
        }
    }

    private static void reflectProjectile(ActiveWard ward, Projectile projectile) {
        long anchor = projectile.getPersistentData().getLong(PROJECTILE_REFLECT_TAG);
        if (anchor == ward.anchor.asLong()) {
            return;
        }
        projectile.getPersistentData().putLong(PROJECTILE_REFLECT_TAG, ward.anchor.asLong());
        Vec3 center = ward.bounds.getCenter();
        Vec3 away = projectile.position().subtract(center).normalize();
        if (away.lengthSqr() < 0.01D) {
            away = new Vec3(0.0D, 0.1D, 1.0D);
        }
        double speed = Math.max(0.8D, projectile.getDeltaMovement().length());
        projectile.setDeltaMovement(away.scale(speed));
        projectile.hurtMarked = true;
    }

    private static void notifyWardAlarm(ServerLevel level, ActiveWard ward, LivingEntity entity) {
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ward.owner);
        if (owner == null) {
            return;
        }
        PlayerTitles.show(owner,
                Component.literal("Ward Alarm").withStyle(ChatFormatting.GOLD),
                Component.literal(entity.getDisplayName().getString() + " entered " + ward.anchor.toShortString()).withStyle(ChatFormatting.YELLOW),
                5, 35, 10);
        level.sendParticles(ParticleTypes.NOTE, ward.anchor.getX() + 0.5D, ward.anchor.getY() + 1.2D, ward.anchor.getZ() + 0.5D, 8, 0.35D, 0.35D, 0.35D, 0.0D);
    }

    private static void enforceLockdown(ServerLevel level, ActiveWard ward) {
        if (ward.lockdownEntities.isEmpty()) {
            return;
        }
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, ward.bounds.inflate(4.0D), entity -> entity.isAlive() && ward.lockdownEntities.contains(entity.getUUID()));
        for (LivingEntity entity : nearby) {
            if (!ward.contains(entity.blockPosition())) {
                Vec3 center = ward.bounds.getCenter();
                entity.teleportTo(center.x, Math.max(ward.bounds.minY + 1.0D, center.y), center.z);
                entity.setDeltaMovement(Vec3.ZERO);
                entity.hurtMarked = true;
            }
        }
        ward.lockdownEntities.removeIf(entityId -> nearby.stream().noneMatch(entity -> entity.getUUID().equals(entityId)));
    }

    private static void removeWardInfrastructure(ServerLevel level, ActiveWard ward) {
        for (long space : ward.spaces) {
            BlockPos pos = BlockPos.of(space);
            if (level.getBlockState(pos).is(ModBlocks.WARD.get()) || level.getBlockState(pos).is(ModBlocks.WARD_BOUNDARY.get())) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        for (long camouflageSpace : Set.copyOf(ward.camouflageSpaces)) {
            restoreCamouflagePosition(level, ward, BlockPos.of(camouflageSpace));
        }
        ward.camouflageSpaces.clear();
    }

    private static void maintainBoundaryShell(ServerLevel level, ActiveWard ward, boolean replaceWater) {
        BlockState boundaryState = ModBlocks.WARD_BOUNDARY.get()
                .defaultBlockState()
                .setValue(WardBoundaryBlock.WATER_TEXTURED, replaceWater);
        for (long space : ward.spaces) {
            BlockPos pos = BlockPos.of(space);
            if (!isBoundaryPosition(ward, pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(ModBlocks.WARD_BOUNDARY.get())) {
                if (!state.equals(boundaryState)) {
                    level.setBlock(pos, boundaryState, 3);
                }
                continue;
            }
            if (state.isAir() || state.is(ModBlocks.WARD.get()) || replaceWater && state.getFluidState().is(FluidTags.WATER)) {
                level.setBlock(pos, boundaryState, 3);
            }
        }
    }

    private static void maintainCamouflage(ServerLevel level, ActiveWard ward) {
        Set<Long> targets = camouflageTargets(level, ward);
        for (long camouflaged : Set.copyOf(ward.camouflageSpaces)) {
            if (!targets.contains(camouflaged)) {
                restoreCamouflagePosition(level, ward, BlockPos.of(camouflaged));
                ward.camouflageSpaces.remove(camouflaged);
            }
        }

        List<Long> orderedTargets = new ArrayList<>(targets);
        orderedTargets.sort(Comparator.comparingInt(target -> BlockPos.of(target).getY()));
        for (long target : orderedTargets) {
            BlockPos pos = BlockPos.of(target);
            BlockState state = level.getBlockState(pos);
            if (!isCamouflageReplaceable(state, ward, pos)) {
                continue;
            }
            Optional<BlockState> camouflageState = camouflageStateFor(level, ward, pos);
            if (camouflageState.isPresent()) {
                setCamouflagePlaceholder(level, pos, camouflageState.get());
            } else if (isLinkedPerimeterForWard(level, ward, pos)) {
                if (!state.isAir()) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            } else if (state.is(ModBlocks.WARD_BOUNDARY.get())) {
                continue;
            } else if (!state.isAir()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
            ward.camouflageSpaces.add(target);
        }
    }

    private static Set<Long> camouflageTargets(ServerLevel level, ActiveWard ward) {
        Set<Long> targets = new HashSet<>();
        for (long space : ward.spaces) {
            BlockPos pos = BlockPos.of(space);
            if (isBoundaryPosition(ward, pos)) {
                targets.add(space);
            }
        }
        addCamouflageTerrainBlendTargets(level, ward, targets);
        targets.addAll(linkedPerimeters(level, ward.anchor));
        return targets;
    }

    private static void addCamouflageTerrainBlendTargets(ServerLevel level, ActiveWard ward, Set<Long> targets) {
        int minX = (int) Math.floor(ward.bounds.minX);
        int minZ = (int) Math.floor(ward.bounds.minZ);
        int maxX = (int) Math.ceil(ward.bounds.maxX) - 1;
        int maxY = (int) Math.ceil(ward.bounds.maxY) - 1;
        int maxZ = (int) Math.ceil(ward.bounds.maxZ) - 1;

        for (int x = minX - CAMOUFLAGE_BLEND_RADIUS; x <= maxX + CAMOUFLAGE_BLEND_RADIUS; x++) {
            for (int z = minZ - CAMOUFLAGE_BLEND_RADIUS; z <= maxZ + CAMOUFLAGE_BLEND_RADIUS; z++) {
                int distance = distanceOutsideFootprint(x, z, minX, minZ, maxX, maxZ);
                if (distance > CAMOUFLAGE_BLEND_RADIUS) {
                    continue;
                }

                if (distance == 0) {
                    addCamouflageTopRelief(level, ward, targets, x, maxY, z);
                    continue;
                }

                int surfaceY = naturalSurfaceY(level, ward, x, z);
                if (surfaceY == Integer.MIN_VALUE) {
                    continue;
                }

                int edgeY = Math.min(maxY, surfaceY + CAMOUFLAGE_MAX_RISE);
                if (edgeY <= surfaceY) {
                    continue;
                }

                double blend = (CAMOUFLAGE_BLEND_RADIUS + 1 - distance) / (double) (CAMOUFLAGE_BLEND_RADIUS + 1);
                int relief = Math.floorMod(camouflageNoise(new BlockPos(x, edgeY, z), 71), 3) - 1;
                int targetY = surfaceY + (int) Math.round((edgeY - surfaceY) * blend) + relief;
                targetY = Math.max(surfaceY, Math.min(edgeY, targetY));
                for (int y = surfaceY + 1; y <= targetY; y++) {
                    addCamouflageTarget(level, ward, targets, new BlockPos(x, y, z));
                }
            }
        }
    }

    private static int distanceOutsideFootprint(int x, int z, int minX, int minZ, int maxX, int maxZ) {
        int dx = x < minX ? minX - x : x > maxX ? x - maxX : 0;
        int dz = z < minZ ? minZ - z : z > maxZ ? z - maxZ : 0;
        return Math.max(dx, dz);
    }

    private static void addCamouflageTopRelief(ServerLevel level, ActiveWard ward, Set<Long> targets, int x, int topY, int z) {
        int height = camouflageTopRelief(new BlockPos(x, topY, z));
        for (int y = 1; y <= height; y++) {
            addCamouflageTarget(level, ward, targets, new BlockPos(x, topY + y, z));
        }
    }

    private static void addCamouflageTarget(ServerLevel level, ActiveWard ward, Set<Long> targets, BlockPos pos) {
        if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight() || ward.contains(pos)) {
            return;
        }
        targets.add(pos.asLong());
    }

    private static int naturalSurfaceY(ServerLevel level, ActiveWard ward, int x, int z) {
        int startY = Math.min(level.getMaxBuildHeight() - 1, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + CAMOUFLAGE_MAX_RISE);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, startY, z);
        for (int y = startY; y >= level.getMinBuildHeight(); y--) {
            pos.setY(y);
            BlockState state = level.getBlockState(pos);
            if (isNaturalTerrainSurface(state, ward, pos)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private static boolean isNaturalTerrainSurface(BlockState state, ActiveWard ward, BlockPos pos) {
        return !ward.camouflageSpaces.contains(pos.asLong())
                && !state.isAir()
                && state.getFluidState().isEmpty()
                && !state.is(ModBlocks.WARD.get())
                && !state.is(ModBlocks.WARD_STONE.get())
                && !state.is(ModBlocks.WARD_PERIMETER_STONE.get())
                && !state.is(ModBlocks.WARD_CAMOUFLAGE.get())
                && !state.is(ModBlocks.WARD_BOUNDARY.get());
    }

    private static int camouflageTopRelief(BlockPos pos) {
        int noise = Math.floorMod(camouflageNoise(pos, 43), 13);
        if (noise == 0) {
            return 3;
        }
        if (noise <= 3) {
            return 2;
        }
        return noise <= 7 ? 1 : 0;
    }

    private static int camouflageNoise(BlockPos pos, int salt) {
        long value = pos.asLong() ^ (long) salt * 0x9E3779B97F4A7C15L;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (int) value;
    }

    private static boolean isCamouflageReplaceable(BlockState state, ActiveWard ward, BlockPos pos) {
        if (state.is(ModBlocks.WARD_STONE.get())) {
            return false;
        }
        return state.isAir()
                || state.is(ModBlocks.WARD.get())
                || state.is(ModBlocks.WARD_BOUNDARY.get())
                || state.is(ModBlocks.WARD_CAMOUFLAGE.get())
                || state.is(ModBlocks.WARD_PERIMETER_STONE.get())
                || ward.camouflageSpaces.contains(pos.asLong());
    }

    private static void setCamouflagePlaceholder(ServerLevel level, BlockPos pos, BlockState mimicState) {
        BlockState placeholder = ModBlocks.WARD_CAMOUFLAGE.get().defaultBlockState();
        if (!level.getBlockState(pos).is(ModBlocks.WARD_CAMOUFLAGE.get())) {
            level.setBlock(pos, placeholder, 3);
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof WardCamouflageBlockEntity camouflageBlockEntity) {
            camouflageBlockEntity.setMimicState(mimicState);
        }
    }

    private static void restoreCamouflagePosition(ServerLevel level, ActiveWard ward, BlockPos pos) {
        if (isLinkedPerimeterForWard(level, ward, pos)) {
            level.setBlock(pos, ModBlocks.WARD_PERIMETER_STONE.get().defaultBlockState(), 3);
            return;
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    private static boolean isLinkedPerimeterForWard(ServerLevel level, ActiveWard ward, BlockPos pos) {
        Long anchor = get(level).perimeterLinks.get(pos.asLong());
        return anchor != null && anchor == ward.anchor.asLong();
    }

    private static Optional<BlockState> camouflageStateFor(ServerLevel level, ActiveWard ward, BlockPos pos) {
        List<Direction> outwardDirections = outwardDirections(ward, pos);
        Optional<BlockState> faceState = firstCamouflageState(level, pos, outwardDirections);
        if (faceState.isPresent()) {
            return Optional.of(stableCamouflageState(level, ward, pos, faceState.get()));
        }

        CamouflageCandidate best = null;
        for (int radius = 1; radius <= 4; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz)) != radius) {
                            continue;
                        }
                        BlockPos samplePos = pos.offset(dx, dy, dz);
                        BlockState sample = level.getBlockState(samplePos);
                        if (!isUsableCamouflageState(sample)) {
                            continue;
                        }
                        int score = camouflageScore(ward, pos, samplePos, outwardDirections);
                        if (best == null || score < best.score()) {
                            best = new CamouflageCandidate(sample, score);
                        }
                    }
                }
            }
            if (best != null && best.score() < 1000) {
                break;
            }
        }
        return best == null
                ? fallbackCamouflageStateFor(level, ward, pos)
                : Optional.of(stableCamouflageState(level, ward, pos, best.state()));
    }

    private static Optional<BlockState> fallbackCamouflageStateFor(ServerLevel level, ActiveWard ward, BlockPos pos) {
        CamouflageCandidate best = null;
        int minY = Math.max(level.getMinBuildHeight(), pos.getY() - CAMOUFLAGE_MAX_RISE - 12);
        for (int radius = 0; radius <= CAMOUFLAGE_BLEND_RADIUS + 4; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    for (int y = pos.getY(); y >= minY; y--) {
                        BlockPos samplePos = new BlockPos(pos.getX() + dx, y, pos.getZ() + dz);
                        BlockState sample = level.getBlockState(samplePos);
                        if (!isUsableCamouflageState(sample)) {
                            continue;
                        }
                        int score = dx * dx + dz * dz + (pos.getY() - y) * 3 + (ward.contains(samplePos) ? 900 : 0);
                        if (best == null || score < best.score()) {
                            best = new CamouflageCandidate(sample, score);
                        }
                    }
                }
            }
            if (best != null) {
                return Optional.of(stableCamouflageState(level, ward, pos, best.state()));
            }
        }
        return Optional.of(Blocks.STONE.defaultBlockState());
    }

    private static Optional<BlockState> firstCamouflageState(ServerLevel level, BlockPos pos, List<Direction> outwardDirections) {
        Iterable<Direction> directions = outwardDirections.isEmpty() ? List.of(Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP) : outwardDirections;
        for (Direction direction : directions) {
            BlockPos.MutableBlockPos samplePos = pos.relative(direction).mutable();
            for (int i = 0; i < 4; i++) {
                BlockState sample = level.getBlockState(samplePos);
                if (isUsableCamouflageState(sample)) {
                    return Optional.of(sample);
                }
                samplePos.move(direction);
            }
        }
        return Optional.empty();
    }

    private static int camouflageScore(ActiveWard ward, BlockPos target, BlockPos sample, List<Direction> outwardDirections) {
        int dx = sample.getX() - target.getX();
        int dy = sample.getY() - target.getY();
        int dz = sample.getZ() - target.getZ();
        int score = (dx * dx + dy * dy + dz * dz) * 12 + Math.abs(dy) * 9;
        boolean outsideWard = !ward.contains(sample);
        score += outsideWard ? 0 : 900;
        if (!outwardDirections.isEmpty() && isInOutwardDirection(target, sample, outwardDirections)) {
            score -= 160;
        }
        if (dy == 0) {
            score -= 40;
        }
        return score;
    }

    private static BlockState stableCamouflageState(ServerLevel level, ActiveWard ward, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof FallingBlock) || hasCamouflageSupport(level, ward, pos)) {
            return state;
        }
        if (state.is(Blocks.SAND)) {
            return Blocks.SANDSTONE.defaultBlockState();
        }
        if (state.is(Blocks.RED_SAND)) {
            return Blocks.RED_SANDSTONE.defaultBlockState();
        }
        if (state.is(Blocks.GRAVEL)) {
            return Blocks.STONE.defaultBlockState();
        }
        return Blocks.STONE.defaultBlockState();
    }

    private static boolean hasCamouflageSupport(ServerLevel level, ActiveWard ward, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        return belowState.isFaceSturdy(level, below, Direction.UP) || ward.camouflageSpaces.contains(below.asLong());
    }

    private static boolean isInOutwardDirection(BlockPos target, BlockPos sample, List<Direction> outwardDirections) {
        int dx = sample.getX() - target.getX();
        int dy = sample.getY() - target.getY();
        int dz = sample.getZ() - target.getZ();
        for (Direction direction : outwardDirections) {
            if (direction.getAxis() == Direction.Axis.X && Integer.signum(dx) == direction.getStepX()) {
                return true;
            }
            if (direction.getAxis() == Direction.Axis.Y && Integer.signum(dy) == direction.getStepY()) {
                return true;
            }
            if (direction.getAxis() == Direction.Axis.Z && Integer.signum(dz) == direction.getStepZ()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isUsableCamouflageState(BlockState state) {
        return !state.isAir()
                && state.getFluidState().isEmpty()
                && !state.hasBlockEntity()
                && !state.is(ModBlocks.WARD.get())
                && !state.is(ModBlocks.WARD_STONE.get())
                && !state.is(ModBlocks.WARD_PERIMETER_STONE.get())
                && !state.is(ModBlocks.WARD_CAMOUFLAGE.get())
                && !state.is(ModBlocks.WARD_BOUNDARY.get())
                && (state.canOcclude() || state.getBlock() instanceof LeavesBlock);
    }

    private static List<Direction> outwardDirections(ActiveWard ward, BlockPos pos) {
        List<Direction> directions = new ArrayList<>(3);
        int minX = (int) Math.floor(ward.bounds.minX);
        int minY = (int) Math.floor(ward.bounds.minY);
        int minZ = (int) Math.floor(ward.bounds.minZ);
        int maxX = (int) Math.ceil(ward.bounds.maxX) - 1;
        int maxY = (int) Math.ceil(ward.bounds.maxY) - 1;
        int maxZ = (int) Math.ceil(ward.bounds.maxZ) - 1;
        if (pos.getX() == minX) {
            directions.add(Direction.WEST);
        }
        if (pos.getX() == maxX) {
            directions.add(Direction.EAST);
        }
        if (pos.getY() == minY) {
            directions.add(Direction.DOWN);
        }
        if (pos.getY() == maxY) {
            directions.add(Direction.UP);
        }
        if (pos.getZ() == minZ) {
            directions.add(Direction.NORTH);
        }
        if (pos.getZ() == maxZ) {
            directions.add(Direction.SOUTH);
        }
        return directions;
    }

    private static boolean isBoundaryPosition(ActiveWard ward, BlockPos pos) {
        int minX = (int) Math.floor(ward.bounds.minX);
        int minY = (int) Math.floor(ward.bounds.minY);
        int minZ = (int) Math.floor(ward.bounds.minZ);
        int maxX = (int) Math.ceil(ward.bounds.maxX) - 1;
        int maxY = (int) Math.ceil(ward.bounds.maxY) - 1;
        int maxZ = (int) Math.ceil(ward.bounds.maxZ) - 1;
        return pos.getX() == minX || pos.getX() == maxX
                || pos.getY() == minY || pos.getY() == maxY
                || pos.getZ() == minZ || pos.getZ() == maxZ;
    }

    private static boolean shouldPlaceWardInterior(ActiveWard ward, BlockPos pos) {
        if (isBoundaryPosition(ward, pos)) {
            return false;
        }
        return ward.spaces.contains(pos.asLong());
    }

    private static int growthInterval(boolean light, boolean fertility, boolean grow) {
        if (grow && light) {
            return 2;
        }
        if (grow || light && fertility) {
            return 4;
        }
        return fertility ? 8 : 16;
    }

    private static void drawOutline(ServerLevel level, AABB bounds) {
        double minX = bounds.minX;
        double minY = bounds.minY;
        double minZ = bounds.minZ;
        double maxX = bounds.maxX;
        double maxY = bounds.maxY;
        double maxZ = bounds.maxZ;
        drawLine(level, minX, minY, minZ, maxX, minY, minZ);
        drawLine(level, minX, minY, maxZ, maxX, minY, maxZ);
        drawLine(level, minX, maxY, minZ, maxX, maxY, minZ);
        drawLine(level, minX, maxY, maxZ, maxX, maxY, maxZ);
        drawLine(level, minX, minY, minZ, minX, minY, maxZ);
        drawLine(level, maxX, minY, minZ, maxX, minY, maxZ);
        drawLine(level, minX, maxY, minZ, minX, maxY, maxZ);
        drawLine(level, maxX, maxY, minZ, maxX, maxY, maxZ);
        drawLine(level, minX, minY, minZ, minX, maxY, minZ);
        drawLine(level, maxX, minY, minZ, maxX, maxY, minZ);
        drawLine(level, minX, minY, maxZ, minX, maxY, maxZ);
        drawLine(level, maxX, minY, maxZ, maxX, maxY, maxZ);
    }

    private static void drawLine(ServerLevel level, double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        int steps = Math.max(1, (int) Math.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz)));
        for (int i = 0; i <= steps; i++) {
            double progress = i / (double) steps;
            level.sendParticles(ParticleTypes.END_ROD, x1 + dx * progress, y1 + dy * progress, z1 + dz * progress, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    private static int setBiomeInBounds(ServerLevel level, AABB bounds, Holder<Biome> biome) {
        int minQuartX = Math.floorDiv((int) Math.floor(bounds.minX), 4);
        int minQuartY = Math.floorDiv((int) Math.floor(bounds.minY), 4);
        int minQuartZ = Math.floorDiv((int) Math.floor(bounds.minZ), 4);
        int maxQuartX = Math.floorDiv((int) Math.ceil(bounds.maxX) + 2, 4);
        int maxQuartY = Math.floorDiv((int) Math.ceil(bounds.maxY) + 2, 4);
        int maxQuartZ = Math.floorDiv((int) Math.ceil(bounds.maxZ) + 2, 4);
        int changed = 0;
        Set<ChunkAccess> touchedChunks = new HashSet<>();

        for (int quartX = minQuartX; quartX <= maxQuartX; quartX++) {
            for (int quartZ = minQuartZ; quartZ <= maxQuartZ; quartZ++) {
                LevelChunk chunk = level.getChunk(QuartPos.toSection(quartX), QuartPos.toSection(quartZ));
                touchedChunks.add(chunk);
                for (int quartY = minQuartY; quartY <= maxQuartY; quartY++) {
                    int sectionIndex = level.getSectionIndex(QuartPos.toBlock(quartY));
                    if (sectionIndex < 0 || sectionIndex >= chunk.getSections().length) {
                        continue;
                    }

                    LevelChunkSection section = chunk.getSection(sectionIndex);
                    if (section.getBiomes() instanceof PalettedContainer<Holder<Biome>> biomes) {
                        biomes.set(quartX & 3, quartY & 3, quartZ & 3, biome);
                        changed++;
                    }
                }
            }
        }

        for (ChunkAccess chunk : touchedChunks) {
            chunk.setUnsaved(true);
        }
        level.getChunkSource().chunkMap.resendBiomesForChunks(List.copyOf(touchedChunks));
        return changed;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag wardTags = new ListTag();
        for (ActiveWard ward : wards) {
            CompoundTag wardTag = new CompoundTag();
            wardTag.putLong("Anchor", ward.anchor.asLong());
            wardTag.putUUID("Owner", ward.owner);
            wardTag.putString("Spell", ward.spellKey);
            wardTag.putString("Name", ward.displayName);
            wardTag.putLongArray("Spaces", ward.spaces.stream().mapToLong(Long::longValue).toArray());
            wardTag.putLongArray("Camouflage", ward.camouflageSpaces.stream().mapToLong(Long::longValue).toArray());
            wardTags.add(wardTag);
        }
        tag.put("Wards", wardTags);
        ListTag linkTags = new ListTag();
        perimeterLinks.forEach((perimeter, anchor) -> {
            CompoundTag linkTag = new CompoundTag();
            linkTag.putLong("Perimeter", perimeter);
            linkTag.putLong("Anchor", anchor);
            linkTags.add(linkTag);
        });
        tag.put("PerimeterLinks", linkTags);
        return tag;
    }

    private static WorldWards load(CompoundTag tag) {
        WorldWards data = new WorldWards();
        ListTag wardTags = tag.getList("Wards", 10);
        for (int i = 0; i < wardTags.size(); i++) {
            CompoundTag wardTag = wardTags.getCompound(i);
            BlockPos anchor = BlockPos.of(wardTag.getLong("Anchor"));
            if (!wardTag.hasUUID("Owner")) {
                continue;
            }
            long[] spaceArray = wardTag.contains("Spaces") ? wardTag.getLongArray("Spaces") : wardTag.getLongArray("Cells");
            Set<Long> spaces = new HashSet<>();
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (long space : spaceArray) {
                BlockPos pos = BlockPos.of(space);
                spaces.add(space);
                minX = Math.min(minX, pos.getX());
                minY = Math.min(minY, pos.getY());
                minZ = Math.min(minZ, pos.getZ());
                maxX = Math.max(maxX, pos.getX());
                maxY = Math.max(maxY, pos.getY());
                maxZ = Math.max(maxZ, pos.getZ());
            }
            if (spaces.isEmpty()) {
                continue;
            }
            AABB bounds = new AABB(minX, minY, minZ, maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D);
            Set<Long> camouflageSpaces = new HashSet<>();
            long[] camouflageArray = wardTag.getLongArray("Camouflage");
            for (long camouflage : camouflageArray) {
                camouflageSpaces.add(camouflage);
            }
            data.wards.add(new ActiveWard(anchor, wardTag.getUUID("Owner"), wardTag.getString("Spell"), wardTag.getString("Name"), Set.copyOf(spaces), bounds, camouflageSpaces));
        }
        ListTag linkTags = tag.getList("PerimeterLinks", 10);
        for (int i = 0; i < linkTags.size(); i++) {
            CompoundTag linkTag = linkTags.getCompound(i);
            data.perimeterLinks.put(linkTag.getLong("Perimeter"), linkTag.getLong("Anchor"));
        }
        return data;
    }

    public record ApplyResult(boolean success, String message) {
    }

    private record ComputedArea(Set<Long> spaces, AABB bounds) {
    }

    private record CamouflageCandidate(BlockState state, int score) {
    }

    private record TargetRule(TargetMode mode, String parameter) {
        private static TargetRule fromShape(String shape) {
            if (shape.startsWith("ward_player_")) {
                return new TargetRule(TargetMode.PLAYER_NAME, shape.substring("ward_player_".length()));
            }
            if (shape.startsWith("ward_mob_")) {
                return new TargetRule(TargetMode.ENTITY_TYPE, shape.substring("ward_mob_".length()));
            }
            if (shape.startsWith("ward_entity_type_")) {
                return new TargetRule(TargetMode.ENTITY_TYPE, shape.substring("ward_entity_type_".length()));
            }
            if (shape.startsWith("ward_entity_")) {
                return new TargetRule(TargetMode.ENTITY_TYPE, shape.substring("ward_entity_".length()));
            }
            TargetMode mode = switch (shape) {
                case "ward_non_allied" -> TargetMode.NON_ALLIED;
                case "ward_hostile" -> TargetMode.HOSTILE;
                case "ward_players" -> TargetMode.PLAYERS;
                case "ward_allies" -> TargetMode.ALLIES;
                case "ward_mobs" -> TargetMode.MOBS;
                case "ward_monsters" -> TargetMode.MONSTERS;
                case "ward_passive" -> TargetMode.PASSIVE;
                case "ward_animals" -> TargetMode.ANIMALS;
                case "ward_any" -> TargetMode.ANY;
                default -> TargetMode.NON_ALLIED;
            };
            return new TargetRule(mode, "");
        }

        private boolean matches(ServerPlayer caster, LivingEntity entity) {
            return switch (mode) {
                case NON_ALLIED -> entity != caster && !MagicAllies.isAlly(caster, entity);
                case HOSTILE -> entity != caster && !MagicAllies.isAlly(caster, entity) && entity.getType().getCategory() == MobCategory.MONSTER;
                case PLAYERS -> entity instanceof ServerPlayer player && player != caster && !MagicAllies.isAlly(caster, player);
                case ALLIES -> MagicAllies.isAlly(caster, entity);
                case MOBS -> entity instanceof Mob && entity != caster && !MagicAllies.isAlly(caster, entity);
                case MONSTERS -> entity != caster && !MagicAllies.isAlly(caster, entity) && entity.getType().getCategory() == MobCategory.MONSTER;
                case PASSIVE -> entity != caster && !MagicAllies.isAlly(caster, entity) && isPassiveCategory(entity.getType().getCategory());
                case ANIMALS -> entity != caster && !MagicAllies.isAlly(caster, entity) && isAnimalCategory(entity.getType().getCategory());
                case PLAYER_NAME -> entity instanceof ServerPlayer player && player.getGameProfile().getName().equalsIgnoreCase(parameter);
                case ENTITY_TYPE -> entityTypeMatches(entity, parameter);
                case ANY -> entity != caster;
            };
        }

        private static boolean isPassiveCategory(MobCategory category) {
            return category != MobCategory.MONSTER;
        }

        private static boolean isAnimalCategory(MobCategory category) {
            return switch (category) {
                case CREATURE, WATER_CREATURE, WATER_AMBIENT, UNDERGROUND_WATER_CREATURE, AXOLOTLS, AMBIENT -> true;
                default -> false;
            };
        }

        private static boolean entityTypeMatches(LivingEntity entity, String parameter) {
            ResourceLocation targetType = ResourceLocation.tryParse(parameter.contains(":") ? parameter : "minecraft:" + parameter);
            return targetType != null && BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).equals(targetType);
        }
    }

    private enum TargetMode {
        NON_ALLIED,
        HOSTILE,
        PLAYERS,
        ALLIES,
        MOBS,
        MONSTERS,
        PASSIVE,
        ANIMALS,
        PLAYER_NAME,
        ENTITY_TYPE,
        ANY
    }

    private static final class ActiveWard {
        private final BlockPos anchor;
        private final UUID owner;
        private final String spellKey;
        private final String displayName;
        private final Set<Long> spaces;
        private final List<Long> spaceList;
        private final AABB bounds;
        private final Set<Long> camouflageSpaces;
        private final Set<UUID> lockdownEntities = new HashSet<>();
        private final Map<UUID, Integer> cooldowns = new HashMap<>();
        private final Map<UUID, Integer> alarmCooldowns = new HashMap<>();
        private int scanCursor;

        private ActiveWard(BlockPos anchor, UUID owner, String spellKey, String displayName, Set<Long> spaces, AABB bounds, Set<Long> camouflageSpaces) {
            this.anchor = anchor;
            this.owner = owner;
            this.spellKey = spellKey;
            this.displayName = displayName;
            this.spaces = spaces;
            this.spaceList = List.copyOf(spaces);
            this.bounds = bounds;
            this.camouflageSpaces = new HashSet<>(camouflageSpaces);
        }

        private boolean contains(BlockPos pos) {
            return spaces.contains(pos.asLong());
        }

        private int cooldown(LivingEntity entity) {
            return cooldowns.getOrDefault(entity.getUUID(), 0);
        }

        private void setCooldown(LivingEntity entity, int ticks) {
            cooldowns.put(entity.getUUID(), ticks);
        }

        private int alarmCooldown(LivingEntity entity) {
            return alarmCooldowns.getOrDefault(entity.getUUID(), 0);
        }

        private void setAlarmCooldown(LivingEntity entity, int ticks) {
            alarmCooldowns.put(entity.getUUID(), ticks);
        }

        private List<BlockPos> nextScanBatch(int limit) {
            if (spaceList.isEmpty()) {
                return List.of();
            }
            List<BlockPos> batch = new ArrayList<>(Math.min(limit, spaceList.size()));
            int count = Math.min(limit, spaceList.size());
            for (int i = 0; i < count; i++) {
                long space = spaceList.get(scanCursor);
                batch.add(BlockPos.of(space));
                scanCursor = (scanCursor + 1) % spaceList.size();
            }
            return batch;
        }

        private void tickCooldowns() {
            cooldowns.entrySet().removeIf(entry -> {
                int next = entry.getValue() - 1;
                if (next <= 0) {
                    return true;
                }
                entry.setValue(next);
                return false;
            });
            alarmCooldowns.entrySet().removeIf(entry -> {
                int next = entry.getValue() - 1;
                if (next <= 0) {
                    return true;
                }
                entry.setValue(next);
                return false;
            });
        }
    }
}
