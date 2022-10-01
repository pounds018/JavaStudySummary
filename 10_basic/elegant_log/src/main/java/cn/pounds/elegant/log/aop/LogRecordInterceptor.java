package cn.pounds.elegant.log.aop;

import cn.pounds.elegant.log.bean.OpLogAnnotations;
import cn.pounds.elegant.log.bean.MethodExecuteResult;
import cn.pounds.elegant.log.bean.OpLogRecord;
import cn.pounds.elegant.log.extend.operator.IOperatorGetService;
import cn.pounds.elegant.log.extend.persistence.ILogRecordService;
import cn.pounds.elegant.log.parse.context.LogRecordContext;
import cn.pounds.elegant.log.parse.core.LogRecordValueParser;
import cn.pounds.elegant.log.parse.func.IFunctionService;
import cn.pounds.elegant.log.parse.template.LogRecordOperationSource;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.expression.EvaluationContext;
import org.springframework.util.CollectionUtils;


import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;

import static org.springframework.aop.support.AopUtils.getTargetClass;

/**
 * @author: pounds
 * @date: 2022/2/20 22:27
 * @desc:
 */
public class LogRecordInterceptor extends LogRecordValueParser implements MethodInterceptor {
	private final static Logger logger = LoggerFactory.getLogger(LogRecordInterceptor.class);

	private LogRecordOperationSource logRecordOperationSource;
	private String tenant;
	/**
	 * 日志持久化服务
	 */
	private ILogRecordService bizLogService;
	/**
	 * 操作人获取服务
	 */
	private IOperatorGetService operatorGetService;

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Method method = invocation.getMethod();
		// 记录日志
		return execute(invocation, invocation.getThis(), method, invocation.getArguments());
	}

	private Object execute(MethodInvocation invoker, Object target, Method method, Object[] args) throws Throwable {
		Class<?> targetClass = getTargetClass(target);
		Object ret = null;
		MethodExecuteResult methodExecuteResult = new MethodExecuteResult(true, null, "");
		// 初始化本次拦截的变量map
		LogRecordContext.putEmptySpan();
		Collection<OpLogAnnotations> operations = new ArrayList<>();
		Map<String, String> functionNameAndReturnMap = new HashMap<>(16);
		try {
			operations = logRecordOperationSource.computeLogRecordOperations(method, targetClass);
			List<String> spElTemplates = getBeforeExecuteFunctionTemplate(operations);
			//业务逻辑执行前的自定义函数解析
			functionNameAndReturnMap = processBeforeExecuteFunctionTemplate(spElTemplates, target, targetClass, method,
					args);
		} catch (Exception e) {
			logger.error("log record parse before function exception", e);
		}
		try {
			// 切点的执行
			ret = invoker.proceed();
		} catch (Exception e) {
			methodExecuteResult = new MethodExecuteResult(false, e, e.getMessage());
		}
		try {
			if (!CollectionUtils.isEmpty(operations)) {
				// 记录执行结果
				recordExecute(ret, target, method, args, operations, targetClass,
						methodExecuteResult.isSuccess(), methodExecuteResult.getErrorMsg(), functionNameAndReturnMap);
			}
		} catch (Exception t) {
			//记录日志错误不要影响业务
			logger.error("log record parse exception", t);
		} finally {
			LogRecordContext.clear();
		}
		if (methodExecuteResult.throwable != null) {
			throw methodExecuteResult.throwable;
		}
		return ret;
	}

	/**
	 * 切点上面的注解解析
	 *
	 * @param operations --- 方法上的注解对象集合
	 * @return 注解模板
	 */
	private List<String> getBeforeExecuteFunctionTemplate(Collection<OpLogAnnotations> operations) {
		List<String> res = new ArrayList<>();
		for (OpLogAnnotations operation : operations) {
			getTemplates(res, operation);
		}
		return res;
	}

	/**
	 * 根据模板里面要执行的自定义前置方法, 把方法和执行结果解析出来
	 *
	 * @param spElTemplates --- spel模板
	 * @param targetClass   --- 目标对象
	 * @param method        --- 目标方法
	 * @param args          --- 参数
	 * @return
	 */
	private Map<String, String> processBeforeExecuteFunctionTemplate(List<String> spElTemplates,
																	 Object target,Class<?> targetClass, Method method,
																	 Object[] args) {
		HashMap<String, String> functionNameAndReturnMap = new HashMap<>(16);
		EvaluationContext evaluationContext = expressionEvaluator.createEvaluationContext(method, args, target,
				targetClass, null, null, beanFactory, parseFunctionFactory);
		for (String spEl : spElTemplates) {
			Matcher matcher = PATTERN.matcher(spEl);
			while (matcher.find()) {
				String expression = matcher.group(2);
				if (expression.contains("#_ret") || expression.contains("#_errorMsg")){
					continue;
				}
				AnnotatedElementKey annotatedElementKey = new AnnotatedElementKey(method, targetClass);
				String funcName = matcher.group(1);
				if (logFunctionParser.beforeFunction(funcName)){
					// 参数的值
					Object value = expressionEvaluator.parseExpression(expression, annotatedElementKey, evaluationContext);
					String functionReturnValue = logFunctionParser.getFunctionReturnValue(null, value, expression, funcName);
					String functionCallInstanceKey = logFunctionParser.getFunctionCallInstanceKey(funcName, expression);
					functionNameAndReturnMap.put(functionCallInstanceKey, functionReturnValue);
				}

			}
		}
		return functionNameAndReturnMap;
	}

	/**
	 * @param ret                      --- 切点执行结果
	 * @param method                   --- 切点对象
	 * @param args                     --- 切点的参数
	 * @param operations               --- 切点的操作记录
	 * @param targetClass              --- 切点所在的类
	 * @param success                  --- 切点是否执行成功
	 * @param errorMsg                 --- 切点执行出现错误的信息
	 * @param functionNameAndReturnMap
	 */
	private void recordExecute(Object ret,
							   Object target,
							   Method method,
							   Object[] args,
							   Collection<OpLogAnnotations> operations,
							   Class<?> targetClass,
							   boolean success,
							   String errorMsg,
							   Map<String, String> functionNameAndReturnMap) {

		operations.forEach(op -> {
			try {
				String action = getLogRecordContent(success, op);
				if (StringUtils.isEmpty(action)) {
					return;
				}
				ArrayList<String> templates = Lists.newArrayList();
				getTemplates(templates,op);
				String operatorIdFromContext = getUserIdPutInTemplate(op, templates);

				Map<String, String> expressionValues = parseTemplates(templates, ret, target, targetClass, method, args, errorMsg,functionNameAndReturnMap);
				if (logConditionPassed(op.getCondition(), expressionValues)){
					OpLogRecord logRecord = OpLogRecord.builder()
							.tenant(tenant)
							.bizAppName(expressionValues.get(op.getBizAppName()))
							.bizAppId(expressionValues.get(op.getBizAppId()))
							.operator(getRealOperatorId(op, operatorIdFromContext, expressionValues))
							.opType(op.getOpType())
							.detail(expressionValues.get(op.getDetail()))
							.action(expressionValues.get(action))
							.fail(!success)
							.createTime(new Date())
							.build();

					//如果 action 为空，不记录日志
					if (StringUtils.isEmpty(logRecord.getAction())) {
						return;
					}
					Preconditions.checkNotNull(bizLogService, "bizLogService not init!!");
					bizLogService.record(logRecord);
				}

			}catch (Exception e) {
				logger.error(String.format("record op log error: targetClass [%s], targetMethod [%s] , errorMsg [%s]",
						targetClass.getSimpleName(), method.getName(), e.getMessage()));
			}
		});
	}

	/**
	 * 根据执行结果获取模板
	 * @param success
	 * @param operation
	 * @return
	 */
	private String getLogRecordContent(boolean success, OpLogAnnotations operation) {
		String action = "";
		if (success) {
			action = operation.getSuccessLog();
		} else {
			action = operation.getFailLog();
		}
		return action;
	}

	/**
	 * 反射获取该对象的所有属性值
	 *
	 * @return
	 */
	public void getTemplates(List<String> list, OpLogAnnotations opLogAnnotations) {
		for (Field declaredField : opLogAnnotations.getClass().getDeclaredFields()) {
			declaredField.setAccessible(true);
			try {
				Object value = declaredField.get(opLogAnnotations);
				list.add((String) value);
			} catch (IllegalAccessException e) {
				logger.error("log record parse error : [{}]", e.getMessage());
			}
		}
	}

	/**
	 * 从注解中获取操作人的id, 如果没有从operatorGetService的实现类里面获取, 实际业务中操作人更多的是根据前端传来的信息保存在上下文中从而获取
	 * @param operation
	 * @param spElTemplates
	 * @return
	 */
	private String getUserIdPutInTemplate(OpLogAnnotations operation, List<String> spElTemplates) {
		String realOperatorId = "";
		if (StringUtils.isEmpty(operation.getOperator())) {
			realOperatorId = operatorGetService.getUser().getId();
			if (StringUtils.isEmpty(realOperatorId)) {
				throw new IllegalArgumentException("[LogRecord] operator is null");
			}
		}else{
			spElTemplates.add(operation.getOperator());
		}
		return realOperatorId;
	}

	/**
	 * 判断是否满足记录条件
	 * @param condition  --- 记录条件
	 * @param expressionValues --- 表达式的值
	 * @return
	 */
	private boolean logConditionPassed(String condition, Map<String, String> expressionValues) {
		return StringUtils.isEmpty(condition) || StringUtils.endsWithIgnoreCase(expressionValues.get(condition), "true");
	}

	private String getRealOperatorId(OpLogAnnotations operation, String operatorIdFromService, Map<String, String> expressionValues) {
		return !StringUtils.isEmpty(operatorIdFromService) ? operatorIdFromService : expressionValues.get(operation.getOperator());
	}



	// --------------------------------- get and set --------------------------------------------\\


	public LogRecordOperationSource getLogRecordOperationSource() {
		return logRecordOperationSource;
	}

	public void setLogRecordOperationSource(LogRecordOperationSource logRecordOperationSource) {
		this.logRecordOperationSource = logRecordOperationSource;
	}

	public String getTenant() {
		return tenant;
	}

	public void setTenant(String tenant) {
		this.tenant = tenant;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@PostConstruct
	public void init() throws Exception {
		bizLogService = beanFactory.getBean(ILogRecordService.class);
		operatorGetService = beanFactory.getBean(IOperatorGetService.class);
		Preconditions.checkNotNull(bizLogService, "bizLogService not null");
	}
}
