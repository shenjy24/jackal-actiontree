package com.jonas.node;

import java.util.ArrayList;
import java.util.List;

public abstract class IBranchNode extends IBaseBehaviour {

    // 上一次执行的叶子节点列表，每次执行会更新
    protected List<IBehaviour> handTickBehaviours = new ArrayList<>();

    private boolean strictMode = true;

    private boolean totalOffset = false;

    @Override
    public void onInitialize() {
    }

    @Override
    public EStatus offsetTick(IBehaviour root, boolean fromWeb) {
        if (this.totalOffset) {
            // 所有的子对象
            for (IBehaviour iBehaviour : this.getChildren()) {
                iBehaviour.offsetTick(root, fromWeb);
            }
        } else {
            // 执行过的对象
            for (IBehaviour iBehaviour : this.handTickBehaviours) {
                iBehaviour.offsetTick(root, fromWeb);
            }
        }

        this.handTickBehaviours.clear();
        this.isExecuted = false;
        return EStatus.SUCCESS;
    }

    /**
     * 添加子节点
     *
     * @param child
     */
    abstract public void addChild(IBehaviour child, int weight);

    /**
     * 移除子节点
     *
     * @param child
     */
    abstract public void removeChild(IBehaviour child);

    /**
     * 清空子节点
     */
    abstract public void clearChild();

    /**
     * 获取子节点列表
     *
     * @return
     */
    abstract public List<IBehaviour> getChildren();

    public List<IBehaviour> getHandTickBehaviours() {
        return handTickBehaviours;
    }

    /**
     * 用于新增
     * @param addRuntime
     */
    public void addRuntimeDelay(int addRuntime) {
        this.setPreRuntimeDelay(this.preRuntimeDelay + addRuntime);
    }

    @Override
    public void setPreRuntimeDelay(int runtimeDelay) {
        super.setPreRuntimeDelay(runtimeDelay);
        List<IBehaviour> children = this.getChildren();
        if (children == null || children.isEmpty()) {
            return;
        }

        for (IBehaviour child : children) {
            if (child instanceof IBaseBehaviour) {
                ((IBaseBehaviour) child).setPreRuntimeDelay(runtimeDelay);
            }
        }
    }

    public boolean isStrictMode() {
        return strictMode;
    }

    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }

    public void setTotalOffset(boolean totalOffset) {
        this.totalOffset = totalOffset;
    }
}
