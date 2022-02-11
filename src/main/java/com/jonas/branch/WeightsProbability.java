package com.jonas.branch;

import com.jonas.node.BehaviorList;
import com.jonas.node.EStatus;
import com.jonas.node.IBehaviour;
import com.jonas.node.IProbability;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 带权重的随机一个子节点执行
 */
public class WeightsProbability extends IProbability {

    private EStatus status = EStatus.INITIALIZATION;

    /**
     * 子节点列表
     */
    private ArrayList<IBehaviour> children = new ArrayList<>();
    private ArrayList<Integer> weights = new ArrayList<>();
    private int maxWeight = 0;

    @Override
    public void addChild(IBehaviour child, int weight) {
        this.children.add(child);
        this.weights.add(weight);
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
        if (this.children.size() != this.weights.size()) {
            throw new RuntimeException("Weight size miss match!");
        }
        this.maxWeight = 0;
        for (int i = 0; i < this.children.size(); i++) {
            this.children.get(i).onInitialize();
            this.maxWeight += this.weights.get(i);
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
        double randomIndex = Math.random() * maxWeight;
        int total = 0;
        for (int i = 0; i < this.weights.size(); i++) {
            total += this.weights.get(i);
            if (randomIndex < total) {
                IBehaviour iBehaviour = this.children.get(i);
                this.status = iBehaviour.tick(root, fromWeb);
                this.handTickBehaviours.add(iBehaviour);
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
