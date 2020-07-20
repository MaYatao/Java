
package example.container.collection;

import java.util.Iterator;


/**
 * MyDeque：双端队列
 * 支持在两端插入和删除元素的线性集合。
 */
public interface MyDeque<E> extends MyQueue<E> {

    /**
     *如果可以在不违反容量限制的情况下立即执行此操作，则将指定的元素插入此双端队列的前面，
     * 如果当前没有可用空间，则抛出IllegalStateException。
     */
    void addFirst(E e);

    /**
     *如果可以在不违反容量限制的情况下立即执行此操作，则将指定的元素插入此双端队列的后面，
     * 如果当前没有可用空间，则抛出IllegalStateException。
     */
    void addLast(E e);

    /**
     *将指定的元素插入此双端队列的前面，除非会违反容量限制。
     */
    boolean offerFirst(E e);

    /**
     *将指定的元素插入此双端队列的后，除非会违反容量限制。
     */
    boolean offerLast(E e);

    /**
     *检索并删除此双端队列的第一个元素。
     */
    E removeFirst();

    /**
     *检索并删除此双端队列的最后第一个元素。
     */
    E removeLast();

    /**
     *检索并删除此双端队列的第一个元素，如果此双端队列为空，则返回null。
     */
    E pollFirst();

    /**
     *检索并删除此双端队列的最后第一个元素，如果此双端队列为空，则返回null。
     */
    E pollLast();

    /**
     *检索但不删除此双端队列的第一个元素。
     */
    E getFirst();

    /**
     *检索但不删除此双端队列的最后第一个元素。
     */
    E getLast();

    /**
     *检索但不删除此双端队列的第一个元素，如果此双端队列为空，则返回null。
     */
    E peekFirst();

    /**
     *检索但不删除此双端队列的最后第一个元素，如果此双端队列为空，则返回null。
     */
    E peekLast();

    /**
     *从此双端队列删除指定元素的第一次出现。
     */
    boolean removeFirstOccurrence(Object o);

    /**
     *从此双端队列移除最后一次出现的指定元素。
     */
    boolean removeLastOccurrence(Object o);


    @Override
    boolean add(E e);


    @Override
    boolean offer(E e);


    @Override
    E remove();


    @Override
    E poll();


    @Override
    E element();


    @Override
    E peek();

    /**
     *如果有可能在不违反容量限制的情况下立即将元素压入此双端队列表示的堆栈（换句话说，此双端队列的头部），
     * 则在当前没有可用空间的情况下抛出IllegalStateException。
     */
    void push(E e);

    /**
     *从此双端队列表示的堆栈中弹出一个元素。
     */
    E pop();


    @Override
    boolean remove(Object o);


    @Override
    boolean contains(Object o);


    @Override
    public int size();


    @Override
    Iterator<E> iterator();

    /**
     *以相反的顺序返回此双端队列中的元素的迭代器。
     */
    Iterator<E> descendingIterator();

}
