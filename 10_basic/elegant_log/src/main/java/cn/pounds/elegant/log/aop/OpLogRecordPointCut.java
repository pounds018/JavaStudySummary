package cn.pounds.elegant.log.aop;


import cn.pounds.elegant.log.parse.template.LogRecordOperationSource;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * @author: pounds
 * @date: 2022/2/20 20:45
 * @desc: 切点实现类
 */
public class OpLogRecordPointCut extends StaticMethodMatcherPointcut implements Serializable {
	/**
	 * logRecord注解的解析类
	 */
	private LogRecordOperationSource logRecordOperationSource;

	@Override
	public boolean matches(@NonNull Method method, @NonNull Class<?> targetClass) {
		// 解析 这个 method 上有没有 @LogRecordAnnotation 注解，有的话会解析出来注解上的各个参数
		return !CollectionUtils.isEmpty(logRecordOperationSource.computeLogRecordOperations(method, targetClass));
	}

	public void setLogRecordOperationSource(LogRecordOperationSource logRecordOperationSource) {
		this.logRecordOperationSource = logRecordOperationSource;
	}
}
