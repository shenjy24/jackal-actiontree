package com.jonas.branch;

import com.jonas.node.EStatus;
import com.jonas.node.IBehaviour;

/**
 * @author zhongxiaofeng
 * @createTime 2021/8/20 17:42
 */
public class FullSequence extends BaseSequence {

    protected EStatus status = EStatus.INITIALIZATION;

    @Override
    public EStatus tick(IBehaviour root, boolean fromWeb) {
        this.handTickBehaviours.clear();
        for (IBehaviour behaviour : this.children) {
            behaviour.tick(root, fromWeb);
            this.handTickBehaviours.add(behaviour);
        }

        this.status = EStatus.SUCCESS;
        this.isExecuted = true;
        return this.status;
    }

}
