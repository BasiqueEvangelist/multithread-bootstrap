package me.basiqueevangelist.multithreadbootstrap.mixin;

import me.basiqueevangelist.multithreadbootstrap.MultithreadBootstrap;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.SpawnEggItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collections;
import java.util.Map;

@Mixin(SpawnEggItem.class)
public class SpawnEggItemMixin {
    @Shadow @Mutable @Final private static Map<EntityType<? extends Mob>, SpawnEggItem> BY_ID;

    static {
        if (MultithreadBootstrap.ENABLED)
            BY_ID = Collections.synchronizedMap(BY_ID);
    }
}
