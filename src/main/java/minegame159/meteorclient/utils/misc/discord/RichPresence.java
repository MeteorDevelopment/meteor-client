package minegame159.meteorclient.utils.misc.discord;

import com.google.gson.JsonObject;

public class RichPresence {
    public long startTimestamp;
    public String largeImage, largeText;
    public String smallImage, smallText;
    public String details, state;

    public JsonObject getJson() {
        JsonObject o = new JsonObject();
        JsonObject assets = new JsonObject();

        if (startTimestamp != 0) {
            JsonObject t = new JsonObject();
            t.addProperty("start", startTimestamp);
            o.add("timestamps", t);
        }

        if (largeImage != null) {
            assets.addProperty("large_image", largeImage);
            if (largeText != null) assets.addProperty("large_text", largeText);
        }

        if (smallImage != null) {
            assets.addProperty("small_image", smallImage);
            if (smallText != null) assets.addProperty("small_text", smallText);
        }

        if (assets.has("large_image")) o.add("assets", assets);

        if (details != null) o.addProperty("details", details);
        if (state != null) o.addProperty("state", state);

        return o;
    }
}
