package com.paperspacecraft.intellij.plugin.tinylynx.spellcheck;

import com.intellij.openapi.diagnostic.Logger;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class SpellcheckTask {
    private static final Logger LOG = Logger.getInstance(SpellcheckTask.class);

    @Getter
    private final String text;

    public abstract boolean isComplete();

    public abstract void complete(SpellcheckResult result);

    public abstract SpellcheckResult getResult();

    public abstract CompletableFuture<SpellcheckResult> getFutureResult();

    abstract void dispose();

    static SpellcheckTask.Async async(String text) {
        return new Async(text);
    }

    static SpellcheckTask.Sync sync(String text) {
        return new Sync(text, false);
    }

    public static SpellcheckTask.Sync syncModal(String text) {
        return new Sync(text, true);
    }

    static class Async extends SpellcheckTask {

        private CompletableFuture<SpellcheckResult> future;

        private Async(String text) {
            super(text);
            future = new CompletableFuture<>();
        }

        @Override
        public boolean isComplete() {
            return future.isDone() || future.isCancelled();
        }

        @Override
        public void complete(SpellcheckResult result) {
            future.complete(result);
        }

        @Override
        public SpellcheckResult getResult() {
            try {
                return future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                LOG.error("Could not complete task", e);
            }
            return SpellcheckResult.EMPTY;
        }

        @Override
        public CompletableFuture<SpellcheckResult> getFutureResult() {
            return future;
        }

        @Override
        public void dispose() {
            this.future.cancel(true);
            this.future = null;
        }

    }

    public static class Sync extends SpellcheckTask {

        private SpellcheckResult result;

        @Getter
        private final boolean isModal;

        private Sync(String text, boolean isModal) {
            super(text);
            this.isModal = isModal;
        }

        @Override
        public boolean isComplete() {
            return result != null;
        }

        @Override
        public void complete(SpellcheckResult result) {
            this.result = result;
        }

        @Override
        public SpellcheckResult getResult() {
            return result;
        }

        @Override
        public CompletableFuture<SpellcheckResult> getFutureResult() {
            return CompletableFuture.completedFuture(result);
        }

        @Override
        public void dispose() {
            result = null;
        }
    }
}
