package org.devx.automatedinvoicesystem.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        String uploadPath = Paths.get("uploads").toFile().getAbsolutePath();

        registry.addResourceHandler("/local-files/**")
                .addResourceLocations("file:" + uploadPath + "/")
                .addResourceLocations("file:local-files/");  // Added this to resolve the python crashing with error 404 when trying to access the file URL. This is because the uploadFileBytes method in FileStorageService is returning a URL with "local-files" in it, so we need to make sure Spring can serve files from that directory as well.
    }

    @org.springframework.beans.factory.annotation.Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void addCorsMappings(org.springframework.web.servlet.config.annotation.CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(frontendUrl) // Only allow your specific frontend
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

}
