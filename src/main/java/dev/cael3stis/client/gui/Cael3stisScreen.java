package dev.cael3stis.client.gui;

import dev.cael3stis.client.Cael3stisClient;
import dev.cael3stis.client.Module;
import dev.cael3stis.client.Setting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.class_11908;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_4185;
import net.minecraft.class_437;

/** Vanilla-widget ClickGUI: avoids mixin and direct renderer patching. */
public final class Cael3stisScreen extends class_437 {
    private static final int GOLD = 0xFFE4B95E;
    private static final int DARK = 0xFF10151E;
    private static final int PANEL = 0xFFF6F4EC;
    private static final String[] CATEGORIES = {"Combat", "Player", "Cpvp", "Render", "Settings", "Visuals"};

    private String category = "Combat";
    private String searchText = "";
    private Module detail;
    private Module assigning;
    private int page;
    private class_342 searchField;
    private class_342 friendField;

    public Cael3stisScreen() { super(class_2561.method_43470("Cael3stis Client")); }

    @Override protected void method_25426() {
        int left = Math.max(8, (field_22789 - 540) / 2);
        int top = Math.max(8, (field_22790 - 324) / 2);
        for (int i=0; i < CATEGORIES.length; i++) {
            String cat = CATEGORIES[i];
            add(left + 10 + i * 88, top + 40, 84, 20, cat, () -> {
                assigning = null; detail = null; category = cat; page = 0; refresh();
            });
        }
        add(left + 446, top + 292, 80, 20, "Close", this::method_25419);
        if (detail != null) { drawDetailWidgets(left, top); return; }
        if (category.equals("Settings")) { drawSettingsWidgets(left, top); return; }
        searchField = new class_342(field_22793, left + 12, top + 74, 232, 20, class_2561.method_43470("Find module"));
        searchField.method_1880(64);
        searchField.method_1852(searchText);
        searchField.method_1887("Search modules...");
        method_37063(searchField);
        add(left + 250, top + 74, 68, 20, "Search", () -> {
            searchText = searchField.method_1882().trim().toLowerCase(Locale.ROOT);
            page = 0; refresh();
        });
        if (category.equals("Visuals")) return;
        List<Module> modules = matching();
        int perPage = 7;
        int start = page * perPage;
        for (int j = start; j < Math.min(start + perPage, modules.size()); j++) {
            Module m = modules.get(j);
            int y = top + 105 + (j - start) * 25;
            add(left + 12, y, 195, 22, m.name, () -> { detail = m; page = 0; refresh(); });
            add(left + 215, y, 80, 22, m.enabled ? "ON" : "OFF", () -> { m.toggle(); refresh(); });
            add(left + 303, y, 82, 22, "Options", () -> { detail = m; page = 0; refresh(); });
        }
        add(left + 12, top + 292, 50, 20, "< Prev", () -> { if (page > 0) { page--; refresh(); } });
        add(left + 67, top + 292, 50, 20, "Next >", () -> { if ((page + 1) * perPage < modules.size()) { page++; refresh(); } });
    }

    private List<Module> matching() {
        List<Module> list = new ArrayList<>();
        for (Module module : Cael3stisClient.modules()) {
            if (module.category.equalsIgnoreCase(category) &&
                (searchText.isBlank() || module.name.toLowerCase(Locale.ROOT).contains(searchText))) list.add(module);
        }
        return list;
    }

    private void drawDetailWidgets(int left, int top) {
        Module m = detail;
        add(left + 12, top + 72, 66, 22, "< Back", () -> { detail = null; assigning = null; page = 0; refresh(); });
        add(left + 84, top + 72, 110, 22, m.enabled ? "Enabled" : "Disabled", () -> { m.toggle(); refresh(); });
        add(left + 203, top + 72, 190, 22,
                assigning == m ? "Press a key..." : ("Keybind: " + (m.key == GLFW.GLFW_KEY_UNKNOWN ? "NONE" : GLFW.glfwGetKeyName(m.key, 0) == null ? "Key " + m.key : GLFW.glfwGetKeyName(m.key, 0))),
                () -> { assigning = m; refresh(); });
        int offset = page * 6;
        for (int i = offset; i < Math.min(offset + 6, m.settings.size()); i++) {
            Setting setting = m.settings.get(i);
            int y = top + 103 + (i - offset) * 29;
            if (setting instanceof Setting.Toggle toggle) {
                add(left + 12, y, 365, 24, setting.name + ": " + setting.display(), () -> { toggle.value = !toggle.value; Cael3stisClient.save(); refresh(); });
            } else if (setting instanceof Setting.Number number) {
                add(left + 12, y, 55, 24, "-", () -> { number.adjust(-1); Cael3stisClient.save(); refresh(); });
                add(left + 72, y, 245, 24, setting.name + ": " + setting.display(), () -> {});
                add(left + 322, y, 55, 24, "+", () -> { number.adjust(1); Cael3stisClient.save(); refresh(); });
            } else if (setting instanceof Setting.Choice choice) {
                add(left + 12, y, 365, 24, setting.name + ": " + setting.display(), () -> { choice.next(1); Cael3stisClient.save(); refresh(); });
            }
        }
        add(left + 12, top + 281, 75, 20, "< Settings", () -> { if(page > 0) { page--; refresh(); } });
        add(left + 92, top + 281, 75, 20, "Settings >", () -> { if((page+1)*6 < m.settings.size()) { page++; refresh(); } });
    }

    private void drawSettingsWidgets(int left, int top) {
        friendField = new class_342(field_22793, left + 12, top + 88, 180, 20, class_2561.method_43470("Player name"));
        friendField.method_1880(16);
        friendField.method_1887("Friend username");
        method_37063(friendField);
        add(left + 200, top + 88, 95, 20, "Add Friend", () -> { Cael3stisClient.addFriend(friendField.method_1882()); refresh(); });
        add(left + 301, top + 88, 95, 20, "Remove", () -> { Cael3stisClient.removeFriend(friendField.method_1882()); refresh(); });
    }

    private void add(int x, int y, int w, int h, String text, Runnable action) {
        method_37063(class_4185.method_46430(class_2561.method_43470(text), button -> action.run()).method_46434(x, y, w, h).method_46431());
    }
    private void refresh() { method_41843(); }

    @Override public boolean method_25404(class_11908 input) {
        int key = input.comp_4795();
        if (assigning != null) {
            Cael3stisClient.assignKey(assigning, key == GLFW.GLFW_KEY_ESCAPE ? GLFW.GLFW_KEY_UNKNOWN : key);
            assigning = null;
            refresh();
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT) { method_25419(); return true; }
        return super.method_25404(input);
    }

    @Override public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        method_25420(ctx, mouseX, mouseY, delta);
        int left = Math.max(8, (field_22789 - 540) / 2);
        int top = Math.max(8, (field_22790 - 324) / 2);
        ctx.method_25294(left - 2, top - 2, left + 542, top + 326, GOLD);
        ctx.method_25294(left, top, left + 540, top + 324, PANEL);
        ctx.method_25294(left, top, left + 540, top + 34, DARK);
        ctx.method_51433(field_22793, "CAEL3STIS  /  HEAVENLY CLIENT", left + 13, top + 12, GOLD, true);
        ctx.method_51433(field_22793, "v" + Cael3stisClient.VERSION, left + 468, top + 12, 0xFFF2F0E8, true);
        if (detail != null) {
            ctx.method_51433(field_22793, detail.name + "  -  " + detail.description, left + 12, top + 261, 0xFF292D35, false);
        } else if (category.equals("Settings")) {
            ctx.method_51433(field_22793, "Friends never become automated targets", left + 12, top + 125, 0xFF292D35, false);
            ctx.method_51433(field_22793, String.join(", ", Cael3stisClient.friends()), left + 12, top + 146, 0xFF292D35, false);
        } else if (category.equals("Visuals")) {
            ctx.method_51433(field_22793, "Visual effects are planned, not implemented in v0.4.0.", left + 12, top + 90, 0xFF292D35, false);
        } else {
            ctx.method_51433(field_22793, "Implemented modules in " + category + ": " + matching().size(), left + 330, top + 80, 0xFF292D35, false);
        }
        super.method_25394(ctx, mouseX, mouseY, delta);
    }
    @Override public boolean method_25421() { return false; }
}
