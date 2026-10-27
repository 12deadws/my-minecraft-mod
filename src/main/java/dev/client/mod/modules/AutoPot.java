package dev.client.mod.modules;

import dev.client.mod.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.lwjgl.glfw.GLFW;

/** Throws a splash Instant Health at your feet at or below Config.autoPotHearts (default 3 hearts). */
public class AutoPot extends Module {
    private int cooldown = 0;
    public AutoPot() {
        super("AutoPot", GLFW.GLFW_KEY_P, false);
        settings.add(new Setting("Hearts", () -> String.format(java.util.Locale.ROOT, "%.1f", Config.autoPotHearts),
                () -> { Config.autoPotHearts = Math.max(0.5, Config.autoPotHearts - 0.5); Config.save(); },
                () -> { Config.autoPotHearts = Math.min(10.0, Config.autoPotHearts + 0.5); Config.save(); }));
    }

    private static boolean isHealPot(ItemStack s) {
        if (!s.is(Items.SPLASH_POTION)) return false;
        PotionContents c = s.get(DataComponents.POTION_CONTENTS);
        return c != null && (c.is(Potions.HEALING) || c.is(Potions.STRONG_HEALING));
    }

    @Override public void onTick() {
        if (cooldown > 0) { cooldown--; return; }
        Minecraft mc = Util.mc;
        LocalPlayer p = mc.player;
        if (p.getHealth() > (float) (Config.autoPotHearts * 2.0) || p.getHealth() <= 0) return;

        int prev = p.getInventory().getSelectedSlot();
        if (!Util.select(AutoPot::isHealPot)) return;

        float yaw = p.getYRot();
        p.connection.send(new ServerboundMovePlayerPacket.Rot(yaw, 90f, p.onGround(), p.horizontalCollision));
        mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
        p.connection.send(new ServerboundMovePlayerPacket.Rot(yaw, p.getXRot(), p.onGround(), p.horizontalCollision));
        p.getInventory().setSelectedSlot(prev);
        cooldown = 10;
    }
}
