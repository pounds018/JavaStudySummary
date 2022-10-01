package cn.pounds.elegant.log.parse.func;


/**
 * @author: pounds
 * @date: 2022/2/20 21:28
 * @desc:
 */
public interface IFunctionService {
	/**
	 * 执行自定义前置解析方法
	 * @param functionName --- 方法名称
	 * @param value --- 参数
	 * @return --- 执行结果
	 */
	String apply(String functionName, Object value);

	/**
	 * 根据方法名, 判断其是否是前置方法
	 * @param functionName --- 方法名称
	 * @return true 前置方法
	 */
	boolean beforeFunction(String functionName);
}
