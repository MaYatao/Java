package example.container.collection.current;


import example.container.collection.*;

import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongBiFunction;
import java.util.function.ToLongFunction;


public class MyConcurrentHashMap<K, V> extends MyAbstractMap<K, V>
        implements MyConcurrentMap<K, V>, Serializable {
    private static final long serialVersionUID = 7249069246763182397L;


    private static final int MAXIMUM_CAPACITY = 1 << 30;


    private static final int DEFAULT_CAPACITY = 16;


    static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;


    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;


    private static final float LOAD_FACTOR = 0.75f;


    static final int TREEIFY_THRESHOLD = 8;


    static final int UNTREEIFY_THRESHOLD = 6;


    static final int MIN_TREEIFY_CAPACITY = 64;


    private static final int MIN_TRANSFER_STRIDE = 16;


    private static int RESIZE_STAMP_BITS = 16;


    private static final int MAX_RESIZERS = (1 << (32 - RESIZE_STAMP_BITS)) - 1;


    private static final int RESIZE_STAMP_SHIFT = 32 - RESIZE_STAMP_BITS;


    static final int MOVED = -1;
    static final int TREEBIN = -2;
    static final int RESERVED = -3;
    static final int HASH_BITS = 0x7fffffff;


    static final int NCPU = Runtime.getRuntime().availableProcessors();


    private static final ObjectStreamField[] serialPersistentFields = {
            new ObjectStreamField("segments", MyConcurrentHashMap.Segment[].class),
            new ObjectStreamField("segmentMask", Integer.TYPE),
            new ObjectStreamField("segmentShift", Integer.TYPE)
    };


    static class Node<K, V> implements MyMap.Entry<K, V> {
        final int hash;
        final K key;
        volatile V val;
        volatile MyConcurrentHashMap.Node<K, V> next;

        Node(int hash, K key, V val, MyConcurrentHashMap.Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.val = val;
            this.next = next;
        }

        @Override
        public final K getKey() {
            return key;
        }

        @Override
        public final V getValue() {
            return val;
        }

        @Override
        public final int hashCode() {
            return key.hashCode() ^ val.hashCode();
        }

        @Override
        public final String toString() {
            return key + "=" + val;
        }

        @Override
        public final V setValue(V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public final boolean equals(Object o) {
            Object k, v, u;
            MyMap.Entry<?, ?> e;
            return ((o instanceof MyMap.Entry) &&
                    (k = (e = (MyMap.Entry<?, ?>) o).getKey()) != null &&
                    (v = e.getValue()) != null &&
                    (k == key || k.equals(key)) &&
                    (v == (u = val) || v.equals(u)));
        }


        Node<K, V> find(int h, Object k) {
            Node<K, V> e = this;
            if (k != null) {
                do {
                    K ek;
                    if (e.hash == h &&
                            ((ek = e.key) == k || (ek != null && k.equals(ek)))) {
                        return e;
                    }
                } while ((e = e.next) != null);
            }
            return null;
        }
    }


    static final int spread(int h) {
        return (h ^ (h >>> 16)) & HASH_BITS;
    }


    private static final int tableSizeFor(int c) {
        int n = c - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }


    static Class<?> comparableClassFor(Object x) {
        if (x instanceof Comparable) {
            Class<?> c;
            Type[] ts, as;
            Type t;
            ParameterizedType p;
            if ((c = x.getClass()) == String.class) {
                return c;
            }
            if ((ts = c.getGenericInterfaces()) != null) {
                for (int i = 0; i < ts.length; ++i) {
                    if (((t = ts[i]) instanceof ParameterizedType) &&
                            ((p = (ParameterizedType) t).getRawType() ==
                                    Comparable.class) &&
                            (as = p.getActualTypeArguments()) != null &&
                            as.length == 1 && as[0] == c) {
                        return c;
                    }
                }
            }
        }
        return null;
    }


    @SuppressWarnings({"rawtypes", "unchecked"})
    static int compareComparables(Class<?> kc, Object k, Object x) {
        return (x == null || x.getClass() != kc ? 0 :
                ((Comparable) k).compareTo(x));
    }


    @SuppressWarnings("unchecked")
    static final <K, V> MyConcurrentHashMap.Node<K, V> tabAt(MyConcurrentHashMap.Node<K, V>[] tab, int i) {
        return (MyConcurrentHashMap.Node<K, V>) U.getObjectVolatile(tab, ((long) i << ASHIFT) + ABASE);
    }

    static final <K, V> boolean casTabAt(MyConcurrentHashMap.Node<K, V>[] tab, int i,
                                         MyConcurrentHashMap.Node<K, V> c, MyConcurrentHashMap.Node<K, V> v) {
        return U.compareAndSwapObject(tab, ((long) i << ASHIFT) + ABASE, c, v);
    }

    static final <K, V> void setTabAt(MyConcurrentHashMap.Node<K, V>[] tab, int i, MyConcurrentHashMap.Node<K, V> v) {
        U.putObjectVolatile(tab, ((long) i << ASHIFT) + ABASE, v);
    }


    transient volatile MyConcurrentHashMap.Node<K, V>[] table;


    private transient volatile MyConcurrentHashMap.Node<K, V>[] nextTable;


    private transient volatile long baseCount;


    private transient volatile int sizeCtl;


    private transient volatile int transferIndex;


    private transient volatile int cellsBusy;


    private transient volatile MyConcurrentHashMap.CounterCell[] counterCells;


    private transient MyConcurrentHashMap.KeySetView<K, V> keySet;
    private transient MyConcurrentHashMap.ValuesView<K, V> values;
    private transient MyConcurrentHashMap.EntrySetView<K, V> entrySet;


    public MyConcurrentHashMap() {
    }


    public MyConcurrentHashMap(int initialCapacity) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException();
        int cap = ((initialCapacity >= (MAXIMUM_CAPACITY >>> 1)) ?
                MAXIMUM_CAPACITY :
                tableSizeFor(initialCapacity + (initialCapacity >>> 1) + 1));
        this.sizeCtl = cap;
    }


    public MyConcurrentHashMap(Map<? extends K, ? extends V> m) {
        this.sizeCtl = DEFAULT_CAPACITY;
        putAll(m);
    }


    public MyConcurrentHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, 1);
    }


    public MyConcurrentHashMap(int initialCapacity,
                               float loadFactor, int concurrencyLevel) {
        if (!(loadFactor > 0.0f) || initialCapacity < 0 || concurrencyLevel <= 0)
            throw new IllegalArgumentException();
        if (initialCapacity < concurrencyLevel)
            initialCapacity = concurrencyLevel;
        long size = (long) (1.0 + (long) initialCapacity / loadFactor);
        int cap = (size >= (long) MAXIMUM_CAPACITY) ?
                MAXIMUM_CAPACITY : tableSizeFor((int) size);
        this.sizeCtl = cap;
    }


    public int size() {
        long n = sumCount();
        return ((n < 0L) ? 0 :
                (n > (long) Integer.MAX_VALUE) ? Integer.MAX_VALUE :
                        (int) n);
    }


    public boolean isEmpty() {
        return sumCount() <= 0L;
    }


    public V get(Object key) {
        MyConcurrentHashMap.Node<K, V>[] tab;
        MyConcurrentHashMap.Node<K, V> e, p;
        int n, eh;
        K ek;
        int h = spread(key.hashCode());
        if ((tab = table) != null && (n = tab.length) > 0 &&
                (e = tabAt(tab, (n - 1) & h)) != null) {
            if ((eh = e.hash) == h) {
                if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                    return e.val;
            } else if (eh < 0)
                return (p = e.find(h, key)) != null ? p.val : null;
            while ((e = e.next) != null) {
                if (e.hash == h &&
                        ((ek = e.key) == key || (ek != null && key.equals(ek))))
                    return e.val;
            }
        }
        return null;
    }


    public boolean containsKey(Object key) {
        return get(key) != null;
    }


    public boolean containsValue(Object value) {
        if (value == null)
            throw new NullPointerException();
        MyConcurrentHashMap.Node<K, V>[] t;
        if ((t = table) != null) {
            MyConcurrentHashMap.Traverser<K, V> it = new MyConcurrentHashMap.Traverser<K, V>(t, t.length, 0, t.length);
            for (MyConcurrentHashMap.Node<K, V> p; (p = it.advance()) != null; ) {
                V v;
                if ((v = p.val) == value || (v != null && value.equals(v)))
                    return true;
            }
        }
        return false;
    }


    public V put(K key, V value) {
        return putVal(key, value, false);
    }


    final V putVal(K key, V value, boolean onlyIfAbsent) {
        if (key == null || value == null) throw new NullPointerException();
        int hash = spread(key.hashCode());
        int binCount = 0;
        for (MyConcurrentHashMap.Node<K, V>[] tab = table; ; ) {
            MyConcurrentHashMap.Node<K, V> f;
            int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
                if (casTabAt(tab, i, null,
                        new MyConcurrentHashMap.Node<K, V>(hash, key, value, null)))
                    break;
            } else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                V oldVal = null;
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (MyConcurrentHashMap.Node<K, V> e = f; ; ++binCount) {
                                K ek;
                                if (e.hash == hash &&
                                        ((ek = e.key) == key ||
                                                (ek != null && key.equals(ek)))) {
                                    oldVal = e.val;
                                    if (!onlyIfAbsent)
                                        e.val = value;
                                    break;
                                }
                                MyConcurrentHashMap.Node<K, V> pred = e;
                                if ((e = e.next) == null) {
                                    pred.next = new MyConcurrentHashMap.Node<K, V>(hash, key,
                                            value, null);
                                    break;
                                }
                            }
                        } else if (f instanceof MyConcurrentHashMap.TreeBin) {
                            MyConcurrentHashMap.Node<K, V> p;
                            binCount = 2;
                            if ((p = ((MyConcurrentHashMap.TreeBin<K, V>) f).putTreeVal(hash, key,
                                    value)) != null) {
                                oldVal = p.val;
                                if (!onlyIfAbsent)
                                    p.val = value;
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD) {
                        treeifyBin(tab, i);
                    }
                    if (oldVal != null) {
                        return oldVal;
                    }
                    break;
                }
            }
        }
        addCount(1L, binCount);
        return null;
    }


    public void putAll(Map<? extends K, ? extends V> m) {
        tryPresize(m.size());
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
            putVal(e.getKey(), e.getValue(), false);
    }


    public V remove(Object key) {
        return replaceNode(key, null, null);
    }


    final V replaceNode(Object key, V value, Object cv) {
        int hash = spread(key.hashCode());
        for (MyConcurrentHashMap.Node<K, V>[] tab = table; ; ) {
            MyConcurrentHashMap.Node<K, V> f;
            int n, i, fh;
            if (tab == null || (n = tab.length) == 0 ||
                    (f = tabAt(tab, i = (n - 1) & hash)) == null)
                break;
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                V oldVal = null;
                boolean validated = false;
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            validated = true;
                            for (MyConcurrentHashMap.Node<K, V> e = f, pred = null; ; ) {
                                K ek;
                                if (e.hash == hash &&
                                        ((ek = e.key) == key ||
                                                (ek != null && key.equals(ek)))) {
                                    V ev = e.val;
                                    if (cv == null || cv == ev ||
                                            (ev != null && cv.equals(ev))) {
                                        oldVal = ev;
                                        if (value != null)
                                            e.val = value;
                                        else if (pred != null)
                                            pred.next = e.next;
                                        else
                                            setTabAt(tab, i, e.next);
                                    }
                                    break;
                                }
                                pred = e;
                                if ((e = e.next) == null)
                                    break;
                            }
                        } else if (f instanceof MyConcurrentHashMap.TreeBin) {
                            validated = true;
                            MyConcurrentHashMap.TreeBin<K, V> t = (MyConcurrentHashMap.TreeBin<K, V>) f;
                            MyConcurrentHashMap.TreeNode<K, V> r, p;
                            if ((r = t.root) != null &&
                                    (p = r.findTreeNode(hash, key, null)) != null) {
                                V pv = p.val;
                                if (cv == null || cv == pv ||
                                        (pv != null && cv.equals(pv))) {
                                    oldVal = pv;
                                    if (value != null)
                                        p.val = value;
                                    else if (t.removeTreeNode(p))
                                        setTabAt(tab, i, untreeify(t.first));
                                }
                            }
                        }
                    }
                }
                if (validated) {
                    if (oldVal != null) {
                        if (value == null)
                            addCount(-1L, -1);
                        return oldVal;
                    }
                    break;
                }
            }
        }
        return null;
    }


    public void clear() {
        long delta = 0L;
        int i = 0;
        MyConcurrentHashMap.Node<K, V>[] tab = table;
        while (tab != null && i < tab.length) {
            int fh;
            MyConcurrentHashMap.Node<K, V> f = tabAt(tab, i);
            if (f == null)
                ++i;
            else if ((fh = f.hash) == MOVED) {
                tab = helpTransfer(tab, f);
                i = 0;
            } else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        MyConcurrentHashMap.Node<K, V> p = (fh >= 0 ? f :
                                (f instanceof MyConcurrentHashMap.TreeBin) ?
                                        ((MyConcurrentHashMap.TreeBin<K, V>) f).first : null);
                        while (p != null) {
                            --delta;
                            p = p.next;
                        }
                        setTabAt(tab, i++, null);
                    }
                }
            }
        }
        if (delta != 0L)
            addCount(delta, -1);
    }


    public MyConcurrentHashMap.KeySetView<K, V> keySet() {
        MyConcurrentHashMap.KeySetView<K, V> ks;
        return (ks = keySet) != null ? ks : (keySet = new MyConcurrentHashMap.KeySetView<K, V>(this, null));
    }


    public MyCollection<V> values() {
        MyConcurrentHashMap.ValuesView<K, V> vs;
        return (vs = values) != null ? vs : (values = new MyConcurrentHashMap.ValuesView<K, V>(this));
    }


    public MySet<MyMap.Entry<K, V>> entrySet() {
        MyConcurrentHashMap.EntrySetView<K, V> es;
        return (es = entrySet) != null ? es : (entrySet = new MyConcurrentHashMap.EntrySetView<K, V>(this));
    }


    public int hashCode() {
        int h = 0;
        MyConcurrentHashMap.Node<K, V>[] t;
        if ((t = table) != null) {
            MyConcurrentHashMap.Traverser<K, V> it = new MyConcurrentHashMap.Traverser<K, V>(t, t.length, 0, t.length);
            for (MyConcurrentHashMap.Node<K, V> p; (p = it.advance()) != null; )
                h += p.key.hashCode() ^ p.val.hashCode();
        }
        return h;
    }


    public String toString() {
        MyConcurrentHashMap.Node<K, V>[] t;
        int f = (t = table) == null ? 0 : t.length;
        MyConcurrentHashMap.Traverser<K, V> it = new MyConcurrentHashMap.Traverser<K, V>(t, f, 0, f);
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        MyConcurrentHashMap.Node<K, V> p;
        if ((p = it.advance()) != null) {
            for (; ; ) {
                K k = p.key;
                V v = p.val;
                sb.append(k == this ? "(this Map)" : k);
                sb.append('=');
                sb.append(v == this ? "(this Map)" : v);
                if ((p = it.advance()) == null) {
                    break;
                }
                sb.append(',').append(' ');
            }
        }
        return sb.append('}').toString();
    }


    @Override
    public boolean equals(Object o) {
        if (o != this) {
            if (!(o instanceof MyMap))
                return false;
            MyMap<?, ?> m = (MyMap<?, ?>) o;
            MyConcurrentHashMap.Node<K, V>[] t;
            int f = (t = table) == null ? 0 : t.length;
            MyConcurrentHashMap.Traverser<K, V> it = new MyConcurrentHashMap.Traverser<K, V>(t, f, 0, f);
            for (MyConcurrentHashMap.Node<K, V> p; (p = it.advance()) != null; ) {
                V val = p.val;
                Object v = m.get(p.key);
                if (v == null || (v != val && !v.equals(val))) {
                    return false;
                }
            }
            for (MyMap.Entry<?, ?> e : m.entrySet()) {
                Object mk, mv, v;
                if ((mk = e.getKey()) == null ||
                        (mv = e.getValue()) == null ||
                        (v = get(mk)) == null ||
                        (mv != v && !mv.equals(v))) {
                    return false;
                }
            }
        }
        return true;
    }


    static class Segment<K, V> extends ReentrantLock implements Serializable {
        private static final long serialVersionUID = 2249069246763182397L;
        final float loadFactor;

        Segment(float lf) {
            this.loadFactor = lf;
        }
    }


    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {


        int sshift = 0;
        int ssize = 1;
        while (ssize < DEFAULT_CONCURRENCY_LEVEL) {
            ++sshift;
            ssize <<= 1;
        }
        int segmentShift = 32 - sshift;
        int segmentMask = ssize - 1;
        @SuppressWarnings("unchecked")
        MyConcurrentHashMap.Segment<K, V>[] segments = (MyConcurrentHashMap.Segment<K, V>[])
                new MyConcurrentHashMap.Segment<?, ?>[DEFAULT_CONCURRENCY_LEVEL];
        for (int i = 0; i < segments.length; ++i) {
            segments[i] = new Segment<K, V>(LOAD_FACTOR);
        }
        s.putFields().put("segments", segments);
        s.putFields().put("segmentShift", segmentShift);
        s.putFields().put("segmentMask", segmentMask);
        s.writeFields();

        MyConcurrentHashMap.Node<K, V>[] t;
        if ((t = table) != null) {
            MyConcurrentHashMap.Traverser<K, V> it = new MyConcurrentHashMap.Traverser<K, V>(t, t.length, 0, t.length);
            for (MyConcurrentHashMap.Node<K, V> p; (p = it.advance()) != null; ) {
                s.writeObject(p.key);
                s.writeObject(p.val);
            }
        }
        s.writeObject(null);
        s.writeObject(null);
        segments = null;
    }


    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {

        sizeCtl = -1;
        s.defaultReadObject();
        long size = 0L;
        MyConcurrentHashMap.Node<K, V> p = null;
        for (; ; ) {
            @SuppressWarnings("unchecked")
            K k = (K) s.readObject();
            @SuppressWarnings("unchecked")
            V v = (V) s.readObject();
            if (k != null && v != null) {
                p = new MyConcurrentHashMap.Node<K, V>(spread(k.hashCode()), k, v, p);
                ++size;
            } else
                break;
        }
        if (size == 0L)
            sizeCtl = 0;
        else {
            int n;
            if (size >= (long) (MAXIMUM_CAPACITY >>> 1))
                n = MAXIMUM_CAPACITY;
            else {
                int sz = (int) size;
                n = tableSizeFor(sz + (sz >>> 1) + 1);
            }
            @SuppressWarnings("unchecked")
            MyConcurrentHashMap.Node<K, V>[] tab = (MyConcurrentHashMap.Node<K, V>[]) new MyConcurrentHashMap.Node<?, ?>[n];
            int mask = n - 1;
            long added = 0L;
            while (p != null) {
                boolean insertAtFront;
                MyConcurrentHashMap.Node<K, V> next = p.next, first;
                int h = p.hash, j = h & mask;
                if ((first = tabAt(tab, j)) == null) {
                    insertAtFront = true;
                } else {
                    K k = p.key;
                    if (first.hash < 0) {
                        MyConcurrentHashMap.TreeBin<K, V> t = (MyConcurrentHashMap.TreeBin<K, V>) first;
                        if (t.putTreeVal(h, k, p.val) == null)
                            ++added;
                        insertAtFront = false;
                    } else {
                        int binCount = 0;
                        insertAtFront = true;
                        MyConcurrentHashMap.Node<K, V> q;
                        K qk;
                        for (q = first; q != null; q = q.next) {
                            if (q.hash == h &&
                                    ((qk = q.key) == k ||
                                            (qk != null && k.equals(qk)))) {
                                insertAtFront = false;
                                break;
                            }
                            ++binCount;
                        }
                        if (insertAtFront && binCount >= TREEIFY_THRESHOLD) {
                            insertAtFront = false;
                            ++added;
                            p.next = first;
                            MyConcurrentHashMap.TreeNode<K, V> hd = null, tl = null;
                            for (q = p; q != null; q = q.next) {
                                MyConcurrentHashMap.TreeNode<K, V> t = new MyConcurrentHashMap.TreeNode<K, V>
                                        (q.hash, q.key, q.val, null, null);
                                if ((t.prev = tl) == null)
                                    hd = t;
                                else
                                    tl.next = t;
                                tl = t;
                            }
                            setTabAt(tab, j, new MyConcurrentHashMap.TreeBin<K, V>(hd));
                        }
                    }
                }
                if (insertAtFront) {
                    ++added;
                    p.next = first;
                    setTabAt(tab, j, p);
                }
                p = next;
            }
            table = tab;
            sizeCtl = n - (n >>> 2);
            baseCount = added;
        }
    }


    public V putIfAbsent(K key, V value) {
        return putVal(key, value, true);
    }


    public boolean remove(Object key, Object value) {
        if (key == null)
            throw new NullPointerException();
        return value != null && replaceNode(key, null, value) != null;
    }


    public boolean replace(K key, V oldValue, V newValue) {
        if (key == null || oldValue == null || newValue == null)
            throw new NullPointerException();
        return replaceNode(key, newValue, oldValue) != null;
    }


    public V replace(K key, V value) {
        if (key == null || value == null)
            throw new NullPointerException();
        return replaceNode(key, value, null);
    }


    public V getOrDefault(Object key, V defaultValue) {
        V v;
        return (v = get(key)) == null ? defaultValue : v;
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        if (action == null) throw new NullPointerException();
        MyConcurrentHashMap.Node<K, V>[] t;
        if ((t = table) != null) {
            MyConcurrentHashMap.Traverser<K, V> it = new MyConcurrentHashMap.Traverser<K, V>(t, t.length, 0, t.length);
            for (MyConcurrentHashMap.Node<K, V> p; (p = it.advance()) != null; ) {
                action.accept(p.key, p.val);
            }
        }
    }

    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        if (function == null) throw new NullPointerException();
        MyConcurrentHashMap.Node<K, V>[] t;
        if ((t = table) != null) {
            MyConcurrentHashMap.Traverser<K, V> it = new MyConcurrentHashMap.Traverser<K, V>(t, t.length, 0, t.length);
            for (MyConcurrentHashMap.Node<K, V> p; (p = it.advance()) != null; ) {
                V oldValue = p.val;
                for (K key = p.key; ; ) {
                    V newValue = function.apply(key, oldValue);
                    if (newValue == null)
                        throw new NullPointerException();
                    if (replaceNode(key, newValue, oldValue) != null ||
                            (oldValue = get(key)) == null)
                        break;
                }
            }
        }
    }


    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        if (key == null || mappingFunction == null)
            throw new NullPointerException();
        int h = spread(key.hashCode());
        V val = null;
        int binCount = 0;
        for (MyConcurrentHashMap.Node<K, V>[] tab = table; ; ) {
            MyConcurrentHashMap.Node<K, V> f;
            int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & h)) == null) {
                MyConcurrentHashMap.Node<K, V> r = new MyConcurrentHashMap.ReservationNode<K, V>();
                synchronized (r) {
                    if (casTabAt(tab, i, null, r)) {
                        binCount = 1;
                        MyConcurrentHashMap.Node<K, V> node = null;
                        try {
                            if ((val = mappingFunction.apply(key)) != null)
                                node = new MyConcurrentHashMap.Node<K, V>(h, key, val, null);
                        } finally {
                            setTabAt(tab, i, node);
                        }
                    }
                }
                if (binCount != 0)
                    break;
            } else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                boolean added = false;
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (MyConcurrentHashMap.Node<K, V> e = f; ; ++binCount) {
                                K ek;
                                V ev;
                                if (e.hash == h &&
                                        ((ek = e.key) == key ||
                                                (ek != null && key.equals(ek)))) {
                                    val = e.val;
                                    break;
                                }
                                MyConcurrentHashMap.Node<K, V> pred = e;
                                if ((e = e.next) == null) {
                                    if ((val = mappingFunction.apply(key)) != null) {
                                        added = true;
                                        pred.next = new MyConcurrentHashMap.Node<K, V>(h, key, val, null);
                                    }
                                    break;
                                }
                            }
                        } else if (f instanceof MyConcurrentHashMap.TreeBin) {
                            binCount = 2;
                            MyConcurrentHashMap.TreeBin<K, V> t = (MyConcurrentHashMap.TreeBin<K, V>) f;
                            MyConcurrentHashMap.TreeNode<K, V> r, p;
                            if ((r = t.root) != null &&
                                    (p = r.findTreeNode(h, key, null)) != null)
                                val = p.val;
                            else if ((val = mappingFunction.apply(key)) != null) {
                                added = true;
                                t.putTreeVal(h, key, val);
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    if (!added)
                        return val;
                    break;
                }
            }
        }
        if (val != null)
            addCount(1L, binCount);
        return val;
    }


    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null || remappingFunction == null)
            throw new NullPointerException();
        int h = spread(key.hashCode());
        V val = null;
        int delta = 0;
        int binCount = 0;
        for (MyConcurrentHashMap.Node<K, V>[] tab = table; ; ) {
            MyConcurrentHashMap.Node<K, V> f;
            int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & h)) == null)
                break;
            else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (MyConcurrentHashMap.Node<K, V> e = f, pred = null; ; ++binCount) {
                                K ek;
                                if (e.hash == h &&
                                        ((ek = e.key) == key ||
                                                (ek != null && key.equals(ek)))) {
                                    val = remappingFunction.apply(key, e.val);
                                    if (val != null)
                                        e.val = val;
                                    else {
                                        delta = -1;
                                        MyConcurrentHashMap.Node<K, V> en = e.next;
                                        if (pred != null)
                                            pred.next = en;
                                        else
                                            setTabAt(tab, i, en);
                                    }
                                    break;
                                }
                                pred = e;
                                if ((e = e.next) == null)
                                    break;
                            }
                        } else if (f instanceof MyConcurrentHashMap.TreeBin) {
                            binCount = 2;
                            MyConcurrentHashMap.TreeBin<K, V> t = (MyConcurrentHashMap.TreeBin<K, V>) f;
                            MyConcurrentHashMap.TreeNode<K, V> r, p;
                            if ((r = t.root) != null &&
                                    (p = r.findTreeNode(h, key, null)) != null) {
                                val = remappingFunction.apply(key, p.val);
                                if (val != null)
                                    p.val = val;
                                else {
                                    delta = -1;
                                    if (t.removeTreeNode(p))
                                        setTabAt(tab, i, untreeify(t.first));
                                }
                            }
                        }
                    }
                }
                if (binCount != 0)
                    break;
            }
        }
        if (delta != 0)
            addCount((long) delta, binCount);
        return val;
    }


    public V compute(K key,
                     BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null || remappingFunction == null)
            throw new NullPointerException();
        int h = spread(key.hashCode());
        V val = null;
        int delta = 0;
        int binCount = 0;
        for (MyConcurrentHashMap.Node<K, V>[] tab = table; ; ) {
            MyConcurrentHashMap.Node<K, V> f;
            int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & h)) == null) {
                MyConcurrentHashMap.Node<K, V> r = new MyConcurrentHashMap.ReservationNode<K, V>();
                synchronized (r) {
                    if (casTabAt(tab, i, null, r)) {
                        binCount = 1;
                        MyConcurrentHashMap.Node<K, V> node = null;
                        try {
                            if ((val = remappingFunction.apply(key, null)) != null) {
                                delta = 1;
                                node = new MyConcurrentHashMap.Node<K, V>(h, key, val, null);
                            }
                        } finally {
                            setTabAt(tab, i, node);
                        }
                    }
                }
                if (binCount != 0)
                    break;
            } else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (MyConcurrentHashMap.Node<K, V> e = f, pred = null; ; ++binCount) {
                                K ek;
                                if (e.hash == h &&
                                        ((ek = e.key) == key ||
                                                (ek != null && key.equals(ek)))) {
                                    val = remappingFunction.apply(key, e.val);
                                    if (val != null)
                                        e.val = val;
                                    else {
                                        delta = -1;
                                        MyConcurrentHashMap.Node<K, V> en = e.next;
                                        if (pred != null)
                                            pred.next = en;
                                        else
                                            setTabAt(tab, i, en);
                                    }
                                    break;
                                }
                                pred = e;
                                if ((e = e.next) == null) {
                                    val = remappingFunction.apply(key, null);
                                    if (val != null) {
                                        delta = 1;
                                        pred.next =
                                                new MyConcurrentHashMap.Node<K, V>(h, key, val, null);
                                    }
                                    break;
                                }
                            }
                        } else if (f instanceof MyConcurrentHashMap.TreeBin) {
                            binCount = 1;
                            MyConcurrentHashMap.TreeBin<K, V> t = (MyConcurrentHashMap.TreeBin<K, V>) f;
                            MyConcurrentHashMap.TreeNode<K, V> r, p;
                            if ((r = t.root) != null)
                                p = r.findTreeNode(h, key, null);
                            else
                                p = null;
                            V pv = (p == null) ? null : p.val;
                            val = remappingFunction.apply(key, pv);
                            if (val != null) {
                                if (p != null)
                                    p.val = val;
                                else {
                                    delta = 1;
                                    t.putTreeVal(h, key, val);
                                }
                            } else if (p != null) {
                                delta = -1;
                                if (t.removeTreeNode(p))
                                    setTabAt(tab, i, untreeify(t.first));
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    break;
                }
            }
        }
        if (delta != 0)
            addCount((long) delta, binCount);
        return val;
    }


    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if (key == null || value == null || remappingFunction == null)
            throw new NullPointerException();
        int h = spread(key.hashCode());
        V val = null;
        int delta = 0;
        int binCount = 0;
        for (MyConcurrentHashMap.Node<K, V>[] tab = table; ; ) {
            MyConcurrentHashMap.Node<K, V> f;
            int n, i, fh;
            if (tab == null || (n = tab.length) == 0)
                tab = initTable();
            else if ((f = tabAt(tab, i = (n - 1) & h)) == null) {
                if (casTabAt(tab, i, null, new MyConcurrentHashMap.Node<K, V>(h, key, value, null))) {
                    delta = 1;
                    val = value;
                    break;
                }
            } else if ((fh = f.hash) == MOVED)
                tab = helpTransfer(tab, f);
            else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        if (fh >= 0) {
                            binCount = 1;
                            for (MyConcurrentHashMap.Node<K, V> e = f, pred = null; ; ++binCount) {
                                K ek;
                                if (e.hash == h &&
                                        ((ek = e.key) == key ||
                                                (ek != null && key.equals(ek)))) {
                                    val = remappingFunction.apply(e.val, value);
                                    if (val != null)
                                        e.val = val;
                                    else {
                                        delta = -1;
                                        MyConcurrentHashMap.Node<K, V> en = e.next;
                                        if (pred != null)
                                            pred.next = en;
                                        else
                                            setTabAt(tab, i, en);
                                    }
                                    break;
                                }
                                pred = e;
                                if ((e = e.next) == null) {
                                    delta = 1;
                                    val = value;
                                    pred.next =
                                            new MyConcurrentHashMap.Node<K, V>(h, key, val, null);
                                    break;
                                }
                            }
                        } else if (f instanceof MyConcurrentHashMap.TreeBin) {
                            binCount = 2;
                            MyConcurrentHashMap.TreeBin<K, V> t = (MyConcurrentHashMap.TreeBin<K, V>) f;
                            MyConcurrentHashMap.TreeNode<K, V> r = t.root;
                            MyConcurrentHashMap.TreeNode<K, V> p = (r == null) ? null :
                                    r.findTreeNode(h, key, null);
                            val = (p == null) ? value :
                                    remappingFunction.apply(p.val, value);
                            if (val != null) {
                                if (p != null)
                                    p.val = val;
                                else {
                                    delta = 1;
                                    t.putTreeVal(h, key, val);
                                }
                            } else if (p != null) {
                                delta = -1;
                                if (t.removeTreeNode(p))
                                    setTabAt(tab, i, untreeify(t.first));
                            }
                        }
                    }
                }
                if (binCount != 0) {
                    if (binCount >= TREEIFY_THRESHOLD)
                        treeifyBin(tab, i);
                    break;
                }
            }
        }
        if (delta != 0)
            addCount((long) delta, binCount);
        return val;
    }


    public boolean contains(Object value) {
        return containsValue(value);
    }


    public Enumeration<K> keys() {
        MyConcurrentHashMap.Node<K, V>[] t;
        int f = (t = table) == null ? 0 : t.length;
        return new MyConcurrentHashMap.KeyIterator<K, V>(t, f, 0, f, this);
    }


    public Enumeration<V> elements() {
        MyConcurrentHashMap.Node<K, V>[] t;
        int f = (t = table) == null ? 0 : t.length;
        return new MyConcurrentHashMap.ValueIterator<K, V>(t, f, 0, f, this);
    }


    public long mappingCount() {
        long n = sumCount();
        return (n < 0L) ? 0L : n;
    }


    public static <K> MyConcurrentHashMap.KeySetView<K, Boolean> newKeySet() {
        return new MyConcurrentHashMap.KeySetView<K, Boolean>
                (new MyConcurrentHashMap<K, Boolean>(), Boolean.TRUE);
    }


    public static <K> MyConcurrentHashMap.KeySetView<K, Boolean> newKeySet(int initialCapacity) {
        return new MyConcurrentHashMap.KeySetView<K, Boolean>
                (new MyConcurrentHashMap<K, Boolean>(initialCapacity), Boolean.TRUE);
    }


    public MyConcurrentHashMap.KeySetView<K, V> keySet(V mappedValue) {
        if (mappedValue == null)
            throw new NullPointerException();
        return new MyConcurrentHashMap.KeySetView<K, V>(this, mappedValue);
    }


    static final class ForwardingNode<K, V> extends MyConcurrentHashMap.Node<K, V> {
        final MyConcurrentHashMap.Node<K, V>[] nextTable;

        ForwardingNode(MyConcurrentHashMap.Node<K, V>[] tab) {
            super(MOVED, null, null, null);
            this.nextTable = tab;
        }

        MyConcurrentHashMap.Node<K, V> find(int h, Object k) {

            outer:
            for (MyConcurrentHashMap.Node<K, V>[] tab = nextTable; ; ) {
                MyConcurrentHashMap.Node<K, V> e;
                int n;
                if (k == null || tab == null || (n = tab.length) == 0 ||
                        (e = tabAt(tab, (n - 1) & h)) == null)
                    return null;
                for (; ; ) {
                    int eh;
                    K ek;
                    if ((eh = e.hash) == h &&
                            ((ek = e.key) == k || (ek != null && k.equals(ek))))
                        return e;
                    if (eh < 0) {
                        if (e instanceof MyConcurrentHashMap.ForwardingNode) {
                            tab = ((MyConcurrentHashMap.ForwardingNode<K, V>) e).nextTable;
                            continue outer;
                        } else
                            return e.find(h, k);
                    }
                    if ((e = e.next) == null)
                        return null;
                }
            }
        }
    }


    static final class ReservationNode<K, V> extends MyConcurrentHashMap.Node<K, V> {
        ReservationNode() {
            super(RESERVED, null, null, null);
        }

        MyConcurrentHashMap.Node<K, V> find(int h, Object k) {
            return null;
        }
    }


    static final int resizeStamp(int n) {
        return Integer.numberOfLeadingZeros(n) | (1 << (RESIZE_STAMP_BITS - 1));
    }


    private final MyConcurrentHashMap.Node<K, V>[] initTable() {
        MyConcurrentHashMap.Node<K, V>[] tab;
        int sc;
        while ((tab = table) == null || tab.length == 0) {
            if ((sc = sizeCtl) < 0)
                Thread.yield();
            else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                try {
                    if ((tab = table) == null || tab.length == 0) {
                        int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                        @SuppressWarnings("unchecked")
                        MyConcurrentHashMap.Node<K, V>[] nt = (MyConcurrentHashMap.Node<K, V>[]) new MyConcurrentHashMap.Node<?, ?>[n];
                        table = tab = nt;
                        sc = n - (n >>> 2);
                    }
                } finally {
                    sizeCtl = sc;
                }
                break;
            }
        }
        return tab;
    }


    private final void addCount(long x, int check) {
        MyConcurrentHashMap.CounterCell[] as;
        long b, s;
        if ((as = counterCells) != null ||
                !U.compareAndSwapLong(this, BASECOUNT, b = baseCount, s = b + x)) {
            MyConcurrentHashMap.CounterCell a;
            long v;
            int m;
            boolean uncontended = true;
            if (as == null || (m = as.length - 1) < 0 ||
                    (a = as[MyThreadLocalRandom.getProbe() & m]) == null ||
                    !(uncontended =
                            U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))) {
                fullAddCount(x, uncontended);
                return;
            }
            if (check <= 1) {
                return;
            }
            s = sumCount();
        }
        if (check >= 0) {
            MyConcurrentHashMap.Node<K, V>[] tab, nt;
            int n, sc;
            while (s >= (long) (sc = sizeCtl) && (tab = table) != null &&
                    (n = tab.length) < MAXIMUM_CAPACITY) {
                int rs = resizeStamp(n);
                if (sc < 0) {
                    if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                            sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                            transferIndex <= 0) {
                        break;
                    }
                    if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {
                        transfer(tab, nt);
                    }
                } else if (U.compareAndSwapInt(this, SIZECTL, sc,
                        (rs << RESIZE_STAMP_SHIFT) + 2))
                    transfer(tab, null);
                s = sumCount();
            }
        }
    }


    final MyConcurrentHashMap.Node<K, V>[] helpTransfer(MyConcurrentHashMap.Node<K, V>[] tab, MyConcurrentHashMap.Node<K, V> f) {
        MyConcurrentHashMap.Node<K, V>[] nextTab;
        int sc;
        if (tab != null && (f instanceof MyConcurrentHashMap.ForwardingNode) &&
                (nextTab = ((MyConcurrentHashMap.ForwardingNode<K, V>) f).nextTable) != null) {
            int rs = resizeStamp(tab.length);
            while (nextTab == nextTable && table == tab &&
                    (sc = sizeCtl) < 0) {
                if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                        sc == rs + MAX_RESIZERS || transferIndex <= 0)
                    break;
                if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1)) {
                    transfer(tab, nextTab);
                    break;
                }
            }
            return nextTab;
        }
        return table;
    }


    private final void tryPresize(int size) {
        int c = (size >= (MAXIMUM_CAPACITY >>> 1)) ? MAXIMUM_CAPACITY :
                tableSizeFor(size + (size >>> 1) + 1);
        int sc;
        while ((sc = sizeCtl) >= 0) {
            MyConcurrentHashMap.Node<K, V>[] tab = table;
            int n;
            if (tab == null || (n = tab.length) == 0) {
                n = (sc > c) ? sc : c;
                if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                    try {
                        if (table == tab) {
                            @SuppressWarnings("unchecked")
                            MyConcurrentHashMap.Node<K, V>[] nt = (MyConcurrentHashMap.Node<K, V>[]) new MyConcurrentHashMap.Node<?, ?>[n];
                            table = nt;
                            sc = n - (n >>> 2);
                        }
                    } finally {
                        sizeCtl = sc;
                    }
                }
            } else if (c <= sc || n >= MAXIMUM_CAPACITY)
                break;
            else if (tab == table) {
                int rs = resizeStamp(n);
                if (sc < 0) {
                    MyConcurrentHashMap.Node<K, V>[] nt;
                    if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                            sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                            transferIndex <= 0)
                        break;
                    if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
                        transfer(tab, nt);
                } else if (U.compareAndSwapInt(this, SIZECTL, sc,
                        (rs << RESIZE_STAMP_SHIFT) + 2))
                    transfer(tab, null);
            }
        }
    }


    private final void transfer(MyConcurrentHashMap.Node<K, V>[] tab, MyConcurrentHashMap.Node<K, V>[] nextTab) {
        int n = tab.length, stride;
        if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE)
            stride = MIN_TRANSFER_STRIDE;
        if (nextTab == null) {
            try {
                @SuppressWarnings("unchecked")
                MyConcurrentHashMap.Node<K, V>[] nt = (MyConcurrentHashMap.Node<K, V>[]) new MyConcurrentHashMap.Node<?, ?>[n << 1];
                nextTab = nt;
            } catch (Throwable ex) {
                sizeCtl = Integer.MAX_VALUE;
                return;
            }
            nextTable = nextTab;
            transferIndex = n;
        }
        int nextn = nextTab.length;
        MyConcurrentHashMap.ForwardingNode<K, V> fwd = new MyConcurrentHashMap.ForwardingNode<K, V>(nextTab);
        boolean advance = true;
        boolean finishing = false;
        for (int i = 0, bound = 0; ; ) {
            MyConcurrentHashMap.Node<K, V> f;
            int fh;
            while (advance) {
                int nextIndex, nextBound;
                if (--i >= bound || finishing)
                    advance = false;
                else if ((nextIndex = transferIndex) <= 0) {
                    i = -1;
                    advance = false;
                } else if (U.compareAndSwapInt
                        (this, TRANSFERINDEX, nextIndex,
                                nextBound = (nextIndex > stride ?
                                        nextIndex - stride : 0))) {
                    bound = nextBound;
                    i = nextIndex - 1;
                    advance = false;
                }
            }
            if (i < 0 || i >= n || i + n >= nextn) {
                int sc;
                if (finishing) {
                    nextTable = null;
                    table = nextTab;
                    sizeCtl = (n << 1) - (n >>> 1);
                    return;
                }
                if (U.compareAndSwapInt(this, SIZECTL, sc = sizeCtl, sc - 1)) {
                    if ((sc - 2) != resizeStamp(n) << RESIZE_STAMP_SHIFT) {
                        return;
                    }
                    finishing = advance = true;
                    i = n;
                }
            } else if ((f = tabAt(tab, i)) == null) {
                advance = casTabAt(tab, i, null, fwd);
            } else if ((fh = f.hash) == MOVED) {
                advance = true;
            } else {
                synchronized (f) {
                    if (tabAt(tab, i) == f) {
                        MyConcurrentHashMap.Node<K, V> ln, hn;
                        if (fh >= 0) {
                            int runBit = fh & n;
                            MyConcurrentHashMap.Node<K, V> lastRun = f;
                            for (MyConcurrentHashMap.Node<K, V> p = f.next; p != null; p = p.next) {
                                int b = p.hash & n;
                                if (b != runBit) {
                                    runBit = b;
                                    lastRun = p;
                                }
                            }
                            if (runBit == 0) {
                                ln = lastRun;
                                hn = null;
                            } else {
                                hn = lastRun;
                                ln = null;
                            }
                            for (MyConcurrentHashMap.Node<K, V> p = f; p != lastRun; p = p.next) {
                                int ph = p.hash;
                                K pk = p.key;
                                V pv = p.val;
                                if ((ph & n) == 0)
                                    ln = new MyConcurrentHashMap.Node<K, V>(ph, pk, pv, ln);
                                else
                                    hn = new MyConcurrentHashMap.Node<K, V>(ph, pk, pv, hn);
                            }
                            setTabAt(nextTab, i, ln);
                            setTabAt(nextTab, i + n, hn);
                            setTabAt(tab, i, fwd);
                            advance = true;
                        } else if (f instanceof MyConcurrentHashMap.TreeBin) {
                            MyConcurrentHashMap.TreeBin<K, V> t = (MyConcurrentHashMap.TreeBin<K, V>) f;
                            MyConcurrentHashMap.TreeNode<K, V> lo = null, loTail = null;
                            MyConcurrentHashMap.TreeNode<K, V> hi = null, hiTail = null;
                            int lc = 0, hc = 0;
                            for (MyConcurrentHashMap.Node<K, V> e = t.first; e != null; e = e.next) {
                                int h = e.hash;
                                MyConcurrentHashMap.TreeNode<K, V> p = new MyConcurrentHashMap.TreeNode<K, V>
                                        (h, e.key, e.val, null, null);
                                if ((h & n) == 0) {
                                    if ((p.prev = loTail) == null)
                                        lo = p;
                                    else
                                        loTail.next = p;
                                    loTail = p;
                                    ++lc;
                                } else {
                                    if ((p.prev = hiTail) == null)
                                        hi = p;
                                    else
                                        hiTail.next = p;
                                    hiTail = p;
                                    ++hc;
                                }
                            }
                            ln = (lc <= UNTREEIFY_THRESHOLD) ? untreeify(lo) :
                                    (hc != 0) ? new MyConcurrentHashMap.TreeBin<K, V>(lo) : t;
                            hn = (hc <= UNTREEIFY_THRESHOLD) ? untreeify(hi) :
                                    (lc != 0) ? new MyConcurrentHashMap.TreeBin<K, V>(hi) : t;
                            setTabAt(nextTab, i, ln);
                            setTabAt(nextTab, i + n, hn);
                            setTabAt(tab, i, fwd);
                            advance = true;
                        }
                    }
                }
            }
        }
    }


    @sun.misc.Contended
    static final class CounterCell {
        volatile long value;

        CounterCell(long x) {
            value = x;
        }
    }

    final long sumCount() {
        MyConcurrentHashMap.CounterCell[] as = counterCells;
        MyConcurrentHashMap.CounterCell a;
        long sum = baseCount;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    sum += a.value;
            }
        }
        return sum;
    }


    private final void fullAddCount(long x, boolean wasUncontended) {
        int h;
        if ((h = MyThreadLocalRandom.getProbe()) == 0) {
            MyThreadLocalRandom.localInit();
            h = MyThreadLocalRandom.getProbe();
            wasUncontended = true;
        }
        boolean collide = false;
        for (; ; ) {
            MyConcurrentHashMap.CounterCell[] as;
            MyConcurrentHashMap.CounterCell a;
            int n;
            long v;
            if ((as = counterCells) != null && (n = as.length) > 0) {
                if ((a = as[(n - 1) & h]) == null) {
                    if (cellsBusy == 0) {
                        MyConcurrentHashMap.CounterCell r = new MyConcurrentHashMap.CounterCell(x);
                        if (cellsBusy == 0 &&
                                U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                            boolean created = false;
                            try {
                                MyConcurrentHashMap.CounterCell[] rs;
                                int m, j;
                                if ((rs = counterCells) != null &&
                                        (m = rs.length) > 0 &&
                                        rs[j = (m - 1) & h] == null) {
                                    rs[j] = r;
                                    created = true;
                                }
                            } finally {
                                cellsBusy = 0;
                            }
                            if (created) {
                                break;
                            }
                            continue;
                        }
                    }
                    collide = false;
                } else if (!wasUncontended) {
                    wasUncontended = true;
                } else if (U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x)) {
                    break;
                } else if (counterCells != as || n >= NCPU) {
                    collide = false;
                } else if (!collide) {
                    collide = true;
                } else if (cellsBusy == 0 &&
                        U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                    try {
                        if (counterCells == as) {
                            MyConcurrentHashMap.CounterCell[] rs = new MyConcurrentHashMap.CounterCell[n << 1];
                            for (int i = 0; i < n; ++i) {
                                rs[i] = as[i];
                            }
                            counterCells = rs;
                        }
                    } finally {
                        cellsBusy = 0;
                    }
                    collide = false;
                    continue;
                }
                h = MyThreadLocalRandom.advanceProbe(h);
            } else if (cellsBusy == 0 && counterCells == as &&
                    U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                boolean init = false;
                try {
                    if (counterCells == as) {
                        MyConcurrentHashMap.CounterCell[] rs = new MyConcurrentHashMap.CounterCell[2];
                        rs[h & 1] = new MyConcurrentHashMap.CounterCell(x);
                        counterCells = rs;
                        init = true;
                    }
                } finally {
                    cellsBusy = 0;
                }
                if (init)
                    break;
            } else if (U.compareAndSwapLong(this, BASECOUNT, v = baseCount, v + x))
                break;
        }
    }


    private final void treeifyBin(MyConcurrentHashMap.Node<K, V>[] tab, int index) {
        MyConcurrentHashMap.Node<K, V> b;
        int n, sc;
        if (tab != null) {
            if ((n = tab.length) < MIN_TREEIFY_CAPACITY)
                tryPresize(n << 1);
            else if ((b = tabAt(tab, index)) != null && b.hash >= 0) {
                synchronized (b) {
                    if (tabAt(tab, index) == b) {
                        MyConcurrentHashMap.TreeNode<K, V> hd = null, tl = null;
                        for (MyConcurrentHashMap.Node<K, V> e = b; e != null; e = e.next) {
                            MyConcurrentHashMap.TreeNode<K, V> p =
                                    new MyConcurrentHashMap.TreeNode<K, V>(e.hash, e.key, e.val,
                                            null, null);
                            if ((p.prev = tl) == null)
                                hd = p;
                            else
                                tl.next = p;
                            tl = p;
                        }
                        setTabAt(tab, index, new MyConcurrentHashMap.TreeBin<K, V>(hd));
                    }
                }
            }
        }
    }


    static <K, V> MyConcurrentHashMap.Node<K, V> untreeify(MyConcurrentHashMap.Node<K, V> b) {
        MyConcurrentHashMap.Node<K, V> hd = null, tl = null;
        for (MyConcurrentHashMap.Node<K, V> q = b; q != null; q = q.next) {
            MyConcurrentHashMap.Node<K, V> p = new MyConcurrentHashMap.Node<K, V>(q.hash, q.key, q.val, null);
            if (tl == null) {
                hd = p;
            } else {
                tl.next = p;
            }
            tl = p;
        }
        return hd;
    }


    static final class TreeNode<K, V> extends MyConcurrentHashMap.Node<K, V> {
        MyConcurrentHashMap.TreeNode<K, V> parent;
        MyConcurrentHashMap.TreeNode<K, V> left;
        MyConcurrentHashMap.TreeNode<K, V> right;
        MyConcurrentHashMap.TreeNode<K, V> prev;
        boolean red;

        TreeNode(int hash, K key, V val, MyConcurrentHashMap.Node<K, V> next,
                 MyConcurrentHashMap.TreeNode<K, V> parent) {
            super(hash, key, val, next);
            this.parent = parent;
        }

        @Override
        MyConcurrentHashMap.Node<K, V> find(int h, Object k) {
            return findTreeNode(h, k, null);
        }


        final MyConcurrentHashMap.TreeNode<K, V> findTreeNode(int h, Object k, Class<?> kc) {
            if (k != null) {
                MyConcurrentHashMap.TreeNode<K, V> p = this;
                do {
                    int ph, dir;
                    K pk;
                    MyConcurrentHashMap.TreeNode<K, V> q;
                    MyConcurrentHashMap.TreeNode<K, V> pl = p.left, pr = p.right;
                    if ((ph = p.hash) > h) {
                        p = pl;
                    } else if (ph < h) {
                        p = pr;
                    } else if ((pk = p.key) == k || (pk != null && k.equals(pk))) {
                        return p;
                    } else if (pl == null) {
                        p = pr;
                    } else if (pr == null) {
                        p = pl;
                    } else if ((kc != null ||
                            (kc = comparableClassFor(k)) != null) &&
                            (dir = compareComparables(kc, k, pk)) != 0) {
                        p = (dir < 0) ? pl : pr;
                    } else if ((q = pr.findTreeNode(h, k, kc)) != null) {
                        return q;
                    } else {
                        p = pl;
                    }
                } while (p != null);
            }
            return null;
        }
    }


    static final class TreeBin<K, V> extends MyConcurrentHashMap.Node<K, V> {
        MyConcurrentHashMap.TreeNode<K, V> root;
        volatile MyConcurrentHashMap.TreeNode<K, V> first;
        volatile Thread waiter;
        volatile int lockState;

        static final int WRITER = 1;
        static final int WAITER = 2;
        static final int READER = 4;


        static int tieBreakOrder(Object a, Object b) {
            int d;
            if (a == null || b == null ||
                    (d = a.getClass().getName().
                            compareTo(b.getClass().getName())) == 0)
                d = (System.identityHashCode(a) <= System.identityHashCode(b) ?
                        -1 : 1);
            return d;
        }


        TreeBin(MyConcurrentHashMap.TreeNode<K, V> b) {
            super(TREEBIN, null, null, null);
            this.first = b;
            MyConcurrentHashMap.TreeNode<K, V> r = null;
            for (MyConcurrentHashMap.TreeNode<K, V> x = b, next; x != null; x = next) {
                next = (MyConcurrentHashMap.TreeNode<K, V>) x.next;
                x.left = x.right = null;
                if (r == null) {
                    x.parent = null;
                    x.red = false;
                    r = x;
                } else {
                    K k = x.key;
                    int h = x.hash;
                    Class<?> kc = null;
                    for (MyConcurrentHashMap.TreeNode<K, V> p = r; ; ) {
                        int dir, ph;
                        K pk = p.key;
                        if ((ph = p.hash) > h)
                            dir = -1;
                        else if (ph < h)
                            dir = 1;
                        else if ((kc == null &&
                                (kc = comparableClassFor(k)) == null) ||
                                (dir = compareComparables(kc, k, pk)) == 0)
                            dir = tieBreakOrder(k, pk);
                        MyConcurrentHashMap.TreeNode<K, V> xp = p;
                        if ((p = (dir <= 0) ? p.left : p.right) == null) {
                            x.parent = xp;
                            if (dir <= 0)
                                xp.left = x;
                            else
                                xp.right = x;
                            r = balanceInsertion(r, x);
                            break;
                        }
                    }
                }
            }
            this.root = r;
            assert checkInvariants(root);
        }


        private final void lockRoot() {
            if (!U.compareAndSwapInt(this, LOCKSTATE, 0, WRITER))
                contendedLock();
        }


        private final void unlockRoot() {
            lockState = 0;
        }


        private final void contendedLock() {
            boolean waiting = false;
            for (int s; ; ) {
                if (((s = lockState) & ~WAITER) == 0) {
                    if (U.compareAndSwapInt(this, LOCKSTATE, s, WRITER)) {
                        if (waiting)
                            waiter = null;
                        return;
                    }
                } else if ((s & WAITER) == 0) {
                    if (U.compareAndSwapInt(this, LOCKSTATE, s, s | WAITER)) {
                        waiting = true;
                        waiter = Thread.currentThread();
                    }
                } else if (waiting)
                    LockSupport.park(this);
            }
        }


        final MyConcurrentHashMap.Node<K, V> find(int h, Object k) {
            if (k != null) {
                for (MyConcurrentHashMap.Node<K, V> e = first; e != null; ) {
                    int s;
                    K ek;
                    if (((s = lockState) & (WAITER | WRITER)) != 0) {
                        if (e.hash == h &&
                                ((ek = e.key) == k || (ek != null && k.equals(ek))))
                            return e;
                        e = e.next;
                    } else if (U.compareAndSwapInt(this, LOCKSTATE, s,
                            s + READER)) {
                        MyConcurrentHashMap.TreeNode<K, V> r, p;
                        try {
                            p = ((r = root) == null ? null :
                                    r.findTreeNode(h, k, null));
                        } finally {
                            Thread w;
                            if (U.getAndAddInt(this, LOCKSTATE, -READER) ==
                                    (READER | WAITER) && (w = waiter) != null)
                                LockSupport.unpark(w);
                        }
                        return p;
                    }
                }
            }
            return null;
        }


        final MyConcurrentHashMap.TreeNode<K, V> putTreeVal(int h, K k, V v) {
            Class<?> kc = null;
            boolean searched = false;
            for (MyConcurrentHashMap.TreeNode<K, V> p = root; ; ) {
                int dir, ph;
                K pk;
                if (p == null) {
                    first = root = new MyConcurrentHashMap.TreeNode<K, V>(h, k, v, null, null);
                    break;
                } else if ((ph = p.hash) > h) {
                    dir = -1;
                } else if (ph < h) {
                    dir = 1;
                } else if ((pk = p.key) == k || (pk != null && k.equals(pk))) {
                    return p;
                } else if ((kc == null &&
                        (kc = comparableClassFor(k)) == null) ||
                        (dir = compareComparables(kc, k, pk)) == 0) {
                    if (!searched) {
                        MyConcurrentHashMap.TreeNode<K, V> q, ch;
                        searched = true;
                        if (((ch = p.left) != null &&
                                (q = ch.findTreeNode(h, k, kc)) != null) ||
                                ((ch = p.right) != null &&
                                        (q = ch.findTreeNode(h, k, kc)) != null)) {
                            return q;
                        }
                    }
                    dir = tieBreakOrder(k, pk);
                }

                MyConcurrentHashMap.TreeNode<K, V> xp = p;
                if ((p = (dir <= 0) ? p.left : p.right) == null) {
                    MyConcurrentHashMap.TreeNode<K, V> x, f = first;
                    first = x = new MyConcurrentHashMap.TreeNode<K, V>(h, k, v, f, xp);
                    if (f != null) {
                        f.prev = x;
                    }
                    if (dir <= 0) {
                        xp.left = x;
                    } else {
                        xp.right = x;
                    }
                    if (!xp.red) {
                        x.red = true;
                    } else {
                        lockRoot();
                        try {
                            root = balanceInsertion(root, x);
                        } finally {
                            unlockRoot();
                        }
                    }
                    break;
                }
            }
            assert checkInvariants(root);
            return null;
        }


        final boolean removeTreeNode(MyConcurrentHashMap.TreeNode<K, V> p) {
            MyConcurrentHashMap.TreeNode<K, V> next = (MyConcurrentHashMap.TreeNode<K, V>) p.next;
            MyConcurrentHashMap.TreeNode<K, V> pred = p.prev;
            MyConcurrentHashMap.TreeNode<K, V> r, rl;
            if (pred == null) {
                first = next;
            } else {
                pred.next = next;
            }
            if (next != null) {
                next.prev = pred;
            }
            if (first == null) {
                root = null;
                return true;
            }
            if ((r = root) == null || r.right == null ||
                    (rl = r.left) == null || rl.left == null) {
                return true;
            }
            lockRoot();
            try {
                MyConcurrentHashMap.TreeNode<K, V> replacement;
                MyConcurrentHashMap.TreeNode<K, V> pl = p.left;
                MyConcurrentHashMap.TreeNode<K, V> pr = p.right;
                if (pl != null && pr != null) {
                    MyConcurrentHashMap.TreeNode<K, V> s = pr, sl;
                    while ((sl = s.left) != null) {
                        s = sl;
                    }
                    boolean c = s.red;
                    s.red = p.red;
                    p.red = c;
                    MyConcurrentHashMap.TreeNode<K, V> sr = s.right;
                    MyConcurrentHashMap.TreeNode<K, V> pp = p.parent;
                    if (s == pr) {
                        p.parent = s;
                        s.right = p;
                    } else {
                        MyConcurrentHashMap.TreeNode<K, V> sp = s.parent;
                        if ((p.parent = sp) != null) {
                            if (s == sp.left) {
                                sp.left = p;
                            } else {
                                sp.right = p;
                            }
                        }
                        if ((s.right = pr) != null) {
                            pr.parent = s;
                        }
                    }
                    p.left = null;
                    if ((p.right = sr) != null) {
                        sr.parent = p;
                    }
                    if ((s.left = pl) != null) {
                        pl.parent = s;
                    }
                    if ((s.parent = pp) == null) {
                        r = s;
                    } else if (p == pp.left) {
                        pp.left = s;
                    } else {
                        pp.right = s;
                    }
                    if (sr != null) {
                        replacement = sr;
                    } else {
                        replacement = p;
                    }
                } else if (pl != null) {
                    replacement = pl;
                } else if (pr != null) {
                    replacement = pr;
                } else {
                    replacement = p;
                }
                if (replacement != p) {
                    MyConcurrentHashMap.TreeNode<K, V> pp = replacement.parent = p.parent;
                    if (pp == null) {
                        r = replacement;
                    } else if (p == pp.left) {
                        pp.left = replacement;
                    } else {
                        pp.right = replacement;
                    }
                    p.left = p.right = p.parent = null;
                }

                root = (p.red) ? r : balanceDeletion(r, replacement);

                if (p == replacement) {
                    MyConcurrentHashMap.TreeNode<K, V> pp;
                    if ((pp = p.parent) != null) {
                        if (p == pp.left) {
                            pp.left = null;
                        } else if (p == pp.right) {
                            pp.right = null;
                        }
                        p.parent = null;
                    }
                }
            } finally {
                unlockRoot();
            }
            assert checkInvariants(root);
            return false;
        }


        static <K, V> MyConcurrentHashMap.TreeNode<K, V> rotateLeft(MyConcurrentHashMap.TreeNode<K, V> root,
                                                                    MyConcurrentHashMap.TreeNode<K, V> p) {
            MyConcurrentHashMap.TreeNode<K, V> r, pp, rl;
            if (p != null && (r = p.right) != null) {
                if ((rl = p.right = r.left) != null) {
                    rl.parent = p;
                }
                if ((pp = r.parent = p.parent) == null) {
                    (root = r).red = false;
                } else if (pp.left == p) {
                    pp.left = r;
                } else {
                    pp.right = r;
                }
                r.left = p;
                p.parent = r;
            }
            return root;
        }

        static <K, V> MyConcurrentHashMap.TreeNode<K, V> rotateRight(MyConcurrentHashMap.TreeNode<K, V> root,
                                                                     MyConcurrentHashMap.TreeNode<K, V> p) {
            MyConcurrentHashMap.TreeNode<K, V> l, pp, lr;
            if (p != null && (l = p.left) != null) {
                if ((lr = p.left = l.right) != null) {
                    lr.parent = p;
                }
                if ((pp = l.parent = p.parent) == null) {
                    (root = l).red = false;
                } else if (pp.right == p) {
                    pp.right = l;
                } else {
                    pp.left = l;
                }
                l.right = p;
                p.parent = l;
            }
            return root;
        }

        static <K, V> MyConcurrentHashMap.TreeNode<K, V> balanceInsertion(MyConcurrentHashMap.TreeNode<K, V> root,
                                                                          MyConcurrentHashMap.TreeNode<K, V> x) {
            x.red = true;
            for (MyConcurrentHashMap.TreeNode<K, V> xp, xpp, xppl, xppr; ; ) {
                if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                } else if (!xp.red || (xpp = xp.parent) == null) {
                    return root;
                }
                if (xp == (xppl = xpp.left)) {
                    if ((xppr = xpp.right) != null && xppr.red) {
                        xppr.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    } else {
                        if (x == xp.right) {
                            root = rotateLeft(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateRight(root, xpp);
                            }
                        }
                    }
                } else {
                    if (xppl != null && xppl.red) {
                        xppl.red = false;
                        xp.red = false;
                        xpp.red = true;
                        x = xpp;
                    } else {
                        if (x == xp.left) {
                            root = rotateRight(root, x = xp);
                            xpp = (xp = x.parent) == null ? null : xp.parent;
                        }
                        if (xp != null) {
                            xp.red = false;
                            if (xpp != null) {
                                xpp.red = true;
                                root = rotateLeft(root, xpp);
                            }
                        }
                    }
                }
            }
        }

        static <K, V> MyConcurrentHashMap.TreeNode<K, V> balanceDeletion(MyConcurrentHashMap.TreeNode<K, V> root,
                                                                         MyConcurrentHashMap.TreeNode<K, V> x) {
            for (MyConcurrentHashMap.TreeNode<K, V> xp, xpl, xpr; ; ) {
                if (x == null || x == root)
                    return root;
                else if ((xp = x.parent) == null) {
                    x.red = false;
                    return x;
                } else if (x.red) {
                    x.red = false;
                    return root;
                } else if ((xpl = xp.left) == x) {
                    if ((xpr = xp.right) != null && xpr.red) {
                        xpr.red = false;
                        xp.red = true;
                        root = rotateLeft(root, xp);
                        xpr = (xp = x.parent) == null ? null : xp.right;
                    }
                    if (xpr == null)
                        x = xp;
                    else {
                        MyConcurrentHashMap.TreeNode<K, V> sl = xpr.left, sr = xpr.right;
                        if ((sr == null || !sr.red) &&
                                (sl == null || !sl.red)) {
                            xpr.red = true;
                            x = xp;
                        } else {
                            if (sr == null || !sr.red) {
                                if (sl != null)
                                    sl.red = false;
                                xpr.red = true;
                                root = rotateRight(root, xpr);
                                xpr = (xp = x.parent) == null ?
                                        null : xp.right;
                            }
                            if (xpr != null) {
                                xpr.red = (xp == null) ? false : xp.red;
                                if ((sr = xpr.right) != null)
                                    sr.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateLeft(root, xp);
                            }
                            x = root;
                        }
                    }
                } else {
                    if (xpl != null && xpl.red) {
                        xpl.red = false;
                        xp.red = true;
                        root = rotateRight(root, xp);
                        xpl = (xp = x.parent) == null ? null : xp.left;
                    }
                    if (xpl == null)
                        x = xp;
                    else {
                        MyConcurrentHashMap.TreeNode<K, V> sl = xpl.left, sr = xpl.right;
                        if ((sl == null || !sl.red) &&
                                (sr == null || !sr.red)) {
                            xpl.red = true;
                            x = xp;
                        } else {
                            if (sl == null || !sl.red) {
                                if (sr != null)
                                    sr.red = false;
                                xpl.red = true;
                                root = rotateLeft(root, xpl);
                                xpl = (xp = x.parent) == null ?
                                        null : xp.left;
                            }
                            if (xpl != null) {
                                xpl.red = (xp == null) ? false : xp.red;
                                if ((sl = xpl.left) != null)
                                    sl.red = false;
                            }
                            if (xp != null) {
                                xp.red = false;
                                root = rotateRight(root, xp);
                            }
                            x = root;
                        }
                    }
                }
            }
        }


        static <K, V> boolean checkInvariants(MyConcurrentHashMap.TreeNode<K, V> t) {
            MyConcurrentHashMap.TreeNode<K, V> tp = t.parent, tl = t.left, tr = t.right,
                    tb = t.prev, tn = (MyConcurrentHashMap.TreeNode<K, V>) t.next;
            if (tb != null && tb.next != t)
                return false;
            if (tn != null && tn.prev != t)
                return false;
            if (tp != null && t != tp.left && t != tp.right)
                return false;
            if (tl != null && (tl.parent != t || tl.hash > t.hash))
                return false;
            if (tr != null && (tr.parent != t || tr.hash < t.hash))
                return false;
            if (t.red && tl != null && tl.red && tr != null && tr.red)
                return false;
            if (tl != null && !checkInvariants(tl))
                return false;
            if (tr != null && !checkInvariants(tr))
                return false;
            return true;
        }

        private static final sun.misc.Unsafe U;
        private static final long LOCKSTATE;

        static {
            try {
                U = sun.misc.Unsafe.getUnsafe();
                Class<?> k = MyConcurrentHashMap.TreeBin.class;
                LOCKSTATE = U.objectFieldOffset
                        (k.getDeclaredField("lockState"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }


    static final class TableStack<K, V> {
        int length;
        int index;
        MyConcurrentHashMap.Node<K, V>[] tab;
        MyConcurrentHashMap.TableStack<K, V> next;
    }


    static class Traverser<K, V> {
        MyConcurrentHashMap.Node<K, V>[] tab;
        MyConcurrentHashMap.Node<K, V> next;
        MyConcurrentHashMap.TableStack<K, V> stack, spare;
        int index;
        int baseIndex;
        int baseLimit;
        final int baseSize;

        Traverser(MyConcurrentHashMap.Node<K, V>[] tab, int size, int index, int limit) {
            this.tab = tab;
            this.baseSize = size;
            this.baseIndex = this.index = index;
            this.baseLimit = limit;
            this.next = null;
        }


        final MyConcurrentHashMap.Node<K, V> advance() {
            MyConcurrentHashMap.Node<K, V> e;
            if ((e = next) != null)
                e = e.next;
            for (; ; ) {
                MyConcurrentHashMap.Node<K, V>[] t;
                int i, n;
                if (e != null) {
                    return next = e;
                }
                if (baseIndex >= baseLimit || (t = tab) == null ||
                        (n = t.length) <= (i = index) || i < 0) {
                    return next = null;
                }
                if ((e = tabAt(t, i)) != null && e.hash < 0) {
                    if (e instanceof MyConcurrentHashMap.ForwardingNode) {
                        tab = ((MyConcurrentHashMap.ForwardingNode<K, V>) e).nextTable;
                        e = null;
                        pushState(t, i, n);
                        continue;
                    } else if (e instanceof MyConcurrentHashMap.TreeBin) {
                        e = ((TreeBin<K, V>) e).first;
                    } else {
                        e = null;
                    }
                }
                if (stack != null)
                    recoverState(n);
                else if ((index = i + baseSize) >= n)
                    index = ++baseIndex;
            }
        }


        private void pushState(MyConcurrentHashMap.Node<K, V>[] t, int i, int n) {
            MyConcurrentHashMap.TableStack<K, V> s = spare;
            if (s != null) {
                spare = s.next;
            } else {
                s = new TableStack<K, V>();
            }
            s.tab = t;
            s.length = n;
            s.index = i;
            s.next = stack;
            stack = s;
        }


        private void recoverState(int n) {
            MyConcurrentHashMap.TableStack<K, V> s;
            int len;
            while ((s = stack) != null && (index += (len = s.length)) >= n) {
                n = len;
                index = s.index;
                tab = s.tab;
                s.tab = null;
                MyConcurrentHashMap.TableStack<K, V> next = s.next;
                s.next = spare;
                stack = next;
                spare = s;
            }
            if (s == null && (index += baseSize) >= n)
                index = ++baseIndex;
        }
    }


    static class BaseIterator<K, V> extends MyConcurrentHashMap.Traverser<K, V> {
        final MyConcurrentHashMap<K, V> map;
        MyConcurrentHashMap.Node<K, V> lastReturned;

        BaseIterator(MyConcurrentHashMap.Node<K, V>[] tab, int size, int index, int limit,
                     MyConcurrentHashMap<K, V> map) {
            super(tab, size, index, limit);
            this.map = map;
            advance();
        }

        public final boolean hasNext() {
            return next != null;
        }

        public final boolean hasMoreElements() {
            return next != null;
        }

        public final void remove() {
            MyConcurrentHashMap.Node<K, V> p;
            if ((p = lastReturned) == null) {
                throw new IllegalStateException();
            }
            lastReturned = null;
            map.replaceNode(p.key, null, null);
        }
    }

    static final class KeyIterator<K, V> extends MyConcurrentHashMap.BaseIterator<K, V>
            implements Iterator<K>, Enumeration<K> {
        KeyIterator(MyConcurrentHashMap.Node<K, V>[] tab, int index, int size, int limit,
                    MyConcurrentHashMap<K, V> map) {
            super(tab, index, size, limit, map);
        }

        @Override
        public final K next() {
            MyConcurrentHashMap.Node<K, V> p;
            if ((p = next) == null) {
                throw new NoSuchElementException();
            }
            K k = p.key;
            lastReturned = p;
            advance();
            return k;
        }

        @Override
        public final K nextElement() {
            return next();
        }
    }

    static final class ValueIterator<K, V> extends MyConcurrentHashMap.BaseIterator<K, V>
            implements Iterator<V>, Enumeration<V> {
        ValueIterator(MyConcurrentHashMap.Node<K, V>[] tab, int index, int size, int limit,
                      MyConcurrentHashMap<K, V> map) {
            super(tab, index, size, limit, map);
        }

        @Override
        public final V next() {
            MyConcurrentHashMap.Node<K, V> p;
            if ((p = next) == null) {
                throw new NoSuchElementException();
            }
            V v = p.val;
            lastReturned = p;
            advance();
            return v;
        }

        @Override
        public final V nextElement() {
            return next();
        }
    }

    static final class EntryIterator<K, V> extends MyConcurrentHashMap.BaseIterator<K, V>
            implements Iterator<MyMap.Entry<K, V>> {
        EntryIterator(MyConcurrentHashMap.Node<K, V>[] tab, int index, int size, int limit,
                      MyConcurrentHashMap<K, V> map) {
            super(tab, index, size, limit, map);
        }

        @Override
        public final MyMap.Entry<K, V> next() {
            MyConcurrentHashMap.Node<K, V> p;
            if ((p = next) == null) {
                throw new NoSuchElementException();
            }
            K k = p.key;
            V v = p.val;
            lastReturned = p;
            advance();
            return new MyConcurrentHashMap.MapEntry<K, V>(k, v, map);
        }
    }


    static final class MapEntry<K, V> implements MyMap.Entry<K, V> {
        final K key;
        V val;
        final MyConcurrentHashMap<K, V> map;

        MapEntry(K key, V val, MyConcurrentHashMap<K, V> map) {
            this.key = key;
            this.val = val;
            this.map = map;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return val;
        }

        @Override
        public int hashCode() {
            return key.hashCode() ^ val.hashCode();
        }

        @Override
        public String toString() {
            return key + "=" + val;
        }

        @Override
        public boolean equals(Object o) {
            Object k, v;
            MyMap.Entry<?, ?> e;
            return ((o instanceof MyMap.Entry) &&
                    (k = (e = (MyMap.Entry<?, ?>) o).getKey()) != null &&
                    (v = e.getValue()) != null &&
                    (k == key || k.equals(key)) &&
                    (v == val || v.equals(val)));
        }


        @Override
        public V setValue(V value) {
            if (value == null) {
                throw new NullPointerException();
            }
            V v = val;
            val = value;
            map.put(key, value);
            return v;
        }
    }

    static final class KeySpliterator<K, V> extends MyConcurrentHashMap.Traverser<K, V>
            implements Spliterator<K> {
        long est;

        KeySpliterator(MyConcurrentHashMap.Node<K, V>[] tab, int size, int index, int limit,
                       long est) {
            super(tab, size, index, limit);
            this.est = est;
        }

        @Override
        public Spliterator<K> trySplit() {
            int i, f, h;
            return (h = ((i = baseIndex) + (f = baseLimit)) >>> 1) <= i ? null :
                    new MyConcurrentHashMap.KeySpliterator<K, V>(tab, baseSize, baseLimit = h,
                            f, est >>>= 1);
        }

        @Override
        public void forEachRemaining(Consumer<? super K> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; ) {
                action.accept(p.key);
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super K> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            MyConcurrentHashMap.Node<K, V> p;
            if ((p = advance()) == null) {
                return false;
            }
            action.accept(p.key);
            return true;
        }

        @Override
        public long estimateSize() {
            return est;
        }

        @Override
        public int characteristics() {
            return Spliterator.DISTINCT | Spliterator.CONCURRENT |
                    Spliterator.NONNULL;
        }
    }

    static final class ValueSpliterator<K, V> extends MyConcurrentHashMap.Traverser<K, V>
            implements Spliterator<V> {
        long est;

        ValueSpliterator(MyConcurrentHashMap.Node<K, V>[] tab, int size, int index, int limit,
                         long est) {
            super(tab, size, index, limit);
            this.est = est;
        }

        @Override
        public Spliterator<V> trySplit() {
            int i, f, h;
            return (h = ((i = baseIndex) + (f = baseLimit)) >>> 1) <= i ? null :
                    new MyConcurrentHashMap.ValueSpliterator<K, V>(tab, baseSize, baseLimit = h,
                            f, est >>>= 1);
        }

        @Override
        public void forEachRemaining(Consumer<? super V> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; ) {
                action.accept(p.val);
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super V> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            MyConcurrentHashMap.Node<K, V> p;
            if ((p = advance()) == null) {
                return false;
            }
            action.accept(p.val);
            return true;
        }

        @Override
        public long estimateSize() {
            return est;
        }

        @Override
        public int characteristics() {
            return Spliterator.CONCURRENT | Spliterator.NONNULL;
        }
    }

    static final class EntrySpliterator<K, V> extends MyConcurrentHashMap.Traverser<K, V>
            implements Spliterator<MyMap.Entry<K, V>> {
        final MyConcurrentHashMap<K, V> map;
        long est;

        EntrySpliterator(MyConcurrentHashMap.Node<K, V>[] tab, int size, int index, int limit,
                         long est, MyConcurrentHashMap<K, V> map) {
            super(tab, size, index, limit);
            this.map = map;
            this.est = est;
        }

        public Spliterator<MyMap.Entry<K, V>> trySplit() {
            int i, f, h;
            return (h = ((i = baseIndex) + (f = baseLimit)) >>> 1) <= i ? null :
                    new MyConcurrentHashMap.EntrySpliterator<K, V>(tab, baseSize, baseLimit = h,
                            f, est >>>= 1, map);
        }

        @Override
        public void forEachRemaining(Consumer<? super MyMap.Entry<K, V>> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; ) {
                action.accept(new MapEntry<K, V>(p.key, p.val, map));
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super MyMap.Entry<K, V>> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            MyConcurrentHashMap.Node<K, V> p;
            if ((p = advance()) == null) {
                return false;
            }
            action.accept(new MyConcurrentHashMap.MapEntry<K, V>(p.key, p.val, map));
            return true;
        }

        @Override
        public long estimateSize() {
            return est;
        }

        @Override
        public int characteristics() {
            return Spliterator.DISTINCT | Spliterator.CONCURRENT |
                    Spliterator.NONNULL;
        }
    }


    final int batchFor(long b) {
        long n;
        if (b == Long.MAX_VALUE || (n = sumCount()) <= 1L || n < b)
            return 0;
        int sp = ForkJoinPool.getCommonPoolParallelism() << 2;
        return (b <= 0L || (n /= b) >= sp) ? sp : (int) n;
    }


    public void forEach(long parallelismThreshold,
                        BiConsumer<? super K, ? super V> action) {
        if (action == null) throw new NullPointerException();
        new MyConcurrentHashMap.ForEachMappingTask<K, V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        action).invoke();
    }


    public <U> void forEach(long parallelismThreshold,
                            BiFunction<? super K, ? super V, ? extends U> transformer,
                            Consumer<? super U> action) {
        if (transformer == null || action == null)
            throw new NullPointerException();
        new MyConcurrentHashMap.ForEachTransformedMappingTask<K, V, U>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        transformer, action).invoke();
    }


    public <U> U search(long parallelismThreshold,
                        BiFunction<? super K, ? super V, ? extends U> searchFunction) {
        if (searchFunction == null) throw new NullPointerException();
        return new MyConcurrentHashMap.SearchMappingsTask<K, V, U>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        searchFunction, new AtomicReference<U>()).invoke();
    }


    public <U> U reduce(long parallelismThreshold,
                        BiFunction<? super K, ? super V, ? extends U> transformer,
                        BiFunction<? super U, ? super U, ? extends U> reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MyConcurrentHashMap.MapReduceMappingsTask<K, V, U>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, reducer).invoke();
    }


    public double reduceToDouble(long parallelismThreshold,
                                 ToDoubleBiFunction<? super K, ? super V> transformer,
                                 double basis,
                                 DoubleBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MyConcurrentHashMap.MapReduceMappingsToDoubleTask<K, V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, basis, reducer).invoke();
    }


    public long reduceToLong(long parallelismThreshold,
                             ToLongBiFunction<? super K, ? super V> transformer,
                             long basis,
                             LongBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MyConcurrentHashMap.MapReduceMappingsToLongTask<K, V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, basis, reducer).invoke();
    }


    public int reduceToInt(long parallelismThreshold,
                           ToIntBiFunction<? super K, ? super V> transformer,
                           int basis,
                           IntBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MyConcurrentHashMap.MapReduceMappingsToIntTask<K, V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, basis, reducer).invoke();
    }


    public void forEachKey(long parallelismThreshold,
                           Consumer<? super K> action) {
        if (action == null) throw new NullPointerException();
        new MyConcurrentHashMap.ForEachKeyTask<K, V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        action).invoke();
    }


    public <U> void forEachKey(long parallelismThreshold,
                               Function<? super K, ? extends U> transformer,
                               Consumer<? super U> action) {
        if (transformer == null || action == null)
            throw new NullPointerException();
        new MyConcurrentHashMap.ForEachTransformedKeyTask<K, V, U>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        transformer, action).invoke();
    }


    public <U> U searchKeys(long parallelismThreshold,
                            Function<? super K, ? extends U> searchFunction) {
        if (searchFunction == null) throw new NullPointerException();
        return new MyConcurrentHashMap.SearchKeysTask<K, V, U>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        searchFunction, new AtomicReference<U>()).invoke();
    }


    public K reduceKeys(long parallelismThreshold,
                        BiFunction<? super K, ? super K, ? extends K> reducer) {
        if (reducer == null) throw new NullPointerException();
        return new MyConcurrentHashMap.ReduceKeysTask<K, V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, reducer).invoke();
    }


    public <U> U reduceKeys(long parallelismThreshold,
                            Function<? super K, ? extends U> transformer,
                            BiFunction<? super U, ? super U, ? extends U> reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MyConcurrentHashMap.MapReduceKeysTask<K, V, U>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, reducer).invoke();
    }


    public double reduceKeysToDouble(long parallelismThreshold,
                                     ToDoubleFunction<? super K> transformer,
                                     double basis,
                                     DoubleBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MyConcurrentHashMap.MapReduceKeysToDoubleTask<K, V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, basis, reducer).invoke();
    }


    public long reduceKeysToLong(long parallelismThreshold,
                                 ToLongFunction<? super K> transformer,
                                 long basis,
                                 LongBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MyConcurrentHashMap.MapReduceKeysToLongTask<K, V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, basis, reducer).invoke();
    }


    public int reduceKeysToInt(long parallelismThreshold,
                               ToIntFunction<? super K> transformer,
                               int basis,
                               IntBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MyConcurrentHashMap.MapReduceKeysToIntTask<K, V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, basis, reducer).invoke();
    }


    public void forEachValue(long parallelismThreshold,
                             Consumer<? super V> action) {
        if (action == null)
            throw new NullPointerException();
        new MyConcurrentHashMap.ForEachValueTask<K, V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        action).invoke();
    }


    public <U> void forEachValue(long parallelismThreshold,
                                 Function<? super V, ? extends U> transformer,
                                 Consumer<? super U> action) {
        if (transformer == null || action == null)
            throw new NullPointerException();
        new MyConcurrentHashMap.ForEachTransformedValueTask<K, V, U>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        transformer, action).invoke();
    }


    public <U> U searchValues(long parallelismThreshold,
                              Function<? super V, ? extends U> searchFunction) {
        if (searchFunction == null) throw new NullPointerException();
        return new MyConcurrentHashMap.SearchValuesTask<K, V, U>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        searchFunction, new AtomicReference<U>()).invoke();
    }


    public V reduceValues(long parallelismThreshold,
                          BiFunction<? super V, ? super V, ? extends V> reducer) {
        if (reducer == null) throw new NullPointerException();
        return new MyConcurrentHashMap.ReduceValuesTask<K, V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, reducer).invoke();
    }


    public <U> U reduceValues(long parallelismThreshold,
                              Function<? super V, ? extends U> transformer,
                              BiFunction<? super U, ? super U, ? extends U> reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MyConcurrentHashMap.MapReduceValuesTask<K, V, U>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, reducer).invoke();
    }


    public double reduceValuesToDouble(long parallelismThreshold,
                                       ToDoubleFunction<? super V> transformer,
                                       double basis,
                                       DoubleBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MyConcurrentHashMap.MapReduceValuesToDoubleTask<K, V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, basis, reducer).invoke();
    }


    public long reduceValuesToLong(long parallelismThreshold,
                                   ToLongFunction<? super V> transformer,
                                   long basis,
                                   LongBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MyConcurrentHashMap.MapReduceValuesToLongTask<K, V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, basis, reducer).invoke();
    }


    public int reduceValuesToInt(long parallelismThreshold,
                                 ToIntFunction<? super V> transformer,
                                 int basis,
                                 IntBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MyConcurrentHashMap.MapReduceValuesToIntTask<K, V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, basis, reducer).invoke();
    }


    public void forEachEntry(long parallelismThreshold,
                             Consumer<? super MyMap.Entry<K, V>> action) {
        if (action == null) throw new NullPointerException();
        new MyConcurrentHashMap.ForEachEntryTask<K, V>(null, batchFor(parallelismThreshold), 0, 0, table,
                action).invoke();
    }


    public <U> void forEachEntry(long parallelismThreshold,
                                 Function<MyMap.Entry<K, V>, ? extends U> transformer,
                                 Consumer<? super U> action) {
        if (transformer == null || action == null)
            throw new NullPointerException();
        new MyConcurrentHashMap.ForEachTransformedEntryTask<K, V, U>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        transformer, action).invoke();
    }


    public <U> U searchEntries(long parallelismThreshold,
                               Function<MyMap.Entry<K, V>, ? extends U> searchFunction) {
        if (searchFunction == null) {
            throw new NullPointerException();
        }
        return new MyConcurrentHashMap.SearchEntriesTask<K, V, U>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        searchFunction, new AtomicReference<U>()).invoke();
    }


    public MyMap.Entry<K, V> reduceEntries(long parallelismThreshold,
                                           BiFunction<MyMap.Entry<K, V>, MyMap.Entry<K, V>, ? extends MyMap.Entry<K, V>> reducer) {
        if (reducer == null) {
            throw new NullPointerException();
        }
        return new MyConcurrentHashMap.ReduceEntriesTask<K, V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, reducer).invoke();
    }


    public <U> U reduceEntries(long parallelismThreshold,
                               Function<MyMap.Entry<K, V>, ? extends U> transformer,
                               BiFunction<? super U, ? super U, ? extends U> reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MyConcurrentHashMap.MapReduceEntriesTask<K, V, U>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, reducer).invoke();
    }


    public double reduceEntriesToDouble(long parallelismThreshold,
                                        ToDoubleFunction<MyMap.Entry<K, V>> transformer,
                                        double basis,
                                        DoubleBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MyConcurrentHashMap.MapReduceEntriesToDoubleTask<K, V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, basis, reducer).invoke();
    }


    public long reduceEntriesToLong(long parallelismThreshold,
                                    ToLongFunction<MyMap.Entry<K, V>> transformer,
                                    long basis,
                                    LongBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MyConcurrentHashMap.MapReduceEntriesToLongTask<K, V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, basis, reducer).invoke();
    }


    public int reduceEntriesToInt(long parallelismThreshold,
                                  ToIntFunction<MyMap.Entry<K, V>> transformer,
                                  int basis,
                                  IntBinaryOperator reducer) {
        if (transformer == null || reducer == null)
            throw new NullPointerException();
        return new MyConcurrentHashMap.MapReduceEntriesToIntTask<K, V>
                (null, batchFor(parallelismThreshold), 0, 0, table,
                        null, transformer, basis, reducer).invoke();
    }


    abstract static class CollectionView<K, V, E>
            implements MyCollection<E>, java.io.Serializable {
        private static final long serialVersionUID = 7249069246763182397L;
        final MyConcurrentHashMap<K, V> map;

        CollectionView(MyConcurrentHashMap<K, V> map) {
            this.map = map;
        }


        public MyConcurrentHashMap<K, V> getMap() {
            return map;
        }

        @Override
        public final void clear() {
            map.clear();
        }

        @Override
        public final int size() {
            return map.size();
        }

        @Override
        public final boolean isEmpty() {
            return map.isEmpty();
        }


        public abstract Iterator<E> iterator();

        public abstract boolean contains(Object o);

        public abstract boolean remove(Object o);

        private static final String oomeMsg = "Required array size too large";

        @Override
        public final Object[] toArray() {
            long sz = map.mappingCount();
            if (sz > MAX_ARRAY_SIZE) {
                throw new OutOfMemoryError(oomeMsg);
            }
            int n = (int) sz;
            Object[] r = new Object[n];
            int i = 0;
            for (E e : this) {
                if (i == n) {
                    if (n >= MAX_ARRAY_SIZE) {
                        throw new OutOfMemoryError(oomeMsg);
                    }
                    if (n >= MAX_ARRAY_SIZE - (MAX_ARRAY_SIZE >>> 1) - 1) {
                        n = MAX_ARRAY_SIZE;
                    } else {
                        n += (n >>> 1) + 1;
                    }
                    r = Arrays.copyOf(r, n);
                }
                r[i++] = e;
            }
            return (i == n) ? r : Arrays.copyOf(r, i);
        }

        @Override
        @SuppressWarnings("unchecked")
        public final <T> T[] toArray(T[] a) {
            long sz = map.mappingCount();
            if (sz > MAX_ARRAY_SIZE) {
                throw new OutOfMemoryError(oomeMsg);
            }
            int m = (int) sz;
            T[] r = (a.length >= m) ? a :
                    (T[]) java.lang.reflect.Array
                            .newInstance(a.getClass().getComponentType(), m);
            int n = r.length;
            int i = 0;
            for (E e : this) {
                if (i == n) {
                    if (n >= MAX_ARRAY_SIZE) {
                        throw new OutOfMemoryError(oomeMsg);
                    }
                    if (n >= MAX_ARRAY_SIZE - (MAX_ARRAY_SIZE >>> 1) - 1) {
                        n = MAX_ARRAY_SIZE;
                    } else {
                        n += (n >>> 1) + 1;
                    }
                    r = Arrays.copyOf(r, n);
                }
                r[i++] = (T) e;
            }
            if (a == r && i < n) {
                r[i] = null;
                return r;
            }
            return (i == n) ? r : Arrays.copyOf(r, i);
        }

        @Override
        public final String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            Iterator<E> it = iterator();
            if (it.hasNext()) {
                for (; ; ) {
                    Object e = it.next();
                    sb.append(e == this ? "(this Collection)" : e);
                    if (!it.hasNext()) {
                        break;
                    }
                    sb.append(',').append(' ');
                }
            }
            return sb.append(']').toString();
        }

        @Override
        public final boolean containsAll(MyCollection<?> c) {
            if (c != this) {
                for (Object e : c) {
                    if (e == null || !contains(e)) {
                        return false;
                    }
                }
            }
            return true;
        }

        @Override
        public final boolean removeAll(MyCollection<?> c) {
            if (c == null) {
                throw new NullPointerException();
            }
            boolean modified = false;
            for (Iterator<E> it = iterator(); it.hasNext(); ) {
                if (c.contains(it.next())) {
                    it.remove();
                    modified = true;
                }
            }
            return modified;
        }

        @Override
        public final boolean retainAll(MyCollection<?> c) {
            if (c == null) {
                throw new NullPointerException();
            }
            boolean modified = false;
            for (Iterator<E> it = iterator(); it.hasNext(); ) {
                if (!c.contains(it.next())) {
                    it.remove();
                    modified = true;
                }
            }
            return modified;
        }

    }


    public static class KeySetView<K, V> extends MyConcurrentHashMap.CollectionView<K, V, K>
            implements MySet<K>, java.io.Serializable {
        private static final long serialVersionUID = 7249069246763182397L;
        private final V value;

        KeySetView(MyConcurrentHashMap<K, V> map, V value) {
            super(map);
            this.value = value;
        }


        public V getMappedValue() {
            return value;
        }

        @Override
        public boolean contains(Object o) {
            return map.containsKey(o);
        }

        @Override
        public boolean remove(Object o) {
            return map.remove(o) != null;
        }

        @Override
        public Iterator<K> iterator() {
            MyConcurrentHashMap.Node<K, V>[] t;
            MyConcurrentHashMap<K, V> m = map;
            int f = (t = m.table) == null ? 0 : t.length;
            return new MyConcurrentHashMap.KeyIterator<K, V>(t, f, 0, f, m);
        }

        @Override
        public boolean add(K e) {
            V v;
            if ((v = value) == null) {
                throw new UnsupportedOperationException();
            }
            return map.putVal(e, v, true) == null;
        }

        @Override
        public boolean addAll(MyCollection<? extends K> c) {
            boolean added = false;
            V v;
            if ((v = value) == null) {
                throw new UnsupportedOperationException();
            }
            for (K e : c) {
                if (map.putVal(e, v, true) == null) {
                    added = true;
                }
            }
            return added;
        }

        @Override
        public int hashCode() {
            int h = 0;
            for (K e : this) {
                h += e.hashCode();
            }
            return h;
        }

        @Override
        public boolean equals(Object o) {
            MySet<?> c;
            return ((o instanceof MySet) &&
                    ((c = (MySet<?>) o) == this ||
                            (containsAll(c) && c.containsAll(this))));
        }

        @Override
        public Spliterator<K> spliterator() {
            MyConcurrentHashMap.Node<K, V>[] t;
            MyConcurrentHashMap<K, V> m = map;
            long n = m.sumCount();
            int f = (t = m.table) == null ? 0 : t.length;
            return new MyConcurrentHashMap.KeySpliterator<K, V>(t, f, 0, f, n < 0L ? 0L : n);
        }

        @Override
        public void forEach(Consumer<? super K> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            MyConcurrentHashMap.Node<K, V>[] t;
            if ((t = map.table) != null) {
                MyConcurrentHashMap.Traverser<K, V> it = new MyConcurrentHashMap.Traverser<K, V>(t, t.length, 0, t.length);
                for (MyConcurrentHashMap.Node<K, V> p; (p = it.advance()) != null; ) {
                    action.accept(p.key);
                }
            }
        }
    }


    static final class ValuesView<K, V> extends MyConcurrentHashMap.CollectionView<K, V, V>
            implements MyCollection<V>, java.io.Serializable {
        private static final long serialVersionUID = 2249069246763182397L;

        ValuesView(MyConcurrentHashMap<K, V> map) {
            super(map);
        }

        @Override
        public final boolean contains(Object o) {
            return map.containsValue(o);
        }

        @Override
        public final boolean remove(Object o) {
            if (o != null) {
                for (Iterator<V> it = iterator(); it.hasNext(); ) {
                    if (o.equals(it.next())) {
                        it.remove();
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public final Iterator<V> iterator() {
            MyConcurrentHashMap<K, V> m = map;
            MyConcurrentHashMap.Node<K, V>[] t;
            int f = (t = m.table) == null ? 0 : t.length;
            return new MyConcurrentHashMap.ValueIterator<K, V>(t, f, 0, f, m);
        }

        @Override
        public final boolean add(V e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public final boolean addAll(MyCollection<? extends V> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Spliterator<V> spliterator() {
            MyConcurrentHashMap.Node<K, V>[] t;
            MyConcurrentHashMap<K, V> m = map;
            long n = m.sumCount();
            int f = (t = m.table) == null ? 0 : t.length;
            return new MyConcurrentHashMap.ValueSpliterator<K, V>(t, f, 0, f, n < 0L ? 0L : n);
        }

        @Override
        public void forEach(Consumer<? super V> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            MyConcurrentHashMap.Node<K, V>[] t;
            if ((t = map.table) != null) {
                MyConcurrentHashMap.Traverser<K, V> it = new MyConcurrentHashMap.Traverser<K, V>(t, t.length, 0, t.length);
                for (MyConcurrentHashMap.Node<K, V> p; (p = it.advance()) != null; ) {
                    action.accept(p.val);
                }
            }
        }
    }


    static final class EntrySetView<K, V> extends MyConcurrentHashMap.CollectionView<K, V, Entry<K, V>>
            implements MySet<MyMap.Entry<K, V>>, java.io.Serializable {
        private static final long serialVersionUID = 2249069246763182397L;

        EntrySetView(MyConcurrentHashMap<K, V> map) {
            super(map);
        }

        @Override
        public boolean contains(Object o) {
            Object k, v, r;
            MyMap.Entry<?, ?> e;
            return ((o instanceof MyMap.Entry) &&
                    (k = (e = (MyMap.Entry<?, ?>) o).getKey()) != null &&
                    (r = map.get(k)) != null &&
                    (v = e.getValue()) != null &&
                    (v == r || v.equals(r)));
        }

        @Override
        public boolean remove(Object o) {
            Object k, v;
            MyMap.Entry<?, ?> e;
            return ((o instanceof MyMap.Entry) &&
                    (k = (e = (MyMap.Entry<?, ?>) o).getKey()) != null &&
                    (v = e.getValue()) != null &&
                    map.remove(k, v));
        }

        @Override
        public Iterator<MyMap.Entry<K, V>> iterator() {
            MyConcurrentHashMap<K, V> m = map;
            MyConcurrentHashMap.Node<K, V>[] t;
            int f = (t = m.table) == null ? 0 : t.length;
            return new MyConcurrentHashMap.EntryIterator<K, V>(t, f, 0, f, m);
        }

        @Override
        public boolean add(Entry<K, V> e) {
            return map.putVal(e.getKey(), e.getValue(), false) == null;
        }

        @Override
        public boolean addAll(MyCollection<? extends Entry<K, V>> c) {
            boolean added = false;
            for (Entry<K, V> e : c) {
                if (add(e)) {
                    added = true;
                }
            }
            return added;
        }

        @Override
        public final int hashCode() {
            int h = 0;
            MyConcurrentHashMap.Node<K, V>[] t;
            if ((t = map.table) != null) {
                MyConcurrentHashMap.Traverser<K, V> it = new MyConcurrentHashMap.Traverser<K, V>(t, t.length, 0, t.length);
                for (MyConcurrentHashMap.Node<K, V> p; (p = it.advance()) != null; ) {
                    h += p.hashCode();
                }
            }
            return h;
        }

        @Override
        public final boolean equals(Object o) {
            MySet<?> c;
            return ((o instanceof Set) &&
                    ((c = (MySet<?>) o) == this ||
                            (containsAll(c) && c.containsAll(this))));
        }

        @Override
        public Spliterator<MyMap.Entry<K, V>> spliterator() {
            MyConcurrentHashMap.Node<K, V>[] t;
            MyConcurrentHashMap<K, V> m = map;
            long n = m.sumCount();
            int f = (t = m.table) == null ? 0 : t.length;
            return new MyConcurrentHashMap.EntrySpliterator<K, V>(t, f, 0, f, n < 0L ? 0L : n, m);
        }

        @Override
        public void forEach(Consumer<? super MyMap.Entry<K, V>> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            MyConcurrentHashMap.Node<K, V>[] t;
            if ((t = map.table) != null) {
                MyConcurrentHashMap.Traverser<K, V> it = new MyConcurrentHashMap.Traverser<K, V>(t, t.length, 0, t.length);
                for (MyConcurrentHashMap.Node<K, V> p; (p = it.advance()) != null; ) {
                    action.accept(new MapEntry<K, V>(p.key, p.val, map));
                }
            }
        }

    }


    @SuppressWarnings("serial")
    abstract static class BulkTask<K, V, R> extends CountedCompleter<R> {
        MyConcurrentHashMap.Node<K, V>[] tab;
        MyConcurrentHashMap.Node<K, V> next;
        MyConcurrentHashMap.TableStack<K, V> stack, spare;
        int index;
        int baseIndex;
        int baseLimit;
        final int baseSize;
        int batch;

        BulkTask(MyConcurrentHashMap.BulkTask<K, V, ?> par, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t) {
            super(par);
            this.batch = b;
            this.index = this.baseIndex = i;
            if ((this.tab = t) == null)
                this.baseSize = this.baseLimit = 0;
            else if (par == null)
                this.baseSize = this.baseLimit = t.length;
            else {
                this.baseLimit = f;
                this.baseSize = par.baseSize;
            }
        }


        final MyConcurrentHashMap.Node<K, V> advance() {
            MyConcurrentHashMap.Node<K, V> e;
            if ((e = next) != null) {
                e = e.next;
            }
            for (; ; ) {
                MyConcurrentHashMap.Node<K, V>[] t;
                int i, n;
                if (e != null) {
                    return next = e;
                }
                if (baseIndex >= baseLimit || (t = tab) == null ||
                        (n = t.length) <= (i = index) || i < 0) {
                    return next = null;
                }
                if ((e = tabAt(t, i)) != null && e.hash < 0) {
                    if (e instanceof MyConcurrentHashMap.ForwardingNode) {
                        tab = ((MyConcurrentHashMap.ForwardingNode<K, V>) e).nextTable;
                        e = null;
                        pushState(t, i, n);
                        continue;
                    } else if (e instanceof MyConcurrentHashMap.TreeBin) {
                        e = ((TreeBin<K, V>) e).first;
                    } else {
                        e = null;
                    }
                }
                if (stack != null) {
                    recoverState(n);
                } else if ((index = i + baseSize) >= n) {
                    index = ++baseIndex;
                }
            }
        }

        private void pushState(MyConcurrentHashMap.Node<K, V>[] t, int i, int n) {
            MyConcurrentHashMap.TableStack<K, V> s = spare;
            if (s != null) {
                spare = s.next;
            } else {
                s = new TableStack<K, V>();
            }
            s.tab = t;
            s.length = n;
            s.index = i;
            s.next = stack;
            stack = s;
        }

        private void recoverState(int n) {
            MyConcurrentHashMap.TableStack<K, V> s;
            int len;
            while ((s = stack) != null && (index += (len = s.length)) >= n) {
                n = len;
                index = s.index;
                tab = s.tab;
                s.tab = null;
                MyConcurrentHashMap.TableStack<K, V> next = s.next;
                s.next = spare;
                stack = next;
                spare = s;
            }
            if (s == null && (index += baseSize) >= n) {
                index = ++baseIndex;
            }
        }
    }


    @SuppressWarnings("serial")
    static final class ForEachKeyTask<K, V>
            extends MyConcurrentHashMap.BulkTask<K, V, Void> {
        final Consumer<? super K> action;

        ForEachKeyTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 Consumer<? super K> action) {
            super(p, b, i, f, t);
            this.action = action;
        }

        @Override
        public final void compute() {
            final Consumer<? super K> action;
            if ((action = this.action) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    addToPendingCount(1);
                    new MyConcurrentHashMap.ForEachKeyTask<K, V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    action).fork();
                }
                for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; ) {
                    action.accept(p.key);
                }
                propagateCompletion();
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ForEachValueTask<K, V>
            extends MyConcurrentHashMap.BulkTask<K, V, Void> {
        final Consumer<? super V> action;

        ForEachValueTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 Consumer<? super V> action) {
            super(p, b, i, f, t);
            this.action = action;
        }

        @Override
        public final void compute() {
            final Consumer<? super V> action;
            if ((action = this.action) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    addToPendingCount(1);
                    new MyConcurrentHashMap.ForEachValueTask<K, V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    action).fork();
                }
                for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; )
                    action.accept(p.val);
                propagateCompletion();
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ForEachEntryTask<K, V>
            extends MyConcurrentHashMap.BulkTask<K, V, Void> {
        final Consumer<? super Entry<K, V>> action;

        ForEachEntryTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 Consumer<? super Entry<K, V>> action) {
            super(p, b, i, f, t);
            this.action = action;
        }

        public final void compute() {
            final Consumer<? super Entry<K, V>> action;
            if ((action = this.action) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    addToPendingCount(1);
                    new MyConcurrentHashMap.ForEachEntryTask<K, V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    action).fork();
                }
                for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; )
                    action.accept(p);
                propagateCompletion();
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ForEachMappingTask<K, V>
            extends MyConcurrentHashMap.BulkTask<K, V, Void> {
        final BiConsumer<? super K, ? super V> action;

        ForEachMappingTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 BiConsumer<? super K, ? super V> action) {
            super(p, b, i, f, t);
            this.action = action;
        }

        public final void compute() {
            final BiConsumer<? super K, ? super V> action;
            if ((action = this.action) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    addToPendingCount(1);
                    new MyConcurrentHashMap.ForEachMappingTask<K, V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    action).fork();
                }
                for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; )
                    action.accept(p.key, p.val);
                propagateCompletion();
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ForEachTransformedKeyTask<K, V, U>
            extends MyConcurrentHashMap.BulkTask<K, V, Void> {
        final Function<? super K, ? extends U> transformer;
        final Consumer<? super U> action;

        ForEachTransformedKeyTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 Function<? super K, ? extends U> transformer, Consumer<? super U> action) {
            super(p, b, i, f, t);
            this.transformer = transformer;
            this.action = action;
        }

        public final void compute() {
            final Function<? super K, ? extends U> transformer;
            final Consumer<? super U> action;
            if ((transformer = this.transformer) != null &&
                    (action = this.action) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    addToPendingCount(1);
                    new MyConcurrentHashMap.ForEachTransformedKeyTask<K, V, U>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    transformer, action).fork();
                }
                for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; ) {
                    U u;
                    if ((u = transformer.apply(p.key)) != null) {
                        action.accept(u);
                    }
                }
                propagateCompletion();
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ForEachTransformedValueTask<K, V, U>
            extends MyConcurrentHashMap.BulkTask<K, V, Void> {
        final Function<? super V, ? extends U> transformer;
        final Consumer<? super U> action;

        ForEachTransformedValueTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 Function<? super V, ? extends U> transformer, Consumer<? super U> action) {
            super(p, b, i, f, t);
            this.transformer = transformer;
            this.action = action;
        }

        @Override
        public final void compute() {
            final Function<? super V, ? extends U> transformer;
            final Consumer<? super U> action;
            if ((transformer = this.transformer) != null &&
                    (action = this.action) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    addToPendingCount(1);
                    new MyConcurrentHashMap.ForEachTransformedValueTask<K, V, U>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    transformer, action).fork();
                }
                for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; ) {
                    U u;
                    if ((u = transformer.apply(p.val)) != null) {
                        action.accept(u);
                    }
                }
                propagateCompletion();
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ForEachTransformedEntryTask<K, V, U>
            extends MyConcurrentHashMap.BulkTask<K, V, Void> {
        final Function<MyMap.Entry<K, V>, ? extends U> transformer;
        final Consumer<? super U> action;

        ForEachTransformedEntryTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 Function<MyMap.Entry<K, V>, ? extends U> transformer, Consumer<? super U> action) {
            super(p, b, i, f, t);
            this.transformer = transformer;
            this.action = action;
        }

        @Override
        public final void compute() {
            final Function<MyMap.Entry<K, V>, ? extends U> transformer;
            final Consumer<? super U> action;
            if ((transformer = this.transformer) != null &&
                    (action = this.action) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    addToPendingCount(1);
                    new MyConcurrentHashMap.ForEachTransformedEntryTask<K, V, U>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    transformer, action).fork();
                }
                for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; ) {
                    U u;
                    if ((u = transformer.apply(p)) != null) {
                        action.accept(u);
                    }
                }
                propagateCompletion();
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ForEachTransformedMappingTask<K, V, U>
            extends MyConcurrentHashMap.BulkTask<K, V, Void> {
        final BiFunction<? super K, ? super V, ? extends U> transformer;
        final Consumer<? super U> action;

        ForEachTransformedMappingTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 BiFunction<? super K, ? super V, ? extends U> transformer,
                 Consumer<? super U> action) {
            super(p, b, i, f, t);
            this.transformer = transformer;
            this.action = action;
        }

        @Override
        public final void compute() {
            final BiFunction<? super K, ? super V, ? extends U> transformer;
            final Consumer<? super U> action;
            if ((transformer = this.transformer) != null &&
                    (action = this.action) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    addToPendingCount(1);
                    new MyConcurrentHashMap.ForEachTransformedMappingTask<K, V, U>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    transformer, action).fork();
                }
                for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; ) {
                    U u;
                    if ((u = transformer.apply(p.key, p.val)) != null) {
                        action.accept(u);
                    }
                }
                propagateCompletion();
            }
        }
    }

    @SuppressWarnings("serial")
    static final class SearchKeysTask<K, V, U>
            extends MyConcurrentHashMap.BulkTask<K, V, U> {
        final Function<? super K, ? extends U> searchFunction;
        final AtomicReference<U> result;

        SearchKeysTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 Function<? super K, ? extends U> searchFunction,
                 AtomicReference<U> result) {
            super(p, b, i, f, t);
            this.searchFunction = searchFunction;
            this.result = result;
        }

        @Override
        public final U getRawResult() {
            return result.get();
        }

        @Override
        public final void compute() {
            final Function<? super K, ? extends U> searchFunction;
            final AtomicReference<U> result;
            if ((searchFunction = this.searchFunction) != null &&
                    (result = this.result) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    if (result.get() != null)
                        return;
                    addToPendingCount(1);
                    new MyConcurrentHashMap.SearchKeysTask<K, V, U>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    searchFunction, result).fork();
                }
                while (result.get() == null) {
                    U u;
                    MyConcurrentHashMap.Node<K, V> p;
                    if ((p = advance()) == null) {
                        propagateCompletion();
                        break;
                    }
                    if ((u = searchFunction.apply(p.key)) != null) {
                        if (result.compareAndSet(null, u))
                            quietlyCompleteRoot();
                        break;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class SearchValuesTask<K, V, U>
            extends MyConcurrentHashMap.BulkTask<K, V, U> {
        final Function<? super V, ? extends U> searchFunction;
        final AtomicReference<U> result;

        SearchValuesTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 Function<? super V, ? extends U> searchFunction,
                 AtomicReference<U> result) {
            super(p, b, i, f, t);
            this.searchFunction = searchFunction;
            this.result = result;
        }

        public final U getRawResult() {
            return result.get();
        }

        public final void compute() {
            final Function<? super V, ? extends U> searchFunction;
            final AtomicReference<U> result;
            if ((searchFunction = this.searchFunction) != null &&
                    (result = this.result) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    if (result.get() != null)
                        return;
                    addToPendingCount(1);
                    new MyConcurrentHashMap.SearchValuesTask<K, V, U>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    searchFunction, result).fork();
                }
                while (result.get() == null) {
                    U u;
                    MyConcurrentHashMap.Node<K, V> p;
                    if ((p = advance()) == null) {
                        propagateCompletion();
                        break;
                    }
                    if ((u = searchFunction.apply(p.val)) != null) {
                        if (result.compareAndSet(null, u))
                            quietlyCompleteRoot();
                        break;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class SearchEntriesTask<K, V, U>
            extends MyConcurrentHashMap.BulkTask<K, V, U> {
        final Function<Entry<K, V>, ? extends U> searchFunction;
        final AtomicReference<U> result;

        SearchEntriesTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 Function<Entry<K, V>, ? extends U> searchFunction,
                 AtomicReference<U> result) {
            super(p, b, i, f, t);
            this.searchFunction = searchFunction;
            this.result = result;
        }

        public final U getRawResult() {
            return result.get();
        }

        public final void compute() {
            final Function<Entry<K, V>, ? extends U> searchFunction;
            final AtomicReference<U> result;
            if ((searchFunction = this.searchFunction) != null &&
                    (result = this.result) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    if (result.get() != null)
                        return;
                    addToPendingCount(1);
                    new MyConcurrentHashMap.SearchEntriesTask<K, V, U>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    searchFunction, result).fork();
                }
                while (result.get() == null) {
                    U u;
                    MyConcurrentHashMap.Node<K, V> p;
                    if ((p = advance()) == null) {
                        propagateCompletion();
                        break;
                    }
                    if ((u = searchFunction.apply(p)) != null) {
                        if (result.compareAndSet(null, u))
                            quietlyCompleteRoot();
                        return;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class SearchMappingsTask<K, V, U>
            extends MyConcurrentHashMap.BulkTask<K, V, U> {
        final BiFunction<? super K, ? super V, ? extends U> searchFunction;
        final AtomicReference<U> result;

        SearchMappingsTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 BiFunction<? super K, ? super V, ? extends U> searchFunction,
                 AtomicReference<U> result) {
            super(p, b, i, f, t);
            this.searchFunction = searchFunction;
            this.result = result;
        }

        public final U getRawResult() {
            return result.get();
        }

        public final void compute() {
            final BiFunction<? super K, ? super V, ? extends U> searchFunction;
            final AtomicReference<U> result;
            if ((searchFunction = this.searchFunction) != null &&
                    (result = this.result) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    if (result.get() != null)
                        return;
                    addToPendingCount(1);
                    new MyConcurrentHashMap.SearchMappingsTask<K, V, U>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    searchFunction, result).fork();
                }
                while (result.get() == null) {
                    U u;
                    MyConcurrentHashMap.Node<K, V> p;
                    if ((p = advance()) == null) {
                        propagateCompletion();
                        break;
                    }
                    if ((u = searchFunction.apply(p.key, p.val)) != null) {
                        if (result.compareAndSet(null, u))
                            quietlyCompleteRoot();
                        break;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ReduceKeysTask<K, V>
            extends MyConcurrentHashMap.BulkTask<K, V, K> {
        final BiFunction<? super K, ? super K, ? extends K> reducer;
        K result;
        MyConcurrentHashMap.ReduceKeysTask<K, V> rights, nextRight;

        ReduceKeysTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 MyConcurrentHashMap.ReduceKeysTask<K, V> nextRight,
                 BiFunction<? super K, ? super K, ? extends K> reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.reducer = reducer;
        }

        public final K getRawResult() {
            return result;
        }

        public final void compute() {
            final BiFunction<? super K, ? super K, ? extends K> reducer;
            if ((reducer = this.reducer) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    addToPendingCount(1);
                    (rights = new MyConcurrentHashMap.ReduceKeysTask<K, V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, reducer)).fork();
                }
                K r = null;
                for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; ) {
                    K u = p.key;
                    r = (r == null) ? u : u == null ? r : reducer.apply(r, u);
                }
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MyConcurrentHashMap.ReduceKeysTask<K, V>
                            t = (MyConcurrentHashMap.ReduceKeysTask<K, V>) c,
                            s = t.rights;
                    while (s != null) {
                        K tr, sr;
                        if ((sr = s.result) != null)
                            t.result = (((tr = t.result) == null) ? sr :
                                    reducer.apply(tr, sr));
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ReduceValuesTask<K, V>
            extends MyConcurrentHashMap.BulkTask<K, V, V> {
        final BiFunction<? super V, ? super V, ? extends V> reducer;
        V result;
        MyConcurrentHashMap.ReduceValuesTask<K, V> rights, nextRight;

        ReduceValuesTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 MyConcurrentHashMap.ReduceValuesTask<K, V> nextRight,
                 BiFunction<? super V, ? super V, ? extends V> reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.reducer = reducer;
        }

        public final V getRawResult() {
            return result;
        }

        public final void compute() {
            final BiFunction<? super V, ? super V, ? extends V> reducer;
            if ((reducer = this.reducer) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    addToPendingCount(1);
                    (rights = new MyConcurrentHashMap.ReduceValuesTask<K, V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, reducer)).fork();
                }
                V r = null;
                for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; ) {
                    V v = p.val;
                    r = (r == null) ? v : reducer.apply(r, v);
                }
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MyConcurrentHashMap.ReduceValuesTask<K, V>
                            t = (MyConcurrentHashMap.ReduceValuesTask<K, V>) c,
                            s = t.rights;
                    while (s != null) {
                        V tr, sr;
                        if ((sr = s.result) != null)
                            t.result = (((tr = t.result) == null) ? sr :
                                    reducer.apply(tr, sr));
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class ReduceEntriesTask<K, V>
            extends MyConcurrentHashMap.BulkTask<K, V, Entry<K, V>> {
        final BiFunction<MyMap.Entry<K, V>, MyMap.Entry<K, V>, ? extends MyMap.Entry<K, V>> reducer;
        MyMap.Entry<K, V> result;
        MyConcurrentHashMap.ReduceEntriesTask<K, V> rights, nextRight;

        ReduceEntriesTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 MyConcurrentHashMap.ReduceEntriesTask<K, V> nextRight,
                 BiFunction<Entry<K, V>, MyMap.Entry<K, V>, ? extends MyMap.Entry<K, V>> reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.reducer = reducer;
        }

        public final MyMap.Entry<K, V> getRawResult() {
            return result;
        }

        public final void compute() {
            final BiFunction<MyMap.Entry<K, V>, MyMap.Entry<K, V>, ? extends MyMap.Entry<K, V>> reducer;
            if ((reducer = this.reducer) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    addToPendingCount(1);
                    (rights = new MyConcurrentHashMap.ReduceEntriesTask<K, V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, reducer)).fork();
                }
                MyMap.Entry<K, V> r = null;
                for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; )
                    r = (r == null) ? p : reducer.apply(r, p);
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MyConcurrentHashMap.ReduceEntriesTask<K, V>
                            t = (MyConcurrentHashMap.ReduceEntriesTask<K, V>) c,
                            s = t.rights;
                    while (s != null) {
                        MyMap.Entry<K, V> tr, sr;
                        if ((sr = s.result) != null)
                            t.result = (((tr = t.result) == null) ? sr :
                                    reducer.apply(tr, sr));
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceKeysTask<K, V, U>
            extends MyConcurrentHashMap.BulkTask<K, V, U> {
        final Function<? super K, ? extends U> transformer;
        final BiFunction<? super U, ? super U, ? extends U> reducer;
        U result;
        MyConcurrentHashMap.MapReduceKeysTask<K, V, U> rights, nextRight;

        MapReduceKeysTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 MyConcurrentHashMap.MapReduceKeysTask<K, V, U> nextRight,
                 Function<? super K, ? extends U> transformer,
                 BiFunction<? super U, ? super U, ? extends U> reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.reducer = reducer;
        }

        public final U getRawResult() {
            return result;
        }

        public final void compute() {
            final Function<? super K, ? extends U> transformer;
            final BiFunction<? super U, ? super U, ? extends U> reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    addToPendingCount(1);
                    (rights = new MyConcurrentHashMap.MapReduceKeysTask<K, V, U>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, reducer)).fork();
                }
                U r = null;
                for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; ) {
                    U u;
                    if ((u = transformer.apply(p.key)) != null)
                        r = (r == null) ? u : reducer.apply(r, u);
                }
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MyConcurrentHashMap.MapReduceKeysTask<K, V, U>
                            t = (MyConcurrentHashMap.MapReduceKeysTask<K, V, U>) c,
                            s = t.rights;
                    while (s != null) {
                        U tr, sr;
                        if ((sr = s.result) != null)
                            t.result = (((tr = t.result) == null) ? sr :
                                    reducer.apply(tr, sr));
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceValuesTask<K, V, U>
            extends MyConcurrentHashMap.BulkTask<K, V, U> {
        final Function<? super V, ? extends U> transformer;
        final BiFunction<? super U, ? super U, ? extends U> reducer;
        U result;
        MyConcurrentHashMap.MapReduceValuesTask<K, V, U> rights, nextRight;

        MapReduceValuesTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 MyConcurrentHashMap.MapReduceValuesTask<K, V, U> nextRight,
                 Function<? super V, ? extends U> transformer,
                 BiFunction<? super U, ? super U, ? extends U> reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.reducer = reducer;
        }

        @Override
        public final U getRawResult() {
            return result;
        }

        @Override
        public final void compute() {
            final Function<? super V, ? extends U> transformer;
            final BiFunction<? super U, ? super U, ? extends U> reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    addToPendingCount(1);
                    (rights = new MyConcurrentHashMap.MapReduceValuesTask<K, V, U>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, reducer)).fork();
                }
                U r = null;
                for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; ) {
                    U u;
                    if ((u = transformer.apply(p.val)) != null)
                        r = (r == null) ? u : reducer.apply(r, u);
                }
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MyConcurrentHashMap.MapReduceValuesTask<K, V, U>
                            t = (MyConcurrentHashMap.MapReduceValuesTask<K, V, U>) c,
                            s = t.rights;
                    while (s != null) {
                        U tr, sr;
                        if ((sr = s.result) != null)
                            t.result = (((tr = t.result) == null) ? sr :
                                    reducer.apply(tr, sr));
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceEntriesTask<K, V, U>
            extends MyConcurrentHashMap.BulkTask<K, V, U> {
        final Function<MyMap.Entry<K, V>, ? extends U> transformer;
        final BiFunction<? super U, ? super U, ? extends U> reducer;
        U result;
        MyConcurrentHashMap.MapReduceEntriesTask<K, V, U> rights, nextRight;

        MapReduceEntriesTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 MyConcurrentHashMap.MapReduceEntriesTask<K, V, U> nextRight,
                 Function<MyMap.Entry<K, V>, ? extends U> transformer,
                 BiFunction<? super U, ? super U, ? extends U> reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.reducer = reducer;
        }

        @Override
        public final U getRawResult() {
            return result;
        }

        @Override
        public final void compute() {
            final Function<MyMap.Entry<K, V>, ? extends U> transformer;
            final BiFunction<? super U, ? super U, ? extends U> reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    addToPendingCount(1);
                    (rights = new MyConcurrentHashMap.MapReduceEntriesTask<K, V, U>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, reducer)).fork();
                }
                U r = null;
                for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; ) {
                    U u;
                    if ((u = transformer.apply(p)) != null) {
                        r = (r == null) ? u : reducer.apply(r, u);
                    }
                }
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MyConcurrentHashMap.MapReduceEntriesTask<K, V, U>
                            t = (MyConcurrentHashMap.MapReduceEntriesTask<K, V, U>) c,
                            s = t.rights;
                    while (s != null) {
                        U tr, sr;
                        if ((sr = s.result) != null) {
                            t.result = (((tr = t.result) == null) ? sr :
                                    reducer.apply(tr, sr));
                        }
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceMappingsTask<K, V, U>
            extends MyConcurrentHashMap.BulkTask<K, V, U> {
        final BiFunction<? super K, ? super V, ? extends U> transformer;
        final BiFunction<? super U, ? super U, ? extends U> reducer;
        U result;
        MyConcurrentHashMap.MapReduceMappingsTask<K, V, U> rights, nextRight;

        MapReduceMappingsTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 MyConcurrentHashMap.MapReduceMappingsTask<K, V, U> nextRight,
                 BiFunction<? super K, ? super V, ? extends U> transformer,
                 BiFunction<? super U, ? super U, ? extends U> reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.reducer = reducer;
        }

        @Override
        public final U getRawResult() {
            return result;
        }

        @Override
        public final void compute() {
            final BiFunction<? super K, ? super V, ? extends U> transformer;
            final BiFunction<? super U, ? super U, ? extends U> reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    addToPendingCount(1);
                    (rights = new MyConcurrentHashMap.MapReduceMappingsTask<K, V, U>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, reducer)).fork();
                }
                U r = null;
                for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; ) {
                    U u;
                    if ((u = transformer.apply(p.key, p.val)) != null)
                        r = (r == null) ? u : reducer.apply(r, u);
                }
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MyConcurrentHashMap.MapReduceMappingsTask<K, V, U>
                            t = (MyConcurrentHashMap.MapReduceMappingsTask<K, V, U>) c,
                            s = t.rights;
                    while (s != null) {
                        U tr, sr;
                        if ((sr = s.result) != null)
                            t.result = (((tr = t.result) == null) ? sr :
                                    reducer.apply(tr, sr));
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceKeysToDoubleTask<K, V>
            extends MyConcurrentHashMap.BulkTask<K, V, Double> {
        final ToDoubleFunction<? super K> transformer;
        final DoubleBinaryOperator reducer;
        final double basis;
        double result;
        MyConcurrentHashMap.MapReduceKeysToDoubleTask<K, V> rights, nextRight;

        MapReduceKeysToDoubleTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 MyConcurrentHashMap.MapReduceKeysToDoubleTask<K, V> nextRight,
                 ToDoubleFunction<? super K> transformer,
                 double basis,
                 DoubleBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        @Override
        public final Double getRawResult() {
            return result;
        }

        @Override
        public final void compute() {
            final ToDoubleFunction<? super K> transformer;
            final DoubleBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                double r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    addToPendingCount(1);
                    (rights = new MyConcurrentHashMap.MapReduceKeysToDoubleTask<K, V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, r, reducer)).fork();
                }
                for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; )
                    r = reducer.applyAsDouble(r, transformer.applyAsDouble(p.key));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MyConcurrentHashMap.MapReduceKeysToDoubleTask<K, V>
                            t = (MyConcurrentHashMap.MapReduceKeysToDoubleTask<K, V>) c,
                            s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsDouble(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceValuesToDoubleTask<K, V>
            extends MyConcurrentHashMap.BulkTask<K, V, Double> {
        final ToDoubleFunction<? super V> transformer;
        final DoubleBinaryOperator reducer;
        final double basis;
        double result;
        MyConcurrentHashMap.MapReduceValuesToDoubleTask<K, V> rights, nextRight;

        MapReduceValuesToDoubleTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 MyConcurrentHashMap.MapReduceValuesToDoubleTask<K, V> nextRight,
                 ToDoubleFunction<? super V> transformer,
                 double basis,
                 DoubleBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        @Override
        public final Double getRawResult() {
            return result;
        }

        @Override
        public final void compute() {
            final ToDoubleFunction<? super V> transformer;
            final DoubleBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                double r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    addToPendingCount(1);
                    (rights = new MyConcurrentHashMap.MapReduceValuesToDoubleTask<K, V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, r, reducer)).fork();
                }
                for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; )
                    r = reducer.applyAsDouble(r, transformer.applyAsDouble(p.val));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MyConcurrentHashMap.MapReduceValuesToDoubleTask<K, V>
                            t = (MyConcurrentHashMap.MapReduceValuesToDoubleTask<K, V>) c,
                            s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsDouble(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceEntriesToDoubleTask<K, V>
            extends MyConcurrentHashMap.BulkTask<K, V, Double> {
        final ToDoubleFunction<MyMap.Entry<K, V>> transformer;
        final DoubleBinaryOperator reducer;
        final double basis;
        double result;
        MyConcurrentHashMap.MapReduceEntriesToDoubleTask<K, V> rights, nextRight;

        MapReduceEntriesToDoubleTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 MyConcurrentHashMap.MapReduceEntriesToDoubleTask<K, V> nextRight,
                 ToDoubleFunction<MyMap.Entry<K, V>> transformer,
                 double basis,
                 DoubleBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        @Override
        public final Double getRawResult() {
            return result;
        }

        @Override
        public final void compute() {
            final ToDoubleFunction<MyMap.Entry<K, V>> transformer;
            final DoubleBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                double r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    addToPendingCount(1);
                    (rights = new MyConcurrentHashMap.MapReduceEntriesToDoubleTask<K, V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, r, reducer)).fork();
                }
                for (Node<K, V> p; (p = advance()) != null; ) {
                    r = reducer.applyAsDouble(r, transformer.applyAsDouble(p));
                }
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MyConcurrentHashMap.MapReduceEntriesToDoubleTask<K, V>
                            t = (MyConcurrentHashMap.MapReduceEntriesToDoubleTask<K, V>) c,
                            s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsDouble(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceMappingsToDoubleTask<K, V>
            extends MyConcurrentHashMap.BulkTask<K, V, Double> {
        final ToDoubleBiFunction<? super K, ? super V> transformer;
        final DoubleBinaryOperator reducer;
        final double basis;
        double result;
        MyConcurrentHashMap.MapReduceMappingsToDoubleTask<K, V> rights, nextRight;

        MapReduceMappingsToDoubleTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 MyConcurrentHashMap.MapReduceMappingsToDoubleTask<K, V> nextRight,
                 ToDoubleBiFunction<? super K, ? super V> transformer,
                 double basis,
                 DoubleBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        public final Double getRawResult() {
            return result;
        }

        public final void compute() {
            final ToDoubleBiFunction<? super K, ? super V> transformer;
            final DoubleBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                double r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    addToPendingCount(1);
                    (rights = new MyConcurrentHashMap.MapReduceMappingsToDoubleTask<K, V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, r, reducer)).fork();
                }
                for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; ) {
                    r = reducer.applyAsDouble(r, transformer.applyAsDouble(p.key, p.val));
                }
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MyConcurrentHashMap.MapReduceMappingsToDoubleTask<K, V>
                            t = (MyConcurrentHashMap.MapReduceMappingsToDoubleTask<K, V>) c,
                            s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsDouble(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceKeysToLongTask<K, V>
            extends MyConcurrentHashMap.BulkTask<K, V, Long> {
        final ToLongFunction<? super K> transformer;
        final LongBinaryOperator reducer;
        final long basis;
        long result;
        MyConcurrentHashMap.MapReduceKeysToLongTask<K, V> rights, nextRight;

        MapReduceKeysToLongTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 MyConcurrentHashMap.MapReduceKeysToLongTask<K, V> nextRight,
                 ToLongFunction<? super K> transformer,
                 long basis,
                 LongBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        @Override
        public final Long getRawResult() {
            return result;
        }

        @Override
        public final void compute() {
            final ToLongFunction<? super K> transformer;
            final LongBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                long r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    addToPendingCount(1);
                    (rights = new MyConcurrentHashMap.MapReduceKeysToLongTask<K, V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, r, reducer)).fork();
                }
                for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; )
                    r = reducer.applyAsLong(r, transformer.applyAsLong(p.key));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MyConcurrentHashMap.MapReduceKeysToLongTask<K, V>
                            t = (MyConcurrentHashMap.MapReduceKeysToLongTask<K, V>) c,
                            s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsLong(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceValuesToLongTask<K, V>
            extends MyConcurrentHashMap.BulkTask<K, V, Long> {
        final ToLongFunction<? super V> transformer;
        final LongBinaryOperator reducer;
        final long basis;
        long result;
        MyConcurrentHashMap.MapReduceValuesToLongTask<K, V> rights, nextRight;

        MapReduceValuesToLongTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 MyConcurrentHashMap.MapReduceValuesToLongTask<K, V> nextRight,
                 ToLongFunction<? super V> transformer,
                 long basis,
                 LongBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        @Override
        public final Long getRawResult() {
            return result;
        }

        @Override
        public final void compute() {
            final ToLongFunction<? super V> transformer;
            final LongBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                long r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    addToPendingCount(1);
                    (rights = new MyConcurrentHashMap.MapReduceValuesToLongTask<K, V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, r, reducer)).fork();
                }
                for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; )
                    r = reducer.applyAsLong(r, transformer.applyAsLong(p.val));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MyConcurrentHashMap.MapReduceValuesToLongTask<K, V>
                            t = (MyConcurrentHashMap.MapReduceValuesToLongTask<K, V>) c,
                            s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsLong(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceEntriesToLongTask<K, V>
            extends MyConcurrentHashMap.BulkTask<K, V, Long> {
        final ToLongFunction<MyMap.Entry<K, V>> transformer;
        final LongBinaryOperator reducer;
        final long basis;
        long result;
        MyConcurrentHashMap.MapReduceEntriesToLongTask<K, V> rights, nextRight;

        MapReduceEntriesToLongTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 MyConcurrentHashMap.MapReduceEntriesToLongTask<K, V> nextRight,
                 ToLongFunction<MyMap.Entry<K, V>> transformer,
                 long basis,
                 LongBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        @Override
        public final Long getRawResult() {
            return result;
        }

        @Override
        public final void compute() {
            final ToLongFunction<MyMap.Entry<K, V>> transformer;
            final LongBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                long r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    addToPendingCount(1);
                    (rights = new MyConcurrentHashMap.MapReduceEntriesToLongTask<K, V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, r, reducer)).fork();
                }
                for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; ) {
                    r = reducer.applyAsLong(r, transformer.applyAsLong(p));
                }
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MyConcurrentHashMap.MapReduceEntriesToLongTask<K, V>
                            t = (MyConcurrentHashMap.MapReduceEntriesToLongTask<K, V>) c,
                            s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsLong(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceMappingsToLongTask<K, V>
            extends MyConcurrentHashMap.BulkTask<K, V, Long> {
        final ToLongBiFunction<? super K, ? super V> transformer;
        final LongBinaryOperator reducer;
        final long basis;
        long result;
        MyConcurrentHashMap.MapReduceMappingsToLongTask<K, V> rights, nextRight;

        MapReduceMappingsToLongTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 MyConcurrentHashMap.MapReduceMappingsToLongTask<K, V> nextRight,
                 ToLongBiFunction<? super K, ? super V> transformer,
                 long basis,
                 LongBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        @Override
        public final Long getRawResult() {
            return result;
        }

        @Override
        public final void compute() {
            final ToLongBiFunction<? super K, ? super V> transformer;
            final LongBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                long r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    addToPendingCount(1);
                    (rights = new MyConcurrentHashMap.MapReduceMappingsToLongTask<K, V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, r, reducer)).fork();
                }
                for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; )
                    r = reducer.applyAsLong(r, transformer.applyAsLong(p.key, p.val));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MyConcurrentHashMap.MapReduceMappingsToLongTask<K, V>
                            t = (MyConcurrentHashMap.MapReduceMappingsToLongTask<K, V>) c,
                            s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsLong(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceKeysToIntTask<K, V>
            extends MyConcurrentHashMap.BulkTask<K, V, Integer> {
        final ToIntFunction<? super K> transformer;
        final IntBinaryOperator reducer;
        final int basis;
        int result;
        MyConcurrentHashMap.MapReduceKeysToIntTask<K, V> rights, nextRight;

        MapReduceKeysToIntTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 MyConcurrentHashMap.MapReduceKeysToIntTask<K, V> nextRight,
                 ToIntFunction<? super K> transformer,
                 int basis,
                 IntBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        @Override
        public final Integer getRawResult() {
            return result;
        }

        @Override
        public final void compute() {
            final ToIntFunction<? super K> transformer;
            final IntBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                int r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    addToPendingCount(1);
                    (rights = new MyConcurrentHashMap.MapReduceKeysToIntTask<K, V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, r, reducer)).fork();
                }
                for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; )
                    r = reducer.applyAsInt(r, transformer.applyAsInt(p.key));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MyConcurrentHashMap.MapReduceKeysToIntTask<K, V>
                            t = (MyConcurrentHashMap.MapReduceKeysToIntTask<K, V>) c,
                            s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsInt(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceValuesToIntTask<K, V>
            extends MyConcurrentHashMap.BulkTask<K, V, Integer> {
        final ToIntFunction<? super V> transformer;
        final IntBinaryOperator reducer;
        final int basis;
        int result;
        MyConcurrentHashMap.MapReduceValuesToIntTask<K, V> rights, nextRight;

        MapReduceValuesToIntTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 MyConcurrentHashMap.MapReduceValuesToIntTask<K, V> nextRight,
                 ToIntFunction<? super V> transformer,
                 int basis,
                 IntBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        @Override
        public final Integer getRawResult() {
            return result;
        }

        @Override
        public final void compute() {
            final ToIntFunction<? super V> transformer;
            final IntBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                int r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    addToPendingCount(1);
                    (rights = new MyConcurrentHashMap.MapReduceValuesToIntTask<K, V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, r, reducer)).fork();
                }
                for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; )
                    r = reducer.applyAsInt(r, transformer.applyAsInt(p.val));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MyConcurrentHashMap.MapReduceValuesToIntTask<K, V>
                            t = (MyConcurrentHashMap.MapReduceValuesToIntTask<K, V>) c,
                            s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsInt(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceEntriesToIntTask<K, V>
            extends MyConcurrentHashMap.BulkTask<K, V, Integer> {
        final ToIntFunction<MyMap.Entry<K, V>> transformer;
        final IntBinaryOperator reducer;
        final int basis;
        int result;
        MyConcurrentHashMap.MapReduceEntriesToIntTask<K, V> rights, nextRight;

        MapReduceEntriesToIntTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 MyConcurrentHashMap.MapReduceEntriesToIntTask<K, V> nextRight,
                 ToIntFunction<MyMap.Entry<K, V>> transformer,
                 int basis,
                 IntBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        @Override
        public final Integer getRawResult() {
            return result;
        }

        @Override
        public final void compute() {
            final ToIntFunction<MyMap.Entry<K, V>> transformer;
            final IntBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                int r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    addToPendingCount(1);
                    (rights = new MyConcurrentHashMap.MapReduceEntriesToIntTask<K, V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, r, reducer)).fork();
                }
                for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; ) {
                    r = reducer.applyAsInt(r, transformer.applyAsInt(p));
                }
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MyConcurrentHashMap.MapReduceEntriesToIntTask<K, V>
                            t = (MyConcurrentHashMap.MapReduceEntriesToIntTask<K, V>) c,
                            s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsInt(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }

    @SuppressWarnings("serial")
    static final class MapReduceMappingsToIntTask<K, V>
            extends MyConcurrentHashMap.BulkTask<K, V, Integer> {
        final ToIntBiFunction<? super K, ? super V> transformer;
        final IntBinaryOperator reducer;
        final int basis;
        int result;
        MyConcurrentHashMap.MapReduceMappingsToIntTask<K, V> rights, nextRight;

        MapReduceMappingsToIntTask
                (MyConcurrentHashMap.BulkTask<K, V, ?> p, int b, int i, int f, MyConcurrentHashMap.Node<K, V>[] t,
                 MyConcurrentHashMap.MapReduceMappingsToIntTask<K, V> nextRight,
                 ToIntBiFunction<? super K, ? super V> transformer,
                 int basis,
                 IntBinaryOperator reducer) {
            super(p, b, i, f, t);
            this.nextRight = nextRight;
            this.transformer = transformer;
            this.basis = basis;
            this.reducer = reducer;
        }

        @Override
        public final Integer getRawResult() {
            return result;
        }

        @Override
        public final void compute() {
            final ToIntBiFunction<? super K, ? super V> transformer;
            final IntBinaryOperator reducer;
            if ((transformer = this.transformer) != null &&
                    (reducer = this.reducer) != null) {
                int r = this.basis;
                for (int i = baseIndex, f, h; batch > 0 &&
                        (h = ((f = baseLimit) + i) >>> 1) > i; ) {
                    addToPendingCount(1);
                    (rights = new MyConcurrentHashMap.MapReduceMappingsToIntTask<K, V>
                            (this, batch >>>= 1, baseLimit = h, f, tab,
                                    rights, transformer, r, reducer)).fork();
                }
                for (MyConcurrentHashMap.Node<K, V> p; (p = advance()) != null; )
                    r = reducer.applyAsInt(r, transformer.applyAsInt(p.key, p.val));
                result = r;
                CountedCompleter<?> c;
                for (c = firstComplete(); c != null; c = c.nextComplete()) {
                    @SuppressWarnings("unchecked")
                    MyConcurrentHashMap.MapReduceMappingsToIntTask<K, V>
                            t = (MyConcurrentHashMap.MapReduceMappingsToIntTask<K, V>) c,
                            s = t.rights;
                    while (s != null) {
                        t.result = reducer.applyAsInt(t.result, s.result);
                        s = t.rights = s.nextRight;
                    }
                }
            }
        }
    }


    private static final sun.misc.Unsafe U;
    private static final long SIZECTL;
    private static final long TRANSFERINDEX;
    private static final long BASECOUNT;
    private static final long CELLSBUSY;
    private static final long CELLVALUE;
    private static final long ABASE;
    private static final int ASHIFT;

    static {
        try {
            U = sun.misc.Unsafe.getUnsafe();
            Class<?> k = MyConcurrentHashMap.class;
            SIZECTL = U.objectFieldOffset
                    (k.getDeclaredField("sizeCtl"));
            TRANSFERINDEX = U.objectFieldOffset
                    (k.getDeclaredField("transferIndex"));
            BASECOUNT = U.objectFieldOffset
                    (k.getDeclaredField("baseCount"));
            CELLSBUSY = U.objectFieldOffset
                    (k.getDeclaredField("cellsBusy"));
            Class<?> ck = MyConcurrentHashMap.CounterCell.class;
            CELLVALUE = U.objectFieldOffset
                    (ck.getDeclaredField("value"));
            Class<?> ak = MyConcurrentHashMap.Node[].class;
            ABASE = U.arrayBaseOffset(ak);
            int scale = U.arrayIndexScale(ak);
            if ((scale & (scale - 1)) != 0) {
                throw new Error("data type scale not a power of two");
            }
            ASHIFT = 31 - Integer.numberOfLeadingZeros(scale);
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
