package com.jonas.branch;


import com.jonas.node.BehaviorList;
import com.jonas.node.EStatus;
import com.jonas.node.IBehaviour;
import com.jonas.node.IProbability;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 随机寻找一个子节点 tick
 */
public class BaseProbability extends IProbability {

    private EStatus status = EStatus.INITIALIZATION;

    /**
     * 子节点列表
     */
    protected ArrayList<IBehaviour> children = new ArrayList<>();

    @Override
    public void addChild(IBehaviour child, int weight) {
        this.children.add(child);
    }

    @Override
    public void removeChild(IBehaviour child) {
        this.children.remove(child);
    }

    @Override
    public void clearChild() {
        this.children.clear();
    }

    @Override
    public List<IBehaviour> getChildren() {
        return new BehaviorList<IBehaviour>(this.children);
    }

    @Override
    public void onInitialize() {
        for (IBehaviour behaviour : this.children) {
            behaviour.onInitialize();
        }
    }

    @Override
    public void trigger(String equipId, String modelId, Map<String, Object> inputs) {
        for (IBehaviour behaviour : this.children) {
            behaviour.trigger(equipId, modelId, inputs);
        }
    }

    @Override
    public void reset(boolean onlyStatus) {
        super.reset(onlyStatus);
        for (IBehaviour behaviour : this.children) {
            behaviour.reset(onlyStatus);
        }
    }

    @Override
    public EStatus tick(IBehaviour root, boolean fromWeb) {
        this.handTickBehaviours.clear();
        int index = (int) (Math.random() * this.children.size());
        IBehaviour iBehaviour = this.children.get(index);
        this.handTickBehaviours.add(iBehaviour);
        this.status = iBehaviour.tick(root, fromWeb);
        if (this.status == EStatus.SUCCESS) {
            this.isExecuted = true;
        }
        return this.status;
    }

}
