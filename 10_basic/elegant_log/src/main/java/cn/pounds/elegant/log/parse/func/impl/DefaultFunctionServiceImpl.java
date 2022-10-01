package cn.pounds.elegant.log.parse.func.impl;

import cn.pounds.elegant.log.parse.func.IFunctionService;
import cn.pounds.elegant.log.extend.custom.IParseFunction;
import cn.pounds.elegant.log.parse.func.ParseFunctionFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: pounds
 * @date: 2022/2/20 21:28
 * @desc:
 */
@Component
public class DefaultFunctionServiceImpl implements IFunctionService {

	private final ParseFunctionFactory parseFunctionFactory;

	public DefaultFunctionServiceImpl(ParseFunctionFactory parseFunctionFactory) {
		this.parseFunctionFactory = parseFunctionFactory;
	}

	@Override
	public String apply(String functionName, Object value) {
		IParseFunction function = parseFunctionFactory.getFunction(functionName);
		if (function == null) {
			return value.toString();
		}
		return function.apply(value);
	}

	@Override
	public boolean beforeFunction(String functionName) {
		return parseFunctionFactory.isBeforeFunction(functionName);
	}

}