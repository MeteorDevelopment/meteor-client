package minegame159.meteorclient.json;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.settings.Setting;

import java.lang.reflect.Type;
import java.util.List;

public class ModuleSerializer {
    public static JsonElement serialize(Module module, JsonSerializationContext context) {
        JsonObject o = new JsonObject();

        o.addProperty("name", module.name);
        if (!module.setting) {
            o.addProperty("active", module.isActive());
            o.addProperty("visible", module.isVisible());
            o.addProperty("key", module.getKey());
        }

        o.add("settings", context.serialize(module.settings, new TypeToken<List<Setting>>() {}.getType()));

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
