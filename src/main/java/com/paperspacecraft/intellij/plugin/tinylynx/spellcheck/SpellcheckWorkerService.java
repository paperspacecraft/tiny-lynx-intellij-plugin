package com.paperspacecraft.intellij.plugin.tinylynx.spellcheck;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.paperspacecraft.intellij.plugin.tinylynx.spellcheck.engine.grammarly.GrammarlyWorker;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@Service
public final class SpellcheckWorkerService {

    static final int PARALLEL_THREADS_COUNT = 5;

    private static final String TASK_NAME = "Tiny Lynx Proofreading";

    private final ExecutorService parallelExecutor = Executors.newFixedThreadPool(PARALLEL_THREADS_COUNT);

    private final MultipleTaskSupplier multipleTaskSupplier = new MultipleTaskSupplier();

    private final Project project;

    @Setter(value = AccessLevel.PACKAGE)
    private BiFunction<Project, Supplier<SpellcheckTask>, SpellcheckWorker> workerFactory = GrammarlyWorker::new;

    public SpellcheckWorkerService(Project project) {
        this.project = project;
    }

    SpellcheckTask run(SpellcheckTask.Async task) {
        if (multipleTaskSupplier.addAndCount(task) < PARALLEL_THREADS_COUNT) {
            parallelExecutor.submit(() -> workerFactory.apply(project, multipleTaskSupplier).run());
        }
        return task;
    }

    public SpellcheckTask run(SpellcheckTask.Sync task) {
        if (task.isModal()) {
            Task.Modal modal = new Task.Modal(project, TASK_NAME, false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    runSync(task, indicator, null);
                }
            };
            ProgressManager.getInstance().run(modal);
            return task;
        }

        TaskLock lock = new TaskLock();
        Task.Backgroundable backgroundable = new Task.Backgroundable(project, TASK_NAME, false, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                runSync(task, indicator, lock);
            }
        };
        ProgressManager.getInstance().run(backgroundable);
        lock.await();
        return task;
    }

    private void runSync(SpellcheckTask.Sync task, ProgressIndicator indicator, TaskLock lock) {
        String statusText = "Checking \"" +
                StringUtils.abbreviate(StringUtils.strip(task.getText(), " *\"/\n\r"), 80) +
                "\"";
        indicator.setText(statusText);
        Supplier<SpellcheckTask> supplier = new SingularTaskSupplier(task);
        workerFactory.apply(project, supplier).runAndWait(lock);
    }


    /* ---------------
       Service classes
       --------------- */

    private static class MultipleTaskSupplier implements Supplier<SpellcheckTask> {
        private final ConcurrentLinkedQueue<SpellcheckTask> queue = new ConcurrentLinkedQueue<>();

        public int addAndCount(SpellcheckTask task) {
            queue.add(task);
            return queue.size();
        }

        @Override
        public SpellcheckTask get() {
            return queue.poll();
        }
    }

    @AllArgsConstructor
    private static class SingularTaskSupplier implements Supplier<SpellcheckTask> {
        private SpellcheckTask task;

        @Override
        public SpellcheckTask get() {
            SpellcheckTask valueToReturn = task;
            task = null;
            return valueToReturn;
        }
    }
}
