/*
 * Copyright 2010, eCollege, Inc.  All rights reserved.
 */
package io.nsxtnet.pipeline;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;

/**
 * Provides a tiny DSL to define the pipeline features.
 * 
 * @author toddf
 * @since Aug 27, 2010
 */
public class PipelineInitializer extends ChannelInitializer<SocketChannel>
{
	// SECTION: CONSTANTS

	private static final int DEFAULT_MAX_CONTENT_LENGTH = 20480;


	// SECTION: INSTANCE VARIABLES

	private ChannelHandler requestHandler;
	private int maxContentLength = DEFAULT_MAX_CONTENT_LENGTH;


	// SECTION: CONSTRUCTORS

	public PipelineInitializer()
	{
		super();
	}


	// SECTION: BUILDER METHODS

	public PipelineInitializer setRequestHandler(ChannelHandler handler)
	{
		requestHandler = handler;
		return this;
	}

	/**
	 * Set the maximum length of the aggregated (chunked) content. If the length of the
	 * aggregated content exceeds this value, a TooLongFrameException will be raised during
	 * the request, which can be mapped in the RestExpress server to return a
	 * BadRequestException, if desired.
	 *
	 * @param value
	 * @return this PipelineBuilder for method chaining.
	 */
	public PipelineInitializer setMaxContentLength(int value)
	{
		this.maxContentLength = value;
		return this;
	}


	// SECTION: CHANNEL PIPELINE FACTORY

	@Override
	public void initChannel(SocketChannel ch)
			throws Exception
	{
		ChannelPipeline pipeline = ch.pipeline();

		pipeline.addLast("decoder", new HttpRequestDecoder());
		pipeline.addLast("aggregator", new HttpObjectAggregator(maxContentLength));
		pipeline.addLast("encoder", new HttpResponseEncoder());
		pipeline.addLast("chunkWriter", new ChunkedWriteHandler());

		// TODO: fix GZIP handling in the pipeline
//		pipeline.addLast("inflater", new HttpContentDecompressor());
//		pipeline.addLast("deflater", new HttpContentCompressor());
		pipeline.addLast(requestHandler.getClass().getSimpleName(), requestHandler);
	}
}
