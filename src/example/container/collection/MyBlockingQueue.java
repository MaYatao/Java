package example.container.collection;


import java.util.concurrent.TimeUnit;

public interface MyBlockingQueue<E> extends MyQueue<E> {

    @Override
    boolean add(E e);

   @Override
   boolean offer(E e);

    void put(E e) throws InterruptedException;

    boolean offer(E e, long timeout, TimeUnit unit)
        throws InterruptedException;

    E take() throws InterruptedException;

    E poll(long timeout, TimeUnit unit)
        throws InterruptedException;

     int remainingCapacity();

    @Override
    boolean remove(Object o);

   @Override
   public boolean contains(Object o);

    int drainTo(MyCollection<? super E> c);

    int drainTo(MyCollection<? super E> c, int maxElements);
}
