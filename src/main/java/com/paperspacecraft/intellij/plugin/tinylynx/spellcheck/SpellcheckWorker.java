package com.paperspacecraft.intellij.plugin.tinylynx.spellcheck;

import com.intellij.openapi.project.Project;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.function.Supplier;

public abstract class SpellcheckWorker {

    @Getter(value = AccessLevel.PROTECTED)
    private final Supplier<SpellcheckTask> taskSupplier;

    @Getter(value = AccessLevel.PROTECTED)
    private final Project project;

    private TaskLock lock;

    protected SpellcheckWorker(Project project, Supplier<SpellcheckTask> taskSupplier) {
        this.project = project;
        this.taskSupplier = taskSupplier;
    }

    public abstract void run();

    public void runAndWait() {
        runAndWait(null);
    }

    public void runAndWait(TaskLock externalLock) {
        lock = externalLock != null ? externalLock : new TaskLock();
        run();
    }

    protected void waitForCompletion() {
        if (lock == null) {
            return;
        }
        lock.await();
    }

    protected void reportCompletion() {
        if (lock != null) {
            lock.release();
        }
    }

}
