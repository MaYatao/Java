package example.container.collection;


import java.util.Iterator;
import java.util.Spliterator;

/**
 * 不包含重复元素的集合
 * 更正式地讲，集合不包含元素对e1和e2，使得e1.equals（e2），最多包含一个空元素
 */

public interface MySet<E> extends MyCollection<E> {


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


    @Override
    boolean add(E e);


    @Override
    boolean remove(Object o);



    @Override
    boolean containsAll(MyCollection<?> c);


    @Override
    boolean addAll(MyCollection<? extends E> c);


    @Override
    boolean retainAll(MyCollection<?> c);


    @Override
    boolean removeAll(MyCollection<?> c);


    @Override
    void clear();


    @Override
    boolean equals(Object o);


    @Override
    int hashCode();


    @Override
    default Spliterator<E> spliterator() {
        return MySpliterators.spliterator(this, Spliterator.DISTINCT);
    }
}
