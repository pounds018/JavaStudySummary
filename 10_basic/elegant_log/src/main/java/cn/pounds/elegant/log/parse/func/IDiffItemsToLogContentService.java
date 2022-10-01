package cn.pounds.elegant.log.parse.func;

import de.danielbechler.diff.node.DiffNode;

/**
 * @author: pounds
 * @date: 2022/2/26 20:40
 * @desc:
 */
public interface IDiffItemsToLogContentService {

	String toLogContent(DiffNode diffNode, final Object o1, final Object o2);
}

