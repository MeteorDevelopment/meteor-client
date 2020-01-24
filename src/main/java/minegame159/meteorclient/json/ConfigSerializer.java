package minegame159.meteorclient.json;

import com.google.gson.*;
import minegame159.meteorclient.Config;
import minegame159.meteorclient.modules.Module;
import minegame159.meteorclient.modules.ModuleManager;

import java.lang.reflect.Type;

public class ConfigSerializer implements JsonSerializer<Config>, JsonDeserializer<Config> {
    @Override
    public JsonElement serialize(Config src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();

        obj.addProperty("prefix", src.prefix);

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

        for (JsonElement e : obj.get("modules").getAsJsonArray()) ModuleJson.loadModule(e.getAsJsonObject());

        return config;
    }
}
