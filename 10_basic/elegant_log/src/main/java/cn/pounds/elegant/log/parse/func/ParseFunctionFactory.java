package cn.pounds.elegant.log.parse.func;

import cn.pounds.elegant.log.extend.custom.IParseFunction;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: pounds
 * @desc: 把所有的 IParseFunction 注入到函数工厂中
 */
public class ParseFunctionFactory {
	private Map<String, IParseFunction> allFunctionMap;

	public ParseFunctionFactory(List<IParseFunction> parseFunctions) {
		if (CollectionUtils.isEmpty(parseFunctions)) {
			return;
		}
		allFunctionMap = new HashMap<>();
		for (IParseFunction parseFunction : parseFunctions) {
			if (StringUtils.isEmpty(parseFunction.functionName())) {
				continue;
			}
			allFunctionMap.put(parseFunction.functionName(), parseFunction);
		}
	}

	public IParseFunction getFunction(String functionName) {
		return allFunctionMap.get(functionName);
	}

	public boolean isBeforeFunction(String functionName) {
		return allFunctionMap.get(functionName) != null && allFunctionMap.get(functionName).executeBefore();
	}

	public Map<String, IParseFunction> getFunctionMap() {
		return allFunctionMap;
	}
}