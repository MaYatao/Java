package example.container.collection.current;


import example.container.collection.*;
import example.container.collection.MyCollections;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class MyConcurrentSkipListMap<K, V> extends MyAbstractMap<K, V>
        implements MyConcurrentNavigableMap<K, V>, Cloneable, Serializable {



    private static final long serialVersionUID = -8627078645895051609L;


    private static final Object BASE_HEADER = new Object();


    private transient volatile MyConcurrentSkipListMap.HeadIndex<K, V> head;

    final Comparator<? super K> comparator;

    private transient MyConcurrentSkipListMap.KeySet<K> keySet;

    private transient MyConcurrentSkipListMap.EntrySet<K, V> entrySet;

    private transient MyConcurrentSkipListMap.Values<V> values;

    private transient MyConcurrentNavigableMap<K, V> descendingMap;


    private void initialize() {
        keySet = null;
        entrySet = null;
        values = null;
        descendingMap = null;
        head = new MyConcurrentSkipListMap.HeadIndex<K, V>(new MyConcurrentSkipListMap.Node<K, V>(null, BASE_HEADER, null),
                null, null, 1);
    }


    private boolean casHead(MyConcurrentSkipListMap.HeadIndex<K, V> cmp, MyConcurrentSkipListMap.HeadIndex<K, V> val) {
        return UNSAFE.compareAndSwapObject(this, headOffset, cmp, val);
    }


    static final class Node<K, V> {
        final K key;
        volatile Object value;
        volatile MyConcurrentSkipListMap.Node<K, V> next;


        Node(K key, Object value, MyConcurrentSkipListMap.Node<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }


        Node(MyConcurrentSkipListMap.Node<K, V> next) {
            this.key = null;
            this.value = this;
            this.next = next;
        }


        boolean casValue(Object cmp, Object val) {
            return UNSAFE.compareAndSwapObject(this, valueOffset, cmp, val);
        }


        boolean casNext(MyConcurrentSkipListMap.Node<K, V> cmp, MyConcurrentSkipListMap.Node<K, V> val) {
            return UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
        }


        boolean isMarker() {
            return value == this;
        }


        boolean isBaseHeader() {
            return value == BASE_HEADER;
        }


        boolean appendMarker(MyConcurrentSkipListMap.Node<K, V> f) {
            return casNext(f, new MyConcurrentSkipListMap.Node<K, V>(f));
        }


        void helpDelete(MyConcurrentSkipListMap.Node<K, V> b, MyConcurrentSkipListMap.Node<K, V> f) {

            if (f == next && this == b.next) {
                if (f == null || f.value != f) {
                    casNext(f, new Node<K, V>(f));
                } else {
                    b.casNext(this, f.next);
                }
            }
        }


        V getValidValue() {

            Object v = value;
            if (v == this || v == BASE_HEADER) {
                return null;
            }
            @SuppressWarnings("unchecked") V vv = (V) v;
            return vv;
        }


        MyAbstractMap.SimpleImmutableEntry<K, V> createSnapshot() {
            Object v = value;
            if (v == null || v == this || v == BASE_HEADER) {
                return null;
            }
            @SuppressWarnings("unchecked") V vv = (V) v;
            return new MyAbstractMap.SimpleImmutableEntry<K, V>(key, vv);
        }


        private static final sun.misc.Unsafe UNSAFE;
        private static final long valueOffset;
        private static final long nextOffset;

        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = MyConcurrentSkipListMap.Node.class;
                valueOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("value"));
                nextOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("next"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }


    static class Index<K, V> {
        final MyConcurrentSkipListMap.Node<K, V> node;
        final MyConcurrentSkipListMap.Index<K, V> down;
        volatile MyConcurrentSkipListMap.Index<K, V> right;


        Index(MyConcurrentSkipListMap.Node<K, V> node, MyConcurrentSkipListMap.Index<K, V> down, MyConcurrentSkipListMap.Index<K, V> right) {
            this.node = node;
            this.down = down;
            this.right = right;
        }


        final boolean casRight(MyConcurrentSkipListMap.Index<K, V> cmp, MyConcurrentSkipListMap.Index<K, V> val) {
            return UNSAFE.compareAndSwapObject(this, rightOffset, cmp, val);
        }


        final boolean indexesDeletedNode() {
            return node.value == null;
        }


        final boolean link(MyConcurrentSkipListMap.Index<K, V> succ, MyConcurrentSkipListMap.Index<K, V> newSucc) {
            MyConcurrentSkipListMap.Node<K, V> n = node;
            newSucc.right = succ;
            return n.value != null && casRight(succ, newSucc);
        }


        final boolean unlink(MyConcurrentSkipListMap.Index<K, V> succ) {
            return node.value != null && casRight(succ, succ.right);
        }


        private static final sun.misc.Unsafe UNSAFE;
        private static final long rightOffset;

        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = MyConcurrentSkipListMap.Index.class;
                rightOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("right"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }


    static final class HeadIndex<K, V> extends MyConcurrentSkipListMap.Index<K, V> {
        final int level;

        HeadIndex(MyConcurrentSkipListMap.Node<K, V> node, MyConcurrentSkipListMap.Index<K, V> down, MyConcurrentSkipListMap.Index<K, V> right, int level) {
            super(node, down, right);
            this.level = level;
        }
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    static final int cpr(Comparator c, Object x, Object y) {
        return (c != null) ? c.compare(x, y) : ((Comparable) x).compareTo(y);
    }


    private MyConcurrentSkipListMap.Node<K, V> findPredecessor(Object key, Comparator<? super K> cmp) {
        if (key == null) {
            throw new NullPointerException();
        }
        for (; ; ) {
            for (MyConcurrentSkipListMap.Index<K, V> q = head, r = q.right, d; ; ) {
                if (r != null) {
                    MyConcurrentSkipListMap.Node<K, V> n = r.node;
                    K k = n.key;
                    if (n.value == null) {
                        if (!q.unlink(r)) {
                            break;
                        }
                        r = q.right;
                        continue;
                    }
                    if (cpr(cmp, key, k) > 0) {
                        q = r;
                        r = r.right;
                        continue;
                    }
                }
                if ((d = q.down) == null) {
                    return q.node;
                }
                q = d;
                r = d.right;
            }
        }
    }


    private MyConcurrentSkipListMap.Node<K, V> findNode(Object key) {
        if (key == null) {
            throw new NullPointerException();
        }
        Comparator<? super K> cmp = comparator;
        outer:
        for (; ; ) {
            for (MyConcurrentSkipListMap.Node<K, V> b = findPredecessor(key, cmp), n = b.next; ; ) {
                Object v;
                int c;
                if (n == null) {
                    break outer;
                }
                MyConcurrentSkipListMap.Node<K, V> f = n.next;
                if (n != b.next) {
                    break;
                }
                if ((v = n.value) == null) {
                    n.helpDelete(b, f);
                    break;
                }
                if (b.value == null || v == n) {
                    break;
                }
                if ((c = cpr(cmp, key, n.key)) == 0) {
                    return n;
                }
                if (c < 0) {
                    break outer;
                }
                b = n;
                n = f;
            }
        }
        return null;
    }


    private V doGet(Object key) {
        if (key == null) {
            throw new NullPointerException();
        }
        Comparator<? super K> cmp = comparator;
        outer:
        for (; ; ) {
            for (MyConcurrentSkipListMap.Node<K, V> b = findPredecessor(key, cmp), n = b.next; ; ) {
                Object v;
                int c;
                if (n == null) {
                    break outer;
                }
                MyConcurrentSkipListMap.Node<K, V> f = n.next;
                if (n != b.next) {
                    break;
                }
                if ((v = n.value) == null) {
                    n.helpDelete(b, f);
                    break;
                }
                if (b.value == null || v == n) {
                    break;
                }
                if ((c = cpr(cmp, key, n.key)) == 0) {
                    @SuppressWarnings("unchecked") V vv = (V) v;
                    return vv;
                }
                if (c < 0) {
                    break outer;
                }
                b = n;
                n = f;
            }
        }
        return null;
    }


    private V doPut(K key, V value, boolean onlyIfAbsent) {
        MyConcurrentSkipListMap.Node<K, V> z;
        if (key == null) {
            throw new NullPointerException();
        }
        Comparator<? super K> cmp = comparator;
        outer:
        for (; ; ) {
            for (MyConcurrentSkipListMap.Node<K, V> b = findPredecessor(key, cmp), n = b.next; ; ) {
                if (n != null) {
                    Object v;
                    int c;
                    MyConcurrentSkipListMap.Node<K, V> f = n.next;
                    if (n != b.next) {
                        break;
                    }
                    if ((v = n.value) == null) {
                        n.helpDelete(b, f);
                        break;
                    }
                    if (b.value == null || v == n) {
                        break;
                    }
                    if ((c = cpr(cmp, key, n.key)) > 0) {
                        b = n;
                        n = f;
                        continue;
                    }
                    if (c == 0) {
                        if (onlyIfAbsent || n.casValue(v, value)) {
                            @SuppressWarnings("unchecked") V vv = (V) v;
                            return vv;
                        }
                        break;
                    }

                }

                z = new MyConcurrentSkipListMap.Node<K, V>(key, value, n);
                if (!b.casNext(n, z)) {
                    break;
                }
                break outer;
            }
        }

        int rnd = MyThreadLocalRandom.nextSecondarySeed();
        if ((rnd & 0x80000001) == 0) {
            int level = 1, max;
            while (((rnd >>>= 1) & 1) != 0) {
                ++level;
            }
            MyConcurrentSkipListMap.Index<K, V> idx = null;
            MyConcurrentSkipListMap.HeadIndex<K, V> h = head;
            if (level <= (max = h.level)) {
                for (int i = 1; i <= level; ++i) {
                    idx = new Index<K, V>(z, idx, null);
                }
            } else {
                level = max + 1;
                @SuppressWarnings("unchecked") MyConcurrentSkipListMap.Index<K, V>[] idxs =
                        (MyConcurrentSkipListMap.Index<K, V>[]) new MyConcurrentSkipListMap.Index<?, ?>[level + 1];
                for (int i = 1; i <= level; ++i)
                    idxs[i] = idx = new MyConcurrentSkipListMap.Index<K, V>(z, idx, null);
                for (; ; ) {
                    h = head;
                    int oldLevel = h.level;
                    if (level <= oldLevel)
                        break;
                    MyConcurrentSkipListMap.HeadIndex<K, V> newh = h;
                    MyConcurrentSkipListMap.Node<K, V> oldbase = h.node;
                    for (int j = oldLevel + 1; j <= level; ++j) {
                        newh = new HeadIndex<K, V>(oldbase, newh, idxs[j], j);
                    }
                    if (casHead(h, newh)) {
                        h = newh;
                        idx = idxs[level = oldLevel];
                        break;
                    }
                }
            }

            splice:
            for (int insertionLevel = level; ; ) {
                int j = h.level;
                for (MyConcurrentSkipListMap.Index<K, V> q = h, r = q.right, t = idx; ; ) {
                    if (q == null || t == null) {
                        break splice;
                    }
                    if (r != null) {
                        MyConcurrentSkipListMap.Node<K, V> n = r.node;

                        int c = cpr(cmp, key, n.key);
                        if (n.value == null) {
                            if (!q.unlink(r)) {
                                break;
                            }
                            r = q.right;
                            continue;
                        }
                        if (c > 0) {
                            q = r;
                            r = r.right;
                            continue;
                        }
                    }

                    if (j == insertionLevel) {
                        if (!q.link(r, t)) {
                            break;
                        }
                        if (t.node.value == null) {
                            findNode(key);
                            break splice;
                        }
                        if (--insertionLevel == 0) {
                            break splice;
                        }
                    }

                    if (--j >= insertionLevel && j < level) {
                        t = t.down;
                    }
                    q = q.down;
                    r = q.right;
                }
            }
        }
        return null;
    }


    final V doRemove(Object key, Object value) {
        if (key == null) {
            throw new NullPointerException();
        }
        Comparator<? super K> cmp = comparator;
        outer:
        for (; ; ) {
            for (MyConcurrentSkipListMap.Node<K, V> b = findPredecessor(key, cmp), n = b.next; ; ) {
                Object v;
                int c;
                if (n == null) {
                    break outer;
                }
                MyConcurrentSkipListMap.Node<K, V> f = n.next;
                if (n != b.next) {
                    break;
                }
                if ((v = n.value) == null) {
                    n.helpDelete(b, f);
                    break;
                }
                if (b.value == null || v == n) {
                    break;
                }
                if ((c = cpr(cmp, key, n.key)) < 0) {
                    break outer;
                }
                if (c > 0) {
                    b = n;
                    n = f;
                    continue;
                }
                if (value != null && !value.equals(v)) {
                    break outer;
                }
                if (!n.casValue(v, null)) {
                    break;
                }
                if (!n.appendMarker(f) || !b.casNext(n, f)) {
                    findNode(key);
                } else {
                    findPredecessor(key, cmp);
                    if (head.right == null) {
                        tryReduceLevel();
                    }
                }
                @SuppressWarnings("unchecked") V vv = (V) v;
                return vv;
            }
        }
        return null;
    }


    private void tryReduceLevel() {
        MyConcurrentSkipListMap.HeadIndex<K, V> h = head;
        MyConcurrentSkipListMap.HeadIndex<K, V> d;
        MyConcurrentSkipListMap.HeadIndex<K, V> e;
        if (h.level > 3 &&
                (d = (MyConcurrentSkipListMap.HeadIndex<K, V>) h.down) != null &&
                (e = (MyConcurrentSkipListMap.HeadIndex<K, V>) d.down) != null &&
                e.right == null &&
                d.right == null &&
                h.right == null &&
                casHead(h, d) &&
                h.right != null) {
            casHead(d, h);
        }
    }


    final MyConcurrentSkipListMap.Node<K, V> findFirst() {
        for (MyConcurrentSkipListMap.Node<K, V> b, n; ; ) {
            if ((n = (b = head.node).next) == null) {
                return null;
            }
            if (n.value != null) {
                return n;
            }
            n.helpDelete(b, n.next);
        }
    }


    private MyMap.Entry<K, V> doRemoveFirstEntry() {
        for (MyConcurrentSkipListMap.Node<K, V> b, n; ; ) {
            if ((n = (b = head.node).next) == null) {
                return null;
            }
            MyConcurrentSkipListMap.Node<K, V> f = n.next;
            if (n != b.next) {
                continue;
            }
            Object v = n.value;
            if (v == null) {
                n.helpDelete(b, f);
                continue;
            }
            if (!n.casValue(v, null)) {
                continue;
            }
            if (!n.appendMarker(f) || !b.casNext(n, f)) {
                findFirst();
            }
            clearIndexToFirst();
            @SuppressWarnings("unchecked") V vv = (V) v;
            return new MyAbstractMap.SimpleImmutableEntry<K, V>(n.key, vv);
        }
    }


    private void clearIndexToFirst() {
        for (; ; ) {
            for (MyConcurrentSkipListMap.Index<K, V> q = head; ; ) {
                MyConcurrentSkipListMap.Index<K, V> r = q.right;
                if (r != null && r.indexesDeletedNode() && !q.unlink(r)) {
                    break;
                }
                if ((q = q.down) == null) {
                    if (head.right == null) {
                        tryReduceLevel();
                    }
                    return;
                }
            }
        }
    }


    private MyMap.Entry<K, V> doRemoveLastEntry() {
        for (; ; ) {
            MyConcurrentSkipListMap.Node<K, V> b = findPredecessorOfLast();
            MyConcurrentSkipListMap.Node<K, V> n = b.next;
            if (n == null) {
                if (b.isBaseHeader()) {
                    return null;
                } else {
                    continue;
                }
            }
            for (; ; ) {
                MyConcurrentSkipListMap.Node<K, V> f = n.next;
                if (n != b.next) {
                    break;
                }
                Object v = n.value;
                if (v == null) {
                    n.helpDelete(b, f);
                    break;
                }
                if (b.value == null || v == n) {
                    break;
                }
                if (f != null) {
                    b = n;
                    n = f;
                    continue;
                }
                if (!n.casValue(v, null)) {
                    break;
                }
                K key = n.key;
                if (!n.appendMarker(f) || !b.casNext(n, f)) {
                    findNode(key);
                } else {
                    findPredecessor(key, comparator);
                    if (head.right == null) {
                        tryReduceLevel();
                    }
                }
                @SuppressWarnings("unchecked") V vv = (V) v;
                return new MyAbstractMap.SimpleImmutableEntry<K, V>(key, vv);
            }
        }
    }


    final MyConcurrentSkipListMap.Node<K, V> findLast() {

        MyConcurrentSkipListMap.Index<K, V> q = head;
        for (; ; ) {
            MyConcurrentSkipListMap.Index<K, V> d, r;
            if ((r = q.right) != null) {
                if (r.indexesDeletedNode()) {
                    q.unlink(r);
                    q = head;
                } else {
                    q = r;
                }
            } else if ((d = q.down) != null) {
                q = d;
            } else {
                for (MyConcurrentSkipListMap.Node<K, V> b = q.node, n = b.next; ; ) {
                    if (n == null) {
                        return b.isBaseHeader() ? null : b;
                    }
                    MyConcurrentSkipListMap.Node<K, V> f = n.next;
                    if (n != b.next) {
                        break;
                    }
                    Object v = n.value;
                    if (v == null) {
                        n.helpDelete(b, f);
                        break;
                    }
                    if (b.value == null || v == n) {
                        break;
                    }
                    b = n;
                    n = f;
                }
                q = head;
            }
        }
    }


    private MyConcurrentSkipListMap.Node<K, V> findPredecessorOfLast() {
        for (; ; ) {
            for (MyConcurrentSkipListMap.Index<K, V> q = head; ; ) {
                MyConcurrentSkipListMap.Index<K, V> d, r;
                if ((r = q.right) != null) {
                    if (r.indexesDeletedNode()) {
                        q.unlink(r);
                        break;
                    }

                    if (r.node.next != null) {
                        q = r;
                        continue;
                    }
                }
                if ((d = q.down) != null) {
                    q = d;
                } else {
                    return q.node;
                }
            }
        }
    }


    private static final int EQ = 1;
    private static final int LT = 2;
    private static final int GT = 0;


    final MyConcurrentSkipListMap.Node<K, V> findNear(K key, int rel, Comparator<? super K> cmp) {
        if (key == null) {
            throw new NullPointerException();
        }
        for (; ; ) {
            for (MyConcurrentSkipListMap.Node<K, V> b = findPredecessor(key, cmp), n = b.next; ; ) {
                Object v;
                if (n == null) {
                    return ((rel & LT) == 0 || b.isBaseHeader()) ? null : b;
                }
                Node<K, V> f = n.next;
                if (n != b.next) {
                    break;
                }
                if ((v = n.value) == null) {
                    n.helpDelete(b, f);
                    break;
                }
                if (b.value == null || v == n) {
                    break;
                }
                int c = cpr(cmp, key, n.key);
                if ((c == 0 && (rel & EQ) != 0) ||
                        (c < 0 && (rel & LT) == 0)) {
                    return n;
                }
                if (c <= 0 && (rel & LT) != 0) {
                    return b.isBaseHeader() ? null : b;
                }
                b = n;
                n = f;
            }
        }
    }


    final MyAbstractMap.SimpleImmutableEntry<K, V> getNear(K key, int rel) {
        Comparator<? super K> cmp = comparator;
        for (; ; ) {
            MyConcurrentSkipListMap.Node<K, V> n = findNear(key, rel, cmp);
            if (n == null) {
                return null;
            }
            MyAbstractMap.SimpleImmutableEntry<K, V> e = n.createSnapshot();
            if (e != null) {
                return e;
            }
        }
    }


    public MyConcurrentSkipListMap() {
        this.comparator = null;
        initialize();
    }


    public MyConcurrentSkipListMap(Comparator<? super K> comparator) {
        this.comparator = comparator;
        initialize();
    }


    public MyConcurrentSkipListMap(MyMap<? extends K, ? extends V> m) {
        this.comparator = null;
        initialize();
        putAll(m);
    }


    public MyConcurrentSkipListMap(MySortedMap<K, ? extends V> m) {
        this.comparator = m.comparator();
        initialize();
        buildFromSorted(m);
    }


    @Override
    public MyConcurrentSkipListMap<K, V> clone() {
        try {
            @SuppressWarnings("unchecked")
            MyConcurrentSkipListMap<K, V> clone =
                    (MyConcurrentSkipListMap<K, V>) super.clone();
            clone.initialize();
            clone.buildFromSorted(this);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }


    private void buildFromSorted(MySortedMap<K, ? extends V> map) {
        if (map == null) {
            throw new NullPointerException();
        }

        MyConcurrentSkipListMap.HeadIndex<K, V> h = head;
        MyConcurrentSkipListMap.Node<K, V> basepred = h.node;


        ArrayList<Index<K, V>> preds = new ArrayList<MyConcurrentSkipListMap.Index<K, V>>();


        for (int i = 0; i <= h.level; ++i)
            preds.add(null);
        MyConcurrentSkipListMap.Index<K, V> q = h;
        for (int i = h.level; i > 0; --i) {
            preds.set(i, q);
            q = q.down;
        }

        Iterator<? extends MyMap.Entry<? extends K, ? extends V>> it =
                map.entrySet().iterator();
        while (it.hasNext()) {
            MyMap.Entry<? extends K, ? extends V> e = it.next();
            int rnd = ThreadLocalRandom.current().nextInt();
            int j = 0;
            if ((rnd & 0x80000001) == 0) {
                do {
                    ++j;
                } while (((rnd >>>= 1) & 1) != 0);
                if (j > h.level) {
                    j = h.level + 1;
                }
            }
            K k = e.getKey();
            V v = e.getValue();
            if (k == null || v == null) {
                throw new NullPointerException();
            }
            MyConcurrentSkipListMap.Node<K, V> z = new MyConcurrentSkipListMap.Node<K, V>(k, v, null);
            basepred.next = z;
            basepred = z;
            if (j > 0) {
                MyConcurrentSkipListMap.Index<K, V> idx = null;
                for (int i = 1; i <= j; ++i) {
                    idx = new MyConcurrentSkipListMap.Index<K, V>(z, idx, null);
                    if (i > h.level) {
                        h = new HeadIndex<K, V>(h.node, h, idx, i);
                    }

                    if (i < preds.size()) {
                        preds.get(i).right = idx;
                        preds.set(i, idx);
                    } else {
                        preds.add(idx);
                    }
                }
            }
        }
        head = h;
    }


    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {

        s.defaultWriteObject();


        for (MyConcurrentSkipListMap.Node<K, V> n = findFirst(); n != null; n = n.next) {
            V v = n.getValidValue();
            if (v != null) {
                s.writeObject(n.key);
                s.writeObject(v);
            }
        }
        s.writeObject(null);
    }


    @SuppressWarnings("unchecked")
    private void readObject(final java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {

        s.defaultReadObject();

        initialize();


        MyConcurrentSkipListMap.HeadIndex<K, V> h = head;
        MyConcurrentSkipListMap.Node<K, V> basepred = h.node;
        ArrayList<MyConcurrentSkipListMap.Index<K, V>> preds = new ArrayList<MyConcurrentSkipListMap.Index<K, V>>();
        for (int i = 0; i <= h.level; ++i)
            preds.add(null);
        MyConcurrentSkipListMap.Index<K, V> q = h;
        for (int i = h.level; i > 0; --i) {
            preds.set(i, q);
            q = q.down;
        }

        for (; ; ) {
            Object k = s.readObject();
            if (k == null) {
                break;
            }
            Object v = s.readObject();
            if (v == null) {
                throw new NullPointerException();
            }
            K key = (K) k;
            V val = (V) v;
            int rnd = ThreadLocalRandom.current().nextInt();
            int j = 0;
            if ((rnd & 0x80000001) == 0) {
                do {
                    ++j;
                } while (((rnd >>>= 1) & 1) != 0);
                if (j > h.level) {
                    j = h.level + 1;
                }
            }
            MyConcurrentSkipListMap.Node<K, V> z = new MyConcurrentSkipListMap.Node<K, V>(key, val, null);
            basepred.next = z;
            basepred = z;
            if (j > 0) {
                MyConcurrentSkipListMap.Index<K, V> idx = null;
                for (int i = 1; i <= j; ++i) {
                    idx = new MyConcurrentSkipListMap.Index<K, V>(z, idx, null);
                    if (i > h.level)
                        h = new MyConcurrentSkipListMap.HeadIndex<K, V>(h.node, h, idx, i);

                    if (i < preds.size()) {
                        preds.get(i).right = idx;
                        preds.set(i, idx);
                    } else {
                        preds.add(idx);
                    }
                }
            }
        }
        head = h;
    }


    @Override
    public boolean containsKey(Object key) {
        return doGet(key) != null;
    }

    @Override
    public V get(Object key) {
        return doGet(key);
    }

    @Override
    public V getOrDefault(Object key, V defaultValue) {
        V v;
        return (v = doGet(key)) == null ? defaultValue : v;
    }

    @Override
    public V put(K key, V value) {
        if (value == null) {
            throw new NullPointerException();
        }
        return doPut(key, value, false);
    }

    @Override
    public V remove(Object key) {
        return doRemove(key, null);
    }

    @Override
    public boolean containsValue(Object value) {
        if (value == null) {
            throw new NullPointerException();
        }
        for (MyConcurrentSkipListMap.Node<K, V> n = findFirst(); n != null; n = n.next) {
            V v = n.getValidValue();
            if (v != null && value.equals(v)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int size() {
        long count = 0;
        for (MyConcurrentSkipListMap.Node<K, V> n = findFirst(); n != null; n = n.next) {
            if (n.getValidValue() != null) {
                ++count;
            }
        }
        return (count >= Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) count;
    }


    @Override
    public boolean isEmpty() {
        return findFirst() == null;
    }


    @Override
    public void clear() {
        for (; ; ) {
            MyConcurrentSkipListMap.Node<K, V> b, n;
            MyConcurrentSkipListMap.HeadIndex<K, V> h = head, d = (MyConcurrentSkipListMap.HeadIndex<K, V>) h.down;
            if (d != null) {
                casHead(h, d);
            } else if ((b = h.node) != null && (n = b.next) != null) {
                MyConcurrentSkipListMap.Node<K, V> f = n.next;
                if (n == b.next) {
                    Object v = n.value;
                    if (v == null) {
                        n.helpDelete(b, f);
                    } else if (n.casValue(v, null) && n.appendMarker(f)) {
                        b.casNext(n, f);
                    }
                }
            } else {
                break;
            }
        }
    }

    @Override
    public V computeIfAbsent(K key,
                             Function<? super K, ? extends V> mappingFunction) {
        if (key == null || mappingFunction == null) {
            throw new NullPointerException();
        }
        V v, p, r;
        if ((v = doGet(key)) == null &&
                (r = mappingFunction.apply(key)) != null) {
            v = (p = doPut(key, r, true)) == null ? r : p;
        }
        return v;
    }


    @Override
    public V computeIfPresent(K key,
                              BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null || remappingFunction == null) {
            throw new NullPointerException();
        }
        MyConcurrentSkipListMap.Node<K, V> n;
        Object v;
        while ((n = findNode(key)) != null) {
            if ((v = n.value) != null) {
                @SuppressWarnings("unchecked") V vv = (V) v;
                V r = remappingFunction.apply(key, vv);
                if (r != null) {
                    if (n.casValue(vv, r)) {
                        return r;
                    }
                } else if (doRemove(key, vv) != null) {
                    break;
                }
            }
        }
        return null;
    }

    @Override
    public V compute(K key,
                     BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null || remappingFunction == null) {
            throw new NullPointerException();
        }
        for (; ; ) {
            MyConcurrentSkipListMap.Node<K, V> n;
            Object v;
            V r;
            if ((n = findNode(key)) == null) {
                if ((r = remappingFunction.apply(key, null)) == null) {
                    break;
                }
                if (doPut(key, r, true) == null) {
                    return r;
                }
            } else if ((v = n.value) != null) {
                @SuppressWarnings("unchecked") V vv = (V) v;
                if ((r = remappingFunction.apply(key, vv)) != null) {
                    if (n.casValue(vv, r)) {
                        return r;
                    }
                } else if (doRemove(key, vv) != null) {
                    break;
                }
            }
        }
        return null;
    }

    @Override
    public V merge(K key, V value,
                   BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if (key == null || value == null || remappingFunction == null) {
            throw new NullPointerException();
        }
        for (; ; ) {
            MyConcurrentSkipListMap.Node<K, V> n;
            Object v;
            V r;
            if ((n = findNode(key)) == null) {
                if (doPut(key, value, true) == null) {
                    return value;
                }
            } else if ((v = n.value) != null) {
                @SuppressWarnings("unchecked") V vv = (V) v;
                if ((r = remappingFunction.apply(vv, value)) != null) {
                    if (n.casValue(vv, r)) {
                        return r;
                    }
                } else if (doRemove(key, vv) != null) {
                    return null;
                }
            }
        }
    }


    @Override
    public MyNavigableSet<K> keySet() {
        MyConcurrentSkipListMap.KeySet<K> ks = keySet;
        return (ks != null) ? ks : (keySet = new MyConcurrentSkipListMap.KeySet<K>(this));
    }

    @Override
    public MyNavigableSet<K> navigableKeySet() {
        MyConcurrentSkipListMap.KeySet<K> ks = keySet;
        return (ks != null) ? ks : (keySet = new MyConcurrentSkipListMap.KeySet<K>(this));
    }


    @Override
    public MyCollection<V> values() {
        MyConcurrentSkipListMap.Values<V> vs = values;
        return (vs != null) ? vs : (values = new MyConcurrentSkipListMap.Values<V>(this));
    }


    @Override
    public MySet<MyMap.Entry<K, V>> entrySet() {
        MyConcurrentSkipListMap.EntrySet<K, V> es = entrySet;
        return (es != null) ? es : (entrySet = new MyConcurrentSkipListMap.EntrySet<K, V>(this));
    }

    @Override
    public MyConcurrentNavigableMap<K, V> descendingMap() {
        MyConcurrentNavigableMap<K, V> dm = descendingMap;
        return (dm != null) ? dm : (descendingMap = new MyConcurrentSkipListMap.SubMap<K, V>
                (this, null, false, null, false, true));
    }

    @Override
    public MyNavigableSet<K> descendingKeySet() {
        return descendingMap().navigableKeySet();
    }


    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Map)) {
            return false;
        }
        MyMap<?, ?> m = (MyMap<?, ?>) o;
        try {
            for (MyMap.Entry<K, V> e : this.entrySet()) {
                if (!e.getValue().equals(m.get(e.getKey()))) {
                    return false;
                }
            }
            for (MyMap.Entry<?, ?> e : m.entrySet()) {
                Object k = e.getKey();
                Object v = e.getValue();
                if (k == null || v == null || !v.equals(get(k))) {
                    return false;
                }
            }
            return true;
        } catch (ClassCastException unused) {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }
    }


    @Override
    public V putIfAbsent(K key, V value) {
        if (value == null) {
            throw new NullPointerException();
        }
        return doPut(key, value, true);
    }

    @Override
    public boolean remove(Object key, Object value) {
        if (key == null) {
            throw new NullPointerException();
        }
        return value != null && doRemove(key, value) != null;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        if (key == null || oldValue == null || newValue == null) {
            throw new NullPointerException();
        }
        for (; ; ) {
            MyConcurrentSkipListMap.Node<K, V> n;
            Object v;
            if ((n = findNode(key)) == null) {
                return false;
            }
            if ((v = n.value) != null) {
                if (!oldValue.equals(v)) {
                    return false;
                }
                if (n.casValue(v, newValue)) {
                    return true;
                }
            }
        }
    }

    @Override
    public V replace(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException();
        }
        for (; ; ) {
            MyConcurrentSkipListMap.Node<K, V> n;
            Object v;
            if ((n = findNode(key)) == null) {
                return null;
            }
            if ((v = n.value) != null && n.casValue(v, value)) {
                @SuppressWarnings("unchecked") V vv = (V) v;
                return vv;
            }
        }
    }


    @Override
    public Comparator<? super K> comparator() {
        return comparator;
    }

    @Override
    public K firstKey() {
        MyConcurrentSkipListMap.Node<K, V> n = findFirst();
        if (n == null) {
            throw new NoSuchElementException();
        }
        return n.key;
    }

    @Override
    public K lastKey() {
        MyConcurrentSkipListMap.Node<K, V> n = findLast();
        if (n == null) {
            throw new NoSuchElementException();
        }
        return n.key;
    }

    @Override
    public MyConcurrentNavigableMap<K, V> subMap(K fromKey,
                                                 boolean fromInclusive,
                                                 K toKey,
                                                 boolean toInclusive) {
        if (fromKey == null || toKey == null) {
            throw new NullPointerException();
        }
        return new MyConcurrentSkipListMap.SubMap<K, V>
                (this, fromKey, fromInclusive, toKey, toInclusive, false);
    }

    @Override
    public MyConcurrentNavigableMap<K, V> headMap(K toKey,
                                                  boolean inclusive) {
        if (toKey == null) {
            throw new NullPointerException();
        }
        return new MyConcurrentSkipListMap.SubMap<K, V>
                (this, null, false, toKey, inclusive, false);
    }

    @Override
    public MyConcurrentNavigableMap<K, V> tailMap(K fromKey,
                                                  boolean inclusive) {
        if (fromKey == null) {
            throw new NullPointerException();
        }
        return new MyConcurrentSkipListMap.SubMap<K, V>
                (this, fromKey, inclusive, null, false, false);
    }

    @Override
    public MyConcurrentNavigableMap<K, V> subMap(K fromKey, K toKey) {
        return subMap(fromKey, true, toKey, false);
    }

    @Override
    public MyConcurrentNavigableMap<K, V> headMap(K toKey) {
        return headMap(toKey, false);
    }

    @Override
    public MyConcurrentNavigableMap<K, V> tailMap(K fromKey) {
        return tailMap(fromKey, true);
    }


    @Override
    public MyMap.Entry<K, V> lowerEntry(K key) {
        return getNear(key, LT);
    }

    @Override
    public K lowerKey(K key) {
        MyConcurrentSkipListMap.Node<K, V> n = findNear(key, LT, comparator);
        return (n == null) ? null : n.key;
    }

    @Override
    public MyMap.Entry<K, V> floorEntry(K key) {
        return getNear(key, LT | EQ);
    }

    @Override
    public K floorKey(K key) {
        MyConcurrentSkipListMap.Node<K, V> n = findNear(key, LT | EQ, comparator);
        return (n == null) ? null : n.key;
    }

    @Override
    public MyMap.Entry<K, V> ceilingEntry(K key) {
        return getNear(key, GT | EQ);
    }

    @Override
    public K ceilingKey(K key) {
        MyConcurrentSkipListMap.Node<K, V> n = findNear(key, GT | EQ, comparator);
        return (n == null) ? null : n.key;
    }

    @Override
    public MyMap.Entry<K, V> higherEntry(K key) {
        return getNear(key, GT);
    }

    @Override
    public K higherKey(K key) {
        MyConcurrentSkipListMap.Node<K, V> n = findNear(key, GT, comparator);
        return (n == null) ? null : n.key;
    }

    @Override
    public MyMap.Entry<K, V> firstEntry() {
        for (; ; ) {
            MyConcurrentSkipListMap.Node<K, V> n = findFirst();
            if (n == null) {
                return null;
            }
            MyAbstractMap.SimpleImmutableEntry<K, V> e = n.createSnapshot();
            if (e != null) {
                return e;
            }
        }
    }

    @Override
    public MyMap.Entry<K, V> lastEntry() {
        for (; ; ) {
            MyConcurrentSkipListMap.Node<K, V> n = findLast();
            if (n == null) {
                return null;
            }
            MyAbstractMap.SimpleImmutableEntry<K, V> e = n.createSnapshot();
            if (e != null) {
                return e;
            }
        }
    }

    @Override
    public MyMap.Entry<K, V> pollFirstEntry() {
        return doRemoveFirstEntry();
    }

    @Override
    public MyMap.Entry<K, V> pollLastEntry() {
        return doRemoveLastEntry();
    }


    abstract class Iter<T> implements Iterator<T> {

        MyConcurrentSkipListMap.Node<K, V> lastReturned;

        MyConcurrentSkipListMap.Node<K, V> next;

        V nextValue;

        Iter() {
            while ((next = findFirst()) != null) {
                Object x = next.value;
                if (x != null && x != next) {
                    @SuppressWarnings("unchecked") V vv = (V) x;
                    nextValue = vv;
                    break;
                }
            }
        }

        @Override
        public final boolean hasNext() {
            return next != null;
        }


        final void advance() {
            if (next == null) {
                throw new NoSuchElementException();
            }
            lastReturned = next;
            while ((next = next.next) != null) {
                Object x = next.value;
                if (x != null && x != next) {
                    @SuppressWarnings("unchecked") V vv = (V) x;
                    nextValue = vv;
                    break;
                }
            }
        }

        @Override
        public void remove() {
            MyConcurrentSkipListMap.Node<K, V> l = lastReturned;
            if (l == null) {
                throw new IllegalStateException();
            }


            MyConcurrentSkipListMap.this.remove(l.key);
            lastReturned = null;
        }

    }

    final class ValueIterator extends Iter<V> {
        @Override
        public V next() {
            V v = nextValue;
            advance();
            return v;
        }
    }

    final class KeyIterator extends Iter<K> {
        @Override
        public K next() {
            MyConcurrentSkipListMap.Node<K, V> n = next;
            advance();
            return n.key;
        }
    }

    final class EntryIterator extends Iter<Entry<K, V>> {
        @Override
        public MyMap.Entry<K, V> next() {
            MyConcurrentSkipListMap.Node<K, V> n = next;
            V v = nextValue;
            advance();
            return new MyAbstractMap.SimpleImmutableEntry<K, V>(n.key, v);
        }
    }


    Iterator<K> keyIterator() {
        return new MyConcurrentSkipListMap.KeyIterator();
    }

    Iterator<V> valueIterator() {
        return new MyConcurrentSkipListMap.ValueIterator();
    }

    Iterator<MyMap.Entry<K, V>> entryIterator() {
        return new MyConcurrentSkipListMap.EntryIterator();
    }


    static final <E> MyList<E> toList(MyCollection<E> c) {

        MyArrayList<E> list = new MyArrayList<E>();
        for (E e : c) {
            list.add(e);
        }
        return list;
    }

    static final class KeySet<E>
            extends MyAbstractSet<E> implements MyNavigableSet<E> {
        final MyConcurrentNavigableMap<E, ?> m;

        KeySet(MyConcurrentNavigableMap<E, ?> map) {
            m = map;
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
        public boolean remove(Object o) {
            return m.remove(o) != null;
        }

        @Override
        public void clear() {
            m.clear();
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
        public E pollFirst() {
            MyMap.Entry<E, ?> e = m.pollFirstEntry();
            return (e == null) ? null : e.getKey();
        }

        @Override
        public E pollLast() {
            MyMap.Entry<E, ?> e = m.pollLastEntry();
            return (e == null) ? null : e.getKey();
        }

        @Override
        @SuppressWarnings("unchecked")
        public Iterator<E> iterator() {
            if (m instanceof MyConcurrentSkipListMap) {
                return ((MyConcurrentSkipListMap<E, Object>) m).keyIterator();
            } else {
                return ((SubMap<E, Object>) m).keyIterator();
            }
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
        public Object[] toArray() {
            return toList(this).toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return toList(this).toArray(a);
        }

        @Override
        public Iterator<E> descendingIterator() {
            return descendingSet().iterator();
        }

        @Override
        public MyNavigableSet<E> subSet(E fromElement,
                                        boolean fromInclusive,
                                        E toElement,
                                        boolean toInclusive) {
            return new MyConcurrentSkipListMap.KeySet<E>(m.subMap(fromElement, fromInclusive,
                    toElement, toInclusive));
        }

        @Override
        public MyNavigableSet<E> headSet(E toElement, boolean inclusive) {
            return new MyConcurrentSkipListMap.KeySet<E>(m.headMap(toElement, inclusive));
        }

        @Override
        public MyNavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return new MyConcurrentSkipListMap.KeySet<E>(m.tailMap(fromElement, inclusive));
        }

        @Override
        public MyNavigableSet<E> subSet(E fromElement, E toElement) {
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
            return new MyConcurrentSkipListMap.KeySet<E>(m.descendingMap());
        }

        @SuppressWarnings("unchecked")
        @Override
        public Spliterator<E> spliterator() {
            if (m instanceof MyConcurrentSkipListMap) {
                return ((MyConcurrentSkipListMap<E, ?>) m).keySpliterator();
            } else {
                return (Spliterator<E>) ((SubMap<E, ?>) m).keyIterator();
            }
        }
    }

    static final class Values<E> extends MyAbstractCollection<E> {
        final MyConcurrentNavigableMap<?, E> m;

        Values(MyConcurrentNavigableMap<?, E> map) {
            m = map;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Iterator<E> iterator() {
            if (m instanceof MyConcurrentSkipListMap) {
                return ((MyConcurrentSkipListMap<?, E>) m).valueIterator();
            } else {
                return ((SubMap<?, E>) m).valueIterator();
            }
        }

        @Override
        public boolean isEmpty() {
            return m.isEmpty();
        }

        @Override
        public int size() {
            return m.size();
        }

        @Override
        public boolean contains(Object o) {
            return m.containsValue(o);
        }

        @Override
        public void clear() {
            m.clear();
        }

        @Override
        public Object[] toArray() {
            return toList(this).toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return toList(this).toArray(a);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Spliterator<E> spliterator() {
            if (m instanceof MyConcurrentSkipListMap) {
                return ((MyConcurrentSkipListMap<?, E>) m).valueSpliterator();
            } else {
                return (Spliterator<E>) ((SubMap<?, E>) m).valueIterator();
            }
        }
    }

    static final class EntrySet<K1, V1> extends MyAbstractSet<MyMap.Entry<K1, V1>> {
        final MyConcurrentNavigableMap<K1, V1> m;

        EntrySet(MyConcurrentNavigableMap<K1, V1> map) {
            m = map;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Iterator<MyMap.Entry<K1, V1>> iterator() {
            if (m instanceof MyConcurrentSkipListMap) {
                return ((MyConcurrentSkipListMap<K1, V1>) m).entryIterator();
            } else {
                return ((SubMap<K1, V1>) m).entryIterator();
            }
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            V1 v = m.get(e.getKey());
            return v != null && v.equals(e.getValue());
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            return m.remove(e.getKey(),
                    e.getValue());
        }

        @Override
        public boolean isEmpty() {
            return m.isEmpty();
        }

        @Override
        public int size() {
            return m.size();
        }

        @Override
        public void clear() {
            m.clear();
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
        public Object[] toArray() {
            return toList(this).toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return toList(this).toArray(a);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Spliterator<MyMap.Entry<K1, V1>> spliterator() {
            if (m instanceof MyConcurrentSkipListMap) {
                return ((MyConcurrentSkipListMap<K1, V1>) m).entrySpliterator();
            } else {
                return (Spliterator<Entry<K1, V1>>)
                        ((SubMap<K1, V1>) m).entryIterator();
            }
        }
    }

    static final class SubMap<K, V> extends MyAbstractMap<K, V>
            implements MyConcurrentNavigableMap<K, V>, Cloneable, Serializable {
        private static final long serialVersionUID = -7647078645895051609L;


        private final MyConcurrentSkipListMap<K, V> m;

        private final K lo;

        private final K hi;

        private final boolean loInclusive;

        private final boolean hiInclusive;

        private final boolean isDescending;


        private transient MyConcurrentSkipListMap.KeySet<K> keySetView;
        private transient MySet<MyMap.Entry<K, V>> entrySetView;
        private transient MyCollection<V> valuesView;


        SubMap(MyConcurrentSkipListMap<K, V> map,
               K fromKey, boolean fromInclusive,
               K toKey, boolean toInclusive,
               boolean isDescending) {
            Comparator<? super K> cmp = map.comparator;
            if (fromKey != null && toKey != null &&
                    cpr(cmp, fromKey, toKey) > 0) {
                throw new IllegalArgumentException("inconsistent range");
            }
            this.m = map;
            this.lo = fromKey;
            this.hi = toKey;
            this.loInclusive = fromInclusive;
            this.hiInclusive = toInclusive;
            this.isDescending = isDescending;
        }


        boolean tooLow(Object key, Comparator<? super K> cmp) {
            int c;
            return (lo != null && ((c = cpr(cmp, key, lo)) < 0 ||
                    (c == 0 && !loInclusive)));
        }

        boolean tooHigh(Object key, Comparator<? super K> cmp) {
            int c;
            return (hi != null && ((c = cpr(cmp, key, hi)) > 0 ||
                    (c == 0 && !hiInclusive)));
        }

        boolean inBounds(Object key, Comparator<? super K> cmp) {
            return !tooLow(key, cmp) && !tooHigh(key, cmp);
        }

        void checkKeyBounds(K key, Comparator<? super K> cmp) {
            if (key == null)
                throw new NullPointerException();
            if (!inBounds(key, cmp))
                throw new IllegalArgumentException("key out of range");
        }


        boolean isBeforeEnd(MyConcurrentSkipListMap.Node<K, V> n,
                            Comparator<? super K> cmp) {
            if (n == null) {
                return false;
            }
            if (hi == null) {
                return true;
            }
            K k = n.key;
            if (k == null) {
                return true;
            }
            int c = cpr(cmp, k, hi);
            if (c > 0 || (c == 0 && !hiInclusive)) {
                return false;
            }
            return true;
        }


        MyConcurrentSkipListMap.Node<K, V> loNode(Comparator<? super K> cmp) {
            if (lo == null) {
                return m.findFirst();
            } else if (loInclusive) {
                return m.findNear(lo, GT | EQ, cmp);
            } else {
                return m.findNear(lo, GT, cmp);
            }
        }


        MyConcurrentSkipListMap.Node<K, V> hiNode(Comparator<? super K> cmp) {
            if (hi == null) {
                return m.findLast();
            } else if (hiInclusive) {
                return m.findNear(hi, LT | EQ, cmp);
            } else {
                return m.findNear(hi, LT, cmp);
            }
        }


        K lowestKey() {
            Comparator<? super K> cmp = m.comparator;
            MyConcurrentSkipListMap.Node<K, V> n = loNode(cmp);
            if (isBeforeEnd(n, cmp)) {
                return n.key;
            } else {
                throw new NoSuchElementException();
            }
        }


        K highestKey() {
            Comparator<? super K> cmp = m.comparator;
            MyConcurrentSkipListMap.Node<K, V> n = hiNode(cmp);
            if (n != null) {
                K last = n.key;
                if (inBounds(last, cmp)) {
                    return last;
                }
            }
            throw new NoSuchElementException();
        }

        MyMap.Entry<K, V> lowestEntry() {
            Comparator<? super K> cmp = m.comparator;
            for (; ; ) {
                MyConcurrentSkipListMap.Node<K, V> n = loNode(cmp);
                if (!isBeforeEnd(n, cmp)) {
                    return null;
                }
                MyMap.Entry<K, V> e = n.createSnapshot();
                if (e != null) {
                    return e;
                }
            }
        }

        MyMap.Entry<K, V> highestEntry() {
            Comparator<? super K> cmp = m.comparator;
            for (; ; ) {
                MyConcurrentSkipListMap.Node<K, V> n = hiNode(cmp);
                if (n == null || !inBounds(n.key, cmp)) {
                    return null;
                }
                MyMap.Entry<K, V> e = n.createSnapshot();
                if (e != null) {
                    return e;
                }
            }
        }

        MyMap.Entry<K, V> removeLowest() {
            Comparator<? super K> cmp = m.comparator;
            for (; ; ) {
                MyConcurrentSkipListMap.Node<K, V> n = loNode(cmp);
                if (n == null)
                    return null;
                K k = n.key;
                if (!inBounds(k, cmp)) {
                    return null;
                }
                V v = m.doRemove(k, null);
                if (v != null) {
                    return new SimpleImmutableEntry<K, V>(k, v);
                }
            }
        }

        MyMap.Entry<K, V> removeHighest() {
            Comparator<? super K> cmp = m.comparator;
            for (; ; ) {
                MyConcurrentSkipListMap.Node<K, V> n = hiNode(cmp);
                if (n == null) {
                    return null;
                }
                K k = n.key;
                if (!inBounds(k, cmp)) {
                    return null;
                }
                V v = m.doRemove(k, null);
                if (v != null) {
                    return new SimpleImmutableEntry<K, V>(k, v);
                }
            }
        }


        MyMap.Entry<K, V> getNearEntry(K key, int rel) {
            Comparator<? super K> cmp = m.comparator;
            if (isDescending) {
                if ((rel & LT) == 0) {
                    rel |= LT;
                } else {
                    rel &= ~LT;
                }
            }
            if (tooLow(key, cmp)) {
                return ((rel & LT) != 0) ? null : lowestEntry();
            }
            if (tooHigh(key, cmp)) {
                return ((rel & LT) != 0) ? highestEntry() : null;
            }
            for (; ; ) {
                MyConcurrentSkipListMap.Node<K, V> n = m.findNear(key, rel, cmp);
                if (n == null || !inBounds(n.key, cmp)) {
                    return null;
                }
                K k = n.key;
                V v = n.getValidValue();
                if (v != null) {
                    return new MyAbstractMap.SimpleImmutableEntry<K, V>(k, v);
                }
            }
        }


        K getNearKey(K key, int rel) {
            Comparator<? super K> cmp = m.comparator;
            if (isDescending) {
                if ((rel & LT) == 0) {
                    rel |= LT;
                } else {
                    rel &= ~LT;
                }
            }
            if (tooLow(key, cmp)) {
                if ((rel & LT) == 0) {
                    MyConcurrentSkipListMap.Node<K, V> n = loNode(cmp);
                    if (isBeforeEnd(n, cmp)) {
                        return n.key;
                    }
                }
                return null;
            }
            if (tooHigh(key, cmp)) {
                if ((rel & LT) != 0) {
                    MyConcurrentSkipListMap.Node<K, V> n = hiNode(cmp);
                    if (n != null) {
                        K last = n.key;
                        if (inBounds(last, cmp))
                            return last;
                    }
                }
                return null;
            }
            for (; ; ) {
                MyConcurrentSkipListMap.Node<K, V> n = m.findNear(key, rel, cmp);
                if (n == null || !inBounds(n.key, cmp)) {
                    return null;
                }
                K k = n.key;
                V v = n.getValidValue();
                if (v != null) {
                    return k;
                }
            }
        }


        @Override
        public boolean containsKey(Object key) {
            if (key == null) {
                throw new NullPointerException();
            }
            return inBounds(key, m.comparator) && m.containsKey(key);
        }

        @Override
        public V get(Object key) {
            if (key == null) {
                throw new NullPointerException();
            }
            return (!inBounds(key, m.comparator)) ? null : m.get(key);
        }

        @Override
        public V put(K key, V value) {
            checkKeyBounds(key, m.comparator);
            return m.put(key, value);
        }

        @Override
        public V remove(Object key) {
            return (!inBounds(key, m.comparator)) ? null : m.remove(key);
        }

        @Override
        public int size() {
            Comparator<? super K> cmp = m.comparator;
            long count = 0;
            for (MyConcurrentSkipListMap.Node<K, V> n = loNode(cmp);
                 isBeforeEnd(n, cmp);
                 n = n.next) {
                if (n.getValidValue() != null) {
                    ++count;
                }
            }
            return count >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
        }

        @Override
        public boolean isEmpty() {
            Comparator<? super K> cmp = m.comparator;
            return !isBeforeEnd(loNode(cmp), cmp);
        }

        @Override
        public boolean containsValue(Object value) {
            if (value == null)
                throw new NullPointerException();
            Comparator<? super K> cmp = m.comparator;
            for (MyConcurrentSkipListMap.Node<K, V> n = loNode(cmp);
                 isBeforeEnd(n, cmp);
                 n = n.next) {
                V v = n.getValidValue();
                if (v != null && value.equals(v)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void clear() {
            Comparator<? super K> cmp = m.comparator;
            for (MyConcurrentSkipListMap.Node<K, V> n = loNode(cmp);
                 isBeforeEnd(n, cmp);
                 n = n.next) {
                if (n.getValidValue() != null) {
                    m.remove(n.key);
                }
            }
        }


        @Override
        public V putIfAbsent(K key, V value) {
            checkKeyBounds(key, m.comparator);
            return m.putIfAbsent(key, value);
        }

        @Override
        public boolean remove(Object key, Object value) {
            return inBounds(key, m.comparator) && m.remove(key, value);
        }

        @Override
        public boolean replace(K key, V oldValue, V newValue) {
            checkKeyBounds(key, m.comparator);
            return m.replace(key, oldValue, newValue);
        }

        @Override
        public V replace(K key, V value) {
            checkKeyBounds(key, m.comparator);
            return m.replace(key, value);
        }


        @Override
        public Comparator<? super K> comparator() {
            Comparator<? super K> cmp = m.comparator();
            if (isDescending) {
                return MyCollections.reverseOrder(cmp);
            } else {
                return cmp;
            }
        }


        MyConcurrentSkipListMap.SubMap<K, V> newSubMap(K fromKey, boolean fromInclusive,
                                                       K toKey, boolean toInclusive) {
            Comparator<? super K> cmp = m.comparator;
            if (isDescending) {
                K tk = fromKey;
                fromKey = toKey;
                toKey = tk;
                boolean ti = fromInclusive;
                fromInclusive = toInclusive;
                toInclusive = ti;
            }
            if (lo != null) {
                if (fromKey == null) {
                    fromKey = lo;
                    fromInclusive = loInclusive;
                } else {
                    int c = cpr(cmp, fromKey, lo);
                    if (c < 0 || (c == 0 && !loInclusive && fromInclusive)) {
                        throw new IllegalArgumentException("key out of range");
                    }
                }
            }
            if (hi != null) {
                if (toKey == null) {
                    toKey = hi;
                    toInclusive = hiInclusive;
                } else {
                    int c = cpr(cmp, toKey, hi);
                    if (c > 0 || (c == 0 && !hiInclusive && toInclusive)) {
                        throw new IllegalArgumentException("key out of range");
                    }
                }
            }
            return new MyConcurrentSkipListMap.SubMap<K, V>(m, fromKey, fromInclusive,
                    toKey, toInclusive, isDescending);
        }

        @Override
        public MyConcurrentSkipListMap.SubMap<K, V> subMap(K fromKey, boolean fromInclusive,
                                                           K toKey, boolean toInclusive) {
            if (fromKey == null || toKey == null) {
                throw new NullPointerException();
            }
            return newSubMap(fromKey, fromInclusive, toKey, toInclusive);
        }

        @Override
        public MyConcurrentSkipListMap.SubMap<K, V> headMap(K toKey, boolean inclusive) {
            if (toKey == null) {
                throw new NullPointerException();
            }
            return newSubMap(null, false, toKey, inclusive);
        }

        @Override
        public MyConcurrentSkipListMap.SubMap<K, V> tailMap(K fromKey, boolean inclusive) {
            if (fromKey == null) {
                throw new NullPointerException();
            }
            return newSubMap(fromKey, inclusive, null, false);
        }

        @Override
        public MyConcurrentSkipListMap.SubMap<K, V> subMap(K fromKey, K toKey) {
            return subMap(fromKey, true, toKey, false);
        }

        @Override
        public MyConcurrentSkipListMap.SubMap<K, V> headMap(K toKey) {
            return headMap(toKey, false);
        }

        @Override
        public MyConcurrentSkipListMap.SubMap<K, V> tailMap(K fromKey) {
            return tailMap(fromKey, true);
        }

        @Override
        public MyConcurrentSkipListMap.SubMap<K, V> descendingMap() {
            return new MyConcurrentSkipListMap.SubMap<K, V>(m, lo, loInclusive,
                    hi, hiInclusive, !isDescending);
        }


        @Override
        public MyMap.Entry<K, V> ceilingEntry(K key) {
            return getNearEntry(key, GT | EQ);
        }

        @Override
        public K ceilingKey(K key) {
            return getNearKey(key, GT | EQ);
        }

        @Override
        public MyMap.Entry<K, V> lowerEntry(K key) {
            return getNearEntry(key, LT);
        }

        @Override
        public K lowerKey(K key) {
            return getNearKey(key, LT);
        }

        @Override
        public MyMap.Entry<K, V> floorEntry(K key) {
            return getNearEntry(key, LT | EQ);
        }

        @Override
        public K floorKey(K key) {
            return getNearKey(key, LT | EQ);
        }

        @Override
        public MyMap.Entry<K, V> higherEntry(K key) {
            return getNearEntry(key, GT);
        }

        @Override
        public K higherKey(K key) {
            return getNearKey(key, GT);
        }

        @Override
        public K firstKey() {
            return isDescending ? highestKey() : lowestKey();
        }

        @Override
        public K lastKey() {
            return isDescending ? lowestKey() : highestKey();
        }

        @Override
        public MyMap.Entry<K, V> firstEntry() {
            return isDescending ? highestEntry() : lowestEntry();
        }

        @Override
        public MyMap.Entry<K, V> lastEntry() {
            return isDescending ? lowestEntry() : highestEntry();
        }

        @Override
        public MyMap.Entry<K, V> pollFirstEntry() {
            return isDescending ? removeHighest() : removeLowest();
        }

        @Override
        public MyMap.Entry<K, V> pollLastEntry() {
            return isDescending ? removeLowest() : removeHighest();
        }


        @Override
        public MyNavigableSet<K> keySet() {
            MyConcurrentSkipListMap.KeySet<K> ks = keySetView;
            return (ks != null) ? ks : (keySetView = new MyConcurrentSkipListMap.KeySet<K>(this));
        }

        @Override
        public MyNavigableSet<K> navigableKeySet() {
            MyConcurrentSkipListMap.KeySet<K> ks = keySetView;
            return (ks != null) ? ks : (keySetView = new MyConcurrentSkipListMap.KeySet<K>(this));
        }

        @Override
        public MyCollection<V> values() {
            MyCollection<V> vs = valuesView;
            return (vs != null) ? vs : (valuesView = new MyConcurrentSkipListMap.Values<V>(this));
        }

        @Override
        public MySet<MyMap.Entry<K, V>> entrySet() {
            MySet<MyMap.Entry<K, V>> es = entrySetView;
            return (es != null) ? es : (entrySetView = new MyConcurrentSkipListMap.EntrySet<K, V>(this));
        }

        @Override
        public MyNavigableSet<K> descendingKeySet() {
            return descendingMap().navigableKeySet();
        }

        Iterator<K> keyIterator() {
            return new MyConcurrentSkipListMap.SubMap.SubMapKeyIterator();
        }

        Iterator<V> valueIterator() {
            return new MyConcurrentSkipListMap.SubMap.SubMapValueIterator();
        }

        Iterator<MyMap.Entry<K, V>> entryIterator() {
            return new MyConcurrentSkipListMap.SubMap.SubMapEntryIterator();
        }


        abstract class SubMapIter<T> implements Iterator<T>, Spliterator<T> {

            MyConcurrentSkipListMap.Node<K, V> lastReturned;

            MyConcurrentSkipListMap.Node<K, V> next;

            V nextValue;

            SubMapIter() {
                Comparator<? super K> cmp = m.comparator;
                for (; ; ) {
                    next = isDescending ? hiNode(cmp) : loNode(cmp);
                    if (next == null) {
                        break;
                    }
                    Object x = next.value;
                    if (x != null && x != next) {
                        if (!inBounds(next.key, cmp)) {
                            next = null;
                        } else {
                            @SuppressWarnings("unchecked") V vv = (V) x;
                            nextValue = vv;
                        }
                        break;
                    }
                }
            }

            @Override
            public final boolean hasNext() {
                return next != null;
            }

            final void advance() {
                if (next == null) {
                    throw new NoSuchElementException();
                }
                lastReturned = next;
                if (isDescending) {
                    descend();
                } else {
                    ascend();
                }
            }

            private void ascend() {
                Comparator<? super K> cmp = m.comparator;
                for (; ; ) {
                    next = next.next;
                    if (next == null) {
                        break;
                    }
                    Object x = next.value;
                    if (x != null && x != next) {
                        if (tooHigh(next.key, cmp)) {
                            next = null;
                        } else {
                            @SuppressWarnings("unchecked") V vv = (V) x;
                            nextValue = vv;
                        }
                        break;
                    }
                }
            }

            private void descend() {
                Comparator<? super K> cmp = m.comparator;
                for (; ; ) {
                    next = m.findNear(lastReturned.key, LT, cmp);
                    if (next == null) {
                        break;
                    }
                    Object x = next.value;
                    if (x != null && x != next) {
                        if (tooLow(next.key, cmp)) {
                            next = null;
                        } else {
                            @SuppressWarnings("unchecked") V vv = (V) x;
                            nextValue = vv;
                        }
                        break;
                    }
                }
            }

            @Override
            public void remove() {
                MyConcurrentSkipListMap.Node<K, V> l = lastReturned;
                if (l == null) {
                    throw new IllegalStateException();
                }
                m.remove(l.key);
                lastReturned = null;
            }

            @Override
            public Spliterator<T> trySplit() {
                return null;
            }

            @Override
            public boolean tryAdvance(Consumer<? super T> action) {
                if (hasNext()) {
                    action.accept(next());
                    return true;
                }
                return false;
            }

            @Override
            public void forEachRemaining(Consumer<? super T> action) {
                while (hasNext()) {
                    action.accept(next());
                }
            }

            @Override
            public long estimateSize() {
                return Long.MAX_VALUE;
            }

        }

        final class SubMapValueIterator extends SubMapIter<V> {
            @Override
            public V next() {
                V v = nextValue;
                advance();
                return v;
            }

            @Override
            public int characteristics() {
                return 0;
            }
        }

        final class SubMapKeyIterator extends SubMapIter<K> {
            @Override
            public K next() {
                MyConcurrentSkipListMap.Node<K, V> n = next;
                advance();
                return n.key;
            }

            @Override
            public int characteristics() {
                return Spliterator.DISTINCT | Spliterator.ORDERED |
                        Spliterator.SORTED;
            }

            @Override
            public final Comparator<? super K> getComparator() {
                return MyConcurrentSkipListMap.SubMap.this.comparator();
            }
        }

        final class SubMapEntryIterator extends SubMapIter<Entry<K, V>> {

            @Override
            public MyMap.Entry<K, V> next() {
                Node<K, V> n = next;
                V v = nextValue;
                advance();
                return new MyAbstractMap.SimpleImmutableEntry<K, V>(n.key, v);
            }

            @Override
            public int characteristics() {
                return Spliterator.DISTINCT;
            }
        }
    }


    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        V v;
        for (MyConcurrentSkipListMap.Node<K, V> n = findFirst(); n != null; n = n.next) {
            if ((v = n.getValidValue()) != null) {
                action.accept(n.key, v);
            }
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        if (function == null) {
            throw new NullPointerException();
        }
        V v;
        for (MyConcurrentSkipListMap.Node<K, V> n = findFirst(); n != null; n = n.next) {
            while ((v = n.getValidValue()) != null) {
                V r = function.apply(n.key, v);
                if (r == null) {
                    throw new NullPointerException();
                }
                if (n.casValue(v, r)) {
                    break;
                }
            }
        }
    }


    abstract static class CSLMSpliterator<K, V> {
        final Comparator<? super K> comparator;
        final K fence;
        MyConcurrentSkipListMap.Index<K, V> row;
        MyConcurrentSkipListMap.Node<K, V> current;
        int est;

        CSLMSpliterator(Comparator<? super K> comparator, MyConcurrentSkipListMap.Index<K, V> row,
                        MyConcurrentSkipListMap.Node<K, V> origin, K fence, int est) {
            this.comparator = comparator;
            this.row = row;
            this.current = origin;
            this.fence = fence;
            this.est = est;
        }

        public final long estimateSize() {
            return (long) est;
        }
    }

    static final class KeySpliterator<K, V> extends MyConcurrentSkipListMap.CSLMSpliterator<K, V>
            implements Spliterator<K> {
        KeySpliterator(Comparator<? super K> comparator, MyConcurrentSkipListMap.Index<K, V> row,
                       MyConcurrentSkipListMap.Node<K, V> origin, K fence, int est) {
            super(comparator, row, origin, fence, est);
        }

        public Spliterator<K> trySplit() {
            MyConcurrentSkipListMap.Node<K, V> e;
            K ek;
            Comparator<? super K> cmp = comparator;
            K f = fence;
            if ((e = current) != null && (ek = e.key) != null) {
                for (MyConcurrentSkipListMap.Index<K, V> q = row; q != null; q = row = q.down) {
                    MyConcurrentSkipListMap.Index<K, V> s;
                    MyConcurrentSkipListMap.Node<K, V> b, n;
                    K sk;
                    if ((s = q.right) != null && (b = s.node) != null &&
                            (n = b.next) != null && n.value != null &&
                            (sk = n.key) != null && cpr(cmp, sk, ek) > 0 &&
                            (f == null || cpr(cmp, sk, f) < 0)) {
                        current = n;
                        MyConcurrentSkipListMap.Index<K, V> r = q.down;
                        row = (s.right != null) ? s : s.down;
                        est -= est >>> 2;
                        return new MyConcurrentSkipListMap.KeySpliterator<K, V>(cmp, r, e, sk, est);
                    }
                }
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super K> action) {
            if (action == null) throw new NullPointerException();
            Comparator<? super K> cmp = comparator;
            K f = fence;
            MyConcurrentSkipListMap.Node<K, V> e = current;
            current = null;
            for (; e != null; e = e.next) {
                K k;
                Object v;
                if ((k = e.key) != null && f != null && cpr(cmp, f, k) <= 0)
                    break;
                if ((v = e.value) != null && v != e)
                    action.accept(k);
            }
        }

        public boolean tryAdvance(Consumer<? super K> action) {
            if (action == null) throw new NullPointerException();
            Comparator<? super K> cmp = comparator;
            K f = fence;
            MyConcurrentSkipListMap.Node<K, V> e = current;
            for (; e != null; e = e.next) {
                K k;
                Object v;
                if ((k = e.key) != null && f != null && cpr(cmp, f, k) <= 0) {
                    e = null;
                    break;
                }
                if ((v = e.value) != null && v != e) {
                    current = e.next;
                    action.accept(k);
                    return true;
                }
            }
            current = e;
            return false;
        }

        public int characteristics() {
            return Spliterator.DISTINCT | Spliterator.SORTED |
                    Spliterator.ORDERED | Spliterator.CONCURRENT |
                    Spliterator.NONNULL;
        }

        public final Comparator<? super K> getComparator() {
            return comparator;
        }
    }

    final MyConcurrentSkipListMap.KeySpliterator<K, V> keySpliterator() {
        Comparator<? super K> cmp = comparator;
        for (; ; ) {
            MyConcurrentSkipListMap.HeadIndex<K, V> h;
            MyConcurrentSkipListMap.Node<K, V> p;
            MyConcurrentSkipListMap.Node<K, V> b = (h = head).node;
            if ((p = b.next) == null || p.value != null)
                return new MyConcurrentSkipListMap.KeySpliterator<K, V>(cmp, h, p, null, (p == null) ?
                        0 : Integer.MAX_VALUE);
            p.helpDelete(b, p.next);
        }
    }

    static final class ValueSpliterator<K, V> extends MyConcurrentSkipListMap.CSLMSpliterator<K, V>
            implements Spliterator<V> {
        ValueSpliterator(Comparator<? super K> comparator, MyConcurrentSkipListMap.Index<K, V> row,
                         MyConcurrentSkipListMap.Node<K, V> origin, K fence, int est) {
            super(comparator, row, origin, fence, est);
        }

        public Spliterator<V> trySplit() {
            MyConcurrentSkipListMap.Node<K, V> e;
            K ek;
            Comparator<? super K> cmp = comparator;
            K f = fence;
            if ((e = current) != null && (ek = e.key) != null) {
                for (MyConcurrentSkipListMap.Index<K, V> q = row; q != null; q = row = q.down) {
                    MyConcurrentSkipListMap.Index<K, V> s;
                    MyConcurrentSkipListMap.Node<K, V> b, n;
                    K sk;
                    if ((s = q.right) != null && (b = s.node) != null &&
                            (n = b.next) != null && n.value != null &&
                            (sk = n.key) != null && cpr(cmp, sk, ek) > 0 &&
                            (f == null || cpr(cmp, sk, f) < 0)) {
                        current = n;
                        MyConcurrentSkipListMap.Index<K, V> r = q.down;
                        row = (s.right != null) ? s : s.down;
                        est -= est >>> 2;
                        return new MyConcurrentSkipListMap.ValueSpliterator<K, V>(cmp, r, e, sk, est);
                    }
                }
            }
            return null;
        }

        public void forEachRemaining(Consumer<? super V> action) {
            if (action == null) throw new NullPointerException();
            Comparator<? super K> cmp = comparator;
            K f = fence;
            MyConcurrentSkipListMap.Node<K, V> e = current;
            current = null;
            for (; e != null; e = e.next) {
                K k;
                Object v;
                if ((k = e.key) != null && f != null && cpr(cmp, f, k) <= 0)
                    break;
                if ((v = e.value) != null && v != e) {
                    @SuppressWarnings("unchecked") V vv = (V) v;
                    action.accept(vv);
                }
            }
        }

        public boolean tryAdvance(Consumer<? super V> action) {
            if (action == null) throw new NullPointerException();
            Comparator<? super K> cmp = comparator;
            K f = fence;
            MyConcurrentSkipListMap.Node<K, V> e = current;
            for (; e != null; e = e.next) {
                K k;
                Object v;
                if ((k = e.key) != null && f != null && cpr(cmp, f, k) <= 0) {
                    e = null;
                    break;
                }
                if ((v = e.value) != null && v != e) {
                    current = e.next;
                    @SuppressWarnings("unchecked") V vv = (V) v;
                    action.accept(vv);
                    return true;
                }
            }
            current = e;
            return false;
        }

        public int characteristics() {
            return Spliterator.CONCURRENT | Spliterator.ORDERED |
                    Spliterator.NONNULL;
        }
    }


    final MyConcurrentSkipListMap.ValueSpliterator<K, V> valueSpliterator() {
        Comparator<? super K> cmp = comparator;
        for (; ; ) {
            MyConcurrentSkipListMap.HeadIndex<K, V> h;
            MyConcurrentSkipListMap.Node<K, V> p;
            MyConcurrentSkipListMap.Node<K, V> b = (h = head).node;
            if ((p = b.next) == null || p.value != null)
                return new MyConcurrentSkipListMap.ValueSpliterator<K, V>(cmp, h, p, null, (p == null) ?
                        0 : Integer.MAX_VALUE);
            p.helpDelete(b, p.next);
        }
    }

    static final class EntrySpliterator<K, V> extends MyConcurrentSkipListMap.CSLMSpliterator<K, V>
            implements Spliterator<MyMap.Entry<K, V>> {
        EntrySpliterator(Comparator<? super K> comparator, MyConcurrentSkipListMap.Index<K, V> row,
                         MyConcurrentSkipListMap.Node<K, V> origin, K fence, int est) {
            super(comparator, row, origin, fence, est);
        }

        @Override
        public Spliterator< MyMap.Entry<K, V>> trySplit() {
            MyConcurrentSkipListMap.Node<K, V> e;
            K ek;
            Comparator<? super K> cmp = comparator;
            K f = fence;
            if ((e = current) != null && (ek = e.key) != null) {
                for (MyConcurrentSkipListMap.Index<K, V> q = row; q != null; q = row = q.down) {
                    MyConcurrentSkipListMap.Index<K, V> s;
                    MyConcurrentSkipListMap.Node<K, V> b, n;
                    K sk;
                    if ((s = q.right) != null && (b = s.node) != null &&
                            (n = b.next) != null && n.value != null &&
                            (sk = n.key) != null && cpr(cmp, sk, ek) > 0 &&
                            (f == null || cpr(cmp, sk, f) < 0)) {
                        current = n;
                        MyConcurrentSkipListMap.Index<K, V> r = q.down;
                        row = (s.right != null) ? s : s.down;
                        est -= est >>> 2;
                        return new MyConcurrentSkipListMap.EntrySpliterator<K, V>(cmp, r, e, sk, est);
                    }
                }
            }
            return null;
        }

        @Override
        public void forEachRemaining(Consumer<? super  MyMap.Entry<K, V>> action) {
            if (action == null) throw new NullPointerException();
            Comparator<? super K> cmp = comparator;
            K f = fence;
            MyConcurrentSkipListMap.Node<K, V> e = current;
            current = null;
            for (; e != null; e = e.next) {
                K k;
                Object v;
                if ((k = e.key) != null && f != null && cpr(cmp, f, k) <= 0)
                    break;
                if ((v = e.value) != null && v != e) {
                    @SuppressWarnings("unchecked") V vv = (V) v;
                    action.accept
                            (new MyAbstractMap.SimpleImmutableEntry<K, V>(k, vv));
                }
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super  MyMap.Entry<K, V>> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            Comparator<? super K> cmp = comparator;
            K f = fence;
            MyConcurrentSkipListMap.Node<K, V> e = current;
            for (; e != null; e = e.next) {
                K k;
                Object v;
                if ((k = e.key) != null && f != null && cpr(cmp, f, k) <= 0) {
                    e = null;
                    break;
                }
                if ((v = e.value) != null && v != e) {
                    current = e.next;
                    @SuppressWarnings("unchecked") V vv = (V) v;
                    action.accept
                            (new MyAbstractMap.SimpleImmutableEntry<K, V>(k, vv));
                    return true;
                }
            }
            current = e;
            return false;
        }

        @Override
        public int characteristics() {
            return Spliterator.DISTINCT | Spliterator.SORTED |
                    Spliterator.ORDERED | Spliterator.CONCURRENT |
                    Spliterator.NONNULL;
        }

        public final Comparator<MyMap.Entry<K, V>> getComparator() {

            if (comparator != null) {
                return MyMap.Entry.comparingByKey(comparator);
            } else {
                return (Comparator<MyMap.Entry<K, V>> & Serializable) (e1, e2) -> {
                    @SuppressWarnings("unchecked")
                    Comparable<? super K> k1 = (Comparable<? super K>) e1.getKey();
                    return k1.compareTo(e2.getKey());
                };
            }
        }
    }


    final MyConcurrentSkipListMap.EntrySpliterator<K, V> entrySpliterator() {
        Comparator<? super K> cmp = comparator;
        for (; ; ) {
            MyConcurrentSkipListMap.HeadIndex<K, V> h;
            MyConcurrentSkipListMap.Node<K, V> p;
            MyConcurrentSkipListMap.Node<K, V> b = (h = head).node;
            if ((p = b.next) == null || p.value != null) {
                return new EntrySpliterator<K, V>(cmp, h, p, null, (p == null) ?
                        0 : Integer.MAX_VALUE);
            }
            p.helpDelete(b, p.next);
        }
    }


    private static final sun.misc.Unsafe UNSAFE;
    private static final long headOffset;
    private static final long SECONDARY;

    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = MyConcurrentSkipListMap.class;
            headOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("head"));
            Class<?> tk = Thread.class;
            SECONDARY = UNSAFE.objectFieldOffset
                    (tk.getDeclaredField("threadLocalRandomSecondarySeed"));

        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
