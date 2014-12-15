package io.nsxtnet.controller;


import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsPoller;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.nsxtnet.http.Request;
import io.nsxtnet.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author toddf
 * @since Aug 31, 2010
 */
public class MonitorStreamController implements NSxtNetRestController {
	private static final Logger log = LoggerFactory.getLogger(MonitorStreamController.class);

	/* used to track number of connections and throttle */
	private static AtomicInteger concurrentConnections = new AtomicInteger(0);
	private static DynamicIntProperty maxConcurrentConnections = DynamicPropertyFactory.getInstance().getIntProperty(
			"hystrix.stream.maxConcurrentConnections", 5);

	@Override
	public Object postMethod(Request request, Response response) {
		/*return new JsonHttpResponse(response, HttpResponseStatus.NOT_IMPLEMENTED);*/
		return null;
	}

	@Override
	public Object getMethod(Request request, Response response) {
		return  null;
/*		new MonitorStream().execute();

		*//* ensure we aren't allowing more connections than we want *//*
		int numberConnections = concurrentConnections.incrementAndGet();

		int delay = 500;
		try {
			String d = (String) request.getParameter("delay");
			if (d != null) {
				delay = Integer.parseInt(d);
			}
		} catch (Exception e) {
			// ignore if it's not a number
		}

		HystrixMetricsPoller poller = null;
		try {
			if (numberConnections > maxConcurrentConnections.get()) {
				response.setResponseCode(503);
				log.error("MaxConcurrentConnections reached: " + maxConcurrentConnections.get());
				// response.sendError(503, "MaxConcurrentConnections reached: "
				// + maxConcurrentConnections.get());

				return new JsonHttpResponse(response, "MaxConcurrentConnections reached: "
						+ maxConcurrentConnections.get());
			} else {

				HttpResponse httpResponse = new DefaultHttpResponse(HTTP_1_1, OK);

				*//* initialize response *//*
				httpResponse.setHeader("Content-Type", "text/event-stream");
				httpResponse.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
				httpResponse.setHeader("Pragma", "no-cache");

				ChannelHandlerContext ctx = (ChannelHandlerContext) request.getAttachment("ctx");
				ctx.channel().write(httpResponse);

				MetricJsonListener jsonListener = new MetricJsonListener();
				poller = new HystrixMetricsPoller(jsonListener, delay);

				// start polling and it will write directly to the output stream
				poller.start();
				log.info("Starting poller");

				// we will use a "single-writer" approach where the Servlet
				// thread does all the writing
				// by fetching JSON messages from the MetricJsonListener to
				// write them to the output
				try {
					while (poller.isRunning() && ctx.getChannel().isOpen()) {
						List<String> jsonMessages = jsonListener.getJsonMetrics();

						if (jsonMessages.isEmpty()) {
							// https://github.com/Netflix/Hystrix/issues/85
							// hystrix.stream holds connection open if no
							// metrics
							// we send a ping to test the connection so that
							// we'll get an IOException if the client has
							// disconnected
							ctx.getChannel().write(ChannelBuffers.copiedBuffer("ping: \n", CharsetUtil.UTF_8))
									.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
						} else {
							for (String json : jsonMessages) {
								ctx.getChannel()
										.write(ChannelBuffers.copiedBuffer("data: " + json + "\n\n", CharsetUtil.UTF_8))
										.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
							}
						}

						// after outputting all the messages we will flush the
						// stream
						// response.flushBuffer();

						// now wait the 'delay' time
						Thread.sleep(delay);
					}
				} catch (Exception e) {
					poller.shutdown();
					log.error("Failed to write. Will stop polling.", e);
				}
				log.debug("Stopping Turbine stream to connection");
			}
		} catch (Exception e) {
			log.error("Error initializing servlet for metrics event stream.", e);
		} finally {
			concurrentConnections.decrementAndGet();
			if (poller != null) {
				poller.shutdown();
			}
		}

		return new JsonHttpResponse(response, "MonitorStream Connection Closed");*/
	}

	/**
	 * This will be called from another thread so needs to be thread-safe.
	 */
	@ThreadSafe
	private static class MetricJsonListener implements HystrixMetricsPoller.MetricsAsJsonPollerListener {

		/**
		 * Setting limit to 1000. In a healthy system there isn't any reason to
		 * hit this limit so if we do it will throw an exception which causes
		 * the poller to stop.
		 * <p>
		 * This is a safety check against a runaway poller causing memory leaks.
		 */
		private final LinkedBlockingQueue<String> jsonMetrics = new LinkedBlockingQueue<String>(1000);

		/**
		 * Store JSON messages in a queue.
		 */
		@Override
		public void handleJsonMetric(String json) {
			jsonMetrics.add(json);
		}

		/**
		 * Get all JSON messages in the queue.
		 * 
		 * @return
		 */
		public List<String> getJsonMetrics() {
			ArrayList<String> metrics = new ArrayList<String>();
			jsonMetrics.drainTo(metrics);
			return metrics;
		}
	}

	/**
	 * The obligatory "Hello World!" showing a simple implementation of a
	 * {@link com.netflix.hystrix.HystrixCommand}.
	 */
	public class MonitorStream extends HystrixCommand<String> {

		public MonitorStream() {
			super(HystrixCommandGroupKey.Factory.asKey("Monitor"));
		}

		@Override
		protected String run() {
			return "";
		}
	}
}
