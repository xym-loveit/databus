package test;

import java.text.DateFormat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import databus.core.Event;
import databus.core.Listener;
import databus.core.Publisher;
import databus.listener.MysqlListener;
import databus.network.MessageParser;
import databus.util.InternetAddress;

public class PubliserMain implements Publisher{
    Gson gson;    

    public PubliserMain() {
        gson = new GsonBuilder().enableComplexMapKeySerialization() 
                                .serializeNulls()   
                                .setDateFormat(DateFormat.LONG)
                                .create();
    }
    
    
    @Override
    public void publish(Event event) {
        System.out.println(event.type() +
                           "=" +
                           gson.toJson(event));
        
    }    
    
    
    @Override
    public void publish(InternetAddress remoteAddress, Event event) {
        // TODO Auto-generated method stub
        
    }


    public static void main(String[] args) {
      
        Publisher publisher = new PubliserMain();
        Listener listener = new MysqlListener(publisher);
        
        listener.start();
        System.out.println("!!!!!!!!!!!!!!!!!!");

    }


    @Override
    public void subscribe(String topic, InternetAddress remoteAddress) {
        // TODO Auto-generated method stub
        
    }


    @Override
    public void unsubscribe(String topic, InternetAddress remoteAddress) {
        // TODO Auto-generated method stub
        
    }
    

}
