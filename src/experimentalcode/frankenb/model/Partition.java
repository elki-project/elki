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
import java.nio.ByteBuffer;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.persistent.OnDiskArray;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * A part of a database that is normally used in a package for precalculating
 * distances for knn. This datatype is backed up on disk so that it should be suitable
 * even for a huge amount of datasets.
 * 
 * @author Florian Frankenberger
 */
public class Partition implements Iterable<Pair<Integer, NumberVector<?, ?>>> {

  private static final int PARTITION_MAGIC_NUMBER = 830920;
  
  private final File storageFile;
  private final int dimensionality;
  
  private OnDiskArray onDiskArray;
  
  public Partition(int dimensionality) throws IOException {
    this(dimensionality, null);
  }
  
  private Partition(int dimensionality, File storageFile) throws IOException {
    this.dimensionality = dimensionality;
    
    if (storageFile == null) {
      storageFile = File.createTempFile("partition_", null);
      storageFile.delete();
    }
    
    this.storageFile = storageFile;
    
    open();
  }
  
  private void open() throws IOException {
    if (this.storageFile.exists()) {
      onDiskArray = new OnDiskArray(
          this.storageFile, 
          PARTITION_MAGIC_NUMBER, 
          0, // = no extra header
          this.dimensionality * 8 + 4, // = 64bit of a double * dimensionality + 1 int id
          true
        );    
    } else {
      onDiskArray = new OnDiskArray(
          this.storageFile, 
          PARTITION_MAGIC_NUMBER, 
          0, // = no extra header
          this.dimensionality * 8 + 4, // = 64bit of a double * dimensionality + 1 int id
          0
        );    
    }
  }
  
  public File getStorageFile() {
    return this.storageFile;
  }
  
  public void addVector(int id, NumberVector<?, ?> vector) throws IOException {
    onDiskArray.resizeFile(onDiskArray.getNumRecords() + 1);
    
    //aquire the buffer
    ByteBuffer buffer = onDiskArray.getRecordBuffer(onDiskArray.getNumRecords() - 1);

    //first we write the id
    buffer.putInt(id);
    
    // at this point we assume that all elements have the same
    // dimensionality within a database
    for (int k = 1; k <= dimensionality; ++k) {
      buffer.putDouble(vector.doubleValue(k));
    }
  }
  
  public void close() throws IOException {
    onDiskArray.close();
  }
  
  @Override
  public Iterator<Pair<Integer, NumberVector<?, ?>>> iterator() {
    return new Iterator<Pair<Integer, NumberVector<?, ?>>>() {

      private int position = 0;
      
      @Override
      public boolean hasNext() {
        return position < onDiskArray.getNumRecords();
      }

      @Override
      public Pair<Integer, NumberVector<?, ?>> next() {
        if (position > onDiskArray.getNumRecords() - 1) 
          throw new IllegalStateException("No more items");
        try {
          ByteBuffer buffer = onDiskArray.getRecordBuffer(position);
          int id = buffer.getInt();
          
          double[] data = new double[dimensionality];
          for (int k = 0; k < dimensionality; ++k) {
            data[k] = buffer.getDouble();
          }
          
          position++;
          return new Pair<Integer, NumberVector<?, ?>>(id, new DoubleVector(data));
        } catch (IOException e) {
          throw new IllegalStateException("Could not read from data file");
        }
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
      
    };
  }
  
  public int getSize() {
    return onDiskArray.getNumRecords();
  }
  
  public int getDimensionality() {
    return this.dimensionality;
  }
  
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
  
  public static Partition loadFromFile(int dimensionality, File file) throws IOException {
    return new Partition(dimensionality, file);
  }
  
}
