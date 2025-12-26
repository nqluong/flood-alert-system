package org.project.floodalert.floodcore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = {
        "org.project.floodalert.floodcore",
        "org.project.floodalert.common"
})
@SpringBootApplication
public class FloodCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(FloodCoreApplication.class, args);
    }
}
