package com.edstem.interviewprep.common.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
@EnableCaching(order = Ordered.LOWEST_PRECEDENCE - 100)
public class CacheConfig {
}
