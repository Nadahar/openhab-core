/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.core.internal.common;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompositeExecutorService implements ScheduledExecutorService {

    @NonNull
    private final Logger logger = LoggerFactory.getLogger(SafeCallManagerImpl.class);

    @NonNull
    private final ExecutorService executor;

    @NonNull
    private final ScheduledExecutorService scheduler;

    public CompositeExecutorService(@NonNull ScheduledExecutorService scheduler, @NonNull ExecutorService executor) {
        this.scheduler = scheduler;
        this.executor = executor;
    }

    @Override
    public void shutdown() {
        scheduler.shutdown();
        executor.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        List<Runnable> result = scheduler.shutdownNow();
        result.addAll(executor.shutdownNow());
        return result;
    }

    @Override
    public boolean isShutdown() {
        return scheduler.isShutdown() && executor.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return scheduler.isTerminated() && executor.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, @Nullable TimeUnit unit) throws InterruptedException {
        TimeUnit timeUnit = unit == null ? TimeUnit.MILLISECONDS : unit;
        long starttime = System.nanoTime();

        boolean result = scheduler.awaitTermination(timeout, timeUnit);
        if (result) {
            long remaining = timeUnit.toNanos(timeout) - System.nanoTime() + starttime;
            if (remaining <= 0L) {
                return false;
            }
            result = executor.awaitTermination(remaining, TimeUnit.NANOSECONDS);
        }
        return result;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return executor.submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return executor.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return executor.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return executor.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return executor.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return executor.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(@NonNull Runnable command) {
        executor.execute(command);
    }

    @Override
    public @NonNull ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        if (delay <= 0L) {
            return new FakeScheduledFuture<>(executor.submit(command));
        }
        return new CompondScheduledFuture<>(scheduler.schedule(() -> {
            return executor.submit(command);
        }, delay, unit));
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        if (delay <= 0L) {
            return new FakeScheduledFuture<>(executor.submit(callable));
        }
        return new CompondScheduledFuture<>(scheduler.schedule(() -> {
            return executor.submit(callable);
        }, delay, unit));
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        Objects.requireNonNull(command);
        Objects.requireNonNull(unit);
        TaskLauncher launcher = new TaskLauncher(command, executor);
        return new TaskLauncherScheduledFuture(scheduler.scheduleAtFixedRate(launcher, initialDelay, period, unit),
                launcher);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        Objects.requireNonNull(command);
        Objects.requireNonNull(unit);
        TaskLauncher launcher = new TaskLauncher(command, executor);
        return new TaskLauncherScheduledFuture(scheduler.scheduleWithFixedDelay(launcher, initialDelay, delay, unit),
                launcher);
    }

    private class FakeScheduledFuture<V> implements ScheduledFuture<V> {

        @NonNull
        private final Future<V> delegate;

        public FakeScheduledFuture(@NonNull Future<V> future) {
            this.delegate = future;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return 0L;
        }

        @Override
        public int compareTo(Delayed other) {
            if (other == this || other instanceof FakeScheduledFuture) {
                return 0;
            }
            long diff = getDelay(TimeUnit.NANOSECONDS) - other.getDelay(TimeUnit.NANOSECONDS);
            return (diff < 0) ? -1 : (diff > 0) ? 1 : 0;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return delegate.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return delegate.isCancelled();
        }

        @Override
        public boolean isDone() {
            return delegate.isDone();
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            return delegate.get();
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.get(timeout, unit);
        }
    }

    private class CompondScheduledFuture<V> implements ScheduledFuture<V> {

        @NonNull
        private final ScheduledFuture<Future<V>> scheduledTask;

        public CompondScheduledFuture(@NonNull ScheduledFuture<Future<V>> scheduledFuture) {
            this.scheduledTask = scheduledFuture;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return scheduledTask.getDelay(unit);
        }

        @Override
        public int compareTo(@NonNull Delayed other) {
            if (other == this) {
                return 0;
            }
            if (other instanceof CompondScheduledFuture o) {
                return scheduledTask.compareTo(o.scheduledTask);
            }
            if (other instanceof TaskLauncherScheduledFuture o) {
                return scheduledTask.compareTo(o.scheduledTask);
            }
            long diff = getDelay(TimeUnit.NANOSECONDS) - other.getDelay(TimeUnit.NANOSECONDS);
            return (diff < 0) ? -1 : (diff > 0) ? 1 : 0;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            boolean result = scheduledTask.cancel(false);
            if (scheduledTask.isCancelled()) {
                return result;
            }
            Future<V> task;
            try {
                task = scheduledTask.get();
            } catch (CancellationException e) {
                return result;
            } catch (InterruptedException e) {
                return false;
            } catch (ExecutionException e) {
                // Should be impossible
                logger.warn("Unexpected exception in CompondScheduledFuture.cancel(): {}", e.getCause());
                return false;
            }
            return task.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            if (!scheduledTask.isCancelled()) {
                return false;
            }
            if (scheduledTask.isDone()) {
                try {
                    return scheduledTask.get().isCancelled();
                } catch (CancellationException e) {
                    return true;
                } catch (InterruptedException e) {
                    return false;
                } catch (ExecutionException e) {
                    // Should be impossible
                    logger.warn("Unexpected exception in CompondScheduledFuture.isCancelled(): {}", e.getCause());
                    return false;
                }
            }
            return false;
        }

        @Override
        public boolean isDone() {
            if (!scheduledTask.isDone()) {
                return false;
            }
            try {
                return scheduledTask.get().isDone();
            } catch (CancellationException e) {
                return true;
            } catch (InterruptedException e) {
                return false;
            } catch (ExecutionException e) {
                // Should be impossible
                logger.warn("Unexpected exception in CompondScheduledFuture.isDone(): {}", e.getCause());
                return false;
            }
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            return scheduledTask.get().get();
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            TimeUnit timeUnit = unit == null ? TimeUnit.MILLISECONDS : unit;
            long starttime = System.nanoTime();

            Future<V> task = scheduledTask.get(timeout, timeUnit);
            long remaining = timeUnit.toNanos(timeout) - System.nanoTime() + starttime;
            if (remaining <= 0L) {
                throw new TimeoutException();
            }
            return task.get(remaining, TimeUnit.NANOSECONDS);
        }
    }

    private class TaskLauncherScheduledFuture implements ScheduledFuture<Void> {

        @NonNull
        private final ScheduledFuture<?> scheduledTask;

        @NonNull
        private final TaskLauncher taskLauncher;

        public TaskLauncherScheduledFuture(@NonNull ScheduledFuture<?> scheduledTask,
                @NonNull TaskLauncher taskLauncher) {
            this.scheduledTask = scheduledTask;
            this.taskLauncher = taskLauncher;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return scheduledTask.getDelay(unit);
        }

        @Override
        public int compareTo(@NonNull Delayed other) {
            if (other == this) {
                return 0;
            }
            if (other instanceof CompondScheduledFuture o) {
                return scheduledTask.compareTo(o.scheduledTask);
            }
            if (other instanceof TaskLauncherScheduledFuture o) {
                return scheduledTask.compareTo(o.scheduledTask);
            }
            long diff = getDelay(TimeUnit.NANOSECONDS) - other.getDelay(TimeUnit.NANOSECONDS);
            return (diff < 0) ? -1 : (diff > 0) ? 1 : 0;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            boolean result = scheduledTask.cancel(false);
            Future<?> task = taskLauncher.getTaskFuture();
            if (task != null) {
                result &= task.cancel(mayInterruptIfRunning);
            }
            return result;
        }

        @Override
        public boolean isCancelled() {
            return scheduledTask.isCancelled();
        }

        @Override
        public boolean isDone() {
            return scheduledTask.isDone();
        }

        @Override
        public Void get() throws InterruptedException, ExecutionException {
            return (Void) scheduledTask.get();
        }

        @Override
        public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return (Void) scheduledTask.get(timeout, unit);
        }
    }

    private static class TaskLauncher implements Runnable {

        @NonNull
        private final Runnable task;
        @NonNull
        private final ExecutorService executor;

        private volatile Future<?> taskFuture;

        public TaskLauncher(@NonNull Runnable task, @NonNull ExecutorService executor) {
            this.task = task;
            this.executor = executor;
        }

        @Override
        public void run() {
            taskFuture = executor.submit(task);
        }

        public Future<?> getTaskFuture() {
            return taskFuture;
        }
    }
}
