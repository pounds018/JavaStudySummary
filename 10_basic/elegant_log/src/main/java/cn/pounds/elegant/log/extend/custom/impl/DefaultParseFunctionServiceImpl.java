package cn.pounds.elegant.log.extend.custom.impl;

import cn.pounds.elegant.log.extend.custom.IParseFunction;

/**
 * @author: pounds
 * @date: 2022/2/20 22:26
 * @desc: 默认的自定义解析类
 */
public class DefaultParseFunctionServiceImpl implements IParseFunction {

	@Override
	public String functionName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public String apply(Object value) {
		return null;
	}
}
