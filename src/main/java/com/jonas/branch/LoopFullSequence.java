package com.jonas.branch;

import com.jonas.node.EStatus;
import com.jonas.node.IBehaviour;

/**
 * @author zhongxiaofeng
 * @createTime 2021/9/9 14:53
 */
public class LoopFullSequence extends LoopSequence{

    @Override
    public EStatus tick(IBehaviour root, boolean fromWeb) {
        int newLoopCounts = this.maxLoopCounts;
        while (newLoopCounts-- > 0) {
            // 增加时间
            this.addRuntimeDelay(loopDuration);
            this.innerTick(root, fromWeb);
        }
        // 减少时间
        this.addRuntimeDelay(-this.maxLoopCounts * loopDuration);
        return EStatus.SUCCESS;
    }

    protected EStatus innerTick(IBehaviour root, boolean fromWeb) {
        this.handTickBehaviours.clear();
        for (IBehaviour behaviour : this.children) {
            behaviour.tick(root, fromWeb);
            this.handTickBehaviours.add(behaviour);
        }
        this.isExecuted = true;
        return EStatus.SUCCESS;
    }
}
