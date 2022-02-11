package com.jonas.node;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * @author shenjy
 * @createTime 2021/9/1 14:03
 */
public abstract class IBaseBehaviour implements IBehaviour{

    // 查找id，有id的才会在页面中显示
    protected String id;
    // 按钮显示名字
    protected String showName;
    // 绑定房间
    protected String bindRoomName = null;
    // 是否执行
    protected boolean isExecuted = false;

    /**
     * runtime的delay, 单位是毫秒
     */
    protected int preRuntimeDelay = 0;

    // 作为复合node的子node，是否需要判断其节点的执行状态
    private boolean isAffectParentUIStatus = true;

    @Override
    public EStatus tick(IBehaviour root) {
        return this.tick(root, false);
    }

    @Override
    public EStatus offsetTick(IBehaviour root) {
        return this.offsetTick(root, false);
    }

    @Override
    public void reset(boolean onlyStatus) {
        this.preRuntimeDelay = 0;
        this.isExecuted = false;
    }

    /**
     * 绑定id和显示名字
     * @param id
     * @param showName
     */
    public void bindId(String id, String showName) {
        if (StringUtils.isEmpty(id)) {
            return;
        }
        if (StringUtils.isEmpty(showName)) {
            showName = id;
        }
        this.id = id;
        this.showName = showName;
    }

    public String getId() {
        return id;
    }
    public String getShowName() {
        return showName;
    }
    public String getBindRoomName() {
        return bindRoomName;
    }
    public void setBindRoomName(String bindRoomName) {
        if (StringUtils.isEmpty(bindRoomName)) {
            return;
        }
        this.bindRoomName = bindRoomName;
    }

    public void setPreRuntimeDelay(int runtimeDelay) {
        this.preRuntimeDelay = runtimeDelay;
    }


    public boolean isExecuted() {
        return isExecuted;
    }

    public void setExecuted(boolean executed) {
        isExecuted = executed;
    }

    public boolean isAffectParentUIStatus() {
        return isAffectParentUIStatus;
    }

    public void setAffectParentUIStatus(boolean affectParentUIStatus) {
        isAffectParentUIStatus = affectParentUIStatus;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("showName", showName)
                .append("bindRoomName", bindRoomName)
                .append("isExecuted", isExecuted)
                .toString();
    }
}
