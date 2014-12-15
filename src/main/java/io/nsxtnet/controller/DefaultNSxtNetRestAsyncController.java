package io.nsxtnet.controller;

import com.netflix.hystrix.HystrixCommand;

import io.netty.handler.codec.http.HttpResponseStatus;

import io.nsxtnet.http.Request;
import io.nsxtnet.http.Response;
import io.nsxtnet.model.NSxtNetHttpResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observer;

import java.lang.reflect.Constructor;

public class DefaultNSxtNetRestAsyncController implements NSxtNetRestController {
	private static final Logger log = LoggerFactory.getLogger(DefaultNSxtNetRestAsyncController.class);
	private Class<?> classHttpCommand;

	@Override
	public Object postMethod(Request request, Response response) {
		return processAsync(request, response);
	}

	@Override
	public Object getMethod(Request request, Response response) {
		return processAsync(request, response);
	}

	private Object processAsync(final Request request, final Response response) {
		try {
			Constructor<?> constructor = getCmdConstructor();
			HystrixCommand<?> instance = (HystrixCommand<?>) constructor.newInstance(request, response);
			Observable<?> fResult = (Observable<?>) instance.observe();
			fResult.subscribe(new Observer<Object>() {

				@Override
				public void onCompleted() {
					log.debug("completed : {}", request.getUrl());
				}

				@Override
				public void onError(Throwable e) {
					log.error(e.toString(), e.getCause());
/*					HttpServerHandler hsh = (HttpServerHandler) request.getAttachment("hsh");
					hsh.processResponse(request, response, true,
							new NSxtNetHttpResponse(response, HttpResponseStatus.GONE.getCode(), e.getCause().getClass()
									.getSimpleName()));*/
				}

				@Override
				public void onNext(Object result) {
/*					HttpServerHandler hsh = (HttpServerHandler) request.getAttachment("hsh");
					hsh.processResponse(request, response, true, result);*/
				}
			});

		} catch (Throwable e) {
			log.error("Exception", e);
			return new NSxtNetHttpResponse(response, HttpResponseStatus.NOT_IMPLEMENTED.code(), e.getCause().getClass().getSimpleName());

		} finally {
		}
		return this;
	}

	private Constructor<?> getCmdConstructor() throws NoSuchMethodException {
		Class<?>[] constructorParameterTypes = { Request.class, Response.class };
		Constructor<?> constructor = classHttpCommand.getConstructor(constructorParameterTypes);
		return constructor;
	}

	public boolean isValid() {
		try {
			Constructor<?> constructor = getCmdConstructor();
		} catch (NoSuchMethodException e) {
			return false;
		}
		return true;
	}

	public DefaultNSxtNetRestAsyncController(Class<?> classHttpCommand) {
		this.classHttpCommand = classHttpCommand;
	}
}
