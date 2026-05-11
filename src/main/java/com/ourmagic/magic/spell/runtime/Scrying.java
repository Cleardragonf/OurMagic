package com.ourmagic.magic.spell.runtime;

import com.ourmagic.OurMagic;
import com.ourmagic.network.ModNetwork;
import com.ourmagic.network.ScryCommandPacket;
import com.ourmagic.network.ScryMarksPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = OurMagic.MOD_ID)
public final class Scrying {
    private static final String ROOT_TAG = "OurMagicScry";
    private static final String MARKS_TAG = "Marks";
    private static final String NEXT_TAG = "Next";
    private static final int MAX_MARKS = 12;
    private static final int VISION_TICKS = 20 * 12;
    private static final List<Vision> VISIONS = new ArrayList<>();
    private static final Map<UUID, Float> PENDING_SELECTIONS = new HashMap<>();

    private Scrying() {
    }

    public static void markLocation(ServerPlayer caster, Vec3 position) {
        CompoundTag mark = new CompoundTag();
        mark.putString("Type", "location");
        mark.putString("Name", "Location " + Math.round(position.x) + " " + Math.round(position.y) + " " + Math.round(position.z));
        mark.putString("Dimension", caster.level().dimension().location().toString());
        mark.putDouble("X", position.x);
        mark.putDouble("Y", position.y);
        mark.putDouble("Z", position.z);
        addMark(caster, mark);
        caster.displayClientMessage(net.minecraft.network.chat.Component.literal("Scry mark set."), true);
    }

    public static boolean markPlayer(ServerPlayer caster, Entity target) {
        if (!(target instanceof ServerPlayer player)) {
            return false;
        }

        CompoundTag mark = new CompoundTag();
        mark.putString("Type", "player");
        mark.putString("Name", player.getGameProfile().getName());
        mark.putUUID("Target", player.getUUID());
        addMark(caster, mark);
        caster.displayClientMessage(net.minecraft.network.chat.Component.literal("Scry mark bound to " + player.getGameProfile().getName() + "."), true);
        return true;
    }

    public static boolean openSelection(ServerPlayer caster, float durationMultiplier) {
        ListTag marks = marks(caster);
        if (marks.isEmpty()) {
            caster.displayClientMessage(net.minecraft.network.chat.Component.literal("No scry marks."), true);
            return false;
        }

        PENDING_SELECTIONS.put(caster.getUUID(), durationMultiplier);
        List<ScryMarksPacket.Entry> entries = new ArrayList<>();
        for (int i = 0; i < marks.size(); i++) {
            CompoundTag mark = marks.getCompound(i);
            entries.add(new ScryMarksPacket.Entry(i, mark.getString("Type"), mark.getString("Name")));
        }
        ModNetwork.sendScryMarks(caster, entries);
        return true;
    }

    public static boolean viewSelected(ServerPlayer caster, int index) {
        float durationMultiplier = PENDING_SELECTIONS.getOrDefault(caster.getUUID(), 1.0F);
        PENDING_SELECTIONS.remove(caster.getUUID());
        return viewIndex(caster, index, durationMultiplier);
    }

    public static void handleCommand(ServerPlayer player, ScryCommandPacket.Command command) {
        Vision vision = activeVision(player).orElse(null);
        if (vision == null) {
            ModNetwork.syncScryState(player, false, "", 0, 0, 0);
            return;
        }

        switch (command) {
            case EXIT -> {
                endVision(player, true);
            }
            case RENEW -> {
                vision.ticks = Math.round(VISION_TICKS * vision.durationMultiplier);
                syncVisionState(player, vision);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Scrying renewed."), true);
            }
            case NEXT -> switchVision(player, vision, 1);
            case PREVIOUS -> switchVision(player, vision, -1);
        }
    }

    public static boolean viewNext(ServerPlayer caster, float durationMultiplier) {
        ListTag marks = marks(caster);
        if (marks.isEmpty()) {
            caster.displayClientMessage(net.minecraft.network.chat.Component.literal("No scry marks."), true);
            return false;
        }

        CompoundTag root = root(caster);
        int index = Math.floorMod(root.getInt(NEXT_TAG), marks.size());
        root.putInt(NEXT_TAG, Math.floorMod(index + 1, marks.size()));
        return viewIndex(caster, index, durationMultiplier);
    }

    private static boolean viewIndex(ServerPlayer caster, int index, float durationMultiplier) {
        ListTag marks = marks(caster);
        if (index < 0 || index >= marks.size()) {
            caster.displayClientMessage(net.minecraft.network.chat.Component.literal("That scry mark is gone."), true);
            return false;
        }

        CompoundTag mark = marks.getCompound(index).copy();
        Optional<RemoteView> remote = resolve(caster.serverLevel().getServer(), mark);
        if (remote.isEmpty()) {
            caster.displayClientMessage(net.minecraft.network.chat.Component.literal("That scry mark is unreachable."), true);
            return false;
        }

        endVision(caster, false);
        RemoteView view = remote.get();
        VISIONS.add(new Vision(
                caster.getUUID(),
                mark,
                Math.round(VISION_TICKS * durationMultiplier),
                caster.serverLevel().dimension(),
                caster.position(),
                caster.getYRot(),
                caster.getXRot(),
                caster.gameMode.getGameModeForPlayer(),
                createBodyCopy(caster),
                index,
                durationMultiplier
        ));
        enterVision(caster, view);
        syncVisionState(caster, activeVision(caster).orElseThrow());
        caster.displayClientMessage(net.minecraft.network.chat.Component.literal("Scrying " + mark.getString("Name") + "."), true);
        return true;
    }

    public static void clearMarksOn(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer marked)) {
            return;
        }
        MinecraftServer server = marked.serverLevel().getServer();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ListTag marks = marks(player);
            boolean changed = false;
            for (int i = marks.size() - 1; i >= 0; i--) {
                CompoundTag mark = marks.getCompound(i);
                if ("player".equals(mark.getString("Type")) && mark.hasUUID("Target") && mark.getUUID("Target").equals(marked.getUUID())) {
                    marks.remove(i);
                    changed = true;
                }
            }
            if (changed) {
                root(player).put(MARKS_TAG, marks);
            }
        }
        Iterator<Vision> visionIterator = VISIONS.iterator();
        while (visionIterator.hasNext()) {
            Vision vision = visionIterator.next();
            if (vision.mark.hasUUID("Target") && vision.mark.getUUID("Target").equals(marked.getUUID())) {
                ServerPlayer owner = server.getPlayerList().getPlayer(vision.owner);
                if (owner != null) {
                    restoreVision(owner, vision);
                    owner.displayClientMessage(net.minecraft.network.chat.Component.literal("A scrying link was broken."), true);
                }
                visionIterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || VISIONS.isEmpty()) {
            return;
        }

        Iterator<Vision> iterator = VISIONS.iterator();
        while (iterator.hasNext()) {
            Vision vision = iterator.next();
            if (vision.ticks-- <= 0) {
                ServerPlayer owner = event.getServer().getPlayerList().getPlayer(vision.owner);
                if (owner != null) {
                    restoreVision(owner, vision);
                    ModNetwork.syncScryState(owner, false, "", 0, 0, 0);
                    owner.displayClientMessage(net.minecraft.network.chat.Component.literal("Scrying ended."), true);
                }
                iterator.remove();
                continue;
            }

            ServerPlayer owner = event.getServer().getPlayerList().getPlayer(vision.owner);
            if (owner == null) {
                iterator.remove();
                continue;
            }
            if (isBodyMissing(event.getServer(), vision)) {
                restoreVision(owner, vision);
                ModNetwork.syncScryState(owner, false, "", 0, 0, 0);
                owner.displayClientMessage(net.minecraft.network.chat.Component.literal("Your scrying body was disturbed."), true);
                iterator.remove();
                continue;
            }

            Optional<RemoteView> remote = resolve(event.getServer(), vision.mark);
            if (remote.isEmpty()) {
                restoreVision(owner, vision);
                ModNetwork.syncScryState(owner, false, "", 0, 0, 0);
                iterator.remove();
                continue;
            }

            RemoteView view = remote.get();
            if (owner.level() != view.level || owner.distanceToSqr(view.center) > 64.0D || "player".equals(vision.mark.getString("Type")) && owner.tickCount % 10 == 0) {
                enterVision(owner, view);
            }
            syncVisionState(owner, vision);
        }
    }

    @SubscribeEvent
    public static void livingAttack(LivingAttackEvent event) {
        if (event.getEntity().level().isClientSide || VISIONS.isEmpty()) {
            return;
        }
        Entity attacked = event.getEntity();
        if (!(attacked.level() instanceof ServerLevel bodyLevel)) {
            return;
        }
        MinecraftServer server = bodyLevel.getServer();

        Iterator<Vision> iterator = VISIONS.iterator();
        while (iterator.hasNext()) {
            Vision vision = iterator.next();
            if (vision.bodyCopy == null || !vision.bodyCopy.equals(attacked.getUUID())) {
                continue;
            }

            event.setCanceled(true);
            ServerPlayer owner = server.getPlayerList().getPlayer(vision.owner);
            if (owner != null) {
                restoreVision(owner, vision);
                ModNetwork.syncScryState(owner, false, "", 0, 0, 0);
                owner.displayClientMessage(net.minecraft.network.chat.Component.literal("Your scrying body was struck."), true);
            } else {
                attacked.discard();
            }
            iterator.remove();
            return;
        }
    }

    private static void endVision(ServerPlayer player, boolean notify) {
        Iterator<Vision> iterator = VISIONS.iterator();
        while (iterator.hasNext()) {
            Vision vision = iterator.next();
            if (vision.owner.equals(player.getUUID())) {
                restoreVision(player, vision);
                iterator.remove();
                ModNetwork.syncScryState(player, false, "", 0, 0, 0);
                if (notify) {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("Scrying ended."), true);
                }
                return;
            }
        }
    }

    private static void enterVision(ServerPlayer player, RemoteView view) {
        Vec3 at = view.center.add(0.0D, 1.7D, 0.0D);
        if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
            player.setGameMode(GameType.SPECTATOR);
        }
        player.teleportTo(view.level, at.x, at.y, at.z, view.yaw, view.pitch);
        player.setDeltaMovement(Vec3.ZERO);
        player.serverLevel().sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 28, 0.55D, 0.55D, 0.55D, 0.06D);
    }

    private static void restoreVision(ServerPlayer player, Vision vision) {
        MinecraftServer server = player.serverLevel().getServer();
        removeBodyCopy(vision, server);
        ServerLevel returnLevel = server.getLevel(vision.returnDimension);
        if (returnLevel != null) {
            player.teleportTo(returnLevel, vision.returnPosition.x, vision.returnPosition.y, vision.returnPosition.z, vision.returnYaw, vision.returnPitch);
        }
        player.setGameMode(vision.previousMode);
        player.setDeltaMovement(Vec3.ZERO);
    }

    private static Optional<Vision> activeVision(ServerPlayer player) {
        return VISIONS.stream().filter(vision -> vision.owner.equals(player.getUUID())).findFirst();
    }

    private static void switchVision(ServerPlayer player, Vision vision, int direction) {
        ListTag marks = marks(player);
        if (marks.isEmpty()) {
            endVision(player, true);
            return;
        }
        int nextIndex = Math.floorMod(vision.markIndex + direction, marks.size());
        Optional<RemoteView> remote = resolve(player.serverLevel().getServer(), marks.getCompound(nextIndex));
        if (remote.isEmpty()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("That scry mark is unreachable."), true);
            return;
        }
        vision.mark = marks.getCompound(nextIndex).copy();
        vision.markIndex = nextIndex;
        vision.ticks = Math.round(VISION_TICKS * vision.durationMultiplier);
        enterVision(player, remote.get());
        syncVisionState(player, vision);
    }

    private static void syncVisionState(ServerPlayer player, Vision vision) {
        int count = marks(player).size();
        ModNetwork.syncScryState(player, true, vision.mark.getString("Name"), vision.markIndex, count, Math.max(0, vision.ticks));
    }

    private static UUID createBodyCopy(ServerPlayer player) {
        ArmorStand body = EntityType.ARMOR_STAND.create(player.serverLevel());
        if (body == null) {
            return null;
        }

        body.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        body.setCustomName(net.minecraft.network.chat.Component.literal(player.getGameProfile().getName()));
        body.setCustomNameVisible(true);
        body.setShowArms(true);
        body.setNoGravity(false);
        body.setInvulnerable(false);
        body.setItemSlot(EquipmentSlot.HEAD, playerHead(player));
        body.setItemSlot(EquipmentSlot.CHEST, player.getItemBySlot(EquipmentSlot.CHEST).copy());
        body.setItemSlot(EquipmentSlot.LEGS, player.getItemBySlot(EquipmentSlot.LEGS).copy());
        body.setItemSlot(EquipmentSlot.FEET, player.getItemBySlot(EquipmentSlot.FEET).copy());
        body.setItemSlot(EquipmentSlot.MAINHAND, player.getMainHandItem().copy());
        body.setItemSlot(EquipmentSlot.OFFHAND, player.getOffhandItem().copy());
        player.serverLevel().addFreshEntity(body);
        player.serverLevel().sendParticles(ParticleTypes.ENCHANT, body.getX(), body.getY() + 1.0D, body.getZ(), 24, 0.45D, 0.65D, 0.45D, 0.03D);
        return body.getUUID();
    }

    private static ItemStack playerHead(ServerPlayer player) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        CompoundTag tag = head.getOrCreateTag();
        tag.put("SkullOwner", NbtUtils.writeGameProfile(new CompoundTag(), player.getGameProfile()));
        return head;
    }

    private static boolean isBodyMissing(MinecraftServer server, Vision vision) {
        if (vision.bodyCopy == null) {
            return false;
        }
        ServerLevel returnLevel = server.getLevel(vision.returnDimension);
        return returnLevel == null || returnLevel.getEntity(vision.bodyCopy) == null;
    }

    private static void removeBodyCopy(Vision vision, MinecraftServer server) {
        if (vision.bodyCopy == null) {
            return;
        }
        ServerLevel returnLevel = server.getLevel(vision.returnDimension);
        if (returnLevel == null) {
            return;
        }
        Entity body = returnLevel.getEntity(vision.bodyCopy);
        if (body != null) {
            returnLevel.sendParticles(ParticleTypes.POOF, body.getX(), body.getY() + body.getBbHeight() * 0.5D, body.getZ(), 18, 0.35D, 0.45D, 0.35D, 0.03D);
            body.discard();
        }
    }

    private static void addMark(ServerPlayer player, CompoundTag mark) {
        ListTag marks = marks(player);
        removeDuplicate(marks, mark);
        marks.add(mark);
        while (marks.size() > MAX_MARKS) {
            marks.remove(0);
        }
        root(player).put(MARKS_TAG, marks);
    }

    private static void removeDuplicate(ListTag marks, CompoundTag mark) {
        for (int i = marks.size() - 1; i >= 0; i--) {
            CompoundTag existing = marks.getCompound(i);
            if (sameMark(existing, mark)) {
                marks.remove(i);
            }
        }
    }

    private static boolean sameMark(CompoundTag a, CompoundTag b) {
        if (!a.getString("Type").equals(b.getString("Type"))) {
            return false;
        }
        if ("player".equals(a.getString("Type"))) {
            return a.hasUUID("Target") && b.hasUUID("Target") && a.getUUID("Target").equals(b.getUUID("Target"));
        }
        return a.getString("Dimension").equals(b.getString("Dimension"))
                && BlockPos.containing(a.getDouble("X"), a.getDouble("Y"), a.getDouble("Z")).equals(BlockPos.containing(b.getDouble("X"), b.getDouble("Y"), b.getDouble("Z")));
    }

    private static CompoundTag root(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(ROOT_TAG, 10)) {
            data.put(ROOT_TAG, new CompoundTag());
        }
        return data.getCompound(ROOT_TAG);
    }

    private static ListTag marks(ServerPlayer player) {
        CompoundTag root = root(player);
        if (!root.contains(MARKS_TAG, 9)) {
            root.put(MARKS_TAG, new ListTag());
        }
        return root.getList(MARKS_TAG, 10);
    }

    private static void renderVision(ServerPlayer owner, CompoundTag mark) {
        Optional<RemoteView> remote = resolve(owner.serverLevel().getServer(), mark);
        Vec3 origin = owner.getEyePosition().add(owner.getLookAngle().normalize().scale(3.0D));
        Vec3 forward = owner.getLookAngle().normalize();
        Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
        if (right.lengthSqr() < 0.001D) {
            right = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            right = right.normalize();
        }
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        ServerLevel displayLevel = owner.serverLevel();

        drawWindow(displayLevel, origin, right, up);
        if (remote.isEmpty()) {
            displayLevel.sendParticles(ParticleTypes.SMOKE, origin.x, origin.y, origin.z, 14, 0.7D, 0.45D, 0.02D, 0.01D);
            return;
        }

        RemoteView view = remote.get();
        drawLiveRemoteBlocks(displayLevel, origin, right, up, view);
        drawRemoteEntities(displayLevel, origin, right, up, view);
    }

    private static Optional<RemoteView> resolve(MinecraftServer server, CompoundTag mark) {
        if ("player".equals(mark.getString("Type"))) {
            if (!mark.hasUUID("Target")) {
                return Optional.empty();
            }
            ServerPlayer target = server.getPlayerList().getPlayer(mark.getUUID("Target"));
            if (target == null) {
                return Optional.empty();
            }
            return Optional.of(new RemoteView(target.serverLevel(), target.position(), Optional.of(target), target.getYRot(), target.getXRot()));
        }

        ResourceLocation dimensionId = ResourceLocation.tryParse(mark.getString("Dimension"));
        if (dimensionId == null) {
            return Optional.empty();
        }
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
        if (level == null) {
            return Optional.empty();
        }
        return Optional.of(new RemoteView(level, new Vec3(mark.getDouble("X"), mark.getDouble("Y"), mark.getDouble("Z")), Optional.empty(), 0.0F, 25.0F));
    }

    private static void drawWindow(ServerLevel level, Vec3 origin, Vec3 right, Vec3 up) {
        for (int i = 0; i <= 24; i++) {
            double x = -1.8D + i * 0.15D;
            particle(level, origin.add(right.scale(x)).add(up.scale(-1.05D)), ParticleTypes.REVERSE_PORTAL);
            particle(level, origin.add(right.scale(x)).add(up.scale(1.05D)), ParticleTypes.REVERSE_PORTAL);
        }
        for (int i = 0; i <= 14; i++) {
            double y = -1.05D + i * 0.15D;
            particle(level, origin.add(right.scale(-1.8D)).add(up.scale(y)), ParticleTypes.REVERSE_PORTAL);
            particle(level, origin.add(right.scale(1.8D)).add(up.scale(y)), ParticleTypes.REVERSE_PORTAL);
        }
    }

    private static void drawLiveRemoteBlocks(ServerLevel displayLevel, Vec3 origin, Vec3 right, Vec3 up, RemoteView view) {
        BlockPos center = BlockPos.containing(view.center);
        int emitted = 0;
        for (int y = 5; y >= -4; y--) {
            for (int x = -6; x <= 6; x += 2) {
                for (int z = -6; z <= 6; z += 2) {
                    BlockPos pos = center.offset(x, y, z);
                    if (!view.level.isLoaded(pos)) {
                        continue;
                    }

                    BlockState state = view.level.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }

                    double depth = 1.0D - Math.min(0.55D, Math.abs(z) / 18.0D);
                    double dx = x / 3.6D;
                    double dy = y / 4.8D - z / 18.0D;
                    net.minecraft.core.particles.ParticleOptions particle = state.getFluidState().isEmpty() ? ParticleTypes.ENCHANT : ParticleTypes.BUBBLE;
                    particle(displayLevel, origin.add(right.scale(dx * depth)).add(up.scale(Math.max(-0.95D, Math.min(0.95D, dy)))), particle);
                    emitted++;
                    if (emitted >= 58) {
                        drawLiveCenter(displayLevel, origin);
                        return;
                    }
                }
            }
        }
        drawLiveCenter(displayLevel, origin);
    }

    private static void drawLiveCenter(ServerLevel displayLevel, Vec3 origin) {
        displayLevel.sendParticles(ParticleTypes.GLOW, origin.x, origin.y, origin.z, 2, 0.03D, 0.03D, 0.03D, 0.0D);
    }

    private static void drawRemoteEntities(ServerLevel displayLevel, Vec3 origin, Vec3 right, Vec3 up, RemoteView view) {
        AABB area = new AABB(view.center, view.center).inflate(8.0D, 5.0D, 8.0D);
        for (LivingEntity living : view.level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isAlive)) {
            Vec3 offset = living.position().subtract(view.center);
            Vec3 at = origin.add(right.scale(Math.max(-1.55D, Math.min(1.55D, offset.x / 4.0D))))
                    .add(up.scale(Math.max(-0.9D, Math.min(0.9D, 0.15D + offset.z / 6.0D))));
            displayLevel.sendParticles(view.focus.filter(living::equals).isPresent() ? ParticleTypes.GLOW : ParticleTypes.WITCH, at.x, at.y, at.z, 3, 0.03D, 0.03D, 0.03D, 0.0D);
        }
    }

    private static void particle(ServerLevel level, Vec3 at, net.minecraft.core.particles.ParticleOptions particle) {
        level.sendParticles(particle, at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static final class Vision {
        private final UUID owner;
        private CompoundTag mark;
        private final ResourceKey<Level> returnDimension;
        private final Vec3 returnPosition;
        private final float returnYaw;
        private final float returnPitch;
        private final GameType previousMode;
        private final UUID bodyCopy;
        private final float durationMultiplier;
        private int markIndex;
        private int ticks;

        private Vision(UUID owner, CompoundTag mark, int ticks, ResourceKey<Level> returnDimension, Vec3 returnPosition, float returnYaw, float returnPitch, GameType previousMode, UUID bodyCopy, int markIndex, float durationMultiplier) {
            this.owner = owner;
            this.mark = mark;
            this.ticks = ticks;
            this.returnDimension = returnDimension;
            this.returnPosition = returnPosition;
            this.returnYaw = returnYaw;
            this.returnPitch = returnPitch;
            this.previousMode = previousMode;
            this.bodyCopy = bodyCopy;
            this.markIndex = markIndex;
            this.durationMultiplier = durationMultiplier;
        }
    }

    private record RemoteView(ServerLevel level, Vec3 center, Optional<ServerPlayer> focus, float yaw, float pitch) {
    }
}
