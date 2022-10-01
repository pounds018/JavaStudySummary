package cn.pounds.elegant.log.parse.core;

import cn.pounds.elegant.log.parse.func.IFunctionService;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * @author: pounds
 * @date: 2022/2/26 19:59
 * @desc:
 */
public class LogFunctionParser {

	private final IFunctionService functionService;


	public String getFunctionReturnValue(Map<String, String> beforeFunctionNameAndReturnMap, Object value, String expression, String functionName) {
		if (StringUtils.isEmpty(functionName)) {
			return value.toString();
		}
		String functionReturnValue;
		String functionCallInstanceKey = getFunctionCallInstanceKey(functionName, expression);
		if (beforeFunctionNameAndReturnMap != null && beforeFunctionNameAndReturnMap.containsKey(functionCallInstanceKey)) {
			functionReturnValue = beforeFunctionNameAndReturnMap.get(functionCallInstanceKey);
		} else {
			functionReturnValue = functionService.apply(functionName, value);
		}
		return functionReturnValue;
	}

	/**
	 * @param functionName    函数名称
	 * @param paramExpression 解析前的表达式
	 * @return 函数缓存的key
	 * 方法执行之前换成函数的结果，此时函数调用的唯一标志：函数名+参数表达式
	 */
	public String getFunctionCallInstanceKey(String functionName, String paramExpression) {
		return functionName + paramExpression;
	}


	public boolean beforeFunction(String functionName) {
		return functionService.beforeFunction(functionName);
	}

	public LogFunctionParser(IFunctionService functionService) {
		this.functionService = functionService;
	}
}
