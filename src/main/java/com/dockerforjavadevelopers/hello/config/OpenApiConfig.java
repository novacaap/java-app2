package com.dockerforjavadevelopers.hello.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${api.external.scheme:https}")
    private String externalScheme;

    @Value("${api.external.host:localhost:8090}")
    private String externalHost;

    @Value("${api.external.base-path:/}")
    private String externalBasePath;

    @Bean
    public OpenAPI customOpenAPI() {
        String url = externalScheme + "://" + externalHost + externalBasePath;

        return new OpenAPI()
                .info(new Info()
                        .title("Java App2 API")
                        .version("1.0.0")
                        .description("Sample REST API - depends on java-app1 from GitHub Package")
                        .contact(new Contact()
                                .name("Java App2")))
                .servers(List.of(new Server().url(url)));
    }
}
