package cn.pounds.elegant.log;

import cn.pounds.elegant.log.config.EnableLogRecord;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * @author: pounds
 * @date: 2022/2/21 0:15
 * @desc:
 */
@EnableAspectJAutoProxy
@SpringBootApplication
@EnableLogRecord(tenant = "cn.pounds")
public class Application {
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}
}
