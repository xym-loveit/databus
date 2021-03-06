package databus.event;

import java.net.InetAddress;

import databus.core.Event;

public abstract class AbstractEvent implements Event{

    @Override
    public long time() {
        return time;
    }
    
    @Override
    public Event time(long time) {
        this.time = time;
        return this;
    }
    
    @Override
    public InetAddress ipAddress() {
        return ipAddress;
    }

    @Override
    public Event ipAddress(InetAddress ipAddress) {
        this.ipAddress = ipAddress;
        return this;
    }
    
    public String fullTopic() {
        return null==ipAddress ? null : ipAddress.getHostAddress() + topic();
    }

    private InetAddress ipAddress;
    private long time;
}
