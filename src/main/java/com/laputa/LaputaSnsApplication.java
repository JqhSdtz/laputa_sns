package com.laputa;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author jqh
 */
@SpringBootApplication
@EnableTransactionManagement
@MapperScan(basePackages={"com.laputa.laputa_sns.dao"})
public class LaputaSnsApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(LaputaSnsApplication.class);
        application.addListeners(new ApplicationPidFileWriter("./bin/lpt.pid"));
        application.run(args);
    }

}
