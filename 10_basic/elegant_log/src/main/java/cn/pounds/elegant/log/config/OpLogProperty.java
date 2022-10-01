package cn.pounds.elegant.log.config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 将属性设置为其他的名字
 * @author: pounds
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface OpLogProperty {

	String name();

	String function() default "";

	//   String dateFormat() default "";
}
