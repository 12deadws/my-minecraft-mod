package dev.client.mod.modules;

import dev.client.mod.*;
import dev.client.mod.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Boxes + labels for chests, shulkers and spawners, drawn on the HUD so they show through walls.
 * Positions are projected to the screen with plain math (no custom render pipeline).
 */
public class Esp extends Module {
    private static final int CHEST = 0, SHULKER = 1, SPAWNER = 2;
    private static final int[] COLORS = { 0xFFFFA726, 0xFFE040FB, 0xFF40C4FF };
    private static final String[] NAMES = { "Chest", "Shulker", "Spawner" };

    private record Hit(BlockPos pos, int type) {}

    private volatile List<Hit> hits = new ArrayList<>();
    private int timer = 0;

    public Esp() {
        super("ESP", GLFW.GLFW_KEY_O, false);
        settings.add(toggle("Chests", () -> Config.espChests, v -> Config.espChests = v));
        settings.add(toggle("Shulkers", () -> Config.espShulkers, v -> Config.espShulkers = v));
        settings.add(toggle("Spawners", () -> Config.espSpawners, v -> Config.espSpawners = v));
        settings.add(new Setting("Range", () -> Config.espRange + " blocks",
                () -> { Config.espRange = Math.max(16, Config.espRange - 16); Config.save(); },
                () -> { Config.espRange = Math.min(128, Config.espRange + 16); Config.save(); }));
    }

    private static Setting toggle(String name, java.util.function.BooleanSupplier get, java.util.function.Consumer<Boolean> set) {
        Runnable flip = () -> { set.accept(!get.getAsBoolean()); Config.save(); };
        return new Setting(name, () -> get.getAsBoolean() ? "ON" : "OFF", flip, flip);
    }

    @Override public void onDisable() { hits = new ArrayList<>(); }

    @Override public void onTick() {
        if (timer-- > 0) return;
        timer = 10;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        ClientLevel level = mc.level;
        if (p == null || level == null) return;

        List<Hit> found = new ArrayList<>();
        int pcx = p.getBlockX() >> 4, pcz = p.getBlockZ() >> 4;
        int r = Config.espRange / 16 + 1;
        double maxSq = (double) Config.espRange * Config.espRange;

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(pcx + dx, pcz + dz);
                if (chunk == null) continue;
                for (BlockEntity be : new ArrayList<>(chunk.getBlockEntities().values())) {
                    int type;
                    if (be instanceof ChestBlockEntity || be instanceof EnderChestBlockEntity) type = CHEST;
                    else if (be instanceof ShulkerBoxBlockEntity) type = SHULKER;
                    else if (be instanceof SpawnerBlockEntity) type = SPAWNER;
                    else continue;
                    if (type == CHEST && !Config.espChests) continue;
                    if (type == SHULKER && !Config.espShulkers) continue;
                    if (type == SPAWNER && !Config.espSpawners) continue;
                    BlockPos pos = be.getBlockPos();
                    if (p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > maxSq) continue;
                    found.add(new Hit(pos, type));
                }
            }
        }
        hits = found;
    }

    /** Called every frame from the HUD callback. */
    public void render(GuiGraphics g, float pt) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;

        Vec3 eye = p.getEyePosition(pt);
        double yaw = Math.toRadians(p.getViewYRot(pt)), pitch = Math.toRadians(p.getViewXRot(pt));
        double cy = Math.cos(yaw), sy = Math.sin(yaw), cp = Math.cos(pitch), sp = Math.sin(pitch);
        double fx = -sy * cp, fy = -sp, fz = cy * cp;      // forward
        double rx = -cy, rz = -sy;                         // right
        double ux = -rz * fy, uy = rz * fx - rx * fz, uz = rx * fy;   // up = right x forward

        int W = mc.getWindow().getGuiScaledWidth(), H = mc.getWindow().getGuiScaledHeight();
        double fov = Math.toRadians(mc.options.fov().get());
        double scale = (H / 2.0) / Math.tan(fov / 2.0);

        for (Hit h : hits) {
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            boolean visible = true;
            for (int i = 0; i < 8 && visible; i++) {
                double dx = h.pos().getX() + (i & 1) - eye.x;
                double dy = h.pos().getY() + ((i >> 1) & 1) - eye.y;
                double dz = h.pos().getZ() + ((i >> 2) & 1) - eye.z;
                double zc = dx * fx + dy * fy + dz * fz;
                if (zc < 0.1) { visible = false; break; }
                double xc = dx * rx + dz * rz;
                double yc = dx * ux + dy * uy + dz * uz;
                double sx = W / 2.0 + xc / zc * scale;
                double sy2 = H / 2.0 - yc / zc * scale;
                minX = Math.min(minX, sx); maxX = Math.max(maxX, sx);
                minY = Math.min(minY, sy2); maxY = Math.max(maxY, sy2);
            }
            if (!visible || maxX < 0 || minX > W || maxY < 0 || minY > H) continue;

            int x1 = (int) minX, y1 = (int) minY, x2 = (int) maxX, y2 = (int) maxY;
            int color = COLORS[h.type()];
            g.fill(x1, y1, x2 + 1, y1 + 1, color);
            g.fill(x1, y2, x2 + 1, y2 + 1, color);
            g.fill(x1, y1, x1 + 1, y2 + 1, color);
            g.fill(x2, y1, x2 + 1, y2 + 1, color);

            double dist = Math.sqrt(p.distanceToSqr(h.pos().getX() + 0.5, h.pos().getY() + 0.5, h.pos().getZ() + 0.5));
            String label = NAMES[h.type()] + " " + (int) dist + "m";
            g.drawString(mc.font, label, (x1 + x2) / 2 - mc.font.width(label) / 2, y1 - 10, color);
        }
    }
}
