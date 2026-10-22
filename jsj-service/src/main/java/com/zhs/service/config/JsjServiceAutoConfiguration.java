package com.zhs.service.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * jsj-service 自动配置：Mapper 扫描。
 * 投放表 Mapper 由 AcqPersistenceConfiguration 绑定到独立 SqlSessionFactory，这里排除，避免重复注册。
 */
@AutoConfiguration
@MapperScan(
        basePackages = "com.zhs.service.mapper",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.zhs\\.service\\.mapper\\.acq\\..*"
        )
)
public class JsjServiceAutoConfiguration {
}
