package com.vivekgude.leastcount.util;

import com.vivekgude.leastcount.model.ApiResponse;

public class ResponseUtil {
    public static <T> ApiResponse<T> success(boolean success, String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<T> error(boolean success, String message) {
        return new ApiResponse<>(false, message, null, null);
    }
}
