package com.jonas.leaf;

import com.jonas.node.EStatus;
import com.jonas.node.IBehaviour;
import com.jonas.node.ILeafNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 行为树中配置复位的叶子节点
 * 主要用于复位某个叶子节点
 * @author zhongxiaofeng
 * @createTime 2021/6/29 16:48
 */
public class ResetTreeLeaf extends ILeafNode {

    private static final Logger logger = LoggerFactory.getLogger(ResetTreeLeaf.class);

    @Override
    public void onInitialize() {
    }

    @Override
    public void trigger(String equipId, String modelId, Map<String, Object> params) {
    }

    @Override
    public EStatus tick(IBehaviour root) {
        return EStatus.SUCCESS;
    }

    @Override
    public void reset(boolean onlyStatus) {
        super.reset(onlyStatus);
        if (TEST_MOD) {
            logger.info("处理ResetTreeLeaf reset时，设备列表：{},{}, {}, {}",
                    this.bindEquipId, this.bindUniqueActorId, this.bindModelId, this.configs.toString());
            return;
        }
        // 如果仅仅是reset状态，则下面的逻辑不执行
        if (onlyStatus) {
            return;
        }
    }
}
