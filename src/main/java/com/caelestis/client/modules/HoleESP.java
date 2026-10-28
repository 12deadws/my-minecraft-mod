package com.caelestis.client.modules;

import com.caelestis.client.module.Category;
import com.caelestis.client.module.Module;
import com.caelestis.client.setting.BoolSetting;
import com.caelestis.client.setting.EnumSetting;
import com.caelestis.client.setting.NumberSetting;
import com.caelestis.client.util.BlockUtil;
import com.caelestis.client.util.Projector;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Highlights one-block (and optionally two-block) holes whose walls and floor are obsidian / bedrock.
 * Scanning runs a few times per second; drawing is a cheap 2D overlay (see Projector).
 */
public class HoleESP extends Module {
    private static final String[] COLORS = {"Green", "Purple", "Cyan", "Red", "Orange", "Yellow", "Blue", "Pink", "White"};
    private static final int[] RGB = {0x55FF55, 0xB45CFF, 0x55FFFF, 0xFF5555, 0xFFAA33, 0xFFFF55, 0x5577FF, 0xFF77CC, 0xFFFFFF};

    private final NumberSetting detectRange = add(new NumberSetting("Detection Range", "How far around you holes are searched.", 8, 3, 12, 1, " m"));
    private final NumberSetting renderRange = add(new NumberSetting("Render Distance", "Holes further than this are not drawn.", 12, 3, 16, 1, " m"));
    private final BoolSetting doubles = add(new BoolSetting("Double Holes", "Also highlight two-block-wide holes.", true));
    private final BoolSetting showDistance = add(new BoolSetting("Show Distance", "Print the distance under each hole.", true));
    private final EnumSetting bedrockColor = add(new EnumSetting("Bedrock Color", "Colour for holes fully protected by bedrock.", "Green", COLORS));
    private final EnumSetting obsidianColor = add(new EnumSetting("Obsidian Color", "Colour for holes with any obsidian wall.", "Purple", COLORS));
    private final NumberSetting outlineAlpha = add(new NumberSetting("Outline Opacity", "Opacity of the outline.", 90, 10, 100, 5, "%"));
    private final NumberSetting fillAlpha = add(new NumberSetting("Fill Opacity", "Opacity of the filled area.", 25, 0, 100, 5, "%"));

    private static final class Hole {
        final BlockPos a, b; // b == null for single holes
        final boolean bedrockOnly;
        double dist;

        Hole(BlockPos a, BlockPos b, boolean bedrockOnly) {
            this.a = a;
            this.b = b;
            this.bedrockOnly = bedrockOnly;
        }
    }

    private volatile List<Hole> holes = new ArrayList<>();
    private int scanTimer = 0;

    public HoleESP() {
        super("Hole ESP", "Outlines safe obsidian / bedrock holes near you.", Category.RENDER, 200);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;
        if (scanTimer-- > 0) return;
        scanTimer = 8; // ~2.5 scans per second
        scan();
    }

    private void scan() {
        List<Hole> found = new ArrayList<>();
        BlockPos origin = mc.player.getBlockPos();
        int r = detectRange.getInt();
        Vec3d me = new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                for (int dy = -3; dy <= 3; dy++) {
                    BlockPos pos = origin.add(dx, dy, dz);
                    if (!open(pos)) continue;

                    // single hole
                    int single = classifySingle(pos);
                    if (single != 0) {
                        found.add(new Hole(pos, null, single == 2));
                        continue;
                    }
                    if (!doubles.get()) continue;
                    // double holes (only look east and south so each pair is found once)
                    for (Direction d : new Direction[]{Direction.EAST, Direction.SOUTH}) {
                        BlockPos other = pos.offset(d);
                        if (!open(other)) continue;
                        int dbl = classifyDouble(pos, other, d);
                        if (dbl != 0) found.add(new Hole(pos, other, dbl == 2));
                    }
                }
            }
        }
        for (Hole h : found) {
            h.dist = me.distanceTo(Vec3d.ofCenter(h.a));
        }
        found.sort(Comparator.comparingDouble(h -> h.dist));
        holes = found;
    }

    /** Two air blocks of standing room. */
    private boolean open(BlockPos pos) {
        return mc.world.getBlockState(pos).isAir() && mc.world.getBlockState(pos.up()).isAir();
    }

    /** 0 = not a hole, 1 = obsidian hole, 2 = full bedrock hole. */
    private int classifySingle(BlockPos pos) {
        boolean allBedrock = true;
        BlockState floor = mc.world.getBlockState(pos.down());
        if (!BlockUtil.isObsidianOrBedrock(floor)) return 0;
        if (!floor.isOf(Blocks.BEDROCK)) allBedrock = false;
        for (Direction d : Direction.Type.HORIZONTAL) {
            BlockState s = mc.world.getBlockState(pos.offset(d));
            if (!BlockUtil.isObsidianOrBedrock(s)) return 0;
            if (!s.isOf(Blocks.BEDROCK)) allBedrock = false;
        }
        return allBedrock ? 2 : 1;
    }

    private int classifyDouble(BlockPos a, BlockPos b, Direction along) {
        boolean allBedrock = true;
        for (BlockPos cell : new BlockPos[]{a, b}) {
            BlockState floor = mc.world.getBlockState(cell.down());
            if (!BlockUtil.isObsidianOrBedrock(floor)) return 0;
            if (!floor.isOf(Blocks.BEDROCK)) allBedrock = false;
            for (Direction d : Direction.Type.HORIZONTAL) {
                BlockPos n = cell.offset(d);
                if (n.equals(a) || n.equals(b)) continue; // the shared opening between the two cells
                BlockState s = mc.world.getBlockState(n);
                if (!BlockUtil.isObsidianOrBedrock(s)) return 0;
                if (!s.isOf(Blocks.BEDROCK)) allBedrock = false;
            }
        }
        return allBedrock ? 2 : 1;
    }

    @Override
    public void onRender(DrawContext ctx, float tickDelta) {
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;
        if (!Projector.begin(tickDelta)) return;

        int drawn = 0;
        double maxDist = renderRange.get();
        for (Hole h : holes) {
            if (h.dist > maxDist) break; // list is sorted by distance
            if (drawn >= 40) break;

            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            boolean ok = true;
            for (BlockPos c : h.b == null ? new BlockPos[]{h.a} : new BlockPos[]{h.a, h.b}) {
                for (int i = 0; i < 4 && ok; i++) {
                    double[] s = Projector.project(new Vec3d(c.getX() + (i & 1), c.getY() + 0.02, c.getZ() + (i >> 1)));
                    if (s == null) {
                        ok = false;
                        break;
                    }
                    minX = Math.min(minX, s[0]);
                    maxX = Math.max(maxX, s[0]);
                    minY = Math.min(minY, s[1]);
                    maxY = Math.max(maxY, s[1]);
                }
            }
            if (!ok || maxX - minX > 600) continue;

            int rgb = RGB[(h.bedrockOnly ? bedrockColor : obsidianColor).index()];
            int outline = ((int) (outlineAlpha.get() * 2.55) << 24) | rgb;
            int fill = ((int) (fillAlpha.get() * 2.55) << 24) | rgb;
            int x1 = (int) minX, y1 = (int) minY, x2 = (int) maxX, y2 = (int) maxY;
            if (x2 - x1 < 2) x2 = x1 + 2;
            if (y2 - y1 < 2) y2 = y1 + 2;

            if (fillAlpha.get() > 0) ctx.fill(x1, y1, x2, y2, fill);
            // Outline with four rectangles (DrawContext.drawBorder is unavailable in 1.21.11).
            ctx.fill(x1, y1, x2, y1 + 1, outline);
            ctx.fill(x1, y2 - 1, x2, y2, outline);
            ctx.fill(x1, y1 + 1, x1 + 1, y2 - 1, outline);
            ctx.fill(x2 - 1, y1 + 1, x2, y2 - 1, outline);
            if (showDistance.get()) {
                String s = String.format("%.1fm", h.dist);
                ctx.drawText(mc.textRenderer, s, (x1 + x2) / 2 - mc.textRenderer.getWidth(s) / 2, y2 + 2, 0xFFFFFFFF, true);
            }
            drawn++;
        }
    }

    @Override
    public void onWorldChange() {
        holes = new ArrayList<>();
        scanTimer = 0;
    }

    @Override
    public void onDisable() {
        holes = new ArrayList<>();
    }
}
