package cn.pounds.elegant.log.test.service;

import cn.pounds.elegant.log.extend.custom.IParseFunction;
import org.springframework.stereotype.Service;

/**
 * @author: pounds
 * @date: 2022/2/24 22:42
 * @desc:
 */
@Service
public class NameParseService implements IParseFunction {
	@Override
	public boolean executeBefore() {
		return true;
	}

	@Override
	public String functionName() {
		return "queryOldUser";
	}

	@Override
	public String apply(Object value) {
		return value.toString() + "111111";
	}
}
