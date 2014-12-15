package io.nsxtnet.model;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.nsxtnet.http.Response;

public class NSxtNetHttpResponseT<T> {
	private int code = 200;
	private String desc = "OK";
	private T content;

	public NSxtNetHttpResponseT(Response response, HttpResponseStatus status) {
		super();
		this.code = status.code();
		this.desc = status.reasonPhrase();
		response.setResponseCode(this.code);
	}

	public NSxtNetHttpResponseT(Response response, int code, String desc) {
		super();
		this.code = code;
		this.desc = desc;
		response.setResponseCode(this.code);
	}

	public NSxtNetHttpResponseT(int code, String desc) {
		super();
		this.code = code;
		this.desc = desc;
	}

	public NSxtNetHttpResponseT() {
		super();
	}

	public int getCode() {
		return code;
	}

	public NSxtNetHttpResponseT<T> setCode(int code) {
		this.code = code;
		return this;
	}

	public String getDesc() {
		return desc;
	}

	public NSxtNetHttpResponseT<T> setDesc(String desc) {
		this.desc = desc;
		return this;
	}

	public T getContent() {
		return content;
	}

	public NSxtNetHttpResponseT<T> setContent(T content) {
		this.content = content;
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
