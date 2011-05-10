package experimentalcode.frankenb.model;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.frankenb.model.ifaces.IPartition;
import experimentalcode.frankenb.model.ifaces.IPositionedPartition;

/**
 * This class represents a partition with additional position infos that
 * can be used by the PartitionPairer implementor to pair the partitions
 * more appropriately
 * 
 * @author Florian Frankenberger
 */
public class PositionedPartition<V> implements IPositionedPartition<V> {

  private IPartition<V> partition;
  private final int[] position;

  public PositionedPartition(int dimension, IPartition<V> partition) {
    this.position = new int[dimension];
    for (int i = 0; i < position[dimension]; ++i) {
      this.position[i] = 0;
    }
    
    this.partition = partition;
  }  
  
  public PositionedPartition(int[] position, IPartition<V> partition) {
    this.position = position;
    this.partition = partition;
  }
  
  @Override
  public int getId() {
    return this.partition.getId();
  }
  
  @Override
  public File getStorageFile() {
    return this.partition.getStorageFile();
  }

  @Override
  public void addVector(DBID id, V vector) {
    this.partition.addVector(id, vector);
  }

  @Override
  public void close() throws IOException {
    this.partition.close();
  }

  @Override
  public Iterator<Pair<DBID, V>> iterator() {
    return this.partition.iterator();
  }

  @Override
  public int getSize() {
    return this.partition.getSize();
  }

  @Override
  public int getDimensionality() {
    return this.partition.getDimensionality();
  }

  @Override
  public void copyTo(File file) throws IOException {
    this.partition.copyTo(file);
  }

  @Override
  public int getPosition(int dimension) {
    return this.position[dimension - 1];
  }
  
  public int[] getPositionArray() {
    return this.position;
  }
  
  public void setPosition(int dimension, int position) {
    this.position[dimension - 1] = position;
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    boolean first = true;
    for (int i = 0; i < position.length; ++i) {
      if (first) {
        first = false;
      } else {
        sb.append(", ");
      }
      sb.append(position[i]);
    }
    sb.append("] (");
    sb.append(position.length);
    sb.append(") => ");
    sb.append(this.partition.toString());
    return sb.toString();
  }

}
