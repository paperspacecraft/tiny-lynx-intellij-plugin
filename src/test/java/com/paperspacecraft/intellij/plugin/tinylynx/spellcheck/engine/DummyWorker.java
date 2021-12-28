package com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.engine;

import com.intellij.openapi.project.Project;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckResult;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckTask;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.SpellcheckWorker;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class DummyWorker extends SpellcheckWorker {

    private final int id;

    public DummyWorker(Project project, Supplier<SpellcheckTask> taskSupplier, AtomicInteger idGenerator) {
        super(project, taskSupplier);
        this.id = idGenerator.getAndIncrement();
    }

    @Override
    public void run() {
        SpellcheckTask task = getTaskSupplier().get();
        while (task != null) {
            CountDownLatch retarder = new CountDownLatch(1);
            try {
                retarder.await(100, TimeUnit.MILLISECONDS);
                task.complete(new SpellcheckResult(task.getText() + " from Worker " + id));
                reportCompletion();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            task = getTaskSupplier().get();
        }
    }
}
