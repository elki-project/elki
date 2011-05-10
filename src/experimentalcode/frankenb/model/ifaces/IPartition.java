package experimentalcode.frankenb.model.ifaces;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public interface IPartition<V> extends Iterable<Pair<DBID, V>> {

  public int getId();
  
  public File getStorageFile();

  public void addVector(DBID id, V vector);

  public void close() throws IOException;

  public Iterator<Pair<DBID, V>> iterator();

  public int getSize();

  public int getDimensionality();

  public void copyTo(File file) throws IOException;

}