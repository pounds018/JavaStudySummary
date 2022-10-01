package cn.pounds.elegant.log.parse.func.impl;

import com.google.common.collect.Lists;
import cn.pounds.elegant.log.config.OpLogProperty;
import cn.pounds.elegant.log.config.LogRecordProperties;
import cn.pounds.elegant.log.parse.func.IDiffItemsToLogContentService;
import cn.pounds.elegant.log.parse.func.IFunctionService;
import de.danielbechler.diff.node.DiffNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author muzhantong
 * create on 2022/1/3 8:52 下午
 */
public class DefaultDiffItemsToLogContentService implements IDiffItemsToLogContentService {
	private final static Logger logger = LoggerFactory.getLogger(DefaultDiffItemsToLogContentService.class);

	private final IFunctionService functionService;
	private final LogRecordProperties logRecordProperties;

	@Override
	public String toLogContent(DiffNode diffNode, final Object sourceObject, final Object targetObject) {
		if (!diffNode.hasChanges()) {
			return "";
		}
		StringBuilder stringBuilder = new StringBuilder();
		diffNode.visit((node, visit) -> {
			generateAllFieldLog(sourceObject, targetObject, stringBuilder, node);
		});
		return stringBuilder.toString().replaceAll(logRecordProperties.getFieldSeparator().concat("$"), "");
	}

	private void generateAllFieldLog(Object sourceObject, Object targetObject, StringBuilder stringBuilder, DiffNode node) {
		if (node.isRootNode()) {
			return;
		}
		OpLogProperty opLogPropertyAnnotation = node.getFieldAnnotation(OpLogProperty.class);
		if (opLogPropertyAnnotation == null || node.getValueTypeInfo() != null) {
			//自定义对象类型直接进入对象里面, diff
			return;
		}
		String filedLogName = getFieldLogName(node, opLogPropertyAnnotation);
		if (StringUtils.isEmpty(filedLogName)) {
			return;
		}
		//是否是List类型的字段
		boolean valueIsCollection = valueIsCollection(node, sourceObject, targetObject);
		//获取值的转换函数
		DiffNode.State state = node.getState();
		String logContent = getDiffLogContent(filedLogName, node, state, sourceObject, targetObject, opLogPropertyAnnotation.function(), valueIsCollection);
		if (!StringUtils.isEmpty(logContent)) {
			stringBuilder.append(logContent).append(logRecordProperties.getFieldSeparator());
		}
	}

	private String getFieldLogName(DiffNode node, OpLogProperty opLogPropertyAnnotation) {
		String filedLogName = opLogPropertyAnnotation.name();
		if (node.getParentNode() != null) {
			//获取对象的定语：比如：创建人的ID
			filedLogName = getParentFieldName(node) + filedLogName;
		}
		return filedLogName;
	}

	private boolean valueIsCollection(DiffNode node, Object sourceObject, Object targetObject) {
		if (sourceObject != null) {
			Object sourceValue = node.canonicalGet(sourceObject);
			if (sourceValue == null) {
				if (targetObject != null) {
					return node.canonicalGet(targetObject) instanceof Collection;
				}
			}
			return sourceValue instanceof Collection;
		}
		return false;
	}

	private String getParentFieldName(DiffNode node) {
		DiffNode parent = node.getParentNode();
		String fieldNamePrefix = "";
		while (parent != null) {
			OpLogProperty opLogPropertyAnnotation = parent.getFieldAnnotation(OpLogProperty.class);
			if (opLogPropertyAnnotation == null) {
				//父节点没有配置名称，不拼接
				parent = parent.getParentNode();
				continue;
			}
			fieldNamePrefix = opLogPropertyAnnotation.name().concat(logRecordProperties.getOfWord()).concat(fieldNamePrefix);
			parent = parent.getParentNode();
		}
		return fieldNamePrefix;
	}

	public String getDiffLogContent(String filedLogName, DiffNode node, DiffNode.State state, Object sourceObject, Object targetObject, String functionName, boolean valueIsCollection) {
		//集合走单独的diff模板
		if (valueIsCollection) {
			Collection<Object> sourceList = getListValue(node, sourceObject);
			Collection<Object> targetList = getListValue(node, targetObject);
			Collection<Object> addItemList = listSubtract(targetList, sourceList);
			Collection<Object> delItemList = listSubtract(sourceList, targetList);
			String listAddContent = listToContent(functionName, addItemList);
			String listDelContent = listToContent(functionName, delItemList);
			return logRecordProperties.formatList(filedLogName, listAddContent, listDelContent);
		}
		switch (state) {
			case ADDED:
				return logRecordProperties.formatAdd(filedLogName, getFunctionValue(getFieldValue(node, targetObject), functionName));
			case CHANGED:
				return logRecordProperties.formatUpdate(filedLogName, getFunctionValue(getFieldValue(node, sourceObject), functionName), getFunctionValue(getFieldValue(node, targetObject), functionName));
			case REMOVED:
				return logRecordProperties.formatDeleted(filedLogName, getFunctionValue(getFieldValue(node, sourceObject), functionName));
			default:
				logger.warn("diff log not support");
				return "";

		}
	}

	private Collection<Object> getListValue(DiffNode node, Object object) {
		Object fieldSourceValue = getFieldValue(node, object);
		//noinspection unchecked
		return fieldSourceValue == null ? Lists.newArrayList() : (Collection<Object>) fieldSourceValue;
	}

	private Collection<Object> listSubtract(Collection<Object> minuend, Collection<Object> subTractor) {
		Collection<Object> addItemList = new ArrayList<>(minuend);
		addItemList.removeAll(subTractor);
		return addItemList;
	}

	private String listToContent(String functionName, Collection<Object> addItemList) {
		StringBuilder listAddContent = new StringBuilder();
		if (!CollectionUtils.isEmpty(addItemList)) {
			for (Object item : addItemList) {
				listAddContent.append(getFunctionValue(item, functionName)).append(logRecordProperties.getListItemSeparator());
			}
		}
		return listAddContent.toString().replaceAll(logRecordProperties.getListItemSeparator() + "$", "");
	}

	private String getFunctionValue(Object canonicalGet, String functionName) {
		if (StringUtils.isEmpty(functionName)) {
			return canonicalGet.toString();
		}
		return functionService.apply(functionName, canonicalGet.toString());
	}

	private Object getFieldValue(DiffNode node, Object o2) {
		return node.canonicalGet(o2);
	}

	public DefaultDiffItemsToLogContentService(IFunctionService functionService, LogRecordProperties logRecordProperties) {
		this.functionService = functionService;
		this.logRecordProperties = logRecordProperties;
	}
}
