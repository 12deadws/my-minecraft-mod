package com.caelestis.client.modules;

import com.caelestis.client.module.Category;
import com.caelestis.client.module.Module;
import com.caelestis.client.setting.BoolSetting;
import com.caelestis.client.setting.NumberSetting;
import com.caelestis.client.util.Projector;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Enhanced name tags: name, health (absolute, percent, bar), distance and, for players, armour icons with
 * durability percentages. Only shows what the client legitimately knows about the entity (synced health and
 * equipment). Drawn as a 2D overlay; the vanilla name tag is still rendered by the game underneath.
 */
public class NameTags extends Module {
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final NumberSetting distance = add(new NumberSetting("Render Distance", "Entities further than this get no tag.", 48, 8, 128, 4, " m"));
    private final NumberSetting scale = add(new NumberSetting("Text Scale", "Size of the tag.", 1.0, 0.5, 2.5, 0.1, "x"));
    private final BoolSetting healthBar = add(new BoolSetting("Health Bars", "Draw a health bar under the name.", true));
    private final BoolSetting background = add(new BoolSetting("Background", "Draw a dark background behind the tag.", true));
    private final BoolSetting armor = add(new BoolSetting("Armor", "Show armour icons and durability (players).", true));
    private final BoolSetting players = add(new BoolSetting("Players", "Tag players.", true));
    private final BoolSetting hostiles = add(new BoolSetting("Hostile Mobs", "Tag hostile mobs.", false));
    private final BoolSetting passives = add(new BoolSetting("Passive Mobs", "Tag animals and other friendly mobs.", false));

    public NameTags() {
        super("NameTags", "Name, health, distance, armour and durability above nearby players and mobs.", Category.RENDER, 210);
    }

    @Override
    public void onRender(DrawContext ctx, float tickDelta) {
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;
        if (!Projector.begin(tickDelta)) return;

        double maxDist = distance.get();
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le) || e == mc.player || e instanceof ArmorStandEntity || !le.isAlive()) continue;
            boolean isPlayer = e instanceof PlayerEntity;
            if (isPlayer ? !players.get() : (e instanceof Monster ? !hostiles.get() : !passives.get())) continue;
            if (e.isInvisible() && !isPlayer) continue;

            double dist = mc.player.distanceTo(e);
            if (dist > maxDist) continue;

            Vec3d pos = le.getLerpedPos(tickDelta).add(0, le.getHeight() + 0.45, 0);
            double[] s = Projector.project(pos);
            if (s == null) continue;
            drawTag(ctx, le, isPlayer, dist, (float) s[0], (float) s[1]);
        }
    }

    private void drawTag(DrawContext ctx, LivingEntity e, boolean isPlayer, double dist, float sx, float sy) {
        var tr = mc.textRenderer;
        float hp = e.getHealth() + e.getAbsorptionAmount();
        float max = Math.max(1f, e.getMaxHealth());
        int pct = Math.round(Math.min(1f, e.getHealth() / max) * 100f);

        String name = e.getName().getString();
        String info = String.format("%.1f/%.0f  %d%%  %.1fm", e.getHealth(), max, pct, dist);
        boolean showArmor = armor.get() && isPlayer;

        int nameW = tr.getWidth(name);
        int infoW = tr.getWidth(info);
        int armorW = showArmor ? 4 * 24 : 0;
        int w = Math.max(Math.max(nameW, infoW), armorW) + 8;

        int armorH = showArmor ? 28 : 0;
        int h = armorH + 11 + 11 + (healthBar.get() ? 5 : 0) + 3;

        float sc = scale.getFloat();
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(sx, sy);
        m.scale(sc, sc);
        m.translate(-w / 2f, -h);

        if (background.get()) ctx.fill(0, 0, w, h, 0x99000000);
        // Draw a one-pixel border using supported DrawContext.fill calls.
        ctx.fill(0, 0, w, 1, 0x66B45CFF);
        ctx.fill(0, h - 1, w, h, 0x66B45CFF);
        ctx.fill(0, 1, 1, h - 1, 0x66B45CFF);
        ctx.fill(w - 1, 1, w, h - 1, 0x66B45CFF);

        int y = 2;
        if (showArmor) {
            int x = (w - armorW) / 2 + 2;
            for (EquipmentSlot slot : ARMOR) {
                ItemStack st = e.getEquippedStack(slot);
                if (!st.isEmpty()) {
                    ctx.drawItem(st, x + 2, y);
                    if (st.isDamageable()) {
                        int max2 = st.getMaxDamage();
                        int pctDur = Math.round((max2 - st.getDamage()) * 100f / max2);
                        String t = pctDur + "%";
                        ctx.drawText(tr, t, x + 10 - tr.getWidth(t) / 2, y + 17, durabilityColor(pctDur), true);
                    }
                }
                x += 24;
            }
            y += armorH;
        }

        ctx.drawText(tr, name, (w - nameW) / 2, y, isPlayer ? 0xFFFFFFFF : 0xFFCCCCCC, true);
        y += 11;
        ctx.drawText(tr, info, (w - infoW) / 2, y, healthColor(e.getHealth() / max), true);
        y += 11;

        if (healthBar.get()) {
            int bw = w - 6;
            ctx.fill(3, y, 3 + bw, y + 3, 0xFF202020);
            int fillW = Math.round(bw * MathHelper.clamp(e.getHealth() / max, 0f, 1f));
            ctx.fill(3, y, 3 + fillW, y + 3, healthColor(e.getHealth() / max));
            float abs = e.getAbsorptionAmount();
            if (abs > 0) ctx.fill(3, y + 3, 3 + Math.round(bw * MathHelper.clamp(abs / max, 0f, 1f)), y + 4, 0xFFFFD700);
        }
        m.popMatrix();
    }

    /** Green: good, yellow: moderate, orange: low, red: almost broken. */
    private static int durabilityColor(int pct) {
        if (pct >= 70) return 0xFF55FF55;
        if (pct >= 40) return 0xFFFFFF55;
        if (pct >= 15) return 0xFFFFAA00;
        return 0xFFFF5555;
    }

    private static int healthColor(float frac) {
        frac = MathHelper.clamp(frac, 0f, 1f);
        int r = (int) (255 * Math.min(1f, (1f - frac) * 2f));
        int g = (int) (255 * Math.min(1f, frac * 2f));
        return 0xFF000000 | (r << 16) | (g << 8) | 0x40;
    }
}
