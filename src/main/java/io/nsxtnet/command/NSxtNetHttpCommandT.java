package io.nsxtnet.command;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.nsxtnet.http.Request;
import io.nsxtnet.http.Response;
import io.nsxtnet.model.NSxtNetHttpResponseT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

/**
 * The obligatory "Hello World!" showing a simple implementation of a
 * {@link com.netflix.hystrix.HystrixCommand}.
 * 
 * @param <T>
 * 
 * @param <T>
 */
public class NSxtNetHttpCommandT<T> extends HystrixCommand<NSxtNetHttpResponseT<T>> {
	private static final Logger log = LoggerFactory.getLogger(NSxtNetHttpCommandT.class);
	protected Request request;
	protected Response response;

	protected String qid = "request_id";
	protected String src = "playerKey_or_serverIp";

	public NSxtNetHttpCommandT(String groupName, Request request, Response response, int timeout) {
		super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupName)).andCommandPropertiesDefaults(
				HystrixCommandProperties.Setter().withExecutionIsolationThreadTimeoutInMilliseconds(timeout)));

		this.request = request;
		this.response = response;
		this.qid = request.getHeader("qid");
		this.src = request.getHeader("src");
	}

	public NSxtNetHttpCommandT(String groupName, Request request, Response response) {
		this(groupName, request, response, 2000);
	}

	public NSxtNetHttpCommandT(Request request, Response response) {
		this("dzix", request, response, 2000);
	}

	public static Constructor<?> getConstructor(Class<?> classHttpCommand) throws NoSuchMethodException,
			SecurityException {
		Class<?>[] constructorParameterTypes = { Request.class, Response.class };
		return classHttpCommand.getConstructor(constructorParameterTypes);
	}

	public NSxtNetHttpResponseT<T> makeResponseBadRequest() {
		ChannelHandlerContext ctx = (ChannelHandlerContext) request.getAttachment("ctx");
		log.debug("BAD_REQUEST : {}", ctx.channel());
		return new NSxtNetHttpResponseT<T>(response, HttpResponseStatus.BAD_REQUEST);
	}

	public NSxtNetHttpResponseT<T> makeResponseNotImplemented() {
		ChannelHandlerContext ctx = (ChannelHandlerContext) request.getAttachment("ctx");
		log.debug("NOT_IMPLEMENTED : {}", ctx.channel());
		return new NSxtNetHttpResponseT<T>(response, HttpResponseStatus.NOT_IMPLEMENTED);
	}

	@Override
	protected NSxtNetHttpResponseT<T> run() {
		return makeResponseNotImplemented();
	}

	@Override
	protected NSxtNetHttpResponseT<T> getFallback() {
		Throwable e = this.getFailedExecutionException();
		log.error(e.toString(), e.getCause());
		return new NSxtNetHttpResponseT<T>(response, HttpResponseStatus.SERVICE_UNAVAILABLE);
	}
}
