
package example.container.collection;

import java.util.Iterator;

public interface MyNavigableSet<E> extends MySortedSet<E> {
    E lower(E e);

    E floor(E e);

    E ceiling(E e);

    E higher(E e);

    E pollFirst();

    E pollLast();

    @Override
    Iterator<E> iterator();

    MyNavigableSet<E> descendingSet();

    Iterator<E> descendingIterator();

    MyNavigableSet<E> subSet(E fromElement, boolean fromInclusive,
                             E toElement, boolean toInclusive);

    MyNavigableSet<E> headSet(E toElement, boolean inclusive);

    MyNavigableSet<E> tailSet(E fromElement, boolean inclusive);

    @Override
    MySortedSet<E> subSet(E fromElement, E toElement);

    @Override
    MySortedSet<E> headSet(E toElement);

    @Override
    MySortedSet<E> tailSet(E fromElement);
}
