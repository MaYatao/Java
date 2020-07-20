package example.container.collection;




import java.util.*;



public class MyLinkedHashSet<E>
        extends MyHashSet<E>
        implements MySet<E>, Cloneable, java.io.Serializable {

    private static final long serialVersionUID = -2851667679971038690L;


    public MyLinkedHashSet(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor, true);
    }


    public MyLinkedHashSet(int initialCapacity) {
        super(initialCapacity, .75f, true);
    }


    public MyLinkedHashSet() {
        super(16, .75f, true);
    }


    public MyLinkedHashSet(MyCollection<? extends E> c) {
        super(Math.max(2*c.size(), 11), .75f, true);
        addAll(c);
    }


    @Override
    public Spliterator<E> spliterator() {
        return MySpliterators.spliterator(this, Spliterator.DISTINCT | Spliterator.ORDERED);
    }
}
