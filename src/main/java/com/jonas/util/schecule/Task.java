package com.jonas.util.schecule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhongxiaofeng
 * @createTime 2021/4/15 17:02
 */
public class Task implements Comparable{

    private static final Logger logger = LoggerFactory.getLogger(Task.class);

    private int taskId;

    // 是否异步
    private final boolean asynchronous;
    // 用于delay task
    private int delay;
    // 用于repeat task
    private int period;
    // 上一次执行的tick
    private long lastRunTick;
    // 下一次执行的tick
    private long nextRunTick;
    // 是否取消
    private boolean cancelled;
    // 绑定的任务链
    private String bindTaskType;

    // 执行时候的调用
    private final Runnable runnable;

    // 取消的时候调用
    private Runnable cancelableCb;

    public Task(Runnable runnable, int taskId, boolean asynchronous) {
        this.runnable = runnable;
        this.taskId = taskId;
        this.asynchronous = asynchronous;
    }

    public Runnable getRunnable() {
        return this.runnable;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getDelay() {
        return this.delay;
    }

    public boolean isDelayed() {
        return this.delay > 0;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public boolean isRepeating() {
        return this.period > 0;
    }

    public int getPeriod() {
        return this.period;
    }

    public long getLastRunTick() {
        return lastRunTick;
    }

    public void setLastRunTick(long lastRunTick) {
        this.lastRunTick = lastRunTick;
    }

    public long getNextRunTick() {
        return this.nextRunTick;
    }

    public void setNextRunTick(long nextRunTick) {
        this.nextRunTick = nextRunTick;
    }

    public void cancel() {
        this.cancelled = true;
        // 如果拥有取消的调用，则调用
        if (this.cancelableCb != null) {
            this.cancelableCb.run();
        }
    }

    /**
     * 执行函数
     * @param currentTick
     */
    public void run(long currentTick) {
        try {
            setLastRunTick(currentTick);
            this.runnable.run();
        } catch (Throwable ex) {
            logger.error("task run failed", ex);
        }
    }

    public boolean isAsynchronous() {
        return asynchronous;
    }

    public int getTaskId() {
        return taskId;
    }

    public String getBindTaskType() {
        return bindTaskType;
    }

    public void setBindTaskType(String bindTaskType) {
        this.bindTaskType = bindTaskType;
    }

    public Runnable getCancelableCb() {
        return cancelableCb;
    }

    public void setCancelableCb(Runnable cancelableCb) {
        this.cancelableCb = cancelableCb;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    @Override
    public int compareTo(Object object) {
        if (object == null) {
            return 1;
        } else if (!(object instanceof Task)) {
            return 1;
        }
        Task right = (Task) object;
        int i = (int)(this.getNextRunTick() - right.getNextRunTick());
        if (i == 0) {
            return this.getTaskId() - right.getTaskId();
        }
        return i;
    }

    public Task clone() {
        // id 是-1
        Task newTask = new Task(this.runnable, -1, this.asynchronous);
        // 用于delay task
        newTask.delay = this.delay;
        // 用于repeat task
        newTask.period = this.period;
        // 下一次执行的tick
        newTask.nextRunTick = this.nextRunTick;
        // 是否取消
        newTask.cancelled = this.cancelled;
        // 绑定的任务链
        newTask.bindTaskType = this.bindTaskType;
        // 取消的时候调用
        newTask.cancelableCb = this.cancelableCb;
        return newTask;
    }
}
