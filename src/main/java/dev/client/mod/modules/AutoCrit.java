package dev.client.mod.modules;

import dev.client.mod.*;
import dev.client.mod.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import org.lwjgl.glfw.GLFW;

/**
 * Crits without jumping: right before each of YOUR attacks (clicks and macros) it sends a tiny
 * hop in the movement packets so the server sees you as falling.
 * Vanilla never crits while sprinting, so it briefly un-sprints for the hit and restores it next tick.
 */
public class AutoCrit extends Module {
    private boolean resprint;

    public AutoCrit() { super("AutoCrit", GLFW.GLFW_KEY_C, false); }

    public void sendCritPackets(LocalPlayer p) {
        if (!p.onGround() || p.isInWater() || p.isInLava() || p.onClimbable() || p.isPassenger()) return;
        if (p.isSprinting()) {
            p.connection.send(new ServerboundPlayerCommandPacket(p, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
            resprint = true;
        }
        double x = p.getX(), y = p.getY(), z = p.getZ();
        p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y + 0.0625, z, false, false));
        p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, false));
    }

    @Override public void onTick() {
        if (!resprint) return;
        resprint = false;
        LocalPlayer p = Minecraft.getInstance().player;
        if (p != null && p.isSprinting()) {
            p.connection.send(new ServerboundPlayerCommandPacket(p, ServerboundPlayerCommandPacket.Action.START_SPRINTING));
        }
    }
}
