package com.laputa.laputa_sns.service;

import com.laputa.laputa_sns.common.Result;
import com.laputa.laputa_sns.common.Secrets;
import com.laputa.laputa_sns.model.entity.Operator;
import com.laputa.laputa_sns.util.CryptUtil;
import com.upyun.FormUploader;
import com.upyun.Params;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;

/**
 * 对象存储服务，调用又拍云的对象存储接口
 *
 * @author JQH
 * @since 下午 2:34 20/04/08
 */

@Service
public class OSSService {

    public static final int AVATAR = 0;
    public static final int CATEGORY = 1;
    public static final int POST = 2;
    public static final int COMMENT = 3;

    private final String BUCKET_NAME = "img-lpt";
    private final String[] FILENAME_PREFIX = {"ava", "cat", "pst", "com"};

    public Result<String> uploadImgSync(byte[] file, int fileType, @NotNull Operator operator) {
        //一次只能上传一个
        boolean hasSigned;
        if (fileType == POST || fileType == COMMENT) {
            hasSigned = !operator.getUserId().equals(-1);
        } else {
            hasSigned = !operator.getUserId().equals(-1) || operator.getUser().getQqOpenId() != null;
        }
        if (!hasSigned) {
            return new Result<String>(Result.FAIL).setErrorCode(1010150201).setMessage("未登录");
        }
        FormUploader uploader = new FormUploader(BUCKET_NAME, Secrets.OSS_OPERATOR_NAME, Secrets.OSS_OPERATOR_PWD);
        final Map<String, Object> paramsMap = new HashMap<>();
        String path = getPath(fileType, operator);
        paramsMap.put(Params.SAVE_KEY, path);
        String style = FILENAME_PREFIX[fileType] + ".standard";
        if (fileType == POST || fileType == COMMENT) {
            String mark = Base64Utils.encodeToString(operator.getUser().getNickName().getBytes());
            style += "/watermark/text/" + mark + "/size/16/color/FFFFFF/font/simhei/margin/25x10/align/southeast/animate/true";
        }
        paramsMap.put(Params.X_GMKERL_THUMB, style);
        try {
            com.upyun.Result result = uploader.upload(paramsMap, file);
            if (!result.isSucceed()) {
                return new Result<String>(Result.FAIL).setErrorCode(1010150202).setMessage(result.toString());
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return new Result<String>(Result.SUCCESS).setObject(path);
    }

    @NotNull
    private String getPath(int fileType, @NotNull Operator operator) {
        if (operator.getUserId().equals(-1) && operator.getUser().getQqOpenId() != null) {
            return FILENAME_PREFIX[fileType] + "-" + CryptUtil.randUrlSafeStr(5, false)
                    + operator.getUser().getQqOpenId();
        } else {
            return FILENAME_PREFIX[fileType] + "-" + CryptUtil.longToStr(System.currentTimeMillis()) + CryptUtil.randUrlSafeStr(5, false)
                    + CryptUtil.longToStr(operator.getUserId());
        }
    }

}
