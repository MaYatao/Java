package example.container.collection.current;

import example.container.collection.MyAbstractQueue;
import example.container.collection.MyBlockingQueue;
import example.container.collection.MyCollection;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;
import java.util.*;
import java.util.Spliterator;
import java.util.Spliterators;


public class MySynchronousQueue<E> extends MyAbstractQueue<E>
        implements MyBlockingQueue<E>, java.io.Serializable {
    private static final long serialVersionUID = -3223113410248163686L;




    abstract static class Transferer<E> {

        abstract E transfer(E e, boolean timed, long nanos);
    }


    static final int NCPUS = Runtime.getRuntime().availableProcessors();


    static final int maxTimedSpins = (NCPUS < 2) ? 0 : 32;


    static final int maxUntimedSpins = maxTimedSpins * 16;


    static final long spinForTimeoutThreshold = 1000L;


    static final class TransferStack<E> extends Transferer<E> {




        static final int REQUEST    = 0;

        static final int DATA       = 1;

        static final int FULFILLING = 2;


        static boolean isFulfilling(int m) { return (m & FULFILLING) != 0; }


        static final class SNode {
            volatile TransferStack.SNode next;
            volatile TransferStack.SNode match;
            volatile Thread waiter;
            Object item;
            int mode;




            SNode(Object item) {
                this.item = item;
            }

            boolean casNext(TransferStack.SNode cmp, TransferStack.SNode val) {
                return cmp == next &&
                        UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
            }


            boolean tryMatch(TransferStack.SNode s) {
                if (match == null &&
                        UNSAFE.compareAndSwapObject(this, matchOffset, null, s)) {
                    Thread w = waiter;
                    if (w != null) {
                        waiter = null;
                        LockSupport.unpark(w);
                    }
                    return true;
                }
                return match == s;
            }


            void tryCancel() {
                UNSAFE.compareAndSwapObject(this, matchOffset, null, this);
            }

            boolean isCancelled() {
                return match == this;
            }


            private static final sun.misc.Unsafe UNSAFE;
            private static final long matchOffset;
            private static final long nextOffset;

            static {
                try {
                    UNSAFE = sun.misc.Unsafe.getUnsafe();
                    Class<?> k = TransferStack.SNode.class;
                    matchOffset = UNSAFE.objectFieldOffset
                            (k.getDeclaredField("match"));
                    nextOffset = UNSAFE.objectFieldOffset
                            (k.getDeclaredField("next"));
                } catch (Exception e) {
                    throw new Error(e);
                }
            }
        }


        volatile TransferStack.SNode head;

        boolean casHead(TransferStack.SNode h, TransferStack.SNode nh) {
            return h == head &&
                    UNSAFE.compareAndSwapObject(this, headOffset, h, nh);
        }


        static TransferStack.SNode snode(TransferStack.SNode s, Object e, TransferStack.SNode next, int mode) {
            if (s == null) {
                s = new SNode(e);
            }
            s.mode = mode;
            s.next = next;
            return s;
        }


        @Override
        @SuppressWarnings("unchecked")
        E transfer(E e, boolean timed, long nanos) {


            TransferStack.SNode s = null;
            int mode = (e == null) ? REQUEST : DATA;

            for (;;) {
                TransferStack.SNode h = head;
                if (h == null || h.mode == mode) {
                    if (timed && nanos <= 0) {
                        if (h != null && h.isCancelled()) {
                            casHead(h, h.next);
                        } else {
                            return null;
                        }
                    } else if (casHead(h, s = snode(s, e, h, mode))) {
                        TransferStack.SNode m = awaitFulfill(s, timed, nanos);
                        if (m == s) {
                            clean(s);
                            return null;
                        }
                        if ((h = head) != null && h.next == s) {
                            casHead(h, s.next);
                        }
                        return (E) ((mode == REQUEST) ? m.item : s.item);
                    }
                } else if (!isFulfilling(h.mode)) {
                    if (h.isCancelled()) {
                        casHead(h, h.next);
                    } else if (casHead(h, s=snode(s, e, h, FULFILLING|mode))) {
                        for (;;) {
                            TransferStack.SNode m = s.next;
                            if (m == null) {
                                casHead(s, null);
                                s = null;
                                break;
                            }
                            TransferStack.SNode mn = m.next;
                            if (m.tryMatch(s)) {
                                casHead(s, mn);
                                return (E) ((mode == REQUEST) ? m.item : s.item);
                            } else {
                                s.casNext(m, mn);
                            }
                        }
                    }
                } else {
                    TransferStack.SNode m = h.next;
                    if (m == null) {
                        casHead(h, null);
                    } else {
                        TransferStack.SNode mn = m.next;
                        if (m.tryMatch(h)) {
                            casHead(h, mn);
                        } else {
                            h.casNext(m, mn);
                        }
                    }
                }
            }
        }


        TransferStack.SNode awaitFulfill(TransferStack.SNode s, boolean timed, long nanos) {

            final long deadline = timed ? System.nanoTime() + nanos : 0L;
            Thread w = Thread.currentThread();
            int spins = (shouldSpin(s) ?
                    (timed ? maxTimedSpins : maxUntimedSpins) : 0);
            for (;;) {
                if (w.isInterrupted()) {
                    s.tryCancel();
                }
                TransferStack.SNode m = s.match;
                if (m != null) {
                    return m;
                }
                if (timed) {
                    nanos = deadline - System.nanoTime();
                    if (nanos <= 0L) {
                        s.tryCancel();
                        continue;
                    }
                }
                if (spins > 0) {
                    spins = shouldSpin(s) ? (spins-1) : 0;
                } else if (s.waiter == null) {
                    s.waiter = w;
                } else if (!timed) {
                    LockSupport.park(this);
                } else if (nanos > spinForTimeoutThreshold) {
                    LockSupport.parkNanos(this, nanos);
                }
            }
        }


        boolean shouldSpin(TransferStack.SNode s) {
            TransferStack.SNode h = head;
            return (h == s || h == null || isFulfilling(h.mode));
        }


        void clean(TransferStack.SNode s) {
            s.item = null;
            s.waiter = null;



            TransferStack.SNode past = s.next;
            if (past != null && past.isCancelled()) {
                past = past.next;
            }


            TransferStack.SNode p;
            while ((p = head) != null && p != past && p.isCancelled()) {
                casHead(p, p.next);
            }


            while (p != null && p != past) {
                TransferStack.SNode n = p.next;
                if (n != null && n.isCancelled()) {
                    p.casNext(n, n.next);
                } else {
                    p = n;
                }
            }
        }


        private static final sun.misc.Unsafe UNSAFE;
        private static final long headOffset;
        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = TransferStack.class;
                headOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("head"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }


    static final class TransferQueue<E> extends Transferer<E> {



        static final class QNode {
            volatile TransferQueue.QNode next;
            volatile Object item;
            volatile Thread waiter;
            final boolean isData;

            QNode(Object item, boolean isData) {
                this.item = item;
                this.isData = isData;
            }

            boolean casNext(TransferQueue.QNode cmp, TransferQueue.QNode val) {
                return next == cmp &&
                        UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
            }

            boolean casItem(Object cmp, Object val) {
                return item == cmp &&
                        UNSAFE.compareAndSwapObject(this, itemOffset, cmp, val);
            }


            void tryCancel(Object cmp) {
                UNSAFE.compareAndSwapObject(this, itemOffset, cmp, this);
            }

            boolean isCancelled() {
                return item == this;
            }


            boolean isOffList() {
                return next == this;
            }


            private static final sun.misc.Unsafe UNSAFE;
            private static final long itemOffset;
            private static final long nextOffset;

            static {
                try {
                    UNSAFE = sun.misc.Unsafe.getUnsafe();
                    Class<?> k = TransferQueue.QNode.class;
                    itemOffset = UNSAFE.objectFieldOffset
                            (k.getDeclaredField("item"));
                    nextOffset = UNSAFE.objectFieldOffset
                            (k.getDeclaredField("next"));
                } catch (Exception e) {
                    throw new Error(e);
                }
            }
        }


        transient volatile TransferQueue.QNode head;

        transient volatile TransferQueue.QNode tail;

        transient volatile TransferQueue.QNode cleanMe;

        TransferQueue() {
            TransferQueue.QNode h = new TransferQueue.QNode(null, false);
            head = h;
            tail = h;
        }


        void advanceHead(TransferQueue.QNode h, TransferQueue.QNode nh) {
            if (h == head &&
                    UNSAFE.compareAndSwapObject(this, headOffset, h, nh)) {
                h.next = h;
            }
        }


        void advanceTail(TransferQueue.QNode t, TransferQueue.QNode nt) {
            if (tail == t) {
                UNSAFE.compareAndSwapObject(this, tailOffset, t, nt);
            }
        }


        boolean casCleanMe(TransferQueue.QNode cmp, TransferQueue.QNode val) {
            return cleanMe == cmp &&
                    UNSAFE.compareAndSwapObject(this, cleanMeOffset, cmp, val);
        }


        @Override
        @SuppressWarnings("unchecked")
        E transfer(E e, boolean timed, long nanos) {


            TransferQueue.QNode s = null;
            boolean isData = (e != null);

            for (;;) {
                TransferQueue.QNode t = tail;
                TransferQueue.QNode h = head;
                if (t == null || h == null) {
                    continue;
                }

                if (h == t || t.isData == isData) {
                    TransferQueue.QNode tn = t.next;
                    if (t != tail) {
                        continue;
                    }
                    if (tn != null) {
                        advanceTail(t, tn);
                        continue;
                    }
                    if (timed && nanos <= 0) {
                        return null;
                    }
                    if (s == null) {
                        s = new QNode(e, isData);
                    }
                    if (!t.casNext(null, s)) {
                        continue;
                    }

                    advanceTail(t, s);
                    Object x = awaitFulfill(s, e, timed, nanos);
                    if (x == s) {
                        clean(t, s);
                        return null;
                    }

                    if (!s.isOffList()) {
                        advanceHead(t, s);
                        if (x != null) {
                            s.item = s;
                        }
                        s.waiter = null;
                    }
                    return (x != null) ? (E)x : e;

                } else {
                    TransferQueue.QNode m = h.next;
                    if (t != tail || m == null || h != head) {
                        continue;
                    }

                    Object x = m.item;
                    if (isData == (x != null) ||
                            x == m ||
                            !m.casItem(x, e)) {
                        advanceHead(h, m);
                        continue;
                    }

                    advanceHead(h, m);
                    LockSupport.unpark(m.waiter);
                    return (x != null) ? (E)x : e;
                }
            }
        }


        Object awaitFulfill(TransferQueue.QNode s, E e, boolean timed, long nanos) {

            final long deadline = timed ? System.nanoTime() + nanos : 0L;
            Thread w = Thread.currentThread();
            int spins = ((head.next == s) ?
                    (timed ? maxTimedSpins : maxUntimedSpins) : 0);
            for (;;) {
                if (w.isInterrupted()) {
                    s.tryCancel(e);
                }
                Object x = s.item;
                if (x != e) {
                    return x;
                }
                if (timed) {
                    nanos = deadline - System.nanoTime();
                    if (nanos <= 0L) {
                        s.tryCancel(e);
                        continue;
                    }
                }
                if (spins > 0) {
                    --spins;
                } else if (s.waiter == null) {
                    s.waiter = w;
                } else if (!timed) {
                    LockSupport.park(this);
                } else if (nanos > spinForTimeoutThreshold) {
                    LockSupport.parkNanos(this, nanos);
                }
            }
        }


        void clean(TransferQueue.QNode pred, TransferQueue.QNode s) {
            s.waiter = null;

            while (pred.next == s) {
                TransferQueue.QNode h = head;
                TransferQueue.QNode hn = h.next;
                if (hn != null && hn.isCancelled()) {
                    advanceHead(h, hn);
                    continue;
                }
                TransferQueue.QNode t = tail;
                if (t == h) {
                    return;
                }
                TransferQueue.QNode tn = t.next;
                if (t != tail) {
                    continue;
                }
                if (tn != null) {
                    advanceTail(t, tn);
                    continue;
                }
                if (s != t) {
                    TransferQueue.QNode sn = s.next;
                    if (sn == s || pred.casNext(s, sn)) {
                        return;
                    }
                }
                TransferQueue.QNode dp = cleanMe;
                if (dp != null) {
                    TransferQueue.QNode d = dp.next;
                    TransferQueue.QNode dn;
                    if (d == null ||
                            d == dp ||
                            !d.isCancelled() ||
                            (d != t &&
                                    (dn = d.next) != null &&
                                    dn != d &&
                                    dp.casNext(d, dn))) {
                        casCleanMe(dp, null);
                    }
                    if (dp == pred) {
                        return;
                    }
                } else if (casCleanMe(null, pred)) {
                    return;
                }
            }
        }

        private static final sun.misc.Unsafe UNSAFE;
        private static final long headOffset;
        private static final long tailOffset;
        private static final long cleanMeOffset;
        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> k = TransferQueue.class;
                headOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("head"));
                tailOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("tail"));
                cleanMeOffset = UNSAFE.objectFieldOffset
                        (k.getDeclaredField("cleanMe"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }


    private transient volatile Transferer<E> transferer;


    public MySynchronousQueue() {
        this(false);
    }


    public MySynchronousQueue(boolean fair) {
        transferer = fair ? new TransferQueue<E>() : new TransferStack<E>();
    }

    @Override
    public void put(E e) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException();
        }
        if (transferer.transfer(e, false, 0) == null) {
            Thread.interrupted();
            throw new InterruptedException();
        }
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit)
            throws InterruptedException {
        if (e == null) {
            throw new NullPointerException();
        }
        if (transferer.transfer(e, true, unit.toNanos(timeout)) != null) {
            return true;
        }
        if (!Thread.interrupted()) {
            return false;
        }
        throw new InterruptedException();
    }

    @Override
    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        return transferer.transfer(e, true, 0) != null;
    }

    @Override
    public E take() throws InterruptedException {
        E e = transferer.transfer(null, false, 0);
        if (e != null) {
            return e;
        }
        Thread.interrupted();
        throw new InterruptedException();
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        E e = transferer.transfer(null, true, unit.toNanos(timeout));
        if (e != null || !Thread.interrupted()) {
            return e;
        }
        throw new InterruptedException();
    }

    @Override
    public E poll() {
        return transferer.transfer(null, true, 0);
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public int remainingCapacity() {
        return 0;
    }

    @Override
    public void clear() {
    }

    @Override
    public boolean contains(Object o) {
        return false;
    }

    @Override
    public boolean remove(Object o) {
        return false;
    }

    @Override
    public boolean containsAll(MyCollection<?> c) {
        return c.isEmpty();
    }

    @Override
    public boolean removeAll(MyCollection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(MyCollection<?> c) {
        return false;
    }

    @Override
    public E peek() {
        return null;
    }

    @Override
    public Iterator<E> iterator() {
        return Collections.emptyIterator();
    }

    @Override
    public Spliterator<E> spliterator() {
        return Spliterators.emptySpliterator();
    }

    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public <T> T[] toArray(T[] a) {
        if (a.length > 0) {
            a[0] = null;
        }
        return a;
    }

    @Override
    public int drainTo(MyCollection<? super E> c) {
        if (c == null)
            throw new NullPointerException();
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
        if (c == null)
            throw new NullPointerException();
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



    @SuppressWarnings("serial")
    static class WaitQueue implements java.io.Serializable { }
    static class LifoWaitQueue extends WaitQueue {
        private static final long serialVersionUID = -3633113410248163686L;
    }
    static class FifoWaitQueue extends WaitQueue {
        private static final long serialVersionUID = -3623113410248163686L;
    }
    private ReentrantLock qlock;
    private WaitQueue waitingProducers;
    private WaitQueue waitingConsumers;


    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {
        boolean fair = transferer instanceof TransferQueue;
        if (fair) {
            qlock = new ReentrantLock(true);
            waitingProducers = new FifoWaitQueue();
            waitingConsumers = new FifoWaitQueue();
        }
        else {
            qlock = new ReentrantLock();
            waitingProducers = new LifoWaitQueue();
            waitingConsumers = new LifoWaitQueue();
        }
        s.defaultWriteObject();
    }


    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        if (waitingProducers instanceof FifoWaitQueue) {
            transferer = new TransferQueue<E>();
        } else {
            transferer = new TransferStack<E>();
        }
    }


    static long objectFieldOffset(sun.misc.Unsafe UNSAFE,
                                  String field, Class<?> klazz) {
        try {
            return UNSAFE.objectFieldOffset(klazz.getDeclaredField(field));
        } catch (NoSuchFieldException e) {

            NoSuchFieldError error = new NoSuchFieldError(field);
            error.initCause(e);
            throw error;
        }
    }

}
