package example.container.collection.current;



import example.container.collection.MyAbstractSet;
import example.container.collection.MyCollection;
import example.container.collection.MySet;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.function.Consumer;


public class MyCopyOnWriteArraySet<E> extends MyAbstractSet<E>
        implements java.io.Serializable {
    private static final long serialVersionUID = 5457747651344034263L;

    private final MyCopyOnWriteArrayList<E> al;


    public MyCopyOnWriteArraySet() {
        al = new MyCopyOnWriteArrayList<E>();
    }


    public MyCopyOnWriteArraySet(MyCollection<? extends E> c) {
        if (c.getClass() == MyCopyOnWriteArraySet.class) {
            @SuppressWarnings("unchecked")MyCopyOnWriteArraySet<E> cc =
                    (MyCopyOnWriteArraySet<E>)c;
            al = new MyCopyOnWriteArrayList<E>(cc.al);
        }
        else {
            al = new MyCopyOnWriteArrayList<E>();
            al.addAllAbsent(c);
        }
    }

    @Override
    public int size() {
        return al.size();
    }

    @Override
    public boolean isEmpty() {
        return al.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return al.contains(o);
    }

    @Override
    public Object[] toArray() {
        return al.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return al.toArray(a);
    }

    @Override
    public void clear() {
        al.clear();
    }

    @Override
    public boolean remove(Object o) {
        return al.remove(o);
    }

    @Override
    public boolean add(E e) {
        return al.addIfAbsent(e);
    }

    @Override
    public boolean containsAll(MyCollection<?> c) {
        return al.containsAll(c);
    }

    @Override
    public boolean addAll(MyCollection<? extends E> c) {
        return al.addAllAbsent(c) > 0;
    }

    @Override
    public boolean removeAll(MyCollection<?> c) {
        return al.removeAll(c);
    }

    @Override
    public boolean retainAll(MyCollection<?> c) {
        return al.retainAll(c);
    }

    @Override
    public Iterator<E> iterator() {
        return al.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof MySet)) {
            return false;
        }
        MySet<?> set = (MySet<?>)(o);
        Iterator<?> it = set.iterator();

        Object[] elements = al.getArray();
        int len = elements.length;

        boolean[] matched = new boolean[len];
        int k = 0;
        outer: while (it.hasNext()) {
            if (++k > len) {
                return false;
            }
            Object x = it.next();
            for (int i = 0; i < len; ++i) {
                if (!matched[i] && eq(x, elements[i])) {
                    matched[i] = true;
                    continue outer;
                }
            }
            return false;
        }
        return k == len;
    }
    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        return al.removeIf(filter);
    }
    @Override
    public void forEach(Consumer<? super E> action) {
        al.forEach(action);
    }

    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.spliterator
                (al.getArray(), Spliterator.IMMUTABLE | Spliterator.DISTINCT);
    }


    private static boolean eq(Object o1, Object o2) {
        return (o1 == null) ? o2 == null : o1.equals(o2);
    }
}
