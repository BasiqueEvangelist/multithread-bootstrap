package me.basiqueevangelist.multithreadbootstrap.mixin;

import me.basiqueevangelist.multithreadbootstrap.MultithreadBootstrap;
import net.minecraft.core.Registry;
import net.minecraft.core.WritableRegistry;
import net.minecraft.data.BuiltinRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.function.BiConsumer;

@Mixin(Registry.class)
public class RegistryMixin {
    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Ljava/util/Map;forEach(Ljava/util/function/BiConsumer;)V"))
    private static void dontBootstrapRegistries(Map<Object, Object> instance, BiConsumer<Object, Object> v) {
        if (!MultithreadBootstrap.ENABLED)
            instance.forEach(v);
    }

    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Registry;checkRegistry(Lnet/minecraft/core/WritableRegistry;)V"))
    private static void dontCheckRegistries(WritableRegistry<? extends WritableRegistry<?>> writableRegistry) {
        if (!MultithreadBootstrap.ENABLED)
            Registry.checkRegistry(writableRegistry);
    }
}
