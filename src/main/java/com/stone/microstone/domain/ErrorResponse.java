package com.stone.microstone.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorResponse {
    private int code;
    private String message;


    public ErrorResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
