package experimentalcode.frankenb.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Iterator;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.frankenb.model.ifaces.IPartition;

/**
 * A part of a database that is normally used in a package for precalculating
 * distances for knn. This datatype is backed up on disk so that it should be
 * suitable even for a huge amount of datasets.
 * 
 * @author Florian Frankenberger
 */
public class DiskBackedPartition<V extends NumberVector<V, ?>> implements IPartition<V> {
  private int id = 0;

  private final File storageFile;

  private int dimensionality;

  private final int recordSize;

  private RandomAccessFile file = null;

  protected V prototype;

  public DiskBackedPartition(int id, int dimensionality, V prototype) throws IOException {
    this(id, dimensionality, null, prototype);
  }

  private DiskBackedPartition(int id, int dimensionality, File storageFile, V prototype) throws IOException {
    this.id = id;
    this.prototype = prototype;
    if(storageFile == null && dimensionality < 1) {
      throw new RuntimeException("You need to specify a dimensionality if you don't load a partition from disk");
    }
    this.dimensionality = dimensionality;
    this.recordSize = (dimensionality * (Double.SIZE / 8)) + (Integer.SIZE / 8);
    if(storageFile == null) {
      storageFile = File.createTempFile("partition_", null);
      storageFile.delete();
    }

    this.storageFile = storageFile;
  }

  @Override
  public int getId() {
    return this.id;
  }

  private void open() {
    if(file != null) {
      return;
    }

    try {
      file = new RandomAccessFile(this.storageFile, "rw");
      file.seek(0);
      if(this.storageFile.exists() && this.storageFile.length() > 0) {
        this.id = file.readInt();
        this.dimensionality = file.readInt();
        file.seek(this.storageFile.length());
      }
      else {
        file.writeInt(this.id);
        file.writeInt(this.dimensionality);
      }
    }
    catch(IOException e) {
      throw new RuntimeException("Could not open file", e);
    }
  }

  @Override
  public File getStorageFile() {
    return this.storageFile;
  }

  @Override
  public void addVector(DBID id, V vector) {
    try {
      open();
      long position = storageFile.length();
      file.setLength(storageFile.length() + recordSize);

      file.seek(position);

      // first we write the id
      file.writeInt(id.getIntegerID());

      // at this point we assume that all elements have the same
      // dimensionality within a database
      for(int k = 1; k <= dimensionality; ++k) {
        file.writeDouble(vector.doubleValue(k));
      }
    }
    catch(IOException e) {
      throw new RuntimeException("Can't add vector", e);
    }
  }

  @Override
  public void close() throws IOException {
    open();
    file.seek(0);
    file.close();
    file = null;
  }

  @Override
  public Iterator<Pair<DBID, V>> iterator() {
    open();
    return new DeserializingIterator();
  }

  @Override
  public int getSize() {
    open();
    return (int) (this.storageFile.length() / this.recordSize);
  }

  @Override
  public int getDimensionality() {
    return this.dimensionality;
  }

  @Override
  public void copyTo(File file) throws IOException {
    close();

    InputStream in = null;
    OutputStream out = null;
    try {
      in = new FileInputStream(this.storageFile);
      out = new FileOutputStream(file);

      int read = 0;
      byte[] buffer = new byte[1024];

      while((read = in.read(buffer)) != -1) {
        out.write(buffer, 0, read);
      }

    }
    finally {
      if(in != null) {
        in.close();
      }
      if(out != null) {
        out.close();
      }
      open();
    }

  }

  public static <V extends NumberVector<V, ?>> DiskBackedPartition<V> loadFromFile(File file, V prototype) throws IOException {
    return new DiskBackedPartition<V>(0, 0, file, prototype);
  }

  /**
   * Deserialize a single record from the file.
   * 
   * @param position Record number.
   * @return Reconstructed record
   * @throws IOException On IO errors.
   */
  protected Pair<DBID, V> deserializeRecord(int position) throws IOException {
    file.seek(position * recordSize);
    DBID id = DBIDUtil.importInteger(file.readInt());

    final double[] data = new double[dimensionality];
    for(int k = 0; k < dimensionality; ++k) {
      data[k] = file.readDouble();
    }
    final V vec = prototype.newNumberVector(data);
    return new Pair<DBID, V>(id, vec);
  }

  protected class DeserializingIterator implements Iterator<Pair<DBID, V>> {
    private int position = 0;

    @Override
    public boolean hasNext() {
      return position < getSize();
    }

    @Override
    public Pair<DBID, V> next() {
      if(position > getSize() - 1) {
        throw new IllegalStateException("No more items");
      }
      Pair<DBID, V> rec = null;
      try {
        rec = deserializeRecord(position);
      }
      catch(IOException e) {
        throw new IllegalStateException("Could not read from data file", e);
      }
      position++;
      return rec;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}