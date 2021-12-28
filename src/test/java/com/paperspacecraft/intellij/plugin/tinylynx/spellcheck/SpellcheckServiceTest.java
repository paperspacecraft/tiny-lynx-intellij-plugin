package com.paperspacecraft.intellij.plugin.tinylynx.spellcheck;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.engine.DummyWorker;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SpellcheckServiceTest extends BasePlatformTestCase {

    private SpellcheckService dispatcherService;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        SpellcheckWorkerService workerService;
        workerService = getProject().getService(SpellcheckWorkerService.class);
        AtomicInteger idSupplier = new AtomicInteger();
        workerService.setWorkerFactory((project, taskSupplier) -> new DummyWorker(getProject(), taskSupplier, idSupplier));
        dispatcherService = getProject().getService(SpellcheckService.class);
        dispatcherService.cleanUp();
    }

    public void testTaskSubmission() throws ExecutionException, InterruptedException {
        CompletableFuture<SpellcheckResult> futureResult = dispatcherService.checkAsync("Hello");
        Assert.assertFalse(futureResult.isDone());
        futureResult.get();
        Assert.assertTrue(futureResult.isDone());
        Assert.assertEquals("Hello from Worker 0", futureResult.get().getText());
    }

    public void testSequentialRun() {
        for (int i = 0; i < 3; i++) {
            SpellcheckResult result = dispatcherService.checkSync("Hello");
            Assert.assertEquals("Hello from Worker 0", result.getText());
        }
    }

    public void testParallelRun() {
        List<CompletableFuture<SpellcheckResult>> futureResults =
                IntStream
                        .range(0, SpellcheckWorkerService.PARALLEL_THREADS_COUNT + 2)
                        .mapToObj(i -> dispatcherService.checkAsync("Hello " + i))
                        .collect(Collectors.toList());
        Assert.assertTrue(futureResults.stream().noneMatch(CompletableFuture::isDone));
        List<SpellcheckResult> results = futureResults.stream().map(CompletableFuture::join).collect(Collectors.toList());
        Assert.assertEquals(
                SpellcheckWorkerService.PARALLEL_THREADS_COUNT + 2,
                results.size());
        Assert.assertTrue(results.stream().map(res -> StringUtils.substringAfter(res.getText(), "from")).distinct().count() <= SpellcheckWorkerService.PARALLEL_THREADS_COUNT);
    }

    public void testParallelRunWithDebounce() {
        Object identity = new Object();
        List<CompletableFuture<SpellcheckResult>> futureResults =
                IntStream
                        .range(0, SpellcheckWorkerService.PARALLEL_THREADS_COUNT)
                        .mapToObj(i -> dispatcherService.checkAsync(identity, "Hello " + i))
                        .collect(Collectors.toList());
        Assert.assertEquals(SpellcheckWorkerService.PARALLEL_THREADS_COUNT, futureResults.size());
        List<SpellcheckResult> results = futureResults.stream().map(CompletableFuture::join).collect(Collectors.toList());
        String commonText = results.get(0).getText();
        Assert.assertTrue(results.stream().map(SpellcheckResult::getText).allMatch(text -> text.equals(commonText)));
    }

    public void testLookUp() throws ExecutionException, InterruptedException {
        Assert.assertEquals(SpellcheckResult.EMPTY, dispatcherService.lookUp("Hello"));
        CompletableFuture<SpellcheckResult> result0 = dispatcherService.checkAsync("Hello 0");
        CompletableFuture<SpellcheckResult> result1 = dispatcherService.checkAsync("Hello 1");
        Assert.assertEquals("Hello 0 from Worker 0", result0.get().getText());
        Assert.assertEquals("Hello 1 from Worker 1", result1.get().getText());
        for (int i = 0; i < 3; i++) {
            // "Lookup" call does not trigger a new thread however often called
            Assert.assertEquals("Hello 1 from Worker 1", dispatcherService.lookUp("Hello 1").getText());
        }
    }
}