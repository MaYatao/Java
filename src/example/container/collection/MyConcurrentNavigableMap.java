
package example.container.collection;

public interface MyConcurrentNavigableMap<K,V>
    extends MyConcurrentMap<K,V>, MyNavigableMap<K,V>
{

    @Override
    MyConcurrentNavigableMap<K,V> subMap(K fromKey, boolean fromInclusive,
                                         K toKey, boolean toInclusive);

    @Override
    MyConcurrentNavigableMap<K,V> headMap(K toKey, boolean inclusive);


    @Override
    MyConcurrentNavigableMap<K,V> tailMap(K fromKey, boolean inclusive);


    @Override
    MyConcurrentNavigableMap<K,V> subMap(K fromKey, K toKey);

    @Override
    MyConcurrentNavigableMap<K,V> headMap(K toKey);


    @Override
    MyConcurrentNavigableMap<K,V> tailMap(K fromKey);


    @Override
    MyConcurrentNavigableMap<K,V> descendingMap();


    @Override
    public MyNavigableSet<K> navigableKeySet();


    @Override
    MyNavigableSet<K> keySet();


    @Override
    public MyNavigableSet<K> descendingKeySet();
}
