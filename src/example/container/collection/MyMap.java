package example.container.collection;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.io.Serializable;

/**
 * MyMap: 将键映射到值的对象。映射不能包含重复的键；每个键最多可以映射到一个值。
 * Map界面提供了三个集合视图，这些视图允许将地图的内容视为一组键，一组值或一组键-值映射。
 * 映射的顺序定义为映射的集合视图上的迭代器返回其元素的顺序。
 * 注意：如果将可变对象用作key，则必须格外小心。如果在对象是映射中的键的情况下以影响等值比较的方式更改对象的值，
 * 则不会指定映射的行为。这种禁止的特殊情况是不允许地图包含自身作为键。虽然允许映射包含自身作为值，
 * 但建议格外小心：在此类映射上不再很好地定义equals和hashCode方法。
 */
public interface MyMap<K, V> {

    /**
     *返回此映射中的键值映射数。
     */
    int size();

    /**
     *如果此映射不包含键值映射，则返回true。
     */
    boolean isEmpty();

    /**
     *如果此映射包含指定键的映射，则返回true。
     */
    boolean containsKey(Object key);

    /**
     *如果此映射将一个或多个键映射到指定值，则返回true。
     */
    boolean containsValue(Object value);

    /**
     *返回指定键映射到的值；如果此映射不包含该键的映射，则返回null。
     */
    V get(Object key);

    /**
     *将指定值与该映射中的指定键相关联
     */
    V put(K key, V value);

    /**
     *如果存在，则从此映射中删除键的映射（
     */
    V remove(Object key);

    /**
     *将所有映射从指定映射复制到此映射
     */
    void putAll(MyMap<? extends K, ? extends V> m);

    /**
     *从此映射中删除所有映射
     */
    void clear();

    /**
     * 返回此映射中包含的映射的Set视图。
     */
    MySet<K> keySet();

    /**
     * 返回此映射中包含的值的Collection视图。
     */
    MyCollection<V> values();

    /**
     *返回此映射中包含的映射的Set视图。
     */
    MySet<MyMap.Entry<K, V>> entrySet();

    /**
     * 映射条目（键值对）。 Map.entrySet方法返回Map的集合视图，该Map的元素属于此类。
     * 获取对Map条目的引用的唯一方法是从此collection-view的迭代器中进行。
     * 这些Map.Entry对象仅在迭代期间有效。 更正式地讲，如果迭代器返回条目后修改了后备映射，
     * 则映射条目的行为是不确定的，除非通过映射条目上的setValue操作。
     * ：
     */
    interface Entry<K, V> {

        /**
         *返回与此条目对应的key。
         */
        K getKey();
        /**
         *返回与此条目对应的value。
         */
        V getValue();
        /**
         *用指定的值替换与此条目对应的值（可选操作）。
         */
        V setValue(V value);


        @Override
        boolean equals(Object o);


        @Override
        int hashCode();

        /**
         *返回一个比较器，该比较器以键上的自然顺序比较Map.Entry。
         */
        public static <K extends Comparable<? super K>, V> Comparator<Entry<K, V>> comparingByKey() {
            return (Comparator<MyMap.Entry<K, V>> & Serializable)
                    (c1, c2) -> c1.getKey().compareTo(c2.getKey());
        }

        /**
         *返回一个比较器，该比较器按值的自然顺序比较Map.Entry。
         */
        public static <K, V extends Comparable<? super V>> Comparator<MyMap.Entry<K, V>> comparingByValue() {
            return (Comparator<MyMap.Entry<K, V>> & Serializable)
                    (c1, c2) -> c1.getValue().compareTo(c2.getValue());
        }

        /**
         *返回一个比较器，该比较器使用给定的Comparator通过键比较Map.Entry。
         */
        public static <K, V> Comparator<MyMap.Entry<K, V>> comparingByKey(Comparator<? super K> cmp) {
           //如果cmp为空抛出NullPointerException
            Objects.requireNonNull(cmp);
            return (Comparator<MyMap.Entry<K, V>> & Serializable)
                    (c1, c2) -> cmp.compare(c1.getKey(), c2.getKey());
        }

        /**
         *返回一个比较器，该比较器使用给定的Comparator通过值比较Map.Entry。
         */
        public static <K, V> Comparator<MyMap.Entry<K, V>> comparingByValue(Comparator<? super V> cmp) {
            Objects.requireNonNull(cmp);
            return (Comparator<MyMap.Entry<K, V>> & Serializable)
                    (c1, c2) -> cmp.compare(c1.getValue(), c2.getValue());
        }
    }


    @Override
    boolean equals(Object o);


    @Override
    int hashCode();
    /**
     * 返回指定键映射到的值，如果此映射不包含键的映射，则返回defaultValue。
     */
    default V getOrDefault(Object key, V defaultValue) {
        V v;
        return (((v = get(key)) != null) || containsKey(key))
                ? v
                : defaultValue;
    }

    /**
     * 在此映射中为每个条目执行给定的操作，直到所有条目都已处理或该操作引发异常。
     */
    default void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        for (MyMap.Entry<K, V> entry : entrySet()) {
            K k;
            V v;
            try {
                k = entry.getKey();
                v = entry.getValue();
            } catch (IllegalStateException ise) {

                throw new ConcurrentModificationException(ise);
            }
            action.accept(k, v);
        }
    }

    /**
     *  用对该条目调用给定函数的结果替换每个条目的值，直到处理完所有条目或该函数引发异常为止。
     */
    default void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        for (MyMap.Entry<K, V> entry : entrySet()) {
            K k;
            V v;
            try {
                k = entry.getKey();
                v = entry.getValue();
            } catch (IllegalStateException ise) {

                throw new ConcurrentModificationException(ise);
            }


            v = function.apply(k, v);

            try {
                entry.setValue(v);
            } catch (IllegalStateException ise) {

                throw new ConcurrentModificationException(ise);
            }
        }
    }

    /**
     *如果指定的键尚未与值关联（或映射为null），则将其与给定值关联并返回null，否则返回当前值。
     */
    default V putIfAbsent(K key, V value) {
        V v = get(key);
        if (v == null) {
            v = put(key, value);
        }

        return v;
    }

    /**
     * 仅当当前映射到指定值时，才删除指定键的条目。
     */
    default boolean remove(Object key, Object value) {
        Object curValue = get(key);
        if (!Objects.equals(curValue, value) ||
                (curValue == null && !containsKey(key))) {
            return false;
        }
        remove(key);
        return true;
    }

    /**
     * 仅当当前映射到指定值时，才替换指定键的条目。
     */
    default boolean replace(K key, V oldValue, V newValue) {
        Object curValue = get(key);
        if (!Objects.equals(curValue, oldValue) ||
                (curValue == null && !containsKey(key))) {
            return false;
        }
        put(key, newValue);
        return true;
    }

    /**
     * 仅当当前映射到某个值时，才替换指定键的条目。
     */
    default V replace(K key, V value) {
        V curValue;
        if (((curValue = get(key)) != null) || containsKey(key)) {
            curValue = put(key, value);
        }
        return curValue;
    }
    /**
     *如果指定的键尚未与值关联（或被映射为null），则尝试使用给定的映射函数计算其值，除非为null，否则将其输入此映射。
     */
    default V computeIfAbsent(K key,
                              Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        V v;
        if ((v = get(key)) == null) {
            V newValue;
            if ((newValue = mappingFunction.apply(key)) != null) {
                put(key, newValue);
                return newValue;
            }
        }

        return v;
    }
    /**
     *如果指定键的值存在且非空，则尝试在给定键及其当前映射值的情况下计算新映射。
     */
    default V computeIfPresent(K key,
                               BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        V oldValue;
        if ((oldValue = get(key)) != null) {
            V newValue = remappingFunction.apply(key, oldValue);
            if (newValue != null) {
                put(key, newValue);
                return newValue;
            } else {
                remove(key);
                return null;
            }
        } else {
            return null;
        }
    }
    /**
     * 尝试计算指定键及其当前映射值的映射（如果没有当前映射，则为null）。
     */
    default V compute(K key,
                      BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        V oldValue = get(key);

        V newValue = remappingFunction.apply(key, oldValue);
        if (newValue == null) {
            // 删除映射
            if (oldValue != null || containsKey(key)) {
                // 移除key
                remove(key);
                return null;
            } else {
                //什么都不做做。保持原样。
                return null;
            }
        } else {
            // add or replace old mapping
            put(key, newValue);
            return newValue;
        }
    }
    /**
     * 如果指定的键尚未与值关联或与null关联，请将其与给定的非null值关联。
     */
    default V merge(K key, V value,
                    BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        Objects.requireNonNull(value);
        V oldValue = get(key);
        V newValue = (oldValue == null) ? value :
                remappingFunction.apply(oldValue, value);
        if (newValue == null) {
            remove(key);
        } else {
            put(key, newValue);
        }
        return newValue;
    }
}
