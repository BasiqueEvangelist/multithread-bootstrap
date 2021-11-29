package me.basiqueevangelist.multithreadbootstrap.mixin;

import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import java.util.function.Supplier;

@Mixin(BuiltinRegistries.class)
public interface BuiltinRegistriesAccessor {
    @Accessor
    static Map<ResourceLocation, Supplier<?>> getLOADERS() {
        throw new AssertionError("Mixin not applied!");
    }
}
