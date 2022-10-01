package cn.pounds.elegant.log.aop;

import java.lang.annotation.*;

/**
 * @author: pounds
 * @date: 2022/2/21 21:03
 * @desc: 同一个方法上多注解支持
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface OpLogs {
	OpLog[] OpLogRecords();
}
