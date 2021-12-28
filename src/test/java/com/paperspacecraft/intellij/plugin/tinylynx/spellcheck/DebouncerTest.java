package com.paperspacecraft.intellij.plugin.tinylynx.spellcheck;

import junit.framework.TestCase;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DebouncerTest extends TestCase {

    private final Debouncer<Integer> debouncer = new Debouncer<>(100);

    public void testDebounceFromCommonThread() throws ExecutionException, InterruptedException {
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            Object token = new Object();
            CompletableFuture<Integer> future = null;
            int iterations = random.ints(1, 10).findFirst().orElse(0);
            for (int j = 0; j < iterations; j++) {
                final int targetInt = j;
                future = debouncer.debounce(token, () -> targetInt);
            }
            assert future != null;
            Assert.assertEquals(iterations - 1, (int) future.get());
        }
    }

    public void testDebounceFromDifferentThreads() throws ExecutionException, InterruptedException {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Object token = new Object();
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            int numOfThreads = random.ints(1, 10).findFirst().orElse(0);
            List<Future<CompletableFuture<Integer>>> futures = new ArrayList<>();
            for (int j = 0; j < numOfThreads; j++) {
                final int valueToSubmit = j < numOfThreads - 1 ? random.nextInt() : 42;
                futures.add(executorService.submit(() -> debouncer.debounce(token, () -> valueToSubmit)));
            }
            for (int j = 0; j < numOfThreads; j++) {
                Assert.assertEquals(
                        42,
                        (int) futures.get(j).get().get());
            }
        }
    }

    public void testDebounceFuture() throws ExecutionException, InterruptedException {
        Object token = new Object();
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        CompletableFuture<Integer> result = null;
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                final int targetInt = j;
                result = debouncer.debounceFuture(
                        token,
                        () -> CompletableFuture.supplyAsync(() -> targetInt, executorService));
            }
            assert result != null;
            Assert.assertEquals(4, (int) result.get());
        }
    }
}