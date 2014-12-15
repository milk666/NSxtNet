package io.nsxtnet.model;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.nsxtnet.http.Response;

import java.util.HashMap;
import java.util.Map;

public class NSxtNetHttpResponse {
    private int code = 200;
    private String desc = "OK";
    private Map<String, Object> content = new HashMap<String, Object>();

    public NSxtNetHttpResponse(Response response, HttpResponseStatus status) {
        super();
        this.code = status.code();
        this.desc = status.reasonPhrase();
        response.setResponseCode(this.code);
    }

    public NSxtNetHttpResponse(Response response, int code, String desc) {
        super();
        this.code = code;
        this.desc = desc;
        response.setResponseCode(this.code);
    }

    public NSxtNetHttpResponse(int code, String desc) {
        super();
        this.code = code;
        this.desc = desc;
    }

    public NSxtNetHttpResponse() {
        super();
    }

    public int getCode() {
        return code;
    }

    public NSxtNetHttpResponse setCode(int code) {
        this.code = code;
        return this;
    }

    public String getDesc() {
        return desc;
    }

    public NSxtNetHttpResponse setDesc(String desc) {
        this.desc = desc;
        return this;
    }

    public Map<String, Object> getContent() {
        return content;
    }

    public Map<?, ?> getContentChildMap(String child) {
        Object childObj = content.get(child);
        if (childObj instanceof Map<?, ?>) return (Map<?, ?>) childObj;
        return null;
    }

    public NSxtNetHttpResponse setContent(Map<String, Object> content) {
        this.content = content;
        return this;
    }

    public NSxtNetHttpResponse addContent(Map<String, Object> content) {
        this.content.putAll(content);
        return this;
    }

    public NSxtNetHttpResponse setContent(String key, Object value) {
        this.content.put(key, value);
        return this;
    }

    public NSxtNetHttpResponse setContentObject(String key, Object value) {
        this.content.put(key, value);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[code=");
        builder.append(code);
        builder.append(", desc=");
        builder.append(desc);
        builder.append(", content=");
        builder.append(content);
        builder.append("]");
        return builder.toString();
    }

    public boolean isOK() {
        return getCode() == 200;
    }
}
