package databus.listener.mysql;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import databus.listener.RestartableListener;
import databus.network.netty.NettyPublisher;
import databus.util.Backup;

public class MysqlListener extends RestartableListener{  
    
    public MysqlListener(NettyPublisher publisher, Properties properties) {
        super(publisher);
        initialize(properties);        
    }

    public MysqlListener() {
        super();
    }
    
    @Override
    public boolean isRunning() {
        return  openRelicator.isRunning();
    }

    @Override
    public void restart() {
        openRelicator.restart();        
    }

    @Override
    public void stop() {
        try {
            openRelicator.stop(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Can't stop sucessfully", e);
            openRelicator.stopQuietly(1, TimeUnit.SECONDS);
        }
        super.stop();
    }

    @Override
    public void start() {        
        if(openRelicator.isRunning()) {
            return;
        }
        try {
            openRelicator.start();
            super.start();
        } catch (Exception e) {
            log.error("OpenRelicator throws a Exception",e);
        }
    }
    
    @Override
    public void initialize(Properties properties){        
        String user = properties.getProperty("mysql.user", "root");
        String password = properties.getProperty("mysql.password", "");
        String host = properties.getProperty("mysql.host", "127.0.0.1");
        int port = Integer.valueOf(properties.getProperty("mysql.port", "3306"));
        int serverId = Integer.valueOf(properties.getProperty("mysql.serverId", "1"));
        
        recordedId = "MysqlListener-" + host + "-" + serverId;
        loadBackup(properties);
        
        String binlogFileName = properties.getProperty("mysql.binlogFileName", "master-bin.000001");
        int nextPosition = Integer.valueOf(properties.getProperty("mysql.position", "1"));
        openRelicator = new DatabusOpenReplicator();
        openRelicator.setUser(user);
        openRelicator.setPassword(password);
        openRelicator.setHost(host);
        openRelicator.setPort(port);
        openRelicator.setServerId(serverId);
        openRelicator.setBinlogFileName(binlogFileName); 
        openRelicator.setBinlogPosition(nextPosition);
        openRelicator.setBinlogEventListener(new DatabusBinlogEventListener(this));
       
        loadPermittedTables(properties);

        MysqlDataSource ds = new MysqlDataSource();
        ds.setUser(user);
        ds.setPassword(password);
        ds.setServerName(host);
        ds.setPort(port);
        loadSchema(ds);
        
        
        Backup.instance()
              .store(recordedId, 
                     "mysql.binlogFileName", binlogFileName,
                     "mysql.position", Long.toString(nextPosition));
    }    
    
    public boolean doesPermit(String fullTableName) {
        return permittedTableSet.contains(fullTableName);
    }

    public void setNextPosition(long nextPosition) {
        openRelicator.setBinlogPosition(nextPosition);
        Backup.instance()
              .store(recordedId,
                     "mysql.position", Long.toString(nextPosition));
    }
    
    public void setBinlog(String binlogFileName, long nextPosition) {
        openRelicator.setBinlogFileName(binlogFileName);
        openRelicator.setBinlogPosition(nextPosition);
        Backup.instance()
              .store(recordedId, 
                     "mysql.binlogFileName", binlogFileName,
                     "mysql.position", Long.toString(nextPosition));
    }
    
    protected String[] getColumns(String fullName) {
        return columnsMap.get(fullName);
    }
    
    protected ColumnAttribute[] getTypes(String fullName) {
        return attributesMap.get(fullName);
    }
    
    protected Set<String> getPrimaryKeys(String fullName) {
        return primaryKeysMap.get(fullName);
    }
    
    private void loadPermittedTables(Properties config){        
        String rawTables = config.getProperty("mysql.permittedTables");
        if (null == rawTables) {
            log.error("mysql.permittedTables is null!");
            System.exit(1);
        }
        String[] tables = rawTables.split(",") ;
        permittedTableSet = new HashSet<String>();
        for(String t : tables) {
            permittedTableSet.add(t.trim().toLowerCase());
        }
    }
    
    private void loadBackup(Properties properties) {
        Map<String, String> backup = Backup.instance().restore(recordedId);
        if (null == backup) {
            return;
        }
        String backupBinfileName =  backup.get("mysql.binlogFileName");
        if (null != backupBinfileName) {
            properties.setProperty("mysql.binlogFileName", backupBinfileName);
        }
        String backupPosition = backup.get("mysql.position");
        if (null != backupPosition) {
            properties.setProperty("mysql.position", backupPosition);
        }
    }
    
    private void loadSchema(MysqlDataSource ds) {
        HashMap<String, Set<String>> tablesMap = new HashMap<String, Set<String>>();
        for(String fullName : permittedTableSet) {
            String[] r = fullName.split("\\.");
            if (r.length != 2) {
                log.error(fullName+" cannot be splitted normally");
                continue;
            }
            String databaseName = r[0].trim();
            String tableName = r[1].trim();
            Set<String> tables = tablesMap.get(databaseName);
            if (null == tables) {
                tables = new HashSet<String>();
                tablesMap.put(databaseName, tables);
            }
            tables.add(tableName);
        }
        
        columnsMap = new HashMap<String, String[]>();
        attributesMap = new HashMap<String, ColumnAttribute[]>();
        primaryKeysMap = new HashMap<String, Set<String>>();
        
        for(String databaseName : tablesMap.keySet()) {
            ds.setDatabaseName(databaseName);
            String fullName = databaseName;
            try (Connection conn = ds.getConnection();){
                DatabaseMetaData metaData = conn.getMetaData();  
                Set<String> tables = tablesMap.get(databaseName);
                for(String tableName : tables) {
                    LinkedList<String> columns = new LinkedList<String>();
                    LinkedList<ColumnAttribute> attribute = new LinkedList<ColumnAttribute>();
                    fullName = databaseName + "." + tableName;
                    try (ResultSet resultSet1 = metaData.getColumns(null, "%", tableName, "%");) {                                   
                        while (resultSet1.next()) {
                            columns.addLast(resultSet1.getString("COLUMN_NAME"));
                            int type = resultSet1.getInt("DATA_TYPE");
                            String typeName = resultSet1.getString("TYPE_NAME");
                            attribute.addLast(new ColumnAttribute(type, typeName));
                        }
                    }                
                    columnsMap.put(fullName, columns.toArray(new String[columns.size()]));
                    attributesMap.put(fullName, 
                                      attribute.toArray(new ColumnAttribute[attribute.size()]));
                    
                    HashSet<String> keys = new HashSet<String>();
                    ResultSet resultSet2 = metaData.getPrimaryKeys(null, null, tableName);
                    while(resultSet2.next()) {
                        keys.add(resultSet2.getString("COLUMN_NAME"));
                    }
                    primaryKeysMap.put(fullName, keys); 
                }
            } catch (SQLException e) {
                log.error("Cannot load the schema of "+fullName, e);
            }           
        } 
    }
    
    private static Log log = LogFactory.getLog(MysqlListener.class);

    private DatabusOpenReplicator openRelicator;
    private Map<String, String[]> columnsMap;
    private Map<String, ColumnAttribute[]> attributesMap;
    private Map<String, Set<String>> primaryKeysMap;
    private Set<String> permittedTableSet;
    
    private String recordedId;
}
