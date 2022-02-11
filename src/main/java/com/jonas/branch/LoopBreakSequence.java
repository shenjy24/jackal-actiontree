package com.jonas.branch;

import com.jonas.node.EStatus;
import com.jonas.node.IBehaviour;
import com.jonas.util.Env;
import com.jonas.util.schecule.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhongxiaofeng
 * @createTime 2021/10/21 19:47
 */
public class LoopBreakSequence extends LoopSequence {

    private static final Logger logger = LoggerFactory.getLogger(LoopBreakSequence.class);

    private boolean running = false;

    @Override
    public EStatus tick(IBehaviour root, boolean fromWeb) {
        this.running = true;
        this.scheduleTick(root, fromWeb, this.maxLoopCounts);
        return EStatus.SUCCESS;
    }

    private void scheduleTick(IBehaviour root, boolean fromWeb, int newLoopCounts) {
        if (newLoopCounts <= 0) {
            return;
        } else if (!this.running) {
            return;
        } else if (this.children.isEmpty()) {
            return;
        }
        this.handTickBehaviours.clear();
        boolean failedBreak = false;
        for (IBehaviour behaviour : this.children) {
            if (behaviour.tick(root, fromWeb) == EStatus.SUCCESS) {
                this.handTickBehaviours.add(behaviour);
            } else {
                failedBreak = true;
                break;
            }
        }
        this.isExecuted = true;
        if (failedBreak) {
            return;
        }
        RootSequence rootSequence = (RootSequence)root;
        final int lCounts = newLoopCounts - 1;
        logger.info("LoopBreakSequence scheduleTick, treeId:{}, maxCounts:{}, currentCounts:{}",
                rootSequence.getTreeId(), this.maxLoopCounts, this.maxLoopCounts - lCounts);
        int newDelay = this.loopDuration / Env.TICK_TIME;
        if (newDelay < 0) {
            newDelay = 1;
        }
        final int lDelay = newDelay;
        Scheduler.getInstance().scheduleDelayedRepeatingTask(() -> {
            scheduleTick(root, fromWeb, lCounts);
        }, lDelay, 0, false, rootSequence.getTreeId());
    }

    @Override
    public void reset(boolean onlyStatus) {
        this.running = false;
        super.reset(onlyStatus);
    }
}
