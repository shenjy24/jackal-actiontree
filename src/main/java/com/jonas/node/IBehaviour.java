package com.jonas.node;

import java.util.Map;

public interface IBehaviour {

    /**
     * 节点初始化后的操作
     */
    void onInitialize();

    // 节点被触发时候调用
    void trigger(String equipId, String modelId, Map<String, Object> params);

    /**
     * Tick决策树的操作
     *
     * @return
     */
    EStatus tick(IBehaviour root);
    EStatus tick(IBehaviour root, boolean fromWeb);
    EStatus offsetTick(IBehaviour root);
    EStatus offsetTick(IBehaviour root, boolean fromWeb);

    /**
     * 配置节点
     *
     * @param key
     * @param val
     */
    default void config(String key, Object val) {
    }

    /**
     * 配置反节点
     * @param key
     * @param val
     */
    default void offsetConfig(String key, Object val) {
    }

    // 节点被恢复
    void reset(boolean onlyStatus);
}
