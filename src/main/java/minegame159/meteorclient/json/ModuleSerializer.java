package minegame159.meteorclient.json;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.settings.Setting;

import java.lang.reflect.Type;
import java.util.List;

public class ModuleSerializer implements JsonSerializer<Module> {
    @Override
    public JsonElement serialize(Module src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject o = new JsonObject();

        o.addProperty("name", src.name);
        o.addProperty("active", src.isActive());
        o.addProperty("visible", src.isVisible());

        o.add("settings", context.serialize(src.settings, new TypeToken<List<Setting>>() {}.getType()));

        return o;
    }

    public static void deserialize(Module module, JsonObject json, JsonDeserializationContext context) {
        boolean active = json.get("active").getAsBoolean();
        if (module.isActive() != active) module.toggle();

        module.setVisible(json.get("visible").getAsBoolean());

        for (JsonElement e : json.get("settings").getAsJsonArray()) {
            JsonObject o = e.getAsJsonObject();
            SettingSerializer.deserialize(module.getSetting(o.get("name").getAsString()), o, context);
        }
    }
}
