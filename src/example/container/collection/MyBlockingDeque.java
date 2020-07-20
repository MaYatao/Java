

package example.container.collection;
import java.util.*;
import java.util.concurrent.TimeUnit;

public interface MyBlockingDeque<E> extends MyBlockingQueue<E>, MyDeque<E> {


   @Override
   void addFirst(E e);

   @Override
   void addLast(E e);


    @Override
    boolean offerFirst(E e);

    @Override
    boolean offerLast(E e);

    void putFirst(E e) throws InterruptedException;

    void putLast(E e) throws InterruptedException;

    boolean offerFirst(E e, long timeout, TimeUnit unit)
        throws InterruptedException;

    boolean offerLast(E e, long timeout, TimeUnit unit)
        throws InterruptedException;

    E takeFirst() throws InterruptedException;

    E takeLast() throws InterruptedException;

    E pollFirst(long timeout, TimeUnit unit)
        throws InterruptedException;

    E pollLast(long timeout, TimeUnit unit)
        throws InterruptedException;

   @Override
   boolean removeFirstOccurrence(Object o);

    @Override
    boolean removeLastOccurrence(Object o);

    // *** BlockingQueue methods ***

   @Override
   boolean add(E e);

    @Override
    boolean offer(E e);

   void put(E e) throws InterruptedException;


    boolean offer(E e, long timeout, TimeUnit unit)
        throws InterruptedException;


    @Override
    E remove();


    @Override
    E poll();


    E take() throws InterruptedException;


    E poll(long timeout, TimeUnit unit)
        throws InterruptedException;


    @Override
    E element();


    @Override
    E peek();

    @Override
    boolean remove(Object o);


    @Override
    public boolean contains(Object o);


    @Override
    public int size();


    @Override
    Iterator<E> iterator();

    // *** Stack methods ***

    @Override
    void push(E e);
}
