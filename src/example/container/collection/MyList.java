package example.container.collection;

import java.util.*;
import java.util.function.UnaryOperator;

/**
 * MyList一个有序集合（也被称为序列）。
 * 此接口的用户在列表中的每个元素都被插入的地方有精确的控制。
 * 用户可以通过它们的整数索引（在列表中的位置）访问元素，并在列表中搜索元素。
 */

public interface MyList<E> extends MyCollection<E> {


    @Override
    int size();


    @Override
    boolean isEmpty();


    @Override
    boolean contains(Object o);


    @Override
    Iterator<E> iterator();


    @Override
    Object[] toArray();

    @Override
    <T> T[] toArray(T[] a);


    //  修改操作

    @Override
    boolean add(E e);

    @Override
    boolean remove(Object o);


    // 批量修改操作

    @Override
    boolean containsAll(MyCollection<?> c);

    @Override
    boolean addAll(MyCollection<? extends E> c);

    boolean addAll(int index, MyCollection<? extends E> c);

    @Override
    boolean removeAll(MyCollection<?> c);


    @Override
    boolean retainAll(MyCollection<?> c);

    /**
     * 用将该运算符应用于该元素的结果替换此列表中的每个元素。
     */
    default void replaceAll(UnaryOperator<E> operator) {
        Objects.requireNonNull(operator);
        final ListIterator<E> li = this.listIterator();
        while (li.hasNext()) {
            li.set(operator.apply(li.next()));
        }
    }
    /**
     *根据由指定比较器引起的顺序对该列表进行排序。
     */
    default void sort(Comparator<? super E> c) {
        Object[] a = this.toArray();
        Arrays.sort(a, (Comparator) c);
        ListIterator<E> i = this.listIterator();
        for (Object e : a) {
            i.next();
            i.set((E) e);
        }
    }

    @Override
    void clear();


    // 比较和哈希

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();


    // 位置访问操作
    /**
     *返回此列表中指定位置的元素。
     */
    E get(int index);
    /**
     * 用指定的元素替换此列表中指定位置的元素。
     */
    E set(int index, E element);
    /**
     *将指定的元素插入此列表中的指定位置
     */
    void add(int index, E element);
    /**
     *删除此列表中指定位置的元素
     */
    E remove(int index);


    // 搜索操作
    /**
     *返回指定元素在此列表中首次出现的索引；如果此列表不包含该元素，则返回-1。
     */
    int indexOf(Object o);
    /**
     *返回指定元素在此列表中最后一次出现的索引；如果此列表不包含该元素，则返回-1。
     */
    int lastIndexOf(Object o);


    // 集合迭代

    /**
     *返回此列表中的元素的列表迭代器（按适当顺序）。
     */
    ListIterator<E> listIterator();
    /**
     *
     * 从列表中的指定位置开始，以适当的顺序返回在此列表中的元素上的列表迭代器。
     */
    ListIterator<E> listIterator(int index);

    // 视图

    /**
     *返回此列表中指定的fromIndex（包括）和toIndex（不包括）之间的视图。
     */
    MyList<E> subList(int fromIndex, int toIndex);


    @Override
    default Spliterator<E> spliterator() {
        return MySpliterators.spliterator(this, Spliterator.ORDERED);
    }
}
