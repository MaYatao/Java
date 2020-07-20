package example.container.collection;


import sun.misc.SharedSecrets;

import java.util.*;


public class MyEnumMap<K extends Enum<K>, V> extends MyAbstractMap<K, V>
        implements java.io.Serializable, Cloneable {

    private final Class<K> keyType;


    private transient K[] keyUniverse;


    private transient Object[] vals;


    private transient int size = 0;


    private static final Object NULL = new Object() {
        @Override
        public int hashCode() {
            return 0;
        }
        @Override
        public String toString() {
            return "MyEnumMap.NULL";
        }
    };

    private Object maskNull(Object value) {
        return (value == null ? NULL : value);
    }

    @SuppressWarnings("unchecked")
    private V unmaskNull(Object value) {
        return (V) (value == NULL ? null : value);
    }

    private static final Enum<?>[] ZERO_LENGTH_ENUM_ARRAY = new Enum<?>[0];


    public MyEnumMap(Class<K> keyType) {
        this.keyType = keyType;
        keyUniverse = getKeyUniverse(keyType);
        vals = new Object[keyUniverse.length];
    }


    public MyEnumMap(MyEnumMap<K, ? extends V> m) {
        keyType = m.keyType;
        keyUniverse = m.keyUniverse;
        vals = m.vals.clone();
        size = m.size;
    }


    public MyEnumMap(MyMap<K, ? extends V> m) {
        if (m instanceof MyEnumMap) {
            MyEnumMap<K, ? extends V> em = (MyEnumMap<K, ? extends V>) m;
            keyType = em.keyType;
            keyUniverse = em.keyUniverse;
            vals = em.vals.clone();
            size = em.size;
        } else {
            if (m.isEmpty()) {
                throw new IllegalArgumentException("Specified map is empty");
            }
            keyType = m.keySet().iterator().next().getDeclaringClass();
            keyUniverse = getKeyUniverse(keyType);
            vals = new Object[keyUniverse.length];
            putAll(m);
        }
    }


    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean containsValue(Object value) {
        value = maskNull(value);

        for (Object val : vals) {
            if (value.equals(val)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean containsKey(Object key) {
        return isValidKey(key) && vals[((Enum<?>) key).ordinal()] != null;
    }

    private boolean containsMapping(Object key, Object value) {
        return isValidKey(key) &&
                maskNull(value).equals(vals[((Enum<?>) key).ordinal()]);
    }

    @Override
    public V get(Object key) {
        return (isValidKey(key) ?
                unmaskNull(vals[((Enum<?>) key).ordinal()]) : null);
    }


    @Override
    public V put(K key, V value) {
        typeCheck(key);

        int index = key.ordinal();
        Object oldValue = vals[index];
        vals[index] = maskNull(value);
        if (oldValue == null) {
            size++;
        }
        return unmaskNull(oldValue);
    }

    @Override
    public V remove(Object key) {
        if (!isValidKey(key)) {
            return null;
        }
        int index = ((Enum<?>) key).ordinal();
        Object oldValue = vals[index];
        vals[index] = null;
        if (oldValue != null) {
            size--;
        }
        return unmaskNull(oldValue);
    }

    private boolean removeMapping(Object key, Object value) {
        if (!isValidKey(key)) {
            return false;
        }
        int index = ((Enum<?>) key).ordinal();
        if (maskNull(value).equals(vals[index])) {
            vals[index] = null;
            size--;
            return true;
        }
        return false;
    }


    private boolean isValidKey(Object key) {
        if (key == null) {
            return false;
        }


        Class<?> keyClass = key.getClass();
        return keyClass == keyType || keyClass.getSuperclass() == keyType;
    }


    @Override
    public void putAll(MyMap<? extends K, ? extends V> m) {
        if (m instanceof MyEnumMap) {
            MyEnumMap<?, ?> em = (MyEnumMap<?, ?>) m;
            if (em.keyType != keyType) {
                if (em.isEmpty()) {
                    return;
                }
                throw new ClassCastException(em.keyType + " != " + keyType);
            }

            for (int i = 0; i < keyUniverse.length; i++) {
                Object emValue = em.vals[i];
                if (emValue != null) {
                    if (vals[i] == null) {
                        size++;
                    }
                    vals[i] = emValue;
                }
            }
        } else {
            super.putAll(m);
        }
    }

    @Override
    public void clear() {
        Arrays.fill(vals, null);
        size = 0;
    }



    transient MySet<K>        keySet;
    transient MyCollection<V> values;
    private transient MySet<MyMap.Entry<K,V>> entrySet;

    @Override
    public MySet<K> keySet() {
        MySet<K> ks = keySet;
        if (ks == null) {
            ks = new MyEnumMap.KeySet();
            keySet = ks;
        }
        return ks;
    }

    private class KeySet extends MyAbstractSet<K> {

        @Override
        public Iterator<K> iterator() {
            return new MyEnumMap.KeyIterator();
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
            MyEnumMap.this.remove(o);
            return size != oldSize;
        }
        @Override
        public void clear() {
            MyEnumMap.this.clear();
        }
    }

    @Override
    public MyCollection<V> values() {
        MyCollection<V> vs = values;
        if (vs == null) {
            vs = new MyEnumMap.Values();
            values = vs;
        }
        return vs;
    }

    private class Values extends MyAbstractCollection<V> {
        @Override
        public Iterator<V> iterator() {
            return new ValueIterator();
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
            o = maskNull(o);

            for (int i = 0; i < vals.length; i++) {
                if (o.equals(vals[i])) {
                    vals[i] = null;
                    size--;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void clear() {
            MyEnumMap.this.clear();
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

    private class EntrySet extends MyAbstractSet<MyMap.Entry<K, V>> {
        @Override
        public Iterator<MyMap.Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
            return containsMapping(entry.getKey(), entry.getValue());
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
            return removeMapping(entry.getKey(), entry.getValue());
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public void clear() {
            MyEnumMap.this.clear();
        }

        @Override
        public Object[] toArray() {
            return fillEntryArray(new Object[size]);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T[] toArray(T[] a) {
            int size = size();
            if (a.length < size) {
                a = (T[]) java.lang.reflect.Array
                        .newInstance(a.getClass().getComponentType(), size);
            }
            if (a.length > size) {
                a[size] = null;
            }
            return (T[]) fillEntryArray(a);
        }

        private Object[] fillEntryArray(Object[] a) {
            int j = 0;
            for (int i = 0; i < vals.length; i++) {
                if (vals[i] != null) {
                    a[j++] = new SimpleEntry<>(
                            keyUniverse[i], unmaskNull(vals[i]));
                }
            }
            return a;
        }
    }

    private abstract class EnumMapIterator<T> implements Iterator<T> {

        int index = 0;


        int lastReturnedIndex = -1;

        @Override
        public boolean hasNext() {
            while (index < vals.length && vals[index] == null) {
                index++;
            }
            return index != vals.length;
        }

        @Override
        public void remove() {
            checkLastReturnedIndex();

            if (vals[lastReturnedIndex] != null) {
                vals[lastReturnedIndex] = null;
                size--;
            }
            lastReturnedIndex = -1;
        }

        private void checkLastReturnedIndex() {
            if (lastReturnedIndex < 0) {
                throw new IllegalStateException();
            }
        }
    }

    private class KeyIterator extends EnumMapIterator<K> {
        @Override
        public K next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            lastReturnedIndex = index++;
            return keyUniverse[lastReturnedIndex];
        }
    }

    private class ValueIterator extends EnumMapIterator<V> {
        @Override
        public V next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            lastReturnedIndex = index++;
            return unmaskNull(vals[lastReturnedIndex]);
        }
    }

    private class EntryIterator extends EnumMapIterator<Entry<K, V>> {
        private EntryIterator.Entry lastReturnedEntry;

        @Override
        public MyMap.Entry<K, V> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            lastReturnedEntry = new MyEnumMap.EntryIterator.Entry(index++);
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
            public K getKey() {
                checkIndexForEntryUse();
                return keyUniverse[index];
            }

            @Override
            public V getValue() {
                checkIndexForEntryUse();
                return unmaskNull(vals[index]);
            }

            @Override
            public V setValue(V value) {
                checkIndexForEntryUse();
                V oldValue = unmaskNull(vals[index]);
                vals[index] = maskNull(value);
                return oldValue;
            }

            @Override
            public boolean equals(Object o) {
                if (index < 0) {
                    return o == this;
                }

                if (!(o instanceof Map.Entry)) {
                    return false;
                }

                Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
                V ourValue = unmaskNull(vals[index]);
                Object hisValue = e.getValue();
                return (e.getKey() == keyUniverse[index] &&
                        (ourValue == hisValue ||
                                (ourValue != null && ourValue.equals(hisValue))));
            }

            @Override
            public int hashCode() {
                if (index < 0) {
                    return super.hashCode();
                }

                return entryHashCode(index);
            }

            @Override
            public String toString() {
                if (index < 0) {
                    return super.toString();
                }

                return keyUniverse[index] + "="
                        + unmaskNull(vals[index]);
            }

            private void checkIndexForEntryUse() {
                if (index < 0) {
                    throw new IllegalStateException("Entry was removed");
                }
            }
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof MyEnumMap) {
            return equals((MyEnumMap<?, ?>) o);
        }
        if (!(o instanceof Map)) {
            return false;
        }

        Map<?, ?> m = (Map<?, ?>) o;
        if (size != m.size()) {
            return false;
        }

        for (int i = 0; i < keyUniverse.length; i++) {
            if (null != vals[i]) {
                K key = keyUniverse[i];
                V value = unmaskNull(vals[i]);
                if (null == value) {
                    if (!((null == m.get(key)) && m.containsKey(key))) {
                        return false;
                    }
                } else {
                    if (!value.equals(m.get(key))) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean equals(MyEnumMap<?, ?> em) {
        if (em.keyType != keyType) {
            return size == 0 && em.size == 0;
        }


        for (int i = 0; i < keyUniverse.length; i++) {
            Object ourValue = vals[i];
            Object hisValue = em.vals[i];
            if (hisValue != ourValue &&
                    (hisValue == null || !hisValue.equals(ourValue))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = 0;

        for (int i = 0; i < keyUniverse.length; i++) {
            if (null != vals[i]) {
                h += entryHashCode(i);
            }
        }

        return h;
    }

    private int entryHashCode(int index) {
        return (keyUniverse[index].hashCode() ^ vals[index].hashCode());
    }

    @Override
    @SuppressWarnings("unchecked")
    public MyEnumMap<K, V> clone() {
        MyEnumMap<K, V> result = null;
        try {
            result = (MyEnumMap<K, V>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
        result.vals = result.vals.clone();
        result.entrySet = null;
        return result;
    }


    private void typeCheck(K key) {
        Class<?> keyClass = key.getClass();
        if (keyClass != keyType && keyClass.getSuperclass() != keyType) {
            throw new ClassCastException(keyClass + " != " + keyType);
        }
    }


    private static <K extends Enum<K>> K[] getKeyUniverse(Class<K> keyType) {
        return SharedSecrets.getJavaLangAccess()
                .getEnumConstantsShared(keyType);
    }

    private static final long serialVersionUID = 458661240069192865L;


    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {

        s.defaultWriteObject();


        s.writeInt(size);


        int entriesToBeWritten = size;
        for (int i = 0; entriesToBeWritten > 0; i++) {
            if (null != vals[i]) {
                s.writeObject(keyUniverse[i]);
                s.writeObject(unmaskNull(vals[i]));
                entriesToBeWritten--;
            }
        }
    }


    @SuppressWarnings("unchecked")
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {

        s.defaultReadObject();

        keyUniverse = getKeyUniverse(keyType);
        vals = new Object[keyUniverse.length];


        int size = s.readInt();


        for (int i = 0; i < size; i++) {
            K key = (K) s.readObject();
            V value = (V) s.readObject();
            put(key, value);
        }
    }
}
