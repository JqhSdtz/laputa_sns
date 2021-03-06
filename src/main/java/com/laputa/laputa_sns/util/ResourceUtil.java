package com.laputa.laputa_sns.util;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;

import java.io.InputStream;

/**
 * @author JQH
 * @since 下午 11:02 21/03/01
 */
public class ResourceUtil {
    @SneakyThrows
    public static String getString(String path) {
        InputStream inputStream = ResourceUtil.class.getResourceAsStream(path);
        return IOUtils.toString(inputStream, "UTF-8");
    }
}
