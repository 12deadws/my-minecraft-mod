package com.caelestis.client.modules;

import com.caelestis.client.module.Category;
import com.caelestis.client.module.Module;
import com.caelestis.client.setting.BoolSetting;
import com.caelestis.client.setting.NumberSetting;
import com.caelestis.client.util.Projector;
import net.minecraft.class_1297;
import net.minecraft.class_1304;
import net.minecraft.class_1309;
import net.minecraft.class_1531;
import net.minecraft.class_1569;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_243;
import net.minecraft.class_332;
import net.minecraft.class_3532;

/**
 * Enhanced name tags: name, health (absolute, percent, bar), distance and, for players, armour icons with
 * durability percentages. Only shows what the client legitimately knows about the entity (synced health and
 * equipment). Drawn as a 2D overlay; the vanilla name tag is still rendered by the game underneath.
 */
public class NameTags extends Module {
    private static final class_1304[] ARMOR = {class_1304.field_6169, class_1304.field_6174, class_1304.field_6172, class_1304.field_6166};

    private final NumberSetting distance = add(new NumberSetting("Render Distance", "Entities further than this get no tag.", 48, 8, 128, 4, " m"));
    private final NumberSetting scale = add(new NumberSetting("Text Scale", "Size of the tag.", 1.0, 0.5, 2.5, 0.1, "x"));
    private final BoolSetting healthBar = add(new BoolSetting("Health Bars", "Draw a health bar under the name.", true));
    private final BoolSetting background = add(new BoolSetting("Background", "Draw an opaque ivory background behind the tag.", true));
    private final BoolSetting armor = add(new BoolSetting("Armor", "Show armour icons and durability (players).", true));
    private final BoolSetting players = add(new BoolSetting("Players", "Tag players.", true));
    private final BoolSetting hostiles = add(new BoolSetting("Hostile Mobs", "Tag hostile mobs.", false));
    private final BoolSetting passives = add(new BoolSetting("Passive Mobs", "Tag animals and other friendly mobs.", false));

    public NameTags() {
        super("NameTags", "Name, health, distance, armour and durability above nearby players and mobs.", Category.RENDER, 210);
    }

    @Override
    public void onRender(class_332 ctx, float tickDelta) {
        if (mc.field_1724 == null || mc.field_1687 == null || mc.field_1690.field_1842) return;
        if (!Projector.begin(tickDelta)) return;

        double maxDist = distance.get();
        for (class_1297 e : mc.field_1687.method_18112()) {
            if (!(e instanceof class_1309 le) || e == mc.field_1724 || e instanceof class_1531 || !le.method_5805()) continue;
            boolean isPlayer = e instanceof class_1657;
            if (isPlayer ? !players.get() : (e instanceof class_1569 ? !hostiles.get() : !passives.get())) continue;
            if (e.method_5767() && !isPlayer) continue;

            double dist = mc.field_1724.method_5739(e);
            if (dist > maxDist) continue;

            class_243 pos = le.method_30950(tickDelta).method_1031(0, le.method_17682() + 0.45, 0);
            double[] s = Projector.project(pos);
            if (s == null) continue;
            drawTag(ctx, le, isPlayer, dist, (float) s[0], (float) s[1]);
        }
    }

    private void drawTag(class_332 ctx, class_1309 e, boolean isPlayer, double dist, float sx, float sy) {
        var tr = mc.field_1772;
        float hp = e.method_6032() + e.method_6067();
        float max = Math.max(1f, e.method_6063());
        int pct = Math.round(Math.min(1f, e.method_6032() / max) * 100f);

        String name = e.method_5477().getString();
        String info = String.format("%.1f/%.0f  %d%%  %.1fm", e.method_6032(), max, pct, dist);
        boolean showArmor = armor.get() && isPlayer;

        int nameW = tr.method_1727(name);
        int infoW = tr.method_1727(info);
        int armorW = showArmor ? 4 * 24 : 0;
        int w = Math.max(Math.max(nameW, infoW), armorW) + 8;

        int armorH = showArmor ? 28 : 0;
        int h = armorH + 11 + 11 + (healthBar.get() ? 5 : 0) + 3;

        float sc = scale.getFloat();
        var m = ctx.method_51448();
        m.pushMatrix();
        m.translate(sx, sy);
        m.scale(sc, sc);
        m.translate(-w / 2f, -h);

        if (background.get()) ctx.method_25294(0, 0, w, h, 0xEEFFF9E8);
        // Draw a one-pixel border using supported DrawContext.fill calls.
        ctx.method_25294(0, 0, w, 1, 0xFFD4AF37);
        ctx.method_25294(0, h - 1, w, h, 0xFFD4AF37);
        ctx.method_25294(0, 1, 1, h - 1, 0xFFD4AF37);
        ctx.method_25294(w - 1, 1, w, h - 1, 0xFFD4AF37);

        int y = 2;
        if (showArmor) {
            int x = (w - armorW) / 2 + 2;
            for (class_1304 slot : ARMOR) {
                class_1799 st = e.method_6118(slot);
                if (!st.method_7960()) {
                    ctx.method_51427(st, x + 2, y);
                    if (st.method_7963()) {
                        int max2 = st.method_7936();
                        int pctDur = Math.round((max2 - st.method_7919()) * 100f / max2);
                        String t = pctDur + "%";
                        ctx.method_51433(tr, t, x + 10 - tr.method_1727(t) / 2, y + 17, durabilityColor(pctDur), true);
                    }
                }
                x += 24;
            }
            y += armorH;
        }

        ctx.method_51433(tr, name, (w - nameW) / 2, y, isPlayer ? 0xFF8B6914 : 0xFF866A2D, true);
        y += 11;
        ctx.method_51433(tr, info, (w - infoW) / 2, y, 0xFF765A28, true);
        y += 11;

        if (healthBar.get()) {
            int bw = w - 6;
            ctx.method_25294(3, y, 3 + bw, y + 3, 0xFFE2D5B7);
            int fillW = Math.round(bw * class_3532.method_15363(e.method_6032() / max, 0f, 1f));
            ctx.method_25294(3, y, 3 + fillW, y + 3, 0xFFD4AF37);
            float abs = e.method_6067();
            if (abs > 0) ctx.method_25294(3, y + 3, 3 + Math.round(bw * class_3532.method_15363(abs / max, 0f, 1f)), y + 4, 0xFFFFE6A0);
        }
        m.popMatrix();
    }

    /** Green: good, yellow: moderate, orange: low, red: almost broken. */
    private static int durabilityColor(int pct) {
        if (pct >= 70) return 0xFFB88922;
        if (pct >= 40) return 0xFFD4AF37;
        if (pct >= 15) return 0xFF987538;
        return 0xFF6E5735;
    }

    private static int healthColor(float frac) {
        frac = class_3532.method_15363(frac, 0f, 1f);
        int r = (int) (255 * Math.min(1f, (1f - frac) * 2f));
        int g = (int) (255 * Math.min(1f, frac * 2f));
        return 0xFF000000 | (r << 16) | (g << 8) | 0x40;
    }
}
