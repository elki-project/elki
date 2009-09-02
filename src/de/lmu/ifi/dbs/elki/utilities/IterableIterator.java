package de.lmu.ifi.dbs.elki.utilities;

import java.util.Iterator;

/**
 * Interface that is both Iterable and an Iterator.
 * 
 * Calling {@code iterator()} repeatedly MAY return the same iterator,
 * e.g. the IterableIterator itself. In fact, this is the expected behavior,
 * since this is just meant to allow the use of this Iterator in a {@code foreach} statement.
 * 
 * @author Erich Schubert
 * @param <T> Data type
 */
public interface IterableIterator<T> extends Iterable<T>, Iterator<T> {
  // no extra conditions
}
