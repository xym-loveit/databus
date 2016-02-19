package databus.application;

import java.net.SocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import databus.listener.BatchListener;
import databus.network.Client;
import databus.network.Publisher;
import databus.network.Server;
import databus.network.Subscriber;

public class BothStartup {

    public static void main(String[] args) throws InterruptedException {                
        log.info("******************************************************************************");
        log.info("BothStartup will begin!");
        
        String configFileName = "conf/databus.xml";
        if (args.length > 0) {
            configFileName = args[0];
        }        
        Configurations config = new Configurations(configFileName);
        SocketAddress localAddress =config.serverAddress();
        Server server = new Server(localAddress, config.serverThreadPoolSize());
        Client client = new Client(config.clientThreadPoolSize());        

        Publisher publisher = new Publisher(client);
        Subscriber subscriber = new Subscriber();
        server.setPublisher(publisher).setSubscriber(subscriber);
        
        Thread serverThread = server.start();       
        Thread clientThread = client.start();
        
        BatchListener listener = config.loadListeners();
        listener.setPublisher(publisher);
        config.loadReceivers(subscriber);
        config.loadSubscribers(publisher);        

        listener.start();
        try {
            serverThread.join();
            clientThread.join();
        } finally {
            client.stop();
            server.stop();
            listener.stop();
        }
        
        log.info("BothStartup has finished!");
        System.exit(0);
    }
    
    private static Log log = LogFactory.getLog(BothStartup.class);
}