package com.jonas.branch;

import com.jonas.node.EStatus;
import com.jonas.node.IBehaviour;

/**
 * 选择节点，从第一个开始
 */
public class FollowSelector extends BaseSelector {

    private EStatus status = EStatus.INITIALIZATION;

    @Override
    public EStatus tick(IBehaviour root, boolean fromWeb) {
        this.handTickBehaviours.clear();
        for (IBehaviour behaviour : super.children) {
            EStatus status = behaviour.tick(root, fromWeb);

            //若成功则直接返回
            if (status == EStatus.SUCCESS || status == EStatus.ABORTED) {
                this.handTickBehaviours.add(behaviour);
                this.status = status;
                if (this.status == EStatus.SUCCESS) {
                    this.isExecuted = true;
                }
                return this.status;
            }
        }

        this.status = EStatus.FAILURE;
        return this.status;
    }

}
