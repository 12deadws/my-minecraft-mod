package dev.cael3stis.client;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public abstract class Module {
    public final String name, category, description;
    public boolean enabled;
    public int key = GLFW.GLFW_KEY_UNKNOWN;
    public final List<Setting> settings = new ArrayList<>();
    protected long nextAction;

    protected Module(String name, String category, String description) {
        this.name=name; this.category=category; this.description=description;
    }
    protected Setting.Toggle toggle(String n, boolean value) { var s = new Setting.Toggle(n,value); settings.add(s); return s; }
    protected Setting.Number number(String n,int value,int min,int max,int step) { var s = new Setting.Number(n,value,min,max,step); settings.add(s); return s; }
    protected Setting.Choice choice(String n,int value,String... opts) { var s = new Setting.Choice(n,value,opts); settings.add(s); return s; }
    public void tick(GameContext c) { }
    public void reset() { nextAction=0; }
    protected boolean ready(long ms) { return System.currentTimeMillis() >= nextAction; }
    protected void delay(int ms) { nextAction=System.currentTimeMillis()+ms; }
    public void setEnabled(boolean enabled) { if(this.enabled!=enabled) { this.enabled=enabled; reset(); Cael3stisClient.save(); } }
    public void toggle() { setEnabled(!enabled); }
    public JsonObject json() {
        var o=new JsonObject(); o.addProperty("enabled",enabled); o.addProperty("key",key);
        var ss=new JsonObject(); for (var s: settings) ss.add(s.name,s.json()); o.add("settings",ss); return o;
    }
    public void read(JsonObject o) {
        if(o.has("enabled")) enabled=o.get("enabled").getAsBoolean();
        if(o.has("key")) { int k=o.get("key").getAsInt(); key=k==GLFW.GLFW_KEY_RIGHT_SHIFT ? GLFW.GLFW_KEY_UNKNOWN : k; }
        if(o.has("settings") && o.get("settings").isJsonObject()) {
            var ss=o.getAsJsonObject("settings"); for(var s:settings) if(ss.has(s.name)) try { s.read(ss.get(s.name)); } catch(RuntimeException ignored) {}
        }
    }
}
