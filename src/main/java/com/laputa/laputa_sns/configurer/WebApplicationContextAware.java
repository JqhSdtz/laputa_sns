package com.laputa.laputa_sns.configurer;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.laputa.laputa_sns.processor.ReturnValueHandlerMethodProcessor;
import com.laputa.laputa_sns.service.OperatorService;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

/**
 * @author JQH
 */
@Configuration
public class WebApplicationContextAware implements ApplicationContextAware {

    private final OperatorService operatorService;

    public WebApplicationContextAware(OperatorService operatorService) {
        this.operatorService = operatorService;
    }

    /**
     * 注册到容器,采用这种注册方式的目的: 自定义的HandlerMethodReturnValueHandler放在默认实现的前面,从而优先采用自定义处理策略
     * 否则,无法覆盖@ResponseBody处理机制,
     * 且String类型的返回值将默认由ViewNameMethodReturnValueHandler处理而映射为视图名
     * 参考https://www.cnblogs.com/MattCheng/p/9486370.html; https://stackoverflow.com/questions/22890952
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        messageConverters.add(converter);
        ObjectMapper mapper = ((MappingJackson2HttpMessageConverter)converter).getObjectMapper();
        mapper.setFilterProvider(new SimpleFilterProvider()
                //序列化operator时去掉token，token通过httpOnly的cookie传输
                .addFilter("OperatorFilter", SimpleBeanPropertyFilter.serializeAllExcept("token"))
                .addFilter("UserFilter", SimpleBeanPropertyFilter.serializeAll())
                .addFilter("PostFilter", SimpleBeanPropertyFilter.serializeAll())
                .addFilter("CommentL1Filter", SimpleBeanPropertyFilter.serializeAll())
                .addFilter("CommentL2Filter", SimpleBeanPropertyFilter.serializeAll())
                .addFilter("PermissionFilter", SimpleBeanPropertyFilter.serializeAll()));
        // true表示转json时时间转化为一个长整型数，false则转化为字符串
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        RequestMappingHandlerAdapter handlerAdapter = applicationContext.getBean(RequestMappingHandlerAdapter.class);
        List<HandlerMethodReturnValueHandler> handlers = new ArrayList<>();
        handlers.add(new ReturnValueHandlerMethodProcessor(messageConverters, operatorService));
        handlers.addAll(handlerAdapter.getReturnValueHandlers());
        handlerAdapter.setReturnValueHandlers(handlers);
    }
}
