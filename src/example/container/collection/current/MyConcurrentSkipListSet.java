package example.container.collection.current;


import example.container.collection.*;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentSkipListMap;


public class MyConcurrentSkipListSet<E>
        extends MyAbstractSet<E>
        implements MyNavigableSet<E>, Cloneable, java.io.Serializable {

    private static final long serialVersionUID = -2479143111061671589L;


    private final MyConcurrentNavigableMap<E,Object> m;


    public MyConcurrentSkipListSet() {
        m = new MyConcurrentSkipListMap<E,Object>();
    }


    public MyConcurrentSkipListSet(Comparator<? super E> comparator) {
        m = new MyConcurrentSkipListMap<E,Object>(comparator);
    }


    public MyConcurrentSkipListSet(MyCollection<? extends E> c) {
        m = new MyConcurrentSkipListMap<E,Object>();
        addAll(c);
    }


    public MyConcurrentSkipListSet(MySortedSet<E> s) {
        m = new MyConcurrentSkipListMap<E,Object>(s.comparator());
        addAll(s);
    }


    MyConcurrentSkipListSet(MyConcurrentNavigableMap<E,Object> m) {
        this.m = m;
    }

    @Override
    public MyConcurrentSkipListSet<E> clone() {
        try {
            @SuppressWarnings("unchecked")
            MyConcurrentSkipListSet<E> clone =
                    (MyConcurrentSkipListSet<E>) super.clone();
            clone.setMap(new MyConcurrentSkipListMap<E,Object>(m));
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }



    @Override
    public int size() {
        return m.size();
    }

    @Override
    public boolean isEmpty() {
        return m.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return m.containsKey(o);
    }

    @Override
    public boolean add(E e) {
        return m.putIfAbsent(e, Boolean.TRUE) == null;
    }

    @Override
    public boolean remove(Object o) {
        return m.remove(o, Boolean.TRUE);
    }

    @Override
    public void clear() {
        m.clear();
    }

    @Override
    public Iterator<E> iterator() {
        return m.navigableKeySet().iterator();
    }

    @Override
    public Iterator<E> descendingIterator() {
        return m.descendingKeySet().iterator();
    }




    @Override
    public boolean equals(Object o) {

        if (o == this) {
            return true;
        }
        if (!(o instanceof Set)) {
            return false;
        }
        MyCollection<?> c = (MyCollection<?>) o;
        try {
            return containsAll(c) && c.containsAll(this);
        } catch (ClassCastException unused) {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }
    }

    @Override
    public boolean removeAll(MyCollection<?> c) {

        boolean modified = false;
        for (Object e : c) {
            if (remove(e)) {
                modified = true;
            }
        }
        return modified;
    }



    @Override
    public E lower(E e) {
        return m.lowerKey(e);
    }

    @Override
    public E floor(E e) {
        return m.floorKey(e);
    }

    @Override
    public E ceiling(E e) {
        return m.ceilingKey(e);
    }

    @Override
    public E higher(E e) {
        return m.higherKey(e);
    }
    @Override
    public E pollFirst() {
        MyMap.Entry<E,Object> e = m.pollFirstEntry();
        return (e == null) ? null : e.getKey();
    }
    @Override
    public E pollLast() {
        MyMap.Entry<E,Object> e = m.pollLastEntry();
        return (e == null) ? null : e.getKey();
    }



    @Override

    public Comparator<? super E> comparator() {
        return m.comparator();
    }

    @Override
    public E first() {
        return m.firstKey();
    }

    @Override
    public E last() {
        return m.lastKey();
    }

    @Override
    public MyNavigableSet<E> subSet(E fromElement,
                                  boolean fromInclusive,
                                  E toElement,
                                  boolean toInclusive) {
        return new MyConcurrentSkipListSet<E>
                (m.subMap(fromElement, fromInclusive,
                        toElement,   toInclusive));
    }

    @Override
    public MyNavigableSet<E> headSet(E toElement, boolean inclusive) {
        return new MyConcurrentSkipListSet<E>(m.headMap(toElement, inclusive));
    }

    @Override
    public MyNavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return new MyConcurrentSkipListSet<E>(m.tailMap(fromElement, inclusive));
    }

    @Override
    public  MyNavigableSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }
    @Override
    public MyNavigableSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public MyNavigableSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public MyNavigableSet<E> descendingSet() {
        return new MyConcurrentSkipListSet<E>(m.descendingMap());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Spliterator<E> spliterator() {
        if (m instanceof ConcurrentSkipListMap) {
            return ((MyConcurrentSkipListMap<E,?>)m).keySpliterator();
        } else {
            return (Spliterator<E>)((MyConcurrentSkipListMap.SubMap<E,?>)m).keyIterator();
        }
    }


    private void setMap(MyConcurrentNavigableMap<E,Object> map) {
        UNSAFE.putObjectVolatile(this, mapOffset, map);
    }

    private static final sun.misc.Unsafe UNSAFE;
    private static final long mapOffset;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = MyConcurrentSkipListSet.class;
            mapOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("m"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
