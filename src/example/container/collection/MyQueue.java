

package example.container.collection;


/**
 * MyQueue：队列，FIFO (first-in-first-out) 先入先出
 * 设计用于在处理之前容纳元素的集合。 除了基本的集合操作外，队列还提供其他插入，提取和检查操作。
 * 这些方法中的每一种都以两种形式存在：一种在操作失败时引发异常，另一种返回一个特殊值（根据操作而为null或false）。
 * 插入操作的后一种形式是专为与容量受限的Queue实现一起使用而设计的； 在大多数实现中，插入操作不会失败。
 */
public interface MyQueue<E> extends MyCollection<E> {

    /**
     * 如果可以立即将指定的元素插入此队列，而不会违反容量限制，则在成功时返回true，
     * 如果当前没有可用空间，则抛出IllegalStateException。
     */
    @Override
    boolean add(E e);

    /**
     * 如果可以在不违反容量限制的情况下立即将指定的元素插入此队列。
     */
    boolean offer(E e);
    /**
     *检索并删除此队列的头。
     */
    E remove();
    /**
     *检索并删除此队列的头部，如果此队列为空，则返回null。
     */
    E poll();

    /**
     *检索但不删除此队列的头。
     */
    E element();
    /**
     * 检索但不删除此队列的头部，如果此队列为空，则返回null。
     */
    E peek();
}
