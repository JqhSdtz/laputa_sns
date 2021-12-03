package com.laputa.laputa_sns.configurer;

import com.laputa.laputa_sns.interceptor.ValidateInterceptor;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author JQH
 * @since 下午 12:31 20/02/14
 */

@Configuration
public class WebConfigurer implements WebMvcConfigurer {

    private final ValidateInterceptor validateInterceptor;

    public WebConfigurer(ValidateInterceptor validateInterceptor) {
        this.validateInterceptor = validateInterceptor;
    }

    @Override
    public void addInterceptors(@NotNull InterceptorRegistry registry) {
        registry.addInterceptor(validateInterceptor).addPathPatterns("/**")
                .excludePathPatterns("/**/*.js", "/**/*.html", "/**/*.css", "/**/*.jpg", "/**/*.png");
    }

//    @Override
//    public void addCorsMappings(@NotNull CorsRegistry registry) {
//        registry.addMapping("/**")
//                .allowedOrigins("*")
//                .allowCredentials(true)
//                .exposedHeaders("X-LPT-USER-TOKEN")
//                .allowedMethods("GET", "POST", "PATCH", "DELETE");
//    }

}
