package example.container.collection;



import java.lang.ref.WeakReference;
import java.lang.ref.ReferenceQueue;


import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;



public class MyWeakHashMap<K,V>
        extends MyAbstractMap<K,V>
        implements MyMap<K,V> {


    private static final int DEFAULT_INITIAL_CAPACITY = 16;


    private static final int MAXIMUM_CAPACITY = 1 << 30;


    private static final float DEFAULT_LOAD_FACTOR = 0.75f;


    MyWeakHashMap.Entry<K,V>[] table;


    private int size;


    private int threshold;


    private final float loadFactor;


    private final ReferenceQueue<Object> queue = new ReferenceQueue<>();


    int modCount;

    @SuppressWarnings("unchecked")
    private MyWeakHashMap.Entry<K,V>[] newTable(int n) {
        return (MyWeakHashMap.Entry<K,V>[]) new MyWeakHashMap.Entry<?,?>[n];
    }


    public MyWeakHashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal Initial Capacity: "+
                    initialCapacity);
        }
        if (initialCapacity > MAXIMUM_CAPACITY) {
            initialCapacity = MAXIMUM_CAPACITY;
        }

        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Illegal Load factor: "+
                    loadFactor);
        }
        int capacity = 1;
        while (capacity < initialCapacity) {
            capacity <<= 1;
        }
        table = newTable(capacity);
        this.loadFactor = loadFactor;
        threshold = (int)(capacity * loadFactor);
    }


    public MyWeakHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }


    public MyWeakHashMap() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }


    public MyWeakHashMap(MyMap<? extends K, ? extends V> m) {
        this(Math.max((int) (m.size() / DEFAULT_LOAD_FACTOR) + 1,
                DEFAULT_INITIAL_CAPACITY),
                DEFAULT_LOAD_FACTOR);
        putAll(m);
    }




    private static final Object NULL_KEY = new Object();


    private static Object maskNull(Object key) {
        return (key == null) ? NULL_KEY : key;
    }


    static Object unmaskNull(Object key) {
        return (key == NULL_KEY) ? null : key;
    }


    private static boolean eq(Object x, Object y) {
        return x == y || x.equals(y);
    }


    final int hash(Object k) {
        int h = k.hashCode();




        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }


    private static int indexFor(int h, int length) {
        return h & (length-1);
    }


    private void expungeStaleEntries() {
        for (Object x; (x = queue.poll()) != null; ) {
            synchronized (queue) {
                @SuppressWarnings("unchecked")
                MyWeakHashMap.Entry<K,V> e = (MyWeakHashMap.Entry<K,V>) x;
                int i = indexFor(e.hash, table.length);

                MyWeakHashMap.Entry<K,V> prev = table[i];
                MyWeakHashMap.Entry<K,V> p = prev;
                while (p != null) {
                    MyWeakHashMap.Entry<K,V> next = p.next;
                    if (p == e) {
                        if (prev == e) {
                            table[i] = next;
                        } else {
                            prev.next = next;
                        }


                        e.value = null;
                        size--;
                        break;
                    }
                    prev = p;
                    p = next;
                }
            }
        }
    }


    private MyWeakHashMap.Entry<K,V>[] getTable() {
        expungeStaleEntries();
        return table;
    }


    @Override
    public int size() {
        if (size == 0) {
            return 0;
        }
        expungeStaleEntries();
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public V get(Object key) {
        Object k = maskNull(key);
        int h = hash(k);
        MyWeakHashMap.Entry<K,V>[] tab = getTable();
        int index = indexFor(h, tab.length);
        MyWeakHashMap.Entry<K,V> e = tab[index];
        while (e != null) {
            if (e.hash == h && eq(k, e.get())) {
                return e.value;
            }
            e = e.next;
        }
        return null;
    }

    @Override
    public boolean containsKey(Object key) {
        return getEntry(key) != null;
    }


    MyWeakHashMap.Entry<K,V> getEntry(Object key) {
        Object k = maskNull(key);
        int h = hash(k);
        MyWeakHashMap.Entry<K,V>[] tab = getTable();
        int index = indexFor(h, tab.length);
        MyWeakHashMap.Entry<K,V> e = tab[index];
        while (e != null && !(e.hash == h && eq(k, e.get()))) {
            e = e.next;
        }
        return e;
    }

    @Override
    public V put(K key, V value) {
        Object k = maskNull(key);
        int h = hash(k);
        MyWeakHashMap.Entry<K,V>[] tab = getTable();
        int i = indexFor(h, tab.length);

        for (MyWeakHashMap.Entry<K,V> e = tab[i]; e != null; e = e.next) {
            if (h == e.hash && eq(k, e.get())) {
                V oldValue = e.value;
                if (value != oldValue) {
                    e.value = value;
                }
                return oldValue;
            }
        }

        modCount++;
        MyWeakHashMap.Entry<K,V> e = tab[i];
        tab[i] = new MyWeakHashMap.Entry<>(k, value, queue, h, e);
        if (++size >= threshold) {
            resize(tab.length * 2);
        }
        return null;
    }


    void resize(int newCapacity) {
        MyWeakHashMap.Entry<K,V>[] oldTable = getTable();
        int oldCapacity = oldTable.length;
        if (oldCapacity == MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }

        MyWeakHashMap.Entry<K,V>[] newTable = newTable(newCapacity);
        transfer(oldTable, newTable);
        table = newTable;


        if (size >= threshold / 2) {
            threshold = (int)(newCapacity * loadFactor);
        } else {
            expungeStaleEntries();
            transfer(newTable, oldTable);
            table = oldTable;
        }
    }


    private void transfer(MyWeakHashMap.Entry<K,V>[] src, MyWeakHashMap.Entry<K,V>[] dest) {
        for (int j = 0; j < src.length; ++j) {
            MyWeakHashMap.Entry<K,V> e = src[j];
            src[j] = null;
            while (e != null) {
                MyWeakHashMap.Entry<K,V> next = e.next;
                Object key = e.get();
                if (key == null) {
                    e.next = null;
                    e.value = null;
                    size--;
                } else {
                    int i = indexFor(e.hash, dest.length);
                    e.next = dest[i];
                    dest[i] = e;
                }
                e = next;
            }
        }
    }


    @Override
    public void putAll(MyMap<? extends K, ? extends V> m) {
        int numKeysToBeAdded = m.size();
        if (numKeysToBeAdded == 0) {
            return;
        }


        if (numKeysToBeAdded > threshold) {
            int targetCapacity = (int)(numKeysToBeAdded / loadFactor + 1);
            if (targetCapacity > MAXIMUM_CAPACITY) {
                targetCapacity = MAXIMUM_CAPACITY;
            }
            int newCapacity = table.length;
            while (newCapacity < targetCapacity) {
                newCapacity <<= 1;
            }
            if (newCapacity > table.length) {
                resize(newCapacity);
            }
        }

        for (MyMap.Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public V remove(Object key) {
        Object k = maskNull(key);
        int h = hash(k);
        MyWeakHashMap.Entry<K,V>[] tab = getTable();
        int i = indexFor(h, tab.length);
        MyWeakHashMap.Entry<K,V> prev = tab[i];
        MyWeakHashMap.Entry<K,V> e = prev;

        while (e != null) {
            MyWeakHashMap.Entry<K,V> next = e.next;
            if (h == e.hash && eq(k, e.get())) {
                modCount++;
                size--;
                if (prev == e) {
                    tab[i] = next;
                } else {
                    prev.next = next;
                }
                return e.value;
            }
            prev = e;
            e = next;
        }

        return null;
    }


    boolean removeMapping(Object o) {
        if (!(o instanceof MyMap.Entry)) {
            return false;
        }
        MyWeakHashMap.Entry<K,V>[] tab = getTable();
        MyMap.Entry<?,?> entry = (MyMap.Entry<?,?>)o;
        Object k = maskNull(entry.getKey());
        int h = hash(k);
        int i = indexFor(h, tab.length);
        MyWeakHashMap.Entry<K,V> prev = tab[i];
        MyWeakHashMap.Entry<K,V> e = prev;

        while (e != null) {
            MyWeakHashMap.Entry<K,V> next = e.next;
            if (h == e.hash && e.equals(entry)) {
                modCount++;
                size--;
                if (prev == e) {
                    tab[i] = next;
                } else {
                    prev.next = next;
                }
                return true;
            }
            prev = e;
            e = next;
        }

        return false;
    }

    @Override
    public void clear() {


        while (queue.poll() != null) {
            ;
        }

        modCount++;
        Arrays.fill(table, null);
        size = 0;




        while (queue.poll() != null) {
            ;
        }
    }

    @Override
    public boolean containsValue(Object value) {
        if (value==null) {
            return containsNullValue();
        }

        MyWeakHashMap.Entry<K,V>[] tab = getTable();
        for (int i = tab.length; i-- > 0;) {
            for (Entry<K,V> e = tab[i]; e != null; e = e.next) {
                if (value.equals(e.value)) {
                    return true;
                }
            }
        }
        return false;
    }


    private boolean containsNullValue() {
        MyWeakHashMap.Entry<K,V>[] tab = getTable();
        for (int i = tab.length; i-- > 0;) {
            for (Entry<K,V> e = tab[i]; e != null; e = e.next) {
                if (e.value==null) {
                    return true;
                }
            }
        }
        return false;
    }


    private static class Entry<K,V> extends WeakReference<Object> implements MyMap.Entry<K,V> {
        V value;
        final int hash;
        MyWeakHashMap.Entry<K,V> next;


        Entry(Object key, V value,
              ReferenceQueue<Object> queue,
              int hash, MyWeakHashMap.Entry<K,V> next) {
            super(key, queue);
            this.value = value;
            this.hash  = hash;
            this.next  = next;
        }

        @SuppressWarnings("unchecked")@Override
        public K getKey() {
            return (K) MyWeakHashMap.unmaskNull(get());
        }
        @Override
        public V getValue() {
            return value;
        }
        @Override
        public V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof MyMap.Entry)) {
                return false;
            }
            MyMap.Entry<?,?> e = (MyMap.Entry<?,?>)o;
            K k1 = getKey();
            Object k2 = e.getKey();
            if (k1 == k2 || (k1 != null && k1.equals(k2))) {
                V v1 = getValue();
                Object v2 = e.getValue();
                if (v1 == v2 || (v1 != null && v1.equals(v2)))
                    return true;
            }
            return false;
        }
        @Override
        public int hashCode() {
            K k = getKey();
            V v = getValue();
            return Objects.hashCode(k) ^ Objects.hashCode(v);
        }
        @Override
        public String toString() {
            return getKey() + "=" + getValue();
        }
    }

    private abstract class HashIterator<T> implements Iterator<T> {
        private int index;
        private MyWeakHashMap.Entry<K,V> entry;
        private MyWeakHashMap.Entry<K,V> lastReturned;
        private int expectedModCount = modCount;


        private Object nextKey;


        private Object currentKey;

        HashIterator() {
            index = isEmpty() ? 0 : table.length;
        }
        @Override
        public boolean hasNext() {
            MyWeakHashMap.Entry<K,V>[] t = table;

            while (nextKey == null) {
                MyWeakHashMap.Entry<K,V> e = entry;
                int i = index;
                while (e == null && i > 0) {
                    e = t[--i];
                }
                entry = e;
                index = i;
                if (e == null) {
                    currentKey = null;
                    return false;
                }
                nextKey = e.get();
                if (nextKey == null) {
                    entry = entry.next;
                }
            }
            return true;
        }


        protected MyWeakHashMap.Entry<K,V> nextEntry() {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            if (nextKey == null && !hasNext()) {
                throw new NoSuchElementException();
            }

            lastReturned = entry;
            entry = entry.next;
            currentKey = nextKey;
            nextKey = null;
            return lastReturned;
        }
        @Override
        public void remove() {
            if (lastReturned == null) {
                throw new IllegalStateException();
            }
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }

            MyWeakHashMap.this.remove(currentKey);
            expectedModCount = modCount;
            lastReturned = null;
            currentKey = null;
        }

    }

    private class ValueIterator extends HashIterator<V> {
        @Override
        public V next() {
            return nextEntry().value;
        }
    }

    private class KeyIterator extends HashIterator<K> {
        @Override
        public K next() {
            return nextEntry().getKey();
        }
    }

    private class EntryIterator extends HashIterator<MyMap.Entry<K,V>> {
        @Override
        public MyMap.Entry<K,V> next() {
            return nextEntry();
        }
    }



    private transient MySet<MyMap.Entry<K,V>> entrySet;
    transient MySet<K>        keySet;
    transient MyCollection<V> values;

    @Override
    public MySet<K> keySet() {
        MySet<K> ks = keySet;
        if (ks == null) {
            ks = new MyWeakHashMap.KeySet();
            keySet = ks;
        }
        return ks;
    }

    private class KeySet extends MyAbstractSet<K> {
        @Override
        public Iterator<K> iterator() {
            return new MyWeakHashMap.KeyIterator();
        }
        @Override
        public int size() {
            return MyWeakHashMap.this.size();
        }
        @Override
        public boolean contains(Object o) {
            return containsKey(o);
        }
        @Override
        public boolean remove(Object o) {
            if (containsKey(o)) {
                MyWeakHashMap.this.remove(o);
                return true;
            }
            else {
                return false;
            }
        }
        @Override
        public void clear() {
            MyWeakHashMap.this.clear();
        }
        @Override
        public Spliterator<K> spliterator() {
            return new MyWeakHashMap.KeySpliterator<>(MyWeakHashMap.this, 0, -1, 0, 0);
        }
    }

    @Override
    public MyCollection<V> values() {
        MyCollection<V> vs = values;
        if (vs == null) {
            vs = new MyWeakHashMap.Values();
            values = vs;
        }
        return vs;
    }

    private class Values extends MyAbstractCollection<V> {
        @Override
        public Iterator<V> iterator() {
            return new MyWeakHashMap.ValueIterator();
        }
        @Override
        public int size() {
            return MyWeakHashMap.this.size();
        }
        @Override
        public boolean contains(Object o) {
            return containsValue(o);
        }
        @Override
        public void clear() {
            MyWeakHashMap.this.clear();
        }
        @Override
        public Spliterator<V> spliterator() {
            return new MyWeakHashMap.ValueSpliterator<>(MyWeakHashMap.this, 0, -1, 0, 0);
        }
    }


    @Override
    public MySet<MyMap.Entry<K,V>> entrySet() {
        MySet<MyMap.Entry<K,V>> es = entrySet;
        return es != null ? es : (entrySet = new MyWeakHashMap.EntrySet());
    }

    private class EntrySet extends MyAbstractSet<MyMap.Entry<K,V>> {
        @Override
        public Iterator<MyMap.Entry<K, V>> iterator() {
            return new MyWeakHashMap.EntryIterator();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof MyMap.Entry)) {
                return false;
            }
            MyMap.Entry<?,?> e = (MyMap.Entry<?,?>)o;
            MyWeakHashMap.Entry<K,V> candidate = getEntry(e.getKey());
            return candidate != null && candidate.equals(e);
        }
        @Override
        public boolean remove(Object o) {
            return removeMapping(o);
        }
        @Override
        public int size() {
            return MyWeakHashMap.this.size();
        }
        @Override
        public void clear() {
            MyWeakHashMap.this.clear();
        }

        private MyList<MyMap.Entry<K,V>> deepCopy() {
            MyList<MyMap.Entry<K,V>> list = new MyArrayList<>(size());
            for (MyMap.Entry<K,V> e : this) {
                list.add(new SimpleEntry<>(e));
            }
            return list;
        }
        @Override
        public Object[] toArray() {
            return deepCopy().toArray();
        }
        @Override
        public <T> T[] toArray(T[] a) {
            return deepCopy().toArray(a);
        }

        @Override
        public Spliterator<MyMap.Entry<K, V>> spliterator() {
            return new  EntrySpliterator<>(MyWeakHashMap.this, 0, -1, 0, 0);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        int expectedModCount = modCount;

        MyWeakHashMap.Entry<K, V>[] tab = getTable();
        for (MyWeakHashMap.Entry<K, V> entry : tab) {
            while (entry != null) {
                Object key = entry.get();
                if (key != null) {
                    action.accept((K) MyWeakHashMap.unmaskNull(key), entry.value);
                }
                entry = entry.next;

                if (expectedModCount != modCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        int expectedModCount = modCount;

        MyWeakHashMap.Entry<K, V>[] tab = getTable();;
        for (MyWeakHashMap.Entry<K, V> entry : tab) {
            while (entry != null) {
                Object key = entry.get();
                if (key != null) {
                    entry.value = function.apply((K) MyWeakHashMap.unmaskNull(key), entry.value);
                }
                entry = entry.next;

                if (expectedModCount != modCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }


    static class MyWeakHashMapSpliterator<K,V> {
        final MyWeakHashMap<K,V> map;
        MyWeakHashMap.Entry<K,V> current;
        int index;
        int fence;
        int est;
        int expectedModCount;

        MyWeakHashMapSpliterator(MyWeakHashMap<K,V> m, int origin,
                               int fence, int est,
                               int expectedModCount) {
            this.map = m;
            this.index = origin;
            this.fence = fence;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        final int getFence() {
            int hi;
            if ((hi = fence) < 0) {
                MyWeakHashMap<K,V> m = map;
                est = m.size();
                expectedModCount = m.modCount;
                hi = fence = m.table.length;
            }
            return hi;
        }

        public final long estimateSize() {
            getFence();
            return (long) est;
        }
    }

    static final class KeySpliterator<K,V>
            extends MyWeakHashMap.MyWeakHashMapSpliterator<K,V>
            implements Spliterator<K> {
        KeySpliterator(MyWeakHashMap<K,V> m, int origin, int fence, int est,
                       int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }
        @Override
        public MyWeakHashMap.KeySpliterator<K,V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid) ? null :
                    new MyWeakHashMap.KeySpliterator<K,V>(map, lo, index = mid, est >>>= 1,
                            expectedModCount);
        }
        @Override
        public void forEachRemaining(Consumer<? super K> action) {
            int i, hi, mc;
            if (action == null) {
                throw new NullPointerException();
            }
            MyWeakHashMap<K,V> m = map;
            MyWeakHashMap.Entry<K,V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = tab.length;
            }
            else {
                mc = expectedModCount;
            }
            if (tab.length >= hi && (i = index) >= 0 &&
                    (i < (index = hi) || current != null)) {
                MyWeakHashMap.Entry<K,V> p = current;
                current = null;
                do {
                    if (p == null) {
                        p = tab[i++];
                    } else {
                        Object x = p.get();
                        p = p.next;
                        if (x != null) {
                            @SuppressWarnings("unchecked") K k =
                                    (K) MyWeakHashMap.unmaskNull(x);
                            action.accept(k);
                        }
                    }
                } while (p != null || i < hi);
            }
            if (m.modCount != mc) {
                throw new ConcurrentModificationException();
            }
        }
        @Override
        public boolean tryAdvance(Consumer<? super K> action) {
            int hi;
            if (action == null) {
                throw new NullPointerException();
            }
            MyWeakHashMap.Entry<K,V>[] tab = map.table;
            if (tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null) {
                        current = tab[index++];
                    } else {
                        Object x = current.get();
                        current = current.next;
                        if (x != null) {
                            @SuppressWarnings("unchecked") K k =
                                    (K) MyWeakHashMap.unmaskNull(x);
                            action.accept(k);
                            if (map.modCount != expectedModCount) {
                                throw new ConcurrentModificationException();
                            }
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        @Override
        public int characteristics() {
            return Spliterator.DISTINCT;
        }
    }

    static final class ValueSpliterator<K,V>
            extends MyWeakHashMap.MyWeakHashMapSpliterator<K,V>
            implements Spliterator<V> {
        ValueSpliterator(MyWeakHashMap<K,V> m, int origin, int fence, int est,
                         int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }
        @Override
        public MyWeakHashMap.ValueSpliterator<K,V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid) ? null :
                    new MyWeakHashMap.ValueSpliterator<K,V>(map, lo, index = mid, est >>>= 1,
                            expectedModCount);
        }
        @Override
        public void forEachRemaining(Consumer<? super V> action) {
            int i, hi, mc;
            if (action == null) {
                throw new NullPointerException();
            }
            MyWeakHashMap<K,V> m = map;
            MyWeakHashMap.Entry<K,V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = tab.length;
            }
            else {
                mc = expectedModCount;
            }
            if (tab.length >= hi && (i = index) >= 0 &&
                    (i < (index = hi) || current != null)) {
                MyWeakHashMap.Entry<K,V> p = current;
                current = null;
                do {
                    if (p == null) {
                        p = tab[i++];
                    } else {
                        Object x = p.get();
                        V v = p.value;
                        p = p.next;
                        if (x != null) {
                            action.accept(v);
                        }
                    }
                } while (p != null || i < hi);
            }
            if (m.modCount != mc) {
                throw new ConcurrentModificationException();
            }
        }
        @Override
        public boolean tryAdvance(Consumer<? super V> action) {
            int hi;
            if (action == null) {
                throw new NullPointerException();
            }
            MyWeakHashMap.Entry<K,V>[] tab = map.table;
            if (tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null) {
                        current = tab[index++];
                    } else {
                        Object x = current.get();
                        V v = current.value;
                        current = current.next;
                        if (x != null) {
                            action.accept(v);
                            if (map.modCount != expectedModCount) {
                                throw new ConcurrentModificationException();
                            }
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        @Override
        public int characteristics() {
            return 0;
        }
    }

    static final class EntrySpliterator<K,V>
            extends MyWeakHashMapSpliterator<K,V>
            implements Spliterator<MyMap.Entry<K,V>> {
        EntrySpliterator(MyWeakHashMap<K,V> m, int origin, int fence, int est,
                         int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        @Override
        public EntrySpliterator<K,V> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid) ? null :
                    new MyWeakHashMap.EntrySpliterator<K,V>(map, lo, index = mid, est >>>= 1,
                            expectedModCount);
        }

        @Override
        public void forEachRemaining(Consumer<? super MyMap.Entry<K, V>> action) {
            int i, hi, mc;
            if (action == null) {
                throw new NullPointerException();
            }
            MyWeakHashMap<K,V> m = map;
            MyWeakHashMap.Entry<K,V>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = tab.length;
            }
            else {
                mc = expectedModCount;
            }
            if (tab.length >= hi && (i = index) >= 0 &&
                    (i < (index = hi) || current != null)) {
                MyWeakHashMap.Entry<K,V> p = current;
                current = null;
                do {
                    if (p == null) {
                        p = tab[i++];
                    } else {
                        Object x = p.get();
                        V v = p.value;
                        p = p.next;
                        if (x != null) {
                            @SuppressWarnings("unchecked") K k =
                                    (K) MyWeakHashMap.unmaskNull(x);
                            action.accept
                                    (new MyAbstractMap.SimpleImmutableEntry<K,V>(k, v));
                        }
                    }
                } while (p != null || i < hi);
            }
            if (m.modCount != mc) {
                throw new ConcurrentModificationException();
            }
        }
        @Override
        public boolean tryAdvance(Consumer<? super MyMap.Entry<K,V>> action) {
            int hi;
            if (action == null) {
                throw new NullPointerException();
            }
            MyWeakHashMap.Entry<K,V>[] tab = map.table;
            if (tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null) {
                        current = tab[index++];
                    } else {
                        Object x = current.get();
                        V v = current.value;
                        current = current.next;
                        if (x != null) {
                            @SuppressWarnings("unchecked") K k =
                                    (K) MyWeakHashMap.unmaskNull(x);
                            action.accept
                                    (new MyAbstractMap.SimpleImmutableEntry<K,V>(k, v));
                            if (map.modCount != expectedModCount) {
                                throw new ConcurrentModificationException();
                            }
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        @Override
        public int characteristics() {
            return Spliterator.DISTINCT;
        }
    }

}
