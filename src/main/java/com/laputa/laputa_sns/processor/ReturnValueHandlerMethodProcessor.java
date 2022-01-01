package com.laputa.laputa_sns.processor;

import java.io.IOException;
import java.util.List;

import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.service.OperatorService;

import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

/**
 * @author JQH
 */
public class ReturnValueHandlerMethodProcessor extends RequestResponseBodyMethodProcessor {

    private OperatorService operatorService;

    public ReturnValueHandlerMethodProcessor(List<HttpMessageConverter<?>> messageConverters, OperatorService operatorService) {
        super(messageConverters);
        this.operatorService = operatorService;
    }

    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        return returnType.getMethod().getReturnType().equals(Result.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer, NativeWebRequest webRequest)
            throws IOException, HttpMediaTypeNotAcceptableException {
        Result<Object> result = (Result<Object>) returnValue;
        Operator operator = result.getOperator();
        // 请求结束后，获取operator的动态和通知数量，并刷新redis中的operator
        if (operator != null) {
            operatorService.pullNewsAndNoticeCnt(operator);
            operatorService.updateOnlineOperator(operator);
        }
        super.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
    }

}
