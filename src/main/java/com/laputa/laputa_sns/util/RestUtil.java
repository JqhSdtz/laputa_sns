package com.laputa.laputa_sns.util;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author JQH
 * @since 上午 10:17 21/03/13
 */
public class RestUtil {
    private static final RestTemplate restTemplate =
            new RestTemplate(new HttpComponentsClientHttpRequestFactory());

    public static ResponseEntity<String> post(String url, MultiValueMap<String, String> data) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(data, headers);
        ResponseEntity<String> entity = restTemplate.postForEntity(url, request, String.class);
        return entity;
    }

    public static ResponseEntity<String> get(String url, MultiValueMap<String, Object> data) {
        StringBuilder urlBuilder = new StringBuilder(url);
        HashMap<String, Object> dataMap = new HashMap<>();
        boolean isFirst = true;
        for (Map.Entry<String, List<Object>> entry : data.entrySet()) {
            if (isFirst) {
                urlBuilder.append('?');
                isFirst = false;
            } else {
                urlBuilder.append('&');
            }
            urlBuilder.append(entry.getKey()).append('=').append('{')
                    .append(entry.getKey()).append('}');
            dataMap.put(entry.getKey(), entry.getValue().get(0));
        }
        ResponseEntity<String> entity = restTemplate
                .getForEntity(urlBuilder.toString(), String.class, dataMap);
        return entity;
    }
}
