package com.jonas.branch;

import com.jonas.node.EStatus;
import com.jonas.node.IBehaviour;

/**
 * 依次轮训，选择一个
 */
public class CycleSelector extends BaseSelector {

    private EStatus status = EStatus.INITIALIZATION;

    private int lastIndex = 0;

    @Override
    public EStatus tick(IBehaviour root, boolean fromWeb) {
        this.handTickBehaviours.clear();
        int index = this.lastIndex;
        if (index == super.children.size()) {
            index = 0;
        }
        try {
            do {
                IBehaviour behaviour = super.children.get(index++);
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
                if (index == super.children.size()) {
                    index = 0;
                }
            } while (index != this.lastIndex);
        } finally {
            this.lastIndex = index;
        }

        this.status = EStatus.FAILURE;
        return this.status;
    }

    @Override
    public void reset(boolean onlyStatus) {
        super.reset(onlyStatus);
        if (!onlyStatus) {
            this.lastIndex = 0;
        }
    }
}
