package com.paperspacecraft.intellij.plugin.tinylynx.spellcheck;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

class Debouncer<T> {

    private static final int DEFAULT_TIMEOUT_MS = 3000;

    private final int timeout;
    private final ConcurrentHashMap<Object, DelayedCallFacade<T>> delayedCalls;

    public Debouncer() {
        this(DEFAULT_TIMEOUT_MS);
    }

    public Debouncer(int timeout) {
        this.timeout = timeout;
        this.delayedCalls = new ConcurrentHashMap<>();
        ScheduledExecutorService timerThread = Executors.newScheduledThreadPool(1);
        timerThread.scheduleAtFixedRate(this::evict, timeout, timeout, TimeUnit.MILLISECONDS);
    }

    public CompletableFuture<T> debounce(Object identity, Supplier<T> supplier) {
        PrimaryDelayedCallFacade<T> resultFacade = (PrimaryDelayedCallFacade<T>) delayedCalls.computeIfAbsent(identity, id -> new PrimaryDelayedCallFacade<>());
        resultFacade.setSupplier(supplier);
        resultFacade.setLastVisited(System.currentTimeMillis());
        return resultFacade.getCompletableFuture();
    }

    public CompletableFuture<T> debounceFuture(Object identity, Supplier<CompletableFuture<T>> supplier) {
        SecondaryDelayedCallFacade<T> resultFacade = (SecondaryDelayedCallFacade<T>) delayedCalls.computeIfAbsent(identity, id -> new SecondaryDelayedCallFacade<>());
        resultFacade.setSupplier(supplier);
        resultFacade.setLastVisited(System.currentTimeMillis());
        return resultFacade.getCompletableFuture();
    }

    private void evict() {
        long timestamp = System.currentTimeMillis();
        Set<Object> identities = delayedCalls.keySet();
        identities.forEach(id -> {
            long delay = timestamp - delayedCalls.get(id).getLastVisited();
            if (delay < timeout) {
                return;
            }
            DelayedCallFacade<T> current = delayedCalls.remove(id);
            current.complete();
        });
    }

    private abstract static class DelayedCallFacade<T> {

        private long lastVisited;

        public long getLastVisited() {
            return lastVisited;
        }

        public void setLastVisited(long lastVisited) {
            this.lastVisited = lastVisited;
        }

        @Getter
        private final CompletableFuture<T> completableFuture = new CompletableFuture<>();

        abstract void complete();

    }

    private static class PrimaryDelayedCallFacade<T> extends DelayedCallFacade<T> {

        @Setter
        private Supplier<T> supplier;

        public void complete() {
            getCompletableFuture().complete(supplier.get());
        }
    }

    private static class SecondaryDelayedCallFacade<T> extends DelayedCallFacade<T> {

        @Setter
        private Supplier<CompletableFuture<T>> supplier;

        @Override
        public CompletableFuture<T> getCompletableFuture() {
            return super.getCompletableFuture().thenCompose(res -> supplier.get());
        }

        public void complete() {
            super.getCompletableFuture().complete(null);
        }
    }

}
