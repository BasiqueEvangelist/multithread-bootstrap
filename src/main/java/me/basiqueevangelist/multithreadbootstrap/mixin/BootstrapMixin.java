package me.basiqueevangelist.multithreadbootstrap.mixin;

import me.basiqueevangelist.multithreadbootstrap.MultithreadBootstrap;
import me.basiqueevangelist.multithreadbootstrap.MultithreadUtil;
import net.minecraft.core.Registry;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(Bootstrap.class)
public class BootstrapMixin {
    @Unique private static final Logger mtbootstrap$LOGGER = LogManager.getLogger("MultithreadBootstrap/BootstrapMixin");
    @Unique private static long mtbootstrap$bootstrapTime = 0;

    @Inject(method = "bootStrap", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Registry;keySet()Ljava/util/Set;", shift = At.Shift.AFTER))
    private static void bootstrapRegistries(CallbackInfo ci) {
        if (!MultithreadBootstrap.ENABLED) return;

        var _unused = Blocks.ACACIA_LOG;
        var _unused2 = Items.ACACIA_LOG;
        MultithreadUtil.waitForAll();
        for (Block block : Registry.BLOCK) {
            for (BlockState blockState2 : block.getStateDefinition().getPossibleStates()) {
                Block.BLOCK_STATE_REGISTRY.add(blockState2);
            }
            block.getLootTable();
        }

        try {
            for (Field field : Blocks.class.getFields()) {
                Block block = (Block)field.get(null);
                for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                    if (Block.BLOCK_STATE_REGISTRY.getId(state) == -1) {
                        System.out.printf("%s is not registered after init!%n", state.toString());

                    }
                }
            }
        } catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }

        try {
            for (Field field : Items.class.getFields()) {
                Item item = (Item)field.get(null);
                if (item == null)
                    System.out.printf("%s is null after init!%n", field.getName());
                else if (Registry.ITEM.get(Registry.ITEM.getKey(item)) != item)
                    System.out.printf("%s is not registered after init!%n", item.toString());
            }
        } catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }

        RegistryAccessor.getLOADERS().forEach((resourceLocation, supplier) -> {
            mtbootstrap$LOGGER.info("Initializing {}", resourceLocation);
            var initTime = System.nanoTime();
            if (supplier.get() == null) {
                mtbootstrap$LOGGER.error("Unable to bootstrap registry '{}'", resourceLocation);
            }
            mtbootstrap$LOGGER.info("Initialized {} in {}", resourceLocation, (System.nanoTime() - initTime) / 1000000000.0);
        });
        BuiltinRegistriesAccessor.getLOADERS().forEach((resourceLocation, supplier) -> {
            mtbootstrap$LOGGER.info("Initializing {}", resourceLocation);
            var initTime = System.nanoTime();
            if (supplier.get() == null) {
                mtbootstrap$LOGGER.error("Unable to bootstrap registry '{}'", resourceLocation);
            }
            mtbootstrap$LOGGER.info("Initialized {} in {}", resourceLocation, (System.nanoTime() - initTime) / 1000000000.0);
        });
    }

    @Inject(method = "bootStrap", at = @At("HEAD"))
    private static void startTime(CallbackInfo ci) {
        mtbootstrap$bootstrapTime = System.nanoTime();
    }

    @Inject(method = "bootStrap", at = @At("RETURN"))
    private static void endTime(CallbackInfo ci) {
        long total = System.nanoTime() - mtbootstrap$bootstrapTime;
        mtbootstrap$LOGGER.info("Bootstrap took {} seconds", total / 1000000000.0);
    }
}
