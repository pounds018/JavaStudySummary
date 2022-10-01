package cn.pounds.elegant.log.parse.template;

import cn.pounds.elegant.log.aop.OpLog;
import cn.pounds.elegant.log.aop.OpLogs;
import cn.pounds.elegant.log.bean.OpLogAnnotations;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author: pounds
 * @date: 2022/2/20 21:04
 * @desc: 注解解析
 */
public class LogRecordOperationSource {

	public Collection<OpLogAnnotations> computeLogRecordOperations(Method method, Class<?> targetClass) {
		// 验证方法是不是public
		if (!Modifier.isPublic(method.getModifiers())){
			return null;
		}
		// 方法可能是接口方法, 获取其实现防方法
		Method mostSpecificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
		// 方法可能含有泛型参数, 获取原始方法
		mostSpecificMethod = BridgeMethodResolver.findBridgedMethod(mostSpecificMethod);

		return parseOpLogRecords(mostSpecificMethod);
	}

	private Collection<OpLogAnnotations> parseOpLogRecords(Method method) {
		Collection<OpLogAnnotations> operations = new ArrayList<>();
		// 多注解支持
		OpLogs opLogRecords = method.getAnnotation(OpLogs.class);
		if (null != opLogRecords){
			for (OpLog opLog : opLogRecords.OpLogRecords()) {
				OpLogAnnotations opLogAnnotations = buildOpLogRecord(opLog);
				operations.add(opLogAnnotations);
			}
			// 多注解之外的opLog注解不解析
			return operations;
		}
		// 单个opLog注解标记方法
		OpLog opLog = method.getAnnotation(OpLog.class);
		if (null != opLog){
			OpLogAnnotations opLogAnnotations = buildOpLogRecord(opLog);
			operations.add(opLogAnnotations);
		}
		return operations;
	}

	private OpLogAnnotations buildOpLogRecord(OpLog opLog){

		return OpLogAnnotations.builder()
				.successLog(opLog.successLog())
				.failLog(opLog.failLog())
				.operator(opLog.operator())
				.bizAppId(opLog.bizAppId())
				.bizAppName(opLog.bizAppName())
				.opType(opLog.opType())
				.detail(opLog.detail())
				.condition(opLog.condition()).build();
	}
}
