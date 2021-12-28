package com.paperspacecraft.intellij.plugin.tinylynx.spellcheck;

import java.util.concurrent.CountDownLatch;

class TaskLock {
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    public void release() {
        countDownLatch.countDown();
    }

    public void await() {
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
