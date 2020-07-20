
package example.container.collection;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public interface MyTransferQueue<E> extends MyBlockingQueue<E> {
    boolean tryTransfer(E e);

    void transfer(E e) throws InterruptedException;

    boolean tryTransfer(E e, long timeout, TimeUnit unit)
        throws InterruptedException;

   boolean hasWaitingConsumer();

    int getWaitingConsumerCount();
}
