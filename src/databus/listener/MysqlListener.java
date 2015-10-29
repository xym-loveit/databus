package databus.listener;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.code.or.OpenReplicator;


import databus.core.Listener;
import databus.core.Publisher;
import databus.event.MysqlEvent;

public class MysqlListener  implements Listener{

    public MysqlListener(Publisher publisher) {
        this(publisher, CONFIG_FILE_NAME);
    }
    
    public MysqlListener(Publisher publisher, String configFileName) {
        this.configFileName = configFileName;
        this.publisher = publisher;
    }
    
    @Override
    public boolean isRunning() {
        return openRelicator.isRunning();
    }

    @Override
    public void stop() {
        try {
            openRelicator.stop(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("cannot stop sucessfully", e);
            openRelicator.stopQuietly(1, TimeUnit.SECONDS);
        }        
    }

    @Override
    public void start() {
        openRelicator = new OpenReplicator();
        initiate(openRelicator); 
        try {
            openRelicator.start();
        } catch (Exception e) {
            log.error("OpenRelicator throws a Exception",e);
        }
    }
    
    public void onEvent(MysqlEvent event) {
        publisher.publish(event);        
    }

    private void initiate(OpenReplicator openRelicator){
        Properties config = new Properties();
        try {
            config.load(new FileReader(configFileName));
        } catch (FileNotFoundException e) {
            log.error("Cannot find "+configFileName, e);
            System.exit(1);
        } catch (IOException e) {
           log.error("Cannot read "+configFileName, e);
           System.exit(1);
        }
        
        String user = config.getProperty(MYSQL_USER, "root");
        String password = config.getProperty(MYSQL_PASSWORD, "");
        String host = config.getProperty(MYSQL_HOST, "127.0.0.1");
        int port = Integer.valueOf(config.getProperty(MYSQL_PORT, "3306"));
        int serverId = Integer.valueOf(config.getProperty(MYSQL_SERVER_ID, "1"));
        String binlogFileName = config.getProperty(MYSQL_BINLOG_FILE_NAME, "aster-bin.000001");
        
        
        openRelicator.setUser(user);
        openRelicator.setPassword(password);
        openRelicator.setHost(host);
        openRelicator.setPort(port);
        openRelicator.setServerId(serverId);
        openRelicator.setBinlogFileName(binlogFileName); 
        openRelicator.setBinlogEventListener(new DatabusBinlogEventListener(this));
        
    }    
    
    
    final private static String CONFIG_FILE_NAME = "conf/listener.mysql.properties";
    
    final private static String MYSQL_USER = "listener.mysql.user";
    final private static String MYSQL_PASSWORD = "listener.mysql.password";
    final private static String MYSQL_HOST = "listener.mysql.host";
    final private static String MYSQL_PORT = "listener.mysql.port";
    final private static String MYSQL_SERVER_ID = "listener.mysql.serverId";
    final private static String MYSQL_BINLOG_FILE_NAME = "listener.mysql.binlogFileName";
    
    private static Log log = LogFactory.getLog(MysqlListener.class);
    
    private Publisher publisher;
    private OpenReplicator openRelicator;
    private String configFileName;

}