/**
 * 
 */
package experimentalcode.frankenb.model;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.data.NumberVector;
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
public class PositionedPartition implements IPositionedPartition {

  private IPartition partition;
  private final int[] position;

  public PositionedPartition(int dimension, IPartition partition) {
    this.position = new int[dimension];
    for (int i = 0; i < position[dimension]; ++i) {
      this.position[i] = 0;
    }
    
    this.partition = partition;
  }  
  
  public PositionedPartition(int[] position, IPartition partition) {
    this.position = position;
    this.partition = partition;
  }
  
  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.Partition#getStorageFile()
   */
  @Override
  public File getStorageFile() {
    return this.partition.getStorageFile();
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.Partition#addVector(int, de.lmu.ifi.dbs.elki.data.NumberVector)
   */
  @Override
  public void addVector(int id, NumberVector<?, ?> vector) {
    this.partition.addVector(id, vector);
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.Partition#close()
   */
  @Override
  public void close() throws IOException {
    this.partition.close();
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.Partition#iterator()
   */
  @Override
  public Iterator<Pair<Integer, NumberVector<?, ?>>> iterator() {
    return this.partition.iterator();
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.Partition#getSize()
   */
  @Override
  public int getSize() {
    return this.partition.getSize();
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.Partition#getDimensionality()
   */
  @Override
  public int getDimensionality() {
    return this.partition.getDimensionality();
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.Partition#copyToFile(java.io.File)
   */
  @Override
  public void copyToFile(File file) throws IOException {
    this.partition.copyToFile(file);
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.ifaces.IPositionedPartition#getPosition(int)
   */
  @Override
  public int getPosition(int dimension) {
    return this.position[dimension - 1];
  }
  
  public void setPosition(int dimension, int position) {
    this.position[dimension - 1] = position;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
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
