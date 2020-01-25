package minegame159.meteorclient.json;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import minegame159.meteorclient.Config;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;
import minegame159.meteorclient.utils.Vector2;

import java.lang.reflect.Type;
import java.util.Map;

public class ConfigSerializer implements JsonSerializer<Config>, JsonDeserializer<Config> {
    @Override
    public JsonElement serialize(Config src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();

        obj.addProperty("prefix", src.prefix);
        obj.add("guiPositions", context.serialize(src.guiPositions));

        JsonArray modules = new JsonArray();
        for (Module module : ModuleManager.getAll()) modules.add(ModuleJson.saveModule(module));
        obj.add("modules", modules);

        return obj;
    }

    @Override
    public Config deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        Config config = new Config();

        config.prefix = obj.get("prefix").getAsString();
        JsonElement guiPositionE = obj.get("guiPositions");
        if (guiPositionE != null) config.guiPositions = context.deserialize(guiPositionE, new TypeToken<Map<Category, Vector2>>() {}.getType());

        for (JsonElement e : obj.get("modules").getAsJsonArray()) ModuleJson.loadModule(e.getAsJsonObject());

        return config;
    }
}
