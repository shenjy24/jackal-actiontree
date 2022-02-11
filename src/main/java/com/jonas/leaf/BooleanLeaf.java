package com.jonas.leaf;

import com.jonas.node.EStatus;
import com.jonas.node.IBehaviour;
import com.jonas.node.ILeafNode;

import java.util.Map;

/**
 * BooleanLeaf
 *
 * @author shenjy
 * @version 1.0
 * @date 2022-02-11
 */
public class BooleanLeaf extends ILeafNode {
    @Override
    public void onInitialize() {
    }

    @Override
    protected EStatus onTick(IBehaviour root, Map<String, Object> configParams, boolean fromWeb) {
        boolean value = (boolean) configParams.getOrDefault("value", false);
        return value ? EStatus.SUCCESS : EStatus.FAILURE;
    }
}
