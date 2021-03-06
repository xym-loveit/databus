package databus.receiver.mysql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSourceFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import databus.core.Event;
import databus.core.Receiver;

public abstract class MysqlReceiver implements Receiver{
    
    @Override
    public void initialize(Properties properties) {
        properties = removePrefix(properties, "mysql.");
        try {
            dataSource = BasicDataSourceFactory.createDataSource(properties);
        } catch (Exception e) {
            log.error("Can't creat DataSoruce for "+properties.toString(), e);
            System.exit(1);
        }                
    }   

    @Override
    public void receive(Event event) {
        try (Connection connection = dataSource.getConnection();){
            receive(connection, event);
        } catch (SQLException e) {
            log.error("Can't create Connection", e);
        }
    }

    abstract protected void receive(Connection conn, Event event);
    
    protected Properties removePrefix(Properties originalProperties, String prefix) {
        Properties properties = new Properties();
        int prefixLength = prefix.length();
        for(String key : originalProperties.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                String value = originalProperties.getProperty(key);
                properties.setProperty(key.substring(prefixLength), value);
            }
        }
        
        return properties;
    }
    
    protected String quoteReplacement(String message) {
        String replace = BSLASH_PATTERN.matcher(message).replaceAll("\\\\\\\\");
        replace = QUOTE_PATTERN.matcher(replace).replaceAll("\\\\'");
        return replace;
    }
    
    protected final static Pattern BSLASH_PATTERN = Pattern.compile("\\\\");
    protected final static Pattern QUOTE_PATTERN = Pattern.compile("\\'");
    
    private static Log log = LogFactory.getLog(MysqlReceiver.class);
    
    private DataSource dataSource = null;
}
