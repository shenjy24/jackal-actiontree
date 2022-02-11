package com.jonas.branch;

import com.jonas.node.EStatus;
import com.jonas.node.IBehaviour;
import com.jonas.node.IBranchNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 逆反节点
 */
public class ReverseNode extends IBranchNode {

    private IBehaviour child;

    @Override
    public void addChild(IBehaviour child, int weight) {
        this.child = child;
    }

    @Override
    public void removeChild(IBehaviour child) {
        this.child = null;
    }

    @Override
    public void clearChild() {
        this.child = null;
    }

    @Override
    public List<IBehaviour> getChildren() {
        return new ArrayList<IBehaviour>() {{
            add(child);
        }};
    }

    @Override
    public void onInitialize() {
        this.child.onInitialize();
    }

    @Override
    public void trigger(String equipId, String modelId, Map<String, Object> inputs) {
        this.child.trigger(equipId, modelId, inputs);
    }

    @Override
    public void reset(boolean onlyStatus) {
        super.reset(onlyStatus);
        this.child.reset(onlyStatus);
    }

    @Override
    public EStatus tick(IBehaviour root, boolean fromWeb) {
        this.handTickBehaviours.clear();
        EStatus status = this.child.tick(root, fromWeb);

        if (status == EStatus.SUCCESS) {
            return EStatus.FAILURE;
        } else if (status == EStatus.FAILURE) {
            this.handTickBehaviours.add(this.child);
            this.isExecuted = true;
            return EStatus.SUCCESS;
        } else {
            return status;
        }
    }
}
