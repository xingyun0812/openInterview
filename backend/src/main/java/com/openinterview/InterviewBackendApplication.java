package com.openinterview;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(excludeName = {
        "org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration"
})
@MapperScan("com.openinterview.mapper")
public class InterviewBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterviewBackendApplication.class, args);
    }
}
