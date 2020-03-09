package minegame159.meteorclient.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.settings.Setting;

public class ModuleJson {
    public static JsonObject saveModule(Module module) {
        JsonObject obj = new JsonObject();

        obj.addProperty("name", module.name);
        obj.addProperty("active", module.isActive());
        obj.addProperty("visible", module.isVisible());
        if (module.getKey() != -1) obj.addProperty("key", module.getKey());

        JsonArray settings = new JsonArray();
        for (Setting setting : module.settings) settings.add(saveSetting(setting));
        obj.add("settings", settings);

        return obj;
    }

    private static JsonObject saveSetting(Setting setting) {
        JsonObject obj = new JsonObject();

        obj.addProperty("name", setting.name);
        obj.addProperty("value", setting.get().toString());

        return obj;
    }

    public static void loadModule(JsonObject obj) {
        Module module = ModuleManager.get(obj.get("name").getAsString());
        if (module == null) return;

        if (obj.has("visible")) module.setVisible(obj.get("visible").getAsBoolean());
        if (obj.has("key")) module.setKey(obj.get("key").getAsInt());

        for (JsonElement sE : obj.get("settings").getAsJsonArray()) {
            JsonObject sO = sE.getAsJsonObject();
            String name = sO.get("name").getAsString();

            for (Setting setting : module.settings) {
                if (!setting.name.equalsIgnoreCase(name)) continue;

                setting.parse(sO.get("value").getAsString());

                break;
            }
        }

        if (obj.get("active").getAsBoolean()) module.toggle();
    }
}
