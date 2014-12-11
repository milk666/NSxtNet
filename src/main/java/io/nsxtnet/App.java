package io.nsxtnet;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "Start NSxtNet Server" );
        NSxtNet server = new NSxtNet();
        server.bind(10080);
    }
}
