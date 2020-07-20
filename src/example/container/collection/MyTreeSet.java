package example.container.collection;



import java.util.*;



public class MyTreeSet<E> extends MyAbstractSet<E>
        implements MyNavigableSet<E>, Cloneable, java.io.Serializable
{

    private transient MyNavigableMap<E,Object> m;


    private static final Object PRESENT = new Object();


    MyTreeSet(MyNavigableMap<E,Object> m) {
        this.m = m;
    }


    public MyTreeSet() {
        this(new MyTreeMap<E,Object>());
    }


    public MyTreeSet(Comparator<? super E> comparator) {
        this(new MyTreeMap<>(comparator));
    }


    public MyTreeSet(MyCollection<? extends E> c) {
        this();
        addAll(c);
    }


    public MyTreeSet(MySortedSet<E> s) {
        this(s.comparator());
        addAll(s);
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
    public MyNavigableSet<E> descendingSet() {
        return new MyTreeSet<>(m.descendingMap());
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
        return m.put(e, PRESENT)==null;
    }

    @Override
    public boolean remove(Object o) {
        return m.remove(o)==PRESENT;
    }

    @Override
    public void clear() {
        m.clear();
    }

    @Override
    public  boolean addAll(MyCollection<? extends E> c) {

        if (m.size()==0 && c.size() > 0 &&
                c instanceof SortedSet &&
                m instanceof MyTreeMap) {
            SortedSet<? extends E> set = (SortedSet<? extends E>) c;
            MyTreeMap<E,Object> map = (MyTreeMap<E, Object>) m;
            Comparator<?> cc = set.comparator();
            Comparator<? super E> mc = map.comparator();
            if (cc==mc || (cc != null && cc.equals(mc))) {
                map.addAllForTreeSet(set, PRESENT);
                return true;
            }
        }
        return super.addAll(c);
    }

    @Override
    public MyNavigableSet<E> subSet(E fromElement, boolean fromInclusive,
                                  E toElement,   boolean toInclusive) {
        return new MyTreeSet<>(m.subMap(fromElement, fromInclusive,
                toElement,   toInclusive));
    }

    @Override
    public MyNavigableSet<E> headSet(E toElement, boolean inclusive) {
        return new MyTreeSet<>(m.headMap(toElement, inclusive));
    }

    @Override
    public MyNavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return new MyTreeSet<>(m.tailMap(fromElement, inclusive));
    }

    @Override
    public MySortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public MySortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public MySortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
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
        MyMap.Entry<E,?> e = m.pollFirstEntry();
        return (e == null) ? null : e.getKey();
    }

    @Override
    public E pollLast() {
        MyMap.Entry<E,?> e = m.pollLastEntry();
        return (e == null) ? null : e.getKey();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object clone() {
        MyTreeSet<E> clone;
        try {
            clone = (MyTreeSet<E>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
        clone.m = new MyTreeMap<>(m);
        return clone;
    }


    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {

        s.defaultWriteObject();

        s.writeObject(m.comparator());

        s.writeInt(m.size());

        for (E e : m.keySet()) {
            s.writeObject(e);
        }
    }


    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {

        s.defaultReadObject();

        @SuppressWarnings("unchecked")
        Comparator<? super E> c = (Comparator<? super E>) s.readObject();

        MyTreeMap<E,Object> tm = new MyTreeMap<>(c);
        m = tm;

        int size = s.readInt();

        tm.readTreeSet(size, s, PRESENT);
    }

    @Override
    public Spliterator<E> spliterator() {
        return MyTreeMap.keySpliteratorFor(m);
    }

    private static final long serialVersionUID = -2479143000061671589L;
}
