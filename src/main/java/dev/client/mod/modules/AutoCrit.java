package dev.client.mod.modules;

import dev.client.mod.*;
import dev.client.mod.Module;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.effect.MobEffects;
import org.lwjgl.glfw.GLFW;

/**
 * Packet crits for EVERY hit you make (clicks and the auto modules), no jumping needed.
 * Hooked in through MultiPlayerGameModeMixin: beforeAttack() runs just ahead of the attack packet, afterAttack() just after.
 *
 * Vanilla only crits when the server thinks you are falling (fallDistance > 0, not on ground) and NOT sprinting.
 * So right before the hit we tell the server "hopped 0.0625 up, came back down" and, if you were sprinting,
 * "stopped sprinting" (then "started sprinting" again right after so your state stays in sync).
 * Trade-off: that one hit has no sprint-knockback bonus.
 */
public class AutoCrit extends Module {
    private boolean stoppedSprint = false;

    public AutoCrit() { super("AutoCrit", GLFW.GLFW_KEY_C, false); }

    private static boolean canCrit(LocalPlayer p) {
        return p.onGround() && !p.isInWater() && !p.onClimbable() && !p.isPassenger()
                && !p.hasEffect(MobEffects.BLINDNESS);
    }

    public void beforeAttack(LocalPlayer p) {
        stoppedSprint = false;
        if (!canCrit(p)) return;

        if (p.isSprinting()) {
            p.connection.send(new ServerboundPlayerCommandPacket(p, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
            stoppedSprint = true;
        }
        double x = p.getX(), y = p.getY(), z = p.getZ();
        p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y + 0.0625, z, false, false));
        p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, false));
    }

    public void afterAttack(LocalPlayer p) {
        if (!stoppedSprint) return;
        stoppedSprint = false;
        p.connection.send(new ServerboundPlayerCommandPacket(p, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
    }
}
