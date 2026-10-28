package dev.client.mod;

import dev.client.mod.modules.AutoCrit;
import dev.client.mod.modules.Reach;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
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

    /** Attack: spears are used with a real left-click (they jab/lunge); everything else is a normal attack packet. */
    public static void attack(Entity e) {
        LocalPlayer p = mc.player;
        if (p.getMainHandItem().is(ItemTags.SPEARS)) {
            clickAttack();
            return;
        }
        // AutoCrit's packets are sent from the attack hook in MyClient, so manual clicks crit too.
        mc.gameMode.attack(p, e);
        p.swing(InteractionHand.MAIN_HAND);
    }

    /** Simulates pressing the attack (left click) key once. Handled by the game on its next input pass. */
    public static void clickAttack() {
        KeyMapping.click(InputConstants.getKey(mc.options.keyAttack.saveString()));
    }

    /** Selects the first hotbar slot whose stack matches. */
    public static boolean select(Predicate<ItemStack> f) {
        LocalPlayer p = mc.player;
        for (int i = 0; i < 9; i++) {
            if (f.test(p.getInventory().getItem(i))) {
                p.getInventory().setSelectedSlot(i);
                return true;
            }
        }
        return false;
    }

    public static boolean ready() {
        return mc.player.getAttackStrengthScale(0f) >= 0.95f;
    }
}
