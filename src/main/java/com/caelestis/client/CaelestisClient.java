package com.caelestis.client;

import com.caelestis.client.config.ConfigManager;
import com.caelestis.client.hud.Hud;
import com.caelestis.client.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caelestis Client entry point. Client-side only; no mixins and no world-render hooks, so it coexists with
 * Sodium and other rendering mods.
 */
public class CaelestisClient implements ClientModInitializer {
    public static final String NAME = "Caelestis Client";
    public static final String ID = "caelestis_client";
    public static final String VERSION = "1.2.0";
    public static final Logger LOG = LoggerFactory.getLogger(ID);

    private static ModuleManager modules;

    public static ModuleManager modules() {
        return modules;
    }

    @Override
    public void onInitializeClient() {
        modules = new ModuleManager();
        ConfigManager.init(modules); // loads keybinds + settings; modules always start disabled

        // Game logic runs at the start of each client tick.
        ClientTickEvents.START_CLIENT_TICK.register(client -> modules.tick(client));

        // Per-frame overlay layer (HUD, NameTags, Hole ESP, Soft Aim smoothing).
        HudElementRegistry.addLast(Identifier.of(ID, "overlay"), (ctx, tickCounter) -> {
            modules.render(ctx, tickCounter.getTickProgress(true));
            Hud.render(ctx, modules);
        });

        // Cleanup on disconnect / world change, and persist on exit.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> modules.cleanup());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> modules.cleanup());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigManager.save());

        LOG.info("{} {} loaded. Press Right Shift in-game to open the ClickGUI.", NAME, VERSION);
    }
}
