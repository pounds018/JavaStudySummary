package cn.pounds;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties
@SpringBootApplication
@MapperScan("cn.pounds")
public class PoundsApplication {

    public static void main(String[] args) {
        SpringApplication.run(PoundsApplication.class, args);
    }

}
