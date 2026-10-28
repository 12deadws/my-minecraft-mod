package com.caelestis.client.gui;

import com.caelestis.client.CaelestisClient;
import com.caelestis.client.config.ConfigManager;
import com.caelestis.client.module.Category;
import com.caelestis.client.module.Module;
import com.caelestis.client.module.ModuleManager;
import com.caelestis.client.modules.ClientSettings;
import com.caelestis.client.setting.BoolSetting;
import com.caelestis.client.setting.ButtonSetting;
import com.caelestis.client.setting.EnumSetting;
import com.caelestis.client.setting.NumberSetting;
import com.caelestis.client.setting.Setting;
import com.caelestis.client.util.KeyNames;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.class_11905;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_3532;
import net.minecraft.class_437;

/**
 * Ivory-and-gold heavenly ClickGUI. Left click a module to toggle it, right click to open its settings.
 * Windows are draggable and scrollable, with a module search bar, sliders, dropdowns and tooltips.
 * Right Shift closes it (and is reserved: it can never be bound to a module).
 */
public class ClickGuiScreen extends class_437 {
    private static final int PW = 470, HEAD = 46, ROW = 38, SROW = 19, SNUM = 27, SOPT = 17, GAP = 12;

    private static final int ACCENT = 0xFFD5A638;
    private static final int ACCENT_DIM = 0xFFC29327;
    private static final int ACCENT_DARK = 0xFFF4E3B7;
    private static final int TEXT = 0xFF43341C;
    private static final int TEXT_DIM = 0xFF837354;
    private static final int OFF_COL = 0xFFDDD2BE;
    private static final int PANEL_BG = 0xFFF9F7EE;
    private static final int ROW_BG = 0xFFF1E9D8;

    private enum Kind {MODULE, KEYBIND, BOOL, NUMBER, ENUM, ENUM_OPTION, BUTTON}

    private record Row(Kind kind, Module m, Setting s, int opt, float x, float y, float w, float h, float clipTop, float clipBottom) {
    }

    private interface Visitor {
        boolean visit(Row r); // return true to stop walking
    }

    private static final class Panel {
        final Category cat;
        float x, y, scroll;

        Panel(Category cat, float x, float y) {
            this.cat = cat;
            this.x = x;
            this.y = y;
        }
    }

    private final ModuleManager mm;
    private final ClientSettings cfg;
    private final class_310 client = class_310.method_1551();
    private final List<Panel> panels = new ArrayList<>(); // draw order, last = front

    private final Map<Module, Float> expand = new HashMap<>();
    private final Set<Module> expanded = new HashSet<>();
    private final Map<Module, Float> toggleAnim = new HashMap<>();
    private final Map<EnumSetting, Float> dropAnim = new HashMap<>();
    private final Set<EnumSetting> openDrops = new HashSet<>();

    private Category selectedCategory = Category.values()[0];
    private String search = "";
    private boolean searchFocused = false;
    private Module binding = null;

    private Panel draggingPanel = null;
    private float dragDX, dragDY;
    private NumberSetting draggingSlider = null;
    private float sliderX, sliderW;

    private String toast = null;
    private long toastUntil = 0;

    private Object hoverKey = null;
    private String hoverText = null;
    private long hoverSince = 0;
    private long lastNanos = System.nanoTime();
    private float openAnim = 0f;

    public ClickGuiScreen(ModuleManager mm) {
        super(class_2561.method_43470(CaelestisClient.NAME));
        this.mm = mm;
        this.cfg = mm.clientSettings;
        int i = 0;
        for (Category c : Category.values()) {
            float[] saved = ConfigManager.PANEL_POS.get(c.name());
            float x = 180;
            float y = 70;
            panels.add(new Panel(c, x, y));
            i++;
        }
    }

    @Override
    public boolean method_25421() {
        return false;
    }

    @Override
    public void method_25432() {
        for (Panel p : panels) ConfigManager.PANEL_POS.put(p.cat.name(), new float[]{p.x, p.y});
        ConfigManager.save();
        super.method_25432();
    }

    // ================================================================== helpers

    private static float ease(float t) {
        t = class_3532.method_15363(t, 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    private static int alpha(int rgb, float a) {
        int al = class_3532.method_15340((int) (a * 255f), 0, 255);
        return (al << 24) | (rgb & 0xFFFFFF);
    }

    private static int mix(int c1, int c2, float t) {
        t = class_3532.method_15363(t, 0f, 1f);
        int a = (int) class_3532.method_48781(t, (c1 >>> 24) & 0xFF, (c2 >>> 24) & 0xFF);
        int r = (int) class_3532.method_48781(t, (c1 >> 16) & 0xFF, (c2 >> 16) & 0xFF);
        int g = (int) class_3532.method_48781(t, (c1 >> 8) & 0xFF, (c2 >> 8) & 0xFF);
        int b = (int) class_3532.method_48781(t, c1 & 0xFF, c2 & 0xFF);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Rounded rectangle built from scan-line fills (no textures, no shaders). */
    private static void roundRect(class_332 ctx, float fx, float fy, float fw, float fh, int r, int color) {
        int x = Math.round(fx), y = Math.round(fy), w = Math.round(fw), h = Math.round(fh);
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
            ctx.method_25294(x + inset, y + i, x + w - inset, y + i + 1, color);
            ctx.method_25294(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
        if (h - 2 * r > 0) ctx.method_25294(x, y + r, x + w, y + h - r, color);
    }

    private float step(Map<Module, Float> map, Module key, float target, float k) {
        float cur = map.getOrDefault(key, 0f);
        cur += (target - cur) * k;
        if (Math.abs(target - cur) < 0.002f) cur = target;
        map.put(key, cur);
        return cur;
    }

    private List<Module> visibleModules(Category c) {
        List<Module> out = new ArrayList<>();
        String q = search.trim().toLowerCase();
        for (Module m : mm.byCategory(c)) {
            if (q.isEmpty() || m.name.toLowerCase().contains(q)) out.add(m);
        }
        return out;
    }

    private float settingHeight(Setting s) {
        if (s instanceof NumberSetting) return SNUM;
        if (s instanceof EnumSetting e) return SROW + SOPT * e.options.length * ease(dropAnim.getOrDefault(e, 0f));
        return SROW;
    }

    private float settingsHeight(Module m) {
        float h = 3;
        if (m.toggleable) h += SROW;
        for (Setting s : m.settings) h += settingHeight(s);
        return h;
    }

    private float contentHeight(List<Module> mods) {
        float h = 3;
        for (Module m : mods) {
            h += ROW + 1 + settingsHeight(m) * ease(expand.getOrDefault(m, 0f));
        }
        return h + 3;
    }

    private float bodyHeight(Panel p, List<Module> mods) {
        float maxH = Math.max(90, field_22790 - p.y - HEAD - 12);
        return Math.min(contentHeight(mods), maxH);
    }

    /** Walks every visible row of a panel top to bottom (same geometry for drawing and for hit testing). */
    private void walk(Panel p, List<Module> mods, Visitor v) {
        float y = p.y + HEAD + 3 - p.scroll;
        float x = p.x + 4, w = PW - 8;
        for (Module m : mods) {
            if (v.visit(new Row(Kind.MODULE, m, null, 0, x, y, w, ROW, y, y + ROW))) return;
            y += ROW;
            float a = ease(expand.getOrDefault(m, 0f));
            if (a > 0.002f) {
                float full = settingsHeight(m);
                float top = y, bottom = y + full * a;
                float cy = y + 2;
                if (m.toggleable) {
                    if (v.visit(new Row(Kind.KEYBIND, m, null, 0, x + 4, cy, w - 8, SROW, top, bottom))) return;
                    cy += SROW;
                }
                for (Setting s : m.settings) {
                    Kind k = s instanceof BoolSetting ? Kind.BOOL : s instanceof NumberSetting ? Kind.NUMBER
                            : s instanceof EnumSetting ? Kind.ENUM : Kind.BUTTON;
                    float rh = k == Kind.NUMBER ? SNUM : SROW;
                    if (v.visit(new Row(k, m, s, 0, x + 4, cy, w - 8, rh, top, bottom))) return;
                    if (s instanceof EnumSetting es) {
                        float da = ease(dropAnim.getOrDefault(es, 0f));
                        if (da > 0.002f) {
                            float oTop = Math.max(top, cy + SROW);
                            float oBottom = Math.min(bottom, cy + SROW + da * SOPT * es.options.length);
                            for (int i = 0; i < es.options.length; i++) {
                                if (v.visit(new Row(Kind.ENUM_OPTION, m, es, i, x + 8, cy + SROW + i * SOPT, w - 16, SOPT, oTop, oBottom))) return;
                            }
                        }
                    }
                    cy += settingHeight(s);
                }
                y += full * a;
            }
            y += 1;
        }
    }

    private void toast(String msg) {
        toast = msg;
        toastUntil = System.currentTimeMillis() + 2500;
    }

    // ================================================================== rendering

    @Override
    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        float k = Math.min(1f, dt * cfg.animSpeed.getInt());
        openAnim = Math.min(1f, openAnim + dt * 6f);

        class_327 tr = field_22787.field_1772;
        float panelAlpha = ease(openAnim);

        // Draw our own translucent overlay; do not invoke Screen.renderBackground() or
        // super.render(), which can request a second blur pass with other screen mods.
        ctx.method_25294(0, 0, field_22789, field_22790, alpha(0xFFFCF7EA, 0.26f * ease(openAnim)));

        // animations
        for (Module m : mm.all()) {
            step(expand, m, expanded.contains(m) ? 1f : 0f, k);
            step(toggleAnim, m, m.isEnabled() ? 1f : 0f, k);
        }
        for (Module m : mm.all()) {
            for (Setting s : m.settings) {
                if (s instanceof EnumSetting e) {
                    float cur = dropAnim.getOrDefault(e, 0f);
                    float target = openDrops.contains(e) ? 1f : 0f;
                    cur += (target - cur) * k;
                    if (Math.abs(target - cur) < 0.002f) cur = target;
                    dropAnim.put(e, cur);
                }
            }
        }

        drawBranding(ctx, tr, mouseX, mouseY, ease(openAnim));
        // Navigation sidebar: selecting a category replaces the old floating windows.
        roundRect(ctx, 12, 70, 156, Math.max(230, Category.values().length * 41 + 68), 7, 0xFFF9F7EE);
        ctx.method_25294(12, 70, 168, 72, ACCENT);
        int ci = 0;
        for (Category cat : Category.values()) {
            int cy = 98 + ci * 41;
            boolean active = cat == selectedCategory;
            roundRect(ctx, 22, cy, 136, 34, 5, active ? 0xFFEBD9B1 : 0xFFF8F3E8);
            ctx.method_25294(22, cy, 24, cy + 34, active ? ACCENT : 0xFFD2BC89);
            ctx.method_51433(tr, cat.label, 34, cy + 12, active ? 0xFF966C15 : TEXT, true);
            ci++;
        }

        Object newHoverKey = null;
        String newHoverText = null;

        for (Panel p : panels) {
            if (p.cat != selectedCategory) continue;
            p.x = 180; p.y = 70;
            List<Module> mods = visibleModules(p.cat);
            if (!search.isBlank() && mods.isEmpty()) continue;

            clampPanel(p);
            float bodyH = bodyHeight(p, mods);
            float maxScroll = Math.max(0, contentHeight(mods) - bodyH);
            p.scroll = class_3532.method_15363(p.scroll, 0, maxScroll);

            float top = p.y, bodyTop = p.y + HEAD, bodyBottom = p.y + HEAD + bodyH;

            // window
            roundRect(ctx, p.x - 1, top - 1, PW + 2, HEAD + bodyH + 5, 6, alpha(0xFFD2A136, 0.35f * panelAlpha));
            roundRect(ctx, p.x, top, PW, HEAD + bodyH + 3, 5, alpha(PANEL_BG, panelAlpha));
            roundRect(ctx, p.x, top, PW, HEAD, 5, alpha(ACCENT_DARK, panelAlpha));
            ctx.method_25294((int) p.x + 2, (int) (top + HEAD - 1), (int) (p.x + PW - 2), (int) (top + HEAD), ACCENT);
            ctx.method_51433(tr, p.cat.label, (int) (p.x + 8), (int) (top + 8), 0xFF69501D, true);
            String count = String.valueOf(mods.size());
            ctx.method_51433(tr, count, (int) (p.x + PW - 8 - tr.method_1727(count)), (int) (top + 8), TEXT_DIM, true);

            final float fBodyTop = bodyTop, fBodyBottom = bodyBottom, fAlpha = panelAlpha;
            final Object[] hk = {null};
            final String[] ht = {null};
            walk(p, mods, r -> {
                float cT = Math.max(fBodyTop, r.clipTop), cB = Math.min(fBodyBottom, r.clipBottom);
                if (cB <= cT || r.y + r.h < cT || r.y > cB) return false;
                ctx.method_44379((int) p.x, (int) cT, (int) (p.x + PW), (int) cB);
                boolean hovered = mouseX >= r.x && mouseX < r.x + r.w && mouseY >= Math.max(cT, r.y) && mouseY < Math.min(cB, r.y + r.h)
                        && topPanelAt(mouseX, mouseY) == p;
                drawRow(ctx, tr, r, hovered, fAlpha, mouseX);
                ctx.method_44380();
                if (hovered) {
                    hk[0] = r.s != null ? r.s : r.m;
                    ht[0] = tooltipFor(r);
                }
                return false;
            });
            if (hk[0] != null) {
                newHoverKey = hk[0];
                newHoverText = ht[0];
            }

            // scrollbar
            if (maxScroll > 0) {
                float trackH = bodyH - 4;
                float thumbH = Math.max(14, trackH * (bodyH / contentHeight(mods)));
                float thumbY = bodyTop + 2 + (trackH - thumbH) * (p.scroll / maxScroll);
                ctx.method_25294((int) (p.x + PW - 3), (int) thumbY, (int) (p.x + PW - 1), (int) (thumbY + thumbH), alpha(0xFFD2A136, 0.7f));
            }
        }

        // tooltip
        if (newHoverKey != hoverKey) {
            hoverKey = newHoverKey;
            hoverSince = System.currentTimeMillis();
        }
        hoverText = newHoverText;
        if (cfg.tooltips.get() && hoverText != null && System.currentTimeMillis() - hoverSince > 350 && binding == null) {
            drawTooltip(ctx, tr, hoverText, mouseX, mouseY);
        }

        // toast
        if (toast != null && System.currentTimeMillis() < toastUntil) {
            int tw = tr.method_1727(toast) + 16;
            int tx = (field_22789 - tw) / 2, ty = field_22790 - 34;
            roundRect(ctx, tx, ty, tw, 18, 4, alpha(ACCENT_DARK, 0.95f));
            ctx.method_51433(tr, toast, tx + 8, ty + 5, TEXT, true);
        }
    }

    private void drawBranding(class_332 ctx, class_327 tr, int mouseX, int mouseY, float a) {
        String title = "CAELESTIAL CLIENT";
        var m = ctx.method_51448();
        m.pushMatrix();
        m.translate(180f, 9f);
        m.scale(2f, 2f);
        ctx.method_51433(tr, title, -tr.method_1727(title) / 2, 0, alpha(0xFFB38120, a), true);
        m.popMatrix();
        String sub = "made by you5died  |  v" + CaelestisClient.VERSION + "  |  Right Shift closes";
        ctx.method_51433(tr, sub, 182, 32, alpha(0xFF9B751D, a), true);

        // search bar
        int sw = 210, sx = Math.max(390, field_22789 - sw - 24), sy = 22;
        roundRect(ctx, sx - 1, sy - 1, sw + 2, 18, 5, searchFocused ? ACCENT : alpha(0xFFD2A136, 0.35f));
        roundRect(ctx, sx, sy, sw, 16, 4, 0xF0FDF9ED);
        String shown = search.isEmpty() && !searchFocused ? "Search modules..." : search + (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
        ctx.method_51433(tr, shown, sx + 6, sy + 4, search.isEmpty() ? TEXT_DIM : TEXT, true);
    }

    private void drawRow(class_332 ctx, class_327 tr, Row r, boolean hovered, float pa, int mouseX) {
        int x = (int) r.x, y = (int) r.y, w = (int) r.w, h = (int) r.h;
        switch (r.kind) {
            case MODULE -> {
                float t = toggleAnim.getOrDefault(r.m, 0f);
                int base = mix(alpha(ROW_BG, pa), alpha(ACCENT_DARK, pa), t);
                if (hovered) base = mix(base, 0xFFF5E7C5, 0.35f);
                roundRect(ctx, x, y, w, h, 5, 0xFFD6AC52);
                roundRect(ctx, x + 1, y + 1, w - 2, h - 2, 5, hovered ? 0xFFFFF9EB : 0xFFFDFBF5);
                if (t > 0.02f) ctx.method_25294(x, y + 3, x + 2, y + h - 3, alpha(0xFFD2A136, t));
                ctx.method_51433(tr, r.m.name, x + 13, y + 12, mix(0xFF75654B, 0xFF71501B, t), true);
                if (r.m.toggleable) {
                    drawPill(ctx, tr, x + w - 46, y + 12, 37, 14, t);
                } else {
                    String chev = expanded.contains(r.m) ? "v" : ">";
                    ctx.method_51433(tr, chev, x + w - 17, y + 12, TEXT_DIM, true);
                }
            }
            case KEYBIND -> {
                boolean active = binding == r.m;
                roundRect(ctx, x, y + 1, w, h - 2, 3, active ? alpha(0xFFD3AB4A, 0.55f) : (hovered ? 0xFFEDE1C9 : 0xFFF4EFDF));
                String s = active ? "Press any key..." : "Keybind: " + KeyNames.name(r.m.keybind);
                ctx.method_51433(tr, s, x + 5, y + 4, active ? 0xFFFFFFFF : (r.m.keybind < 0 ? TEXT_DIM : 0xFFAB7C16), true);
            }
            case BOOL -> {
                BoolSetting b = (BoolSetting) r.s;
                roundRect(ctx, x, y + 1, w, h - 2, 3, hovered ? 0xFFEDE1C9 : 0xFFF4EFDF);
                ctx.method_51433(tr, r.s.name, x + 5, y + 4, TEXT, true);
                drawPill(ctx, tr, x + w - 27, y + 3, 23, 11, b.get() ? 1f : 0f);
            }
            case NUMBER -> {
                NumberSetting n = (NumberSetting) r.s;
                roundRect(ctx, x, y + 1, w, h - 2, 3, hovered || draggingSlider == n ? 0xFFEDE1C9 : 0xFFF4EFDF);
                ctx.method_51433(tr, r.s.name, x + 5, y + 3, TEXT, true);
                String v = n.display();
                ctx.method_51433(tr, v, x + w - 5 - tr.method_1727(v), y + 3, 0xFFAB7C16, true);
                int tx = x + 5, tw = w - 10, ty = y + h - 8;
                ctx.method_25294(tx, ty, tx + tw, ty + 3, 0xFFE2D3B6);
                int fw = (int) (tw * class_3532.method_15363((float) n.fraction(), 0f, 1f));
                ctx.method_25294(tx, ty, tx + fw, ty + 3, ACCENT);
                ctx.method_25294(tx + fw - 1, ty - 1, tx + fw + 2, ty + 4, 0xFFFFFFFF);
            }
            case ENUM -> {
                EnumSetting e = (EnumSetting) r.s;
                roundRect(ctx, x, y + 1, w, h - 2, 3, hovered ? 0xFFEDE1C9 : 0xFFF4EFDF);
                ctx.method_51433(tr, r.s.name, x + 5, y + 4, TEXT, true);
                String v = e.get() + (openDrops.contains(e) ? " ^" : " v");
                ctx.method_51433(tr, v, x + w - 5 - tr.method_1727(v), y + 4, 0xFFAB7C16, true);
            }
            case ENUM_OPTION -> {
                EnumSetting e = (EnumSetting) r.s;
                boolean sel = e.index() == r.opt;
                roundRect(ctx, x, y, w, h - 1, 2, sel ? alpha(0xFFD3AB4A, 0.6f) : (hovered ? 0xFFE7D8BA : 0xFFF9F5E9));
                ctx.method_51433(tr, e.options[r.opt], x + 6, y + 3, sel ? 0xFFFFFFFF : TEXT_DIM, true);
            }
            case BUTTON -> {
                roundRect(ctx, x, y + 1, w, h - 2, 3, hovered ? alpha(0xFFD3AB4A, 0.7f) : alpha(ACCENT_DARK, 0.9f));
                ctx.method_51433(tr, r.s.name, x + (w - tr.method_1727(r.s.name)) / 2, y + 4, TEXT, true);
            }
        }
    }

    /** Clear ON / OFF indicator. */
    private void drawPill(class_332 ctx, class_327 tr, int x, int y, int w, int h, float t) {
        roundRect(ctx, x, y, w, h, 4, mix(OFF_COL, ACCENT_DIM, t));
        String s = t > 0.5f ? "ON" : "OFF";
        ctx.method_51433(tr, s, x + (w - tr.method_1727(s)) / 2, y + 2, t > 0.5f ? 0xFF5F4419 : 0xFF796C57, false);
    }

    private String tooltipFor(Row r) {
        switch (r.kind) {
            case MODULE:
                return r.m.description + (r.m.toggleable ? "\nLeft click: toggle   Right click: settings" : "\nClick to open settings");
            case KEYBIND:
                return "Click, then press any key to bind.\nEsc clears the bind. Right click also clears.\nRight Shift is reserved for the GUI.";
            case ENUM_OPTION:
                return null;
            default:
                return r.s == null ? null : r.s.tooltip;
        }
    }

    private void drawTooltip(class_332 ctx, class_327 tr, String text, int mx, int my) {
        List<String> lines = new ArrayList<>();
        for (String para : text.split("\n")) {
            StringBuilder cur = new StringBuilder();
            for (String word : para.split(" ")) {
                if (tr.method_1727(cur + " " + word) > 190 && cur.length() > 0) {
                    lines.add(cur.toString());
                    cur = new StringBuilder();
                }
                if (cur.length() > 0) cur.append(' ');
                cur.append(word);
            }
            lines.add(cur.toString());
        }
        int w = 0;
        for (String l : lines) w = Math.max(w, tr.method_1727(l));
        int h = lines.size() * 10 + 6;
        int x = Math.min(mx + 10, field_22789 - w - 12), y = Math.min(my + 12, field_22790 - h - 4);
        roundRect(ctx, x - 1, y - 1, w + 12, h + 2, 4, alpha(0xFFD2A136, 0.9f));
        roundRect(ctx, x, y, w + 10, h, 3, 0xF0FBF8EE);
        int ly = y + 4;
        for (String l : lines) {
            ctx.method_51433(tr, l, x + 5, ly, TEXT, false);
            ly += 10;
        }
    }

    private void clampPanel(Panel p) {
        p.x = class_3532.method_15363(p.x, 0, Math.max(0, field_22789 - PW));
        p.y = class_3532.method_15363(p.y, 0, Math.max(0, field_22790 - HEAD - 10));
    }

    private Panel topPanelAt(double mx, double my) {
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel p = panels.get(i);
            if (p.cat != selectedCategory) continue;
            List<Module> mods = visibleModules(p.cat);
            if (!search.isBlank() && mods.isEmpty()) continue;
            float h = HEAD + bodyHeight(p, mods) + 3;
            if (mx >= p.x && mx < p.x + PW && my >= p.y && my < p.y + h) return p;
        }
        return null;
    }

    // ================================================================== input

    @Override
    public boolean method_25402(class_11909 click, boolean doubled) {
        double mx = click.comp_4798(), my = click.comp_4799();
        int button = click.method_74245();

        if (binding != null) { // clicking anywhere cancels key assignment
            binding = null;
            return true;
        }

        int sw = 210, sx = Math.max(390, field_22789 - sw - 24), sy = 22;
        searchFocused = mx >= sx && mx < sx + sw && my >= sy && my < sy + 16;
        if (searchFocused) return true;

        if (button == 0 && mx >= 22 && mx < 158) {
            int idx = (int) ((my - 98) / 41);
            if (my >= 98 && idx >= 0 && idx < Category.values().length && my < 98 + idx * 41 + 34) {
                selectedCategory = Category.values()[idx];
                return true;
            }
        }
        Panel p = topPanelAt(mx, my);
        if (p == null) return super.method_25402(click, doubled);

        panels.remove(p);
        panels.add(p); // bring to front

        if (my < p.y + HEAD) return true;

        List<Module> mods = visibleModules(p.cat);
        float bodyTop = p.y + HEAD, bodyBottom = p.y + HEAD + bodyHeight(p, mods);
        final Row[] hit = {null};
        walk(p, mods, r -> {
            float cT = Math.max(bodyTop, r.clipTop), cB = Math.min(bodyBottom, r.clipBottom);
            if (cB <= cT) return false;
            if (mx >= r.x && mx < r.x + r.w && my >= Math.max(cT, r.y) && my < Math.min(cB, r.y + r.h)) {
                hit[0] = r;
                return true;
            }
            return false;
        });
        if (hit[0] == null) return true;
        handleRowClick(hit[0], button, mx);
        return true;
    }

    private void handleRowClick(Row r, int button, double mx) {
        switch (r.kind) {
            case MODULE -> {
                if (button == 0 && r.m.toggleable) {
                    r.m.toggle();
                } else if (button == 1 || (button == 0 && !r.m.toggleable)) {
                    if (!expanded.remove(r.m)) expanded.add(r.m);
                }
            }
            case KEYBIND -> {
                if (button == 0) {
                    binding = r.m;
                } else if (button == 1) {
                    mm.assignKey(r.m, -1);
                    toast(r.m.name + " keybind cleared");
                    ConfigManager.save();
                }
            }
            case BOOL -> {
                if (button == 0) ((BoolSetting) r.s).toggle();
            }
            case NUMBER -> {
                if (button == 0) {
                    draggingSlider = (NumberSetting) r.s;
                    sliderX = r.x + 5;
                    sliderW = r.w - 10;
                    draggingSlider.setFraction((mx - sliderX) / sliderW);
                }
            }
            case ENUM -> {
                EnumSetting e = (EnumSetting) r.s;
                if (button == 0) {
                    if (!openDrops.remove(e)) openDrops.add(e);
                } else if (button == 1) {
                    e.setIndex((e.index() + 1) % e.options.length);
                }
            }
            case ENUM_OPTION -> {
                EnumSetting e = (EnumSetting) r.s;
                if (button == 0) {
                    e.setIndex(r.opt);
                    openDrops.remove(e);
                }
            }
            case BUTTON -> {
                if (button == 0) ((ButtonSetting) r.s).run();
            }
        }
    }

    @Override
    public boolean method_25403(class_11909 click, double offsetX, double offsetY) {
        if (draggingSlider != null) {
            draggingSlider.setFraction((click.comp_4798() - sliderX) / sliderW);
            return true;
        }
        if (draggingPanel != null) {
            draggingPanel.x = (float) click.comp_4798() - dragDX;
            draggingPanel.y = (float) click.comp_4799() - dragDY;
            clampPanel(draggingPanel);
            return true;
        }
        return super.method_25403(click, offsetX, offsetY);
    }

    @Override
    public boolean method_25406(class_11909 click) {
        boolean was = draggingSlider != null || draggingPanel != null;
        draggingSlider = null;
        draggingPanel = null;
        if (was) {
            for (Panel p : panels) ConfigManager.PANEL_POS.put(p.cat.name(), new float[]{p.x, p.y});
            ConfigManager.save();
            return true;
        }
        return super.method_25406(click);
    }

    @Override
    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        Panel p = topPanelAt(mouseX, mouseY);
        if (p != null) {
            p.scroll -= (float) verticalAmount * 18f;
            return true;
        }
        return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean method_25404(class_11908 input) {
        int key = input.comp_4795();

        if (binding != null) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                mm.assignKey(binding, -1); // Escape while assigning clears the bind
                toast(binding.name + " keybind cleared");
                binding = null;
                ConfigManager.save();
                return true;
            }
            if (key == ModuleManager.GUI_KEY) {
                toast("Right Shift is reserved for the ClickGUI - pick another key");
                return true; // stay in binding mode
            }
            String msg = mm.assignKey(binding, key);
            toast(msg != null ? msg : binding.name + " bound to " + KeyNames.name(key));
            binding = null;
            ConfigManager.save();
            return true;
        }

        if (searchFocused) {
            if (key == GLFW.GLFW_KEY_BACKSPACE) {
                if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                if (key == GLFW.GLFW_KEY_ESCAPE) search = "";
                return true;
            }
            if (key == ModuleManager.GUI_KEY) {
                method_25419();
                return true;
            }
            return true; // swallow other keys while typing
        }

        if (key == ModuleManager.GUI_KEY) {
            method_25419();
            return true;
        }
        return super.method_25404(input);
    }

    @Override
    public boolean method_25400(class_11905 input) {
        if (searchFocused && search.length() < 24) {
            int cp = input.comp_4793();
            if (cp >= 32 && cp != 127) {
                search += new String(Character.toChars(cp));
                return true;
            }
        }
        return super.method_25400(input);
    }
}
