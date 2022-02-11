package com.jonas.branch;

import com.jonas.action.AICallbackTask;
import com.jonas.node.*;
import com.jonas.util.Env;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RootSequence extends FollowSequence {

    private static final Logger logger = LoggerFactory.getLogger(RootSequence.class);

    private int maxDelay = 0;

    private String treeId = "";

    /**
     * 是否略过condition的进度判断
     */
    private boolean isSkipCondition = false;

    // 暂停的时调用的task, 主要是针对流媒体
    private List<AICallbackTask> onPauseTasks = new ArrayList<>();

    // 恢复的时调用的task, 主要是针对流媒体
    private List<AICallbackTask> onResumeTasks = new ArrayList<>();

    // 停止的时调用的task, 主要是针对流媒体
    private List<AICallbackTask> onStopTasks = new ArrayList<>();

    public void refreshMaxDelay(int value) {
        if (value > this.maxDelay) {
            this.maxDelay = value;
        }
    }

    // 下一次允许运行的时间，如果是0，则表示不限制
    private long nextRuntime = 0;


    /**
     * 绑定id和显示名字
     *
     * @param id
     * @param showName
     */
    @Override
    public void bindId(String id, String showName) {
        if (StringUtils.isEmpty(id)) {
            return;
        }
        this.id = id;
        this.showName = showName;
    }

    @Override
    public EStatus tick(IBehaviour root) {
        long now = System.currentTimeMillis();
        if (now < this.nextRuntime) {
            logger.warn("RootSequence不允许tick检查，原因：上次执行还未结束");
            this.status = EStatus.FAILURE;
            return this.status;
        }
        // 重置
        this.maxDelay = 0;
        this.nextRuntime = 0;
        for (IBehaviour behaviour : this.children) {
            EStatus status = behaviour.tick(root);
            //若失败则直接返回
            if (status == EStatus.FAILURE || status == EStatus.ABORTED) {
                this.status = status;
                return this.status;
            }
        }

        this.status = EStatus.SUCCESS;
        // 表示在运行中的时间
        now = System.currentTimeMillis();
        // 每个tick 20毫秒
        this.nextRuntime = now + (this.maxDelay * Env.TICK_TIME);
        return this.status;
    }

    @Override
    public void reset(boolean onlyStatus) {
        super.reset(onlyStatus);
        // 重置变量
        this.maxDelay = 0;
        this.nextRuntime = 0;
        // 重置是否略过condition
        this.isSkipCondition = false;
        // 清理task
        this.clearAllTasks();
    }

    /**
     * 清除所有的调度任务
     */
    private void clearAllTasks() {
        this.onPauseTasks.clear();
        this.onResumeTasks.clear();
        this.onStopTasks.clear();
    }

    /**
     * 是否在运行中
     *
     * @return
     */
    public boolean isRunning() {
        long now = System.currentTimeMillis();
        if (now < this.nextRuntime) {
            return true;
        }
        return false;
    }

    public int getMaxDelay() {
        return maxDelay;
    }

    public String getTreeId() {
        return treeId;
    }

    public void setTreeId(String treeId) {
        this.treeId = treeId;
    }

    public boolean isSkipCondition() {
        return isSkipCondition;
    }

    public void setSkipCondition(boolean skipCondition) {
        isSkipCondition = skipCondition;
    }

    public List<AICallbackTask> getOnPauseTasks() {
        return onPauseTasks;
    }

    public List<AICallbackTask> getOnResumeTasks() {
        return onResumeTasks;
    }

    public List<AICallbackTask> getOnStopTasks() {
        return onStopTasks;
    }

    public void addPauseTasks(AICallbackTask task) {
        this.onPauseTasks.add(task);
    }

    public void addResumeTasks(AICallbackTask task) {
        this.onResumeTasks.add(task);
    }

    public void addStopTasks(AICallbackTask task) {
        this.onStopTasks.add(task);
    }

    /**
     * 根据id寻找到可执行的叶子节点
     *
     * @param behaviourId
     * @return
     */
    public IBaseBehaviour findBehaviourById(String behaviourId) {
        if (!StringUtils.isEmpty(this.getId())
                && behaviourId.equals(this.getId())) {
            return this;
        }
        return this.findBehaviourById(this, behaviourId);
    }

    /**
     * 递归查找
     *
     * @param parent
     * @param behaviourId
     * @return
     */
    private IBaseBehaviour findBehaviourById(IBehaviour parent, String behaviourId) {
        if (parent instanceof IBranchNode) {
            IBranchNode currentNode = (IBranchNode) parent;
            // 先判断自己的id是否满足
            if (!StringUtils.isEmpty(currentNode.getId())
                    && behaviourId.equals(currentNode.getId())) {
                return currentNode;
            }
            // 查找子对象
            for (IBehaviour child : currentNode.getChildren()) {
                IBaseBehaviour result = this.findBehaviourById(child, behaviourId);
                if (result != null) {
                    return result;
                }
            }
        } else if (parent instanceof ILeafNode) {
            ILeafNode currentNode = (ILeafNode) parent;
            // 判断leaf的id是否满足
            if (!StringUtils.isEmpty(currentNode.getId())
                    && behaviourId.equals(currentNode.getId())) {
                return currentNode;
            }
        }
        return null;
    }
}
