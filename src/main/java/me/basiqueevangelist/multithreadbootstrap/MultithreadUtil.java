package me.basiqueevangelist.multithreadbootstrap;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public final class MultithreadUtil {
    private MultithreadUtil() {

    }

    private static final HashMap<String, CompletableFuture<Void>> blockFutures = new HashMap<>();

    public static void init() {
        SoundType bsg = SoundType.ANCIENT_DEBRIS;
    }

    public static void runDelayed(String blockName, String dependencies, Runnable runnable) {
        CompletableFuture<Void> future;
        if (dependencies.isEmpty())
            future = CompletableFuture.runAsync(runnable);
        else
            future = CompletableFuture.allOf(
                Arrays.stream(dependencies.split(";"))
                    .filter(x -> x.length() > 0)
                    .map(blockFutures::get)
                    .toArray(CompletableFuture[]::new))
                .thenRunAsync(runnable);
        blockFutures.put(blockName, future);
    }

    public static void waitForAll() {
        var initTime = System.nanoTime();
        try {
            for (Map.Entry<String, CompletableFuture<Void>> futureEntry : blockFutures.entrySet()) {
                futureEntry.getValue().get();
                System.out.printf("%s has finished.%n", futureEntry.getKey());
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        blockFutures.clear();
        System.out.printf("Initialized registries in %f", (System.nanoTime() - initTime) / 1000000000.0);

        try {
            for (Field field : Blocks.class.getFields()) {
                if (field.get(null) == null)
                    System.out.printf("%s is null after init!%n", field.getName());
            }
        } catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
    }
}
