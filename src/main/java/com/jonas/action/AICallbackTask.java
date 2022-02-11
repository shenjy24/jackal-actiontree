package com.jonas.action;

import com.jonas.util.Env;

/**
 * @author jonas
 * @createTime 2021/7/21 17:26
 */
public class AICallbackTask {

    // 单位是tick
    private int previous = 0;

    // 单位是毫秒
    private int continued = 0;

    private Runnable runnable;

    public AICallbackTask(Runnable runnable, int previous) {
        this(runnable, previous, 0);
    }

    public AICallbackTask(Runnable runnable, int previous, int continued) {
        this.runnable = runnable;
        this.previous = previous;
        this.continued = continued;
    }

    /**
     * 延迟previous时间，用于暂停后的复位
     * 单位毫秒
     * @param delay
     */
    public void delayPreviousTime(long delay) {
        if (delay < 0) {
            return;
        }
        int delayTick = (int)(delay / Env.TICK_TIME);
        this.previous += delayTick;
    }

    /**
     * 根据时间进行回调
     * @param treeStartTime
     */
    public void execute(long treeStartTime, boolean enforce) {
        if (this.isValid(treeStartTime, enforce)) {
            this.runnable.run();
        }
    }

    /**
     * 是否有效
     * @param treeStartTime
     * @param enforce
     * @return
     */
    public boolean isValid(long treeStartTime, boolean enforce) {
        if (treeStartTime <= 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        int previousTime = this.previous * Env.TICK_TIME;
        if (now < treeStartTime + previousTime && !enforce) {
            // 主任务还没触发, 且非强制
            return false;
        } else if (now >= treeStartTime + previousTime + this.continued) {
            // 已经触发完成
            return false;
        }
        return true;
    }
}
