package example.container.collection;


import java.lang.reflect.Array;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import sun.misc.SharedSecrets;


public class MyIdentityHashMap<K, V>
        extends MyAbstractMap<K, V>
        implements MyMap<K, V>, java.io.Serializable, Cloneable {

    private static final int DEFAULT_CAPACITY = 32;


    private static final int MINIMUM_CAPACITY = 4;


    private static final int MAXIMUM_CAPACITY = 1 << 29;


    transient Object[] table;

    int size;


    transient int modCount;


    static final Object NULL_KEY = new Object();


    private static Object maskNull(Object key) {
        return (key == null ? NULL_KEY : key);
    }


    static final Object unmaskNull(Object key) {
        return (key == NULL_KEY ? null : key);
    }


    public MyIdentityHashMap() {
        init(DEFAULT_CAPACITY);
    }


    public MyIdentityHashMap(int expectedMaxSize) {
        if (expectedMaxSize < 0) {
            throw new IllegalArgumentException("expectedMaxSize is negative: "
                    + expectedMaxSize);
        }
        init(capacity(expectedMaxSize));
    }


    private static int capacity(int expectedMaxSize) {

        return
                (expectedMaxSize > MAXIMUM_CAPACITY / 3) ? MAXIMUM_CAPACITY :
                        (expectedMaxSize <= 2 * MINIMUM_CAPACITY / 3) ? MINIMUM_CAPACITY :
                                Integer.highestOneBit(expectedMaxSize + (expectedMaxSize << 1));
    }


    private void init(int initCapacity) {


        table = new Object[2 * initCapacity];
    }


    public MyIdentityHashMap(MyMap<? extends K, ? extends V> m) {

        this((int) ((1 + m.size()) * 1.1));
        putAll(m);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }


    private static int hash(Object x, int length) {
        int h = System.identityHashCode(x);

        return ((h << 1) - (h << 8)) & (length - 1);
    }


    private static int nextKeyIndex(int i, int len) {
        return (i + 2 < len ? i + 2 : 0);
    }


    @SuppressWarnings("unchecked")
    @Override
    public V get(Object key) {
        Object k = maskNull(key);
        Object[] tab = table;
        int len = tab.length;
        int i = hash(k, len);
        while (true) {
            Object item = tab[i];
            if (item == k) {
                return (V) tab[i + 1];
            }
            if (item == null) {
                return null;
            }
            i = nextKeyIndex(i, len);
        }
    }

    @Override
    public boolean containsKey(Object key) {
        Object k = maskNull(key);
        Object[] tab = table;
        int len = tab.length;
        int i = hash(k, len);
        while (true) {
            Object item = tab[i];
            if (item == k) {
                return true;
            }
            if (item == null) {
                return false;
            }
            i = nextKeyIndex(i, len);
        }
    }

    @Override
    public boolean containsValue(Object value) {
        Object[] tab = table;
        for (int i = 1; i < tab.length; i += 2) {
            if (tab[i] == value && tab[i - 1] != null) {
                return true;
            }
        }

        return false;
    }


    private boolean containsMapping(Object key, Object value) {
        Object k = maskNull(key);
        Object[] tab = table;
        int len = tab.length;
        int i = hash(k, len);
        while (true) {
            Object item = tab[i];
            if (item == k) {
                return tab[i + 1] == value;
            }
            if (item == null) {
                return false;
            }
            i = nextKeyIndex(i, len);
        }
    }


    @Override
    public V put(K key, V value) {
        final Object k = maskNull(key);

        retryAfterResize:
        for (; ; ) {
            final Object[] tab = table;
            final int len = tab.length;
            int i = hash(k, len);

            for (Object item; (item = tab[i]) != null;
                 i = nextKeyIndex(i, len)) {
                if (item == k) {
                    @SuppressWarnings("unchecked")
                    V oldValue = (V) tab[i + 1];
                    tab[i + 1] = value;
                    return oldValue;
                }
            }

            final int s = size + 1;


            if (s + (s << 1) > len && resize(len)) {
                continue retryAfterResize;
            }

            modCount++;
            tab[i] = k;
            tab[i + 1] = value;
            size = s;
            return null;
        }
    }


    private boolean resize(int newCapacity) {

        int newLength = newCapacity * 2;

        Object[] oldTable = table;
        int oldLength = oldTable.length;
        if (oldLength == 2 * MAXIMUM_CAPACITY) {
            if (size == MAXIMUM_CAPACITY - 1) {
                throw new IllegalStateException("Capacity exhausted.");
            }
            return false;
        }
        if (oldLength >= newLength) {
            return false;
        }

        Object[] newTable = new Object[newLength];

        for (int j = 0; j < oldLength; j += 2) {
            Object key = oldTable[j];
            if (key != null) {
                Object value = oldTable[j + 1];
                oldTable[j] = null;
                oldTable[j + 1] = null;
                int i = hash(key, newLength);
                while (newTable[i] != null) {
                    i = nextKeyIndex(i, newLength);
                }
                newTable[i] = key;
                newTable[i + 1] = value;
            }
        }
        table = newTable;
        return true;
    }

    @Override
    public void putAll(MyMap<? extends K, ? extends V> m) {
        int n = m.size();
        if (n == 0) {
            return;
        }
        if (n > size) {
            resize(capacity(n));
        }

        for (Entry<? extends K, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public V remove(Object key) {
        Object k = maskNull(key);
        Object[] tab = table;
        int len = tab.length;
        int i = hash(k, len);

        while (true) {
            Object item = tab[i];
            if (item == k) {
                modCount++;
                size--;
                @SuppressWarnings("unchecked")
                V oldValue = (V) tab[i + 1];
                tab[i + 1] = null;
                tab[i] = null;
                closeDeletion(i);
                return oldValue;
            }
            if (item == null) {
                return null;
            }
            i = nextKeyIndex(i, len);
        }
    }


    private boolean removeMapping(Object key, Object value) {
        Object k = maskNull(key);
        Object[] tab = table;
        int len = tab.length;
        int i = hash(k, len);

        while (true) {
            Object item = tab[i];
            if (item == k) {
                if (tab[i + 1] != value) {
                    return false;
                }
                modCount++;
                size--;
                tab[i] = null;
                tab[i + 1] = null;
                closeDeletion(i);
                return true;
            }
            if (item == null) {
                return false;
            }
            i = nextKeyIndex(i, len);
        }
    }


    private void closeDeletion(int d) {

        Object[] tab = table;
        int len = tab.length;


        Object item;
        for (int i = nextKeyIndex(d, len); (item = tab[i]) != null;
             i = nextKeyIndex(i, len)) {


            int r = hash(item, len);
            if ((i < r && (r <= d || d <= i)) || (r <= d && d <= i)) {
                tab[d] = item;
                tab[d + 1] = tab[i + 1];
                tab[i] = null;
                tab[i + 1] = null;
                d = i;
            }
        }
    }

    @Override
    public void clear() {
        modCount++;
        Object[] tab = table;
        for (int i = 0; i < tab.length; i++) {
            tab[i] = null;
        }
        size = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof MyIdentityHashMap) {
            MyIdentityHashMap<?, ?> m = (MyIdentityHashMap<?, ?>) o;
            if (m.size() != size) {
                return false;
            }

            Object[] tab = m.table;
            for (int i = 0; i < tab.length; i += 2) {
                Object k = tab[i];
                if (k != null && !containsMapping(k, tab[i + 1])) {
                    return false;
                }
            }
            return true;
        } else if (o instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) o;
            return entrySet().equals(m.entrySet());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = 0;
        Object[] tab = table;
        for (int i = 0; i < tab.length; i += 2) {
            Object key = tab[i];
            if (key != null) {
                Object k = unmaskNull(key);
                result += System.identityHashCode(k) ^
                        System.identityHashCode(tab[i + 1]);
            }
        }
        return result;
    }

    @Override
    public Object clone() {
        try {
            MyIdentityHashMap<?, ?> m = (MyIdentityHashMap<?, ?>) super.clone();
            m.entrySet = null;
            m.table = table.clone();
            return m;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    private abstract class MyIdentityHashMapIterator<T> implements Iterator<T> {
        int index = (size != 0 ? 0 : table.length);
        int expectedModCount = modCount;
        int lastReturnedIndex = -1;
        boolean indexValid;
        Object[] traversalTable = table;
        @Override
        public boolean hasNext() {
            Object[] tab = traversalTable;
            for (int i = index; i < tab.length; i += 2) {
                Object key = tab[i];
                if (key != null) {
                    index = i;
                    return indexValid = true;
                }
            }
            index = tab.length;
            return false;
        }

        protected int nextIndex() {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            if (!indexValid && !hasNext()) {
                throw new NoSuchElementException();
            }

            indexValid = false;
            lastReturnedIndex = index;
            index += 2;
            return lastReturnedIndex;
        }
        @Override
        public void remove() {
            if (lastReturnedIndex == -1) {
                throw new IllegalStateException();
            }
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }

            expectedModCount = ++modCount;
            int deletedSlot = lastReturnedIndex;
            lastReturnedIndex = -1;

            index = deletedSlot;
            indexValid = false;


            Object[] tab = traversalTable;
            int len = tab.length;

            int d = deletedSlot;
            Object key = tab[d];
            tab[d] = null;
            tab[d + 1] = null;


            if (tab != MyIdentityHashMap.this.table) {
                MyIdentityHashMap.this.remove(key);
                expectedModCount = modCount;
                return;
            }

            size--;

            Object item;
            for (int i = nextKeyIndex(d, len); (item = tab[i]) != null;
                 i = nextKeyIndex(i, len)) {
                int r = hash(item, len);

                if ((i < r && (r <= d || d <= i)) ||
                        (r <= d && d <= i)) {


                    if (i < deletedSlot && d >= deletedSlot &&
                            traversalTable == MyIdentityHashMap.this.table) {
                        int remaining = len - deletedSlot;
                        Object[] newTable = new Object[remaining];
                        System.arraycopy(tab, deletedSlot,
                                newTable, 0, remaining);
                        traversalTable = newTable;
                        index = 0;
                    }

                    tab[d] = item;
                    tab[d + 1] = tab[i + 1];
                    tab[i] = null;
                    tab[i + 1] = null;
                    d = i;
                }
            }
        }
    }

    private class KeyIterator extends MyIdentityHashMapIterator<K> {
        @SuppressWarnings("unchecked")
        @Override
        public K next() {
            return (K) unmaskNull(traversalTable[nextIndex()]);
        }
    }

    private class ValueIterator extends MyIdentityHashMapIterator<V> {
        @SuppressWarnings("unchecked")
        @Override
        public V next() {
            return (V) traversalTable[nextIndex() + 1];
        }
    }

    private class EntryIterator
            extends MyIdentityHashMapIterator<Entry<K, V>> {
        private MyIdentityHashMap.EntryIterator.Entry lastReturnedEntry;
        @Override
        public MyMap.Entry<K, V> next() {
            lastReturnedEntry = new MyIdentityHashMap.EntryIterator.Entry(nextIndex());
            return lastReturnedEntry;
        }
        @Override
        public void remove() {
            lastReturnedIndex =
                    ((null == lastReturnedEntry) ? -1 : lastReturnedEntry.index);
            super.remove();
            lastReturnedEntry.index = lastReturnedIndex;
            lastReturnedEntry = null;
        }

        private class Entry implements MyMap.Entry<K, V> {
            private int index;

            private Entry(int index) {
                this.index = index;
            }

            @Override
            @SuppressWarnings("unchecked")
            public K getKey() {
                checkIndexForEntryUse();
                return (K) unmaskNull(traversalTable[index]);
            }

            @Override
            @SuppressWarnings("unchecked")
            public V getValue() {
                checkIndexForEntryUse();
                return (V) traversalTable[index + 1];
            }

            @Override
            @SuppressWarnings("unchecked")
            public V setValue(V value) {
                checkIndexForEntryUse();
                V oldValue = (V) traversalTable[index + 1];
                traversalTable[index + 1] = value;

                if (traversalTable != MyIdentityHashMap.this.table) {
                    put((K) traversalTable[index], value);
                }
                return oldValue;
            }
            @Override
            public boolean equals(Object o) {
                if (index < 0) {
                    return super.equals(o);
                }

                if (!(o instanceof MyMap.Entry)) {
                    return false;
                }
                MyMap.Entry<?, ?> e = (MyMap.Entry<?, ?>) o;
                return (e.getKey() == unmaskNull(traversalTable[index]) &&
                        e.getValue() == traversalTable[index + 1]);
            }
            @Override
            public int hashCode() {
                if (lastReturnedIndex < 0) {
                    return super.hashCode();
                }

                return (System.identityHashCode(unmaskNull(traversalTable[index])) ^
                        System.identityHashCode(traversalTable[index + 1]));
            }
            @Override
            public String toString() {
                if (index < 0) {
                    return super.toString();
                }

                return (unmaskNull(traversalTable[index]) + "="
                        + traversalTable[index + 1]);
            }

            private void checkIndexForEntryUse() {
                if (index < 0) {
                    throw new IllegalStateException("Entry was removed");
                }
            }
        }
    }


    private transient MySet<MyMap.Entry<K,V>> entrySet;
    transient MySet<K>        keySet;
    transient MyCollection<V> values;
    @Override
    public MySet<K> keySet() {
        MySet<K> ks = keySet;
        if (ks == null) {
            ks = new MyIdentityHashMap.KeySet();
            keySet = ks;
        }
        return ks;
    }

    private class KeySet extends MyAbstractSet<K> {
        @Override
        public Iterator<K> iterator() {
            return new MyIdentityHashMap.KeyIterator();
        }
        @Override
        public int size() {
            return size;
        }
        @Override
        public boolean contains(Object o) {
            return containsKey(o);
        }
        @Override
        public boolean remove(Object o) {
            int oldSize = size;
            MyIdentityHashMap.this.remove(o);
            return size != oldSize;
        }
        @Override
        public boolean removeAll(MyCollection<?> c) {
            Objects.requireNonNull(c);
            boolean modified = false;
            for (Iterator<K> i = iterator(); i.hasNext(); ) {
                if (c.contains(i.next())) {
                    i.remove();
                    modified = true;
                }
            }
            return modified;
        }
        @Override
        public void clear() {
            MyIdentityHashMap.this.clear();
        }
        @Override
        public int hashCode() {
            int result = 0;
            for (K key : this) {
                result += System.identityHashCode(key);
            }
            return result;
        }
        @Override
        public Object[] toArray() {
            return toArray(new Object[0]);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T[] toArray(T[] a) {
            int expectedModCount = modCount;
            int size = size();
            if (a.length < size) {
                a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
            }
            Object[] tab = table;
            int ti = 0;
            for (int si = 0; si < tab.length; si += 2) {
                Object key;
                if ((key = tab[si]) != null) {

                    if (ti >= size) {
                        throw new ConcurrentModificationException();
                    }
                    a[ti++] = (T) unmaskNull(key);
                }
            }

            if (ti < size || expectedModCount != modCount) {
                throw new ConcurrentModificationException();
            }

            if (ti < a.length) {
                a[ti] = null;
            }
            return a;
        }
        @Override
        public Spliterator<K> spliterator() {
            return new MyIdentityHashMap.KeySpliterator<>(MyIdentityHashMap.this, 0, -1, 0, 0);
        }
    }


    @Override
    public MyCollection<V> values() {
        MyCollection<V> vs = values;
        if (vs == null) {
            vs = new MyIdentityHashMap.Values();
            values = vs;
        }
        return vs;
    }

    private class Values extends MyAbstractCollection<V> {
        @Override
        public Iterator<V> iterator() {
            return new MyIdentityHashMap.ValueIterator();
        }
        @Override
        public int size() {
            return size;
        }
        @Override
        public boolean contains(Object o) {
            return containsValue(o);
        }
        @Override
        public boolean remove(Object o) {
            for (Iterator<V> i = iterator(); i.hasNext(); ) {
                if (i.next() == o) {
                    i.remove();
                    return true;
                }
            }
            return false;
        }
        @Override
        public void clear() {
            MyIdentityHashMap.this.clear();
        }
        @Override
        public Object[] toArray() {
            return toArray(new Object[0]);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T[] toArray(T[] a) {
            int expectedModCount = modCount;
            int size = size();
            if (a.length < size) {
                a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
            }
            Object[] tab = table;
            int ti = 0;
            for (int si = 0; si < tab.length; si += 2) {
                if (tab[si] != null) {

                    if (ti >= size) {
                        throw new ConcurrentModificationException();
                    }
                    a[ti++] = (T) tab[si + 1];
                }
            }

            if (ti < size || expectedModCount != modCount) {
                throw new ConcurrentModificationException();
            }

            if (ti < a.length) {
                a[ti] = null;
            }
            return a;
        }
        @Override
        public Spliterator<V> spliterator() {
            return new MyIdentityHashMap.ValueSpliterator<>(MyIdentityHashMap.this, 0, -1, 0, 0);
        }
    }

    @Override
    public MySet<MyMap.Entry<K, V>> entrySet() {
        MySet<MyMap.Entry<K, V>> es = entrySet;
        if (es != null) {
            return es;
       } else {
            return entrySet = new EntrySet();
        }
    }

    private class EntrySet extends MyAbstractSet<MyMap.Entry<K,V>> {
        @Override
        public Iterator<MyMap.Entry<K, V>> iterator() {
            return new MyIdentityHashMap.EntryIterator();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof MyMap.Entry)) {
                return false;
            }
            MyMap.Entry<?, ?> entry = (MyMap.Entry<?, ?>) o;
            return containsMapping(entry.getKey(), entry.getValue());
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof MyMap.Entry)) {
                return false;
            }
            MyMap.Entry<?, ?> entry = (MyMap.Entry<?, ?>) o;
            return removeMapping(entry.getKey(), entry.getValue());
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public void clear() {
            MyIdentityHashMap.this.clear();
        }

        public boolean removeAll(Collection<?> c) {
            Objects.requireNonNull(c);
            boolean modified = false;
            for (Iterator<MyMap.Entry<K, V>> i = iterator(); i.hasNext(); ) {
                if (c.contains(i.next())) {
                    i.remove();
                    modified = true;
                }
            }
            return modified;
        }

        @Override
        public Object[] toArray() {
            return toArray(new Object[0]);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T[] toArray(T[] a) {
            int expectedModCount = modCount;
            int size = size();
            if (a.length < size) {
                a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
            }
            Object[] tab = table;
            int ti = 0;
            for (int si = 0; si < tab.length; si += 2) {
                Object key;
                if ((key = tab[si]) != null) {

                    if (ti >= size) {
                        throw new ConcurrentModificationException();
                    }
                    a[ti++] = (T) new AbstractMap.SimpleEntry<>(unmaskNull(key), tab[si + 1]);
                }
            }

            if (ti < size || expectedModCount != modCount) {
                throw new ConcurrentModificationException();
            }

            if (ti < a.length) {
                a[ti] = null;
            }
            return a;
        }
        @Override
        public Spliterator<MyMap.Entry<K, V>> spliterator() {
            return new EntrySpliterator<>(MyIdentityHashMap.this, 0, -1, 0, 0);
        }
    }


    private static final long serialVersionUID = 8188218128353913216L;


    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {

        s.defaultWriteObject();


        s.writeInt(size);


        Object[] tab = table;
        for (int i = 0; i < tab.length; i += 2) {
            Object key = tab[i];
            if (key != null) {
                s.writeObject(unmaskNull(key));
                s.writeObject(tab[i + 1]);
            }
        }
    }


    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {

        s.defaultReadObject();


        int size = s.readInt();
        if (size < 0) {
            throw new java.io.StreamCorruptedException
                    ("Illegal mappings count: " + size);
        }
        int cap = capacity(size);
        SharedSecrets.getJavaOISAccess().checkArray(s, Object[].class, cap);
        init(cap);


        for (int i = 0; i < size; i++) {
            @SuppressWarnings("unchecked")
            K key = (K) s.readObject();
            @SuppressWarnings("unchecked")
            V value = (V) s.readObject();
            putForCreate(key, value);
        }
    }


    private void putForCreate(K key, V value)
            throws java.io.StreamCorruptedException {
        Object k = maskNull(key);
        Object[] tab = table;
        int len = tab.length;
        int i = hash(k, len);

        Object item;
        while ((item = tab[i]) != null) {
            if (item == k) {
                throw new java.io.StreamCorruptedException();
            }
            i = nextKeyIndex(i, len);
        }
        tab[i] = k;
        tab[i + 1] = value;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        int expectedModCount = modCount;

        Object[] t = table;
        for (int index = 0; index < t.length; index += 2) {
            Object k = t[index];
            if (k != null) {
                action.accept((K) unmaskNull(k), (V) t[index + 1]);
            }

            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        int expectedModCount = modCount;

        Object[] t = table;
        for (int index = 0; index < t.length; index += 2) {
            Object k = t[index];
            if (k != null) {
                t[index + 1] = function.apply((K) unmaskNull(k), (V) t[index + 1]);
            }

            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
        }
    }


    static class MyIdentityHashMapSpliterator<K, V> {
        final MyIdentityHashMap<K, V> map;
        int index;
        int fence;
        int est;
        int expectedModCount;

        MyIdentityHashMapSpliterator(MyIdentityHashMap<K, V> map, int origin,
                                     int fence, int est, int expectedModCount) {
            this.map = map;
            this.index = origin;
            this.fence = fence;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        final int getFence() {
            int hi;
            if ((hi = fence) < 0) {
                est = map.size;
                expectedModCount = map.modCount;
                hi = fence = map.table.length;
            }
            return hi;
        }

        public final long estimateSize() {
            getFence();
            return (long) est;
        }
    }

    static final class KeySpliterator<K, V>
            extends MyIdentityHashMap.MyIdentityHashMapSpliterator<K, V>
            implements Spliterator<K> {
        KeySpliterator(MyIdentityHashMap<K, V> map, int origin, int fence, int est,
                       int expectedModCount) {
            super(map, origin, fence, est, expectedModCount);
        }

        @Override
        public MyIdentityHashMap.KeySpliterator<K, V> trySplit() {
            int hi = getFence(), lo = index, mid = ((lo + hi) >>> 1) & ~1;
            return (lo >= mid) ? null :
                    new MyIdentityHashMap.KeySpliterator<K, V>(map, lo, index = mid, est >>>= 1,
                            expectedModCount);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void forEachRemaining(Consumer<? super K> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            int i, hi, mc;
            Object key;
            MyIdentityHashMap<K, V> m;
            Object[] a;
            if ((m = map) != null && (a = m.table) != null &&
                    (i = index) >= 0 && (index = hi = getFence()) <= a.length) {
                for (; i < hi; i += 2) {
                    if ((key = a[i]) != null) {
                        action.accept((K) unmaskNull(key));
                    }
                }
                if (m.modCount == expectedModCount) {
                    return;
                }
            }
            throw new ConcurrentModificationException();
        }

        @SuppressWarnings("unchecked")
        @Override
        public boolean tryAdvance(Consumer<? super K> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            Object[] a = map.table;
            int hi = getFence();
            while (index < hi) {
                Object key = a[index];
                index += 2;
                if (key != null) {
                    action.accept((K) unmaskNull(key));
                    if (map.modCount != expectedModCount) {
                        throw new ConcurrentModificationException();
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
        public int characteristics() {
            return (fence < 0 || est == map.size ? SIZED : 0) | Spliterator.DISTINCT;
        }
    }

    static final class ValueSpliterator<K, V>
            extends MyIdentityHashMap.MyIdentityHashMapSpliterator<K, V>
            implements Spliterator<V> {
        ValueSpliterator(MyIdentityHashMap<K, V> m, int origin, int fence, int est,
                         int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        @Override
        public MyIdentityHashMap.ValueSpliterator<K, V> trySplit() {
            int hi = getFence(), lo = index, mid = ((lo + hi) >>> 1) & ~1;
            return (lo >= mid) ? null :
                    new MyIdentityHashMap.ValueSpliterator<K, V>(map, lo, index = mid, est >>>= 1,
                            expectedModCount);
        }

        @Override
        public void forEachRemaining(Consumer<? super V> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            int i, hi, mc;
            MyIdentityHashMap<K, V> m;
            Object[] a;
            if ((m = map) != null && (a = m.table) != null &&
                    (i = index) >= 0 && (index = hi = getFence()) <= a.length) {
                for (; i < hi; i += 2) {
                    if (a[i] != null) {
                        @SuppressWarnings("unchecked") V v = (V) a[i + 1];
                        action.accept(v);
                    }
                }
                if (m.modCount == expectedModCount) {
                    return;
                }
            }
            throw new ConcurrentModificationException();
        }

        @Override
        public boolean tryAdvance(Consumer<? super V> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            Object[] a = map.table;
            int hi = getFence();
            while (index < hi) {
                Object key = a[index];
                @SuppressWarnings("unchecked") V v = (V) a[index + 1];
                index += 2;
                if (key != null) {
                    action.accept(v);
                    if (map.modCount != expectedModCount) {
                        throw new ConcurrentModificationException();
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
        public int characteristics() {
            return (fence < 0 || est == map.size ? SIZED : 0);
        }

    }

    static final class EntrySpliterator<K, V>
            extends MyIdentityHashMap.MyIdentityHashMapSpliterator<K, V>
            implements Spliterator<MyMap.Entry<K, V>> {
        EntrySpliterator(MyIdentityHashMap<K, V> m, int origin, int fence, int est,
                         int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        @Override
        public MyIdentityHashMap.EntrySpliterator<K, V> trySplit() {
            int hi = getFence(), lo = index, mid = ((lo + hi) >>> 1) & ~1;
            return (lo >= mid) ? null :
                    new MyIdentityHashMap.EntrySpliterator<K, V>(map, lo, index = mid, est >>>= 1,
                            expectedModCount);
        }

        @Override
        public void forEachRemaining(Consumer<? super MyMap.Entry<K, V>> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            int i, hi, mc;
            MyIdentityHashMap<K, V> m;
            Object[] a;
            if ((m = map) != null && (a = m.table) != null &&
                    (i = index) >= 0 && (index = hi = getFence()) <= a.length) {
                for (; i < hi; i += 2) {
                    Object key = a[i];
                    if (key != null) {
                        @SuppressWarnings("unchecked") K k =
                                (K) unmaskNull(key);
                        @SuppressWarnings("unchecked") V v = (V) a[i + 1];
                        action.accept
                                (new MyAbstractMap.SimpleImmutableEntry<K, V>(k, v));

                    }
                }
                if (m.modCount == expectedModCount) {
                    return;
                }
            }
            throw new ConcurrentModificationException();
        }

        @Override
        public boolean tryAdvance(Consumer<? super MyMap.Entry<K, V>> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            Object[] a = map.table;
            int hi = getFence();
            while (index < hi) {
                Object key = a[index];
                @SuppressWarnings("unchecked") V v = (V) a[index + 1];
                index += 2;
                if (key != null) {
                    @SuppressWarnings("unchecked") K k =
                            (K) unmaskNull(key);
                    action.accept
                            (new MyAbstractMap.SimpleImmutableEntry<K, V>(k, v));
                    if (map.modCount != expectedModCount) {
                        throw new ConcurrentModificationException();
                    }
                    return true;
                }
            }
            return false;
        }

        @Override
        public int characteristics() {
            return (fence < 0 || est == map.size ? SIZED : 0) | Spliterator.DISTINCT;
        }
    }

}
