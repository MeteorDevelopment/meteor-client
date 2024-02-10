package me.jellysquid.mods.lithium.common.reflection;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.WeakHashMap;

public class ReflectionUtil {

    public static boolean hasMethodOverride(Class<?> clazz, Class<?> superclass, boolean fallbackResult, String methodName, Class<?>... methodArgs) {
        while (clazz != null && clazz != superclass && superclass.isAssignableFrom(clazz)) {
            try {
                clazz.getDeclaredMethod(methodName, methodArgs);
                return true;
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            } catch (NoClassDefFoundError error) {
                Logger logger = LogManager.getLogger("Lithium Class Analysis");
                logger.warn("Lithium Class Analysis Error: Class " + clazz.getName() + " cannot be analysed, because" +
                        " getting declared methods crashes with NoClassDefFoundError: " + error.getMessage() +
                        ". This is usually caused by modded" +
                        " entities declaring methods that have a return type or parameter type that is annotated" +
                        " with @Environment(value=EnvType.CLIENT). Loading the type is not possible, because" +
                        " it only exists in the CLIENT environment. The recommended fix is to annotate the method with" +
                        " this argument or return type with the same annotation." +
                        " Lithium handles this error by assuming the class cannot be included in some optimizations.");
                return fallbackResult;
            } catch (Throwable e) {
                final String crashedClass = clazz.getName();
                CrashReport crashReport = CrashReport.create(e, "Lithium Class Analysis");
                CrashReportSection crashReportSection = crashReport.addElement(e.getClass().toString() + " when getting declared methods.");
                crashReportSection.add("Analyzed class", crashedClass);
                crashReportSection.add("Analyzed method name", methodName);
                crashReportSection.add("Analyzed method args", methodArgs);

                throw new CrashException(crashReport);
            }
        }
        return false;
    }

    //How to find the remapped methods:
    //1) Run in the debugger: System.out.println(FabricLoader.getInstance().getMappingResolver().getNamespaceData("intermediary").methodNames)
    //2) Ctrl+F for the method name, in this case "onEntityCollision". Make sure to find the correct one.
    private static final String REMAPPED_ON_ENTITY_COLLISION = FabricLoader.getInstance().getMappingResolver().mapMethodName("intermediary", "net.minecraft.class_4970", "method_9548", "(Lnet/minecraft/class_2680;Lnet/minecraft/class_1937;Lnet/minecraft/class_2338;Lnet/minecraft/class_1297;)V");
    private static final WeakHashMap<Class<?>, Boolean> CACHED_IS_ENTITY_TOUCHABLE = new WeakHashMap<>();
    public static boolean isBlockStateEntityTouchable(BlockState operand) {
        Class<? extends Block> blockClazz = operand.getBlock().getClass();
        //Caching results in hashmap as this calculation takes over a second for all blocks together
        Boolean result = CACHED_IS_ENTITY_TOUCHABLE.get(blockClazz);
        if (result != null) {
            return result;
        }
        boolean res = ReflectionUtil.hasMethodOverride(blockClazz, AbstractBlock.class, true, REMAPPED_ON_ENTITY_COLLISION, BlockState.class, World.class, BlockPos.class, Entity.class);
        CACHED_IS_ENTITY_TOUCHABLE.put(blockClazz, res);
        return res;
    }
}
