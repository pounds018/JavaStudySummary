package cn.pounds.elegant.log.parse.core;


import cn.pounds.elegant.log.parse.func.ParseFunctionFactory;
import cn.pounds.elegant.log.parse.func.impl.DiffParseFunction;
import com.google.common.base.Strings;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: pounds
 * @date: 2022/2/20 23:02
 * @desc:
 */
@Component
public class LogRecordValueParser implements BeanFactoryAware {
	/**
	 * 正则表达含义: #{[0次或者多次空白字符`\f\n\r\t\v`等][0次或者多次包括下划线的任何单词字符][0次或者多次空白字符`\f\n\r\t\v`等][0次或者一次'(']
	 * [0次或者多次除“\r”“\n”之外的任何单个字符][0次或者1次')']}
	 */
	protected static final Pattern PATTERN = Pattern.compile("#\\{\\s*(\\w*)\\s*\\(?(.*?)\\)?}");
	protected static final String COMMA = ",";

	protected BeanFactory beanFactory;

	protected LogRecordExpressionEvaluator expressionEvaluator = new LogRecordExpressionEvaluator();

	protected LogFunctionParser logFunctionParser;

	protected DiffParseFunction diffParseFunction;

	@Resource
	protected ParseFunctionFactory parseFunctionFactory;

	public static int strCount(String srcText, String findText) {
		int count = 0;
		int index = 0;
		while ((index = srcText.indexOf(findText, index)) != -1) {
			index = index + findText.length();
			count++;
		}
		return count;
	}

	/**
	 *
	 * @param templates --- SpEL模板
	 * @param result --- 业务方法执行结果
	 * @param target --- 业务方法
	 * @param targetClass --- 业务方法所在类
	 * @param method --- 业务方法
	 * @param args --- 业务方法参数
	 * @param errorMsg --- 错误信息
	 * @param beforeFunctionNameAndReturnMap --- 前置方法名称结果map{keytemplate -> returnVal}
	 * @return
	 */
	public Map<String, String> parseTemplates(Collection<String> templates,
											  Object result,
											  Object target,
											  Class<?> targetClass,
											  Method method,
											  Object[] args,
											  String errorMsg,
											  Map<String, String> beforeFunctionNameAndReturnMap) {
		Map<String, String> expressionValues = new HashMap<>();
		// 构建解析上下文
		EvaluationContext evalContext = expressionEvaluator.createEvaluationContext(method, args, target,
				targetClass, result, errorMsg, beanFactory, parseFunctionFactory);

		for (String expressionTemplate : templates) {
			AnnotatedElementKey annotatedElementKey = new AnnotatedElementKey(method, targetClass);
			Object parseExpression = expressionEvaluator.parseExpression(expressionTemplate, annotatedElementKey, evalContext);
			expressionValues.put(expressionTemplate, parseExpression.toString());

			// if (expressionTemplate.contains("#{")) {
			// 	Matcher matcher = pattern.matcher(expressionTemplate);
			// 	StringBuffer parsedStr = new StringBuffer();
			// 	AnnotatedElementKey annotatedElementKey = new AnnotatedElementKey(method, targetClass);
			// 	while (matcher.find()) {
			//
			// 		String expression = matcher.group(2);
			// 		String functionName = matcher.group(1);
			// 		if (DiffParseFunction.diffFunctionName.equals(functionName)) {
			// 			expression = getDiffFunctionValue(evalContext, annotatedElementKey, expression);
			// 		} else {
			// 			Object value = expressionEvaluator.parseExpression(expression, annotatedElementKey, evalContext);
			// 			expression = logFunctionParser.getFunctionReturnValue(beforeFunctionNameAndReturnMap, value, expression, functionName);
			// 		}
			// 		matcher.appendReplacement(parsedStr, Matcher.quoteReplacement(Strings.nullToEmpty(expression)));
			// 	}
			// 	matcher.appendTail(parsedStr);
			// 	expressionValues.put(expressionTemplate, parsedStr.toString());
			// } else {
			// 	expressionValues.put(expressionTemplate, expressionTemplate);
			// }

		}
		return expressionValues;
	}

	private String getDiffFunctionValue(EvaluationContext evaluationContext, AnnotatedElementKey annotatedElementKey, String expression) {
		String[] params = parseDiffFunction(expression);
		if (params.length == 1) {
			Object targetObj = expressionEvaluator.parseExpression(params[0], annotatedElementKey, evaluationContext);
			expression = diffParseFunction.diff(targetObj);
		} else if (params.length == 2) {
			Object sourceObj = expressionEvaluator.parseExpression(params[0], annotatedElementKey, evaluationContext);
			Object targetObj = expressionEvaluator.parseExpression(params[1], annotatedElementKey, evaluationContext);
			expression = diffParseFunction.diff(sourceObj, targetObj);
		}
		return expression;
	}

	private String[] parseDiffFunction(String expression) {
		if (expression.contains(COMMA) && strCount(expression, COMMA) == 1) {
			return expression.split(COMMA);
		}
		return new String[]{expression};
	}


	@Override
	public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	public void setLogFunctionParser(LogFunctionParser logFunctionParser) {
		this.logFunctionParser = logFunctionParser;
	}

	public void setDiffParseFunction(DiffParseFunction diffParseFunction) {
		this.diffParseFunction = diffParseFunction;
	}
}
