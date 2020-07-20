

package example.container.collection;

import java.util.Comparator;


public interface MySortedMap<K, V> extends MyMap<K, V> {
    Comparator<? super K> comparator();

    MySortedMap<K, V> subMap(K fromKey, K toKey);

    MySortedMap<K, V> headMap(K toKey);

    MySortedMap<K, V> tailMap(K fromKey);

    K firstKey();

    K lastKey();


    @Override
    MySet<K> keySet();


    @Override
    MyCollection<V> values();

    @Override
    MySet< Entry<K, V>> entrySet();
}
