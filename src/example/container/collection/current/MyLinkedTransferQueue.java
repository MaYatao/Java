package example.container.collection.current;


import example.container.collection.MyAbstractQueue;
import example.container.collection.MyCollection;
import example.container.collection.MyTransferQueue;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.locks.LockSupport;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;


public class MyLinkedTransferQueue<E> extends MyAbstractQueue<E>
        implements MyTransferQueue<E>, java.io.Serializable {
    private static final long serialVersionUID = -3223113410248163686L;




    private static final boolean MP =
            Runtime.getRuntime().availableProcessors() > 1;


    private static final int FRONT_SPINS   = 1 << 7;


    private static final int CHAINED_SPINS = FRONT_SPINS >>> 1;


    static final int SWEEP_THRESHOLD = 32;


    static final class Node {
        final boolean isData;
        volatile Object item;
        volatile Node next;
        volatile Thread waiter;


        final boolean casNext(Node cmp, Node val) {
            return UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
        }

        final boolean casItem(Object cmp, Object val) {

            return UNSAFE.compareAndSwapObject(this, itemOffset, cmp, val);
        }


        Node(Object item, boolean isData) {
            UNSAFE.putObject(this, itemOffset, item);
            this.isData = isData;
        }


        final void forgetNext() {
            UNSAFE.putObject(this, nextOffset, this);
        }


        final void forgetContents() {
            UNSAFE.putObject(this, itemOffset, this);
            UNSAFE.putObject(this, waiterOffset, null);
        }


        final boolean isMatched() {
            Object x = item;
            return (x == this) || ((x == null) == isData);
        }


        final boolean isUnmatchedRequest() {
            return !isData && item == null;
        }


        final boolean cannotPrecede(boolean haveData) {
            boolean d = isData;
            Object x;
            return d != haveData && (x = item) != this && (x != null) == d;
        }


        final boolean tryMatchData() {

            Object x = item;
            if (x != null && x != this && casItem(x, null)) {
                LockSupport.unpark(waiter);
                return true;
            }
            return false;
        }

        private static final long serialVersionUID = -3375979862319811754L;


        private static final sun.misc.Unsafe UNSAFE;
        private static final long itemOffset;
        private static final long nextOffset;
        private static final long waiterOffset;
        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = Node.class;
                itemOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("item"));
                nextOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("next"));
                waiterOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("waiter"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }


    transient volatile Node head;


    private transient volatile Node tail;


    private transient volatile int sweepVotes;


    private boolean casTail(Node cmp, Node val) {
        return UNSAFE.compareAndSwapObject(this, tailOffset, cmp, val);
    }

    private boolean casHead(Node cmp, Node val) {
        return UNSAFE.compareAndSwapObject(this, headOffset, cmp, val);
    }

    private boolean casSweepVotes(int cmp, int val) {
        return UNSAFE.compareAndSwapInt(this, sweepVotesOffset, cmp, val);
    }


    private static final int NOW   = 0;
    private static final int ASYNC = 1;
    private static final int SYNC  = 2;
    private static final int TIMED = 3;

    @SuppressWarnings("unchecked")
    static <E> E cast(Object item) {

        return (E) item;
    }


    private E xfer(E e, boolean haveData, int how, long nanos) {
        if (haveData && (e == null)) {
            throw new NullPointerException();
        }
        Node s = null;

        retry:
        for (;;) {

            for (Node h = head, p = h; p != null;) {
                boolean isData = p.isData;
                Object item = p.item;
                if (item != p && (item != null) == isData) {
                    if (isData == haveData) {
                        break;
                    }
                    if (p.casItem(item, e)) {
                        for (Node q = p; q != h;) {
                            Node n = q.next;
                            if (head == h && casHead(h, n == null ? q : n)) {
                                h.forgetNext();
                                break;
                            }
                            if ((h = head)   == null ||
                                    (q = h.next) == null || !q.isMatched()) {
                                break;
                            }
                        }
                        LockSupport.unpark(p.waiter);
                        return MyLinkedTransferQueue.<E>cast(item);
                    }
                }
                Node n = p.next;
                p = (p != n) ? n : (h = head);
            }

            if (how != NOW) {
                if (s == null) {
                    s = new Node(e, haveData);
                }
                Node pred = tryAppend(s, haveData);
                if (pred == null) {
                    continue retry;
                }
                if (how != ASYNC) {
                    return awaitMatch(s, pred, e, (how == TIMED), nanos);
                }
            }
            return e;
        }
    }


    private Node tryAppend(Node s, boolean haveData) {
        for (Node t = tail, p = t;;) {
            Node n, u;
            if (p == null && (p = head) == null) {
                if (casHead(null, s)) {
                    return s;
                }
            }
            else if (p.cannotPrecede(haveData)) {
                return null;
            } else if ((n = p.next) != null) {
                p = p != t && t != (u = tail) ? (t = u) :
                        (p != n) ? n : null;
            } else if (!p.casNext(null, s)) {
                p = p.next;
            } else {
                if (p != t) {
                    while ((tail != t || !casTail(t, s)) &&
                            (t = tail)   != null &&
                            (s = t.next) != null &&
                            (s = s.next) != null && s != t) {
                        ;
                    }
                }
                return p;
            }
        }
    }


    private E awaitMatch(Node s, Node pred, E e, boolean timed, long nanos) {
        final long deadline = timed ? System.nanoTime() + nanos : 0L;
        Thread w = Thread.currentThread();
        int spins = -1;
        ThreadLocalRandom randomYields = null;

        for (;;) {
            Object item = s.item;
            if (item != e) {

                s.forgetContents();
                return MyLinkedTransferQueue.<E>cast(item);
            }
            if ((w.isInterrupted() || (timed && nanos <= 0)) &&
                    s.casItem(e, s)) {
                unsplice(pred, s);
                return e;
            }

            if (spins < 0) {
                if ((spins = spinsFor(pred, s.isData)) > 0) {
                    randomYields = ThreadLocalRandom.current();
                }
            }
            else if (spins > 0) {
                --spins;
                if (randomYields.nextInt(CHAINED_SPINS) == 0) {
                    Thread.yield();
                }
            }
            else if (s.waiter == null) {
                s.waiter = w;
            }
            else if (timed) {
                nanos = deadline - System.nanoTime();
                if (nanos > 0L) {
                    LockSupport.parkNanos(this, nanos);
                }
            }
            else {
                LockSupport.park(this);
            }
        }
    }


    private static int spinsFor(Node pred, boolean haveData) {
        if (MP && pred != null) {
            if (pred.isData != haveData) {
                return FRONT_SPINS + CHAINED_SPINS;
            }
            if (pred.isMatched()) {
                return FRONT_SPINS;
            }
            if (pred.waiter == null) {
                return CHAINED_SPINS;
            }
        }
        return 0;
    }




    final Node succ(Node p) {
        Node next = p.next;
        return (p == next) ? head : next;
    }


    private Node firstOfMode(boolean isData) {
        for (Node p = head; p != null; p = succ(p)) {
            if (!p.isMatched()) {
                return (p.isData == isData) ? p : null;
            }
        }
        return null;
    }


    final Node firstDataNode() {
        for (Node p = head; p != null;) {
            Object item = p.item;
            if (p.isData) {
                if (item != null && item != p) {
                    return p;
                }
            }
            else if (item == null) {
                break;
            }
            if (p == (p = p.next)) {
                p = head;
            }
        }
        return null;
    }


    private E firstDataItem() {
        for (Node p = head; p != null; p = succ(p)) {
            Object item = p.item;
            if (p.isData) {
                if (item != null && item != p) {
                    return MyLinkedTransferQueue.<E>cast(item);
                }
            }
            else if (item == null) {
                return null;
            }
        }
        return null;
    }


    private int countOfMode(boolean data) {
        int count = 0;
        for (Node p = head; p != null; ) {
            if (!p.isMatched()) {
                if (p.isData != data) {
                    return 0;
                }
                if (++count == Integer.MAX_VALUE) {
                    break;
                }
            }
            Node n = p.next;
            if (n != p) {
                p = n;
            } else {
                count = 0;
                p = head;
            }
        }
        return count;
    }

    final class Itr implements Iterator<E> {
        private Node nextNode;
        private E nextItem;
        private Node lastRet;
        private Node lastPred;


        private void advance(Node prev) {


            Node r, b;
            if ((r = lastRet) != null && !r.isMatched()) {
                lastPred = r;
            } else if ((b = lastPred) == null || b.isMatched()) {
                lastPred = null;
            } else {
                Node s, n;
                while ((s = b.next) != null &&
                        s != b && s.isMatched() &&
                        (n = s.next) != null && n != s) {
                    b.casNext(s, n);
                }
            }

            this.lastRet = prev;

            for (Node p = prev, s, n;;) {
                s = (p == null) ? head : p.next;
                if (s == null) {
                    break;
                } else if (s == p) {
                    p = null;
                    continue;
                }
                Object item = s.item;
                if (s.isData) {
                    if (item != null && item != s) {
                        nextItem = MyLinkedTransferQueue.<E>cast(item);
                        nextNode = s;
                        return;
                    }
                }
                else if (item == null) {
                    break;
                }

                if (p == null) {
                    p = s;
                } else if ((n = s.next) == null) {
                    break;
                } else if (s == n) {
                    p = null;
                } else {
                    p.casNext(s, n);
                }
            }
            nextNode = null;
            nextItem = null;
        }

        Itr() {
            advance(null);
        }
        @Override
        public final boolean hasNext() {
            return nextNode != null;
        }
        @Override
        public final E next() {
            Node p = nextNode;
            if (p == null) {
                throw new NoSuchElementException();
            }
            E e = nextItem;
            advance(p);
            return e;
        }
        @Override
        public final void remove() {
            final Node lastRet = this.lastRet;
            if (lastRet == null) {
                throw new IllegalStateException();
            }
            this.lastRet = null;
            if (lastRet.tryMatchData()) {
                unsplice(lastPred, lastRet);
            }
        }
    }


    static final class LTQSpliterator<E> implements Spliterator<E> {
        static final int MAX_BATCH = 1 << 25;
        final MyLinkedTransferQueue<E> queue;
        Node current;
        int batch;
        boolean exhausted;
        LTQSpliterator(MyLinkedTransferQueue<E> queue) {
            this.queue = queue;
        }
        @Override
        public Spliterator<E> trySplit() {
            Node p;
            final MyLinkedTransferQueue<E> q = this.queue;
            int b = batch;
            int n = (b <= 0) ? 1 : (b >= MAX_BATCH) ? MAX_BATCH : b + 1;
            if (!exhausted &&
                    ((p = current) != null || (p = q.firstDataNode()) != null) &&
                    p.next != null) {
                Object[] a = new Object[n];
                int i = 0;
                do {
                    Object e = p.item;
                    if (e != p && (a[i] = e) != null) {
                        ++i;
                    }
                    if (p == (p = p.next)) {
                        p = q.firstDataNode();
                    }
                } while (p != null && i < n && p.isData);
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
        @SuppressWarnings("unchecked")
        public void forEachRemaining(Consumer<? super E> action) {
            Node p;
            if (action == null) {
                throw new NullPointerException();
            }
            final MyLinkedTransferQueue<E> q = this.queue;
            if (!exhausted &&
                    ((p = current) != null || (p = q.firstDataNode()) != null)) {
                exhausted = true;
                do {
                    Object e = p.item;
                    if (e != null && e != p) {
                        action.accept((E)e);
                    }
                    if (p == (p = p.next)) {
                        p = q.firstDataNode();
                    }
                } while (p != null && p.isData);
            }
        }
        @Override
        @SuppressWarnings("unchecked")
        public boolean tryAdvance(Consumer<? super E> action) {
            Node p;
            if (action == null) {
                throw new NullPointerException();
            }
            final MyLinkedTransferQueue<E> q = this.queue;
            if (!exhausted &&
                    ((p = current) != null || (p = q.firstDataNode()) != null)) {
                Object e;
                do {
                    if ((e = p.item) == p) {
                        e = null;
                    }
                    if (p == (p = p.next)) {
                        p = q.firstDataNode();
                    }
                } while (e == null && p != null && p.isData);
                if ((current = p) == null) {
                    exhausted = true;
                }
                if (e != null) {
                    action.accept((E)e);
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
        return new LTQSpliterator<E>(this);
    }




    final void unsplice(Node pred, Node s) {
        s.forgetContents();

        if (pred != null && pred != s && pred.next == s) {
            Node n = s.next;
            if (n == null ||
                    (n != s && pred.casNext(s, n) && pred.isMatched())) {
                for (;;) {
                    Node h = head;
                    if (h == pred || h == s || h == null) {
                        return;
                    }
                    if (!h.isMatched()) {
                        break;
                    }
                    Node hn = h.next;
                    if (hn == null) {
                        return;
                    }
                    if (hn != h && casHead(h, hn)) {
                        h.forgetNext();
                    }
                }
                if (pred.next != pred && s.next != s) {
                    for (;;) {
                        int v = sweepVotes;
                        if (v < SWEEP_THRESHOLD) {
                            if (casSweepVotes(v, v + 1)) {
                                break;
                            }
                        }
                        else if (casSweepVotes(v, 0)) {
                            sweep();
                            break;
                        }
                    }
                }
            }
        }
    }


    private void sweep() {
        for (Node p = head, s, n; p != null && (s = p.next) != null; ) {
            if (!s.isMatched()) {
                p = s;
            } else if ((n = s.next) == null) {
                break;
            } else if (s == n) {
                p = head;
            } else {
                p.casNext(s, n);
            }
        }
    }


    private boolean findAndRemove(Object e) {
        if (e != null) {
            for (Node pred = null, p = head; p != null; ) {
                Object item = p.item;
                if (p.isData) {
                    if (item != null && item != p && e.equals(item) &&
                            p.tryMatchData()) {
                        unsplice(pred, p);
                        return true;
                    }
                }
                else if (item == null) {
                    break;
                }
                pred = p;
                if ((p = p.next) == pred) {
                    pred = null;
                    p = head;
                }
            }
        }
        return false;
    }


    public MyLinkedTransferQueue() {
    }


    public MyLinkedTransferQueue(MyCollection<? extends E> c) {
        this();
        addAll(c);
    }

    @Override
    public void put(E e) {
        xfer(e, true, ASYNC, 0);
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) {
        xfer(e, true, ASYNC, 0);
        return true;
    }

    @Override
    public boolean offer(E e) {
        xfer(e, true, ASYNC, 0);
        return true;
    }

    @Override
    public boolean add(E e) {
        xfer(e, true, ASYNC, 0);
        return true;
    }

    @Override
    public boolean tryTransfer(E e) {
        return xfer(e, true, NOW, 0) == null;
    }

    @Override
    public void transfer(E e) throws InterruptedException {
        if (xfer(e, true, SYNC, 0) != null) {
            Thread.interrupted();
            throw new InterruptedException();
        }
    }

    @Override
    public boolean tryTransfer(E e, long timeout, TimeUnit unit)
            throws InterruptedException {
        if (xfer(e, true, TIMED, unit.toNanos(timeout)) == null) {
            return true;
        }
        if (!Thread.interrupted()) {
            return false;
        }
        throw new InterruptedException();
    }
    @Override
    public E take() throws InterruptedException {
        E e = xfer(null, false, SYNC, 0);
        if (e != null) {
            return e;
        }
        Thread.interrupted();
        throw new InterruptedException();
    }
    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E e = xfer(null, false, TIMED, unit.toNanos(timeout));
        if (e != null || !Thread.interrupted()) {
            return e;
        }
        throw new InterruptedException();
    }
    @Override
    public E poll() {
        return xfer(null, false, NOW, 0);
    }

    @Override
    public int drainTo(MyCollection<? super E> c) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        int n = 0;
        for (E e; (e = poll()) != null;) {
            c.add(e);
            ++n;
        }
        return n;
    }

    @Override
    public int drainTo(MyCollection<? super E> c, int maxElements) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        int n = 0;
        for (E e; n < maxElements && (e = poll()) != null;) {
            c.add(e);
            ++n;
        }
        return n;
    }

    @Override
    public Iterator<E> iterator() {
        return new Itr();
    }
    @Override
    public E peek() {
        return firstDataItem();
    }

    @Override
    public boolean isEmpty() {
        for (Node p = head; p != null; p = succ(p)) {
            if (!p.isMatched()) {
                return !p.isData;
            }
        }
        return true;
    }
    @Override
    public boolean hasWaitingConsumer() {
        return firstOfMode(false) != null;
    }

    @Override
    public int size() {
        return countOfMode(true);
    }
    @Override
    public int getWaitingConsumerCount() {
        return countOfMode(false);
    }

    @Override
    public boolean remove(Object o) {
        return findAndRemove(o);
    }

    @Override
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        for (Node p = head; p != null; p = succ(p)) {
            Object item = p.item;
            if (p.isData) {
                if (item != null && item != p && o.equals(item)) {
                    return true;
                }
            }
            else if (item == null) {
                break;
            }
        }
        return false;
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }


    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {
        s.defaultWriteObject();
        for (E e : this) {
            s.writeObject(e);
        }

        s.writeObject(null);
    }


    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        for (;;) {
            @SuppressWarnings("unchecked")
            E item = (E) s.readObject();
            if (item == null) {
                break;
            } else {
                offer(item);
            }
        }
    }

    private static final sun.misc.Unsafe UNSAFE;
    private static final long headOffset;
    private static final long tailOffset;
    private static final long sweepVotesOffset;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> k = MyLinkedTransferQueue.class;
            headOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("head"));
            tailOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("tail"));
            sweepVotesOffset = UNSAFE.objectFieldOffset
                    (k.getDeclaredField("sweepVotes"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
