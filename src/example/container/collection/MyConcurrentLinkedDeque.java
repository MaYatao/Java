package example.container.collection;


import java.util.*;
import java.util.function.Consumer;


public class MyConcurrentLinkedDeque<E>
        extends MyAbstractCollection<E>
        implements MyDeque<E>, java.io.Serializable {


    private static final long serialVersionUID = 876323262645176354L;


    private transient volatile Node<E> head;


    private transient volatile Node<E> tail;

    private static final Node<Object> PREV_TERMINATOR, NEXT_TERMINATOR;

    @SuppressWarnings("unchecked")
    Node<E> prevTerminator() {
        return (Node<E>) PREV_TERMINATOR;
    }

    @SuppressWarnings("unchecked")
    Node<E> nextTerminator() {
        return (Node<E>) NEXT_TERMINATOR;
    }

    static final class Node<E> {
        volatile Node<E> prev;
        volatile E item;
        volatile Node<E> next;

        Node() {
        }


        Node(E item) {
            UNSAFE.putObject(this, itemOffset, item);
        }

        boolean casItem(E cmp, E val) {
            return UNSAFE.compareAndSwapObject(this, itemOffset, cmp, val);
        }

        void lazySetNext(Node<E> val) {
            UNSAFE.putOrderedObject(this, nextOffset, val);
        }

        boolean casNext(Node<E> cmp, Node<E> val) {
            return UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
        }

        void lazySetPrev(Node<E> val) {
            UNSAFE.putOrderedObject(this, prevOffset, val);
        }

        boolean casPrev(Node<E> cmp, Node<E> val) {
            return UNSAFE.compareAndSwapObject(this, prevOffset, cmp, val);
        }


        private static final sun.misc.Unsafe UNSAFE;
        private static final long prevOffset;
        private static final long itemOffset;
        private static final long nextOffset;

        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = Node.class;
                prevOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("prev"));
                itemOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("item"));
                nextOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("next"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }


    private void linkFirst(E e) {
        checkNotNull(e);
        final Node<E> newNode = new Node<E>(e);

        restartFromHead:
        for (; ; ) {
            for (Node<E> h = head, p = h, q; ; ) {
                if ((q = p.prev) != null &&
                        (q = (p = q).prev) != null) {
                    p = (h != (h = head)) ? h : q;
                } else if (p.next == p) {
                    continue restartFromHead;
                } else {

                    newNode.lazySetNext(p);
                    if (p.casPrev(null, newNode)) {


                        if (p != h) {
                            casHead(h, newNode);
                        }
                        return;
                    }

                }
            }
        }
    }


    private void linkLast(E e) {
        checkNotNull(e);
        final Node<E> newNode = new Node<E>(e);

        restartFromTail:
        for (; ; ) {
            for (Node<E> t = tail, p = t, q; ; ) {
                if ((q = p.next) != null &&
                        (q = (p = q).next) != null) {
                    p = (t != (t = tail)) ? t : q;
                } else if (p.prev == p) {
                    continue restartFromTail;
                } else {

                    newNode.lazySetPrev(p);
                    if (p.casNext(null, newNode)) {


                        if (p != t) {
                            casTail(t, newNode);
                        }
                        return;
                    }

                }
            }
        }
    }

    private static final int HOPS = 2;


    void unlink(Node<E> x) {


        final Node<E> prev = x.prev;
        final Node<E> next = x.next;
        if (prev == null) {
            unlinkFirst(x, next);
        } else if (next == null) {
            unlinkLast(x, prev);
        } else {


            Node<E> activePred, activeSucc;
            boolean isFirst, isLast;
            int hops = 1;


            for (Node<E> p = prev; ; ++hops) {
                if (p.item != null) {
                    activePred = p;
                    isFirst = false;
                    break;
                }
                Node<E> q = p.prev;
                if (q == null) {
                    if (p.next == p) {
                        return;
                    }
                    activePred = p;
                    isFirst = true;
                    break;
                } else if (p == q) {
                    return;
                } else {
                    p = q;
                }
            }


            for (Node<E> p = next; ; ++hops) {
                if (p.item != null) {
                    activeSucc = p;
                    isLast = false;
                    break;
                }
                Node<E> q = p.next;
                if (q == null) {
                    if (p.prev == p) {
                        return;
                    }
                    activeSucc = p;
                    isLast = true;
                    break;
                } else if (p == q) {
                    return;
                } else {
                    p = q;
                }
            }


            if (hops < HOPS

                    && (isFirst | isLast)) {
                return;
            }


            skipDeletedSuccessors(activePred);
            skipDeletedPredecessors(activeSucc);


            if ((isFirst | isLast) &&


                    (activePred.next == activeSucc) &&
                    (activeSucc.prev == activePred) &&
                    (isFirst ? activePred.prev == null : activePred.item != null) &&
                    (isLast ? activeSucc.next == null : activeSucc.item != null)) {

                updateHead();
                updateTail();


                x.lazySetPrev(isFirst ? prevTerminator() : x);
                x.lazySetNext(isLast ? nextTerminator() : x);
            }
        }
    }


    private void unlinkFirst(Node<E> first, Node<E> next) {


        for (Node<E> o = null, p = next, q; ; ) {
            if (p.item != null || (q = p.next) == null) {
                if (o != null && p.prev != p && first.casNext(next, p)) {
                    skipDeletedPredecessors(p);
                    if (first.prev == null &&
                            (p.next == null || p.item != null) &&
                            p.prev == first) {

                        updateHead();
                        updateTail();


                        o.lazySetNext(o);
                        o.lazySetPrev(prevTerminator());
                    }
                }
                return;
            } else if (p == q) {
                return;
            } else {
                o = p;
                p = q;
            }
        }
    }


    private void unlinkLast(Node<E> last, Node<E> prev) {


        for (Node<E> o = null, p = prev, q; ; ) {
            if (p.item != null || (q = p.prev) == null) {
                if (o != null && p.next != p && last.casPrev(prev, p)) {
                    skipDeletedSuccessors(p);
                    if (last.next == null &&
                            (p.prev == null || p.item != null) &&
                            p.next == last) {

                        updateHead();
                        updateTail();


                        o.lazySetPrev(o);
                        o.lazySetNext(nextTerminator());
                    }
                }
                return;
            } else if (p == q) {
                return;
            } else {
                o = p;
                p = q;
            }
        }
    }


    private final void updateHead() {


        Node<E> h, p, q;
        restartFromHead:
        while ((h = head).item == null && (p = h.prev) != null) {
            for (; ; ) {
                if ((q = p.prev) == null ||
                        (q = (p = q).prev) == null) {


                    if (casHead(h, p)) {
                        return;
                    } else {
                        continue restartFromHead;
                    }
                } else if (h != head) {
                    continue restartFromHead;
                } else {
                    p = q;
                }
            }
        }
    }


    private final void updateTail() {


        Node<E> t, p, q;
        restartFromTail:
        while ((t = tail).item == null && (p = t.next) != null) {
            for (; ; ) {
                if ((q = p.next) == null ||
                        (q = (p = q).next) == null) {


                    if (casTail(t, p)) {
                        return;
                    } else {
                        continue restartFromTail;
                    }
                } else if (t != tail) {
                    continue restartFromTail;
                } else {
                    p = q;
                }
            }
        }
    }

    private void skipDeletedPredecessors(Node<E> x) {
        whileActive:
        do {
            Node<E> prev = x.prev;


            Node<E> p = prev;
            findActive:
            for (; ; ) {
                if (p.item != null) {
                    break findActive;
                }
                Node<E> q = p.prev;
                if (q == null) {
                    if (p.next == p) {
                        continue whileActive;
                    }
                    break findActive;
                } else if (p == q) {
                    continue whileActive;
                } else {
                    p = q;
                }
            }


            if (prev == p || x.casPrev(prev, p)) {
                return;
            }

        } while (x.item != null || x.next == null);
    }

    private void skipDeletedSuccessors(Node<E> x) {
        whileActive:
        do {
            Node<E> next = x.next;


            Node<E> p = next;
            findActive:
            for (; ; ) {
                if (p.item != null)
                    break findActive;
                Node<E> q = p.next;
                if (q == null) {
                    if (p.prev == p)
                        continue whileActive;
                    break findActive;
                } else if (p == q) {
                    continue whileActive;
                } else {
                    p = q;
                }
            }


            if (next == p || x.casNext(next, p)) {
                return;
            }

        } while (x.item != null || x.prev == null);
    }


    final Node<E> succ(Node<E> p) {

        Node<E> q = p.next;
        return (p == q) ? first() : q;
    }


    final Node<E> pred(Node<E> p) {
        Node<E> q = p.prev;
        return (p == q) ? last() : q;
    }


    Node<E> first() {
        restartFromHead:
        for (; ; ) {
            for (Node<E> h = head, p = h, q; ; ) {
                if ((q = p.prev) != null &&
                        (q = (p = q).prev) != null) {
                    p = (h != (h = head)) ? h : q;
                } else if (p == h


                        || casHead(h, p)) {
                    return p;
                } else {
                    continue restartFromHead;
                }
            }
        }
    }


    Node<E> last() {
        restartFromTail:
        for (; ; ) {
            for (Node<E> t = tail, p = t, q; ; ) {
                if ((q = p.next) != null &&
                        (q = (p = q).next) != null) {
                    p = (t != (t = tail)) ? t : q;
                } else if (p == t


                        || casTail(t, p)) {
                    return p;
                } else {
                    continue restartFromTail;
                }
            }
        }
    }


    private static void checkNotNull(Object v) {
        if (v == null) {
            throw new NullPointerException();
        }
    }


    private E screenNullResult(E v) {
        if (v == null) {
            throw new NoSuchElementException();
        }
        return v;
    }


    private ArrayList<E> toArrayList() {
        ArrayList<E> list = new ArrayList<E>();
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null) {
                list.add(item);
            }
        }
        return list;
    }


    public MyConcurrentLinkedDeque() {
        head = tail = new Node<E>(null);
    }


    public MyConcurrentLinkedDeque(MyCollection<? extends E> c) {

        Node<E> h = null, t = null;
        for (E e : c) {
            checkNotNull(e);
            Node<E> newNode = new Node<E>(e);
            if (h == null) {
                h = t = newNode;
            } else {
                t.lazySetNext(newNode);
                newNode.lazySetPrev(t);
                t = newNode;
            }
        }
        initHeadTail(h, t);
    }


    private void initHeadTail(Node<E> h, Node<E> t) {
        if (h == t) {
            if (h == null) {
                h = t = new Node<E>(null);
            } else {

                Node<E> newNode = new Node<E>(null);
                t.lazySetNext(newNode);
                newNode.lazySetPrev(t);
                t = newNode;
            }
        }
        head = h;
        tail = t;
    }

    @Override
    public void addFirst(E e) {
        linkFirst(e);
    }

    @Override
    public void addLast(E e) {
        linkLast(e);
    }

    @Override
    public boolean offerFirst(E e) {
        linkFirst(e);
        return true;
    }

    @Override
    public boolean offerLast(E e) {
        linkLast(e);
        return true;
    }

    @Override
    public E peekFirst() {
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null) {
                return item;
            }
        }
        return null;
    }

    @Override
    public E peekLast() {
        for (Node<E> p = last(); p != null; p = pred(p)) {
            E item = p.item;
            if (item != null) {
                return item;
            }
        }
        return null;
    }

    @Override
    public E getFirst() {
        return screenNullResult(peekFirst());
    }

    @Override
    public E getLast() {
        return screenNullResult(peekLast());
    }

    @Override
    public E pollFirst() {
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null && p.casItem(item, null)) {
                unlink(p);
                return item;
            }
        }
        return null;
    }

    @Override
    public E pollLast() {
        for (Node<E> p = last(); p != null; p = pred(p)) {
            E item = p.item;
            if (item != null && p.casItem(item, null)) {
                unlink(p);
                return item;
            }
        }
        return null;
    }

    @Override
    public E removeFirst() {
        return screenNullResult(pollFirst());
    }

    @Override
    public E removeLast() {
        return screenNullResult(pollLast());
    }


    @Override
    public boolean offer(E e) {
        return offerLast(e);
    }

    @Override
    public boolean add(E e) {
        return offerLast(e);
    }

    @Override
    public E poll() {
        return pollFirst();
    }

    @Override
    public E peek() {
        return peekFirst();
    }

    @Override
    public E remove() {
        return removeFirst();
    }

    @Override
    public E pop() {
        return removeFirst();
    }

    @Override
    public E element() {
        return getFirst();
    }

    @Override
    public void push(E e) {
        addFirst(e);
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        checkNotNull(o);
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null && o.equals(item) && p.casItem(item, null)) {
                unlink(p);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        checkNotNull(o);
        for (Node<E> p = last(); p != null; p = pred(p)) {
            E item = p.item;
            if (item != null && o.equals(item) && p.casItem(item, null)) {
                unlink(p);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null && o.equals(item)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        return peekFirst() == null;
    }

    @Override
    public int size() {
        int count = 0;
        for (Node<E> p = first(); p != null; p = succ(p)) {
            if (p.item != null) {
                if (++count == Integer.MAX_VALUE) {
                    break;
                }
            }
        }
        return count;
    }

    @Override
    public boolean remove(Object o) {
        return removeFirstOccurrence(o);
    }

    @Override
    public boolean addAll(MyCollection<? extends E> c) {
        if (c == this) {
            throw new IllegalArgumentException();
        }


        Node<E> beginningOfTheEnd = null, last = null;
        for (E e : c) {
            checkNotNull(e);
            Node<E> newNode = new Node<E>(e);
            if (beginningOfTheEnd == null) {
                beginningOfTheEnd = last = newNode;
            } else {
                last.lazySetNext(newNode);
                newNode.lazySetPrev(last);
                last = newNode;
            }
        }
        if (beginningOfTheEnd == null) {
            return false;
        }


        restartFromTail:
        for (; ; ) {
            for (Node<E> t = tail, p = t, q; ; ) {
                if ((q = p.next) != null &&
                        (q = (p = q).next) != null) {
                    p = (t != (t = tail)) ? t : q;
                } else if (p.prev == p) {
                    continue restartFromTail;
                } else {

                    beginningOfTheEnd.lazySetPrev(p);
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
            }
        }
    }

    @Override
    public void clear() {
        while (pollFirst() != null)
            ;
    }

    @Override
    public Object[] toArray() {
        return toArrayList().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return toArrayList().toArray(a);
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new DescendingItr();
    }

    private abstract class AbstractItr implements Iterator<E> {

        private Node<E> nextNode;


        private E nextItem;


        private Node<E> lastRet;

        abstract Node<E> startNode();

        abstract Node<E> nextNode(Node<E> p);

        AbstractItr() {
            advance();
        }


        private void advance() {
            lastRet = nextNode;

            Node<E> p = (nextNode == null) ? startNode() : nextNode(nextNode);
            for (; ; p = nextNode(p)) {
                if (p == null) {

                    nextNode = null;
                    nextItem = null;
                    break;
                }
                E item = p.item;
                if (item != null) {
                    nextNode = p;
                    nextItem = item;
                    break;
                }
            }
        }

        @Override
        public boolean hasNext() {
            return nextItem != null;
        }

        @Override
        public E next() {
            E item = nextItem;
            if (item == null) {
                throw new NoSuchElementException();
            }
            advance();
            return item;
        }

        @Override
        public void remove() {
            Node<E> l = lastRet;
            if (l == null) {
                throw new IllegalStateException();
            }
            l.item = null;
            unlink(l);
            lastRet = null;
        }
    }


    private class Itr extends AbstractItr {
        @Override
        Node<E> startNode() {
            return first();
        }

        @Override
        Node<E> nextNode(Node<E> p) {
            return succ(p);
        }
    }


    private class DescendingItr extends AbstractItr {
        @Override
        Node<E> startNode() {
            return last();
        }

        @Override
        Node<E> nextNode(Node<E> p) {
            return pred(p);
        }
    }


    static final class CLDSpliterator<E> implements Spliterator<E> {
        static final int MAX_BATCH = 1 << 25;
        final MyConcurrentLinkedDeque<E> queue;
        Node<E> current;
        int batch;
        boolean exhausted;

        CLDSpliterator(MyConcurrentLinkedDeque<E> queue) {
            this.queue = queue;
        }

        @Override
        public Spliterator<E> trySplit() {
            Node<E> p;
            final MyConcurrentLinkedDeque<E> q = this.queue;
            int b = batch;
            int n = (b <= 0) ? 1 : (b >= MAX_BATCH) ? MAX_BATCH : b + 1;
            if (!exhausted &&
                    ((p = current) != null || (p = q.first()) != null)) {
                if (p.item == null && p == (p = p.next)) {
                    current = p = q.first();
                }
                if (p != null && p.next != null) {
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
            }
            return null;
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            Node<E> p;
            if (action == null) {
                throw new NullPointerException();
            }
            final MyConcurrentLinkedDeque<E> q = this.queue;
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
            Node<E> p;
            if (action == null) {
                throw new NullPointerException();
            }
            final MyConcurrentLinkedDeque<E> q = this.queue;
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
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.NONNULL |
                    Spliterator.CONCURRENT;
        }
    }

    @Override
    public Spliterator<E> spliterator() {
        return new CLDSpliterator<E>(this);
    }


    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {


        s.defaultWriteObject();


        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null) {
                s.writeObject(item);
            }
        }


        s.writeObject(null);
    }


    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();


        Node<E> h = null, t = null;
        Object item;
        while ((item = s.readObject()) != null) {
            @SuppressWarnings("unchecked")
            Node<E> newNode = new Node<E>((E) item);
            if (h == null)
                h = t = newNode;
            else {
                t.lazySetNext(newNode);
                newNode.lazySetPrev(t);
                t = newNode;
            }
        }
        initHeadTail(h, t);
    }

    private boolean casHead(Node<E> cmp, Node<E> val) {
        return UNSAFE.compareAndSwapObject(this, headOffset, cmp, val);
    }

    private boolean casTail(Node<E> cmp, Node<E> val) {
        return UNSAFE.compareAndSwapObject(this, tailOffset, cmp, val);
    }


    private static final sun.misc.Unsafe UNSAFE;
    private static final long headOffset;
    private static final long tailOffset;

    static {
        PREV_TERMINATOR = new Node<Object>();
        PREV_TERMINATOR.next = PREV_TERMINATOR;
        NEXT_TERMINATOR = new Node<Object>();
        NEXT_TERMINATOR.prev = NEXT_TERMINATOR;
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = MyConcurrentLinkedDeque.class ;
            headOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("head"));
            tailOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("tail"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
