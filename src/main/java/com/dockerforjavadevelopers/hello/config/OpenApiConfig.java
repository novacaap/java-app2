package com.dockerforjavadevelopers.hello.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Java App2 API")
                        .version("1.0.0")
                        .description("Sample REST API - depends on java-app1 from GitHub Package")
                        .contact(new Contact()
                                .name("Java App2")));
    }
}
