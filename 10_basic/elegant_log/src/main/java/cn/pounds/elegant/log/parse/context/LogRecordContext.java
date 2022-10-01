package cn.pounds.elegant.log.parse.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

/**
 * @author: pounds
 * @date: 2022.02.21
 * @desc: 日志上下文
 * todo:
 * 1. InheritableThreadLocal在使用线程池的时候, 父子线程无意义, 解决方案是使用阿里的TTL框架
 * 2. 如何通过logContent记录更多的非模板日志
 */
public class LogRecordContext {

	private static final InheritableThreadLocal<Stack<Map<String, Object>>> VARIABLE_MAP_STACK =
			new InheritableThreadLocal<Stack<Map<String, Object>>>() {
				@Override
				protected Stack<Map<String, Object>> initialValue() {
					return new Stack<>();
				}
			};

	public static void putEmptySpan() {
		VARIABLE_MAP_STACK.get().add(new HashMap<>(16));
	}

	public static void clear() {
		Optional.ofNullable(VARIABLE_MAP_STACK.get()).ifPresent(s -> {
			if(s.size() == 1) {
				VARIABLE_MAP_STACK.remove();
			} else {
				s.pop();
			}
		});

	}

	/**
	 * 每一次方法被拦截到的时候都会先, putEmptySpan()初始化一个map入站, 方法执行完毕的时候会pop掉这个map
	 *
	 * @param varName --- 变量名字
	 * @param value   --- 变量值
	 */
	public static void putVariable(String varName, Object value) {
		Stack<Map<String, Object>> kvMapStack = VARIABLE_MAP_STACK.get();
		Map<String, Object> topMap = kvMapStack.peek();
		topMap.put(varName, value);
	}

	/**
	 * 获取当前方法中设置的日志信息
	 *
	 * @return
	 */
	public static Map<String, Object> getVariables() {
		return VARIABLE_MAP_STACK.get().peek();
	}

	public static Object getVariable(String oldObject) {
		return VARIABLE_MAP_STACK.get().peek().get(oldObject);
	}
}