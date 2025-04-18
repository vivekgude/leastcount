package com.vivekgude.leastcount.model;

import lombok.Data;

@Data
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private Object metadata;

    public ApiResponse(boolean success, String message, T data, Object metadata) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.metadata = metadata;
    }
}
