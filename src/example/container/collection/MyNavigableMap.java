

package example.container.collection;





public interface MyNavigableMap<K, V> extends  MySortedMap<K, V> {
    Entry<K, V> lowerEntry(K key);

    K lowerKey(K key);

    MyMap.Entry<K, V> floorEntry(K key);

    K floorKey(K key);

    MyMap.Entry<K, V> ceilingEntry(K key);

    K ceilingKey(K key);

    MyMap.Entry<K, V> higherEntry(K key);

    K higherKey(K key);

    MyMap.Entry<K, V> firstEntry();

    MyMap.Entry<K, V> lastEntry();

    MyMap.Entry<K, V> pollFirstEntry();

    MyMap.Entry<K, V> pollLastEntry();

    MyNavigableMap<K, V> descendingMap();

    MyNavigableSet<K> navigableKeySet();

    MyNavigableSet<K> descendingKeySet();

    MyNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive,
                                K toKey, boolean toInclusive);

    MyNavigableMap<K, V> headMap(K toKey, boolean inclusive);

    MyNavigableMap<K, V> tailMap(K fromKey, boolean inclusive);

    @Override
    MySortedMap<K, V> subMap(K fromKey, K toKey);

    @Override
    MySortedMap<K, V> headMap(K toKey);

    @Override
    MySortedMap<K, V> tailMap(K fromKey);
}
