package me.basiqueevangelist.multithreadbootstrap.mixin;

import com.google.common.collect.Maps;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.function.Supplier;

@Mixin(Registry.class)
public interface RegistryAccessor {
    @Accessor
    static Map<ResourceLocation, Supplier<?>> getLOADERS() {
        throw new AssertionError("Mixin not applied!");
    }
}
