package com.example.RealTime_Attendance.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

@Configuration
@DependsOn("dataSource")
public class SchedulerConfig {


    @Value("${DB_URL}")
    private String dbUrl;

    @Value("${DB_USERNAME}")
    private String dbUser;

    @Value("${DB_PASSWORD}")
    private String dbPass;

    @Value("${DB_DRIVER}")
    private String dbDriver;

    @Bean
    public Properties quartzProperties() {
        Properties props = new Properties();
        props.setProperty("org.quartz.scheduler.instanceName", "AttendanceScheduler");
        props.setProperty("org.quartz.scheduler.instanceId", "AUTO");
        props.setProperty("org.quartz.threadPool.class","org.quartz.simpl.SimpleThreadPool");
        props.setProperty("org.quartz.threadPool.threadCount","25");
        props.setProperty("org.quartz.threadPool.threadPriority", "5");
        props.setProperty("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
        props.setProperty("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
        props.setProperty("org.quartz.jobStore.tablePrefix", "qrtz_");
        props.setProperty("org.quartz.jobStore.isClustered", "true");
        props.setProperty("org.quartz.jobStore.misfireThreshold", "60000");
        props.setProperty("org.quartz.jobStore.dataSource", "myDS");
        props.setProperty("org.quartz.dataSource.myDS.driver", dbDriver);
        props.setProperty("org.quartz.dataSource.myDS.URL", dbUrl);
        props.setProperty("org.quartz.dataSource.myDS.user", dbUser);
        props.setProperty("org.quartz.dataSource.myDS.password", dbPass);
        props.setProperty("org.quartz.dataSource.myDS.maxConnections", "10");
        return props;
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(PlatformTransactionManager transactionManager) throws IOException {
        SchedulerFactoryBean scheduler = new SchedulerFactoryBean();
//         scheduler.setDataSource(dataSource);
        PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
        propertiesFactoryBean.setLocation(new ClassPathResource("/quartz.properties"));
        propertiesFactoryBean.afterPropertiesSet();
//        scheduler.setQuartzProperties(Objects.requireNonNull(propertiesFactoryBean.getObject()));
        scheduler.setQuartzProperties(quartzProperties());
        scheduler.setTransactionManager(transactionManager);
        scheduler.setSchedulerName("AttendanceScheduler");
        scheduler.setJobFactory(springBeanJobFactory());
        scheduler.setOverwriteExistingJobs(true);
        scheduler.setAutoStartup(true);
        return scheduler;
    }

    @Bean
    public SpringBeanJobFactory springBeanJobFactory() {
        return new SpringBeanJobFactory();
    }
}
