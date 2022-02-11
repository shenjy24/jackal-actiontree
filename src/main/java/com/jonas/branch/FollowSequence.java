package com.jonas.branch;

import com.jonas.node.EStatus;
import com.jonas.node.IBehaviour;

/**
 * 顺序节点，从第一个开始
 */
public class FollowSequence extends BaseSequence {

    protected EStatus status = EStatus.INITIALIZATION;

    @Override
    public EStatus tick(IBehaviour root, boolean fromWeb) {
        this.handTickBehaviours.clear();
        for (IBehaviour behaviour : this.children) {
            EStatus status = behaviour.tick(root, fromWeb);

            //若失败则直接返回
            if (status == EStatus.FAILURE || status == EStatus.ABORTED) {
                this.status = status;
                return this.status;
            }
            this.handTickBehaviours.add(behaviour);
        }

        this.status = EStatus.SUCCESS;
        this.isExecuted = true;
        return this.status;
    }

}
