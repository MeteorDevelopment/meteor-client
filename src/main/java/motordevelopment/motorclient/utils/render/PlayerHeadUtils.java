package motordevelopment.motorclient.utils.render;

import com.google.gson.Gson;
import motordevelopment.motorclient.MotorClient;
import motordevelopment.motorclient.systems.accounts.TexturesJson;
import motordevelopment.motorclient.systems.accounts.UuidToProfileResponse;
import motordevelopment.motorclient.utils.PostInit;
import motordevelopment.motorclient.utils.network.Http;

import java.util.Base64;
import java.util.UUID;

public class PlayerHeadUtils {
    public static PlayerHeadTexture STEVE_HEAD;

    private PlayerHeadUtils() {
    }

    @PostInit
    public static void init() {
        STEVE_HEAD = new PlayerHeadTexture();
    }

    public static PlayerHeadTexture fetchHead(UUID id) {
        if (id == null) return null;

        String url = getSkinUrl(id);
        return url != null ? new PlayerHeadTexture(url) : null;
    }

    public static String getSkinUrl(UUID id) {
        UuidToProfileResponse res2 = Http.get("https://sessionserver.mojang.com/session/minecraft/profile/" + id)
            .exceptionHandler(e -> MotorClient.LOG.error("Could not contact mojang session servers.", e))
            .sendJson(UuidToProfileResponse.class);
        if (res2 == null) return null;

        String base64Textures = res2.getPropertyValue("textures");
        if (base64Textures == null) return null;

        TexturesJson textures = new Gson().fromJson(new String(Base64.getDecoder().decode(base64Textures)), TexturesJson.class);
        if (textures.textures.SKIN == null) return null;

        return textures.textures.SKIN.url;
    }
}
