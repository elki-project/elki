package de.lmu.ifi.dbs.elki.utilities;

import java.util.ArrayList;
import java.util.ListIterator;

import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Manages a list of handlers for objects. Handlers are appended to a list in
 * sequence, and when querying, the last added handler will be used where the
 * query object can be cast to the handlers restriction class.
 * 
 * @author Erich Schubert
 * 
 * @param <H> Parent class/interface for all handlers
 */
public final class HandlerList<H> {
  /**
   * List with registered Handlers. The list is kept in backwards order, that is
   * the later entrys take precedence.
   */
  private ArrayList<Pair<Class<?>, H>> handlers = new ArrayList<Pair<Class<?>, H>>();

  /**
   * Insert a handler to the beginning of the stack.
   * 
   * @param restrictionClass
   * @param handler
   */
  public void insertHandler(Class<?> restrictionClass, H handler) {
    // note that the handlers list is kept in a list that is traversed in
    // backwards order.
    handlers.add(new Pair<Class<?>, H>(restrictionClass, handler));
  }

  /**
   * Find a matching handler for the given object
   * 
   * @param o object to find handler for
   * @return handler for the object. null if no handler was found.
   */
  public H getHandler(Object o) {
    if(o == null) {
      return null;
    }
    // note that we start at the end of the list.
    ListIterator<Pair<Class<?>, H>> iter = handlers.listIterator(handlers.size());
    while(iter.hasPrevious()) {
      Pair<Class<?>, H> pair = iter.previous();
      try {
        // if we can cast to the restriction class, use the given handler.
        pair.getFirst().cast(o);
        return pair.getSecond();
      }
      catch(ClassCastException e) {
        // do nothing, but try previous in list
      }
    }
    return null;
  }
}