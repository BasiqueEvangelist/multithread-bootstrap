package me.basiqueevangelist.multithreadbootstrap.mixin;

import me.basiqueevangelist.multithreadbootstrap.MultithreadBootstrap;
import net.minecraft.data.BuiltinRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.function.BiConsumer;

@Mixin(BuiltinRegistries.class)
public class BuiltinRegistriesMixin {
    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Ljava/util/Map;forEach(Ljava/util/function/BiConsumer;)V"))
    private static void dontBootstrapRegistries(Map<Object, Object> instance, BiConsumer<Object, Object> v) {
        if (!MultithreadBootstrap.ENABLED)
            instance.forEach(v);
    }
}
