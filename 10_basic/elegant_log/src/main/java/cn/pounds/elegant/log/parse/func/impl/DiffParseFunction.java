package cn.pounds.elegant.log.parse.func.impl;

import cn.pounds.elegant.log.parse.context.LogRecordContext;
import cn.pounds.elegant.log.parse.func.IDiffItemsToLogContentService;
import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

/**
 * @author: pounds
 * @date: 2022/2/26 20:54
 * @desc:
 */
public class DiffParseFunction {
	private final static Logger logger = LoggerFactory.getLogger(DiffParseFunction.class);

	public static final String diffFunctionName = "_DIFF";
	public static final String OLD_OBJECT = "_oldObj";

	private static IDiffItemsToLogContentService diffItemsToLogContentService;

	//@Override
	public String functionName() {
		return diffFunctionName;
	}

	//@Override
	public String diff(Object source, Object target) {
		if (source == null && target == null) {
			return "";
		}
		if (source == null || target == null) {
			try {
				Class<?> clazz = source == null ? target.getClass() : source.getClass();
				source = source == null ? clazz.getDeclaredConstructor().newInstance() : source;
				target = target == null ? clazz.getDeclaredConstructor().newInstance() : target;
			} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}
		if (!Objects.equals(source.getClass(), target.getClass())) {
			logger.error("diff的两个对象类型不同, source.class={}, target.class={}", source.getClass().toString(), target.getClass().toString());
			return "";
		}
		DiffNode diffNode = ObjectDifferBuilder.buildDefault().compare(target, source);
		return diffItemsToLogContentService.toLogContent(diffNode, source, target);
	}

	public String diff(Object newObj) {
		Object oldObj = LogRecordContext.getVariable(OLD_OBJECT);
		return diff(oldObj, newObj);
	}

	public void setDiffItemsToLogContentService(IDiffItemsToLogContentService diffItemsToLogContentService) {
		DiffParseFunction.diffItemsToLogContentService = diffItemsToLogContentService;
	}
}
