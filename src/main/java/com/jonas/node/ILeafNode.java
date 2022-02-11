package com.jonas.node;

import com.jonas.util.Env;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public abstract class ILeafNode extends IBaseBehaviour {

    public static final boolean TEST_MOD = false;

    // 绑定的设备id
    protected String bindEquipId;
    // 绑定设备的actorId
    protected String bindUniqueActorId;
    protected String bindEquipType;
    protected String bindModelId;

    /**
     * 配置delay
     */
    protected int delay = 0;
    /**
     * 配置的是否允许skip
     */
    protected boolean allowSkip = true;

    protected Map<String, Object> configs = new HashMap<>();
    private boolean hasOffsetConfig = false;
    protected Map<String, Object> offsetConfig = new HashMap<>();

    // 执行reset的Node
    private IBehaviour resetLeafNode = null;

    @Override
    public void trigger(String equipId, String modelId, Map<String, Object> params) {
        this.checkEquipId();
    }

    @Override
    public EStatus tick(IBehaviour root, boolean fromWeb) {
        this.checkEquipId();
        EStatus result = this.onTick(root, this.configs, fromWeb);
        if (result == EStatus.SUCCESS) {
            isExecuted = true;
        }
        return result;
    }

    @Override
    public EStatus offsetTick(IBehaviour root, boolean fromWeb) {
        // 如果有配置resetLeafNode，优先执行
        if (this.resetLeafNode != null) {
            this.checkEquipId();
            this.resetLeafNode.reset(false);
            isExecuted = false;
        } else {
            // 没有配置相反执行节点的，不会重置节点状态
            if (this.hasOffsetConfig) {
                this.checkEquipId();
                EStatus result = this.onTick(root, this.offsetConfig, fromWeb);
                if (result == EStatus.SUCCESS) {
                    isExecuted = false;
                }
                return result;
            }
        }
        return EStatus.SUCCESS;
    }

    /**
     * 同步设备id
     * 兼容启动后新注册的设备
     */
    private void checkEquipId() {
        if (StringUtils.isEmpty(this.bindEquipId) && !StringUtils.isEmpty(this.bindUniqueActorId)) {
//            PeachEquipContainer bindContainer = EquipmentManager.getInstance().getPeachEquipmentByActorId(this.bindUniqueActorId);
//            if (bindContainer != null) {
//                this.bindEquipId = bindContainer.getDeviceId();
//            }
        }
    }

    /**
     * 带参数的tick
     *
     * @param root
     * @param configParams
     * @return
     */
    protected EStatus onTick(IBehaviour root, Map<String, Object> configParams, boolean fromWeb) {
        return EStatus.SUCCESS;
    }

    // 设置装备
    public void bindEquipment(String equipId) {
        this.bindEquipId = equipId;
    }

    // 设置设备
    public void bindEquipActorId(String actorId) {
        this.bindUniqueActorId = actorId;
    }

    public void bindEquipType(String equipType) {
        this.bindEquipType = equipType;
    }

    public void bindDelay(int delay) {
        // 毫秒转tick
        delay = Math.max(delay / Env.TICK_TIME, 0);
        this.delay = delay;
    }

    /**
     * 是否允许skip
     *
     * @param allowSkip
     */
    public void bindAllowSkip(boolean allowSkip) {
        this.allowSkip = allowSkip;
    }

    public void bindModelId(String bindModelId) {
        this.bindModelId = bindModelId;
    }

    // 覆盖
    public void bindResetLeafNode(IBehaviour resetNode) {
        this.resetLeafNode = resetNode;
    }

    public String getBindEquipId() {
        return bindEquipId;
    }

    public String getBindModelId() {
        return bindModelId;
    }

    protected boolean isDelay() {
        return (this.delay + this.preRuntimeDelay / Env.TICK_TIME) > 0;
    }

    protected int getRealDelay() {
        int newDelay = this.delay + this.preRuntimeDelay / Env.TICK_TIME;
        if (newDelay < 0) {
            newDelay = 0;
        }
        return newDelay;
    }

    @Override
    public void config(String key, Object val) {
        this.configs.putIfAbsent(key, val);
        // 如果不存在，则写入
        this.offsetConfig.putIfAbsent(key, val);
    }

    @Override
    public void offsetConfig(String key, Object val) {
        // 覆盖写入
        this.offsetConfig.put(key, val);
        this.hasOffsetConfig = true;
    }

    public Map<String, Object> getConditionConfigs() {
        return configs;
    }
}
