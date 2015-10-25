package databus.event;

import databus.core.Event;

public interface ManagementEvent extends Event{
    
    public static enum Type {SUBSCRIPTION, COUNTERMAND}
    
    public String ipAddress();
    
    public int port();
    
    public String topic();
}