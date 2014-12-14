/*
 * Copyright 2010, eCollege, Inc.  All rights reserved.
 */
package io.nsxtnet.response;

import io.netty.channel.ChannelHandlerContext;
import io.nsxtnet.http.Request;
import io.nsxtnet.http.Response;

/**
 * @author toddf
 * @since Aug 26, 2010
 */
public interface HttpResponseWriter
{
	public void write(ChannelHandlerContext ctx, Request request, Response response);
}
