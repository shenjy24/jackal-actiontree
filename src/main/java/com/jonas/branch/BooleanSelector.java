package com.jonas.branch;

import com.jonas.node.EStatus;
import com.jonas.node.IBehaviour;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 如果子项有3个，如果第一个执行结果为真，则执行第二个子项，并返回其执行结果。如果第一个执行结果为假，则执行第三个子项，并返回其执行结果。
 * 可看做Boolean表达式。 boolean result =  [child1 expression] ? [child2 expression] : [child3 expression]
 *
 * 如果子项有2个，如果第一个执行结果为真，则执行第二个子项，并返回其执行结果。如果第一个执行结果为假，则直接返回EStatus.SUCCESS。
 * 可看做Boolean表达式。 boolean result =  [child1 expression] ? [child2 expression] : true
 *
 * 如果子项数量，为其他数值，返回EStatus.FAILURE。
 *
 * @author zhongxiaofeng
 * @createTime 2021/10/14 11:26
 */
public class BooleanSelector extends BaseSelector{

    private static final Logger logger = LoggerFactory.getLogger(BooleanSelector.class);

    @Override
    public EStatus tick(IBehaviour root, boolean fromWeb) {
        this.handTickBehaviours.clear();
        int childrenLen = super.children.size();
        if (childrenLen <= 1 || childrenLen > 3) {
            logger.error("BooleanSelector child length is not match, so the return value is false!");
            return EStatus.FAILURE;
        } else if (childrenLen == 3) {
            EStatus status = super.children.get(0).tick(root, fromWeb);
            if (status == EStatus.SUCCESS) {
                return super.children.get(1).tick(root, fromWeb);
            } else {
                return super.children.get(2).tick(root, fromWeb);
            }
        } else {
            EStatus status = super.children.get(0).tick(root, fromWeb);
            if (status == EStatus.SUCCESS) {
                return super.children.get(1).tick(root, fromWeb);
            } else {
                return EStatus.SUCCESS;
            }
        }
    }
}
