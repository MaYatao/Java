package example.container.collection;


import sun.corba.SharedSecrets;

import java.io.InvalidObjectException;
import java.util.*;


public class MyHashSet<E>
        extends MyAbstractSet<E>
        implements MySet<E>, Cloneable, java.io.Serializable
{
    static final long serialVersionUID = -502474406713321676L;

    private transient MyHashMap<E,Object> map;


    private static final Object PRESENT = new Object();


    public MyHashSet() {
        map = new MyHashMap<>();
    }


    public MyHashSet(MyCollection<? extends E> c) {
        map = new MyHashMap<>(Math.max((int) (c.size()/.75f) + 1, 16));
        addAll(c);
    }


    public MyHashSet(int initialCapacity, float loadFactor) {
        map = new MyHashMap<>(initialCapacity, loadFactor);
    }


    public MyHashSet(int initialCapacity) {
        map = new MyHashMap<>(initialCapacity);
    }


    MyHashSet(int initialCapacity, float lodFactor, boolean dummy) {
        map = new MyLinkedHashMap<>(initialCapacity, lodFactor);
    }

    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public boolean add(E e) {
        return map.put(e, PRESENT)==null;
    }

    @Override
    public boolean remove(Object o) {
        return map.remove(o)==PRESENT;
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object clone() {
        try {
            MyHashSet<E> newSet = (MyHashSet<E>) super.clone();
            newSet.map = (MyHashMap<E, Object>) map.clone();
            return newSet;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }


    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {

        s.defaultWriteObject();


        s.writeInt(map.capacity());
        s.writeFloat(map.loadFactor());


        s.writeInt(map.size());


        for (E e : map.keySet()) {
            s.writeObject(e);
        }
    }


    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {

        s.defaultReadObject();


        int capacity = s.readInt();
        if (capacity < 0) {
            throw new InvalidObjectException("Illegal capacity: " +
                    capacity);
        }


        float loadFactor = s.readFloat();
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new InvalidObjectException("Illegal load factor: " +
                    loadFactor);
        }


        int size = s.readInt();
        if (size < 0) {
            throw new InvalidObjectException("Illegal size: " +
                    size);
        }


        capacity = (int) Math.min(size * Math.min(1 / loadFactor, 4.0f),
                MyHashMap.MAXIMUM_CAPACITY);

        SharedSecrets.getJavaOISAccess()
                .checkArray(s, Map.Entry[].class, MyHashMap.tableSizeFor(capacity));

        map = (((MyHashSet<?>)this) instanceof MyLinkedHashSet ?
                new MyLinkedHashMap<E,Object>(capacity, loadFactor) :
                new MyHashMap<E,Object>(capacity, loadFactor));


        for (int i=0; i<size; i++) {
            @SuppressWarnings("unchecked")
            E e = (E) s.readObject();
            map.put(e, PRESENT);
        }
    }

    @Override
    public Spliterator<E> spliterator() {
        return new MyHashMap.KeySpliterator<E,Object>(map, 0, -1, 0, 0);
    }
}
