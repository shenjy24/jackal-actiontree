package com.jonas.leaf;

import com.jonas.branch.RootSequence;
import com.jonas.node.EStatus;
import com.jonas.node.IBehaviour;
import com.jonas.node.ILeafNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 叶子节点，判断条件是否满足
 * @author zhongxiaofeng
 * @createTime 2021/4/7 15:53
 */
public class ConditionLeaf extends ILeafNode {

    private static final Logger logger = LoggerFactory.getLogger(ConditionLeaf.class);

    private int progress = 0;

    private int total = 1;

    // 是否校验当前的数值
    private boolean checkCurrent = false;

    private boolean checkChange = true;

    @Override
    public void onInitialize() {
    }

    @Override
    public void trigger(String equipId, String modelId, Map<String, Object> inputs) {
        super.trigger(equipId, modelId, inputs);
    }

    @Override
    public void reset(boolean onlyStatus) {
        super.reset(onlyStatus);
        this.progress = 0;
    }

    @Override
    public EStatus onTick(IBehaviour root, Map<String, Object> configParams, boolean fromWeb) {
        RootSequence rootSequence = (RootSequence) root;
        if (this.allowSkip && rootSequence.isSkipCondition()) {
            logger.warn("[{}]由于是跳过导致的触发，条件限制无效果，equipId:{}, actorId:{}, modelId:{}",
                    rootSequence.getTreeId(), this.bindEquipId, this.bindUniqueActorId, this.bindModelId);
            return EStatus.SUCCESS;
        }
        // 需要检测到有变化
        if (this.checkChange && this.progress < this.total) {
            return EStatus.FAILURE;
        } else if (StringUtils.isEmpty(this.bindEquipType) && StringUtils.isEmpty(this.bindEquipId)) {
            return EStatus.FAILURE;
        }

        // 如果是绑定类型的，默认是返回成功的
        if (!StringUtils.isEmpty(this.bindEquipType)) {
            return EStatus.SUCCESS;
        } else {
            return EStatus.FAILURE;
        }
    }

    public void bindTotalProgress(int total) {
        this.total = total;
    }

    public void bindCheckCurrent(boolean checkCurrent) {
        this.checkCurrent = checkCurrent;
    }

    public void bindCheckChange(boolean checkChange) {
        this.checkChange = checkChange;
    }
}
