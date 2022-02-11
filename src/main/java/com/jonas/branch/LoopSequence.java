package com.jonas.branch;

/**
 * @author zhongxiaofeng
 * @createTime 2021/10/21 20:45
 */
public abstract class LoopSequence extends BaseSequence {

    // 最大loop次数
    protected int maxLoopCounts = 1;
    // loop间隔
    protected int loopDuration = 50;

    public void bindMaxLoopCounts(int maxLoopCounts) {
        this.maxLoopCounts = maxLoopCounts;
    }

    public void bindLoopDuration(int loopDuration) {
        this.loopDuration = loopDuration;
    }
}
