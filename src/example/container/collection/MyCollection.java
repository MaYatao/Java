

package example.container.collection;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * MyCollection： 集合层次结构中的根接口，定义来属于集合的基本方法
 * 同时继承了Iterable接口，可循环集合内元素
 */
public interface MyCollection<E> extends Iterable<E> {

    /***
     * 返回集合内元素个数
     */
    int size();

    /***
     * 如果集合元素为空返回 true。
     */
    boolean isEmpty();

    /***
     * 返回 true如果集合包含指定元素。
     */

    boolean contains(Object o);

    /**
     * 接口Iterable中的抽象方法
     * 返回此集合中的元素的迭代器。
     */

    Iterator<E> iterator();

    /**
     * 返回包含此集合中所有元素的数组
     */
    Object[] toArray();

    /**
     * 返回一个包含此集合中所有元素的数组； 返回数组的运行时类型是指定数组的运行时类型。
     */
    <T> T[] toArray(T[] a);

    // Modification Operations

    /**
     * 确保此集合包含指定的元素（可选操作）。
     */
    boolean add(E e);

    /**
     * 从这个集合中移除指定元素的一个实例，如果它是存在的
     */
    boolean remove(Object o);


    /****
     * 返回 true如果这个集合包含指定集合的所有元素。
     */
    boolean containsAll(MyCollection<?> c);

    /**
     * 把一个集合的元素添加到该集合
     */
    boolean addAll(MyCollection<? extends E> c);

    /**
     * 删除此集合中包含的所有元素（可选操作）的所有元素
     */
    boolean removeAll(MyCollection<?> c);

    /**
     * 删除此集合中满足给定谓词的所有元素。在迭代过程中或谓词所引发的错误或运行时异常被传递给调用方。
     * 实现要求：
     * 默认实现遍历集合的元素使用 iterator()。每个匹配的元素，删除使用 Iterator.remove()。如果集合的迭代器不支持去除然后 UnsupportedOperationException将抛出第一个匹配的元素。
     * 参数：
     * filter-谓词，对于要删除的元素返回true
     * 结果：
     * 如果删除了任何元素，则为true
     */
    default boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter);
        boolean removed = false;
        final Iterator<E> each = iterator();
        while (each.hasNext()) {
            if (filter.test(each.next())) {
                each.remove();
                removed = true;
            }
        }
        return removed;
    }

    /**
     * 仅保留包含在指定集合中的这个集合中的元素（可选操作）
     */
    boolean retainAll(MyCollection<?> c);

    /**
     * 从这个集合中移除所有的元素（可选操作）。
     */

    void clear();


    /**
     * Object类中的方法 ， 将指定的对象与此集合进行比较，以进行相等性。
     */
    @Override
    boolean equals(Object o);

    /**
     * Object类中的方法 ，返回此集合的哈希代码值。
     */
    @Override
    int hashCode();

    /**
     * 创建此集合中的元素的 Spliterator。
     */
    @Override
    default Spliterator<E> spliterator() {
        return MySpliterators.spliterator(this, 0);
    }

    /**
     * 返回一个以该集合为源的序列流。
     */

    default Stream<E> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /**
     * 返回一个以集合为源的能并行序列流。
     */
    default Stream<E> parallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }
}
