package example.container.collection.current;



import example.container.collection.MyAbstractQueue;
import example.container.collection.MyCollection;
import example.container.collection.MyQueue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;


public class MyConcurrentLinkedQueue<E> extends MyAbstractQueue<E>
        implements MyQueue<E>, java.io.Serializable {
    private static final long serialVersionUID = 196745693267521676L;



    private static class Node<E> {
        volatile E item;
        volatile MyConcurrentLinkedQueue.Node<E> next;


        Node(E item) {
            UNSAFE.putObject(this, itemOffset, item);
        }

        boolean casItem(E cmp, E val) {
            return UNSAFE.compareAndSwapObject(this, itemOffset, cmp, val);
        }

        void lazySetNext(MyConcurrentLinkedQueue.Node<E> val) {
            UNSAFE.putOrderedObject(this, nextOffset, val);
        }

        boolean casNext(MyConcurrentLinkedQueue.Node<E> cmp, MyConcurrentLinkedQueue.Node<E> val) {
            return UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
        }



        private static final sun.misc.Unsafe UNSAFE;
        private static final long itemOffset;
        private static final long nextOffset;

        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = MyConcurrentLinkedQueue.Node.class;
                itemOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("item"));
                nextOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("next"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }


    private transient volatile MyConcurrentLinkedQueue.Node<E> head;


    private transient volatile MyConcurrentLinkedQueue.Node<E> tail;


    public MyConcurrentLinkedQueue() {
        head = tail = new MyConcurrentLinkedQueue.Node<E>(null);
    }


    public MyConcurrentLinkedQueue(Collection<? extends E> c) {
        MyConcurrentLinkedQueue.Node<E> h = null, t = null;
        for (E e : c) {
            checkNotNull(e);
            MyConcurrentLinkedQueue.Node<E> newNode = new MyConcurrentLinkedQueue.Node<E>(e);
            if (h == null)
                h = t = newNode;
            else {
                t.lazySetNext(newNode);
                t = newNode;
            }
        }
        if (h == null) {
            h = t = new Node<E>(null);
        }
        head = h;
        tail = t;
    }



    @Override
    public boolean add(E e) {
        return offer(e);
    }


    final void updateHead(MyConcurrentLinkedQueue.Node<E> h, MyConcurrentLinkedQueue.Node<E> p) {
        if (h != p && casHead(h, p)) {
            h.lazySetNext(h);
        }
    }


    final MyConcurrentLinkedQueue.Node<E> succ(MyConcurrentLinkedQueue.Node<E> p) {
        MyConcurrentLinkedQueue.Node<E> next = p.next;
        return (p == next) ? head : next;
    }

    @Override
    public boolean offer(E e) {
        checkNotNull(e);
        final MyConcurrentLinkedQueue.Node<E> newNode = new MyConcurrentLinkedQueue.Node<E>(e);

        for (MyConcurrentLinkedQueue.Node<E> t = tail, p = t;;) {
            MyConcurrentLinkedQueue.Node<E> q = p.next;
            if (q == null) {

                if (p.casNext(null, newNode)) {



                    if (p != t) {
                        casTail(t, newNode);
                    }
                    return true;
                }

            }
            else if (p == q) {
                p = (t != (t = tail)) ? t : head;
            } else {
                p = (p != t && t != (t = tail)) ? t : q;
            }
        }
    }

    @Override
    public E poll() {
        restartFromHead:
        for (;;) {
            for (MyConcurrentLinkedQueue.Node<E> h = head, p = h, q;;) {
                E item = p.item;

                if (item != null && p.casItem(item, null)) {


                    if (p != h) {
                        updateHead(h, ((q = p.next) != null) ? q : p);
                    }
                    return item;
                }
                else if ((q = p.next) == null) {
                    updateHead(h, p);
                    return null;
                }
                else if (p == q) {
                    continue restartFromHead;
                } else {
                    p = q;
                }
            }
        }
    }
    @Override
    public E peek() {
        restartFromHead:
        for (;;) {
            for (MyConcurrentLinkedQueue.Node<E> h = head, p = h, q;;) {
                E item = p.item;
                if (item != null || (q = p.next) == null) {
                    updateHead(h, p);
                    return item;
                }
                else if (p == q) {
                    continue restartFromHead;
                } else {
                    p = q;
                }
            }
        }
    }


    Node<E> first() {
        restartFromHead:
        for (;;) {
            for (MyConcurrentLinkedQueue.Node<E> h = head, p = h, q;;) {
                boolean hasItem = (p.item != null);
                if (hasItem || (q = p.next) == null) {
                    updateHead(h, p);
                    return hasItem ? p : null;
                }
                else if (p == q) {
                    continue restartFromHead;
                } else {
                    p = q;
                }
            }
        }
    }

    @Override
    public boolean isEmpty() {
        return first() == null;
    }

    @Override
    public int size() {
        int count = 0;
        for (MyConcurrentLinkedQueue.Node<E> p = first(); p != null; p = succ(p)) {
            if (p.item != null) {
                if (++count == Integer.MAX_VALUE) {
                    break;
                }
            }
        }
        return count;
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        for (MyConcurrentLinkedQueue.Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null && o.equals(item))
                return true;
        }
        return false;
    }

    @Override
    public boolean remove(Object o) {
        if (o != null) {
            MyConcurrentLinkedQueue.Node<E> next, pred = null;
            for (MyConcurrentLinkedQueue.Node<E> p = first(); p != null; pred = p, p = next) {
                boolean removed = false;
                E item = p.item;
                if (item != null) {
                    if (!o.equals(item)) {
                        next = succ(p);
                        continue;
                    }
                    removed = p.casItem(item, null);
                }

                next = succ(p);
                if (pred != null && next != null) {
                    pred.casNext(p, next);
                }
                if (removed) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean addAll(MyCollection<? extends E> c) {
        if (c == this) {
            throw new IllegalArgumentException();
        }


        MyConcurrentLinkedQueue.Node<E> beginningOfTheEnd = null, last = null;
        for (E e : c) {
            checkNotNull(e);
            MyConcurrentLinkedQueue.Node<E> newNode = new MyConcurrentLinkedQueue.Node<E>(e);
            if (beginningOfTheEnd == null) {
                beginningOfTheEnd = last = newNode;
            } else {
                last.lazySetNext(newNode);
                last = newNode;
            }
        }
        if (beginningOfTheEnd == null) {
            return false;
        }


        for (MyConcurrentLinkedQueue.Node<E> t = tail, p = t;;) {
            MyConcurrentLinkedQueue.Node<E> q = p.next;
            if (q == null) {

                if (p.casNext(null, beginningOfTheEnd)) {


                    if (!casTail(t, last)) {


                        t = tail;
                        if (last.next == null) {
                            casTail(t, last);
                        }
                    }
                    return true;
                }

            }
            else if (p == q) {
                p = (t != (t = tail)) ? t : head;
            } else {
                p = (p != t && t != (t = tail)) ? t : q;
            }
        }
    }

    @Override
    public Object[] toArray() {

        ArrayList<E> al = new ArrayList<E>();
        for (MyConcurrentLinkedQueue.Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null) {
                al.add(item);
            }
        }
        return al.toArray();
    }


    @SuppressWarnings("unchecked")@Override
    public <T> T[] toArray(T[] a) {

        int k = 0;
        MyConcurrentLinkedQueue.Node<E> p;
        for (p = first(); p != null && k < a.length; p = succ(p)) {
            E item = p.item;
            if (item != null) {
                a[k++] = (T)item;
            }
        }
        if (p == null) {
            if (k < a.length) {
                a[k] = null;
            }
            return a;
        }


        ArrayList<E> al = new ArrayList<E>();
        for (MyConcurrentLinkedQueue.Node<E> q = first(); q != null; q = succ(q)) {
            E item = q.item;
            if (item != null) {
                al.add(item);
            }
        }
        return al.toArray(a);
    }

    @Override
    public Iterator<E> iterator() {
        return new MyConcurrentLinkedQueue.Itr();
    }

    private class Itr implements Iterator<E> {

        private MyConcurrentLinkedQueue.Node<E> nextNode;


        private E nextItem;


        private MyConcurrentLinkedQueue.Node<E> lastRet;

        Itr() {
            advance();
        }


        private E advance() {
            lastRet = nextNode;
            E x = nextItem;

            MyConcurrentLinkedQueue.Node<E> pred, p;
            if (nextNode == null) {
                p = first();
                pred = null;
            } else {
                pred = nextNode;
                p = succ(nextNode);
            }

            for (;;) {
                if (p == null) {
                    nextNode = null;
                    nextItem = null;
                    return x;
                }
                E item = p.item;
                if (item != null) {
                    nextNode = p;
                    nextItem = item;
                    return x;
                } else {

                    MyConcurrentLinkedQueue.Node<E> next = succ(p);
                    if (pred != null && next != null) {
                        pred.casNext(p, next);
                    }
                    p = next;
                }
            }
        }
        @Override
        public boolean hasNext() {
            return nextNode != null;
        }
        @Override
        public E next() {
            if (nextNode == null) {
                throw new NoSuchElementException();
            }
            return advance();
        }
        @Override
        public void remove() {
            MyConcurrentLinkedQueue.Node<E> l = lastRet;
            if (l == null) {
                throw new IllegalStateException();
            }

            l.item = null;
            lastRet = null;
        }
    }


    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {


        s.defaultWriteObject();


        for (MyConcurrentLinkedQueue.Node<E> p = first(); p != null; p = succ(p)) {
            Object item = p.item;
            if (item != null) {
                s.writeObject(item);
            }
        }


        s.writeObject(null);
    }


    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();


        MyConcurrentLinkedQueue.Node<E> h = null, t = null;
        Object item;
        while ((item = s.readObject()) != null) {
            @SuppressWarnings("unchecked")
            MyConcurrentLinkedQueue.Node<E> newNode = new MyConcurrentLinkedQueue.Node<E>((E) item);
            if (h == null) {
                h = t = newNode;
            } else {
                t.lazySetNext(newNode);
                t = newNode;
            }
        }
        if (h == null) {
            h = t = new Node<E>(null);
        }
        head = h;
        tail = t;
    }


    static final class CLQSpliterator<E> implements Spliterator<E> {
        static final int MAX_BATCH = 1 << 25;
        final MyConcurrentLinkedQueue<E> queue;
        MyConcurrentLinkedQueue.Node<E> current;
        int batch;
        boolean exhausted;
        CLQSpliterator(MyConcurrentLinkedQueue<E> queue) {
            this.queue = queue;
        }
        @Override
        public Spliterator<E> trySplit() {
            MyConcurrentLinkedQueue.Node<E> p;
            final MyConcurrentLinkedQueue<E> q = this.queue;
            int b = batch;
            int n = (b <= 0) ? 1 : (b >= MAX_BATCH) ? MAX_BATCH : b + 1;
            if (!exhausted &&
                    ((p = current) != null || (p = q.first()) != null) &&
                    p.next != null) {
                Object[] a = new Object[n];
                int i = 0;
                do {
                    if ((a[i] = p.item) != null) {
                        ++i;
                    }
                    if (p == (p = p.next)) {
                        p = q.first();
                    }
                } while (p != null && i < n);
                if ((current = p) == null) {
                    exhausted = true;
                }
                if (i > 0) {
                    batch = i;
                    return Spliterators.spliterator
                            (a, 0, i, Spliterator.ORDERED | Spliterator.NONNULL |
                                    Spliterator.CONCURRENT);
                }
            }
            return null;
        }
        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            MyConcurrentLinkedQueue.Node<E> p;
            if (action == null) {
                throw new NullPointerException();
            }
            final MyConcurrentLinkedQueue<E> q = this.queue;
            if (!exhausted &&
                    ((p = current) != null || (p = q.first()) != null)) {
                exhausted = true;
                do {
                    E e = p.item;
                    if (p == (p = p.next)) {
                        p = q.first();
                    }
                    if (e != null) {
                        action.accept(e);
                    }
                } while (p != null);
            }
        }
        @Override
        public boolean tryAdvance(Consumer<? super E> action) {
            MyConcurrentLinkedQueue.Node<E> p;
            if (action == null) {
                throw new NullPointerException();
            }
            final MyConcurrentLinkedQueue<E> q = this.queue;
            if (!exhausted &&
                    ((p = current) != null || (p = q.first()) != null)) {
                E e;
                do {
                    e = p.item;
                    if (p == (p = p.next)) {
                        p = q.first();
                    }
                } while (e == null && p != null);
                if ((current = p) == null) {
                    exhausted = true;
                }
                if (e != null) {
                    action.accept(e);
                    return true;
                }
            }
            return false;
        }
        @Override
        public long estimateSize() { return Long.MAX_VALUE; }
        @Override
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.NONNULL |
                    Spliterator.CONCURRENT;
        }
    }


    @Override
    public Spliterator<E> spliterator() {
        return new MyConcurrentLinkedQueue.CLQSpliterator<E>(this);
    }


    private static void checkNotNull(Object v) {
        if (v == null) {
            throw new NullPointerException();
        }
    }

    private boolean casTail(MyConcurrentLinkedQueue.Node<E> cmp, MyConcurrentLinkedQueue.Node<E> val) {
        return UNSAFE.compareAndSwapObject(this, tailOffset, cmp, val);
    }

    private boolean casHead(MyConcurrentLinkedQueue.Node<E> cmp, MyConcurrentLinkedQueue.Node<E> val) {
        return UNSAFE.compareAndSwapObject(this, headOffset, cmp, val);
    }



    private static final sun.misc.Unsafe UNSAFE;
    private static final long headOffset;
    private static final long tailOffset;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = MyConcurrentLinkedQueue.class;
            headOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("head"));
            tailOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("tail"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
