package meteordevelopment.meteorclient.mixin;

import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(value = FabricLoaderImpl.class, remap = false)
public class FabricLoaderMixin {

    // ==========================================
    // 在这里添加你要隐藏的 Mod ID (全部小写)
    // ==========================================
    @Unique
    private static final Set<String> HIDDEN_MODS = new HashSet<>(Arrays.asList(
            "meteor-client",
            "baritone",
            "freecam",
            "specppoof"

    ));

    @Inject(method = "getAllMods", at = @At("RETURN"), cancellable = true)
    private void filterModList(CallbackInfoReturnable<Collection<ModContainer>> cir) {
        // 1. 检查调用栈，确认是否是网络线程
        // 这是一个性能开销较大的操作，但只在进服时触发几次，所以没问题
        boolean isNetworking = false;
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (className.contains("net.fabricmc.fabric.impl.networking") ||
                    className.contains("net.fabricmc.fabric.mixin.networking")) {
                isNetworking = true;
                break;
            }
        }

        // 2. 如果不是网络请求，直接放行 (否则游戏本体会读不到Mod而崩溃)
        if (!isNetworking)
            return;

        // 3. 开始过滤
        Collection<ModContainer> originalList = cir.getReturnValue();
        List<ModContainer> filteredList = new ArrayList<>();
        StringJoiner hiddenLog = new StringJoiner(", ");

        for (ModContainer mod : originalList) {
            String modId = mod.getMetadata().getId();

            if (HIDDEN_MODS.contains(modId)) {
                // 命中黑名单，记录并跳过
                hiddenLog.add(modId);
            } else {
                // 白名单，放行
                filteredList.add(mod);
            }
        }

        // 4. 打印日志 (方便你在控制台确认是否生效)
        if (hiddenLog.length() > 0) {
            System.out.println("[Anti-Leak] 已拦截并向服务器隐藏以下 Mod: " + hiddenLog);
        }

        // 5. 替换返回值
        cir.setReturnValue(filteredList);
    }
}