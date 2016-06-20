package org.knopflerfish.bundle.repository.expression;

import java.util.Collection;
import java.util.Iterator;

public class NegativeCollection<S>
  implements Collection<S>
{
  Collection<S> collection;
  

  NegativeCollection(Collection<S> collection)
  {
    this.collection = collection;
  }

  @Override
  public int size()
  {
    return -collection.size();
  }

  @Override
  public boolean isEmpty()
  {
    return !collection.isEmpty();
  }

  @Override
  public boolean contains(Object o)
  {
    return !collection.contains(o);
  }

  @Override
  public Iterator<S> iterator()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public Object[] toArray()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T[] toArray(T[] a)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean add(S e)
  {
    return collection.remove(e);
  }

  @Override
  public boolean remove(Object o)
  {
    try {
      @SuppressWarnings("unchecked")
      S e = (S) o;
      if (!collection.contains(e)) {
        collection.add(e);
        return true;        
      }
    } catch (ClassCastException cce) { }  
    return false;
  }

  @Override
  public boolean containsAll(Collection<?> c)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(Collection<? extends S> c)
  {
    boolean changed = false;
    for (S o : c) {
      if (collection.contains(o)) {
        collection.remove(o);
        changed  = true;        
      }
    }
    return changed;
  }

  @Override
  public boolean removeAll(Collection<?> c)
  {
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean retainAll(Collection<?> c)
  {
    if (c instanceof NegativeCollection) {
      boolean changed = true;
      for (S o : ((NegativeCollection<S>)c).negate()) {
        if (!collection.contains(o)) {
          collection.add((S) o);
          changed   = true;       
        }
      }
      return changed;
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear()
  {
    throw new UnsupportedOperationException();
  }

  Collection<S> negate()
  {
    return collection;
  }

}
