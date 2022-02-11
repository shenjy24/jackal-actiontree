package com.jonas.util.schecule;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author zhongxiaofeng
 * @createTime 2021/4/15 17:58
 */
public class Scheduler {

    private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

    private static Scheduler instance = new Scheduler();

    public static Scheduler getInstance() {
        return instance;
    }

    private final AsyncPool asyncPool;
    private final Queue<Task> pending;
    private final Queue<Task> queue;
    private final Map<Integer, Task> taskMap;
    private final AtomicInteger currentTaskId;

    private volatile long currentTick = 0;

    private Scheduler() {
        this.pending = new ConcurrentLinkedQueue<>();
        this.currentTaskId = new AtomicInteger();
        // 执行顺序排序
        this.queue = new PriorityQueue<>(11, peachTaskComparator);
        this.taskMap = new ConcurrentHashMap<>();
        this.asyncPool = new AsyncPool(1);
    }

    // 固定排序
    private static Comparator<Task> peachTaskComparator = new Comparator<Task>() {
        @Override
        public int compare(Task left, Task right) {
            int i = (int) (left.getNextRunTick() - right.getNextRunTick());
            if (i == 0) {
                return left.getTaskId() - right.getTaskId();
            }
            return i;
        }
    };

    /**
     * 取消所有的task
     */
    public void cancelAllTasks() {
        for (Map.Entry<Integer, Task> entry : this.taskMap.entrySet()) {
            try {
                Task value = entry.getValue();
                if (value.isCancelled()) {
                    continue;
                }
                value.cancel();
            } catch (RuntimeException ex) {
                logger.error("Exception while cancelAllTasks", ex);
            }
        }
        this.taskMap.clear();
        this.queue.clear();
        this.currentTaskId.set(0);
    }

    /**
     * 取消某个任务类型
     *
     * @param bindTaskType
     */
    public void cancelTasks(String bindTaskType) {
        if (StringUtils.isEmpty(bindTaskType)) {
            return;
        }
        for (Map.Entry<Integer, Task> entry : this.taskMap.entrySet()) {
            Task task = entry.getValue();
            if (task.isCancelled()) {
                continue;
            }
            try {
                // 如果bindTaskType一致，则取消
                if (bindTaskType.equalsIgnoreCase(task.getBindTaskType())) {
                    task.cancel();
                }
            } catch (RuntimeException ex) {
                logger.error("Exception while cancelAllTasks", ex);
            }
        }
    }

    /**
     * 取消task
     *
     * @param taskId
     */
    public void cancelTask(int taskId) {
        if (taskMap.containsKey(taskId)) {
            try {
                Task task = taskMap.get(taskId);
                this.cancelTask(task);
            } catch (RuntimeException ex) {
                logger.error("Exception while invoking onCancel", ex);
            }
        }
    }

    /**
     * 取消task
     *
     * @param task
     */
    public void cancelTask(Task task) {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    /**
     * 获取有效的任务类型
     * 已经排序了
     *
     * @param bindTaskType
     * @return
     */
    public Queue<Task> fetchValidTasks(String bindTaskType) {
        Queue<Task> results = new PriorityQueue<>(11, peachTaskComparator);
        if (StringUtils.isEmpty(bindTaskType)) {
            return results;
        }
        for (Map.Entry<Integer, Task> entry : this.taskMap.entrySet()) {
            Task task = entry.getValue();
            if (task.isCancelled()) {
                continue;
            }
            // 如果bindTaskType一致，则取消
            if (bindTaskType.equalsIgnoreCase(task.getBindTaskType())) {
                results.offer(task);
            }
        }
        return results;
    }

    // 添加同步调度任务
    public Task scheduleDelayedRepeatingTask(Runnable runnable, int delay, int period) {
        return this.addTask(runnable, delay, period, false, null);
    }

    // 添加同步调度任务
    public Task scheduleTask(Runnable runnable) {
        return this.addTask(runnable, 0, 0, false, null);
    }

    // 添加同步调度任务
    public Task scheduleDelayedTask(Runnable runnable, int delay) {
        return this.addTask(runnable, delay, 0, false, null);
    }

    // 添加同步调度任务
    public Task scheduleRepeatingTask(Runnable runnable, int period) {
        return this.addTask(runnable, 0, period, false, null);
    }

    // 添加调度任务
    public Task scheduleDelayedRepeatingTask(Runnable runnable, int delay, int period, boolean asynchronous, String bindTaskType) {
        return this.addTask(runnable, delay, period, asynchronous, bindTaskType);
    }

    // 添加调度任务
    public Task scheduleDelayedTask(Runnable runnable, int delay, boolean asynchronous) {
        return this.addTask(runnable, delay, 0, asynchronous, null);
    }

    // 添加调度任务
    public Task scheduleRepeatingTask(Runnable runnable, int period, boolean asynchronous) {
        return this.addTask(runnable, 0, period, asynchronous, null);
    }

    // 添加调度任务
    public Task scheduleTask(Task task) {
        if (task == null) {
            logger.error("scheduleTask failed, the peachTask is null!");
            return null;
        }
        if (task.getDelay() < 0 || task.getPeriod() < 0) {
            logger.error("Attempted to scheduleTask with negative delay or period.");
            return null;
        }
        // 需要更新taskId
        task.setTaskId(this.nextTaskId());
        // 重新设置执行时间
        task.setNextRunTick(task.isDelayed() ? currentTick + task.getDelay() : currentTick);
        pending.offer(task);
        taskMap.put(task.getTaskId(), task);
        return task;
    }

    // tick check
    public void tickCheck(long tick) {
        try {
            this.currentTick = tick;
            while (!pending.isEmpty()) {
                queue.offer(pending.poll());
            }

            while (isReady(currentTick)) {
                Task task = queue.poll();
                if (task == null) {
                    continue;
                } else if (task.isCancelled()) {
                    taskMap.remove(task.getTaskId());
                    continue;
                } else if (task.isAsynchronous()) {
                    asyncPool.execute(() -> {
                        try {
                            task.setLastRunTick(currentTick);
                            task.getRunnable().run();
                        } catch (Throwable e) {
                            logger.error("Exception while tickCheck asyncPool.execute", e);
                        }
                    });
                } else {
                    try {
                        task.run(currentTick);
                    } catch (Throwable e) {
                        logger.error("tickCheck failed!", e);
                    }
                }
                if (task.isRepeating()) {
                    task.setNextRunTick(currentTick + task.getPeriod());
                    pending.offer(task);
                } else {
                    taskMap.remove(task.getTaskId());
                }
            }
        } catch (Throwable t) {
            logger.error("tickCheck failed!", t);
        }

    }

    private Task addTask(Runnable task, int delay, int period, boolean asynchronous, String bindTaskType) {
        if (delay < 0 || period < 0) {
            logger.error("Attempted to register a task with negative delay or period.");
            return null;
        } else if (task == null) {
            logger.error("addTask failed, the task is null!");
            return null;
        }
        Task peachTask = new Task(task, nextTaskId(), asynchronous);
        peachTask.setDelay(delay);
        peachTask.setPeriod(period);
        peachTask.setNextRunTick(peachTask.isDelayed() ? currentTick + peachTask.getDelay() : currentTick);
        peachTask.setBindTaskType(bindTaskType);
        pending.offer(peachTask);
        taskMap.put(peachTask.getTaskId(), peachTask);
        return peachTask;
    }

    private boolean isReady(long currentTick) {
        return this.queue.peek() != null && this.queue.peek().getNextRunTick() <= currentTick;
    }

    private int nextTaskId() {
        return currentTaskId.incrementAndGet();
    }

}
