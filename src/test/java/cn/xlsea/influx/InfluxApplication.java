package cn.xlsea.influx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
// @ComponentScan(basePackages = {"cn.xlsea"})
public class InfluxApplication {

    public static void main(String[] args) {
        SpringApplication.run(InfluxApplication.class, args);
    }

}
