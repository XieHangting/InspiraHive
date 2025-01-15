package com.xht.inspirahivebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.xht.inspirahivebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class InspiraHiveBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(InspiraHiveBackendApplication.class, args);
    }

}
