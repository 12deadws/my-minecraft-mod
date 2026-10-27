package dev.client.mod;

import dev.client.mod.modules.AutoCrit;
import dev.client.mod.modules.AutoPot;
import dev.client.mod.modules.Reach;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

public class Util {
    public static final Minecraft mc = Minecraft.getInstance();

    /** Current attack range: vanilla 3.0, or whatever the Reach module is set to. */
    public static double reach() {
        Reach r = ModuleManager.get(Reach.class);
        return (r != null && r.enabled) ? Config.reachRange : 3.0;
    }

    /** Entity along the crosshair within range (ignores walls; add a block raycast if you want that). */
    public static Entity target(double range) {
        LocalPlayer p = mc.player;
        Vec3 start = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        Vec3 end = start.add(look.scale(range));
        AABB box = p.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p, start, end, box,
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e != p
                        && !(e instanceof Player pl && Config.isFriend(pl.getName().getString())),
                range * range);
        return hit == null ? null : hit.getEntity();
    }

    public static void attack(Entity e) {
        LocalPlayer p = mc.player;
        AutoCrit crit = ModuleManager.get(AutoCrit.class);
        if (crit != null && crit.enabled) crit.sendCritPackets(p);
        mc.gameMode.attack(p, e);
        p.swing(InteractionHand.MAIN_HAND);
    }

    /** True while AutoPot is mid-throw; other modules must not touch the hotbar or attack then. */
    public static boolean potBusy() {
        AutoPot ap = ModuleManager.get(AutoPot.class);
        return ap != null && ap.busy();
    }

    /** Selects the first hotbar slot whose stack matches. */
    public static boolean select(Predicate<ItemStack> f) {
        if (potBusy()) return false;
        LocalPlayer p = mc.player;
        for (int i = 0; i < 9; i++) {
            if (f.test(p.getInventory().getItem(i))) {
                p.getInventory().setSelectedSlot(i);
                return true;
            }
        }
        return false;
    }

    /** True only at a full attack charge (and never while AutoPot is mid-throw). */
    public static boolean ready() {
        if (potBusy()) return false;
        return mc.player.getAttackStrengthScale(0f) >= 1.0f;
    }
}
