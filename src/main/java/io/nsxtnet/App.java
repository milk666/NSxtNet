package io.nsxtnet;

import io.nsxtnet.config.Configuration;
import io.nsxtnet.config.Environment;
import io.nsxtnet.serialization.DefaultSerializationProvider;

import java.io.FileNotFoundException;
import java.io.IOException;

public class App 
{
    public static void main(String[] args) throws Exception
    {
        NSxtNet.setSerializationProvider(new DefaultSerializationProvider());
        Configuration config = loadEnvironment(args);
        NSxtNet server = new NSxtNet()
                .setName(config.getName())
                .setPort(config.getPort());

        defineRoutes(server, config);

        if (config.getWorkerCount() > 0)
        {
            server.setIoThreadCount(config.getWorkerCount());
        }

        if (config.getExecutorThreadCount() > 0)
        {
            server.setExecutorThreadCount(config.getExecutorThreadCount());
        }

        mapExceptions(server);
        server.bind();
        server.awaitShutdown();
    }

    /**
     * @param server
     * @param config
     */
    private static void defineRoutes(NSxtNet server, Configuration config)
    {
        // This route supports GET, POST, PUT, DELETE echoing the 'echo' query-string parameter in the response.
        // GET and DELETE are also supported but require an 'echo' header or query-string parameter.
        server.uri("/test", config.getEchoController());
    }

    /**
     * @param server
     */
    private static void mapExceptions(NSxtNet server)
    {
//    	server
//    	.mapException(ItemNotFoundException.class, NotFoundException.class)
//    	.mapException(DuplicateItemException.class, ConflictException.class)
//    	.mapException(ValidationException.class, BadRequestException.class);
    }

    private static Configuration loadEnvironment(String[] args) throws FileNotFoundException, IOException
    {
        String curDir = System.getProperty("user.dir");
        System.out.println(curDir);
        curDir = "../RestExpress-Examples/echo/config/dev/";

        if (args.length > 0)
        {
            return Environment.from(args[0], Configuration.class);
        }

        return Environment.from(curDir, Configuration.class);
        //return Environment.fromDefault(Configuration.class);
    }
}
