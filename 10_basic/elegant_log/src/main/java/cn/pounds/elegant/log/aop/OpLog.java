package cn.pounds.elegant.log.aop;

import java.lang.annotation.*;

/**
 * @Author: pounds
 * @Date: 2022/2/20
 * @Desc: 操作日志的aop拦截标志, 通过注解标记哪个方法需要被拦截
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface OpLog {
	String successLog(); // 操作日志成功的文本模板,必填

	String failLog() default ""; // 操作日志失败的文本版本

	String operator() default ""; // 操作日志的执行人

	String bizAppId();
	// 操作日志绑定的业务对象标识
	String bizAppName();// 操作日志绑定的业务对象标识

	String opType() default ""; // 操作日志的种类

	String detail() default ""; // 扩展参数，记录操作日志的修改详情

	String condition() default ""; // 记录日志的条件
}
