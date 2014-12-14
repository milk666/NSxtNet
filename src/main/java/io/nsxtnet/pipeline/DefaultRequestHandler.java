/*
 * Copyright 2009, Strategic Gains, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.nsxtnet.pipeline;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

import io.nsxtnet.contenttype.ContentType;
import io.nsxtnet.exception.DefaultExceptionMapper;
import io.nsxtnet.exception.ExceptionMapping;
import io.nsxtnet.exception.ExceptionUtils;
import io.nsxtnet.exception.ServiceException;
import io.nsxtnet.http.Request;
import io.nsxtnet.http.Response;
import io.nsxtnet.response.DefaultHttpResponseWriter;
import io.nsxtnet.response.HttpResponseWriter;
import io.nsxtnet.route.Action;
import io.nsxtnet.route.RouteResolver;
import io.nsxtnet.serialization.SerializationProvider;
import io.nsxtnet.serialization.SerializationSettings;
import io.nsxtnet.util.HttpSpecification;

import java.util.ArrayList;
import java.util.List;

/**
 * @author toddf
 * @since Nov 13, 2009
 */
@ChannelHandler.Sharable
public class DefaultRequestHandler extends SimpleChannelInboundHandler<Object>
{
	private static final AttributeKey<MessageContext> CONTEXT_KEY = AttributeKey.valueOf("context");

	// SECTION: INSTANCE VARIABLES

	private RouteResolver routeResolver;
	private SerializationProvider serializationProvider;
	private HttpResponseWriter responseWriter;
	private List<Preprocessor> preprocessors = new ArrayList<Preprocessor>();
	private List<Postprocessor> postprocessors = new ArrayList<Postprocessor>();
	private List<Postprocessor> finallyProcessors = new ArrayList<Postprocessor>();
	private ExceptionMapping exceptionMap = new DefaultExceptionMapper();
	private List<MessageObserver> messageObservers = new ArrayList<MessageObserver>();


	// SECTION: CONSTRUCTORS

	public DefaultRequestHandler(RouteResolver routeResolver, SerializationProvider serializationProvider)
	{
		this(routeResolver, serializationProvider, new DefaultHttpResponseWriter());
	}

	public DefaultRequestHandler(RouteResolver routeResolver, SerializationProvider serializationProvider,
								 HttpResponseWriter responseWriter)
	{
		super(true);
		this.routeResolver = routeResolver;
		this.serializationProvider = serializationProvider;
		setResponseWriter(responseWriter);
	}


	// SECTION: MUTATORS

	public void addMessageObserver(MessageObserver... observers)
	{
		for (MessageObserver observer : observers)
		{
			if (!messageObservers.contains(observer))
			{
				messageObservers.add(observer);
			}
		}
	}

	public <T extends Throwable, U extends ServiceException> DefaultRequestHandler mapException(Class<T> from, Class<U> to)
	{
		exceptionMap.map(from, to);
		return this;
	}

	public DefaultRequestHandler setExceptionMap(ExceptionMapping map)
	{
		this.exceptionMap = map;
		return this;
	}

	public HttpResponseWriter getResponseWriter()
	{
		return this.responseWriter;
	}

	public void setResponseWriter(HttpResponseWriter writer)
	{
		this.responseWriter = writer;
	}


	// SECTION: SIMPLE-CHANNEL-UPSTREAM-HANDLER

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception
	{
		ctx.flush();
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, Object msg)
			throws Exception
	{
		if (!(msg instanceof HttpRequest)) return;

		FullHttpRequest event = (FullHttpRequest) msg;
		MessageContext context = createInitialContext(ctx, event);

		try
		{
			notifyReceived(context);
			resolveRoute(context);
			resolveResponseProcessor(context);
			invokePreprocessors(preprocessors, context.getRequest());
			Object result = context.getAction().invoke(context.getRequest(), context.getResponse());

			if (result != null)
			{
				context.getResponse().setBody(result);
			}

			invokePostprocessors(postprocessors, context.getRequest(), context.getResponse());
			serializeResponse(context, false);
			enforceHttpSpecification(context);

			// TODO: this is a problem if a FinallyProcessor changes the response.  It will only work in 'accidentally' and intermittently.
			writeResponse(ctx, context);
			notifySuccess(context);
		}
		catch(Throwable t)
		{
			handleRestExpressException(ctx, t);
		}
		finally
		{
			// TODO: this is a problem if a FinallyProcessor changes the response.  It will only work in 'accidentally' and intermittently.
			invokeFinallyProcessors(finallyProcessors, context.getRequest(), context.getResponse());
			notifyComplete(context);
		}
	}

	private void resolveResponseProcessor(MessageContext context)
	{
		SerializationSettings s = serializationProvider.resolveResponse(context.getRequest(), context.getResponse(), false);
		context.setSerializationSettings(s);
	}

	/**
	 * @param context
	 */
	private void enforceHttpSpecification(MessageContext context)
	{
		HttpSpecification.enforce(context.getResponse());
	}

	private void handleRestExpressException(ChannelHandlerContext ctx, Throwable cause)
			throws Exception
	{
		Throwable rootCause = mapServiceException(cause);
		MessageContext context = ctx.attr(CONTEXT_KEY).get();

		if (rootCause != null) // was/is a ServiceException
		{
			context.setHttpStatus(((ServiceException) rootCause).getHttpStatus());

			if (ServiceException.class.isAssignableFrom(rootCause.getClass()))
			{
				((ServiceException) rootCause).augmentResponse(context.getResponse());
			}
		}
		else
		{
			rootCause = ExceptionUtils.findRootCause(cause);
			context.setHttpStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
		}

		context.setException(rootCause);
		notifyException(context);
		serializeResponse(context, true);
		writeResponse(ctx, context);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable)
			throws Exception
	{
		try
		{
			MessageContext messageContext = ctx.attr(CONTEXT_KEY).get();

			if (messageContext != null)
			{
				messageContext.setException(throwable);
				notifyException(messageContext);
			}
		}
		catch(Throwable t)
		{
			System.err.print("DefaultRequestHandler.exceptionCaught() threw an exception.");
			t.printStackTrace();
		}
		finally
		{
			ctx.channel().close();
		}
	}

	private MessageContext createInitialContext(ChannelHandlerContext ctx, FullHttpRequest httpRequest)
	{
		Request request = createRequest(httpRequest, ctx);
		Response response = createResponse();
		MessageContext context = new MessageContext(request, response);
		Attribute<MessageContext> attr = ctx.attr(CONTEXT_KEY);
		attr.set(context);
		return context;
	}

	private void resolveRoute(MessageContext context)
	{
		Action action = routeResolver.resolve(context.getRequest());
		context.setAction(action);
	}


	/**
	 * @param request
	 * @param response
	 */
	private void notifyReceived(MessageContext context)
	{
		for (MessageObserver observer : messageObservers)
		{
			observer.onReceived(context.getRequest(), context.getResponse());
		}
	}

	/**
	 * @param request
	 * @param response
	 */
	private void notifyComplete(MessageContext context)
	{
		for (MessageObserver observer : messageObservers)
		{
			observer.onComplete(context.getRequest(), context.getResponse());
		}
	}

	// SECTION: UTILITY -- PRIVATE

	/**
	 * @param exception
	 * @param request
	 * @param response
	 */
	private void notifyException(MessageContext context)
	{
		Throwable exception = context.getException();

		for (MessageObserver observer : messageObservers)
		{
			observer.onException(exception, context.getRequest(), context.getResponse());
		}
	}

	/**
	 * @param request
	 * @param response
	 */
	private void notifySuccess(MessageContext context)
	{
		for (MessageObserver observer : messageObservers)
		{
			observer.onSuccess(context.getRequest(), context.getResponse());
		}
	}

	public void addPreprocessor(Preprocessor handler)
	{
		if (!preprocessors.contains(handler))
		{
			preprocessors.add(handler);
		}
	}

	public void addPostprocessor(Postprocessor handler)
	{
		if (!postprocessors.contains(handler))
		{
			postprocessors.add(handler);
		}
	}

	public void addFinallyProcessor(Postprocessor handler)
	{
		if (!finallyProcessors.contains(handler))
		{
			finallyProcessors.add(handler);
		}
	}

	private void invokePreprocessors(List<Preprocessor> processors, Request request)
	{
		for (Preprocessor handler : processors)
		{
			handler.process(request);
		}

		request.getBody().resetReaderIndex();
	}

	private void invokePostprocessors(List<Postprocessor> processors, Request request, Response response)
	{
		for (Postprocessor handler : processors)
		{
			handler.process(request, response);
		}
	}

	private void invokeFinallyProcessors(List<Postprocessor> processors, Request request, Response response)
	{
		for (Postprocessor handler : processors)
		{
			try
			{
				handler.process(request, response);
			}
			catch(Throwable t)
			{
				t.printStackTrace(System.err);
			}
		}
	}

	/**
	 * Uses the exceptionMap to map a Throwable to a ServiceException, if possible.
	 *
	 * @param cause
	 * @return Either a ServiceException or the root cause of the exception.
	 */
	private Throwable mapServiceException(Throwable cause)
	{
		if (ServiceException.isAssignableFrom(cause))
		{
			return cause;
		}

		return exceptionMap.getExceptionFor(cause);
	}

	/**
	 * @param request
	 * @return
	 */
	private Request createRequest(FullHttpRequest httpRequest, ChannelHandlerContext context)
	{
		return new Request(context.channel().remoteAddress(), httpRequest, routeResolver, serializationProvider);
	}

	/**
	 * @param request
	 * @return
	 */
	private Response createResponse()
	{
		return new Response();
	}

	/**
	 * @param message
	 * @return
	 */
	private void writeResponse(ChannelHandlerContext ctx, MessageContext context)
	{
		getResponseWriter().write(ctx, context.getRequest(), context.getResponse());
	}

	private void serializeResponse(MessageContext context, boolean force)
	{
		Response response = context.getResponse();

		if (HttpSpecification.isContentTypeAllowed(response))
		{
			SerializationSettings settings = null;

			if (response.hasSerializationSettings())
			{
				settings = response.getSerializationSettings();
			}
			else if (force)
			{
				settings = serializationProvider.resolveResponse(context.getRequest(), response, force);
			}

			if (settings != null)
			{
				if (response.isSerialized())
				{
					String serialized = settings.serialize(response);

					if (serialized != null)
					{
						response.setBody(serialized);

						if (!response.hasHeader(HttpHeaders.Names.CONTENT_TYPE))
						{
							response.setContentType(settings.getMediaType());
						}
					}
				}
			}

			if (!response.hasHeader(HttpHeaders.Names.CONTENT_TYPE))
			{
				response.setContentType(ContentType.TEXT_PLAIN);
			}
		}
	}
}
