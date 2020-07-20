

package example.container.collection;

import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


import sun.misc.SharedSecrets;


public class MyCollections {
    // Suppresses default constructor, ensuring non-instantiability.
    private MyCollections() {
    }


    private static final int BINARYSEARCH_THRESHOLD = 5000;
    private static final int REVERSE_THRESHOLD = 18;
    private static final int SHUFFLE_THRESHOLD = 5;
    private static final int FILL_THRESHOLD = 25;
    private static final int ROTATE_THRESHOLD = 100;
    private static final int COPY_THRESHOLD = 10;
    private static final int REPLACEALL_THRESHOLD = 11;
    private static final int INDEXOFSUBLIST_THRESHOLD = 35;

    @SuppressWarnings("unchecked")
    public static <T extends Comparable<? super T>> void sort(MyList<T> list) {
        list.sort(null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> void sort(MyList<T> list, Comparator<? super T> c) {
        list.sort(c);
    }


    public static <T>
    int binarySearch(MyList<? extends Comparable<? super T>> list, T key) {
        if (list instanceof RandomAccess || list.size() < BINARYSEARCH_THRESHOLD) {
            return MyCollections.indexedBinarySearch(list, key);
        } else {
            return MyCollections.iteratorBinarySearch(list, key);
        }
    }

    private static <T>
    int indexedBinarySearch(MyList<? extends Comparable<? super T>> list, T key) {
        int low = 0;
        int high = list.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            Comparable<? super T> midVal = list.get(mid);
            int cmp = midVal.compareTo(key);

            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid; // key found
            }
        }
        return -(low + 1);  // key not found
    }

    private static <T>
    int iteratorBinarySearch(MyList<? extends Comparable<? super T>> list, T key) {
        int low = 0;
        int high = list.size() - 1;
        ListIterator<? extends Comparable<? super T>> i = list.listIterator();

        while (low <= high) {
            int mid = (low + high) >>> 1;
            Comparable<? super T> midVal = get(i, mid);
            int cmp = midVal.compareTo(key);

            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid; // key found
            }
        }
        return -(low + 1);  // key not found
    }


    private static <T> T get(ListIterator<? extends T> i, int index) {
        T obj = null;
        int pos = i.nextIndex();
        if (pos <= index) {
            do {
                obj = i.next();
            } while (pos++ < index);
        } else {
            do {
                obj = i.previous();
            } while (--pos > index);
        }
        return obj;
    }

    @SuppressWarnings("unchecked")
    public static <T> int binarySearch(MyList<? extends T> list, T key, Comparator<? super T> c) {
        if (c == null) {
            return binarySearch((MyList<? extends Comparable<? super T>>) list, key);
        }

        if (list instanceof RandomAccess || list.size() < BINARYSEARCH_THRESHOLD) {
            return MyCollections.indexedBinarySearch(list, key, c);
        } else {
            return MyCollections.iteratorBinarySearch(list, key, c);
        }
    }

    private static <T> int indexedBinarySearch(MyList<? extends T> l, T key, Comparator<? super T> c) {
        int low = 0;
        int high = l.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            T midVal = l.get(mid);
            int cmp = c.compare(midVal, key);

            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid; // key found
            }
        }
        return -(low + 1);  // key not found
    }

    private static <T> int iteratorBinarySearch(MyList<? extends T> l, T key, Comparator<? super T> c) {
        int low = 0;
        int high = l.size() - 1;
        ListIterator<? extends T> i = l.listIterator();

        while (low <= high) {
            int mid = (low + high) >>> 1;
            T midVal = get(i, mid);
            int cmp = c.compare(midVal, key);

            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid; // key found
            }
        }
        return -(low + 1);  // key not found
    }


    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void reverse(MyList<?> list) {
        int size = list.size();
        if (size < REVERSE_THRESHOLD || list instanceof RandomAccess) {
            for (int i = 0, mid = size >> 1, j = size - 1; i < mid; i++, j--) {
                swap(list, i, j);
            }
        } else {

            ListIterator fwd = list.listIterator();
            ListIterator rev = list.listIterator(size);
            for (int i = 0, mid = list.size() >> 1; i < mid; i++) {
                Object tmp = fwd.next();
                fwd.set(rev.previous());
                rev.set(tmp);
            }
        }
    }

    public static void shuffle(MyList<?> list) {
        Random rnd = r;
        if (rnd == null)
            r = rnd = new Random(); // harmless race.
        shuffle(list, rnd);
    }

    private static Random r;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void shuffle(MyList<?> list, Random rnd) {
        int size = list.size();
        if (size < SHUFFLE_THRESHOLD || list instanceof RandomAccess) {
            for (int i = size; i > 1; i--) {
                swap(list, i - 1, rnd.nextInt(i));
            }
        } else {
            Object[] arr = list.toArray();

            // Shuffle array
            for (int i = size; i > 1; i--) {
                swap(arr, i - 1, rnd.nextInt(i));
            }


            ListIterator it = list.listIterator();
            for (int i = 0; i < arr.length; i++) {
                it.next();
                it.set(arr[i]);
            }
        }
    }


    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void swap(MyList<?> list, int i, int j) {

        final MyList l = list;
        l.set(i, l.set(j, l.get(i)));
    }


    private static void swap(Object[] arr, int i, int j) {
        Object tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }


    public static <T> void fill(MyList<? super T> list, T obj) {
        int size = list.size();

        if (size < FILL_THRESHOLD || list instanceof RandomAccess) {
            for (int i = 0; i < size; i++) {
                list.set(i, obj);
            }
        } else {
            ListIterator<? super T> itr = list.listIterator();
            for (int i = 0; i < size; i++) {
                itr.next();
                itr.set(obj);
            }
        }
    }

    public static <T> void copy(MyList<? super T> dest, MyList<? extends T> src) {
        int srcSize = src.size();
        if (srcSize > dest.size()) {
            throw new IndexOutOfBoundsException("Source does not fit in dest");
        }

        if (srcSize < COPY_THRESHOLD ||
                (src instanceof RandomAccess && dest instanceof RandomAccess)) {
            for (int i = 0; i < srcSize; i++) {
                dest.set(i, src.get(i));
            }
        } else {
            ListIterator<? super T> di = dest.listIterator();
            ListIterator<? extends T> si = src.listIterator();
            for (int i = 0; i < srcSize; i++) {
                di.next();
                di.set(si.next());
            }
        }
    }


    public static <T extends Object & Comparable<? super T>> T min(MyCollection<? extends T> coll) {
        Iterator<? extends T> i = coll.iterator();
        T candidate = i.next();

        while (i.hasNext()) {
            T next = i.next();
            if (next.compareTo(candidate) < 0) {
                candidate = next;
            }
        }
        return candidate;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T min(MyCollection<? extends T> coll, Comparator<? super T> comp) {
        if (comp == null) {
            return (T) min((MyCollection) coll);
        }

        Iterator<? extends T> i = coll.iterator();
        T candidate = i.next();

        while (i.hasNext()) {
            T next = i.next();
            if (comp.compare(next, candidate) < 0) {
                candidate = next;
            }
        }
        return candidate;
    }

    public static <T extends Object & Comparable<? super T>> T max(MyCollection<? extends T> coll) {
        Iterator<? extends T> i = coll.iterator();
        T candidate = i.next();

        while (i.hasNext()) {
            T next = i.next();
            if (next.compareTo(candidate) > 0) {
                candidate = next;
            }
        }
        return candidate;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> T max(MyCollection<? extends T> coll, Comparator<? super T> comp) {
        if (comp == null) {
            return (T) max((MyCollection) coll);
        }

        Iterator<? extends T> i = coll.iterator();
        T candidate = i.next();

        while (i.hasNext()) {
            T next = i.next();
            if (comp.compare(next, candidate) > 0) {
                candidate = next;
            }
        }
        return candidate;
    }

    public static void rotate(MyList<?> list, int distance) {
        if (list instanceof RandomAccess || list.size() < ROTATE_THRESHOLD) {
            rotate1(list, distance);
        } else {
            rotate2(list, distance);
        }
    }

    private static <T> void rotate1(MyList<T> list, int distance) {
        int size = list.size();
        if (size == 0) {
            return;
        }
        distance = distance % size;
        if (distance < 0) {
            distance += size;
        }
        if (distance == 0) {
            return;
        }

        for (int cycleStart = 0, nMoved = 0; nMoved != size; cycleStart++) {
            T displaced = list.get(cycleStart);
            int i = cycleStart;
            do {
                i += distance;
                if (i >= size) {
                    i -= size;
                }
                displaced = list.set(i, displaced);
                nMoved++;
            } while (i != cycleStart);
        }
    }

    private static void rotate2(MyList<?> list, int distance) {
        int size = list.size();
        if (size == 0) {
            return;
        }
        int mid = -distance % size;
        if (mid < 0) {
            mid += size;
        }
        if (mid == 0) {
            return;
        }

        reverse(list.subList(0, mid));
        reverse(list.subList(mid, size));
        reverse(list);
    }


    public static <T> boolean replaceAll(List<T> list, T oldVal, T newVal) {
        boolean result = false;
        int size = list.size();
        if (size < REPLACEALL_THRESHOLD || list instanceof RandomAccess) {
            if (oldVal == null) {
                for (int i = 0; i < size; i++) {
                    if (list.get(i) == null) {
                        list.set(i, newVal);
                        result = true;
                    }
                }
            } else {
                for (int i = 0; i < size; i++) {
                    if (oldVal.equals(list.get(i))) {
                        list.set(i, newVal);
                        result = true;
                    }
                }
            }
        } else {
            ListIterator<T> itr = list.listIterator();
            if (oldVal == null) {
                for (int i = 0; i < size; i++) {
                    if (itr.next() == null) {
                        itr.set(newVal);
                        result = true;
                    }
                }
            } else {
                for (int i = 0; i < size; i++) {
                    if (oldVal.equals(itr.next())) {
                        itr.set(newVal);
                        result = true;
                    }
                }
            }
        }
        return result;
    }


    public static int indexOfSubList(List<?> source, List<?> target) {
        int sourceSize = source.size();
        int targetSize = target.size();
        int maxCandidate = sourceSize - targetSize;

        if (sourceSize < INDEXOFSUBLIST_THRESHOLD ||
                (source instanceof RandomAccess && target instanceof RandomAccess)) {
            nextCand:
            for (int candidate = 0; candidate <= maxCandidate; candidate++) {
                for (int i = 0, j = candidate; i < targetSize; i++, j++) {
                    if (!eq(target.get(i), source.get(j))) {
                        continue nextCand;  // Element mismatch, try next cand
                    }
                }
                return candidate;  // All elements of candidate matched target
            }
        } else {  // Iterator version of above algorithm
            ListIterator<?> si = source.listIterator();
            nextCand:
            for (int candidate = 0; candidate <= maxCandidate; candidate++) {
                ListIterator<?> ti = target.listIterator();
                for (int i = 0; i < targetSize; i++) {
                    if (!eq(ti.next(), si.next())) {
                        // Back up source iterator to next candidate
                        for (int j = 0; j < i; j++) {
                            si.previous();
                        }
                        continue nextCand;
                    }
                }
                return candidate;
            }
        }
        return -1;  // No candidate matched the target
    }


    public static int lastIndexOfSubList(MyList<?> source, MyList<?> target) {
        int sourceSize = source.size();
        int targetSize = target.size();
        int maxCandidate = sourceSize - targetSize;

        if (sourceSize < INDEXOFSUBLIST_THRESHOLD ||
                source instanceof RandomAccess) {   // Index access version
            nextCand:
            for (int candidate = maxCandidate; candidate >= 0; candidate--) {
                for (int i = 0, j = candidate; i < targetSize; i++, j++) {
                    if (!eq(target.get(i), source.get(j))) {
                        continue nextCand;  // Element mismatch, try next cand
                    }
                }
                return candidate;  // All elements of candidate matched target
            }
        } else {  // Iterator version of above algorithm
            if (maxCandidate < 0) {
                return -1;
            }
            ListIterator<?> si = source.listIterator(maxCandidate);
            nextCand:
            for (int candidate = maxCandidate; candidate >= 0; candidate--) {
                ListIterator<?> ti = target.listIterator();
                for (int i = 0; i < targetSize; i++) {
                    if (!eq(ti.next(), si.next())) {
                        if (candidate != 0) {
                            // Back up source iterator to next candidate
                            for (int j = 0; j <= i + 1; j++) {
                                si.previous();
                            }
                        }
                        continue nextCand;
                    }
                }
                return candidate;
            }
        }
        return -1;  // No candidate matched the target
    }


    // Unmodifiable Wrappers


    public static <T> MyCollection<T> unmodifiableCollection(MyCollection<? extends T> c) {
        return new UnmodifiableCollection<>(c);
    }

    /**
     * @serial include
     */
    static class UnmodifiableCollection<E> implements MyCollection<E>, Serializable {
        private static final long serialVersionUID = 1820017752578914078L;

        final MyCollection<? extends E> c;

        UnmodifiableCollection(MyCollection<? extends E> c) {
            if (c == null) {
                throw new NullPointerException();
            }
            this.c = c;
        }

        @Override
        public int size() {
            return c.size();
        }

        @Override
        public boolean isEmpty() {
            return c.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return c.contains(o);
        }

        @Override
        public Object[] toArray() {
            return c.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return c.toArray(a);
        }

        @Override
        public String toString() {
            return c.toString();
        }

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {
                private final Iterator<? extends E> i = c.iterator();

                @Override
                public boolean hasNext() {
                    return i.hasNext();
                }

                @Override
                public E next() {
                    return i.next();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void forEachRemaining(Consumer<? super E> action) {
                    // Use backing collection version
                    i.forEachRemaining(action);
                }
            };
        }

        @Override
        public boolean add(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(MyCollection<?> coll) {
            return c.containsAll(coll);
        }

        @Override
        public boolean addAll(MyCollection<? extends E> coll) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(MyCollection<?> coll) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(MyCollection<?> coll) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        // Override default methods in Collection
        @Override
        public void forEach(Consumer<? super E> action) {
            c.forEach(action);
        }

        @Override
        public boolean removeIf(Predicate<? super E> filter) {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Spliterator<E> spliterator() {
            return (Spliterator<E>) c.spliterator();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Stream<E> stream() {
            return (Stream<E>) c.stream();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Stream<E> parallelStream() {
            return (Stream<E>) c.parallelStream();
        }
    }


    public static <T> MySet<T> unmodifiableSet(MySet<? extends T> s) {
        return new UnmodifiableSet<>(s);
    }

    /**
     * @serial include
     */
    static class UnmodifiableSet<E> extends UnmodifiableCollection<E>
            implements MySet<E>, Serializable {
        private static final long serialVersionUID = -9215047833775013803L;

        UnmodifiableSet(MySet<? extends E> s) {
            super(s);
        }

        @Override
        public boolean equals(Object o) {
            return o == this || c.equals(o);
        }

        @Override
        public int hashCode() {
            return c.hashCode();
        }
    }


    public static <T> MySortedSet<T> unmodifiableSortedSet(MySortedSet<T> s) {
        return new UnmodifiableSortedSet<>(s);
    }

    /**
     * @serial include
     */
    static class UnmodifiableSortedSet<E>
            extends UnmodifiableSet<E>
            implements MySortedSet<E>, Serializable {
        private static final long serialVersionUID = -4929149591599911165L;
        private final MySortedSet<E> ss;

        UnmodifiableSortedSet(MySortedSet<E> s) {
            super(s);
            ss = s;
        }

        @Override
        public Comparator<? super E> comparator() {
            return ss.comparator();
        }

        @Override
        public MySortedSet<E> subSet(E fromElement, E toElement) {
            return new UnmodifiableSortedSet<>(ss.subSet(fromElement, toElement));
        }

        @Override
        public MySortedSet<E> headSet(E toElement) {
            return new UnmodifiableSortedSet<>(ss.headSet(toElement));
        }

        @Override
        public MySortedSet<E> tailSet(E fromElement) {
            return new UnmodifiableSortedSet<>(ss.tailSet(fromElement));
        }

        @Override
        public E first() {
            return ss.first();
        }

        @Override
        public E last() {
            return ss.last();
        }
    }

    public static <T> MyNavigableSet<T> unmodifiableNavigableSet(MyNavigableSet<T> s) {
        return new UnmodifiableNavigableSet<>(s);
    }


    static class UnmodifiableNavigableSet<E>
            extends UnmodifiableSortedSet<E>
            implements MyNavigableSet<E>, Serializable {

        private static final long serialVersionUID = -6027448201786391929L;


        private static class EmptyNavigableSet<E> extends UnmodifiableNavigableSet<E>
                implements Serializable {
            private static final long serialVersionUID = -6291252904449939134L;

            public EmptyNavigableSet() {
                super(new MyTreeSet<E>());
            }

            private Object readResolve() {
                return EMPTY_NAVIGABLE_SET;
            }
        }

        @SuppressWarnings("rawtypes")
        private static final MyNavigableSet<?> EMPTY_NAVIGABLE_SET =
                new EmptyNavigableSet<>();

        /**
         * The instance we are protecting.
         */
        private final MyNavigableSet<E> ns;

        UnmodifiableNavigableSet(MyNavigableSet<E> s) {
            super(s);
            ns = s;
        }

        @Override
        public E lower(E e) {
            return ns.lower(e);
        }

        @Override
        public E floor(E e) {
            return ns.floor(e);
        }

        @Override
        public E ceiling(E e) {
            return ns.ceiling(e);
        }

        @Override
        public E higher(E e) {
            return ns.higher(e);
        }

        @Override
        public E pollFirst() {
            throw new UnsupportedOperationException();
        }

        @Override
        public E pollLast() {
            throw new UnsupportedOperationException();
        }

        @Override
        public MyNavigableSet<E> descendingSet() {
            return new UnmodifiableNavigableSet<>(ns.descendingSet());
        }

        @Override
        public Iterator<E> descendingIterator() {
            return descendingSet().iterator();
        }

        @Override
        public MyNavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
            return new UnmodifiableNavigableSet<>(
                    ns.subSet(fromElement, fromInclusive, toElement, toInclusive));
        }

        @Override
        public MyNavigableSet<E> headSet(E toElement, boolean inclusive) {
            return new UnmodifiableNavigableSet<>(
                    ns.headSet(toElement, inclusive));
        }

        @Override
        public MyNavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return new UnmodifiableNavigableSet<>(
                    ns.tailSet(fromElement, inclusive));
        }
    }


    public static <T> MyList<T> unmodifiableList(MyList<? extends T> list) {
        return (list instanceof RandomAccess ?
                new UnmodifiableRandomAccessList<>(list) :
                new UnmodifiableList<>(list));
    }

    /**
     * @serial include
     */
    static class UnmodifiableList<E> extends UnmodifiableCollection<E>
            implements MyList<E> {
        private static final long serialVersionUID = -283967356065247728L;

        final MyList<? extends E> list;

        UnmodifiableList(MyList<? extends E> list) {
            super(list);
            this.list = list;
        }

        @Override
        public boolean equals(Object o) {
            return o == this || list.equals(o);
        }

        @Override
        public int hashCode() {
            return list.hashCode();
        }

        @Override
        public E get(int index) {
            return list.get(index);
        }

        @Override
        public E set(int index, E element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(int index, E element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public E remove(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int indexOf(Object o) {
            return list.indexOf(o);
        }

        @Override
        public int lastIndexOf(Object o) {
            return list.lastIndexOf(o);
        }

        @Override
        public boolean addAll(int index, MyCollection<? extends E> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void replaceAll(UnaryOperator<E> operator) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void sort(Comparator<? super E> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ListIterator<E> listIterator() {
            return listIterator(0);
        }

        @Override
        public ListIterator<E> listIterator(final int index) {
            return new ListIterator<E>() {
                private final ListIterator<? extends E> i
                        = list.listIterator(index);

                @Override
                public boolean hasNext() {
                    return i.hasNext();
                }

                @Override
                public E next() {
                    return i.next();
                }

                @Override
                public boolean hasPrevious() {
                    return i.hasPrevious();
                }

                @Override
                public E previous() {
                    return i.previous();
                }

                @Override
                public int nextIndex() {
                    return i.nextIndex();
                }

                @Override
                public int previousIndex() {
                    return i.previousIndex();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void set(E e) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void add(E e) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void forEachRemaining(Consumer<? super E> action) {
                    i.forEachRemaining(action);
                }
            };
        }

        @Override
        public MyList<E> subList(int fromIndex, int toIndex) {
            return new UnmodifiableList<>(list.subList(fromIndex, toIndex));
        }


        private Object readResolve() {
            return (list instanceof RandomAccess
                    ? new UnmodifiableRandomAccessList<>(list)
                    : this);
        }
    }

    /**
     * @serial include
     */
    static class UnmodifiableRandomAccessList<E> extends UnmodifiableList<E>
            implements RandomAccess {
        UnmodifiableRandomAccessList(MyList<? extends E> list) {
            super(list);
        }

        @Override
        public MyList<E> subList(int fromIndex, int toIndex) {
            return new UnmodifiableRandomAccessList<>(
                    list.subList(fromIndex, toIndex));
        }

        private static final long serialVersionUID = -2542308836966382001L;


        private Object writeReplace() {
            return new UnmodifiableList<>(list);
        }
    }


    public static <K, V> MyMap<K, V> unmodifiableMap(MyMap<? extends K, ? extends V> m) {
        return new UnmodifiableMap<>(m);
    }

    /**
     * @serial include
     */
    private static class UnmodifiableMap<K, V> implements MyMap<K, V>, Serializable {
        private static final long serialVersionUID = -1034234728574286014L;

        private final MyMap<? extends K, ? extends V> m;

        UnmodifiableMap(MyMap<? extends K, ? extends V> m) {
            if (m == null) {
                throw new NullPointerException();
            }
            this.m = m;
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
        public boolean containsKey(Object key) {
            return m.containsKey(key);
        }

        @Override
        public boolean containsValue(Object val) {
            return m.containsValue(val);
        }

        @Override
        public V get(Object key) {
            return m.get(key);
        }

        @Override
        public V put(K key, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V remove(Object key) {
            throw new UnsupportedOperationException();
        }

        public void putAll(MyMap<? extends K, ? extends V> m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        private transient MySet<K> keySet;
        private transient MySet<MyMap.Entry<K, V>> entrySet;
        private transient MyCollection<V> values;

        @Override
        public MySet<K> keySet() {
            if (keySet == null) {
                keySet = unmodifiableSet(m.keySet());
            }
            return keySet;
        }

        @Override
        public MySet<MyMap.Entry<K, V>> entrySet() {
            if (entrySet == null) {
                entrySet = new UnmodifiableEntrySet<>(m.entrySet());
            }
            return entrySet;
        }

        @Override
        public MyCollection<V> values() {
            if (values == null) {
                values = unmodifiableCollection(m.values());
            }
            return values;
        }

        @Override
        public boolean equals(Object o) {
            return o == this || m.equals(o);
        }

        @Override
        public int hashCode() {
            return m.hashCode();
        }

        public String toString() {
            return m.toString();
        }

        // Override default methods in Map
        @Override
        @SuppressWarnings("unchecked")
        public V getOrDefault(Object k, V defaultValue) {
            // Safe cast as we don't change the value
            return ((Map<K, V>) m).getOrDefault(k, defaultValue);
        }

        @Override
        public void forEach(BiConsumer<? super K, ? super V> action) {
            m.forEach(action);
        }

        @Override
        public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V putIfAbsent(K key, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object key, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean replace(K key, V oldValue, V newValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V replace(K key, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V computeIfPresent(K key,
                                  BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V compute(K key,
                         BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V merge(K key, V value,
                       BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
            throw new UnsupportedOperationException();
        }


        static class UnmodifiableEntrySet<K, V>
                extends UnmodifiableSet<MyMap.Entry<K, V>> {
            private static final long serialVersionUID = 7854390611657943733L;

            @SuppressWarnings({"unchecked", "rawtypes"})
            UnmodifiableEntrySet(MySet<? extends MyMap.Entry<? extends K, ? extends V>> s) {
                // Need to cast to raw in order to work around a limitation in the type system
                super((MySet) s);
            }

            static <K, V> Consumer<MyMap.Entry<K, V>> entryConsumer(Consumer<? super MyMap.Entry<K, V>> action) {
                return e -> action.accept(new UnmodifiableEntry<>(e));
            }

            @Override
            public void forEach(Consumer<? super MyMap.Entry<K, V>> action) {
                Objects.requireNonNull(action);
                c.forEach(entryConsumer(action));
            }

            static final class UnmodifiableEntrySetSpliterator<K, V>
                    implements Spliterator<MyMap.Entry<K, V>> {
                final Spliterator<MyMap.Entry<K, V>> s;

                UnmodifiableEntrySetSpliterator(Spliterator<MyMap.Entry<K, V>> s) {
                    this.s = s;
                }

                @Override
                public boolean tryAdvance(Consumer<? super MyMap.Entry<K, V>> action) {
                    Objects.requireNonNull(action);
                    return s.tryAdvance(entryConsumer(action));
                }

                @Override
                public void forEachRemaining(Consumer<? super MyMap.Entry<K, V>> action) {
                    Objects.requireNonNull(action);
                    s.forEachRemaining(entryConsumer(action));
                }

                @Override
                public Spliterator<MyMap.Entry<K, V>> trySplit() {
                    Spliterator<MyMap.Entry<K, V>> split = s.trySplit();
                    return split == null
                            ? null
                            : new UnmodifiableEntrySetSpliterator<>(split);
                }

                @Override
                public long estimateSize() {
                    return s.estimateSize();
                }

                @Override
                public long getExactSizeIfKnown() {
                    return s.getExactSizeIfKnown();
                }

                @Override
                public int characteristics() {
                    return s.characteristics();
                }

                @Override
                public boolean hasCharacteristics(int characteristics) {
                    return s.hasCharacteristics(characteristics);
                }

                @Override
                public Comparator<? super MyMap.Entry<K, V>> getComparator() {
                    return s.getComparator();
                }
            }

            @Override
            @SuppressWarnings("unchecked")
            public Spliterator<MyMap.Entry<K, V>> spliterator() {
                return new UnmodifiableEntrySetSpliterator<>(
                        (Spliterator<MyMap.Entry<K, V>>) c.spliterator());
            }

            @Override
            public Stream<MyMap.Entry<K, V>> stream() {
                return StreamSupport.stream(spliterator(), false);
            }

            @Override
            public Stream<MyMap.Entry<K, V>> parallelStream() {
                return StreamSupport.stream(spliterator(), true);
            }

            @Override
            public Iterator<MyMap.Entry<K, V>> iterator() {
                return new Iterator<MyMap.Entry<K, V>>() {
                    private final Iterator<? extends MyMap.Entry<? extends K, ? extends V>> i = c.iterator();

                    public boolean hasNext() {
                        return i.hasNext();
                    }

                    public MyMap.Entry<K, V> next() {
                        return new UnmodifiableEntry<>(i.next());
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override
            @SuppressWarnings("unchecked")
            public Object[] toArray() {
                Object[] a = c.toArray();
                for (int i = 0; i < a.length; i++) {
                    a[i] = new UnmodifiableEntry<>((Entry<? extends K, ? extends V>) a[i]);
                }
                return a;
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> T[] toArray(T[] a) {
                Object[] arr = c.toArray(a.length == 0 ? a : Arrays.copyOf(a, 0));

                for (int i = 0; i < arr.length; i++) {
                    arr[i] = new UnmodifiableEntry<>((Entry<? extends K, ? extends V>) arr[i]);
                }

                if (arr.length > a.length) {
                    return (T[]) arr;
                }

                System.arraycopy(arr, 0, a, 0, arr.length);
                if (a.length > arr.length) {
                    a[arr.length] = null;
                }
                return a;
            }


            @Override
            public boolean contains(Object o) {
                if (!(o instanceof MyMap.Entry)) {
                    return false;
                }
                return c.contains(
                        new UnmodifiableEntry<>((MyMap.Entry<?, ?>) o));
            }


            @Override
            public boolean containsAll(MyCollection<?> coll) {
                for (Object e : coll) {
                    if (!contains(e)) // Invokes safe contains() above
                    {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public boolean equals(Object o) {
                if (o == this) {
                    return true;
                }

                if (!(o instanceof Set)) {
                    return false;
                }
                MySet<?> s = (MySet<?>) o;
                if (s.size() != c.size()) {
                    return false;
                }
                return containsAll(s); // Invokes safe containsAll() above
            }


            private static class UnmodifiableEntry<K, V> implements MyMap.Entry<K, V> {
                private MyMap.Entry<? extends K, ? extends V> e;

                UnmodifiableEntry(MyMap.Entry<? extends K, ? extends V> e) {
                    this.e = Objects.requireNonNull(e);
                }

                @Override
                public K getKey() {
                    return e.getKey();
                }

                public V getValue() {
                    return e.getValue();
                }

                @Override
                public V setValue(V value) {
                    throw new UnsupportedOperationException();
                }

                public int hashCode() {
                    return e.hashCode();
                }

                public boolean equals(Object o) {
                    if (this == o) {
                        return true;
                    }
                    if (!(o instanceof Map.Entry)) {
                        return false;
                    }
                    Map.Entry<?, ?> t = (Map.Entry<?, ?>) o;
                    return eq(e.getKey(), t.getKey()) &&
                            eq(e.getValue(), t.getValue());
                }

                public String toString() {
                    return e.toString();
                }
            }
        }
    }


    public static <K, V> MySortedMap<K, V> unmodifiableSortedMap(MySortedMap<K, ? extends V> m) {
        return new UnmodifiableSortedMap<>(m);
    }


    static class UnmodifiableSortedMap<K, V>
            extends UnmodifiableMap<K, V>
            implements MySortedMap<K, V>, Serializable {
        private static final long serialVersionUID = -8806743815996713206L;

        private final MySortedMap<K, ? extends V> sm;

        UnmodifiableSortedMap(MySortedMap<K, ? extends V> m) {
            super(m);
            sm = m;
        }

        @Override
        public Comparator<? super K> comparator() {
            return sm.comparator();
        }

        @Override
        public MySortedMap<K, V> subMap(K fromKey, K toKey) {
            return new UnmodifiableSortedMap<>(sm.subMap(fromKey, toKey));
        }

        @Override
        public MySortedMap<K, V> headMap(K toKey) {
            return new UnmodifiableSortedMap<>(sm.headMap(toKey));
        }

        @Override
        public MySortedMap<K, V> tailMap(K fromKey) {
            return new UnmodifiableSortedMap<>(sm.tailMap(fromKey));
        }

        @Override
        public K firstKey() {
            return sm.firstKey();
        }

        @Override
        public K lastKey() {
            return sm.lastKey();
        }
    }


    public static <K, V> MyNavigableMap<K, V> unmodifiableNavigableMap(MyNavigableMap<K, ? extends V> m) {
        return new UnmodifiableNavigableMap<>(m);
    }


    static class UnmodifiableNavigableMap<K, V>
            extends UnmodifiableSortedMap<K, V>
            implements MyNavigableMap<K, V>, Serializable {
        private static final long serialVersionUID = -4858195264774772197L;


        private static class EmptyNavigableMap<K, V> extends UnmodifiableNavigableMap<K, V>
                implements Serializable {

            private static final long serialVersionUID = -2239321462712562324L;

            EmptyNavigableMap() {
                super(new MyTreeMap<K, V>());
            }

            @Override
            public MyNavigableSet<K> navigableKeySet() {
                return emptyNavigableSet();
            }

            private Object readResolve() {
                return EMPTY_NAVIGABLE_MAP;
            }
        }


        private static final EmptyNavigableMap<?, ?> EMPTY_NAVIGABLE_MAP =
                new EmptyNavigableMap<>();


        private final MyNavigableMap<K, ? extends V> nm;

        UnmodifiableNavigableMap(MyNavigableMap<K, ? extends V> m) {
            super(m);
            nm = m;
        }

        @Override
        public K lowerKey(K key) {
            return nm.lowerKey(key);
        }

        @Override
        public K floorKey(K key) {
            return nm.floorKey(key);
        }

        @Override
        public K ceilingKey(K key) {
            return nm.ceilingKey(key);
        }

        @Override
        public K higherKey(K key) {
            return nm.higherKey(key);
        }

        @SuppressWarnings("unchecked")
        @Override
        public MyMap.Entry<K, V> lowerEntry(K key) {
            MyMap.Entry<K, V> lower = (MyMap.Entry<K, V>) nm.lowerEntry(key);
            return (null != lower)
                    ? new UnmodifiableEntrySet.UnmodifiableEntry<>(lower)
                    : null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public MyMap.Entry<K, V> floorEntry(K key) {
            MyMap.Entry<K, V> floor = (MyMap.Entry<K, V>) nm.floorEntry(key);
            return (null != floor)
                    ? new UnmodifiableEntrySet.UnmodifiableEntry<>(floor)
                    : null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public MyMap.Entry<K, V> ceilingEntry(K key) {
            MyMap.Entry<K, V> ceiling = (MyMap.Entry<K, V>) nm.ceilingEntry(key);
            return (null != ceiling)
                    ? new UnmodifiableEntrySet.UnmodifiableEntry<>(ceiling)
                    : null;
        }


        @SuppressWarnings("unchecked")
        @Override
        public MyMap.Entry<K, V> higherEntry(K key) {
            MyMap.Entry<K, V> higher = (MyMap.Entry<K, V>) nm.higherEntry(key);
            return (null != higher)
                    ? new UnmodifiableEntrySet.UnmodifiableEntry<>(higher)
                    : null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public MyMap.Entry<K, V> firstEntry() {
            MyMap.Entry<K, V> first = (MyMap.Entry<K, V>) nm.firstEntry();
            return (null != first)
                    ? new UnmodifiableEntrySet.UnmodifiableEntry<>(first)
                    : null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public MyMap.Entry<K, V> lastEntry() {
            MyMap.Entry<K, V> last = (MyMap.Entry<K, V>) nm.lastEntry();
            return (null != last)
                    ? new UnmodifiableEntrySet.UnmodifiableEntry<>(last)
                    : null;
        }

        @Override
        public MyMap.Entry<K, V> pollFirstEntry() {
            throw new UnsupportedOperationException();
        }

        @Override
        public MyMap.Entry<K, V> pollLastEntry() {
            throw new UnsupportedOperationException();
        }

        @Override
        public MyNavigableMap<K, V> descendingMap() {
            return unmodifiableNavigableMap(nm.descendingMap());
        }

        @Override
        public MyNavigableSet<K> navigableKeySet() {
            return unmodifiableNavigableSet(nm.navigableKeySet());
        }

        @Override
        public MyNavigableSet<K> descendingKeySet() {
            return unmodifiableNavigableSet(nm.descendingKeySet());
        }

        @Override
        public MyNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
            return unmodifiableNavigableMap(
                    nm.subMap(fromKey, fromInclusive, toKey, toInclusive));
        }

        @Override
        public MyNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
            return unmodifiableNavigableMap(nm.headMap(toKey, inclusive));
        }

        @Override
        public MyNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
            return unmodifiableNavigableMap(nm.tailMap(fromKey, inclusive));
        }
    }

    // Synch Wrappers

    public static <T> MyCollection<T> synchronizedCollection(MyCollection<T> c) {
        return new SynchronizedCollection<>(c);
    }

    static <T> MyCollection<T> synchronizedCollection(MyCollection<T> c, Object mutex) {
        return new SynchronizedCollection<>(c, mutex);
    }

    /**
     * @serial include
     */
    static class SynchronizedCollection<E> implements MyCollection<E>, Serializable {
        private static final long serialVersionUID = 3053995032091335093L;

        final MyCollection<E> c;  // Backing Collection
        final Object mutex;     // Object on which to synchronize

        SynchronizedCollection(MyCollection<E> c) {
            this.c = Objects.requireNonNull(c);
            mutex = this;
        }

        SynchronizedCollection(MyCollection<E> c, Object mutex) {
            this.c = Objects.requireNonNull(c);
            this.mutex = Objects.requireNonNull(mutex);
        }

        @Override
        public int size() {
            synchronized (mutex) {
                return c.size();
            }
        }

        @Override
        public boolean isEmpty() {
            synchronized (mutex) {
                return c.isEmpty();
            }
        }

        @Override
        public boolean contains(Object o) {
            synchronized (mutex) {
                return c.contains(o);
            }
        }

        @Override
        public Object[] toArray() {
            synchronized (mutex) {
                return c.toArray();
            }
        }

        @Override
        public <T> T[] toArray(T[] a) {
            synchronized (mutex) {
                return c.toArray(a);
            }
        }

        @Override
        public Iterator<E> iterator() {
            return c.iterator(); // Must be manually synched by user!
        }

        @Override
        public boolean add(E e) {
            synchronized (mutex) {
                return c.add(e);
            }
        }

        @Override
        public boolean remove(Object o) {
            synchronized (mutex) {
                return c.remove(o);
            }
        }

        @Override
        public boolean containsAll(MyCollection<?> coll) {
            synchronized (mutex) {
                return c.containsAll(coll);
            }
        }

        @Override
        public boolean addAll(MyCollection<? extends E> coll) {
            synchronized (mutex) {
                return c.addAll(coll);
            }
        }

        @Override
        public boolean removeAll(MyCollection<?> coll) {
            synchronized (mutex) {
                return c.removeAll(coll);
            }
        }

        @Override
        public boolean retainAll(MyCollection<?> coll) {
            synchronized (mutex) {
                return c.retainAll(coll);
            }
        }

        @Override
        public void clear() {
            synchronized (mutex) {
                c.clear();
            }
        }

        @Override
        public String toString() {
            synchronized (mutex) {
                return c.toString();
            }
        }

        // Override default methods in Collection
        @Override
        public void forEach(Consumer<? super E> consumer) {
            synchronized (mutex) {
                c.forEach(consumer);
            }
        }

        @Override
        public boolean removeIf(Predicate<? super E> filter) {
            synchronized (mutex) {
                return c.removeIf(filter);
            }
        }

        @Override
        public Spliterator<E> spliterator() {
            return c.spliterator(); // Must be manually synched by user!
        }

        @Override
        public Stream<E> stream() {
            return c.stream(); // Must be manually synched by user!
        }

        @Override
        public Stream<E> parallelStream() {
            return c.parallelStream(); // Must be manually synched by user!
        }

        private void writeObject(ObjectOutputStream s) throws IOException {
            synchronized (mutex) {
                s.defaultWriteObject();
            }
        }
    }

    public static <T> MySet<T> synchronizedSet(MySet<T> s) {
        return new SynchronizedSet<>(s);
    }

    static <T> MySet<T> synchronizedSet(MySet<T> s, Object mutex) {
        return new SynchronizedSet<>(s, mutex);
    }

    /**
     * @serial include
     */
    static class SynchronizedSet<E>
            extends SynchronizedCollection<E>
            implements MySet<E> {
        private static final long serialVersionUID = 487447009682186044L;

        SynchronizedSet(MySet<E> s) {
            super(s);
        }

        SynchronizedSet(MySet<E> s, Object mutex) {
            super(s, mutex);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            synchronized (mutex) {
                return c.equals(o);
            }
        }

        @Override
        public int hashCode() {
            synchronized (mutex) {
                return c.hashCode();
            }
        }
    }

    public static <T> MySortedSet<T> synchronizedSortedSet(MySortedSet<T> s) {
        return new SynchronizedSortedSet<>(s);
    }

    /**
     * @serial include
     */
    static class SynchronizedSortedSet<E>
            extends SynchronizedSet<E>
            implements MySortedSet<E> {
        private static final long serialVersionUID = 8695801310862127406L;

        private final MySortedSet<E> ss;

        SynchronizedSortedSet(MySortedSet<E> s) {
            super(s);
            ss = s;
        }

        SynchronizedSortedSet(MySortedSet<E> s, Object mutex) {
            super(s, mutex);
            ss = s;
        }

        @Override
        public Comparator<? super E> comparator() {
            synchronized (mutex) {
                return ss.comparator();
            }
        }

        @Override
        public MySortedSet<E> subSet(E fromElement, E toElement) {
            synchronized (mutex) {
                return new SynchronizedSortedSet<>(
                        ss.subSet(fromElement, toElement), mutex);
            }
        }

        @Override
        public MySortedSet<E> headSet(E toElement) {
            synchronized (mutex) {
                return new SynchronizedSortedSet<>(ss.headSet(toElement), mutex);
            }
        }

        @Override
        public MySortedSet<E> tailSet(E fromElement) {
            synchronized (mutex) {
                return new SynchronizedSortedSet<>(ss.tailSet(fromElement), mutex);
            }
        }

        @Override
        public E first() {
            synchronized (mutex) {
                return ss.first();
            }
        }

        @Override
        public E last() {
            synchronized (mutex) {
                return ss.last();
            }
        }
    }

    public static <T> MyNavigableSet<T> synchronizedNavigableSet(MyNavigableSet<T> s) {
        return new SynchronizedNavigableSet<>(s);
    }

    /**
     * @serial include
     */
    static class SynchronizedNavigableSet<E>
            extends SynchronizedSortedSet<E>
            implements MyNavigableSet<E> {
        private static final long serialVersionUID = -5505529816273629798L;

        private final MyNavigableSet<E> ns;

        SynchronizedNavigableSet(MyNavigableSet<E> s) {
            super(s);
            ns = s;
        }

        SynchronizedNavigableSet(MyNavigableSet<E> s, Object mutex) {
            super(s, mutex);
            ns = s;
        }

        @Override
        public E lower(E e) {
            synchronized (mutex) {
                return ns.lower(e);
            }
        }

        @Override
        public E floor(E e) {
            synchronized (mutex) {
                return ns.floor(e);
            }
        }

        @Override
        public E ceiling(E e) {
            synchronized (mutex) {
                return ns.ceiling(e);
            }
        }

        @Override
        public E higher(E e) {
            synchronized (mutex) {
                return ns.higher(e);
            }
        }

        @Override
        public E pollFirst() {
            synchronized (mutex) {
                return ns.pollFirst();
            }
        }

        @Override
        public E pollLast() {
            synchronized (mutex) {
                return ns.pollLast();
            }
        }

        @Override
        public MyNavigableSet<E> descendingSet() {
            synchronized (mutex) {
                return new SynchronizedNavigableSet<>(ns.descendingSet(), mutex);
            }
        }

        @Override
        public Iterator<E> descendingIterator() {
            synchronized (mutex) {
                return descendingSet().iterator();
            }
        }

        @Override
        public MyNavigableSet<E> subSet(E fromElement, E toElement) {
            synchronized (mutex) {
                return new SynchronizedNavigableSet<>(ns.subSet(fromElement, true, toElement, false), mutex);
            }
        }

        @Override
        public MyNavigableSet<E> headSet(E toElement) {
            synchronized (mutex) {
                return new SynchronizedNavigableSet<>(ns.headSet(toElement, false), mutex);
            }
        }

        @Override
        public MyNavigableSet<E> tailSet(E fromElement) {
            synchronized (mutex) {
                return new SynchronizedNavigableSet<>(ns.tailSet(fromElement, true), mutex);
            }
        }

        @Override
        public MyNavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
            synchronized (mutex) {
                return new SynchronizedNavigableSet<>(ns.subSet(fromElement, fromInclusive, toElement, toInclusive), mutex);
            }
        }

        @Override
        public MyNavigableSet<E> headSet(E toElement, boolean inclusive) {
            synchronized (mutex) {
                return new SynchronizedNavigableSet<>(ns.headSet(toElement, inclusive), mutex);
            }
        }

        @Override
        public MyNavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            synchronized (mutex) {
                return new SynchronizedNavigableSet<>(ns.tailSet(fromElement, inclusive), mutex);
            }
        }
    }

    public static <T> MyList<T> synchronizedList(MyList<T> list) {
        return (list instanceof RandomAccess ?
                new SynchronizedRandomAccessList<>(list) :
                new SynchronizedList<>(list));
    }

    static <T> MyList<T> synchronizedList(MyList<T> list, Object mutex) {
        return (list instanceof RandomAccess ?
                new SynchronizedRandomAccessList<>(list, mutex) :
                new SynchronizedList<>(list, mutex));
    }


    static class SynchronizedList<E>
            extends SynchronizedCollection<E>
            implements MyList<E> {
        private static final long serialVersionUID = -7754090372962971524L;

        final MyList<E> list;

        SynchronizedList(MyList<E> list) {
            super(list);
            this.list = list;
        }

        SynchronizedList(MyList<E> list, Object mutex) {
            super(list, mutex);
            this.list = list;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            synchronized (mutex) {
                return list.equals(o);
            }
        }

        @Override
        public int hashCode() {
            synchronized (mutex) {
                return list.hashCode();
            }
        }

        @Override
        public E get(int index) {
            synchronized (mutex) {
                return list.get(index);
            }
        }

        @Override
        public E set(int index, E element) {
            synchronized (mutex) {
                return list.set(index, element);
            }
        }

        @Override
        public void add(int index, E element) {
            synchronized (mutex) {
                list.add(index, element);
            }
        }

        @Override
        public E remove(int index) {
            synchronized (mutex) {
                return list.remove(index);
            }
        }

        @Override
        public int indexOf(Object o) {
            synchronized (mutex) {
                return list.indexOf(o);
            }
        }

        @Override
        public int lastIndexOf(Object o) {
            synchronized (mutex) {
                return list.lastIndexOf(o);
            }
        }

        @Override
        public boolean addAll(int index, MyCollection<? extends E> c) {
            synchronized (mutex) {
                return list.addAll(index, c);
            }
        }

        @Override
        public ListIterator<E> listIterator() {
            return list.listIterator(); // Must be manually synched by user
        }

        @Override
        public ListIterator<E> listIterator(int index) {
            return list.listIterator(index); // Must be manually synched by user
        }

        @Override
        public MyList<E> subList(int fromIndex, int toIndex) {
            synchronized (mutex) {
                return new SynchronizedList<>(list.subList(fromIndex, toIndex),
                        mutex);
            }
        }

        @Override
        public void replaceAll(UnaryOperator<E> operator) {
            synchronized (mutex) {
                list.replaceAll(operator);
            }
        }

        @Override
        public void sort(Comparator<? super E> c) {
            synchronized (mutex) {
                list.sort(c);
            }
        }

        private Object readResolve() {
            return (list instanceof RandomAccess
                    ? new SynchronizedRandomAccessList<>(list)
                    : this);
        }
    }

    static class SynchronizedRandomAccessList<E>
            extends SynchronizedList<E>
            implements RandomAccess {

        SynchronizedRandomAccessList(MyList<E> list) {
            super(list);
        }

        SynchronizedRandomAccessList(MyList<E> list, Object mutex) {
            super(list, mutex);
        }

        @Override
        public MyList<E> subList(int fromIndex, int toIndex) {
            synchronized (mutex) {
                return new SynchronizedRandomAccessList<>(
                        list.subList(fromIndex, toIndex), mutex);
            }
        }

        private static final long serialVersionUID = 1530674583602358482L;

        /**
         * Allows instances to be deserialized in pre-1.4 JREs (which do
         * not have SynchronizedRandomAccessList).  SynchronizedList has
         * a readResolve method that inverts this transformation upon
         * deserialization.
         */
        private Object writeReplace() {
            return new SynchronizedList<>(list);
        }
    }

    public static <K, V>  MyMap<K, V> synchronizedMap( MyMap<K, V> m) {
        return new SynchronizedMap<>(m);
    }


    private static class SynchronizedMap<K, V>
            implements MyMap<K, V>, Serializable {
        private static final long serialVersionUID = 1978198479659022715L;

        private final MyMap<K, V> m;     // Backing Map
        final Object mutex;        // Object on which to synchronize

        SynchronizedMap(MyMap<K, V> m) {
            this.m = Objects.requireNonNull(m);
            mutex = this;
        }

        SynchronizedMap(MyMap<K, V> m, Object mutex) {
            this.m = m;
            this.mutex = mutex;
        }

        @Override
        public int size() {
            synchronized (mutex) {
                return m.size();
            }
        }

        @Override
        public boolean isEmpty() {
            synchronized (mutex) {
                return m.isEmpty();
            }
        }

        @Override
        public boolean containsKey(Object key) {
            synchronized (mutex) {
                return m.containsKey(key);
            }
        }

        @Override
        public boolean containsValue(Object value) {
            synchronized (mutex) {
                return m.containsValue(value);
            }
        }

        @Override
        public V get(Object key) {
            synchronized (mutex) {
                return m.get(key);
            }
        }

        @Override
        public V put(K key, V value) {
            synchronized (mutex) {
                return m.put(key, value);
            }
        }

        @Override
        public V remove(Object key) {
            synchronized (mutex) {
                return m.remove(key);
            }
        }

        @Override
        public void putAll(MyMap<? extends K, ? extends V> map) {
            synchronized (mutex) {
                m.putAll(map);
            }
        }

        @Override
        public void clear() {
            synchronized (mutex) {
                m.clear();
            }
        }

        private transient MySet<K> keySet;
        private transient MySet<MyMap.Entry<K, V>> entrySet;
        private transient MyCollection<V> values;

        @Override
        public MySet<K> keySet() {
            synchronized (mutex) {
                if (keySet == null) {
                    keySet = new SynchronizedSet<>(m.keySet(), mutex);
                }
                return keySet;
            }
        }

        @Override
        public MySet<MyMap.Entry<K, V>> entrySet() {
            synchronized (mutex) {
                if (entrySet == null) {
                    entrySet = new SynchronizedSet<>(m.entrySet(), mutex);
                }
                return entrySet;
            }
        }

        @Override
        public MyCollection<V> values() {
            synchronized (mutex) {
                if (values == null) {
                    values = new SynchronizedCollection<>(m.values(), mutex);
                }
                return values;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            synchronized (mutex) {
                return m.equals(o);
            }
        }

        @Override
        public int hashCode() {
            synchronized (mutex) {
                return m.hashCode();
            }
        }

        @Override
        public String toString() {
            synchronized (mutex) {
                return m.toString();
            }
        }

        // Override default methods in Map
        @Override
        public V getOrDefault(Object k, V defaultValue) {
            synchronized (mutex) {
                return m.getOrDefault(k, defaultValue);
            }
        }

        @Override
        public void forEach(BiConsumer<? super K, ? super V> action) {
            synchronized (mutex) {
                m.forEach(action);
            }
        }

        @Override
        public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
            synchronized (mutex) {
                m.replaceAll(function);
            }
        }

        @Override
        public V putIfAbsent(K key, V value) {
            synchronized (mutex) {
                return m.putIfAbsent(key, value);
            }
        }

        @Override
        public boolean remove(Object key, Object value) {
            synchronized (mutex) {
                return m.remove(key, value);
            }
        }

        @Override
        public boolean replace(K key, V oldValue, V newValue) {
            synchronized (mutex) {
                return m.replace(key, oldValue, newValue);
            }
        }

        @Override
        public V replace(K key, V value) {
            synchronized (mutex) {
                return m.replace(key, value);
            }
        }

        @Override
        public V computeIfAbsent(K key,
                                 Function<? super K, ? extends V> mappingFunction) {
            synchronized (mutex) {
                return m.computeIfAbsent(key, mappingFunction);
            }
        }

        @Override
        public V computeIfPresent(K key,
                                  BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            synchronized (mutex) {
                return m.computeIfPresent(key, remappingFunction);
            }
        }

        @Override
        public V compute(K key,
                         BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            synchronized (mutex) {
                return m.compute(key, remappingFunction);
            }
        }

        @Override
        public V merge(K key, V value,
                       BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
            synchronized (mutex) {
                return m.merge(key, value, remappingFunction);
            }
        }

        private void writeObject(ObjectOutputStream s) throws IOException {
            synchronized (mutex) {
                s.defaultWriteObject();
            }
        }
    }

    public static <K, V> MySortedMap<K, V> synchronizedSortedMap(MySortedMap<K, V> m) {
        return new SynchronizedSortedMap<>(m);
    }

    /**
     * @serial include
     */
    static class SynchronizedSortedMap<K, V>
            extends SynchronizedMap<K, V>
            implements MySortedMap<K, V> {
        private static final long serialVersionUID = -8798146769416483793L;

        private final MySortedMap<K, V> sm;

        SynchronizedSortedMap(MySortedMap<K, V> m) {
            super(m);
            sm = m;
        }

        SynchronizedSortedMap(MySortedMap<K, V> m, Object mutex) {
            super(m, mutex);
            sm = m;
        }

        @Override
        public Comparator<? super K> comparator() {
            synchronized (mutex) {
                return sm.comparator();
            }
        }

        @Override
        public MySortedMap<K, V> subMap(K fromKey, K toKey) {
            synchronized (mutex) {
                return new SynchronizedSortedMap<>(
                        sm.subMap(fromKey, toKey), mutex);
            }
        }

        @Override
        public MySortedMap<K, V> headMap(K toKey) {
            synchronized (mutex) {
                return new SynchronizedSortedMap<>(sm.headMap(toKey), mutex);
            }
        }

        @Override
        public MySortedMap<K, V> tailMap(K fromKey) {
            synchronized (mutex) {
                return new SynchronizedSortedMap<>(sm.tailMap(fromKey), mutex);
            }
        }

        @Override
        public K firstKey() {
            synchronized (mutex) {
                return sm.firstKey();
            }
        }

        @Override
        public K lastKey() {
            synchronized (mutex) {
                return sm.lastKey();
            }
        }
    }

    public static <K, V> MyNavigableMap<K, V> synchronizedNavigableMap(MyNavigableMap<K, V> m) {
        return new SynchronizedNavigableMap<>(m);
    }


    static class SynchronizedNavigableMap<K, V>
            extends SynchronizedSortedMap<K, V>
            implements MyNavigableMap<K, V> {
        private static final long serialVersionUID = 699392247599746807L;

        private final MyNavigableMap<K, V> nm;

        SynchronizedNavigableMap(MyNavigableMap<K, V> m) {
            super(m);
            nm = m;
        }

        SynchronizedNavigableMap(MyNavigableMap<K, V> m, Object mutex) {
            super(m, mutex);
            nm = m;
        }

        @Override
        public Entry<K, V> lowerEntry(K key) {
            synchronized (mutex) {
                return nm.lowerEntry(key);
            }
        }

        @Override
        public K lowerKey(K key) {
            synchronized (mutex) {
                return nm.lowerKey(key);
            }
        }

        @Override
        public Entry<K, V> floorEntry(K key) {
            synchronized (mutex) {
                return nm.floorEntry(key);
            }
        }

        @Override
        public K floorKey(K key) {
            synchronized (mutex) {
                return nm.floorKey(key);
            }
        }

        @Override
        public Entry<K, V> ceilingEntry(K key) {
            synchronized (mutex) {
                return nm.ceilingEntry(key);
            }
        }

        @Override
        public K ceilingKey(K key) {
            synchronized (mutex) {
                return nm.ceilingKey(key);
            }
        }

        @Override
        public Entry<K, V> higherEntry(K key) {
            synchronized (mutex) {
                return nm.higherEntry(key);
            }
        }

        @Override
        public K higherKey(K key) {
            synchronized (mutex) {
                return nm.higherKey(key);
            }
        }

        @Override
        public Entry<K, V> firstEntry() {
            synchronized (mutex) {
                return nm.firstEntry();
            }
        }

        @Override
        public Entry<K, V> lastEntry() {
            synchronized (mutex) {
                return nm.lastEntry();
            }
        }

        @Override
        public Entry<K, V> pollFirstEntry() {
            synchronized (mutex) {
                return nm.pollFirstEntry();
            }
        }

        @Override
        public Entry<K, V> pollLastEntry() {
            synchronized (mutex) {
                return nm.pollLastEntry();
            }
        }

        @Override
        public MyNavigableMap<K, V> descendingMap() {
            synchronized (mutex) {
                return
                        new SynchronizedNavigableMap<>(nm.descendingMap(), mutex);
            }
        }

        @Override
        public MyNavigableSet<K> keySet() {
            return navigableKeySet();
        }

        @Override
        public MyNavigableSet<K> navigableKeySet() {
            synchronized (mutex) {
                return new SynchronizedNavigableSet<>(nm.navigableKeySet(), mutex);
            }
        }

        @Override
        public MyNavigableSet<K> descendingKeySet() {
            synchronized (mutex) {
                return new SynchronizedNavigableSet<>(nm.descendingKeySet(), mutex);
            }
        }


        @Override
        public MySortedMap<K, V> subMap(K fromKey, K toKey) {
            synchronized (mutex) {
                return new SynchronizedNavigableMap<>(
                        nm.subMap(fromKey, true, toKey, false), mutex);
            }
        }

        @Override
        public MySortedMap<K, V> headMap(K toKey) {
            synchronized (mutex) {
                return new SynchronizedNavigableMap<>(nm.headMap(toKey, false), mutex);
            }
        }

        @Override
        public MySortedMap<K, V> tailMap(K fromKey) {
            synchronized (mutex) {
                return new SynchronizedNavigableMap<>(nm.tailMap(fromKey, true), mutex);
            }
        }

        @Override
        public MyNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
            synchronized (mutex) {
                return new SynchronizedNavigableMap<>(
                        nm.subMap(fromKey, fromInclusive, toKey, toInclusive), mutex);
            }
        }

        @Override
        public MyNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
            synchronized (mutex) {
                return new SynchronizedNavigableMap<>(
                        nm.headMap(toKey, inclusive), mutex);
            }
        }

        @Override
        public MyNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
            synchronized (mutex) {
                return new SynchronizedNavigableMap<>(
                        nm.tailMap(fromKey, inclusive), mutex);
            }
        }
    }

    // Dynamically typesafe collection wrappers

    public static <E> MyCollection<E> checkedCollection(MyCollection<E> c,
                                                        Class<E> type) {
        return new CheckedCollection<>(c, type);
    }

    @SuppressWarnings("unchecked")
    static <T> T[] zeroLengthArray(Class<T> type) {
        return (T[]) Array.newInstance(type, 0);
    }

    /**
     * @serial include
     */
    static class CheckedCollection<E> implements MyCollection<E>, Serializable {
        private static final long serialVersionUID = 1578914078182001775L;

        final MyCollection<E> c;
        final Class<E> type;

        @SuppressWarnings("unchecked")
        E typeCheck(Object o) {
            if (o != null && !type.isInstance(o)) {
                throw new ClassCastException(badElementMsg(o));
            }
            return (E) o;
        }

        private String badElementMsg(Object o) {
            return "Attempt to insert " + o.getClass() +
                    " element into collection with element type " + type;
        }

        CheckedCollection(MyCollection<E> c, Class<E> type) {
            this.c = Objects.requireNonNull(c, "c");
            this.type = Objects.requireNonNull(type, "type");
        }

        @Override
        public int size() {
            return c.size();
        }

        @Override
        public boolean isEmpty() {
            return c.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return c.contains(o);
        }

        @Override
        public Object[] toArray() {
            return c.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return c.toArray(a);
        }

        @Override
        public String toString() {
            return c.toString();
        }

        @Override
        public boolean remove(Object o) {
            return c.remove(o);
        }

        @Override
        public void clear() {
            c.clear();
        }


        @Override
        public boolean containsAll(MyCollection<?> coll) {
            return c.containsAll(coll);
        }


        @Override
        public boolean removeAll(MyCollection<?> coll) {
            return c.removeAll(coll);
        }

        @Override
        public boolean retainAll(MyCollection<?> coll) {
            return c.retainAll(coll);
        }

        @Override
        public Iterator<E> iterator() {
            // JDK-6363904 - unwrapped iterator could be typecast to
            // ListIterator with unsafe set()
            final Iterator<E> it = c.iterator();
            return new Iterator<E>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public E next() {
                    return it.next();
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };
        }

        @Override
        public boolean add(E e) {
            return c.add(typeCheck(e));
        }

        private E[] zeroLengthElementArray; // Lazily initialized

        private E[] zeroLengthElementArray() {
            return zeroLengthElementArray != null ? zeroLengthElementArray :
                    (zeroLengthElementArray = zeroLengthArray(type));
        }

        @SuppressWarnings("unchecked")
        MyCollection<E> checkedCopyOf(MyCollection<? extends E> coll) {
            Object[] a;
            try {
                E[] z = zeroLengthElementArray();
                a = coll.toArray(z);
                // Defend against coll violating the toArray contract
                if (a.getClass() != z.getClass()) {
                    a = Arrays.copyOf(a, a.length, z.getClass());
                }
            } catch (ArrayStoreException ignore) {
                a = coll.toArray().clone();
                for (Object o : a) {
                    typeCheck(o);
                }
            }
            // A slight abuse of the type system, but safe here.
            return (MyCollection<E>) Arrays.asList(a);
        }

        @Override
        public boolean addAll(MyCollection<? extends E> coll) {

            return c.addAll(checkedCopyOf(coll));
        }

        // Override default methods in Collection
        @Override
        public void forEach(Consumer<? super E> action) {
            c.forEach(action);
        }

        @Override
        public boolean removeIf(Predicate<? super E> filter) {
            return c.removeIf(filter);
        }

        @Override
        public Spliterator<E> spliterator() {
            return c.spliterator();
        }

        @Override
        public Stream<E> stream() {
            return c.stream();
        }

        @Override
        public Stream<E> parallelStream() {
            return c.parallelStream();
        }
    }

    public static <E> MyQueue<E> checkedQueue(MyQueue<E> queue, Class<E> type) {
        return new CheckedQueue<>(queue, type);
    }

    /**
     * @serial include
     */
    static class CheckedQueue<E>
            extends CheckedCollection<E>
            implements MyQueue<E>, Serializable {
        private static final long serialVersionUID = 1433151992604707767L;
        final MyQueue<E> queue;

        CheckedQueue(MyQueue<E> queue, Class<E> elementType) {
            super(queue, elementType);
            this.queue = queue;
        }

        @Override
        public E element() {
            return queue.element();
        }

        @Override
        public boolean equals(Object o) {
            return o == this || c.equals(o);
        }

        @Override
        public int hashCode() {
            return c.hashCode();
        }

        @Override
        public E peek() {
            return queue.peek();
        }

        @Override
        public E poll() {
            return queue.poll();
        }

        @Override
        public E remove() {
            return queue.remove();
        }

        @Override
        public boolean offer(E e) {
            return queue.offer(typeCheck(e));
        }
    }

    public static <E> MySet<E> checkedSet(MySet<E> s, Class<E> type) {
        return new CheckedSet<>(s, type);
    }

    /**
     * @serial include
     */
    static class CheckedSet<E> extends CheckedCollection<E>
            implements MySet<E>, Serializable {
        private static final long serialVersionUID = 4694047833775013803L;

        CheckedSet(MySet<E> s, Class<E> elementType) {
            super(s, elementType);
        }

        @Override
        public boolean equals(Object o) {
            return o == this || c.equals(o);
        }

        @Override
        public int hashCode() {
            return c.hashCode();
        }
    }

    public static <E> MySortedSet<E> checkedSortedSet(MySortedSet<E> s,
                                                      Class<E> type) {
        return new CheckedSortedSet<>(s, type);
    }

    /**
     * @serial include
     */
    static class CheckedSortedSet<E> extends CheckedSet<E>
            implements MySortedSet<E>, Serializable {
        private static final long serialVersionUID = 1599911165492914959L;

        private final MySortedSet<E> ss;

        CheckedSortedSet(MySortedSet<E> s, Class<E> type) {
            super(s, type);
            ss = s;
        }

        @Override
        public Comparator<? super E> comparator() {
            return ss.comparator();
        }

        @Override
        public E first() {
            return ss.first();
        }

        @Override
        public E last() {
            return ss.last();
        }

        @Override
        public MySortedSet<E> subSet(E fromElement, E toElement) {
            return checkedSortedSet(ss.subSet(fromElement, toElement), type);
        }

        @Override
        public MySortedSet<E> headSet(E toElement) {
            return checkedSortedSet(ss.headSet(toElement), type);
        }

        @Override
        public MySortedSet<E> tailSet(E fromElement) {
            return checkedSortedSet(ss.tailSet(fromElement), type);
        }
    }


    public static <E> MyNavigableSet<E> checkedNavigableSet(MyNavigableSet<E> s,
                                                            Class<E> type) {
        return new CheckedNavigableSet<>(s, type);
    }

    /**
     * @serial include
     */
    static class CheckedNavigableSet<E> extends CheckedSortedSet<E>
            implements MyNavigableSet<E>, Serializable {
        private static final long serialVersionUID = -5429120189805438922L;

        private final MyNavigableSet<E> ns;

        CheckedNavigableSet(MyNavigableSet<E> s, Class<E> type) {
            super(s, type);
            ns = s;
        }

        @Override
        public E lower(E e) {
            return ns.lower(e);
        }

        @Override
        public E floor(E e) {
            return ns.floor(e);
        }

        @Override
        public E ceiling(E e) {
            return ns.ceiling(e);
        }

        @Override
        public E higher(E e) {
            return ns.higher(e);
        }

        @Override
        public E pollFirst() {
            return ns.pollFirst();
        }

        @Override
        public E pollLast() {
            return ns.pollLast();
        }

        @Override
        public MyNavigableSet<E> descendingSet() {
            return checkedNavigableSet(ns.descendingSet(), type);
        }

        @Override
        public Iterator<E> descendingIterator() {
            return checkedNavigableSet(ns.descendingSet(), type).iterator();
        }

        @Override
        public MyNavigableSet<E> subSet(E fromElement, E toElement) {
            return checkedNavigableSet(ns.subSet(fromElement, true, toElement, false), type);
        }

        @Override
        public MyNavigableSet<E> headSet(E toElement) {
            return checkedNavigableSet(ns.headSet(toElement, false), type);
        }

        @Override
        public MyNavigableSet<E> tailSet(E fromElement) {
            return checkedNavigableSet(ns.tailSet(fromElement, true), type);
        }

        @Override
        public MyNavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
            return checkedNavigableSet(ns.subSet(fromElement, fromInclusive, toElement, toInclusive), type);
        }

        @Override
        public MyNavigableSet<E> headSet(E toElement, boolean inclusive) {
            return checkedNavigableSet(ns.headSet(toElement, inclusive), type);
        }

        @Override
        public MyNavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return checkedNavigableSet(ns.tailSet(fromElement, inclusive), type);
        }
    }

    public static <E> MyList<E> checkedList(MyList<E> list, Class<E> type) {
        return (list instanceof RandomAccess ?
                new CheckedRandomAccessList<>(list, type) :
                new CheckedList<>(list, type));
    }

    /**
     * @serial include
     */
    static class CheckedList<E>
            extends CheckedCollection<E>
            implements MyList<E> {
        private static final long serialVersionUID = 65247728283967356L;
        final MyList<E> list;

        CheckedList(MyList<E> list, Class<E> type) {
            super(list, type);
            this.list = list;
        }

        @Override
        public boolean equals(Object o) {
            return o == this || list.equals(o);
        }

        @Override
        public int hashCode() {
            return list.hashCode();
        }

        @Override
        public E get(int index) {
            return list.get(index);
        }

        @Override
        public E remove(int index) {
            return list.remove(index);
        }

        @Override
        public int indexOf(Object o) {
            return list.indexOf(o);
        }

        @Override
        public int lastIndexOf(Object o) {
            return list.lastIndexOf(o);
        }

        @Override
        public E set(int index, E element) {
            return list.set(index, typeCheck(element));
        }

        @Override
        public void add(int index, E element) {
            list.add(index, typeCheck(element));
        }

        @Override
        public boolean addAll(int index, MyCollection<? extends E> c) {
            return list.addAll(index, checkedCopyOf(c));
        }

        @Override
        public ListIterator<E> listIterator() {
            return listIterator(0);
        }

        @Override
        public ListIterator<E> listIterator(final int index) {
            final ListIterator<E> i = list.listIterator(index);

            return new ListIterator<E>() {
                @Override
                public boolean hasNext() {
                    return i.hasNext();
                }

                @Override
                public E next() {
                    return i.next();
                }

                @Override
                public boolean hasPrevious() {
                    return i.hasPrevious();
                }

                @Override
                public E previous() {
                    return i.previous();
                }

                @Override
                public int nextIndex() {
                    return i.nextIndex();
                }

                @Override
                public int previousIndex() {
                    return i.previousIndex();
                }

                @Override
                public void remove() {
                    i.remove();
                }

                @Override
                public void set(E e) {
                    i.set(typeCheck(e));
                }

                @Override
                public void add(E e) {
                    i.add(typeCheck(e));
                }

                @Override
                public void forEachRemaining(Consumer<? super E> action) {
                    i.forEachRemaining(action);
                }
            };
        }

        @Override
        public MyList<E> subList(int fromIndex, int toIndex) {
            return new CheckedList<>(list.subList(fromIndex, toIndex), type);
        }


        @Override
        public void replaceAll(UnaryOperator<E> operator) {
            Objects.requireNonNull(operator);
            list.replaceAll(e -> typeCheck(operator.apply(e)));
        }

        @Override
        public void sort(Comparator<? super E> c) {
            list.sort(c);
        }
    }

    /**
     * @serial include
     */
    static class CheckedRandomAccessList<E> extends CheckedList<E>
            implements RandomAccess {
        private static final long serialVersionUID = 1638200125423088369L;

        CheckedRandomAccessList(MyList<E> list, Class<E> type) {
            super(list, type);
        }

        @Override
        public MyList<E> subList(int fromIndex, int toIndex) {
            return new CheckedRandomAccessList<>(
                    list.subList(fromIndex, toIndex), type);
        }
    }

    public static <K, V> MyMap<K, V> checkedMap(MyMap<K, V> m,
                                                Class<K> keyType,
                                                Class<V> valueType) {
        return new CheckedMap<>(m, keyType, valueType);
    }


    private static class CheckedMap<K, V>
            implements MyMap<K, V>, Serializable {
        private static final long serialVersionUID = 5742860141034234728L;

        private final MyMap<K, V> m;
        final Class<K> keyType;
        final Class<V> valueType;

        private void typeCheck(Object key, Object value) {
            if (key != null && !keyType.isInstance(key)) {
                throw new ClassCastException(badKeyMsg(key));
            }

            if (value != null && !valueType.isInstance(value)) {
                throw new ClassCastException(badValueMsg(value));
            }
        }

        private BiFunction<? super K, ? super V, ? extends V> typeCheck(
                BiFunction<? super K, ? super V, ? extends V> func) {
            Objects.requireNonNull(func);
            return (k, v) -> {
                V newValue = func.apply(k, v);
                typeCheck(k, newValue);
                return newValue;
            };
        }

        private String badKeyMsg(Object key) {
            return "Attempt to insert " + key.getClass() +
                    " key into map with key type " + keyType;
        }

        private String badValueMsg(Object value) {
            return "Attempt to insert " + value.getClass() +
                    " value into map with value type " + valueType;
        }

        CheckedMap(MyMap<K, V> m, Class<K> keyType, Class<V> valueType) {
            this.m = Objects.requireNonNull(m);
            this.keyType = Objects.requireNonNull(keyType);
            this.valueType = Objects.requireNonNull(valueType);
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
        public boolean containsKey(Object key) {
            return m.containsKey(key);
        }

        @Override
        public boolean containsValue(Object v) {
            return m.containsValue(v);
        }

        @Override
        public V get(Object key) {
            return m.get(key);
        }

        @Override
        public V remove(Object key) {
            return m.remove(key);
        }

        @Override
        public void clear() {
            m.clear();
        }

        @Override
        public MySet<K> keySet() {
            return m.keySet();
        }

        @Override
        public MyCollection<V> values() {
            return m.values();
        }

        @Override
        public boolean equals(Object o) {
            return o == this || m.equals(o);
        }

        @Override
        public int hashCode() {
            return m.hashCode();
        }

        @Override
        public String toString() {
            return m.toString();
        }

        @Override
        public V put(K key, V value) {
            typeCheck(key, value);
            return m.put(key, value);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void putAll(MyMap<? extends K, ? extends V> t) {

            Object[] entries = t.entrySet().toArray();
            MyList<MyMap.Entry<K, V>> checked = new MyArrayList<>(entries.length);
            for (Object o : entries) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                Object k = e.getKey();
                Object v = e.getValue();
                typeCheck(k, v);
                checked.add(
                        new  MyAbstractMap.SimpleImmutableEntry<>((K) k, (V) v));
            }
            for (MyMap.Entry<K, V> e : checked) {
                m.put(e.getKey(), e.getValue());
            }
        }

        private transient MySet< MyMap.Entry<K, V>> entrySet;

        @Override
        public MySet<MyMap.Entry<K, V>> entrySet() {
            if (entrySet == null) {
                entrySet = new CheckedEntrySet<>(m.entrySet(), valueType);
            }
            return entrySet;
        }

        // Override default methods in Map
        @Override
        public void forEach(BiConsumer<? super K, ? super V> action) {
            m.forEach(action);
        }

        @Override
        public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
            m.replaceAll(typeCheck(function));
        }

        @Override
        public V putIfAbsent(K key, V value) {
            typeCheck(key, value);
            return m.putIfAbsent(key, value);
        }

        @Override
        public boolean remove(Object key, Object value) {
            return m.remove(key, value);
        }

        @Override
        public boolean replace(K key, V oldValue, V newValue) {
            typeCheck(key, newValue);
            return m.replace(key, oldValue, newValue);
        }

        @Override
        public V replace(K key, V value) {
            typeCheck(key, value);
            return m.replace(key, value);
        }

        @Override
        public V computeIfAbsent(K key,
                                 Function<? super K, ? extends V> mappingFunction) {
            Objects.requireNonNull(mappingFunction);
            return m.computeIfAbsent(key, k -> {
                V value = mappingFunction.apply(k);
                typeCheck(k, value);
                return value;
            });
        }

        @Override
        public V computeIfPresent(K key,
                                  BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            return m.computeIfPresent(key, typeCheck(remappingFunction));
        }

        @Override
        public V compute(K key,
                         BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            return m.compute(key, typeCheck(remappingFunction));
        }

        @Override
        public V merge(K key, V value,
                       BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
            Objects.requireNonNull(remappingFunction);
            return m.merge(key, value, (v1, v2) -> {
                V newValue = remappingFunction.apply(v1, v2);
                typeCheck(null, newValue);
                return newValue;
            });
        }


        static class CheckedEntrySet<K, V> implements MySet<MyMap.Entry<K, V>> {
            private final MySet<MyMap.Entry<K, V>> s;
            private final Class<V> valueType;

            CheckedEntrySet(MySet<MyMap.Entry<K, V>> s, Class<V> valueType) {
                this.s = s;
                this.valueType = valueType;
            }

            @Override
            public int size() {
                return s.size();
            }

            @Override
            public boolean isEmpty() {
                return s.isEmpty();
            }

            @Override
            public String toString() {
                return s.toString();
            }

            @Override
            public int hashCode() {
                return s.hashCode();
            }

            @Override
            public void clear() {
                s.clear();
            }

            @Override
            public boolean add(MyMap.Entry<K, V> e) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean addAll(MyCollection<? extends MyMap.Entry<K, V>> coll) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Iterator<MyMap.Entry<K, V>> iterator() {
                final Iterator<MyMap.Entry<K, V>> i = s.iterator();
                final Class<V> valueType = this.valueType;

                return new Iterator<MyMap.Entry<K, V>>() {
                    @Override
                    public boolean hasNext() {
                        return i.hasNext();
                    }

                    @Override
                    public void remove() {
                        i.remove();
                    }

                    @Override
                    public MyMap.Entry<K, V> next() {
                        return checkedEntry(i.next(), valueType);
                    }
                };
            }

            @Override
            @SuppressWarnings("unchecked")
            public Object[] toArray() {
                Object[] source = s.toArray();

                /*
                 * Ensure that we don't get an ArrayStoreException even if
                 * s.toArray returns an array of something other than Object
                 */
                Object[] dest = (CheckedEntry.class.isInstance(
                        source.getClass().getComponentType()) ? source :
                        new Object[source.length]);

                for (int i = 0; i < source.length; i++)
                    dest[i] = checkedEntry(( MyMap.Entry<K, V>) source[i],
                            valueType);
                return dest;
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> T[] toArray(T[] a) {
                // We don't pass a to s.toArray, to avoid window of
                // vulnerability wherein an unscrupulous multithreaded client
                // could get his hands on raw (unwrapped) Entries from s.
                T[] arr = s.toArray(a.length == 0 ? a : Arrays.copyOf(a, 0));

                for (int i = 0; i < arr.length; i++) {
                    arr[i] = (T) checkedEntry((MyMap.Entry<K, V>) arr[i],
                            valueType);
                }
                if (arr.length > a.length) {
                    return arr;
                }

                System.arraycopy(arr, 0, a, 0, arr.length);
                if (a.length > arr.length) {
                    a[arr.length] = null;
                }
                return a;
            }


            @Override
            public boolean contains(Object o) {
                if (!(o instanceof Map.Entry)) {
                    return false;
                }
                MyMap.Entry<?, ?> e = (MyMap.Entry<?, ?>) o;
                return s.contains(
                        (e instanceof CheckedEntry) ? e : checkedEntry(e, valueType));
            }


            @Override
            public boolean containsAll(MyCollection<?> c) {
                for (Object o : c) {
                    if (!contains(o)) // Invokes safe contains() above
                    {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public boolean remove(Object o) {
                if (!(o instanceof  MyMap.Entry)) {
                    return false;
                }
                return s.remove(new  MyAbstractMap.SimpleImmutableEntry
                        <>(( MyMap.Entry<?, ?>) o));
            }

            @Override
            public boolean removeAll( MyCollection<?> c) {
                return batchRemove(c, false);
            }

            @Override
            public boolean retainAll( MyCollection<?> c) {
                return batchRemove(c, true);
            }

            private boolean batchRemove( MyCollection<?> c, boolean complement) {
                Objects.requireNonNull(c);
                boolean modified = false;
                Iterator<MyMap.Entry<K, V>> it = iterator();
                while (it.hasNext()) {
                    if (c.contains(it.next()) != complement) {
                        it.remove();
                        modified = true;
                    }
                }
                return modified;
            }

            @Override
            public boolean equals(Object o) {
                if (o == this) {
                    return true;
                }
                if (!(o instanceof Set)) {
                    return false;
                }
                MySet<?> that = (MySet<?>) o;
                return that.size() == s.size()
                        && containsAll(that); // Invokes safe containsAll() above
            }

            static <K, V, T> CheckedEntry<K, V, T> checkedEntry(MyMap.Entry<K, V> e,
                                                                Class<T> valueType) {
                return new CheckedEntry<>(e, valueType);
            }


            private static class CheckedEntry<K, V, T> implements MyMap.Entry<K, V> {
                private final MyMap.Entry<K, V> e;
                private final Class<T> valueType;

                CheckedEntry(MyMap.Entry<K, V> e, Class<T> valueType) {
                    this.e = Objects.requireNonNull(e);
                    this.valueType = Objects.requireNonNull(valueType);
                }

                @Override
                public K getKey() {
                    return e.getKey();
                }

                @Override
                public V getValue() {
                    return e.getValue();
                }

                @Override
                public int hashCode() {
                    return e.hashCode();
                }

                @Override
                public String toString() {
                    return e.toString();
                }

                @Override
                public V setValue(V value) {
                    if (value != null && !valueType.isInstance(value)) {
                        throw new ClassCastException(badValueMsg(value));
                    }
                    return e.setValue(value);
                }

                private String badValueMsg(Object value) {
                    return "Attempt to insert " + value.getClass() +
                            " value into map with value type " + valueType;
                }

                @Override
                public boolean equals(Object o) {
                    if (o == this) {
                        return true;
                    }
                    if (!(o instanceof Map.Entry)) {
                        return false;
                    }
                    return e.equals(new MyAbstractMap.SimpleImmutableEntry
                            <>((MyMap.Entry<?, ?>) o));
                }
            }
        }
    }

    public static <K, V> MySortedMap<K, V> checkedSortedMap(MySortedMap<K, V> m,
                                                            Class<K> keyType,
                                                            Class<V> valueType) {
        return new CheckedSortedMap<>(m, keyType, valueType);
    }


    static class CheckedSortedMap<K, V> extends CheckedMap<K, V>
            implements MySortedMap<K, V>, Serializable {
        private static final long serialVersionUID = 1599671320688067438L;

        private final MySortedMap<K, V> sm;

        CheckedSortedMap(MySortedMap<K, V> m,
                         Class<K> keyType, Class<V> valueType) {
            super(m, keyType, valueType);
            sm = m;
        }

        @Override
        public Comparator<? super K> comparator() {
            return sm.comparator();
        }

        @Override
        public K firstKey() {
            return sm.firstKey();
        }

        @Override
        public K lastKey() {
            return sm.lastKey();
        }

        @Override
        public MySortedMap<K, V> subMap(K fromKey, K toKey) {
            return checkedSortedMap(sm.subMap(fromKey, toKey),
                    keyType, valueType);
        }

        @Override
        public MySortedMap<K, V> headMap(K toKey) {
            return checkedSortedMap(sm.headMap(toKey), keyType, valueType);
        }

        @Override
        public MySortedMap<K, V> tailMap(K fromKey) {
            return checkedSortedMap(sm.tailMap(fromKey), keyType, valueType);
        }
    }

    public static <K, V> MyNavigableMap<K, V> checkedNavigableMap(MyNavigableMap<K, V> m,
                                                                  Class<K> keyType,
                                                                  Class<V> valueType) {
        return new CheckedNavigableMap<>(m, keyType, valueType);
    }

    /**
     * @serial include
     */
    static class CheckedNavigableMap<K, V> extends CheckedSortedMap<K, V>
            implements MyNavigableMap<K, V>, Serializable {
        private static final long serialVersionUID = -4852462692372534096L;

        private final MyNavigableMap<K, V> nm;

        CheckedNavigableMap(MyNavigableMap<K, V> m,
                            Class<K> keyType, Class<V> valueType) {
            super(m, keyType, valueType);
            nm = m;
        }

        @Override
        public Comparator<? super K> comparator() {
            return nm.comparator();
        }

        @Override
        public K firstKey() {
            return nm.firstKey();
        }

        @Override
        public K lastKey() {
            return nm.lastKey();
        }

        @Override
        public Entry<K, V> lowerEntry(K key) {
            Entry<K, V> lower = nm.lowerEntry(key);
            return (null != lower)
                    ? new CheckedMap.CheckedEntrySet.CheckedEntry<>(lower, valueType)
                    : null;
        }

        @Override
        public K lowerKey(K key) {
            return nm.lowerKey(key);
        }

        @Override
        public Entry<K, V> floorEntry(K key) {
            Entry<K, V> floor = nm.floorEntry(key);
            return (null != floor)
                    ? new CheckedMap.CheckedEntrySet.CheckedEntry<>(floor, valueType)
                    : null;
        }

        @Override
        public K floorKey(K key) {
            return nm.floorKey(key);
        }

        @Override
        public Entry<K, V> ceilingEntry(K key) {
            Entry<K, V> ceiling = nm.ceilingEntry(key);
            return (null != ceiling)
                    ? new CheckedMap.CheckedEntrySet.CheckedEntry<>(ceiling, valueType)
                    : null;
        }

        @Override
        public K ceilingKey(K key) {
            return nm.ceilingKey(key);
        }

        @Override
        public Entry<K, V> higherEntry(K key) {
            Entry<K, V> higher = nm.higherEntry(key);
            return (null != higher)
                    ? new CheckedMap.CheckedEntrySet.CheckedEntry<>(higher, valueType)
                    : null;
        }

        @Override
        public K higherKey(K key) {
            return nm.higherKey(key);
        }

        @Override
        public Entry<K, V> firstEntry() {
            Entry<K, V> first = nm.firstEntry();
            return (null != first)
                    ? new CheckedMap.CheckedEntrySet.CheckedEntry<>(first, valueType)
                    : null;
        }

        @Override
        public Entry<K, V> lastEntry() {
            Entry<K, V> last = nm.lastEntry();
            return (null != last)
                    ? new CheckedMap.CheckedEntrySet.CheckedEntry<>(last, valueType)
                    : null;
        }

        @Override
        public Entry<K, V> pollFirstEntry() {
            Entry<K, V> entry = nm.pollFirstEntry();
            return (null == entry)
                    ? null
                    : new CheckedMap.CheckedEntrySet.CheckedEntry<>(entry, valueType);
        }

        @Override
        public Entry<K, V> pollLastEntry() {
            Entry<K, V> entry = nm.pollLastEntry();
            return (null == entry)
                    ? null
                    : new CheckedMap.CheckedEntrySet.CheckedEntry<>(entry, valueType);
        }

        @Override
        public MyNavigableMap<K, V> descendingMap() {
            return checkedNavigableMap(nm.descendingMap(), keyType, valueType);
        }

        @Override
        public MyNavigableSet<K> keySet() {
            return navigableKeySet();
        }

        @Override
        public MyNavigableSet<K> navigableKeySet() {
            return checkedNavigableSet(nm.navigableKeySet(), keyType);
        }

        @Override
        public MyNavigableSet<K> descendingKeySet() {
            return checkedNavigableSet(nm.descendingKeySet(), keyType);
        }

        @Override
        public MyNavigableMap<K, V> subMap(K fromKey, K toKey) {
            return checkedNavigableMap(nm.subMap(fromKey, true, toKey, false),
                    keyType, valueType);
        }

        @Override
        public MyNavigableMap<K, V> headMap(K toKey) {
            return checkedNavigableMap(nm.headMap(toKey, false), keyType, valueType);
        }

        @Override
        public MyNavigableMap<K, V> tailMap(K fromKey) {
            return checkedNavigableMap(nm.tailMap(fromKey, true), keyType, valueType);
        }

        @Override
        public MyNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
            return checkedNavigableMap(nm.subMap(fromKey, fromInclusive, toKey, toInclusive), keyType, valueType);
        }

        @Override
        public MyNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
            return checkedNavigableMap(nm.headMap(toKey, inclusive), keyType, valueType);
        }

        @Override
        public MyNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
            return checkedNavigableMap(nm.tailMap(fromKey, inclusive), keyType, valueType);
        }
    }


    @SuppressWarnings("unchecked")
    public static <T> Iterator<T> emptyIterator() {
        return (Iterator<T>) EmptyIterator.EMPTY_ITERATOR;
    }

    private static class EmptyIterator<E> implements Iterator<E> {
        static final EmptyIterator<Object> EMPTY_ITERATOR
                = new EmptyIterator<>();

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public E next() {
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new IllegalStateException();
        }

        @Override
        public void forEachRemaining(Consumer<? super E> action) {
            Objects.requireNonNull(action);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> ListIterator<T> emptyListIterator() {
        return (ListIterator<T>) EmptyListIterator.EMPTY_ITERATOR;
    }

    private static class EmptyListIterator<E>
            extends EmptyIterator<E>
            implements ListIterator<E> {
        static final EmptyListIterator<Object> EMPTY_ITERATOR
                = new EmptyListIterator<>();

        @Override
        public boolean hasPrevious() {
            return false;
        }

        @Override
        public E previous() {
            throw new NoSuchElementException();
        }

        @Override
        public int nextIndex() {
            return 0;
        }

        @Override
        public int previousIndex() {
            return -1;
        }

        @Override
        public void set(E e) {
            throw new IllegalStateException();
        }

        @Override
        public void add(E e) {
            throw new UnsupportedOperationException();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Enumeration<T> emptyEnumeration() {
        return (Enumeration<T>) EmptyEnumeration.EMPTY_ENUMERATION;
    }

    private static class EmptyEnumeration<E> implements Enumeration<E> {
        static final EmptyEnumeration<Object> EMPTY_ENUMERATION
                = new EmptyEnumeration<>();

        public boolean hasMoreElements() {
            return false;
        }

        public E nextElement() {
            throw new NoSuchElementException();
        }
    }


    @SuppressWarnings("rawtypes")
    public static final MySet EMPTY_SET = new EmptySet<>();


    @SuppressWarnings("unchecked")
    public static final <T> MySet<T> emptySet() {
        return (MySet<T>) EMPTY_SET;
    }


    private static class EmptySet<E>
            extends MyAbstractSet<E>
            implements Serializable {
        private static final long serialVersionUID = 1582296315990362920L;

        @Override
        public Iterator<E> iterator() {
            return emptyIterator();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean contains(Object obj) {
            return false;
        }

        public boolean containsAll(Collection<?> c) {
            return c.isEmpty();
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

        // Override default methods in Collection
        @Override
        public void forEach(Consumer<? super E> action) {
            Objects.requireNonNull(action);
        }

        @Override
        public boolean removeIf(Predicate<? super E> filter) {
            Objects.requireNonNull(filter);
            return false;
        }

        @Override
        public Spliterator<E> spliterator() {
            return MySpliterators.emptySpliterator();
        }

        private Object readResolve() {
            return EMPTY_SET;
        }
    }

    @SuppressWarnings("unchecked")
    public static <E> MySortedSet<E> emptySortedSet() {
        return (MySortedSet<E>) UnmodifiableNavigableSet.EMPTY_NAVIGABLE_SET;
    }

    @SuppressWarnings("unchecked")
    public static <E> MyNavigableSet<E> emptyNavigableSet() {
        return (MyNavigableSet<E>) UnmodifiableNavigableSet.EMPTY_NAVIGABLE_SET;
    }


    @SuppressWarnings("rawtypes")
    public static final MyList EMPTY_LIST = new EmptyList<>();

    @SuppressWarnings("unchecked")
    public static final <T> MyList<T> emptyList() {
        return (MyList<T>) EMPTY_LIST;
    }


    private static class EmptyList<E>
            extends MyAbstractList<E>
            implements RandomAccess, Serializable {
        private static final long serialVersionUID = 8842843931221139166L;

        @Override
        public Iterator<E> iterator() {
            return emptyIterator();
        }

        @Override
        public ListIterator<E> listIterator() {
            return emptyListIterator();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean contains(Object obj) {
            return false;
        }

        @Override
        public boolean containsAll(MyCollection<?> c) {
            return c.isEmpty();
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
        public E get(int index) {
            throw new IndexOutOfBoundsException("Index: " + index);
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof MyList) && ((MyList<?>) o).isEmpty();
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean removeIf(Predicate<? super E> filter) {
            Objects.requireNonNull(filter);
            return false;
        }

        @Override
        public void replaceAll(UnaryOperator<E> operator) {
            Objects.requireNonNull(operator);
        }

        @Override
        public void sort(Comparator<? super E> c) {
        }

        // Override default methods in Collection
        @Override
        public void forEach(Consumer<? super E> action) {
            Objects.requireNonNull(action);
        }

        @Override
        public Spliterator<E> spliterator() {
            return MySpliterators.emptySpliterator();
        }

        // Preserves singleton property
        private Object readResolve() {
            return EMPTY_LIST;
        }
    }


    @SuppressWarnings("rawtypes")
    public static final MyMap EMPTY_MAP = new EmptyMap<>();

    @SuppressWarnings("unchecked")
    public static final <K, V> MyMap<K, V> emptyMap() {
        return (MyMap<K, V>) EMPTY_MAP;
    }

    @SuppressWarnings("unchecked")
    public static final <K, V> MySortedMap<K, V> emptySortedMap() {
        return (MySortedMap<K, V>) UnmodifiableNavigableMap.EMPTY_NAVIGABLE_MAP;
    }


    @SuppressWarnings("unchecked")
    public static final <K, V> MyNavigableMap<K, V> emptyNavigableMap() {
        return (MyNavigableMap<K, V>) UnmodifiableNavigableMap.EMPTY_NAVIGABLE_MAP;
    }

    /**
     * @serial include
     */
    private static class EmptyMap<K, V>
            extends MyAbstractMap<K, V>
            implements Serializable {
        private static final long serialVersionUID = 6428348081105594320L;

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean containsKey(Object key) {
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public V get(Object key) {
            return null;
        }

        @Override
        public MySet<K> keySet() {
            return emptySet();
        }

        @Override
        public MyCollection<V> values() {
            return emptySet();
        }

        @Override
        public MySet<MyMap.Entry<K, V>> entrySet() {
            return emptySet();
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof MyMap) && ((MyMap<?, ?>) o).isEmpty();
        }

        @Override
        public int hashCode() {
            return 0;
        }

        // Override default methods in Map
        @Override
        @SuppressWarnings("unchecked")
        public V getOrDefault(Object k, V defaultValue) {
            return defaultValue;
        }

        @Override
        public void forEach(BiConsumer<? super K, ? super V> action) {
            Objects.requireNonNull(action);
        }

        @Override
        public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
            Objects.requireNonNull(function);
        }

        @Override
        public V putIfAbsent(K key, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object key, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean replace(K key, V oldValue, V newValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V replace(K key, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V computeIfAbsent(K key,
                                 Function<? super K, ? extends V> mappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V computeIfPresent(K key,
                                  BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V compute(K key,
                         BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V merge(K key, V value,
                       BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
            throw new UnsupportedOperationException();
        }

        // Preserves singleton property
        private Object readResolve() {
            return EMPTY_MAP;
        }
    }

    // Singleton collections


    public static <T> MySet<T> singleton(T o) {
        return new SingletonSet<>(o);
    }

    static <E> Iterator<E> singletonIterator(final E e) {
        return new Iterator<E>() {
            private boolean hasNext = true;

            @Override
            public boolean hasNext() {
                return hasNext;
            }

            @Override
            public E next() {
                if (hasNext) {
                    hasNext = false;
                    return e;
                }
                throw new NoSuchElementException();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void forEachRemaining(Consumer<? super E> action) {
                Objects.requireNonNull(action);
                if (hasNext) {
                    action.accept(e);
                    hasNext = false;
                }
            }
        };
    }


    static <T> Spliterator<T> singletonSpliterator(final T element) {
        return new Spliterator<T>() {
            long est = 1;

            @Override
            public Spliterator<T> trySplit() {
                return null;
            }

            @Override
            public boolean tryAdvance(Consumer<? super T> consumer) {
                Objects.requireNonNull(consumer);
                if (est > 0) {
                    est--;
                    consumer.accept(element);
                    return true;
                }
                return false;
            }

            @Override
            public void forEachRemaining(Consumer<? super T> consumer) {
                tryAdvance(consumer);
            }

            @Override
            public long estimateSize() {
                return est;
            }

            @Override
            public int characteristics() {
                int value = (element != null) ? Spliterator.NONNULL : 0;

                return value | Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE |
                        Spliterator.DISTINCT | Spliterator.ORDERED;
            }
        };
    }

    /**
     * @serial include
     */
    private static class SingletonSet<E>
            extends  MyAbstractSet<E>
            implements Serializable {
        private static final long serialVersionUID = 3193687207550431679L;

        private final E element;

        SingletonSet(E e) {
            element = e;
        }

        public Iterator<E> iterator() {
            return singletonIterator(element);
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean contains(Object o) {
            return eq(o, element);
        }

        // Override default methods for Collection
        @Override
        public void forEach(Consumer<? super E> action) {
            action.accept(element);
        }

        @Override
        public Spliterator<E> spliterator() {
            return singletonSpliterator(element);
        }

        @Override
        public boolean removeIf(Predicate<? super E> filter) {
            throw new UnsupportedOperationException();
        }
    }


    public static <T> List<T> singletonList(T o) {
        return new SingletonList<>(o);
    }


    private static class SingletonList<E>
            extends AbstractList<E>
            implements RandomAccess, Serializable {

        private static final long serialVersionUID = 3093736618740652951L;

        private final E element;

        SingletonList(E obj) {
            element = obj;
        }

        public Iterator<E> iterator() {
            return singletonIterator(element);
        }

        public int size() {
            return 1;
        }

        public boolean contains(Object obj) {
            return eq(obj, element);
        }

        public E get(int index) {
            if (index != 0)
                throw new IndexOutOfBoundsException("Index: " + index + ", Size: 1");
            return element;
        }

        // Override default methods for Collection
        @Override
        public void forEach(Consumer<? super E> action) {
            action.accept(element);
        }

        @Override
        public boolean removeIf(Predicate<? super E> filter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void replaceAll(UnaryOperator<E> operator) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void sort(Comparator<? super E> c) {
        }

        @Override
        public Spliterator<E> spliterator() {
            return singletonSpliterator(element);
        }
    }

    public static <K, V>  MyMap<K, V> singletonMap(K key, V value) {
        return new SingletonMap<>(key, value);
    }

    /**
     * @serial include
     */
    private static class SingletonMap<K, V>
            extends  MyAbstractMap<K, V>
            implements Serializable {
        private static final long serialVersionUID = -6979724477215052911L;

        private final K k;
        private final V v;

        SingletonMap(K key, V value) {
            k = key;
            v = value;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean containsKey(Object key) {
            return eq(key, k);
        }

        @Override
        public boolean containsValue(Object value) {
            return eq(value, v);
        }

        @Override
        public V get(Object key) {
            return (eq(key, k) ? v : null);
        }

        private transient  MySet<K> keySet;
        private transient  MySet< MyMap.Entry<K, V>> entrySet;
        private transient  MyCollection<V> values;

        @Override
        public  MySet<K> keySet() {
            if (keySet == null) {
                keySet = singleton(k);
            }
            return keySet;
        }

        @Override
        public  MySet< MyMap.Entry<K, V>> entrySet() {
            if (entrySet == null) {
                entrySet =  MyCollections.< MyMap.Entry<K, V>>singleton(
                        new SimpleImmutableEntry<>(k, v));
            }
            return entrySet;
        }

        @Override
        public  MyCollection<V> values() {
            if (values == null) {
                values = singleton(v);
            }
            return values;
        }

        // Override default methods in Map
        @Override
        public V getOrDefault(Object key, V defaultValue) {
            return eq(key, k) ? v : defaultValue;
        }

        @Override
        public void forEach(BiConsumer<? super K, ? super V> action) {
            action.accept(k, v);
        }

        @Override
        public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V putIfAbsent(K key, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object key, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean replace(K key, V oldValue, V newValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V replace(K key, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V computeIfAbsent(K key,
                                 Function<? super K, ? extends V> mappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V computeIfPresent(K key,
                                  BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V compute(K key,
                         BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V merge(K key, V value,
                       BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
            throw new UnsupportedOperationException();
        }
    }
    // Miscellaneous

    public static <T> MyList<T> nCopies(int n, T o) {
        if (n < 0) {
            throw new IllegalArgumentException("List length = " + n);
        }
        return new CopiesList<>(n, o);
    }

    /**
     * @serial include
     */
    private static class CopiesList<E>
            extends MyAbstractList<E>
            implements RandomAccess, Serializable {
        private static final long serialVersionUID = 2739099268398711800L;

        final int n;
        final E element;

        CopiesList(int n, E e) {
            assert n >= 0;
            this.n = n;
            element = e;
        }

        @Override
        public int size() {
            return n;
        }

        @Override
        public boolean contains(Object obj) {
            return n != 0 && eq(obj, element);
        }

        @Override
        public int indexOf(Object o) {
            return contains(o) ? 0 : -1;
        }

        @Override
        public int lastIndexOf(Object o) {
            return contains(o) ? n - 1 : -1;
        }

        @Override
        public E get(int index) {
            if (index < 0 || index >= n) {
                throw new IndexOutOfBoundsException("Index: " + index +
                        ", Size: " + n);
            }
            return element;
        }

        @Override
        public Object[] toArray() {
            final Object[] a = new Object[n];
            if (element != null) {
                Arrays.fill(a, 0, n, element);
            }
            return a;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            final int n = this.n;
            if (a.length < n) {
                a = (T[]) java.lang.reflect.Array
                        .newInstance(a.getClass().getComponentType(), n);
                if (element != null) {
                    Arrays.fill(a, 0, n, element);
                }
            } else {
                Arrays.fill(a, 0, n, element);
                if (a.length > n) {
                    a[n] = null;
                }
            }
            return a;
        }

        @Override
        public MyList<E> subList(int fromIndex, int toIndex) {
            if (fromIndex < 0) {
                throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
            }
            if (toIndex > n) {
                throw new IndexOutOfBoundsException("toIndex = " + toIndex);
            }
            if (fromIndex > toIndex) {
                throw new IllegalArgumentException("fromIndex(" + fromIndex +
                        ") > toIndex(" + toIndex + ")");
            }
            return new CopiesList<>(toIndex - fromIndex, element);
        }

        @Override
        public int hashCode() {
            if (n == 0) {
                return 1;
            }
            int pow = 31;
            int sum = 1;
            for (int i = Integer.numberOfLeadingZeros(n) + 1; i < Integer.SIZE; i++) {
                sum *= pow + 1;
                pow *= pow;
                if ((n << i) < 0) {
                    pow *= 31;
                    sum = sum * 31 + 1;
                }
            }
            return pow + sum * (element == null ? 0 : element.hashCode());
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (o instanceof CopiesList) {
                CopiesList<?> other = (CopiesList<?>) o;
                return n == other.n && (n == 0 || eq(element, other.element));
            }
            if (!(o instanceof List)) {
                return false;
            }

            int remaining = n;
            E e = element;
            Iterator<?> itr = ((MyList<?>) o).iterator();
            if (e == null) {
                while (itr.hasNext() && remaining-- > 0) {
                    if (itr.next() != null) {
                        return false;
                    }
                }
            } else {
                while (itr.hasNext() && remaining-- > 0) {
                    if (!e.equals(itr.next())) {
                        return false;
                    }
                }
            }
            return remaining == 0 && !itr.hasNext();
        }

        // Override default methods in Collection
        @Override
        public Stream<E> stream() {
            return IntStream.range(0, n).mapToObj(i -> element);
        }

        @Override
        public Stream<E> parallelStream() {
            return IntStream.range(0, n).parallel().mapToObj(i -> element);
        }

        @Override
        public Spliterator<E> spliterator() {
            return stream().spliterator();
        }

        private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
            ois.defaultReadObject();
            SharedSecrets.getJavaOISAccess().checkArray(ois, Object[].class, n);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Comparator<T> reverseOrder() {
        return (Comparator<T>) ReverseComparator.REVERSE_ORDER;
    }


    private static class ReverseComparator
            implements Comparator<Comparable<Object>>, Serializable {

        private static final long serialVersionUID = 7207038068494060240L;

        static final ReverseComparator REVERSE_ORDER
                = new ReverseComparator();

        @Override
        public int compare(Comparable<Object> c1, Comparable<Object> c2) {
            return c2.compareTo(c1);
        }

        private Object readResolve() {
            return Collections.reverseOrder();
        }

        @Override
        public Comparator<Comparable<Object>> reversed() {
            return Comparator.naturalOrder();
        }
    }


    public static <T> Comparator<T> reverseOrder(Comparator<T> cmp) {
        if (cmp == null) {
            return reverseOrder();
        }

        if (cmp instanceof ReverseComparator2) {
            return ((ReverseComparator2<T>) cmp).cmp;
        }

        return new ReverseComparator2<>(cmp);
    }


    private static class ReverseComparator2<T> implements Comparator<T>,
            Serializable {
        private static final long serialVersionUID = 4374092139857L;


        final Comparator<T> cmp;

        ReverseComparator2(Comparator<T> cmp) {
            assert cmp != null;
            this.cmp = cmp;
        }

        @Override
        public int compare(T t1, T t2) {
            return cmp.compare(t2, t1);
        }

        @Override
        public boolean equals(Object o) {
            return (o == this) ||
                    (o instanceof ReverseComparator2 &&
                            cmp.equals(((ReverseComparator2) o).cmp));
        }

        @Override
        public int hashCode() {
            return cmp.hashCode() ^ Integer.MIN_VALUE;
        }

        @Override
        public Comparator<T> reversed() {
            return cmp;
        }
    }

    public static <T> Enumeration<T> enumeration(final MyCollection<T> c) {
        return new Enumeration<T>() {
            private final Iterator<T> i = c.iterator();

            @Override
            public boolean hasMoreElements() {
                return i.hasNext();
            }

            @Override
            public T nextElement() {
                return i.next();
            }
        };
    }


    public static <T> MyArrayList<T> list(Enumeration<T> e) {
         MyArrayList<T> l = new MyArrayList<>();
        while (e.hasMoreElements()) {
            l.add(e.nextElement());
        }
        return l;
    }


    static boolean eq(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }


    public static int frequency(MyCollection<?> c, Object o) {
        int result = 0;
        if (o == null) {
            for (Object e : c) {
                if (e == null) {
                    result++;
                }
            }
        } else {
            for (Object e : c) {
                if (o.equals(e)) {
                    result++;
                }
            }
        }
        return result;
    }

    public static boolean disjoint(MyCollection<?> c1, MyCollection<?> c2) {

        MyCollection<?> contains = c2;

        MyCollection<?> iterate = c1;


        if (c1 instanceof Set) {
            iterate = c2;
            contains = c1;
        } else if (!(c2 instanceof Set)) {

            int c1size = c1.size();
            int c2size = c2.size();
            if (c1size == 0 || c2size == 0) {
                return true;
            }

            if (c1size > c2size) {
                iterate = c2;
                contains = c1;
            }
        }

        for (Object e : iterate) {
            if (contains.contains(e)) {
                return false;
            }
        }

        // No common elements were found.
        return true;
    }

    @SafeVarargs
    public static <T> boolean addAll(MyCollection<? super T> c, T... elements) {
        boolean result = false;
        for (T element : elements) {
            result |= c.add(element);
        }
        return result;
    }

    public static <E> MySet<E> newSetFromMap(MyMap<E, Boolean> map) {
        return new SetFromMap<>(map);
    }


    private static class SetFromMap<E> extends MyAbstractSet<E>
            implements MySet<E>, Serializable {
        private final MyMap<E, Boolean> m;  // The backing map
        private transient MySet<E> s;       // Its keySet

        SetFromMap(MyMap<E, Boolean> map) {
            if (!map.isEmpty()) {
                throw new IllegalArgumentException("Map is non-empty");
            }
            m = map;
            s = map.keySet();
        }

        @Override
        public void clear() {
            m.clear();
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
        public boolean add(E e) {
            return m.put(e, Boolean.TRUE) == null;
        }

        @Override
        public Iterator<E> iterator() {
            return s.iterator();
        }

        @Override
        public Object[] toArray() {
            return s.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return s.toArray(a);
        }

        @Override
        public String toString() {
            return s.toString();
        }

        @Override
        public int hashCode() {
            return s.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return o == this || s.equals(o);
        }

        @Override
        public boolean containsAll(MyCollection<?> c) {
            return s.containsAll(c);
        }

        @Override
        public boolean removeAll(MyCollection<?> c) {
            return s.removeAll(c);
        }

        @Override
        public boolean retainAll(MyCollection<?> c) {
            return s.retainAll(c);
        }

        @Override
        public void forEach(Consumer<? super E> action) {
            s.forEach(action);
        }

        @Override
        public boolean removeIf(Predicate<? super E> filter) {
            return s.removeIf(filter);
        }

        @Override
        public Spliterator<E> spliterator() {
            return s.spliterator();
        }

        @Override
        public Stream<E> stream() {
            return s.stream();
        }

        @Override
        public Stream<E> parallelStream() {
            return s.parallelStream();
        }

        private static final long serialVersionUID = 2454657854757543876L;

        private void readObject(java.io.ObjectInputStream stream)
                throws IOException, ClassNotFoundException {
            stream.defaultReadObject();
            s = m.keySet();
        }
    }

    public static <T> MyQueue<T> asLifoQueue(MyDeque<T> deque) {
        return new AsLIFOQueue<>(deque);
    }


    static class AsLIFOQueue<E> extends MyAbstractQueue<E>
            implements MyQueue<E>, Serializable {
        private static final long serialVersionUID = 1802017725587941708L;
        private final MyDeque<E> q;

        AsLIFOQueue(MyDeque<E> q) {
            this.q = q;
        }

        @Override
        public boolean add(E e) {
            q.addFirst(e);
            return true;
        }

        @Override
        public boolean offer(E e) {
            return q.offerFirst(e);
        }

        @Override
        public E poll() {
            return q.pollFirst();
        }

        @Override
        public E remove() {
            return q.removeFirst();
        }

        @Override
        public E peek() {
            return q.peekFirst();
        }

        @Override
        public E element() {
            return q.getFirst();
        }

        @Override
        public void clear() {
            q.clear();
        }

        @Override
        public int size() {
            return q.size();
        }

        @Override
        public boolean isEmpty() {
            return q.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return q.contains(o);
        }

        @Override
        public boolean remove(Object o) {
            return q.remove(o);
        }

        @Override
        public Iterator<E> iterator() {
            return q.iterator();
        }

        @Override
        public Object[] toArray() {
            return q.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return q.toArray(a);
        }

        @Override
        public String toString() {
            return q.toString();
        }

        @Override
        public boolean containsAll(MyCollection<?> c) {
            return q.containsAll(c);
        }

        @Override
        public boolean removeAll(MyCollection<?> c) {
            return q.removeAll(c);
        }

        @Override
        public boolean retainAll(MyCollection<?> c) {
            return q.retainAll(c);
        }

        @Override
        public void forEach(Consumer<? super E> action) {
            q.forEach(action);
        }

        @Override
        public boolean removeIf(Predicate<? super E> filter) {
            return q.removeIf(filter);
        }

        @Override
        public Spliterator<E> spliterator() {
            return q.spliterator();
        }

        @Override
        public Stream<E> stream() {
            return q.stream();
        }

        @Override
        public Stream<E> parallelStream() {
            return q.parallelStream();
        }
    }
}
