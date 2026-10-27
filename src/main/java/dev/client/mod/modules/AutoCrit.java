package dev.client.mod.modules;

import dev.client.mod.*;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.lwjgl.glfw.GLFW;

/** Packet crits: fakes a tiny hop right before the hit. Needs a server that doesn't flag it. */
public class AutoCrit extends Module {
    public AutoCrit() { super("AutoCrit", GLFW.GLFW_KEY_C, false); }

    public void sendCritPackets(LocalPlayer p) {
        if (!p.onGround() || p.isInWater() || p.onClimbable()) return;
        double x = p.getX(), y = p.getY(), z = p.getZ();
        p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y + 0.0625, z, false, false));
        p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, false));
    }
}
