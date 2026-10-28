package com.caelestis.client.modules;

import com.caelestis.client.config.ConfigManager;
import com.caelestis.client.module.Category;
import com.caelestis.client.module.Module;
import com.caelestis.client.setting.ButtonSetting;
import com.caelestis.client.util.Friends;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;

/**
 * Settings tab: friend management. Friends are never targeted by any module. Teammates (vanilla scoreboard teams)
 * are ignored automatically as well. The list is also stored in the config JSON ("friends").
 */
public class FriendsSettings extends Module {
    public FriendsSettings() {
        super("Friends", "Players you never want to target.", Category.SETTINGS, 1001, false);
        add(new ButtonSetting("Add Player Under Crosshair", "Adds the player you are looking at to your friends.", () -> {
            PlayerEntity p = lookedAt();
            if (p == null) {
                say("No player under your crosshair.");
                return;
            }
            Friends.add(p.getName().getString());
            ConfigManager.save();
            say("Added friend: " + p.getName().getString());
        }));
        add(new ButtonSetting("Remove Player Under Crosshair", "Removes the player you are looking at from your friends.", () -> {
            PlayerEntity p = lookedAt();
            if (p == null) {
                say("No player under your crosshair.");
                return;
            }
            Friends.remove(p.getName().getString());
            ConfigManager.save();
            say("Removed friend: " + p.getName().getString());
        }));
        add(new ButtonSetting("Clear All Friends", "Remove everyone from your friends list.", () -> {
            Friends.clear();
            ConfigManager.save();
            say("Friends cleared.");
        }));
        add(new ButtonSetting("List Friends", "Print your friends in chat.", () -> {
            say(Friends.size() == 0 ? "No friends." : "Friends: " + String.join(", ", Friends.snapshot()));
        }));
    }

    private PlayerEntity lookedAt() {
        if (mc.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() instanceof PlayerEntity p) return p;
        return null;
    }

    private void say(String msg) {
        if (mc.player != null) mc.player.sendMessage(Text.literal("[Caelestis Client] " + msg), false);
    }
}
