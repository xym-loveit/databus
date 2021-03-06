package databus.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import databus.core.Publisher;
import databus.core.Runner;
import databus.core.ThreadHolder;

public abstract class RunnableListener extends AbstractListener {    
        
    public RunnableListener(Publisher publisher, String name) {
        setPublisher(publisher);
        holder = new ThreadHolder(createListeningRunner());
    }
    
    public RunnableListener(String name) {
        this(null, name);
    }
    
    public RunnableListener() {
        this(RunnableListener.class.getSimpleName());
    }
    
    @Override
    public void start() {
        if (!holder.isRunning()) {
            holder.start();
        }
    }

    @Override
    public void stop() {
        holder.stop();      
    }    
    
    @Override
    public void join() throws InterruptedException {
        holder.join();        
    }

    protected abstract ListeningRunner createListeningRunner();

    
    protected static abstract class ListeningRunner implements Runner {        
        
        @Override
        public void runOnce() throws Exception {
            sleepingSeconds = 0;            
        }

        @Override
        public void processException(Exception e) {
            sleepingSeconds++;
            sleep(sleepingSeconds);          
        }
        
        private void sleep(long duration) {
            try {
                Thread.sleep(duration * 1000);
            } catch (InterruptedException e) {
                log.warn("Sleep has been interrupted", e);
            }
        }
        
        private int sleepingSeconds = 1;
    }
    
    private static Log log = LogFactory.getLog(RunnableListener.class);
    
    private ThreadHolder holder;    
    
}
