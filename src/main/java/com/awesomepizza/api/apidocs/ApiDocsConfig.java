package com.awesomepizza.api.apidocs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiDocsConfig {

  @Bean
  public OpenAPI customOpenAPI(final @Value("${application-description}") String appDescription,
                               final @Value("${application-version}") String appVersion,
                               final @Value("${application-title}") String appTitle) {
    var openApi = new OpenAPI()
      .info(new Info()
        .title(appTitle)
        .version(appVersion)
        .description(appDescription));
    return openApi;
  }
}
