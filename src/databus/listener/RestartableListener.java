package databus.listener;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import databus.core.Publisher;
import databus.core.Restartable;


public abstract class RestartableListener extends AbstractListener implements Restartable {

    public RestartableListener(Publisher publisher) {
        setPublisher(publisher);     
    }
    
    public RestartableListener() {
        this(null);        
    }

    @Override
    public void start() {
        listeners.addLast(this);
        if ((null==monitor) || (monitor.getState()==Thread.State.TERMINATED)) {
            synchronized (lock) {
                if ((null==monitor) || (monitor.getState()==Thread.State.TERMINATED)) {
                    monitor = new Thread(new RunningMonitor(), "RestartableListener Monitor");
                    monitor.start();
                }
            }
        }
    }

    @Override
    public void stop() {
        listeners.remove(this);
        if (listeners.isEmpty() && (null!=monitor) && monitor.isAlive()) {
            synchronized (lock) {
                if (listeners.isEmpty() && (null!=monitor) && monitor.isAlive()) {
                    monitor.interrupt();
                }
            }
            try {
                log.info("Waiting RestartableListener.RunningMonitor");
                monitor.join();
            } catch (InterruptedException e) {
            }
        }
    }
    
    @Override
    public void join() throws InterruptedException {
        if (null != monitor) {
            monitor.join();
        }
    }

    final private static long TEN_SECONDS = 10000L;
    
    private static Log log = LogFactory.getLog(RestartableListener.class);
    private static Deque<Restartable> listeners = new ConcurrentLinkedDeque<Restartable>();
    private static Thread monitor = null;
    private static Object lock = new Object();
    
    private static class RunningMonitor implements Runnable {

        @Override
        public void run() {
            while(!listeners.isEmpty())  {
                try {
                    Thread.sleep(TEN_SECONDS);
                } catch (InterruptedException e) {
                    
                }
                for(Restartable listener : listeners) {
                    if (!listener.isRunning()) {
                        listener.restart();
                    }
                }
            }
        }
        
    }

}
