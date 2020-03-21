package minegame159.meteorclient.json;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.Setting;

import java.lang.reflect.Type;
import java.util.List;

public class ModuleSerializer implements JsonSerializer<Module> {
    @Override
    public JsonElement serialize(Module src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject o = new JsonObject();

        o.addProperty("name", src.name);
        if (!src.setting) {
            o.addProperty("active", src.isActive());
            o.addProperty("visible", src.isVisible());
            o.addProperty("key", src.getKey());
        }

        o.add("settings", context.serialize(src.settings, new TypeToken<List<Setting>>() {}.getType()));

        return o;
    }

    public static void deserialize(Module module, JsonObject json, JsonDeserializationContext context) {
        if (!module.setting) {
            boolean active = json.get("active").getAsBoolean();
            if (module.isActive() != active) module.toggle(false);

            module.setVisible(json.get("visible").getAsBoolean());
            module.setKey(json.get("key").getAsInt(), false);
        }

        for (JsonElement e : json.get("settings").getAsJsonArray()) {
            JsonObject o = e.getAsJsonObject();
            Setting<?> setting = module.getSetting(o.get("name").getAsString());
            if (setting == null) continue;
            SettingSerializer.deserialize(setting, o, context);
        }
    }
}
