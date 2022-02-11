package com.jonas.leaf;

import com.jonas.node.EStatus;
import com.jonas.node.IBehaviour;
import com.jonas.node.ILeafNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 打印叶子节点，主要用于测试
 *
 * @author jonas
 * @createTime 2021/4/7 15:58
 */
public class PrintLeaf extends ILeafNode {

    private static final Logger logger = LoggerFactory.getLogger(PrintLeaf.class);

    @Override
    public void onInitialize() {
    }

    @Override
    public EStatus onTick(IBehaviour root, Map<String, Object> configParams, boolean fromWeb) {
        String value = (String) configParams.getOrDefault("value", "");
        System.out.println(String.format("[PrintLeaf] value is '%s'", value));
        return EStatus.SUCCESS;
    }

}
