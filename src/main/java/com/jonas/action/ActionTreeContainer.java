package com.jonas.action;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ActionContainer
 *
 * @author shenjy
 * @version 1.0
 * @date 2022-02-11
 */
public class ActionTreeContainer {

    private final Map<String, ActionTree> actionTreeMap = new ConcurrentHashMap<>();

    private final static ActionTreeContainer container = new ActionTreeContainer();

    public static ActionTreeContainer getInstance() {
        return container;
    }

    private ActionTreeContainer() {
    }

    public void addActionTree(String actionName, ActionTree actionTree) {
        actionTreeMap.put(actionName, actionTree);
    }

    public ActionTree getActionTree(String actionName) {
        return actionTreeMap.get(actionName);
    }
}
