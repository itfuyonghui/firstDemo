package com.ultrapower.pojo;

/**
 * Created by dell on 2018/12/27.
 */
public class LinkResult {
    private boolean result;
    private String message;

    public LinkResult(boolean result, String message) {
        this.result = result;
        this.message = message;
    }

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
