package app.marca.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/** 把 /uploads/** 映射到磁盘上的 upload 目录，供前端通过 url 直接 GET。 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final String uploadDir;
    private final String urlPrefix;

    public WebMvcConfig(
            @Value("${marca.upload.dir}") String uploadDir,
            @Value("${marca.upload.url-prefix}") String urlPrefix
    ) {
        this.uploadDir = uploadDir;
        this.urlPrefix = urlPrefix.endsWith("/") ? urlPrefix : urlPrefix + "/";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(uploadDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler(urlPrefix + "**").addResourceLocations(location);
    }
}
