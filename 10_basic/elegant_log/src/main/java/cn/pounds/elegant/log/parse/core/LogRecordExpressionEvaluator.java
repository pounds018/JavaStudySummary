package cn.pounds.elegant.log.parse.core;

import cn.pounds.elegant.log.parse.func.ParseFunctionFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.CachedExpressionEvaluator;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: pounds
 * @date: 2022/2/20 22:58
 * @desc: 注解表达式计算器
 */
@Component
public class LogRecordExpressionEvaluator extends CachedExpressionEvaluator {

	private static final String EXPRESSION_PREFIX = "#{";

	private static final String EXPRESSION_SUFFIX = "}";

	/**
	 *  表示没有结果变量
	 */
	public static final Object NO_RESULT = new Object();
	/**
	 *  结果变量不可使用
	 */
	public static final Object RESULT_UNAVAILABLE = new Object();
	/**
	 *  计算上下文中，保存结果对象的变量名称
	 */
	public static final String RESULT_VARIABLE = "result";

	/**
	 * expressionCache 是为了缓存方法、表达式和 SpEL 的 Expression 的对应关系，让方法注解上添加的 SpEL 表达式只解析一次。
	 */
	private final Map<ExpressionKey, Expression> expressionCache = new ConcurrentHashMap<>(64);
	/**
	 *  targetMethodCache 是为了缓存传入到 Expression 表达式的 Object
	 */
	private final Map<AnnotatedElementKey, Method> targetMethodCache = new ConcurrentHashMap<>(64);

	/**
	 *  condition 条件表达式缓存
	 */
	private final Map<ExpressionKey, Expression> conditionCache = new ConcurrentHashMap<>(64);

	/**
	 *
	 * @param method --- 目标方法
	 * @param args --- 方法参数
	 * @param target --- 目标对象
	 * @param targetClass --- 目标对象Class类型
	 * @param result --- 方法执行结果
	 * @param errorMsg --- 错误信息
	 * @param beanFactory --- bean的访问控制对象
	 * @return
	 */
	public EvaluationContext createEvaluationContext(Method method,
													 Object[] args,
													 Object target,
	                                                 Class<?> targetClass,
													 Object result,
													 String errorMsg,
													 BeanFactory beanFactory,
													 ParseFunctionFactory parseFunctionFactory) {
		// 从方法缓存里面获取目标方法
		Method targetMethod = getTargetMethod(targetClass, method);
		// 创建计算上下文的根对象
		final ExpressionRootObject rootObject = new ExpressionRootObject(method, args, target, targetClass);
		// 创建缓存计算上下文
		final LogRecordEvaluationContext evaluationContext = new LogRecordEvaluationContext(
				rootObject, method, args, getParameterNameDiscoverer(), result, errorMsg, parseFunctionFactory);
		// 1）方法返回值不可用
		if (result == RESULT_UNAVAILABLE) {
			evaluationContext.addUnavailableVariable(RESULT_VARIABLE);
		}
		// 2）方法返回值可用，则以 result 作为 key 将其加入到计算变量中
		else if (result != NO_RESULT) {
			evaluationContext.setVariable(RESULT_VARIABLE, result);
		}
		if (beanFactory != null) {
			// 写入 BeanFactoryResolver
			evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
		}
		return evaluationContext;
	}
	/**
	 *  计算 condition 值
	 */
	public boolean condition(String conditionExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext) {
		return Boolean.TRUE.equals(getExpression1(conditionCache, methodKey, conditionExpression).getValue(
				evalContext, Boolean.class));
	}

	public Object parseExpression(String conditionExpression, AnnotatedElementKey methodKey,
							  EvaluationContext evalContext) {
		return getExpression1(this.expressionCache, methodKey, conditionExpression).getValue(evalContext, String.class);
	}

	protected Expression getExpression1(Map<ExpressionKey, Expression> cache,
									   AnnotatedElementKey elementKey, String expression) {

		ExpressionKey expressionKey = createKey(elementKey, expression);
		Expression expr = cache.get(expressionKey);
		if (expr == null) {
			expr = getParser().parseExpression(expression, new TemplateParserContext(EXPRESSION_PREFIX,EXPRESSION_SUFFIX));
			cache.put(expressionKey, expr);
		}
		return expr;
	}

	private ExpressionKey createKey(AnnotatedElementKey elementKey, String expression) {
		return new ExpressionKey(elementKey, expression);
	}


	/**
	 * 获取真正的具体的方法并缓存
	 *
	 * @param targetClass 目标class
	 * @param method      来自接口或者父类的方法签名
	 * @return 目标class实现的具体方法
	 */
	private Method getTargetMethod(Class<?> targetClass, Method method) {
		AnnotatedElementKey methodKey = new AnnotatedElementKey(method, targetClass);
		Method targetMethod = this.targetMethodCache.get(methodKey);
		if (targetMethod == null) {
			targetMethod = AopUtils.getMostSpecificMethod(method, targetClass);
			this.targetMethodCache.put(methodKey, targetMethod);
		}
		return targetMethod;
	}

	protected static class ExpressionKey implements Comparable<ExpressionKey> {

		private final AnnotatedElementKey element;

		private final String expression;

		protected ExpressionKey(AnnotatedElementKey element, String expression) {
			Assert.notNull(element, "AnnotatedElementKey must not be null");
			Assert.notNull(expression, "Expression must not be null");
			this.element = element;
			this.expression = expression;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof CachedExpressionEvaluator.ExpressionKey)) {
				return false;
			}
			ExpressionKey otherKey = (ExpressionKey) other;
			return (this.element.equals(otherKey.element) &&
					ObjectUtils.nullSafeEquals(this.expression, otherKey.expression));
		}

		@Override
		public int hashCode() {
			return this.element.hashCode() * 29 + this.expression.hashCode();
		}

		@Override
		public String toString() {
			return this.element + " with expression \"" + this.expression + "\"";
		}

		@Override
		public int compareTo(ExpressionKey other) {
			int result = this.element.toString().compareTo(other.element.toString());
			if (result == 0) {
				result = this.expression.compareTo(other.expression);
			}
			return result;
		}
	}



}
