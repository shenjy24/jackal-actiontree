package com.jonas;

import com.jonas.action.AIConfigLoader;
import com.jonas.action.ActionTree;
import com.jonas.action.ActionTreeContainer;
import org.junit.Before;
import org.junit.Test;

public class AppTest {

    private static final String actionName = "action1";

    @Before
    public void prepare() {
        AIConfigLoader.getInstance().loadConfig(actionName);
    }

    @Test
    public void testActionTree() {
        ActionTree actionTree = ActionTreeContainer.getInstance().getActionTree(actionName);
        if (null != actionTree) {
            Thread thread = new Thread(() -> actionTree.execute());
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
