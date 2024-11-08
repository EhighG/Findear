package com.findear.main.common.config;

import com.blazebit.persistence.querydsl.JPQLNextTemplates;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class QuerydslConfig {

    private final EntityManager em;

    public QuerydslConfig(EntityManager em) {
        this.em = em;
    }

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        // sql의 window function 사용을 위함
        return new JPAQueryFactory(JPQLNextTemplates.DEFAULT, em);
    }
}
