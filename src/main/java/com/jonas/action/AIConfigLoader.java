package com.jonas.action;

import com.jonas.branch.LoopSequence;
import com.jonas.leaf.ConditionLeaf;
import com.jonas.node.IBranchNode;
import com.jonas.node.ILeafNode;
import com.jonas.util.PlaceHolder;
import com.jonas.util.xml.ViewNode;
import com.jonas.util.xml.XmlParser;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class AIConfigLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIConfigLoader.class);

    private static AIConfigLoader instance = new AIConfigLoader();

    public static AIConfigLoader getInstance() {
        return instance;
    }

    // 管理所有的branch节点
    private Map<String, Class<? extends IBranchNode>> branchClassMap = new HashMap<>();

    // 管理所有的叶子节点
    private Map<String, Class<? extends ILeafNode>> leafClassMap = new HashMap<>();

    private boolean isLoadCls = false;

    /**
     * 私有化构造函数
     */
    private AIConfigLoader() {
    }

    /**
     * 加载策略的配置
     */
    public void loadConfig(String actionName) {
        try {
            this.loadClass();
        } catch (Exception e) {
            LOGGER.error("Fail to load ai class", e);
        }

        // 读取配置文件
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(String.format("%s.xml", actionName));
        if (null == inputStream) {
            LOGGER.error("Fail to load ai config, the actionName[{}] is not existed!", actionName);
            return;
        }

        // 读取节点信息
        ActionTree actionTree = null;
        try {
            actionTree = this.parseConfigData(actionName, inputStream);
            if (actionTree != null) {
                ActionTreeContainer.getInstance().addActionTree(actionName, actionTree);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 管理所有的的节点class
     */
    private void loadClass() {
        if (this.isLoadCls) {
            return;
        }
        this.isLoadCls = true;
        Reflections reflections = new Reflections("com.jonas");
        for (Class<? extends IBranchNode> clazz : reflections.getSubTypesOf(IBranchNode.class)) {
            this.branchClassMap.put(clazz.getSimpleName(), clazz);
        }
        for (Class<? extends ILeafNode> clazz : reflections.getSubTypesOf(ILeafNode.class)) {
            this.leafClassMap.put(clazz.getSimpleName(), clazz);
        }
    }

    /**
     * 根据文件解析行为树
     *
     * @param configData
     * @throws Exception
     */
    private ActionTree parseConfigData(String actionName, InputStream configData) throws Exception {
        ViewNode configNode = XmlParser.parse(configData);
        String rootName = configNode.getViewName();
        // 非Root开头的标签
        if (rootName == null || !rootName.equals("Root")) {
            return null;
        }

        String id = configNode.getAttributes().get("id");
        // 作用域
        String scope = configNode.getAttributes().get("scope");
        if (scope == null) {
            scope = TreeScope.ONCE.getScope();
        }
        // 配置了该字段，在跳过列表中，多这个选项
        String showName = configNode.getAttributes().get("showName");
        // 配置了该字段，页面上会多个跳过按钮
        String name = configNode.getAttributes().get("name");
        String priorityStr = configNode.getAttributes().getOrDefault("priority", "0");
        int priority = Integer.parseInt(priorityStr);

        // 创建行为树构造器
        ActionTreeBuilder actionTreeBuilder = ActionTreeBuilder.create();
        // 读取节点信息
        for (ViewNode childNode : configNode.getChildren()) {
            this.parseConfigNode(actionName, actionTreeBuilder, childNode, id, 0);
        }

        // 编译行为树
        return actionTreeBuilder.build(id, name, scope, showName, priority);
    }

    private void parseBranchNode(String actionName, ActionTreeBuilder builder, ViewNode configNode,
                                 String treeId, int parentDelay) throws IllegalAccessException, InstantiationException {
        // 检索是否有该节点
        Class<? extends IBranchNode> branchNode = this.branchClassMap.get(configNode.getAttributes().get("type"));
        if (branchNode != null) {
            // ----------控制相关的参数-------------------
            // 读取权重
            String weightStr = configNode.getAttributes().get("weight");
            int weight = 1;
            if (weightStr != null) {
                weight = Integer.parseInt(weightStr);
            }
            // 设置的延迟时间
            String delayStr = configNode.getAttributes().get("delay");
            int delay = 0;
            if (!StringUtils.isEmpty(delayStr)) {
                try {
                    delay = (int) this.parseConfigVal(actionName, delayStr);
                } catch (Exception e) {
                    LOGGER.error("parseConfigVal delay failed!, {}, {}", actionName, treeId, e);
                }
            }
            // 是否是严格模式
            String strictModeStr = configNode.getAttributes().get("strictMode");
            boolean strictMode = true;
            if (!StringUtils.isEmpty(strictModeStr)) {
                try {
                    strictMode = (boolean) this.parseConfigVal(actionName, strictModeStr);
                } catch (Exception e) {
                    LOGGER.error("parseConfigVal strictMode failed!, {}, {}", actionName, treeId, e);
                }
            }
            // 作为子节点，是否影响父节点的UI状态
            String affectParentUIStatusStr = configNode.getAttributes().get("affectParentUIStatus");
            boolean affectParentUIStatus = true;
            if (!StringUtils.isEmpty(affectParentUIStatusStr)) {
                try {
                    affectParentUIStatus = (boolean) this.parseConfigVal(actionName, affectParentUIStatusStr);
                } catch (Exception e) {
                    LOGGER.error("parseConfigVal affectParentUIStatus failed!, {}, {}", actionName, treeId, e);
                }
            }
            // 子对象offset的操作时，是否需要判断正执行时，触发过的子节点，默认是需要
            String totalOffsetStr = configNode.getAttributes().get("totalOffset");
            boolean totalOffset = false;
            if (!StringUtils.isEmpty(totalOffsetStr)) {
                try {
                    totalOffset = (boolean) this.parseConfigVal(actionName, totalOffsetStr);
                } catch (Exception e) {
                    LOGGER.error("parseConfigVal totalOffset failed!, {}, {}", actionName, treeId, e);
                }
            }

            // ----------中控面板相关的参数-------------------
            // 配置的时候，只要同一个树不一致就行，只有配置了以下id和name，才会出现可控制列表中
            String leafId = configNode.getAttributes().get("id");
            String leafName = configNode.getAttributes().get("name");
            if (!StringUtils.isEmpty(leafId)) {
                leafId = treeId + "_" + leafId;
            }
            // 绑定所在的房间
            String bindRoomName = configNode.getAttributes().get("bindRoomName");

            // 绑定id和名字，绑定房间
            IBranchNode iBranchNodeInstance = branchNode.newInstance();
            iBranchNodeInstance.bindId(leafId, leafName);
            iBranchNodeInstance.setBindRoomName(bindRoomName);
            iBranchNodeInstance.setStrictMode(strictMode);
            iBranchNodeInstance.setTotalOffset(totalOffset);
            iBranchNodeInstance.setAffectParentUIStatus(affectParentUIStatus);

            // 循环节点的特殊处理
            if (iBranchNodeInstance instanceof LoopSequence) {
                this.bindLoopFullSequenceParam(actionName, (LoopSequence) iBranchNodeInstance, configNode);
            }

            // 创建分支节点
            builder.branch(iBranchNodeInstance, weight);
            // 递归构造
            List<ViewNode> children = configNode.getChildren();
            if (children != null) {
                for (ViewNode childConfigNode : configNode.getChildren()) {
                    this.parseConfigNode(actionName, builder, childConfigNode, treeId, parentDelay + delay);
                }
            }
            // 封闭节点
            builder.upper();
        } else {
            LOGGER.error("Missing node {}, {}, {}", actionName, treeId, configNode.getAttributes().get("type"));
        }
    }

    private void bindLoopFullSequenceParam(String actionName, LoopSequence iBranchNodeInstance, ViewNode configNode) {
        String maxLoopCountsStr = configNode.getAttributes().get("maxLoopCounts");
        int maxLoopCounts = 1;
        if (maxLoopCountsStr != null) {
            maxLoopCounts = Integer.parseInt(maxLoopCountsStr);
        }
        // 最大循环次数
        iBranchNodeInstance.bindMaxLoopCounts(maxLoopCounts);

        String loopDurationStr = configNode.getAttributes().get("loopDuration");
        int loopDuration = 50;
        if (loopDurationStr != null) {
            loopDuration = (int) parseConfigVal(actionName, loopDurationStr);
        }
        // 循环间隔
        iBranchNodeInstance.bindLoopDuration(loopDuration);
    }

    private void parseLeafNode(String actionName, ActionTreeBuilder builder, ViewNode configNode,
                               String treeId, int parentDelay) throws IllegalAccessException, InstantiationException {
        // 检索是否有该节点
        Class<? extends ILeafNode> leafNode = this.leafClassMap.get(configNode.getAttributes().get("type"));
        // ----------设备相关的参数-------------------
        // 绑定的设备id, 已停用
        String bindEquipId = configNode.getAttributes().get("bindEquipId");
        // 绑定的uniqueActorId, 如果存在bindEquipId
        String bindUniqueActorId = configNode.getAttributes().get("bindUniqueActorId");

        // 绑定的modelId
        String bindModelId = configNode.getAttributes().get("bindModelId");
        // Condition: 触发的设备类型
        String bindEquipType = configNode.getAttributes().get("bindEquipType");

        // ----------控制相关的参数-------------------
        // 设置的延迟时间
        String delayStr = configNode.getAttributes().get("delay");
        int delay = 0;
        if (!StringUtils.isEmpty(delayStr)) {
            try {
                delay = (int) this.parseConfigVal(actionName, delayStr);
            } catch (Exception e) {
                LOGGER.error("parseLeafNode config delay failed!, {}, {}", actionName, treeId, e);
            }
        }
        // 读取节点相关信息
        String weightStr = configNode.getAttributes().get("weight");
        int weight = 1;
        if (weightStr != null) {
            weight = Integer.parseInt(weightStr);
        }
        // 是否允许skip
        String allowSkipStr = configNode.getAttributes().get("allowSkip");
        boolean allowSkip = true;
        if (!StringUtils.isEmpty(allowSkipStr)) {
            try {
                allowSkip = (boolean) this.parseConfigVal(actionName, allowSkipStr);
            } catch (Exception e) {
                LOGGER.error("parseLeafNode config allowSkip failed!, {}, {}", actionName, treeId, e);
            }
        }
        // 作为子节点，是否影响父节点的UI状态
        String affectParentUIStatusStr = configNode.getAttributes().get("affectParentUIStatus");
        boolean affectParentUIStatus = true;
        if (!StringUtils.isEmpty(affectParentUIStatusStr)) {
            try {
                affectParentUIStatus = (boolean) this.parseConfigVal(actionName, affectParentUIStatusStr);
            } catch (Exception e) {
                LOGGER.error("parseLeafNode affectParentUIStatus failed!, {}, {}", actionName, treeId, e);
            }
        }

        // ----------中控面板相关的参数-------------------
        // 配置的时候，只要同一个树不一致就行，只有配置了以下id和name，才会出现可控制列表中
        String leafId = configNode.getAttributes().get("id");
        String leafName = configNode.getAttributes().get("name");
        if (!StringUtils.isEmpty(leafId)) {
            leafId = treeId + "_" + leafId;
        }
        // 绑定所在的房间
        String bindRoomName = configNode.getAttributes().get("bindRoomName");
        // 指向reset_leaf
        String resetLeafId = configNode.getAttributes().get("resetLeafId");
        if (!StringUtils.isEmpty(resetLeafId)) {
            resetLeafId = treeId + "_" + resetLeafId;
        }

        // ----------绑定配置参数-------------------
        if (leafNode != null) {
            // 创建分支节点
            ILeafNode leafNodeInstance = leafNode.newInstance();
            this.autoFillParam(leafNodeInstance, actionName);

            // 绑定equipId
            leafNodeInstance.bindEquipment(bindEquipId);
            // 绑定设备的actorId
            leafNodeInstance.bindEquipActorId(bindUniqueActorId);
            // 绑定modelId
            leafNodeInstance.bindModelId(bindModelId);
            // 绑定设备类型
            leafNodeInstance.bindEquipType(bindEquipType);
            // 延迟执行
            if (parentDelay + delay > 0) {
                leafNodeInstance.bindDelay(parentDelay + delay);
            }
            // 是否允许allow
            leafNodeInstance.bindAllowSkip(allowSkip);
            // 是否影响子节点的状态
            leafNodeInstance.setAffectParentUIStatus(affectParentUIStatus);
            // 绑定id和名字，绑定房间
            leafNodeInstance.bindId(leafId, leafName);
            leafNodeInstance.setBindRoomName(bindRoomName);

            // ConditionLeaf的特殊处理
            if (leafNodeInstance instanceof ConditionLeaf) {
                this.bindConditionLeafParam((ConditionLeaf) leafNodeInstance, configNode);
            }

            //
            builder.leaf(bindEquipId, bindUniqueActorId, bindEquipType, bindModelId, leafNodeInstance, weight, leafId, resetLeafId);
            // 读取配置
            List<ViewNode> children = configNode.getChildren();
            if (children != null) {
                for (ViewNode childConfigNode : configNode.getChildren()) {
                    this.parseConfigNode(actionName, builder, childConfigNode, treeId, parentDelay + delay);
                }
            }
        } else {
            LOGGER.error("Missing node , {}, {}, {}", actionName, treeId, configNode.getAttributes().get("type"));
        }
    }

    private void autoFillParam(ILeafNode leafNodeInstance, String actionName) {
        if (leafNodeInstance == null) {
            return;
        }
        // 如果对于MarkThemeCacheLeaf, TriggerTreeLeaf, TriggerResetTreeLeaf. 自动填充themeName，会被覆盖
//        if ((leafNodeInstance instanceof MarkThemeCacheLeaf) ||
//                (leafNodeInstance instanceof StateTimeoutCheckLeaf) ||
//                (leafNodeInstance instanceof TriggerTreeLeaf) ||
//                (leafNodeInstance instanceof TriggerResetTreeLeaf) ||
//                (leafNodeInstance instanceof StateValueLeaf) ||
//                (leafNodeInstance instanceof StateValueCheckLeaf)) {
        leafNodeInstance.config("actionName", actionName);
//        }
    }

    private void bindConditionLeafParam(ConditionLeaf leafNodeInstance, ViewNode configNode) {
        // Condition：触发总进度条
        String totalProgStr = configNode.getAttributes().getOrDefault("totalProgress", "1");
        int totalProg = 1;
        try {
            totalProg = Integer.parseInt(totalProgStr);
        } catch (NumberFormatException nfe) {
            LOGGER.error("bindConditionLeafParam WARN! totalProgress is illegal", nfe);
        }

        // 是否检测当前节点的值
        String checkCurrentStr = configNode.getAttributes().get("checkCurrent");
        boolean checkCurrent = false;
        if (!StringUtils.isEmpty(checkCurrentStr)) {
            checkCurrent = Boolean.parseBoolean(checkCurrentStr);
        }

        // 是否需要检测变化
        String checkChangeStr = configNode.getAttributes().get("checkChange");
        boolean checkChange = true;
        if (!StringUtils.isEmpty(checkChangeStr)) {
            checkChange = Boolean.parseBoolean(checkChangeStr);
        }

        // 绑定总进度条
        leafNodeInstance.bindTotalProgress(totalProg);
        // 是否检测当前的数值
        leafNodeInstance.bindCheckCurrent(checkCurrent);
        // 是否需要检测有变化
        leafNodeInstance.bindCheckChange(checkChange);
    }


    /**
     * 递归解析行为树的节点
     *
     * @param builder
     * @param configNode
     * @throws Exception
     */
    private void parseConfigNode(String actionName, ActionTreeBuilder builder, ViewNode configNode, String treeId, int parentDelay) {
        String nodeType = configNode.getViewName();

        if (nodeType.equals("branch")) {
            try {
                this.parseBranchNode(actionName, builder, configNode, treeId, parentDelay);
            } catch (IllegalAccessException | InstantiationException e) {
                LOGGER.error("parseBranchNode failed, {}, {}", actionName, treeId, e);
            }
        } else if (nodeType.equals("leaf")) {
            try {
                this.parseLeafNode(actionName, builder, configNode, treeId, parentDelay);
            } catch (IllegalAccessException | InstantiationException e) {
                LOGGER.error("parseLeafNode failed, {}, {}", actionName, treeId, e);
            }
        } else if (nodeType.equals("config")) {
            String key = configNode.getAttributes().get("key");
            String val = configNode.getAttributes().get("val");
            builder.config(key, this.parseConfigVal(actionName, val));
        } else if (nodeType.equals("offset")) {
            // 读取配置
            builder.setOffsetConfig(true);
            List<ViewNode> children = configNode.getChildren();
            if (children != null) {
                for (ViewNode childConfigNode : children) {
                    String childNodeType = childConfigNode.getViewName();
                    // offset的子对象，只允许是config
                    if (childNodeType.equals("config")) {
                        this.parseConfigNode(actionName, builder, childConfigNode, treeId, parentDelay);
                    }
                }
            }
            builder.setOffsetConfig(false);
        } else if (nodeType.equals("multi-configs")) {
            // 多配置容器
            String key = configNode.getAttributes().getOrDefault("key", "key");
            if (StringUtils.isEmpty(key)) {
                LOGGER.error("warn: parseConfigNode, the multi-configs key is null! {}, {}", actionName, treeId);
                return;
            }
            String unique_key = key + "_" + UUID.randomUUID().toString();
            // 标记多配置开始
            builder.markMultiConfig(true, unique_key);
            List<ViewNode> children = configNode.getChildren();
            if (children != null) {
                for (ViewNode childConfigNode : children) {
                    String childNodeType = childConfigNode.getViewName();
                    // offset的子对象，只允许是config
                    if (childNodeType.equals("multi-config")) {
                        this.parseConfigNode(actionName, builder, childConfigNode, treeId, parentDelay);
                    }
                }
            }
            // 标记多配置结束
            builder.markMultiConfig(false, unique_key);
        } else if (nodeType.equals("multi-config")) {
            // 多配置单项
            String key = configNode.getAttributes().get("key");
            String val = configNode.getAttributes().get("val");
            builder.multiConfig(key, this.parseConfigVal(actionName, val));
        }
    }

    /**
     * 转值
     *
     * @param val
     * @return
     */
    private Object parseConfigVal(String actionName, String val) {
        if (val.startsWith("str:")) {
            return val.substring(4);
        }
        // ${video:xxxx}, 其中xxxx表示视频的timekey
        val = PlaceHolder.getInstance().quickCompile(actionName, val);

        if (val.matches("\\d+L")) {
            return Long.parseLong(val.substring(0, val.length() - 1));
        } else if (val.matches("\\d+(\\.\\d+)?+F")) {
            return Float.parseFloat(val.substring(0, val.length() - 1));
        } else if (val.matches("\\d+(\\.\\d+)?+D")) {
            return Double.parseDouble(val.substring(0, val.length() - 1));
        } else if (val.matches("\\d+")) {
            return Integer.parseInt(val);
        } else if (val.equalsIgnoreCase("true")) {
            return true;
        } else if (val.equalsIgnoreCase("false")) {
            return false;
        } else {
            return val;
        }
    }
}
