package com.findear.batch.common.config;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories
public class ElasticSearchConfig extends AbstractElasticsearchConfiguration {

    @Value("${my.elasticsearch-port}")
    private String ELASTIC_SEARCH_PORT;

    @Override
    public RestHighLevelClient elasticsearchClient() {
        // http port 와 통신할 주소
        ClientConfiguration configuration = ClientConfiguration.builder().connectedTo(ELASTIC_SEARCH_PORT)
                .withConnectTimeout(20000)
                .withSocketTimeout(60000).build();

        return RestClients.create(configuration).rest();
    }

}