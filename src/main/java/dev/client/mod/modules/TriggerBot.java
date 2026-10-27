package dev.client.mod.modules;

import dev.client.mod.*;
import dev.client.mod.Module;
import net.minecraft.world.entity.Entity;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.ThreadLocalRandom;

public class TriggerBot extends Module {
    private int retryDelay = 0;
    private int reaction = -1;   // -1 = no target being tracked

    public TriggerBot() {
        super("TriggerBot", GLFW.GLFW_KEY_R, false);
        settings.add(new Setting("Hit chance", () -> Config.triggerHitChance + "%",
                () -> { Config.triggerHitChance = Config.clamp(Config.triggerHitChance - 5); Config.save(); },
                () -> { Config.triggerHitChance = Config.clamp(Config.triggerHitChance + 5); Config.save(); }));
        settings.add(new Setting("Min delay", () -> Config.triggerMinDelay + " ticks",
                () -> { Config.triggerMinDelay = Math.max(0, Config.triggerMinDelay - 1); Config.save(); },
                () -> {
                    Config.triggerMinDelay++;
                    if (Config.triggerMaxDelay < Config.triggerMinDelay) Config.triggerMaxDelay = Config.triggerMinDelay;
                    Config.save();
                }));
        settings.add(new Setting("Max delay", () -> Config.triggerMaxDelay + " ticks",
                () -> { Config.triggerMaxDelay = Math.max(Config.triggerMinDelay, Config.triggerMaxDelay - 1); Config.save(); },
                () -> { Config.triggerMaxDelay++; Config.save(); }));
    }

    @Override public void onTick() {
        if (retryDelay > 0) { retryDelay--; return; }

        Entity t = Util.target(Util.reach());
        if (t == null) { reaction = -1; return; }   // target lost: next one gets a fresh delay
        if (!Util.ready()) return;

        // Reaction delay: picked once when a target is lined up, then counted down.
        if (reaction < 0) {
            ThreadLocalRandom r = ThreadLocalRandom.current();
            reaction = Config.triggerMinDelay + r.nextInt(Config.triggerMaxDelay - Config.triggerMinDelay + 1);
        }
        if (reaction > 0) { reaction--; return; }

        // Hit chance: one roll per attempt. On a miss, wait ~half a second before rolling again.
        if (ThreadLocalRandom.current().nextInt(100) < Config.triggerHitChance) {
            Util.attack(t);
        } else {
            retryDelay = 10;
        }
        reaction = -1;
    }

    @Override public void onDisable() { reaction = -1; retryDelay = 0; }
}
