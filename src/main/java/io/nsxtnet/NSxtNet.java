package io.nsxtnet;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import io.nsxtnet.config.ServerSettings;
import io.nsxtnet.config.SocketSettings;
import io.nsxtnet.domain.metadata.ServerMetadata;
import io.nsxtnet.exception.DefaultExceptionMapper;
import io.nsxtnet.exception.ExceptionMapping;
import io.nsxtnet.exception.ServiceException;
import io.nsxtnet.pipeline.*;
import io.nsxtnet.route.RouteBuilder;
import io.nsxtnet.route.RouteDeclaration;
import io.nsxtnet.config.RouteDefaults;
import io.nsxtnet.route.RouteResolver;
import io.nsxtnet.route.ParameterizedRouteBuilder;
import io.nsxtnet.serialization.DefaultSerializationProvider;
import io.nsxtnet.serialization.SerializationProvider;
import io.nsxtnet.util.Callback;
import io.nsxtnet.util.DefaultShutdownHook;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by kosh on 2014-12-09.
 */
public class NSxtNet {

    public static final String DEFAULT_NAME = "NSxtNet";
    public static final int DEFAULT_PORT = 10080;

    private static SerializationProvider SERIALIZATION_PROVIDER = null;

    private ServerBootstrap bootstrap;
    private SocketSettings socketSettings = new SocketSettings();
    private ServerSettings serverSettings = new ServerSettings();
    private RouteDefaults routeDefaults = new RouteDefaults();
    private Channel channel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;


    private List<MessageObserver> messageObservers = new ArrayList<MessageObserver>();
    private List<Preprocessor> preprocessors = new ArrayList<Preprocessor>();
    private List<Postprocessor> postprocessors = new ArrayList<Postprocessor>();
    private List<Postprocessor> finallyProcessors = new ArrayList<Postprocessor>();
    private RouteDeclaration routeDeclarations = new RouteDeclaration();
    private ExceptionMapping exceptionMap = new DefaultExceptionMapper();

    /**
     * Change the default behavior for serialization.
     * If no SerializationProcessor is set, default of DefaultSerializationProcessor is used,
     * which uses Jackson for JSON, XStream for XML.
     *
     * @param provider a SerializationProvider instance.
     */
    public static void setSerializationProvider(SerializationProvider provider)
    {
        SERIALIZATION_PROVIDER = provider;
    }

    public static SerializationProvider getSerializationProvider()
    {
        if (SERIALIZATION_PROVIDER == null)
        {
            SERIALIZATION_PROVIDER = new DefaultSerializationProvider();
        }

        return SERIALIZATION_PROVIDER;
    }

    /**
     * Create a new RestExpress service. By default, RestExpress uses port 8081.
     * Supports JSON, and XML, providing JSEND-style wrapped responses. And
     * displays some messages on System.out. These can be altered with the
     * setPort(), noJson(), noXml(), noSystemOut(), and useRawResponses() DSL
     * modifiers, respectively, as needed.
     *
     * <p/>
     * The default input and output format for messages is JSON. To change that,
     * use the setDefaultFormat(String) DSL modifier, passing the format to use
     * by default. Make sure there's a corresponding SerializationProcessor for
     * that particular format. The Format class has the basics.
     *
     * <p/>
     * This DSL was created as a thin veneer on Netty functionality. The bind()
     * method simply builds a Netty pipeline and uses this builder class to
     * create it. Underneath the covers, RestExpress uses Google GSON for JSON
     * handling and XStream for XML processing. However, both of those can be
     * swapped out using the putSerializationProcessor(String,
     * SerializationProcessor) method, creating your own instance of
     * SerializationProcessor as necessary.
     *
     * @param routes
     *            a RouteDeclaration that declares the URL routes that this
     *            service supports.
     */
    public NSxtNet()
    {
        super();
        setName(DEFAULT_NAME);
    }

    public String getBaseUrl()
    {
        return routeDefaults.getBaseUrl();
    }

    public NSxtNet setBaseUrl(String baseUrl)
    {
        routeDefaults.setBaseUrl(baseUrl);
        return this;
    }

    /**
     * Get the name of this RestExpress service.
     *
     * @return a String representing the name of this service suite.
     */
    public String getName()
    {
        return serverSettings.getName();
    }

    /**
     * Set the name of this RestExpress service suite.
     *
     * @param name
     *            the name.
     * @return the RestExpress instance to facilitate DSL-style method chaining.
     */
    public NSxtNet setName(String name)
    {
        serverSettings.setName(name);
        return this;
    }

    public int getPort()
    {
        return serverSettings.getPort();
    }

    public NSxtNet setPort(int port)
    {
        serverSettings.setPort(port);
        return this;
    }

    public NSxtNet addMessageObserver(MessageObserver observer)
    {
        if (!messageObservers.contains(observer))
        {
            messageObservers.add(observer);
        }

        return this;
    }

    public List<MessageObserver> getMessageObservers()
    {
        return Collections.unmodifiableList(messageObservers);
    }

    /**
     * Add a Preprocessor instance that gets called before an incoming message
     * gets processed. Preprocessors get called in the order in which they are
     * added. To break out of the chain, simply throw an exception.
     *
     * @param processor
     * @return
     */
    public NSxtNet addPreprocessor(Preprocessor processor)
    {
        if (!preprocessors.contains(processor))
        {
            preprocessors.add(processor);
        }

        return this;
    }

    public List<Preprocessor> getPreprocessors()
    {
        return Collections.unmodifiableList(preprocessors);
    }

    /**
     * Add a Postprocessor instance that gets called after an incoming message is
     * processed. A Postprocessor is useful for augmenting or transforming the
     * results of a controller or adding headers, etc. Postprocessors get called
     * in the order in which they are added.
     * Note however, they do NOT get called in the case of an exception or error
     * within the route.
     *
     * @param processor
     * @return
     */
    public NSxtNet addPostprocessor(Postprocessor processor)
    {
        if (!postprocessors.contains(processor))
        {
            postprocessors.add(processor);
        }

        return this;
    }

    public List<Postprocessor> getPostprocessors()
    {
        return Collections.unmodifiableList(postprocessors);
    }

    /**
     * Add a Postprocessor instance that gets called in a finally block after
     * the message is processed.  Finally processors are Postprocessor instances
     * that are guaranteed to run even if an error is thrown from the controller
     * or somewhere else in the route.  A Finally Processor is useful for adding
     * headers or transforming results even during error conditions. Finally
     * processors get called in the order in which they are added.
     *
     * If an exception is thrown during finally processor execution, the finally processors
     * following it are executed after printing a stack trace to the System.err stream.
     *
     * @param processor
     * @return RestExpress for method chaining.
     */
    public NSxtNet addFinallyProcessor(Postprocessor processor)
    {
        if (!postprocessors.contains(processor))
        {
            postprocessors.add(processor);
        }

        return this;
    }

    public List<Postprocessor> getFinallyProcessors()
    {
        return Collections.unmodifiableList(finallyProcessors);
    }

    public boolean useTcpNoDelay()
    {
        return socketSettings.useTcpNoDelay();
    }

    public NSxtNet setUseTcpNoDelay(boolean useTcpNoDelay)
    {
        socketSettings.setUseTcpNoDelay(useTcpNoDelay);
        return this;
    }

    public boolean useKeepAlive()
    {
        return serverSettings.isKeepAlive();
    }

    public NSxtNet setKeepAlive(boolean useKeepAlive)
    {
        serverSettings.setKeepAlive(useKeepAlive);
        return this;
    }

    public boolean shouldReuseAddress()
    {
        return serverSettings.isReuseAddress();
    }

    public NSxtNet setReuseAddress(boolean reuseAddress)
    {
        serverSettings.setReuseAddress(reuseAddress);
        return this;
    }

    public int getSoLinger()
    {
        return socketSettings.getSoLinger();
    }

    public NSxtNet setSoLinger(int soLinger)
    {
        socketSettings.setSoLinger(soLinger);
        return this;
    }

    public int getReceiveBufferSize()
    {
        return socketSettings.getReceiveBufferSize();
    }

    public NSxtNet setReceiveBufferSize(int receiveBufferSize)
    {
        socketSettings.setReceiveBufferSize(receiveBufferSize);
        return this;
    }

    public int getConnectTimeoutMillis()
    {
        return socketSettings.getConnectTimeoutMillis();
    }

    public NSxtNet setConnectTimeoutMillis(int connectTimeoutMillis)
    {
        socketSettings.setConnectTimeoutMillis(connectTimeoutMillis);
        return this;
    }

    /**
     *
     * @param elementName
     * @param theClass
     * @return
     */
    public NSxtNet alias(String elementName, Class<?> theClass)
    {
        routeDefaults.addXmlAlias(elementName, theClass);
        return this;
    }

    public <T extends Exception, U extends ServiceException> NSxtNet mapException(
            Class<T> from, Class<U> to)
    {
        exceptionMap.map(from, to);
        return this;
    }

    public NSxtNet setExceptionMap(ExceptionMapping mapping)
    {
        this.exceptionMap = mapping;
        return this;
    }

    /**
     * Return the number of requested NIO/HTTP-handling worker threads.
     *
     * @return the number of requested worker threads.
     */
    public int getIoThreadCount()
    {
        return serverSettings.getIoThreadCount();
    }

    /**
     * Set the number of NIO/HTTP-handling worker threads.  This
     * value controls the number of simultaneous connections the
     * application can handle.
     *
     * The default (if this value is not set, or set to zero) is
     * the Netty default, which is 2 times the number of processors
     * (or cores).
     *
     * @param value the number of desired NIO worker threads.
     * @return the RestExpress instance.
     */
    public NSxtNet setIoThreadCount(int value)
    {
        serverSettings.setIoThreadCount(value);
        return this;
    }

    /**
     * Returns the number of background request-handling (executor) threads.
     *
     * @return the number of executor threads.
     */
    public int getExecutorThreadCount()
    {
        return serverSettings.getExecutorThreadPoolSize();
    }

    /**
     * Set the number of background request-handling (executor) threads.
     * This value controls the number of simultaneous blocking requests that
     * the server can handle.  For longer-running requests, a higher number
     * may be indicated.
     *
     * For VERY short-running requests, a value of zero will cause no
     * background threads to be created, causing all processing to occur in
     * the NIO (front-end) worker thread.
     *
     * @param value the number of executor threads to create.
     * @return the RestExpress instance.
     */
    public NSxtNet setExecutorThreadCount(int value)
    {
        serverSettings.setExecutorThreadPoolSize(value);
        return this;
    }

    /**
     * Set the maximum length of the content in a request. If the length of the content exceeds this value,
     * the server closes the connection immediately without sending a response.
     *
     * @param size the maximum size in bytes.
     * @return the RestExpress instance.
     */
    public NSxtNet setMaxContentSize(int size)
    {
        serverSettings.setMaxContentSize(size);
        return this;
    }

    /**
     * Can be called after routes are defined to augment or get data from
     * all the currently-defined routes.
     *
     * @param callback a Callback implementor.
     */
    public void iterateRouteBuilders(Callback<RouteBuilder> callback)
    {
        routeDeclarations.iterateRouteBuilders(callback);
    }

    public Channel bind()
    {
        return bind((getPort() > 0 ? getPort() : DEFAULT_PORT));
    }

    /**
     * The last call in the building of a RestExpress server, bind() causes
     * Netty to bind to the listening address and process incoming messages.
     *
     * @return Channel
     */
    public Channel bind(int port)
    {
        setPort(port);

        // Configure the server.
        if (getIoThreadCount() > 0)
        {
            bossGroup = new NioEventLoopGroup(getIoThreadCount());
        }
        else
        {
            bossGroup = new NioEventLoopGroup();
        }

        if (getExecutorThreadCount() > 0)
        {
            workerGroup = new NioEventLoopGroup(getExecutorThreadCount());
        }
        else
        {
            workerGroup = new NioEventLoopGroup();
        }

        bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class);

        // Set up the event pipeline factory.
        DefaultRequestHandler requestHandler = new DefaultRequestHandler(createRouteResolver(), SERIALIZATION_PROVIDER);

        // Add MessageObservers to the request handler here, if desired...
        //requestHandler.addMessageObserver(messageObservers.toArray(new MessageObserver[0]));
        requestHandler.addMessageObserver( new SimpleConsoleLogMessageObserver() );
        requestHandler.setExceptionMap(exceptionMap);

        // Add pre/post processors to the request handler here...
        addPreprocessors(requestHandler);
        addPostprocessors(requestHandler);
        addFinallyProcessors(requestHandler);

        PipelineInitializer pf = new PipelineInitializer()
                .setRequestHandler(requestHandler)
                .setMaxContentLength(serverSettings.getMaxContentSize());

        bootstrap.childHandler(pf);
        setBootstrapOptions();

        // Bind and start to accept incoming connections.
        channel = bootstrap.bind(new InetSocketAddress(port)).channel();
        return channel;
    }

    private void setBootstrapOptions()
    {
        bootstrap.option(ChannelOption.TCP_NODELAY, useTcpNoDelay());
        bootstrap.option(ChannelOption.SO_KEEPALIVE, serverSettings.isKeepAlive());
        bootstrap.option(ChannelOption.SO_REUSEADDR, shouldReuseAddress());
        bootstrap.option(ChannelOption.SO_LINGER, getSoLinger());
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, getConnectTimeoutMillis());
        bootstrap.option(ChannelOption.SO_RCVBUF, getReceiveBufferSize());
    }

    /**
     * Used in main() to install a default JVM shutdown hook and shut down the
     * server cleanly. Calls shutdown() when JVM termination detected. To
     * utilize your own shutdown hook(s), install your own shutdown hook(s) and
     * call shutdown() instead of awaitShutdown().
     */
    public void awaitShutdown()
    {
        Runtime.getRuntime().addShutdownHook(new DefaultShutdownHook(this));
        boolean interrupted = false;

        do
        {
            try
            {
                Thread.sleep(300);
            }
            catch (InterruptedException e)
            {
                interrupted = true;
            }
        }
        while (!interrupted);
    }

    /**
     * Releases all resources associated with this server so the JVM can
     * shutdown cleanly. Call this method to finish using the server. To utilize
     * the default shutdown hook in main() provided by RestExpress, call
     * awaitShutdown() instead.
     */
    public void shutdown()
    {
        channel.closeFuture(); //.syncUninterruptibly();
        bossGroup.shutdownGracefully(); //.awaitUninterruptibly();
        workerGroup.shutdownGracefully(); //.awaitUninterruptibly();
    }

    /**
     * @return
     */
    private RouteResolver createRouteResolver()
    {
        return new RouteResolver(routeDeclarations.createRouteMapping(routeDefaults));
    }

    /**
     * @return
     */
    public ServerMetadata getRouteMetadata()
    {
        ServerMetadata m = new ServerMetadata();
        m.setName(getName());
        m.setPort(getPort());
        //TODO: create a good substitute for this...
//		m.setDefaultFormat(getDefaultFormat());
//		m.addAllSupportedFormats(getResponseProcessors().keySet());
        m.addAllRoutes(routeDeclarations.getMetadata());
        return m;
    }

    /**
     * @param requestHandler
     */
    private void addPreprocessors(DefaultRequestHandler requestHandler)
    {
        for (Preprocessor processor : getPreprocessors())
        {
            requestHandler.addPreprocessor(processor);
        }
    }

    /**
     * @param requestHandler
     */
    private void addPostprocessors(DefaultRequestHandler requestHandler)
    {
        for (Postprocessor processor : getPostprocessors())
        {
            requestHandler.addPostprocessor(processor);
        }
    }

    /**
     * @param requestHandler
     */
    private void addFinallyProcessors(DefaultRequestHandler requestHandler)
    {
        for (Postprocessor processor : getFinallyProcessors())
        {
            requestHandler.addFinallyProcessor(processor);
        }
    }


    // SECTION: ROUTE CREATION
    public ParameterizedRouteBuilder uri(String uriPattern, Object controller)
    {
        return routeDeclarations.uri(uriPattern, controller, routeDefaults);
    }
}
