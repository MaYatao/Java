
package example.container.collection;


import java.util.NoSuchElementException;

public abstract class MyAbstractQueue<E>
    extends MyAbstractCollection<E>
    implements MyQueue<E> {


    protected MyAbstractQueue() {
    }

  @Override
  public boolean add(E e) {
        if (offer(e)) {
            return true;
        } else {
            throw new IllegalStateException("Queue full");
        }
    }
    @Override
 public E remove() {
        E x = poll();
        if (x != null) {
            return x;
        } else {
            throw new NoSuchElementException();
        }
    }


    @Override
    public E element() {
        E x = peek();
        if (x != null) {
            return x;
        } else {
            throw new NoSuchElementException();
        }
    }


    @Override
    public void clear() {
        while (poll() != null) {
            ;
        }
    }

   @Override
   public boolean addAll(MyCollection<? extends E> c) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        boolean modified = false;
        for (E e : c) {
            if (add(e)) {
                modified = true;
            }
        }
        return modified;
    }

}
