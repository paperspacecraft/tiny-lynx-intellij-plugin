package com.paperspacecraft.intellij.plugin.tinylynx.spellcheck;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.paperspacecraft.intellij.plugin.tinylynx.settings.SettingsService;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public final class SpellcheckService {
    private static final Logger LOG = Logger.getInstance(SpellcheckService.class);

    private static final String CACHE_EXCEPTION = "Could not retrieve a spellcheck for entry '%s' via the tasks cache";

    private final SpellcheckWorkerService workerService;

    private final LoadingCache<CacheKey, SpellcheckTask> cache;

    private final Debouncer<SpellcheckResult> debouncer;

    public SpellcheckService(Project project) {
        this.workerService = project.getService(SpellcheckWorkerService.class);
        SettingsService settingsService = project.getService(SettingsService.class);
        this.cache = CacheBuilder
                .newBuilder()
                .expireAfterWrite(settingsService.getCacheLifespan(), TimeUnit.MINUTES)
                .build(new LocalCacheLoader());
        this.debouncer = new Debouncer<>();
    }

    public CompletableFuture<SpellcheckResult> checkAsync(String text) {
        try {
            return cache.get(CacheKey.async(text)).getFutureResult();
        } catch (ExecutionException e) {
            LOG.error(String.format(CACHE_EXCEPTION, text), e);
        }
        return CompletableFuture.completedFuture(SpellcheckResult.EMPTY);
    }

    public CompletableFuture<SpellcheckResult> checkAsync(Object identity, String text) {
        if (identity == null) {
            return checkAsync(text);
        }
        return debouncer.debounceFuture(identity, () -> checkAsync(text));
    }

    public SpellcheckResult checkSync(String text) {
        try {
            return cache.get(CacheKey.sync(text)).getFutureResult().get();
        } catch (ExecutionException e) {
            LOG.error(String.format(CACHE_EXCEPTION, text), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return SpellcheckResult.EMPTY;
    }

    public SpellcheckResult lookUp(String text) {
        SpellcheckTask task = cache.getIfPresent(CacheKey.of(text));
        if (task == null || !task.getFutureResult().isDone()) {
            return SpellcheckResult.EMPTY;
        }
        try {
            return task.getFutureResult().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            LOG.error(String.format(CACHE_EXCEPTION, text), e);
        }
        return SpellcheckResult.EMPTY;
    }

    public void cleanUp() {
        cache.asMap().values().forEach(SpellcheckTask::dispose);
        cache.invalidateAll();
    }

    public static SpellcheckService getInstance(Project project) {
        return project.getService(SpellcheckService.class);
    }

    private class LocalCacheLoader extends CacheLoader<CacheKey, SpellcheckTask> {
        @Override
        public SpellcheckTask load(@NotNull CacheKey key) {
            if (key.isSync()) {
                return workerService.run(SpellcheckTask.sync(key.getValue()));
            }
            return workerService.run(SpellcheckTask.async(key.getValue()));
        }
    }

    @AllArgsConstructor (access = AccessLevel.PRIVATE)
    @Getter
    private static class CacheKey {
        private final String value;
        private final boolean sync;

        public static CacheKey of(String value) {
            return new CacheKey(value, true);
        }

        public static CacheKey async(String value) {
            return new CacheKey(value, false);
        }

        public static CacheKey sync(String value) {
            return new CacheKey(value, true);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CacheKey cacheKey = (CacheKey) o;

            return value.equals(cacheKey.value);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }
}
