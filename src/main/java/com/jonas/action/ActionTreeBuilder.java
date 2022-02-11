package com.jonas.action;

import com.jonas.branch.RootSequence;
import com.jonas.leaf.ActionLeaf;
import com.jonas.leaf.ConditionLeaf;
import com.jonas.leaf.ResetTreeLeaf;
import com.jonas.node.IBehaviour;
import com.jonas.node.IBranchNode;
import com.jonas.node.ILeafNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ActionTreeBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ActionTreeBuilder.class);

    private RootSequence rootSequence;
    private Stack<IBranchNode> operatorCache = new Stack<>();

    private List<String> triggerDevices = new ArrayList<>();

    private List<String> triggerEquipTypes = new ArrayList<>();

    private List<String> triggerActorIds = new ArrayList<>();

    // 缓存需要明确resetId的actionLeaf
    private Map<ILeafNode, String> toBindResetNodeCaches = new HashMap<>();

    // 缓存每一个leafNode
    private Map<String, ILeafNode> allLeafNodes = new HashMap<>();

    // 多配制
    private Map<String, Object> multiConfigDicts;

    private boolean offsetConfig = false;

    /**
     * 创建行为属构造器
     *
     * @return
     */
    public static ActionTreeBuilder create() {
        return new ActionTreeBuilder();
    }

    /**
     * 创建行为树
     *
     */
    private ActionTreeBuilder() {
        this.rootSequence = new RootSequence();
        this.operatorCache.push(this.rootSequence);
    }

    /**
     * 添加枝干节点
     *
     * @param branchNode
     * @param weight
     * @return
     */
    public ActionTreeBuilder branch(IBranchNode branchNode, int weight) {
        this.operatorCache.peek().addChild(branchNode, weight);
        this.operatorCache.push(branchNode);
        return this;
    }

    /**
     * 闭合节点
     */
    public ActionTreeBuilder upper() {
        this.operatorCache.pop();
        return this;
    }

    public void setOffsetConfig(boolean offset) {
        this.offsetConfig = offset;
    }

    /**
     * 标记多配置
     * @param start
     * @param key
     */
    public void markMultiConfig(boolean start, String key) {
        if (start) {
            this.multiConfigDicts = new HashMap<>();
        } else {
            this.config(key, this.multiConfigDicts);
            this.multiConfigDicts = null;
        }
    }

    /**
     * 增加叶节点
     *
     * @param leafNode
     */
    public ActionTreeBuilder leaf(String bindEquipId, String bindUniqueActorId, String bindEquipType, String bindModelId,
                                  ILeafNode leafNode, int weight, String leafId, String resetLeafId) {
        // 如果是ConditionLeaf叶子判断节点
        if (leafNode instanceof ConditionLeaf) {
            if (!StringUtils.isEmpty(bindEquipId)) {
                // 触发的equipId
                String uniKey = String.format("%s|%s", bindEquipId, bindModelId);
                if (!this.triggerDevices.contains(uniKey)) {
                    this.triggerDevices.add(uniKey);
                }
            } else if (!StringUtils.isEmpty(bindEquipType) &&
                    !this.triggerEquipTypes.contains(bindEquipType)) {
                // 触发的EquipType
                this.triggerEquipTypes.add(bindEquipType);
            } else if (!StringUtils.isEmpty(bindUniqueActorId)) {
                // 触发的equipActorId
                String uniKey = String.format("%s|%s", bindUniqueActorId, bindModelId);
                if (!this.triggerActorIds.contains(uniKey)) {
                    this.triggerActorIds.add(uniKey);
                }
            }
        }

        // 如果是actionLeaf，resetLeafId又不为空，缓存起来
        if (leafNode instanceof ActionLeaf && !StringUtils.isEmpty(resetLeafId)) {
            this.toBindResetNodeCaches.put(leafNode, resetLeafId);
        }
        // 缓存每一个叶子节点
        if (!StringUtils.isEmpty(leafId)) {
            this.allLeafNodes.put(leafId, leafNode);
        }
        this.operatorCache.peek().addChild(leafNode, weight);
        return this;
    }


    /**
     * 配置最上层节点
     *
     * @param key
     * @param val
     */
    public ActionTreeBuilder config(String key, Object val) {
        IBranchNode node = this.operatorCache.peek();
        List<IBehaviour> children = node.getChildren();
        if (this.offsetConfig) {
            if (children.size() == 0) {
                node.offsetConfig(key, val);
            } else {
                children.get(children.size() - 1).offsetConfig(key, val);
            }
        } else {
            if (children.size() == 0) {
                node.config(key, val);
            } else {
                children.get(children.size() - 1).config(key, val);
            }
        }

        return this;
    }

    /**
     * 多配置
     * @param key
     * @param val
     * @return
     */
    public ActionTreeBuilder multiConfig(String key, Object val) {
        if (this.multiConfigDicts == null) {
            return this;
        }
        this.multiConfigDicts.put(key, val);
        return this;
    }

    /**
     * 构造行为树
     *
     * @return
     */
    public ActionTree build(String id, String name, String scope, String showName, int priority) {
        // 预处理
        Set<Map.Entry<ILeafNode, String>> entries = toBindResetNodeCaches.entrySet();

        for (Map.Entry<ILeafNode, String> entryMap : entries) {
            String resetLeafId =  entryMap.getValue();
            ILeafNode leafNode = entryMap.getKey();
            ILeafNode targetLeafNode = this.allLeafNodes.get(resetLeafId);
            if (targetLeafNode == null) {
                logger.error("没找到绑定的叶子节点，treeId:{}, leafId:{}, resetLeafId:{}", id, leafNode.getId(), resetLeafId);
                continue;
            } else if (! (targetLeafNode instanceof ResetTreeLeaf)) {
                logger.error("找到绑定的叶子节点, 不是resetTreeLeaf类型，treeId:{}, leafId:{}, resetLeafId:{}", id, leafNode.getId(), resetLeafId);
                continue;
            }
            // 绑定resetName
            leafNode.bindResetLeafNode(targetLeafNode);
        }
        this.rootSequence.onInitialize();
        // rootSequence的treeId，ActionTree的id，必须是一致的
        this.rootSequence.setTreeId(id);
        // 绑定id和名称
        this.rootSequence.bindId(id, name);
        return new ActionTree(this.rootSequence, showName, scope, priority);
    }

}
