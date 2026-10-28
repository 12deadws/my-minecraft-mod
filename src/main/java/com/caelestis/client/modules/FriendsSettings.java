package com.caelestis.client.modules;

import com.caelestis.client.config.ConfigManager;
import com.caelestis.client.module.Category;
import com.caelestis.client.module.Module;
import com.caelestis.client.setting.ButtonSetting;
import com.caelestis.client.util.Friends;
import net.minecraft.class_1657;
import net.minecraft.class_2561;
import net.minecraft.class_3966;

/**
 * Settings tab: friend management. Friends are never targeted by any module. Teammates (vanilla scoreboard teams)
 * are ignored automatically as well. The list is also stored in the config JSON ("friends").
 */
public class FriendsSettings extends Module {
    public FriendsSettings() {
        super("Friends", "Players you never want to target.", Category.SETTINGS, 1001, false);
        add(new ButtonSetting("Add Player Under Crosshair", "Adds the player you are looking at to your friends.", () -> {
            class_1657 p = lookedAt();
            if (p == null) {
                say("No player under your crosshair.");
                return;
            }
            Friends.add(p.method_5477().getString());
            ConfigManager.save();
            say("Added friend: " + p.method_5477().getString());
        }));
        add(new ButtonSetting("Remove Player Under Crosshair", "Removes the player you are looking at from your friends.", () -> {
            class_1657 p = lookedAt();
            if (p == null) {
                say("No player under your crosshair.");
                return;
            }
            Friends.remove(p.method_5477().getString());
            ConfigManager.save();
            say("Removed friend: " + p.method_5477().getString());
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

    private class_1657 lookedAt() {
        if (mc.field_1765 instanceof class_3966 ehr && ehr.method_17782() instanceof class_1657 p) return p;
        return null;
    }

    private void say(String msg) {
        if (mc.field_1724 != null) mc.field_1724.method_7353(class_2561.method_43470("[Caelestis Client] " + msg), false);
    }
}
