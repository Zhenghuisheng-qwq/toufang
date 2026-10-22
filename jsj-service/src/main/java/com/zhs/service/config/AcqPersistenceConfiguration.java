package com.zhs.service.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * 投放业务表使用独立 SqlSessionFactory。
 * 后台配置了 acq.datasource.url 时写入隔离业务库；未配置时只允许落在当前应用唯一的业务数据源上。
 */
@Configuration
@MapperScan(basePackages = "com.zhs.service.mapper.acq", sqlSessionFactoryRef = "acqSqlSessionFactory")
public class AcqPersistenceConfiguration {

    @Bean(name = "acqDataSource")
    @ConditionalOnProperty(prefix = "acq.datasource", name = "url")
    @ConfigurationProperties(prefix = "acq.datasource")
    public DataSource acqDataSource() {
        return new DruidDataSource();
    }

    @Bean(name = "acqTransactionManager")
    public PlatformTransactionManager acqTransactionManager(
            @Autowired(required = false) @Qualifier("acqDataSource") DataSource acqDataSource,
            @Autowired(required = false) @Qualifier("dataSource") DataSource bootDataSource,
            @Autowired(required = false) @Qualifier("dynamicDataSource") DataSource dynamicDataSource) {
        return new DataSourceTransactionManager(selectDataSource(acqDataSource, bootDataSource, dynamicDataSource));
    }

    @Bean(name = "acqSqlSessionFactory")
    public SqlSessionFactory acqSqlSessionFactory(
            @Autowired(required = false) @Qualifier("acqDataSource") DataSource acqDataSource,
            @Autowired(required = false) @Qualifier("dataSource") DataSource bootDataSource,
            @Autowired(required = false) @Qualifier("dynamicDataSource") DataSource dynamicDataSource,
            MyMetaObjectHandler metaObjectHandler) throws Exception {
        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(selectDataSource(acqDataSource, bootDataSource, dynamicDataSource));

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        factory.setConfiguration(configuration);

        GlobalConfig.DbConfig dbConfig = new GlobalConfig.DbConfig();
        dbConfig.setIdType(IdType.AUTO);
        dbConfig.setLogicDeleteField("isDelete");
        dbConfig.setLogicDeleteValue("1");
        dbConfig.setLogicNotDeleteValue("0");
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setDbConfig(dbConfig);
        globalConfig.setMetaObjectHandler(metaObjectHandler);
        factory.setGlobalConfig(globalConfig);

        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        factory.setPlugins(interceptor);
        return factory.getObject();
    }

    private static DataSource selectDataSource(DataSource acqDataSource, DataSource bootDataSource, DataSource dynamicDataSource) {
        if (acqDataSource != null) {
            return acqDataSource;
        }
        if (dynamicDataSource != null) {
            throw new IllegalStateException("后台必须配置 acq.datasource.url，投放业务不能写入系统库");
        }
        if (bootDataSource == null) {
            throw new IllegalStateException("投放业务数据源未配置");
        }
        return bootDataSource;
    }
}
