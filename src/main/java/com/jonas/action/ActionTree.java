package com.jonas.action;

import com.jonas.branch.RootSequence;
import com.jonas.leaf.ActionLeaf;
import com.jonas.leaf.ConditionLeaf;
import com.jonas.node.EStatus;
import com.jonas.node.IBaseBehaviour;
import com.jonas.node.IBehaviour;
import com.jonas.node.IBranchNode;
import com.jonas.util.Env;
import com.jonas.util.schecule.Scheduler;
import com.jonas.util.schecule.Task;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class ActionTree {

    // 行为树id
    private final String id;

    private String showName;

    // 行为树作用域，一次还是可重复
    private final String scope;

    // 优先级
    private int priority = 0;

    // 行为树的顶
    private final RootSequence root;

    // 该行为树已经执行过了，用于同一行为树判断
    private boolean hasExecuted = false;

    // 该行为树已经执行完成，用于依赖行为树判断
    private boolean hasExecutedDone = false;

    // 上一次触发成功执行的时间
    private long startExecuteTime = 0;

    // 是否在暂停中
    private boolean isPause = false;
    // 开始暂停时间
    private long startPauseTime = 0;

    private Queue<Task> pausedScheduleTasks;

    public ActionTree(RootSequence root, String showName, String scope, int priority) {
        this.root = root;
        this.id = root.getId();
        this.scope = scope;
        this.showName = showName;
        this.priority = priority;
    }

    public String getId() {
        return id;
    }

    public RootSequence getRoot() {
        return root;
    }

    // 是否在暂停中
    public boolean isPause() {
        return isPause;
    }

    // 获取名称
    public String getShowName() {
        return showName;
    }

    /**
     * 是否允许跳过，根据是否存在showName来判断
     * @return
     */
    public boolean isAllowSkip() {
        if (StringUtils.isEmpty(this.showName)) {
            return false;
        } else if (this.isPause) {
            return false;
        } else if (this.isHasExecutedDone()) {
            return false;
        }
        return true;
    }

    /**
     * 是否是重复的行为树
     * @return
     */
    public boolean isRepeated() {
        return TreeScope.REPEATED.equals(this.scope);
    }

    /**
     * 执行该行为树
     * @return
     */
    public EStatus execute() {
        EStatus result = this.root.tick(this.root);
        if (result == EStatus.SUCCESS) {
            this.startExecuteTime = System.currentTimeMillis();
        }
        return result;
    }

    /**
     * 执行某个叶子节点
     * 如果已经执行过的，则反执行
     * 如果没有执行，相当于无条件执行
     * 只处理ActionLeaf
     * @param leafId
     * @return
     */
    public EStatus executeLeaf(String leafId, boolean positive) {
        // 再判断树里面的节点
        IBaseBehaviour baseBNode = this.findBehaviourById(leafId);
        if (baseBNode == null) {
            return EStatus.FAILURE;
        } else if (baseBNode instanceof RootSequence) {
            return EStatus.FAILURE;
        } else if (baseBNode instanceof IBranchNode || baseBNode instanceof ActionLeaf) {
            if (positive) {
                return baseBNode.tick(this.root, true);
            } else {
                return baseBNode.offsetTick(this.root,true);
            }
        }
        return EStatus.FAILURE;
    }

    /**
     * 查找某棵树下的branch、leaf node
     * @param behaviourId
     * @return
     */
    public IBaseBehaviour findBehaviourById(String behaviourId) {
        if (StringUtils.isEmpty(behaviourId)) {
            return null;
        }
        return this.root.findBehaviourById(behaviourId);
    }

    /**
     * 携带id的叶子节点
     * @return
     */
    public List<IBaseBehaviour> findBehaviourWithIds () {
        List<IBaseBehaviour> results = new ArrayList<>();

        return this.findBehaviourWithIds(results, this.root);
    }

    /**
     * 寻找带id的叶子节点
     * @return
     */
    private List<IBaseBehaviour> findBehaviourWithIds(List<IBaseBehaviour> results, IBehaviour parent) {
        if (parent instanceof IBranchNode) {
            IBranchNode currentNode = (IBranchNode) parent;
            if (!StringUtils.isEmpty(currentNode.getId())) {
                results.add(currentNode);
            }
            // 子类
            for (IBehaviour child : currentNode.getChildren()) {
                this.findBehaviourWithIds(results, child);
            }
        } else if (parent instanceof ConditionLeaf) {
            ConditionLeaf currentNode = (ConditionLeaf) parent;
            // condition
            if (!StringUtils.isEmpty(currentNode.getId())) {
                results.add(currentNode);
            }
        } else if (parent instanceof ActionLeaf) {
            ActionLeaf currentNode = (ActionLeaf) parent;
            // action
            if (!StringUtils.isEmpty(currentNode.getId())) {
                results.add(currentNode);
            }
        }
        return results;
    }

    // reset
    public void reset(boolean onlyStatus) {
        // 取消遗留的任务
        Scheduler.getInstance().cancelTasks(this.getId());
        this.startExecuteTime = 0;
        this.startPauseTime = 0;
        this.pausedScheduleTasks = null;
        // 设置状态
        this.setHasExecuted(false);
        this.setHasExecutedDone(false);
        // 每个节点执行reset
        this.root.reset(onlyStatus);
        this.isPause = false;
    }

    /**
     * 暂停行为树
     */
    public void pauseTree() {
        if (this.isPause) {
            // 已经在暂停中
            return;
        }
        this.isPause = true;
        this.startPauseTime = System.currentTimeMillis();
        // 还未开始
        if (!this.isHasExecuted()) {
            return;
        }

        // 获取该行为树的调度任务
        this.pausedScheduleTasks = Scheduler.getInstance().fetchValidTasks(this.id);
        // 取消任务的执行
        long now = System.currentTimeMillis();
        long duration = now - this.startPauseTime;
        int hasDelayed = (int)(duration / Env.TICK_TIME);
        for (Task peachTask : this.pausedScheduleTasks) {
            // 重置delay时间
            int newDelay = Math.max(peachTask.getDelay() - hasDelayed, 0);
            peachTask.setDelay(newDelay);
            Scheduler.getInstance().cancelTask(peachTask);
        }
        // 执行暂停的流媒体任务
        List<AICallbackTask> onPauseTasks = this.root.getOnPauseTasks();
        for (AICallbackTask aiCallbackTask : onPauseTasks) {
            aiCallbackTask.execute(this.startExecuteTime, false);
        }
    }

    /**
     * 恢复行为树
     */
    public void resumeTree() {
        if (!this.isPause) {
            // 未在暂停中
            return;
        }
        this.isPause = false;
        // 还未开始
        if (!this.isHasExecuted()) {
            this.startPauseTime = 0;
            return;
        }
        long now = System.currentTimeMillis();
        long duration = now - this.startPauseTime;
        this.startPauseTime = 0;

        if (this.pausedScheduleTasks != null) {
            Task itemPeachTask = this.pausedScheduleTasks.poll();
            while (itemPeachTask != null) {
                Scheduler.getInstance().scheduleTask(itemPeachTask);
                itemPeachTask = this.pausedScheduleTasks.poll();
            }
        }

        // 暂停的流媒体任务，调整延迟时间
        List<AICallbackTask> onPauseTasks = this.root.getOnPauseTasks();
        for (AICallbackTask aiCallbackTask : onPauseTasks) {
            aiCallbackTask.delayPreviousTime(duration);
        }

        // 恢复的流媒体任务，调整延迟时间
        List<AICallbackTask> onResumeTasks = this.root.getOnResumeTasks();
        for (AICallbackTask aiCallbackTask : onResumeTasks) {
            aiCallbackTask.delayPreviousTime(duration);
            aiCallbackTask.execute(this.startExecuteTime, false);
        }

        // stop的流媒体任务，调整延迟时间
        List<AICallbackTask> onStopTasks = this.root.getOnStopTasks();
        for (AICallbackTask aiCallbackTask : onStopTasks) {
            aiCallbackTask.delayPreviousTime(duration);
        }
    }

    // 快速结束
    public void stopMedia() {
        // 只要执行过，就需要判定结束
        if (this.hasExecuted) {
            List<AICallbackTask> onStopTasks = this.root.getOnStopTasks();
            for (AICallbackTask aiCallbackTask: onStopTasks) {
                // 强制结束
                aiCallbackTask.execute(this.startExecuteTime, true);
            }
        }
    }

    public void setHasExecuted(boolean hasExecuted) {
        this.hasExecuted = hasExecuted;
    }

    public boolean isHasExecuted() {
        return hasExecuted;
    }

    public boolean isHasExecutedDone() {
        return hasExecutedDone;
    }

    public void setHasExecutedDone(boolean hasExecutedDone) {
        this.hasExecutedDone = hasExecutedDone;
    }

    public int getPriority() {
        return priority;
    }
}
