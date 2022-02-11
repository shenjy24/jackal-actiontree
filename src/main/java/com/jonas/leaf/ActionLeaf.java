package com.jonas.leaf;

import com.jonas.node.EStatus;
import com.jonas.node.IBehaviour;
import com.jonas.node.ILeafNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 处理触发逻辑的action
 *
 * @author jonas
 * @createTime 2021/4/7 15:58
 */
public class ActionLeaf extends ILeafNode {

    @Override
    public void onInitialize() {
    }

    @Override
    public void trigger(String equipId, String modelId, Map<String, Object> params) {
    }

    @Override
    public void reset(boolean onlyStatus) {
        super.reset(onlyStatus);
    }

    @Override
    public EStatus onTick(IBehaviour root, Map<String, Object> configParams, boolean fromWeb) {
        return EStatus.SUCCESS;
    }

}
