package me.north30.erp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * ERP 系统唯一启动入口（全工程不允许出现第二个启动类）。
 */
@SpringBootApplication
@ConfigurationPropertiesScan("me.north30.erp")
public class ErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(ErpApplication.class, args);
    }
}
