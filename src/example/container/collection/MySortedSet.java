

package example.container.collection;

import java.util.Comparator;
import java.util.Spliterator;


public interface MySortedSet<E> extends MySet<E> {

    Comparator<? super E> comparator();

    MySortedSet<E> subSet(E fromElement, E toElement);

    MySortedSet<E> headSet(E toElement);

    MySortedSet<E> tailSet(E fromElement);

    E first();

    E last();

    @Override
    default Spliterator<E> spliterator() {
        return new MySpliterators.IteratorSpliterator<E>(
                this, Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.ORDERED) {
            @Override
            public Comparator<? super E> getComparator() {
                return MySortedSet.this.comparator();
            }
        };
    }
}
