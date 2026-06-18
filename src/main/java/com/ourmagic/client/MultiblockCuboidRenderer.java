package com.ourmagic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class MultiblockCuboidRenderer {
    private static final int MAX_BLOCKS = 128;
    private static final Comparator<BlockPos> MASTER_ORDER = Comparator
            .comparingInt((BlockPos pos) -> pos.getY())
            .thenComparingInt(pos -> pos.getX())
            .thenComparingInt(pos -> pos.getZ());

    private MultiblockCuboidRenderer() {
    }

    static void render(BlockEntity blockEntity, Block block, ResourceLocation texture, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay,
                       float red, float green, float blue) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        Cluster cluster = cluster(level, blockEntity.getBlockPos(), block);
        if (cluster == null || !blockEntity.getBlockPos().equals(cluster.master())) {
            return;
        }

        BlockPos origin = blockEntity.getBlockPos();
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(texture));
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        Map<FaceLayer, Set<Cell>> layers = new HashMap<>();
        for (long blockKey : cluster.blocks()) {
            BlockPos blockPos = BlockPos.of(blockKey);
            for (Direction direction : Direction.values()) {
                if (!cluster.contains(blockPos.relative(direction))) {
                    addFace(layers, origin, blockPos, direction);
                }
            }
        }
        for (Map.Entry<FaceLayer, Set<Cell>> entry : layers.entrySet()) {
            renderLayer(consumer, matrix, normal, packedLight, packedOverlay, red, green, blue, entry.getKey(), entry.getValue());
        }
    }

    private static Cluster cluster(Level level, BlockPos start, Block block) {
        if (!level.getBlockState(start).is(block)) {
            return null;
        }

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        BlockPos master = start.immutable();
        queue.add(start.immutable());

        while (!queue.isEmpty() && visited.size() < MAX_BLOCKS) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos.asLong()) || !level.getBlockState(pos).is(block)) {
                continue;
            }
            if (MASTER_ORDER.compare(pos, master) < 0) {
                master = pos.immutable();
            }

            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (!visited.contains(neighbor.asLong()) && level.getBlockState(neighbor).is(block)) {
                    queue.add(neighbor.immutable());
                }
            }
        }

        return new Cluster(master, Set.copyOf(visited));
    }

    private static void addFace(Map<FaceLayer, Set<Cell>> layers, BlockPos origin, BlockPos pos, Direction direction) {
        int x = pos.getX() - origin.getX();
        int y = pos.getY() - origin.getY();
        int z = pos.getZ() - origin.getZ();
        FaceLayer layer;
        Cell cell;
        switch (direction) {
            case SOUTH -> {
                layer = new FaceLayer(direction, z + 1);
                cell = new Cell(x, y);
            }
            case NORTH -> {
                layer = new FaceLayer(direction, z);
                cell = new Cell(x, y);
            }
            case EAST -> {
                layer = new FaceLayer(direction, x + 1);
                cell = new Cell(z, y);
            }
            case WEST -> {
                layer = new FaceLayer(direction, x);
                cell = new Cell(z, y);
            }
            case UP -> {
                layer = new FaceLayer(direction, y + 1);
                cell = new Cell(x, z);
            }
            case DOWN -> {
                layer = new FaceLayer(direction, y);
                cell = new Cell(x, z);
            }
            default -> throw new IllegalStateException("Unhandled direction " + direction);
        }
        layers.computeIfAbsent(layer, ignored -> new HashSet<>()).add(cell);
    }

    private static void renderLayer(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int packedLight, int packedOverlay, float red, float green, float blue,
                                    FaceLayer layer, Set<Cell> cells) {
        int minA = cells.stream().mapToInt(Cell::a).min().orElse(0);
        int minB = cells.stream().mapToInt(Cell::b).min().orElse(0);
        int maxA = cells.stream().mapToInt(Cell::a).max().orElse(0) + 1;
        int maxB = cells.stream().mapToInt(Cell::b).max().orElse(0) + 1;
        Set<Cell> remaining = new HashSet<>(cells);
        List<Cell> ordered = new ArrayList<>(cells);
        ordered.sort(Comparator.comparingInt(Cell::b).thenComparingInt(Cell::a));
        for (Cell start : ordered) {
            if (!remaining.contains(start)) {
                continue;
            }
            int width = 1;
            while (remaining.contains(new Cell(start.a() + width, start.b()))) {
                width++;
            }
            int height = 1;
            boolean canGrow = true;
            while (canGrow) {
                for (int dx = 0; dx < width; dx++) {
                    if (!remaining.contains(new Cell(start.a() + dx, start.b() + height))) {
                        canGrow = false;
                        break;
                    }
                }
                if (canGrow) {
                    height++;
                }
            }
            for (int dx = 0; dx < width; dx++) {
                for (int dy = 0; dy < height; dy++) {
                    remaining.remove(new Cell(start.a() + dx, start.b() + dy));
                }
            }
            mergedFace(consumer, matrix, normal, packedLight, packedOverlay, red, green, blue, layer,
                    start.a(), start.b(), start.a() + width, start.b() + height, minA, minB, maxA, maxB);
        }
    }

    private static void mergedFace(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int packedLight, int packedOverlay, float red, float green, float blue,
                                   FaceLayer layer, float minA, float minB, float maxA, float maxB, float layerMinA, float layerMinB, float layerMaxA, float layerMaxB) {
        float plane = layer.plane();
        float u1 = (minA - layerMinA) / Math.max(1.0F, layerMaxA - layerMinA);
        float u2 = (maxA - layerMinA) / Math.max(1.0F, layerMaxA - layerMinA);
        float v1 = 1.0F - (maxB - layerMinB) / Math.max(1.0F, layerMaxB - layerMinB);
        float v2 = 1.0F - (minB - layerMinB) / Math.max(1.0F, layerMaxB - layerMinB);
        switch (layer.direction()) {
            case SOUTH -> quad(consumer, matrix, normal, packedLight, packedOverlay, red, green, blue, minA, minB, plane, maxA, minB, plane, maxA, maxB, plane, minA, maxB, plane, u1, v2, u2, v1, 0, 0, 1);
            case NORTH -> quad(consumer, matrix, normal, packedLight, packedOverlay, red, green, blue, maxA, minB, plane, minA, minB, plane, minA, maxB, plane, maxA, maxB, plane, u1, v2, u2, v1, 0, 0, -1);
            case EAST -> quad(consumer, matrix, normal, packedLight, packedOverlay, red, green, blue, plane, minB, maxA, plane, minB, minA, plane, maxB, minA, plane, maxB, maxA, u1, v2, u2, v1, 1, 0, 0);
            case WEST -> quad(consumer, matrix, normal, packedLight, packedOverlay, red, green, blue, plane, minB, minA, plane, minB, maxA, plane, maxB, maxA, plane, maxB, minA, u1, v2, u2, v1, -1, 0, 0);
            case UP -> quad(consumer, matrix, normal, packedLight, packedOverlay, red, green, blue, minA, plane, maxB, maxA, plane, maxB, maxA, plane, minB, minA, plane, minB, u1, v2, u2, v1, 0, 1, 0);
            case DOWN -> quad(consumer, matrix, normal, packedLight, packedOverlay, red, green, blue, minA, plane, minB, maxA, plane, minB, maxA, plane, maxB, minA, plane, maxB, u1, v2, u2, v1, 0, -1, 0);
        }
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int packedLight, int packedOverlay, float red, float green, float blue,
                             float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4,
                             float u1, float v1, float u2, float v2, float normalX, float normalY, float normalZ) {
        vertex(consumer, matrix, normal, packedLight, packedOverlay, red, green, blue, x1, y1, z1, u1, v1, normalX, normalY, normalZ);
        vertex(consumer, matrix, normal, packedLight, packedOverlay, red, green, blue, x2, y2, z2, u2, v1, normalX, normalY, normalZ);
        vertex(consumer, matrix, normal, packedLight, packedOverlay, red, green, blue, x3, y3, z3, u2, v2, normalX, normalY, normalZ);
        vertex(consumer, matrix, normal, packedLight, packedOverlay, red, green, blue, x4, y4, z4, u1, v2, normalX, normalY, normalZ);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, int packedLight, int packedOverlay, float red, float green, float blue,
                               float x, float y, float z, float u, float v, float normalX, float normalY, float normalZ) {
        consumer.vertex(matrix, x, y, z)
                .color(red, green, blue, 1.0F)
                .uv(u, v)
                .overlayCoords(packedOverlay)
                .uv2(packedLight)
                .normal(normal, normalX, normalY, normalZ)
                .endVertex();
    }

    private record Cluster(BlockPos master, Set<Long> blocks) {
        private boolean contains(BlockPos pos) {
            return blocks.contains(pos.asLong());
        }
    }

    private record FaceLayer(Direction direction, int plane) {
    }

    private record Cell(int a, int b) {
    }
}
