package com.caelestis.client.modules;

import com.caelestis.client.config.ConfigManager;
import com.caelestis.client.module.Category;
import com.caelestis.client.module.Module;
import com.caelestis.client.setting.BoolSetting;
import com.caelestis.client.setting.ButtonSetting;
import com.caelestis.client.setting.NumberSetting;

/** Settings tab: HUD and GUI options. Configuration only, not a toggleable module. */
public class ClientSettings extends Module {
    public final BoolSetting watermark = add(new BoolSetting("Watermark", "Show the Caelestis Client watermark.", true));
    public final BoolSetting moduleList = add(new BoolSetting("Module List", "Show enabled modules on the HUD.", true));
    public final BoolSetting tooltips = add(new BoolSetting("Tooltips", "Show tooltips in the ClickGUI.", true));
    public final NumberSetting animSpeed = add(new NumberSetting("Animation Speed", "Speed of ClickGUI animations.", 10, 3, 30, 1));
    public final NumberSetting guiOpacity = add(new NumberSetting("GUI Opacity", "Opacity of ClickGUI panels.", 92, 40, 100, 1, "%"));

    public ClientSettings() {
        super("Client", "HUD and ClickGUI options.", Category.SETTINGS, 1000, false);
        add(new ButtonSetting("Save Config", "Write all settings and keybinds to disk now.", ConfigManager::save));
        add(new ButtonSetting("Reload Config", "Re-read the config file from disk.", ConfigManager::reload));
    }
}
