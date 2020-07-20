package example.container.collection;




import java.io.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;


import sun.misc.SharedSecrets;


public class MyHashtable<K,V>
        extends Dictionary<K,V>
        implements MyMap<K,V>, Cloneable, java.io.Serializable {


    private transient MyHashtable.Entry<?,?>[] table;


    private transient int count;


    private int threshold;


    private float loadFactor;


    private transient int modCount = 0;


    private static final long serialVersionUID = 1421746759512286392L;


    public MyHashtable(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Illegal Capacity: "+
                    initialCapacity);
        }
        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Illegal Load: "+loadFactor);
        }

        if (initialCapacity==0) {
            initialCapacity = 1;
        }
        this.loadFactor = loadFactor;
        table = new MyHashtable.Entry<?,?>[initialCapacity];
        threshold = (int)Math.min(initialCapacity * loadFactor, MAX_ARRAY_SIZE + 1);
    }


    public MyHashtable(int initialCapacity) {
        this(initialCapacity, 0.75f);
    }


    public MyHashtable() {
        this(11, 0.75f);
    }


    public MyHashtable(MyMap<? extends K, ? extends V> t) {
        this(Math.max(2*t.size(), 11), 0.75f);
        putAll(t);
    }

    @Override
    public synchronized int size() {
        return count;
    }

    @Override
    public synchronized boolean isEmpty() {
        return count == 0;
    }

    @Override
    public synchronized Enumeration<K> keys() {
        return this.<K>getEnumeration(KEYS);
    }

    @Override
    public synchronized Enumeration<V> elements() {
        return this.<V>getEnumeration(VALUES);
    }


    public synchronized boolean contains(Object value) {
        if (value == null) {
            throw new NullPointerException();
        }

        MyHashtable.Entry<?,?> tab[] = table;
        for (int i = tab.length ; i-- > 0 ;) {
            for (MyHashtable.Entry<?,?> e = tab[i]; e != null ; e = e.next) {
                if (e.value.equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        return contains(value);
    }

    @Override
    public synchronized boolean containsKey(Object key) {
        MyHashtable.Entry<?,?> tab[] = table;
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (MyHashtable.Entry<?,?> e = tab[index]; e != null ; e = e.next) {
            if ((e.hash == hash) && e.key.equals(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized V get(Object key) {
        MyHashtable.Entry<?,?> tab[] = table;
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (MyHashtable.Entry<?,?> e = tab[index]; e != null ; e = e.next) {
            if ((e.hash == hash) && e.key.equals(key)) {
                return (V)e.value;
            }
        }
        return null;
    }


    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;


    @SuppressWarnings("unchecked")
    protected void rehash() {
        int oldCapacity = table.length;
        MyHashtable.Entry<?,?>[] oldMap = table;


        int newCapacity = (oldCapacity << 1) + 1;
        if (newCapacity - MAX_ARRAY_SIZE > 0) {
            if (oldCapacity == MAX_ARRAY_SIZE) {
                return;
            }
            newCapacity = MAX_ARRAY_SIZE;
        }
        MyHashtable.Entry<?,?>[] newMap = new MyHashtable.Entry<?,?>[newCapacity];

        modCount++;
        threshold = (int)Math.min(newCapacity * loadFactor, MAX_ARRAY_SIZE + 1);
        table = newMap;

        for (int i = oldCapacity ; i-- > 0 ;) {
            for (MyHashtable.Entry<K,V> old = (MyHashtable.Entry<K,V>)oldMap[i]; old != null ; ) {
                MyHashtable.Entry<K,V> e = old;
                old = old.next;

                int index = (e.hash & 0x7FFFFFFF) % newCapacity;
                e.next = (MyHashtable.Entry<K,V>)newMap[index];
                newMap[index] = e;
            }
        }
    }

    private void addEntry(int hash, K key, V value, int index) {
        modCount++;

        MyHashtable.Entry<?,?> tab[] = table;
        if (count >= threshold) {

            rehash();

            tab = table;
            hash = key.hashCode();
            index = (hash & 0x7FFFFFFF) % tab.length;
        }


        @SuppressWarnings("unchecked")
        MyHashtable.Entry<K,V> e = (MyHashtable.Entry<K,V>) tab[index];
        tab[index] = new MyHashtable.Entry<>(hash, key, value, e);
        count++;
    }

    @Override
    public synchronized V put(K key, V value) {

        if (value == null) {
            throw new NullPointerException();
        }


        MyHashtable.Entry<?,?> tab[] = table;
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        @SuppressWarnings("unchecked")
        MyHashtable.Entry<K,V> entry = (MyHashtable.Entry<K,V>)tab[index];
        for(; entry != null ; entry = entry.next) {
            if ((entry.hash == hash) && entry.key.equals(key)) {
                V old = entry.value;
                entry.value = value;
                return old;
            }
        }

        addEntry(hash, key, value, index);
        return null;
    }

    @Override
    public synchronized V remove(Object key) {
        MyHashtable.Entry<?,?> tab[] = table;
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        @SuppressWarnings("unchecked")
        MyHashtable.Entry<K,V> e = (MyHashtable.Entry<K,V>)tab[index];
        for(MyHashtable.Entry<K,V> prev = null; e != null ; prev = e, e = e.next) {
            if ((e.hash == hash) && e.key.equals(key)) {
                modCount++;
                if (prev != null) {
                    prev.next = e.next;
                } else {
                    tab[index] = e.next;
                }
                count--;
                V oldValue = e.value;
                e.value = null;
                return oldValue;
            }
        }
        return null;
    }

    @Override
    public synchronized void putAll(MyMap<? extends K, ? extends V> t) {
        for (MyMap.Entry<? extends K, ? extends V> e : t.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public synchronized void clear() {
        MyHashtable.Entry<?,?> tab[] = table;
        modCount++;
        for (int index = tab.length; --index >= 0; ) {
            tab[index] = null;
        }
        count = 0;
    }

    @Override
    public synchronized Object clone() {
        try {
            MyHashtable<?,?> t = (MyHashtable<?,?>)super.clone();
            t.table = new MyHashtable.Entry<?,?>[table.length];
            for (int i = table.length ; i-- > 0 ; ) {
                t.table[i] = (table[i] != null)
                        ? (MyHashtable.Entry<?,?>) table[i].clone() : null;
            }
            t.keySet = null;
            t.entrySet = null;
            t.values = null;
            t.modCount = 0;
            return t;
        } catch (CloneNotSupportedException e) {

            throw new InternalError(e);
        }
    }

    @Override
    public synchronized String toString() {
        int max = size() - 1;
        if (max == -1) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder();
        Iterator<MyMap.Entry<K,V>> it = entrySet().iterator();

        sb.append('{');
        for (int i = 0; ; i++) {
            MyMap.Entry<K,V> e = it.next();
            K key = e.getKey();
            V value = e.getValue();
            sb.append(key   == this ? "(this Map)" : key.toString());
            sb.append('=');
            sb.append(value == this ? "(this Map)" : value.toString());

            if (i == max) {
                return sb.append('}').toString();
            }
            sb.append(", ");
        }
    }


    private <T> Enumeration<T> getEnumeration(int type) {
        if (count == 0) {
            return Collections.emptyEnumeration();
        } else {
            return new Enumerator<>(type, false);
        }
    }

    private <T> Iterator<T> getIterator(int type) {
        if (count == 0) {
            return Collections.emptyIterator();
        } else {
            return new Enumerator<>(type, true);
        }
    }




    private transient volatile MySet<K> keySet;
    private transient volatile MySet<MyMap.Entry<K,V>> entrySet;
    private transient volatile MyCollection<V> values;


    @Override
    public MySet<K> keySet() {
        if (keySet == null) {
            keySet = MyCollections.synchronizedSet(new KeySet(), this);
        }
        return keySet;
    }

    private class KeySet extends MyAbstractSet<K> {
        @Override
        public Iterator<K> iterator() {
            return getIterator(KEYS);
        }
        @Override
        public int size() {
            return count;
        }
        @Override
        public boolean contains(Object o) {
            return containsKey(o);
        }
        @Override
        public boolean remove(Object o) {
            return MyHashtable.this.remove(o) != null;
        }
        @Override
        public void clear() {
            MyHashtable.this.clear();
        }
    }

    @Override
    public MySet<MyMap.Entry<K,V>> entrySet() {
        if (entrySet==null) {
            entrySet = MyCollections.synchronizedSet(new EntrySet(), this);
        }
        return entrySet;
    }

    private class EntrySet extends MyAbstractSet<MyMap.Entry<K,V>> {
        @Override
        public Iterator<MyMap.Entry<K,V>> iterator() {
            return getIterator(ENTRIES);
        }
        @Override
        public boolean add(MyMap.Entry<K,V> o) {
            return super.add(o);
        }
        @Override
        public boolean contains(Object o) {
            if (!(o instanceof MyMap.Entry)) {
                return false;
            }
            Map.Entry<?,?> entry = (Map.Entry<?,?>)o;
            Object key = entry.getKey();
            MyHashtable.Entry<?,?>[] tab = table;
            int hash = key.hashCode();
            int index = (hash & 0x7FFFFFFF) % tab.length;

            for (MyHashtable.Entry<?,?> e = tab[index]; e != null; e = e.next) {
                if (e.hash==hash && e.equals(entry)) {
                    return true;
                }
            }
            return false;
        }
        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?,?> entry = (Map.Entry<?,?>) o;
            Object key = entry.getKey();
            MyHashtable.Entry<?,?>[] tab = table;
            int hash = key.hashCode();
            int index = (hash & 0x7FFFFFFF) % tab.length;

            @SuppressWarnings("unchecked")
            MyHashtable.Entry<K,V> e = (MyHashtable.Entry<K,V>)tab[index];
            for(MyHashtable.Entry<K,V> prev = null; e != null; prev = e, e = e.next) {
                if (e.hash==hash && e.equals(entry)) {
                    modCount++;
                    if (prev != null) {
                        prev.next = e.next;
                    } else {
                        tab[index] = e.next;
                    }

                    count--;
                    e.value = null;
                    return true;
                }
            }
            return false;
        }
        @Override
        public int size() {
            return count;
        }
        @Override
        public void clear() {
            MyHashtable.this.clear();
        }
    }

    @Override
    public MyCollection<V> values() {
        if (values==null) {
            values = MyCollections.synchronizedCollection(new ValueCollection(),
                    this);
        }
        return values;
    }

    private class ValueCollection extends MyAbstractCollection<V> {
        @Override
        public Iterator<V> iterator() {
            return getIterator(VALUES);
        }
        @Override
        public int size() {
            return count;
        }
        @Override
        public boolean contains(Object o) {
            return containsValue(o);
        }
        @Override
        public void clear() {
            MyHashtable.this.clear();
        }
    }




    @Override
    public synchronized boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Map)) {
            return false;
        }
        Map<?,?> t = (Map<?,?>) o;
        if (t.size() != size()) {
            return false;
        }

        try {
            Iterator<MyMap.Entry<K,V>> i = entrySet().iterator();
            while (i.hasNext()) {
                MyMap.Entry<K,V> e = i.next();
                K key = e.getKey();
                V value = e.getValue();
                if (value == null) {
                    if (!(t.get(key)==null && t.containsKey(key))) {
                        return false;
                    }
                } else {
                    if (!value.equals(t.get(key))) {
                        return false;
                    }
                }
            }
        } catch (ClassCastException unused)   {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }

        return true;
    }

    @Override
    public synchronized int hashCode() {

        int h = 0;
        if (count == 0 || loadFactor < 0) {
            return h;
        }

        loadFactor = -loadFactor;
        MyHashtable.Entry<?,?>[] tab = table;
        for (MyHashtable.Entry<?,?> entry : tab) {
            while (entry != null) {
                h += entry.hashCode();
                entry = entry.next;
            }
        }

        loadFactor = -loadFactor;

        return h;
    }

    @Override
    public synchronized V getOrDefault(Object key, V defaultValue) {
        V result = get(key);
        return (null == result) ? defaultValue : result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);

        final int expectedModCount = modCount;

        MyHashtable.Entry<?, ?>[] tab = table;
        for (MyHashtable.Entry<?, ?> entry : tab) {
            while (entry != null) {
                action.accept((K)entry.key, (V)entry.value);
                entry = entry.next;

                if (expectedModCount != modCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);

        final int expectedModCount = modCount;

        MyHashtable.Entry<K, V>[] tab = (MyHashtable.Entry<K, V>[])table;
        for (MyHashtable.Entry<K, V> entry : tab) {
            while (entry != null) {
                entry.value = Objects.requireNonNull(
                        function.apply(entry.key, entry.value));
                entry = entry.next;

                if (expectedModCount != modCount) {
                    throw new ConcurrentModificationException();
                }
            }
        }
    }

    @Override
    public synchronized V putIfAbsent(K key, V value) {
        Objects.requireNonNull(value);


        MyHashtable.Entry<?,?> tab[] = table;
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        @SuppressWarnings("unchecked")
        MyHashtable.Entry<K,V> entry = (MyHashtable.Entry<K,V>)tab[index];
        for (; entry != null; entry = entry.next) {
            if ((entry.hash == hash) && entry.key.equals(key)) {
                V old = entry.value;
                if (old == null) {
                    entry.value = value;
                }
                return old;
            }
        }

        addEntry(hash, key, value, index);
        return null;
    }

    @Override
    public synchronized boolean remove(Object key, Object value) {
        Objects.requireNonNull(value);

        MyHashtable.Entry<?,?> tab[] = table;
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        @SuppressWarnings("unchecked")
        MyHashtable.Entry<K,V> e = (MyHashtable.Entry<K,V>)tab[index];
        for (MyHashtable.Entry<K,V> prev = null; e != null; prev = e, e = e.next) {
            if ((e.hash == hash) && e.key.equals(key) && e.value.equals(value)) {
                modCount++;
                if (prev != null) {
                    prev.next = e.next;
                } else {
                    tab[index] = e.next;
                }
                count--;
                e.value = null;
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized boolean replace(K key, V oldValue, V newValue) {
        Objects.requireNonNull(oldValue);
        Objects.requireNonNull(newValue);
        MyHashtable.Entry<?,?> tab[] = table;
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        @SuppressWarnings("unchecked")
        MyHashtable.Entry<K,V> e = (MyHashtable.Entry<K,V>)tab[index];
        for (; e != null; e = e.next) {
            if ((e.hash == hash) && e.key.equals(key)) {
                if (e.value.equals(oldValue)) {
                    e.value = newValue;
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public synchronized V replace(K key, V value) {
        Objects.requireNonNull(value);
        MyHashtable.Entry<?,?> tab[] = table;
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        @SuppressWarnings("unchecked")
        MyHashtable.Entry<K,V> e = (MyHashtable.Entry<K,V>)tab[index];
        for (; e != null; e = e.next) {
            if ((e.hash == hash) && e.key.equals(key)) {
                V oldValue = e.value;
                e.value = value;
                return oldValue;
            }
        }
        return null;
    }

    @Override
    public synchronized V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);

        MyHashtable.Entry<?,?> tab[] = table;
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        @SuppressWarnings("unchecked")
        MyHashtable.Entry<K,V> e = (MyHashtable.Entry<K,V>)tab[index];
        for (; e != null; e = e.next) {
            if (e.hash == hash && e.key.equals(key)) {

                return e.value;
            }
        }

        V newValue = mappingFunction.apply(key);
        if (newValue != null) {
            addEntry(hash, key, newValue, index);
        }

        return newValue;
    }

    @Override
    public synchronized V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);

        MyHashtable.Entry<?,?> tab[] = table;
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        @SuppressWarnings("unchecked")
        MyHashtable.Entry<K,V> e = (MyHashtable.Entry<K,V>)tab[index];
        for (MyHashtable.Entry<K,V> prev = null; e != null; prev = e, e = e.next) {
            if (e.hash == hash && e.key.equals(key)) {
                V newValue = remappingFunction.apply(key, e.value);
                if (newValue == null) {
                    modCount++;
                    if (prev != null) {
                        prev.next = e.next;
                    } else {
                        tab[index] = e.next;
                    }
                    count--;
                } else {
                    e.value = newValue;
                }
                return newValue;
            }
        }
        return null;
    }

    @Override
    public synchronized V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);

        MyHashtable.Entry<?,?> tab[] = table;
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        @SuppressWarnings("unchecked")
        MyHashtable.Entry<K,V> e = (MyHashtable.Entry<K,V>)tab[index];
        for (MyHashtable.Entry<K,V> prev = null; e != null; prev = e, e = e.next) {
            if (e.hash == hash && Objects.equals(e.key, key)) {
                V newValue = remappingFunction.apply(key, e.value);
                if (newValue == null) {
                    modCount++;
                    if (prev != null) {
                        prev.next = e.next;
                    } else {
                        tab[index] = e.next;
                    }
                    count--;
                } else {
                    e.value = newValue;
                }
                return newValue;
            }
        }

        V newValue = remappingFunction.apply(key, null);
        if (newValue != null) {
            addEntry(hash, key, newValue, index);
        }

        return newValue;
    }

    @Override
    public synchronized V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);

        MyHashtable.Entry<?,?> tab[] = table;
        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        @SuppressWarnings("unchecked")
        MyHashtable.Entry<K,V> e = (MyHashtable.Entry<K,V>)tab[index];
        for (MyHashtable.Entry<K,V> prev = null; e != null; prev = e, e = e.next) {
            if (e.hash == hash && e.key.equals(key)) {
                V newValue = remappingFunction.apply(e.value, value);
                if (newValue == null) {
                    modCount++;
                    if (prev != null) {
                        prev.next = e.next;
                    } else {
                        tab[index] = e.next;
                    }
                    count--;
                } else {
                    e.value = newValue;
                }
                return newValue;
            }
        }

        if (value != null) {
            addEntry(hash, key, value, index);
        }

        return value;
    }


    private void writeObject(java.io.ObjectOutputStream s)
            throws IOException {
        MyHashtable.Entry<Object, Object> entryStack = null;

        synchronized (this) {

            s.defaultWriteObject();


            s.writeInt(table.length);
            s.writeInt(count);


            for (int index = 0; index < table.length; index++) {
                MyHashtable.Entry<?,?> entry = table[index];

                while (entry != null) {
                    entryStack =
                            new MyHashtable.Entry<>(0, entry.key, entry.value, entryStack);
                    entry = entry.next;
                }
            }
        }


        while (entryStack != null) {
            s.writeObject(entryStack.key);
            s.writeObject(entryStack.value);
            entryStack = entryStack.next;
        }
    }


    private void readObject(java.io.ObjectInputStream s)
            throws IOException, ClassNotFoundException
    {

        s.defaultReadObject();


        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new StreamCorruptedException("Illegal Load: " + loadFactor);
        }


        int origlength = s.readInt();
        int elements = s.readInt();


        if (elements < 0) {
            throw new StreamCorruptedException("Illegal # of Elements: " + elements);
        }



        origlength = Math.max(origlength, (int)(elements / loadFactor) + 1);





        int length = (int)((elements + elements / 20) / loadFactor) + 3;
        if (length > elements && (length & 1) == 0) {
            length--;
        }
        length = Math.min(length, origlength);

        if (length < 0) {
            length = origlength;
        }



        SharedSecrets.getJavaOISAccess().checkArray(s, Map.Entry[].class, length);
        table = new MyHashtable.Entry<?,?>[length];
        threshold = (int)Math.min(length * loadFactor, MAX_ARRAY_SIZE + 1);
        count = 0;


        for (; elements > 0; elements--) {
            @SuppressWarnings("unchecked")
            K key = (K)s.readObject();
            @SuppressWarnings("unchecked")
            V value = (V)s.readObject();

            reconstitutionPut(table, key, value);
        }
    }


    private void reconstitutionPut(MyHashtable.Entry<?,?>[] tab, K key, V value)
            throws StreamCorruptedException
    {
        if (value == null) {
            throw new java.io.StreamCorruptedException();
        }


        int hash = key.hashCode();
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (MyHashtable.Entry<?,?> e = tab[index]; e != null ; e = e.next) {
            if ((e.hash == hash) && e.key.equals(key)) {
                throw new java.io.StreamCorruptedException();
            }
        }

        @SuppressWarnings("unchecked")
        MyHashtable.Entry<K,V> e = (MyHashtable.Entry<K,V>)tab[index];
        tab[index] = new MyHashtable.Entry<>(hash, key, value, e);
        count++;
    }


    private static class Entry<K,V> implements Map.Entry<K,V> {
        final int hash;
        final K key;
        V value;
        MyHashtable.Entry<K,V> next;

        protected Entry(int hash, K key, V value, MyHashtable.Entry<K,V> next) {
            this.hash = hash;
            this.key =  key;
            this.value = value;
            this.next = next;
        }

        @SuppressWarnings("unchecked")
        protected Object clone() {
            return new MyHashtable.Entry<>(hash, key, value,
                    (next==null ? null : (MyHashtable.Entry<K,V>) next.clone()));
        }



        @Override
        public K getKey() {
            return key;
        }
        @Override
        public V getValue() {
            return value;
        }
        @Override
        public V setValue(V value) {
            if (value == null) {
                throw new NullPointerException();
            }

            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry<?,?> e = (Map.Entry<?,?>)o;

            return (key==null ? e.getKey()==null : key.equals(e.getKey())) &&
                    (value==null ? e.getValue()==null : value.equals(e.getValue()));
        }
        @Override
        public int hashCode() {
            return hash ^ Objects.hashCode(value);
        }
        @Override
        public String toString() {
            return key.toString()+"="+value.toString();
        }
    }


    private static final int KEYS = 0;
    private static final int VALUES = 1;
    private static final int ENTRIES = 2;


    private class Enumerator<T> implements Enumeration<T>, Iterator<T> {
        MyHashtable.Entry<?,?>[] table = MyHashtable.this.table;
        int index = table.length;
        MyHashtable.Entry<?,?> entry;
        MyHashtable.Entry<?,?> lastReturned;
        int type;


        boolean iterator;


        protected int expectedModCount = modCount;

        Enumerator(int type, boolean iterator) {
            this.type = type;
            this.iterator = iterator;
        }
        @Override
        public boolean hasMoreElements() {
            MyHashtable.Entry<?,?> e = entry;
            int i = index;
            MyHashtable.Entry<?,?>[] t = table;

            while (e == null && i > 0) {
                e = t[--i];
            }
            entry = e;
            index = i;
            return e != null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public T nextElement() {
            MyHashtable.Entry<?,?> et = entry;
            int i = index;
            MyHashtable.Entry<?,?>[] t = table;

            while (et == null && i > 0) {
                et = t[--i];
            }
            entry = et;
            index = i;
            if (et != null) {
                MyHashtable.Entry<?,?> e = lastReturned = entry;
                entry = e.next;
                return type == KEYS ? (T)e.key : (type == VALUES ? (T)e.value : (T)e);
            }
            throw new NoSuchElementException("MyHashtable Enumerator");
        }

        @Override
        public boolean hasNext() {
            return hasMoreElements();
        }
        @Override
        public T next() {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            return nextElement();
        }
        @Override
        public void remove() {
            if (!iterator) {
                throw new UnsupportedOperationException();
            }
            if (lastReturned == null) {
                throw new IllegalStateException("MyHashtable Enumerator");
            }
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }

            synchronized(MyHashtable.this) {
                MyHashtable.Entry<?,?>[] tab = MyHashtable.this.table;
                int index = (lastReturned.hash & 0x7FFFFFFF) % tab.length;

                @SuppressWarnings("unchecked")
                MyHashtable.Entry<K,V> e = (MyHashtable.Entry<K,V>)tab[index];
                for(MyHashtable.Entry<K,V> prev = null; e != null; prev = e, e = e.next) {
                    if (e == lastReturned) {
                        modCount++;
                        expectedModCount++;
                        if (prev == null) {
                            tab[index] = e.next;
                        } else {
                            prev.next = e.next;
                        }
                        count--;
                        lastReturned = null;
                        return;
                    }
                }
                throw new ConcurrentModificationException();
            }
        }
    }
}
