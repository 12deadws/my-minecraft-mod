package dev.cael3stis.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.List;

/** Configurable module dashboard. All visual controls are functional; gameplay modules remain separate work. */
public final class ClientScreen extends Screen {
    private static final String[] CATEGORIES = {"Combat", "Player", "Cpvp", "Render", "Settings", "Visuals"};
    private String category = "Combat";
    private String search = "";
    private Cael3stisClient.Module awaitingBind;
    private int scroll = 0;
    public ClientScreen() { super(Text.literal("Cael3stis Client")); }

    private List<Cael3stisClient.Module> visible() {
        List<Cael3stisClient.Module> result = new ArrayList<>();
        for (var module : Cael3stisClient.MODULES.values())
            if (module.category.equals(category) && module.name.toLowerCase().contains(search.toLowerCase())) result.add(module);
        return result;
    }
    private int left() { return width / 2 - 205; }
    private int top() { return height / 2 - 135; }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int x = left(), y = top();
        ctx.fill(x, y, x + 410, y + 270, 0xF5FAF8F2);
        ctx.fill(x, y, x + 410, y + 5, 0xFFD6AC51);
        ctx.fill(x, y + 5, x + 116, y + 270, 0xFFF1EAD9);
        ctx.drawTextWithShadow(textRenderer, "CAEL3STIS", x + 12, y + 17, 0xFF8C681F);
        ctx.drawTextWithShadow(textRenderer, "CLIENT", x + 12, y + 30, 0xFF8C681F);
        for (int i = 0; i < CATEGORIES.length; i++) {
            int yy = y + 60 + i * 29;
            if (CATEGORIES[i].equals(category)) ctx.fill(x + 5, yy - 4, x + 111, yy + 15, 0xFFE1C98D);
            ctx.drawTextWithShadow(textRenderer, CATEGORIES[i], x + 13, yy, 0xFF493A1B);
        }
        ctx.fill(x + 126, y + 15, x + 397, y + 37, 0xFFECE5D5);
        ctx.drawTextWithShadow(textRenderer, "Search: " + search + "_", x + 134, y + 22, 0xFF514C42);
        if (category.equals("Settings") || category.equals("Visuals")) {
            ctx.drawTextWithShadow(textRenderer, category.equals("Settings") ? "Use Combat category to configure active modules" : "Visual effects not yet implemented", x + 133, y + 62, 0xFF6A6252);
        } else {
            List<Cael3stisClient.Module> modules = visible();
            for (int i = scroll; i < modules.size() && i < scroll + 8; i++) {
                var m = modules.get(i);
                int yy = y + 50 + (i - scroll) * 25;
                ctx.fill(x + 126, yy, x + 397, yy + 22, 0xFFECE5D5);
                ctx.drawTextWithShadow(textRenderer, m.name, x + 132, yy + 7, 0xFF51452D);
                ctx.fill(x + 300, yy + 3, x + 333, yy + 19, m.enabled ? 0xFFB99A4B : 0xFF99958C);
                ctx.drawTextWithShadow(textRenderer, m.enabled ? "ON" : "OFF", x + 306, yy + 7, 0xFFFFFFFF);
                String key = awaitingBind == m ? "..." : m.key == GLFW.GLFW_KEY_UNKNOWN ? "NONE" : GLFW.glfwGetKeyName(m.key, 0);
                if (key == null) key = "#" + m.key;
                ctx.drawTextWithShadow(textRenderer, key, x + 340, yy + 7, 0xFF8C681F);
            }
        }
        if (category.equals("Settings")) {
            String[] names = {"Trigger delay (ms)", "Jump chance (%)", "Jump delay (ms)"};
            for (int i = 0; i < names.length; i++) {
                int yy = y + 50 + i * 29;
                ctx.drawTextWithShadow(textRenderer, names[i] + ": " + Cael3stisClient.getSetting(names[i]), x + 130, yy + 4, 0xFF51452D);
                ctx.drawTextWithShadow(textRenderer, "-   +", x + 354, yy + 4, 0xFF8C681F);
            }
        }
        ctx.drawTextWithShadow(textRenderer, "Click ON/OFF to toggle; key field to bind", x + 127, y + 253, 0xFF7D6B4C);
    }
    @Override public boolean mouseClicked(double mx, double my, int button) {
        int x = left(), y = top();
        if (button == 0) {
            for (int i = 0; i < CATEGORIES.length; i++) {
                int yy = y + 60 + i * 29;
                if (mx >= x + 5 && mx < x + 115 && my >= yy - 4 && my < yy + 16) {
                    category = CATEGORIES[i]; scroll = 0; awaitingBind = null; return true;
                }
            }
            if (category.equals("Settings")) {
                String[] names = {"Trigger delay (ms)", "Jump chance (%)", "Jump delay (ms)"};
                for (int i = 0; i < names.length; i++) {
                    int yy = y + 50 + i * 29;
                    if (my >= yy && my < yy + 20) {
                        if (mx >= x + 348 && mx < x + 368) { Cael3stisClient.adjustSetting(names[i], -1); return true; }
                        if (mx >= x + 370 && mx < x + 396) { Cael3stisClient.adjustSetting(names[i], 1); return true; }
                    }
                }
            }
            var modules = visible();
            for (int i = scroll; i < modules.size() && i < scroll + 8; i++) {
                int yy = y + 50 + (i - scroll) * 25;
                if (my >= yy && my < yy + 22) {
                    if (mx >= x + 300 && mx < x + 335) { modules.get(i).toggle(client); return true; }
                    if (mx >= x + 338 && mx < x + 397) { awaitingBind = modules.get(i); return true; }
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (awaitingBind != null) {
            if (keyCode != GLFW.GLFW_KEY_RIGHT_SHIFT) {
                for (var m : Cael3stisClient.MODULES.values()) if (m.key == keyCode) m.key = GLFW.GLFW_KEY_UNKNOWN;
                awaitingBind.key = keyCode == GLFW.GLFW_KEY_ESCAPE ? GLFW.GLFW_KEY_UNKNOWN : keyCode;
                Cael3stisClient.save();
            }
            awaitingBind = null; return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) { search = search.substring(0, search.length() - 1); scroll = 0; return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    @Override public boolean charTyped(char chr, int modifiers) {
        if (awaitingBind == null && search.length() < 28 && !Character.isISOControl(chr)) { search += chr; scroll = 0; return true; }
        return super.charTyped(chr, modifiers);
    }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = Math.max(0, Math.min(Math.max(0, visible().size() - 8), scroll - (int)Math.signum(verticalAmount)));
        return true;
    }
    @Override public boolean shouldPause() { return false; }
}
