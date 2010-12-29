/**
 * 
 */
package experimentalcode.frankenb.model.ifaces;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public interface Partition extends Iterable<Pair<Integer, NumberVector<?, ?>>> {

  public abstract File getStorageFile();

  public abstract void addVector(int id, NumberVector<?, ?> vector);

  public abstract void close() throws IOException;

  public abstract Iterator<Pair<Integer, NumberVector<?, ?>>> iterator();

  public abstract int getSize();

  public abstract int getDimensionality();

  public abstract void copyToFile(File file) throws IOException;

}