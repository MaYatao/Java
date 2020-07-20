package example.container.collection;


import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class MyLinkedHashMap<K, V>
        extends MyHashMap<K, V>
        implements MyMap<K, V> {


    static class Entry<K, V> extends MyHashMap.Node<K, V> {
        Entry<K, V> before, after;

        Entry(int hash, K key, V value, Node<K, V> next) {
            super(hash, key, value, next);
        }
    }

    private static final long serialVersionUID = 3801124242820219131L;


    transient Entry<K, V> head;


    transient Entry<K, V> tail;


    final boolean accessOrder;


    private void linkNodeLast(Entry<K, V> p) {
        Entry<K, V> last = tail;
        tail = p;
        if (last == null) {
            head = p;
        } else {
            p.before = last;
            last.after = p;
        }
    }


    private void transferLinks(Entry<K, V> src,
                               Entry<K, V> dst) {
        Entry<K, V> b = dst.before = src.before;
        Entry<K, V> a = dst.after = src.after;
        if (b == null) {
            head = dst;
        } else {
            b.after = dst;
        }
        if (a == null) {
            tail = dst;
        } else {
            a.before = dst;
        }
    }


    @Override
    void reinitialize() {
        super.reinitialize();
        head = tail = null;
    }

    @Override
    Node<K, V> newNode(int hash, K key, V value, Node<K, V> e) {
        Entry<K, V> p =
                new Entry<K, V>(hash, key, value, e);
        linkNodeLast(p);
        return p;
    }

    @Override
    Node<K, V> replacementNode(Node<K, V> p, Node<K, V> next) {
        Entry<K, V> q = (Entry<K, V>) p;
        Entry<K, V> t =
                new Entry<K, V>(q.hash, q.key, q.value, next);
        transferLinks(q, t);
        return t;
    }

    @Override
    MyHashMap.TreeNode<K, V> newTreeNode(int hash, K key, V value, Node<K, V> next) {
        TreeNode<K, V> p = new TreeNode<K, V>(hash, key, value, next);
        linkNodeLast(p);
        return p;
    }

    @Override
    TreeNode<K, V> replacementTreeNode(Node<K, V> p, Node<K, V> next) {
        Entry<K, V> q = (Entry<K, V>) p;
        TreeNode<K, V> t = new TreeNode<K, V>(q.hash, q.key, q.value, next);
        transferLinks(q, t);
        return t;
    }

    @Override
    void afterNodeRemoval(Node<K, V> e) {
        Entry<K, V> p =
                (Entry<K, V>) e, b = p.before, a = p.after;
        p.before = p.after = null;
        if (b == null) {
            head = a;
        } else {
            b.after = a;
        }
        if (a == null) {
            tail = b;
        } else {
            a.before = b;
        }
    }

    @Override
    void afterNodeInsertion(boolean evict) {
        Entry<K, V> first;
        if (evict && (first = head) != null && removeEldestEntry(first)) {
            K key = first.key;
            removeNode(hash(key), key, null, false, true);
        }
    }

    @Override
    void afterNodeAccess(Node<K, V> e) {
        Entry<K, V> last;
        if (accessOrder && (last = tail) != e) {
            Entry<K, V> p =
                    (Entry<K, V>) e, b = p.before, a = p.after;
            p.after = null;
            if (b == null) {
                head = a;
            } else {
                b.after = a;
            }
            if (a != null) {
                a.before = b;
            } else {
                last = b;
            }
            if (last == null) {
                head = p;
            } else {
                p.before = last;
                last.after = p;
            }
            tail = p;
            ++modCount;
        }
    }

    @Override
    void internalWriteEntries(ObjectOutputStream s) throws IOException {
        for (Entry<K, V> e = head; e != null; e = e.after) {
            s.writeObject(e.key);
            s.writeObject(e.value);
        }
    }


    public MyLinkedHashMap(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
        accessOrder = false;
    }


    public MyLinkedHashMap(int initialCapacity) {
        super(initialCapacity);
        accessOrder = false;
    }


    public MyLinkedHashMap() {
        super();
        accessOrder = false;
    }


    public MyLinkedHashMap(MyMap<? extends K, ? extends V> m) {
        super();
        accessOrder = false;
        putMapEntries(m, false);
    }


    public MyLinkedHashMap(int initialCapacity,
                           float loadFactor,
                           boolean accessOrder) {
        super(initialCapacity, loadFactor);
        this.accessOrder = accessOrder;
    }


    @Override
    public boolean containsValue(Object value) {
        for (Entry<K, V> e = head; e != null; e = e.after) {
            V v = e.value;
            if (v == value || (value != null && value.equals(v)))
                return true;
        }
        return false;
    }


    @Override
    public V get(Object key) {
        Node<K, V> e;
        if ((e = getNode(hash(key), key)) == null) {
            return null;
        }
        if (accessOrder) {
            afterNodeAccess(e);
        }
        return e.value;
    }


    @Override
    public V getOrDefault(Object key, V defaultValue) {
        Node<K, V> e;
        if ((e = getNode(hash(key), key)) == null) {
            return defaultValue;
        }
        if (accessOrder) {
            afterNodeAccess(e);
        }
        return e.value;
    }


    @Override
    public void clear() {
        super.clear();
        head = tail = null;
    }


    protected boolean removeEldestEntry(MyMap.Entry<K, V> eldest) {
        return false;
    }



    @Override
    public MySet<K> keySet() {
        MySet<K> ks = keySet;
        if (ks == null) {
            ks = new LinkedKeySet();
            keySet = ks;
        }
        return ks;
    }

    final class LinkedKeySet extends MyAbstractSet<K> {
        @Override
        public final int size() {
            return size;
        }

        @Override
        public final void clear() {
            this.clear();
        }

        @Override
        public final Iterator<K> iterator() {
            return new LinkedKeyIterator();
        }

        @Override
        public final boolean contains(Object o) {
            return containsKey(o);
        }

        @Override
        public final boolean remove(Object key) {
            return removeNode(hash(key), key, null, false, true) != null;
        }

        @Override
        public final Spliterator<K> spliterator() {
            return MySpliterators.spliterator(this, Spliterator.SIZED |
                    Spliterator.ORDERED |
                    Spliterator.DISTINCT);
        }

        @Override
        public final void forEach(Consumer<? super K> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            int mc = modCount;
            for (Entry<K, V> e = head; e != null; e = e.after) {
                action.accept(e.key);
            }
            if (modCount != mc) {
                throw new ConcurrentModificationException();
            }
        }
    }

    @Override
    public MyCollection<V> values() {
        MyCollection<V> vs = values;
        if (vs == null) {
            vs = new LinkedValues();
            values = vs;
        }
        return vs;
    }

    final class LinkedValues extends MyAbstractCollection<V> {
        @Override
        public final int size() {
            return size;
        }

        @Override

        public final void clear() {
            this.clear();
        }

        @Override

        public final Iterator<V> iterator() {
            return new LinkedValueIterator();
        }

        @Override

        public final boolean contains(Object o) {
            return containsValue(o);
        }

        @Override
        public final Spliterator<V> spliterator() {
            return MySpliterators.spliterator(this, Spliterator.SIZED |
                    Spliterator.ORDERED);
        }

        @Override

        public final void forEach(Consumer<? super V> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            int mc = modCount;
            for (Entry<K, V> e = head; e != null; e = e.after) {
                action.accept(e.value);
            }
            if (modCount != mc) {
                throw new ConcurrentModificationException();
            }
        }
    }


    @Override
    public MySet<MyMap.Entry<K, V>> entrySet() {
        MySet<MyMap.Entry<K, V>> es;
        return (es = entrySet) == null ? (entrySet = new LinkedEntrySet()) : es;
    }

    final class LinkedEntrySet extends MyAbstractSet<MyMap.Entry<K, V>> {
        @Override
        public final int size() {
            return size;
        }

        @Override
        public final void clear() {
            this.clear();
        }

        @Override
        public final Iterator<MyMap.Entry<K, V>> iterator() {
            return new LinkedEntryIterator();
        }

        @Override
        public final boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            Object key = e.getKey();
            MyLinkedHashMap.Node<K, V> candidate = getNode(hash(key), key);
            return candidate != null && candidate.equals(e);
        }

        @Override
        public final boolean remove(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                Object key = e.getKey();
                Object value = e.getValue();
                return removeNode(hash(key), key, value, true, true) != null;
            }
            return false;
        }

        @Override
        public final Spliterator<MyMap.Entry<K, V>> spliterator() {
            return MySpliterators.spliterator(this, Spliterator.SIZED |
                    Spliterator.ORDERED |
                    Spliterator.DISTINCT);
        }


        @Override
        public final void forEach(Consumer<? super MyMap.Entry<K,V>> action) {
            if (action == null) {
                throw new NullPointerException();
            }
            int mc = modCount;
            for (Entry<K, V> e = head; e != null; e = e.after) {
                action.accept(e);
            }
            if (modCount != mc) {
                throw new ConcurrentModificationException();
            }
        }
    }


    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        if (action == null) {
            throw new NullPointerException();
        }
        int mc = modCount;
        for (Entry<K, V> e = head; e != null; e = e.after) {
            action.accept(e.key, e.value);
        }
        if (modCount != mc) {
            throw new ConcurrentModificationException();
        }
    }

    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        if (function == null) {
            throw new NullPointerException();
        }
        int mc = modCount;
        for (Entry<K, V> e = head; e != null; e = e.after) {
            e.value = function.apply(e.key, e.value);
        }
        if (modCount != mc) {
            throw new ConcurrentModificationException();
        }
    }


    abstract class LinkedHashIterator {
        Entry<K, V> next;
        Entry<K, V> current;
        int expectedModCount;

        LinkedHashIterator() {
            next = head;
            expectedModCount = modCount;
            current = null;
        }

        public final boolean hasNext() {
            return next != null;
        }

        final Entry<K, V> nextNode() {
            Entry<K, V> e = next;
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            if (e == null) {
                throw new NoSuchElementException();
            }
            current = e;
            next = e.after;
            return e;
        }

        public final void remove() {
            Node<K, V> p = current;
            if (p == null) {
                throw new IllegalStateException();
            }
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            current = null;
            K key = p.key;
            removeNode(hash(key), key, null, false, false);
            expectedModCount = modCount;
        }
    }

    final class LinkedKeyIterator extends LinkedHashIterator
            implements Iterator<K> {
        @Override
        public final K next() {
            return nextNode().getKey();
        }
    }

    final class LinkedValueIterator extends LinkedHashIterator
            implements Iterator<V> {
        @Override
        public final V next() {
            return nextNode().value;
        }
    }

    final class LinkedEntryIterator extends LinkedHashIterator
            implements Iterator<MyMap.Entry<K, V>> {
        @Override
        public final MyMap.Entry<K, V> next() {
            return nextNode();
        }
    }


}
