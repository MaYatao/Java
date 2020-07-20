package example.container.collection;


import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

/**
 * MyAbstractCollection：
 * 此类提供了Collection接口的基本实现，以最大程度地减少实现此接口所需的工作。
 * 要实现不可修改的集合，程序员只需扩展此类并为iterator和size方法提供实现。 （由迭代器方法返回的迭代器必须实现hasNext和next。）
 * 要实现可修改的集合，程序员必须另外重写此类的add方法（否则将引发UnsupportedOperationException），并且由迭代器方法返回的迭代器必须另外实现其删除方法。
 * 此类中每个非抽象方法的文档都详细描述了其实现。如果正在实现的集合允许更有效的实现，则可以覆盖这些方法中的每一个。
 */

public abstract class MyAbstractCollection<E> implements MyCollection<E> {

    protected MyAbstractCollection() {
    }

    /**
     * 迭代器
     */

    @Override
    public abstract Iterator<E> iterator();

    @Override
    public abstract int size();


    @Override
    public boolean isEmpty() {
        return size() == 0;
    }


    @Override
    public boolean contains(Object o) {
        //迭代集合
        Iterator<E> it = iterator();
        if (o == null) {
            while (it.hasNext()) {
                if (it.next() == null) {
                    return true;
                }
            }
        } else {
            while (it.hasNext()) {
                if (o.equals(it.next())) {
                    return true;
                }
            }
        }
        return false;
    }


    @Override
    public Object[] toArray() {

        Object[] r = new Object[size()];
        Iterator<E> it = iterator();
        for (int i = 0; i < r.length; i++) {
            if (!it.hasNext()) {
                //复制指定的数组，要复制的数组，要返回的副本的长度
                return Arrays.copyOf(r, i);
            }
            r[i] = it.next();
        }
        return it.hasNext() ? finishToArray(r, it) : r;
    }

    /**
     * 返回一个包含此集合中所有元素的数组； 返回数组的运行时类型是指定数组的运行时类型。
     */
    @SuppressWarnings("unchecked") //告诉编译器忽略 unchecked 警告信息，如使用List，ArrayList等未进行参数化产生的警告信息。
    @Override
    public <T> T[] toArray(T[] a) {

        int size = size();
        T[] r = a.length >= size ? a :
                (T[]) Array.newInstance(a.getClass().getComponentType(), size);
        Iterator<E> it = iterator();

        for (int i = 0; i < r.length; i++) {
            if (!it.hasNext()) {
                if (a == r) {
                    r[i] = null;
                } else if (a.length < i) {
                    return Arrays.copyOf(r, i);
                } else {
                    System.arraycopy(r, 0, a, 0, i);
                    if (a.length > i) {
                        a[i] = null;
                    }
                }
                return a;
            }
            r[i] = (T) it.next();
        }

        return it.hasNext() ? finishToArray(r, it) : r;
    }

    /**
     * 数组作为一个对象，需要一定的内存存储对象头信息，对象头信息最大占用内存不可超过8字节。
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;


    /**
     *当迭代器返回的元素比预期的多时，在toArray中重新分配使用的数组，并完成从迭代器填充的操作。
     * 返回数组，其中包含给定数组中的元素，以及迭代器返回的所有其他元素，被裁剪为大小
     */
    private static <T> T[] finishToArray(T[] r, Iterator<?> it) {
        int i = r.length;
        while (it.hasNext()) {
            int cap = r.length;
            if (i == cap) {
                //右移一位相当于除以2
                //新的数组大小=原始大小+原数组/2+1
                int newCap = cap + (cap >> 1) + 1;

                if (newCap - MAX_ARRAY_SIZE > 0) {
                    newCap = hugeCapacity(cap + 1);
                }
                r = Arrays.copyOf(r, newCap);
            }
            r[i++] = (T) it.next();
        }

        return (i == r.length) ? r : Arrays.copyOf(r, i);
    }

    /**
     *
     */
    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) {
            throw new OutOfMemoryError
                    ("Required array size too large");
        }
        return (minCapacity > MAX_ARRAY_SIZE) ?
                Integer.MAX_VALUE :
                MAX_ARRAY_SIZE;
    }


    /**
     *确保此集合包含指定的元素（可选操作）。 如果此集合由于调用而更改，则返回true。
     *  （如果此集合不允许重复并且已经包含指定的元素，则返回false。）
     * 支持此操作的集合可能会对可以添加到此集合的元素施加限制。
     * 特别是，某些集合将拒绝添加null元素，而其他集合将对可能添加的元素类型施加限制。
     * 集合类应在其文档中明确指定对可以添加哪些元素的任何限制。
     * 如果某个集合由于已经包含该元素以外的其他原因拒绝添加该元素，则它必须引发异常（而不是返回false）。
     * 这保留了不变，即在此调用返回之后，集合始终包含指定的元素。
     * 此实现始终抛出UnsupportedOperationException。
     */
    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }


    @Override
    public boolean remove(Object o) {
        Iterator<E> it = iterator();
        if (o == null) {
            while (it.hasNext()) {
                if (it.next() == null) {
                    it.remove();
                    return true;
                }
            }
        } else {
            while (it.hasNext()) {
                if (o.equals(it.next())) {
                    it.remove();
                    return true;
                }
            }
        }
        return false;
    }


    @Override
    public boolean containsAll(MyCollection<?> c) {
        for (Object e : c) {
            if (!contains(e)) {
                return false;
            }
        }
        return true;
    }


    @Override
    public boolean addAll(MyCollection<? extends E> c) {
        boolean modified = false;
        for (E e : c) {
            if (add(e)) {
                modified = true;
            }
        }
        return modified;
    }


    @Override
    public boolean removeAll(MyCollection<?> c) {
        Objects.requireNonNull(c);
        boolean modified = false;
        Iterator<?> it = iterator();
        while (it.hasNext()) {
            if (c.contains(it.next())) {
                it.remove();
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean retainAll(MyCollection<?> c) {
        Objects.requireNonNull(c);
        boolean modified = false;
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            if (!c.contains(it.next())) {
                it.remove();
                modified = true;
            }
        }
        return modified;
    }


    @Override
    public void clear() {
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
    }


    @Override
    public String toString() {
        Iterator<E> it = iterator();
        if (!it.hasNext()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (; ; ) {
            E e = it.next();
            sb.append(e == this ? "(this Collection)" : e);
            if (!it.hasNext()) {
                return sb.append(']').toString();
            }
            sb.append(',').append(' ');
        }
    }

}
