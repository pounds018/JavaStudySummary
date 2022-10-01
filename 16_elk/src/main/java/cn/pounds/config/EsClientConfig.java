package cn.pounds.config;

import lombok.Data;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Date: 2020/10/18 23:56
 * @author: by Martin
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "elasticsearch")
public class EsClientConfig {
    private String host;
    private Integer port1;
    private Integer port2;
    private Integer port3;

    @Bean
    public RestHighLevelClient esClient(){
        return new RestHighLevelClient(
            RestClient.builder(
                    new HttpHost(host,port1,"http"),
                    new HttpHost(host,port2,"http"),
                    new HttpHost(host,port3,"http")
            )
        );
    }
}
