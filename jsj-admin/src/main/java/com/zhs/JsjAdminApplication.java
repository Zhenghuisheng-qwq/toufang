package com.zhs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * 启动程序
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@ComponentScan(
        basePackages = "com.zhs",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.zhs\\.service\\.auth\\..*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.zhs\\.service\\.im\\..*")
        }
)
public class JsjAdminApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(JsjAdminApplication.class, args);
        System.out.println("后台管理项目启动成功！！！");
    }
}
