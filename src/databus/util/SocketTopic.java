package databus.util;

import java.net.InetSocketAddress;

public class SocketTopic extends IpTopic {
    
    public SocketTopic(InetSocketAddress socketAddress, String topic) {
        super(socketAddress.getAddress(), topic);
        this.socketAddress = socketAddress;
        hashCode =(socketAddress.toString() + topic).hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof SocketTopic) {
            SocketTopic o = (SocketTopic) other;
            return super.equals(o) && (port()==o.port());            
        }
        return false;
    }

    @Override
    public String toString() {
        return socketAddress.toString() + topic();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
    
    public int port() {
        return socketAddress.getPort();
    }
    
    public InetSocketAddress socketAddress() {
        return socketAddress;
    }
    
    private InetSocketAddress socketAddress;

}
