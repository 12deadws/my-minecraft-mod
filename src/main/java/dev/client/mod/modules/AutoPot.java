package dev.client.mod.modules;

import dev.client.mod.*;
import dev.client.mod.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.lwjgl.glfw.GLFW;

/**
 * Throws a splash Instant Health at your feet at or below Config.autoPotHearts (default 3 hearts).
 *
 * Instead of snapping the camera, it eases the view down, throws once it is looking straight down,
 * then eases back to where you were looking. The whole motion lasts Config.autoPotLookTicks
 * (default 8 ticks = 0.4s): ~1/3 looking down, throw at the midpoint, ~1/3 looking back.
 */
public class AutoPot extends Module {
    private int cooldown = 0;
    private int tick = -1;          // -1 = idle, otherwise ticks since the motion started
    private int prevSlot, potSlot;
    private float startPitch, yaw;
    private boolean thrown;

    public AutoPot() {
        super("AutoPot", GLFW.GLFW_KEY_P, false);
        settings.add(new Setting("Hearts", () -> String.format(java.util.Locale.ROOT, "%.1f", Config.autoPotHearts),
                () -> { Config.autoPotHearts = Math.max(0.5, Config.autoPotHearts - 0.5); Config.save(); },
                () -> { Config.autoPotHearts = Math.min(10.0, Config.autoPotHearts + 0.5); Config.save(); }));
        settings.add(new Setting("Look time", () -> String.format(java.util.Locale.ROOT, "%.2fs", Config.autoPotLookTicks / 20.0),
                () -> { Config.autoPotLookTicks = Math.max(4, Config.autoPotLookTicks - 1); Config.save(); },
                () -> { Config.autoPotLookTicks = Math.min(20, Config.autoPotLookTicks + 1); Config.save(); }));
        settings.add(new Setting("Refill slot", () -> Integer.toString(Config.autoPotRefillSlot),
                () -> { Config.autoPotRefillSlot = Math.max(1, Config.autoPotRefillSlot - 1); Config.save(); },
                () -> { Config.autoPotRefillSlot = Math.min(9, Config.autoPotRefillSlot + 1); Config.save(); }));
    }

    /** True while the look-down / throw / look-back motion is running. */
    public boolean busy() { return tick >= 0; }

    private static boolean isHealPot(ItemStack s) {
        if (!s.is(Items.SPLASH_POTION)) return false;
        PotionContents c = s.get(DataComponents.POTION_CONTENTS);
        return c != null && (c.is(Potions.HEALING) || c.is(Potions.STRONG_HEALING));
    }

    private static int findPot(LocalPlayer p) {
        for (int i = 0; i < 9; i++) {
            if (isHealPot(p.getInventory().getItem(i))) return i;
        }
        return -1;
    }

    /** Inventory index (0-8 hotbar, 9-35 main) -> slot id in the player inventory menu. */
    private static int menuSlot(int invIndex) { return invIndex < 9 ? 36 + invIndex : invIndex; }

    /**
     * Hotbar has no heal pots: swap one in from the main inventory. Goes into an empty hotbar slot if there is
     * one, otherwise into Config.autoPotRefillSlot (the displaced item moves to where the pot was).
     */
    private static boolean refill(Minecraft mc, LocalPlayer p) {
        int from = -1;
        for (int i = 9; i < 36; i++) {
            if (isHealPot(p.getInventory().getItem(i))) { from = i; break; }
        }
        if (from < 0) return false;

        int to = -1;
        for (int i = 0; i < 9; i++) {
            if (p.getInventory().getItem(i).isEmpty()) { to = i; break; }
        }
        if (to < 0) to = Config.autoPotRefillSlot - 1;

        // SWAP click: "button" is the hotbar slot (0-8) to swap with.
        mc.gameMode.handleInventoryMouseClick(p.inventoryMenu.containerId, menuSlot(from), to, ClickType.SWAP, p);
        return true;
    }

    private static float ease(float t) { return t * t * (3f - 2f * t); }   // smoothstep
    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    @Override public void onTick() {
        Minecraft mc = Util.mc;
        LocalPlayer p = mc.player;

        if (tick >= 0) { step(mc, p); return; }
        if (cooldown > 0) { cooldown--; return; }
        if (mc.screen != null) return;

        potSlot = findPot(p);
        if (potSlot < 0) {
            // Hotbar ran out: restock from the inventory so the next throw is ready.
            if (refill(mc, p)) cooldown = 4;
            return;
        }

        float hp = p.getHealth();
        if (hp > (float) (Config.autoPotHearts * 2.0) || hp <= 0) return;

        prevSlot = p.getInventory().getSelectedSlot();
        p.getInventory().setSelectedSlot(potSlot);
        startPitch = p.getXRot();
        yaw = p.getYRot();
        thrown = false;
        tick = 0;
        step(mc, p);
    }

    private void step(Minecraft mc, LocalPlayer p) {
        int total = Math.max(4, Config.autoPotLookTicks);
        int ramp = Math.max(1, total / 3);
        int throwAt = total / 2;

        float pitch;
        if (tick < ramp) {
            pitch = lerp(startPitch, 90f, ease((tick + 1) / (float) ramp));
        } else if (tick < total - ramp) {
            pitch = 90f;
        } else {
            pitch = lerp(90f, startPitch, ease((tick - (total - ramp) + 1) / (float) ramp));
        }
        p.setXRot(pitch);

        if (!thrown && tick >= throwAt) {
            // useItem sends our current rotation, so make sure the server has seen "straight down" first.
            p.getInventory().setSelectedSlot(potSlot);
            p.connection.send(new ServerboundMovePlayerPacket.Rot(yaw, 90f, p.onGround(), p.horizontalCollision));
            mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
            thrown = true;
        }

        if (++tick >= total) finish(p);
    }

    private void finish(LocalPlayer p) {
        p.setXRot(startPitch);
        p.getInventory().setSelectedSlot(prevSlot);
        tick = -1;
        cooldown = 15;
    }

    @Override public void onDisable() {
        if (tick < 0) return;
        LocalPlayer p = Util.mc.player;
        if (p != null) finish(p);
        else tick = -1;
    }
}
