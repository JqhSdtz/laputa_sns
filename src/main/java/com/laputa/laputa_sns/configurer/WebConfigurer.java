package com.laputa.laputa_sns.configurer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.laputa.laputa_sns.interceptor.ValidateInterceptor;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

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

    @Override
    public void addCorsMappings(@NotNull CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowCredentials(true)
                .exposedHeaders("X-LPT-USER-TOKEN")
                .allowedMethods("GET", "POST", "PATCH", "DELETE");
    }

    @Override
    public void extendMessageConverters(@NotNull List<HttpMessageConverter<?>> converters) {
        for(HttpMessageConverter<?> converter: converters) {
            if(converter instanceof MappingJackson2HttpMessageConverter) {
                ObjectMapper mapper = ((MappingJackson2HttpMessageConverter)converter).getObjectMapper();
                mapper.setFilterProvider(new SimpleFilterProvider()
                        //序列化operator时去掉token，token通过httpOnly的cookie传输
                        .addFilter("OperatorFilter", SimpleBeanPropertyFilter.serializeAllExcept("token"))
                        .addFilter("UserFilter", SimpleBeanPropertyFilter.serializeAll())
                        .addFilter("PostFilter", SimpleBeanPropertyFilter.serializeAll())
                        .addFilter("CommentL1Filter", SimpleBeanPropertyFilter.serializeAll())
                        .addFilter("CommentL2Filter", SimpleBeanPropertyFilter.serializeAll())
                        .addFilter("PermissionFilter", SimpleBeanPropertyFilter.serializeAll()));
                //true表示转json时时间转化为一个长整型数，false则转化为字符串
                mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
            }
        }
    }
}
