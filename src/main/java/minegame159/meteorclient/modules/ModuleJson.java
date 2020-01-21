package minegame159.meteorclient.modules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import minegame159.meteorclient.settings.*;
import minegame159.meteorclient.utils.Color;

public class ModuleJson {
    public static JsonObject saveModule(Module module) {
        JsonObject obj = new JsonObject();

        obj.addProperty("name", module.name);
        obj.addProperty("active", module.isActive());
        if (module.key != -1) obj.addProperty("key", module.key);

        JsonArray settings = new JsonArray();
        for (Setting setting : module.settings) settings.add(saveSetting(setting));
        obj.add("settings", settings);

        return obj;
    }

    private static JsonObject saveSetting(Setting setting) {
        JsonObject obj = new JsonObject();

        obj.addProperty("name", setting.name);

        if (setting instanceof BoolSetting) obj.addProperty("value", (boolean) setting.value);
        else if (setting instanceof IntSetting) obj.addProperty("value", (int) setting.value);
        else if (setting instanceof FloatSetting) obj.addProperty("value", (float) setting.value);
        else if (setting instanceof DoubleSetting) obj.addProperty("value", (double) setting.value);
        else if (setting instanceof ColorSetting) obj.add("value", saveColor((Color) setting.value));
        else if (setting instanceof EnumSetting) obj.addProperty("value", setting.value.toString());

        return obj;
    }

    private static JsonObject saveColor(Color color) {
        JsonObject obj = new JsonObject();

        obj.addProperty("r", color.r);
        obj.addProperty("g", color.g);
        obj.addProperty("b", color.b);
        obj.addProperty("a", color.a);

        return obj;
    }

    public static void loadModule(JsonObject obj) {
        Module module = ModuleManager.get(obj.get("name").getAsString());
        if (module == null) return;

        if (obj.has("key")) module.key = obj.get("key").getAsInt();

        for (JsonElement sE : obj.get("settings").getAsJsonArray()) {
            JsonObject sO = sE.getAsJsonObject();
            String name = sO.get("name").getAsString();

            for (Setting setting : module.settings) {
                if (!setting.name.equalsIgnoreCase(name)) continue;

                if (setting instanceof BoolSetting) setting.value = sO.get("value").getAsBoolean();
                else if (setting instanceof IntSetting) setting.value = sO.get("value").getAsInt();
                else if (setting instanceof FloatSetting) setting.value = sO.get("value").getAsFloat();
                else if (setting instanceof DoubleSetting) setting.value = sO.get("value").getAsDouble();
                else if (setting instanceof ColorSetting) setting.value = loadColor(sO.get("value").getAsJsonObject());
                else if (setting instanceof EnumSetting) setting.value = sO.get("value").getAsString();

                break;
            }
        }

        if (obj.get("active").getAsBoolean()) module.toggle();
    }

    private static Color loadColor(JsonObject obj) {
        return new Color(
                obj.get("r").getAsInt(),
                obj.get("g").getAsInt(),
                obj.get("b").getAsInt(),
                obj.get("a").getAsInt()
        );
    }
}
