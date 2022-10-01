package cn.pounds.elegant.log.parse.core;

import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @author: pounds
 * @date: 2022/2/27 18:45
 * @desc:
 */

public class ExpressionRootObject {

	private final Method method;

	private final Object[] args;

	private final Object target;

	private final Class<?> targetClass;

	public ExpressionRootObject(Method method, Object[] args, Object target, Class<?> targetClass) {
		this.method = method;
		this.target = target;
		this.targetClass = targetClass;
		this.args = args;
	}


	public String getMethodName() {
		return this.method.getName();
	}

	public Method getMethod() {
		return method;
	}

	public Object[] getArgs() {
		return args;
	}

	public Object getTarget() {
		return target;
	}

	public Class<?> getTargetClass() {
		return targetClass;
	}

	public String queryOldUser(Object value) {
		return value.toString();
	}
}
