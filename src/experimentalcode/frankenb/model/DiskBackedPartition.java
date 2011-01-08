/**
 * 
 */
package experimentalcode.frankenb.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.frankenb.model.ifaces.IPartition;

/**
 * A part of a database that is normally used in a package for precalculating
 * distances for knn. This datatype is backed up on disk so that it should be suitable
 * even for a huge amount of datasets.
 * 
 * @author Florian Frankenberger
 */
public class DiskBackedPartition implements IPartition {

  private final File storageFile;
  private final int dimensionality;
  private final int recordSize;
  
  private RandomAccessFile file = null;
  
  public DiskBackedPartition(int dimensionality) throws IOException {
    this(dimensionality, null);
  }
  
  private DiskBackedPartition(int dimensionality, File storageFile) throws IOException {
    this.dimensionality = dimensionality;
    this.recordSize = (dimensionality * (Double.SIZE / 8)) + (Integer.SIZE / 8);
    if (storageFile == null) {
      storageFile = File.createTempFile("partition_", null);
      storageFile.delete();
    }
    
    this.storageFile = storageFile;
  }
  
  private void open() {
    if (file != null) return;

    try {
      file = new RandomAccessFile(this.storageFile, "rw");
      if (this.storageFile.exists()) {
        file.seek(this.storageFile.length());
      }
    } catch (IOException e) {
      throw new RuntimeException("Could not open file", e);
    }
  }
  
  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.Partition#getStorageFile()
   */
  @Override
  public File getStorageFile() {
    return this.storageFile;
  }
  
  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.Partition#addVector(int, de.lmu.ifi.dbs.elki.data.NumberVector)
   */
  @Override
  public void addVector(int id, NumberVector<?, ?> vector) {
    try {
      open();
      long position = storageFile.length();
      file.setLength(storageFile.length() + recordSize);
      
      file.seek(position);
      
      //first we write the id
      file.writeInt(id);
      
      // at this point we assume that all elements have the same
      // dimensionality within a database
      for (int k = 1; k <= dimensionality; ++k) {
        file.writeDouble(vector.doubleValue(k));
      }
    } catch (IOException e) {
      throw new RuntimeException("Can't add vector", e);
    }
  }

  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.Partition#close()
   */
  @Override
  public void close() throws IOException {
    open();
    file.close();
    file = null;
  }
  
  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.Partition#iterator()
   */
  @Override
  public Iterator<Pair<Integer, NumberVector<?, ?>>> iterator() {
    open();
    return new Iterator<Pair<Integer, NumberVector<?, ?>>>() {

      private int position = 0;
      
      @Override
      public boolean hasNext() {
        return position < getSize();
      }

      @Override
      public Pair<Integer, NumberVector<?, ?>> next() {
        if (position > getSize() - 1) 
          throw new IllegalStateException("No more items");
        try {
          file.seek(position * recordSize);
          int id = file.readInt();
          
          double[] data = new double[dimensionality];
          for (int k = 0; k < dimensionality; ++k) {
            data[k] = file.readDouble();
          }
          
          position++;
          return new Pair<Integer, NumberVector<?, ?>>(id, new DoubleVector(data));
        } catch (IOException e) {
          throw new IllegalStateException("Could not read from data file", e);
        }
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
      
    };
  }
  
  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.Partition#getSize()
   */
  @Override
  public int getSize() {
    open();
    return (int) (this.storageFile.length() / this.recordSize);
  }
  
  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.Partition#getDimensionality()
   */
  @Override
  public int getDimensionality() {
    return this.dimensionality;
  }
  
  /* (non-Javadoc)
   * @see experimentalcode.frankenb.model.Partition#copyToFile(java.io.File)
   */
  @Override
  public void copyToFile(File file) throws IOException {
    close();
    
    InputStream in = null;
    OutputStream out = null;
    try {
      in = new FileInputStream(this.storageFile);
      out = new FileOutputStream(file);
      
      int read = 0;
      byte[] buffer = new byte[1024];
      
      while ((read = in.read(buffer)) != -1) {
        out.write(buffer, 0, read);
      }
      
    } finally {
      if (in != null) in.close();
      if (out != null) out.close();
      open();
    }
    
  }
  
  public static DiskBackedPartition loadFromFile(int dimensionality, File file) throws IOException {
    return new DiskBackedPartition(dimensionality, file);
  }
  
}
