package com.caelestis.client.modules;

import com.caelestis.client.module.Category;
import com.caelestis.client.module.Module;
import com.caelestis.client.setting.BoolSetting;
import com.caelestis.client.setting.EnumSetting;
import com.caelestis.client.setting.NumberSetting;
import com.caelestis.client.util.BlockUtil;
import com.caelestis.client.util.Projector;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_332;

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
        final class_2338 a, b; // b == null for single holes
        final boolean bedrockOnly;
        double dist;

        Hole(class_2338 a, class_2338 b, boolean bedrockOnly) {
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
        if (mc.field_1724 == null || mc.field_1687 == null) return;
        if (scanTimer-- > 0) return;
        scanTimer = 8; // ~2.5 scans per second
        scan();
    }

    private void scan() {
        List<Hole> found = new ArrayList<>();
        class_2338 origin = mc.field_1724.method_24515();
        int r = detectRange.getInt();
        class_243 me = new class_243(mc.field_1724.method_23317(), mc.field_1724.method_23318(), mc.field_1724.method_23321());

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                for (int dy = -3; dy <= 3; dy++) {
                    class_2338 pos = origin.method_10069(dx, dy, dz);
                    if (!open(pos)) continue;

                    // single hole
                    int single = classifySingle(pos);
                    if (single != 0) {
                        found.add(new Hole(pos, null, single == 2));
                        continue;
                    }
                    if (!doubles.get()) continue;
                    // double holes (only look east and south so each pair is found once)
                    for (class_2350 d : new class_2350[]{class_2350.field_11034, class_2350.field_11035}) {
                        class_2338 other = pos.method_10093(d);
                        if (!open(other)) continue;
                        int dbl = classifyDouble(pos, other, d);
                        if (dbl != 0) found.add(new Hole(pos, other, dbl == 2));
                    }
                }
            }
        }
        for (Hole h : found) {
            h.dist = me.method_1022(class_243.method_24953(h.a));
        }
        found.sort(Comparator.comparingDouble(h -> h.dist));
        holes = found;
    }

    /** Two air blocks of standing room. */
    private boolean open(class_2338 pos) {
        return mc.field_1687.method_8320(pos).method_26215() && mc.field_1687.method_8320(pos.method_10084()).method_26215();
    }

    /** 0 = not a hole, 1 = obsidian hole, 2 = full bedrock hole. */
    private int classifySingle(class_2338 pos) {
        boolean allBedrock = true;
        class_2680 floor = mc.field_1687.method_8320(pos.method_10074());
        if (!BlockUtil.isObsidianOrBedrock(floor)) return 0;
        if (!floor.method_27852(class_2246.field_9987)) allBedrock = false;
        for (class_2350 d : class_2350.class_2353.field_11062) {
            class_2680 s = mc.field_1687.method_8320(pos.method_10093(d));
            if (!BlockUtil.isObsidianOrBedrock(s)) return 0;
            if (!s.method_27852(class_2246.field_9987)) allBedrock = false;
        }
        return allBedrock ? 2 : 1;
    }

    private int classifyDouble(class_2338 a, class_2338 b, class_2350 along) {
        boolean allBedrock = true;
        for (class_2338 cell : new class_2338[]{a, b}) {
            class_2680 floor = mc.field_1687.method_8320(cell.method_10074());
            if (!BlockUtil.isObsidianOrBedrock(floor)) return 0;
            if (!floor.method_27852(class_2246.field_9987)) allBedrock = false;
            for (class_2350 d : class_2350.class_2353.field_11062) {
                class_2338 n = cell.method_10093(d);
                if (n.equals(a) || n.equals(b)) continue; // the shared opening between the two cells
                class_2680 s = mc.field_1687.method_8320(n);
                if (!BlockUtil.isObsidianOrBedrock(s)) return 0;
                if (!s.method_27852(class_2246.field_9987)) allBedrock = false;
            }
        }
        return allBedrock ? 2 : 1;
    }

    @Override
    public void onRender(class_332 ctx, float tickDelta) {
        if (mc.field_1724 == null || mc.field_1687 == null || mc.field_1690.field_1842) return;
        if (!Projector.begin(tickDelta)) return;

        int drawn = 0;
        double maxDist = renderRange.get();
        for (Hole h : holes) {
            if (h.dist > maxDist) break; // list is sorted by distance
            if (drawn >= 40) break;

            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            boolean ok = true;
            for (class_2338 c : h.b == null ? new class_2338[]{h.a} : new class_2338[]{h.a, h.b}) {
                for (int i = 0; i < 4 && ok; i++) {
                    double[] s = Projector.project(new class_243(c.method_10263() + (i & 1), c.method_10264() + 0.02, c.method_10260() + (i >> 1)));
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

            if (fillAlpha.get() > 0) ctx.method_25294(x1, y1, x2, y2, fill);
            // Outline with four rectangles (DrawContext.drawBorder is unavailable in 1.21.11).
            ctx.method_25294(x1, y1, x2, y1 + 1, outline);
            ctx.method_25294(x1, y2 - 1, x2, y2, outline);
            ctx.method_25294(x1, y1 + 1, x1 + 1, y2 - 1, outline);
            ctx.method_25294(x2 - 1, y1 + 1, x2, y2 - 1, outline);
            if (showDistance.get()) {
                String s = String.format("%.1fm", h.dist);
                ctx.method_51433(mc.field_1772, s, (x1 + x2) / 2 - mc.field_1772.method_1727(s) / 2, y2 + 2, 0xFFFFFFFF, true);
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
