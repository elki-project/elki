/**
 * 
 */
package experimentalcode.frankenb.model;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.ByteBufferSerializer;
import experimentalcode.frankenb.model.ifaces.ConstantSizeByteBufferSerializer;

/**
 * A B+ Tree implementation that can handle variable data page sizes. This tree
 * can't delete or overwrite values.
 * 
 * <p/>
 * Directory format:
 * <ul>
 *  <li>1 int determining the max of keys per bucket</li>
 *  <li>1 long determining the next free bucket position</li>
 *  <li>1 long pointing to the root of the tree</li>
 *  <li>1 long determining the amount of elements in the tree</li> 
 * </ul>
 * <p/>
 * Bucket format:
 * <ul>
 *  <li>1 int determining the size of the bucket</li>
 *  <li>1 byte determining if this is a bucket that points to directory or data addresses</li>
 *  <li>1..key_max keys</li>
 * </ul>
 * Key format:
 * <ul>
 *  <li>1 long pointer</li>
 *  <li>1 K key</li>
 *  <li>1 long pointer</li>
 *  <li>1 K key</li>
 *  <li>...</li>
 *  <li>1 long pointer</li>
 * </ul>
 * 
 * @author Florian Frankenberger
 */
public class DynamicBPlusTree<K extends Comparable<K>, V> {
  
  private static final Logging LOG = Logging.getLogger(DynamicBPlusTree.class);
  
  private final RandomAccessFile directoryRandomAccessFile, dataRandomAccessFile;
  private final File directoryFile, dataFile;
//  private final FileLock directoryFileLock, dataFileLock;
  private final ConstantSizeByteBufferSerializer<K> keySerializer;
  private final ByteBufferSerializer<V> valueSerializer;
  
  private long nextBucketPosition = 4 * Long.SIZE / 8 + Integer.SIZE / 8;
  
  private int bucketByteSize;
  private long rootBucketPosition;
  private int maxKeysPerBucket;
  
  private long nextDataPosition = 0;
  private long size = 0;
  
  private static final class Trace {
    private Long targetAddress;
    private List<Long> bucketAddresses = new ArrayList<Long>();
    
    public Trace() {
    }
    
    public Trace(long bucketAddress) {
      this.bucketAddresses.add(bucketAddress);
    }
    
    public void addBucketAddress(long bucketAddress) {
      this.bucketAddresses.add(bucketAddress);
    }
    
    public void addAll(Trace trace) {
      this.bucketAddresses.addAll(trace.bucketAddresses);
      if (trace.hasTargetAddress()) {
        this.targetAddress = trace.getTargetAddress();
      }
    }
    
    public long popBucketAddress() {
      return this.bucketAddresses.remove(this.bucketAddresses.size() - 1);
    }
    
    public boolean isEmpty() {
      return this.bucketAddresses.isEmpty();
    }
    
    public void setTargetAddress(long targetAddress) {
      this.targetAddress = targetAddress;
    }
    
    public long getTargetAddress() {
      return this.targetAddress;
    }
    
    public boolean hasTargetAddress() {
      return this.targetAddress != null;
    }
    
  }
  
  /**
   * Creates a new BPlusTree
   * 
   * @param directoryFile
   * @param dataFile
   * @param keySerializer
   * @param valueSerializer
   * @param maxKeysPerBucket
   * @throws IOException
   */
  public DynamicBPlusTree(File directoryFile, File dataFile, ConstantSizeByteBufferSerializer<K> keySerializer, ByteBufferSerializer<V> valueSerializer, int maxKeysPerBucket) throws IOException {
    if ((directoryFile.exists() && directoryFile.length() > 0L) || (dataFile.exists() && dataFile.length() > 0))
      throw new IOException("The data and/or directory file already exists");

    this.directoryRandomAccessFile = new RandomAccessFile(directoryFile, "rw");
    this.directoryFile = directoryFile;
    this.dataRandomAccessFile = new RandomAccessFile(dataFile, "rw");
    this.dataFile = dataFile;
//    this.directoryFileLock = this.directoryFile.getChannel().lock();
//    this.dataFileLock = this.dataFile.getChannel().lock();
    
    this.keySerializer = keySerializer;
    this.valueSerializer = valueSerializer;
    this.maxKeysPerBucket = maxKeysPerBucket;
    
    
    initTree();
    createTree();
  }
  
  /**
   * Uses a already existing tree
   * 
   * @param directoryFile
   * @param dataFile
   * @param keySerializer
   * @param valueSerializer
   * @throws IOException
   */
  public DynamicBPlusTree(File directoryFile, File dataFile, ConstantSizeByteBufferSerializer<K> keySerializer, ByteBufferSerializer<V> valueSerializer) throws IOException {
    if (!directoryFile.exists()) throw new IOException("Unable to find directory file");
    
    this.directoryRandomAccessFile = new RandomAccessFile(directoryFile, "rw");
    this.directoryFile = directoryFile;
    
    this.dataRandomAccessFile = new RandomAccessFile(dataFile, "rw");
    this.dataFile = dataFile;
//    this.directoryFileLock = this.directoryFile.getChannel().lock();
//    this.dataFileLock = this.dataFile.getChannel().lock();
    this.keySerializer = keySerializer;
    this.valueSerializer = valueSerializer;

    loadHeader();
    initTree();
  }
 
  private void initTree() {
    this.bucketByteSize = 
      this.keySerializer.getConstantByteSize() * maxKeysPerBucket //items
      + (maxKeysPerBucket + 1) * (Long.SIZE / 8) //pointers
      + Integer.SIZE / 8 + Byte.SIZE / 8; //size indicator
  }
  
  private void createTree() throws IOException {
    this.rootBucketPosition = createBucket(false);
    writeHeader();
  }
  
  private void loadHeader() throws IOException {
    directoryRandomAccessFile.seek(0);
    this.maxKeysPerBucket = directoryRandomAccessFile.readInt();
    this.nextBucketPosition = directoryRandomAccessFile.readLong();
    this.rootBucketPosition = directoryRandomAccessFile.readLong();
    this.nextDataPosition = directoryRandomAccessFile.readLong();
    this.size = directoryRandomAccessFile.readLong();
    
    initTree();
  }
  
  private void writeHeader() throws IOException {
    long lastPosition = directoryRandomAccessFile.getFilePointer();
    directoryRandomAccessFile.seek(0);
    
    directoryRandomAccessFile.writeInt(this.maxKeysPerBucket);
    directoryRandomAccessFile.writeLong(this.nextBucketPosition);
    directoryRandomAccessFile.writeLong(this.rootBucketPosition);
    directoryRandomAccessFile.writeLong(this.nextDataPosition);
    directoryRandomAccessFile.writeLong(this.size);
    
    directoryRandomAccessFile.seek(lastPosition);
  }
  
  /**
   * Adds a key value pair to the tree. 
   * <p/>
   * If the key already exists it is overwritten and the
   * data is put at the same position in the data file
   * but only if it's size hasn't changed else it is added
   * to the end of the data file.
   *  
   * @param key
   * @param value
   * @throws IOException 
   */
  public void put(K key, V value) throws IOException {
    Trace trace = findBucket(this.rootBucketPosition, key);
    if (trace.hasTargetAddress()) {
      //key already exists
      long dataAddress = trace.getTargetAddress();
      
      dataRandomAccessFile.seek(dataAddress);
      long dataSize = dataRandomAccessFile.readLong();
      long newDataSize = this.valueSerializer.getByteSize(value);
      if (newDataSize > dataSize) {
        dataAddress = putData(value);
        updateDataBucket(trace.popBucketAddress(), key, dataAddress);
      } else {
        putData(value, dataAddress);
      }
    } else {
      //key does not exist
      long dataAddress = putData(value);
      addToBucket(trace, key, dataAddress, 0L);

      this.size++;
      this.writeHeader();
    }
  }
  
  public V get(K key) throws IOException {
    Trace trace = findBucket(this.rootBucketPosition, key);
    if (!trace.hasTargetAddress()) {
      return null;
    } else {
      return getData(trace.getTargetAddress());
    }
  }
  
  public long getSize() {
    return this.size;
  }
  
  public File getDirectoryFile() {
    return directoryFile;
  }
  
  public File getDataFile() {
    return dataFile;
  }
  
  private V getData(long address) throws IOException {
    dataRandomAccessFile.seek(address);
    long size = dataRandomAccessFile.readLong();
    LOG.debug("Loading data @" + address + "(size: " + size + ")");
    
    ByteBuffer buffer = dataRandomAccessFile.getChannel().map(MapMode.READ_ONLY, this.dataRandomAccessFile.getFilePointer(), size);
    return this.valueSerializer.fromByteBuffer(buffer);
  }  
  
  private long putData(V value) throws IOException {
    long position = this.nextDataPosition;
    long size = getDataSize(value);
    dataRandomAccessFile.setLength(position + size + (Long.SIZE / 8));

    putData(value, position);
    
    this.nextDataPosition += (size + (Long.SIZE / 8));
    this.writeHeader();
    
    return position;
  }
  
  private long getDataSize(V value) throws IOException {
    long size = valueSerializer.getByteSize(value);
    if (valueSerializer instanceof ConstantSizeByteBufferSerializer<?>) {
      size = ((ConstantSizeByteBufferSerializer<V>) valueSerializer).getConstantByteSize();
    }
    return size;
  }
  
  private void putData(V value, long position) throws IOException {
    long size = getDataSize(value);
    
    dataRandomAccessFile.seek(position);
    dataRandomAccessFile.writeLong(size);
    
    ByteBuffer buffer = dataRandomAccessFile.getChannel().map(MapMode.READ_WRITE, this.dataRandomAccessFile.getFilePointer(), size);
    this.valueSerializer.toByteBuffer(buffer, value);
  }
  
  private void addToBucket(Trace bucketTrace, K key, long leftAddress, long rightAddress) throws IOException {
    long bucketPosition = bucketTrace.popBucketAddress();
    directoryRandomAccessFile.seek(bucketPosition);
    
    int size = directoryRandomAccessFile.readInt();
    boolean isDirectoryBucket = directoryRandomAccessFile.readBoolean();
    
    //now we search for the insert position
    int position = size;
    long insertAddress = this.directoryRandomAccessFile.getFilePointer() + size * (this.keySerializer.getConstantByteSize() + (Long.SIZE / 8));
    for (int i = 0; i < size; ++i) {
      directoryRandomAccessFile.readLong();
      
      ByteBuffer buffer = directoryRandomAccessFile.getChannel().map(MapMode.READ_ONLY, directoryRandomAccessFile.getFilePointer(), keySerializer.getConstantByteSize());
      directoryRandomAccessFile.seek(directoryRandomAccessFile.getFilePointer() + keySerializer.getConstantByteSize());

      K aKey = this.keySerializer.fromByteBuffer(buffer);
      if (aKey.compareTo(key) > 0) {
        insertAddress = directoryRandomAccessFile.getFilePointer() - (this.keySerializer.getConstantByteSize()) - Long.SIZE / 8;
        position = i;
        break;
      }
    }    
    
    int keyPlusAddressSize = (Long.SIZE / 8) + this.keySerializer.getConstantByteSize();
    
    if (size < this.maxKeysPerBucket) {
      //if there is enough space in the bucket for one more
      if (position < size) {
        directoryRandomAccessFile.seek(insertAddress);
        byte[] buffer = new byte[((size - position) * keyPlusAddressSize) + Long.SIZE / 8];
        long newPosition = insertAddress + keyPlusAddressSize; 
        directoryRandomAccessFile.read(buffer);
        directoryRandomAccessFile.seek(newPosition);
        directoryRandomAccessFile.write(buffer);
      }
      
      //actually write key
      directoryRandomAccessFile.seek(insertAddress);
      directoryRandomAccessFile.writeLong(leftAddress);
      ByteBuffer bBuffer = directoryRandomAccessFile.getChannel().map(MapMode.READ_WRITE, directoryRandomAccessFile.getFilePointer(), keySerializer.getConstantByteSize());
      this.keySerializer.toByteBuffer(bBuffer, key);
      
      if (isDirectoryBucket) {
        directoryRandomAccessFile.seek(directoryRandomAccessFile.getFilePointer() + keySerializer.getConstantByteSize());
        directoryRandomAccessFile.writeLong(rightAddress);
      }
      
      directoryRandomAccessFile.seek(bucketPosition);
      directoryRandomAccessFile.writeInt(size + 1);
      
    } else {
      //we have to split this bucket
      //so we take the upper half of this bucket and move them to a new bucket
      long newBucketPosition = createBucket(isDirectoryBucket);

      int firstHalfSize = this.maxKeysPerBucket / 2;
      int secondHalfSize = size - firstHalfSize;
      
      byte[] buffer = new byte[secondHalfSize * keyPlusAddressSize + (isDirectoryBucket ? Long.SIZE / 8 : 0)];
      
      directoryRandomAccessFile.seek(bucketPosition + ((Integer.SIZE + Byte.SIZE) / 8) + firstHalfSize * keyPlusAddressSize);
      directoryRandomAccessFile.read(buffer);
      
      directoryRandomAccessFile.seek(newBucketPosition);
      directoryRandomAccessFile.writeInt(secondHalfSize);
      directoryRandomAccessFile.writeBoolean(isDirectoryBucket);
      directoryRandomAccessFile.write(buffer);
      
      directoryRandomAccessFile.seek(bucketPosition);
      directoryRandomAccessFile.writeInt(firstHalfSize);
      
      if (position <= firstHalfSize) {
        addToBucket(new Trace(bucketPosition), key, leftAddress, rightAddress);
      } else {
        addToBucket(new Trace(newBucketPosition), key, leftAddress, rightAddress);
      }
      
      if (bucketTrace.isEmpty()) {
        //if the trace is empty this was the root - so we now create a new bucket and make it the root
        //(the root is ALWAYS a directory bucket)
        long newRootBucketPosition = createBucket(true);
        bucketTrace.addBucketAddress(newRootBucketPosition);
        this.rootBucketPosition = newRootBucketPosition;
        this.writeHeader();
      }
      
      K lowestKey = getLowestOfBucket(newBucketPosition);
      addToBucket(bucketTrace, lowestKey, bucketPosition, newBucketPosition);
      
    }   
    
  }
  
  private void updateDataBucket(long bucketPosition, K key, long newAddress) throws IOException {
    directoryRandomAccessFile.seek(bucketPosition);
    
    int size = directoryRandomAccessFile.readInt();
    directoryRandomAccessFile.readBoolean();
    
    //now we search for the insert position
    long insertAddress = -1;
    for (int i = 0; i < size; ++i) {
      directoryRandomAccessFile.readLong();
      
      ByteBuffer buffer = directoryRandomAccessFile.getChannel().map(MapMode.READ_ONLY, directoryRandomAccessFile.getFilePointer(), keySerializer.getConstantByteSize());
      directoryRandomAccessFile.seek(directoryRandomAccessFile.getFilePointer() + keySerializer.getConstantByteSize());

      K aKey = this.keySerializer.fromByteBuffer(buffer);
      if (aKey.equals(key)) {
        insertAddress = directoryRandomAccessFile.getFilePointer() - (this.keySerializer.getConstantByteSize()) - Long.SIZE / 8;
        break;
      }
    }
    
    if (insertAddress == -1) throw new IOException("Could not update key in bucket @" + bucketPosition);
    directoryRandomAccessFile.seek(insertAddress);
    directoryRandomAccessFile.writeLong(newAddress);
  }

  
  private K getLowestOfBucket(long bucketPosition) throws IOException {
    this.directoryRandomAccessFile.seek(bucketPosition);
    int size = directoryRandomAccessFile.readInt();
    directoryRandomAccessFile.readBoolean();
    
    if (size == 0) return null;
    directoryRandomAccessFile.readLong();
    ByteBuffer buffer = directoryRandomAccessFile.getChannel().map(MapMode.READ_ONLY, directoryRandomAccessFile.getFilePointer(), keySerializer.getConstantByteSize());
    K aKey = this.keySerializer.fromByteBuffer(buffer);
    return aKey;
  }
  
  /**
   * Tries to find the given key starting from the current position and
   * returns a trace in which the last element points to the current
   * buckets start position
   * 
   * @param key
   * @return
   * @throws IOException
   */
  private Trace findBucket(long startAddress, K key) throws IOException {
    Trace trace = new Trace();
    trace.addBucketAddress(startAddress);
    
    directoryRandomAccessFile.seek(startAddress);
    int size = directoryRandomAccessFile.readInt();
    boolean directoryPointers = directoryRandomAccessFile.readBoolean();
    
    
    if (size == 0) {
      //this has to be the root
      return trace;
    }
    
    //read bucket
    boolean found = false;
    for (int i = 0; i < size; ++i) {
      long targetAddress = directoryRandomAccessFile.readLong();
      ByteBuffer buffer = directoryRandomAccessFile.getChannel().map(MapMode.READ_ONLY, directoryRandomAccessFile.getFilePointer(), keySerializer.getConstantByteSize());
      directoryRandomAccessFile.seek(directoryRandomAccessFile.getFilePointer() + keySerializer.getConstantByteSize());
      
      K aKey = this.keySerializer.fromByteBuffer(buffer);
      if (aKey.compareTo(key) > 0) {
        if (directoryPointers) {
          trace.addAll(findBucket(targetAddress, key));
        }
        found = true;
        break;
      } else 
      if (aKey.compareTo(key) == 0 && !directoryPointers) {
        trace.setTargetAddress(targetAddress);
        found = true;
        break;
      }
    }
    
    if (!found && directoryPointers) {
      long address = directoryRandomAccessFile.readLong();
      trace.addAll(findBucket(address, key));
    }
    return trace;
  }
  
//  private void showBucket(long position) throws IOException {
//    System.out.println(bucketToString(position));
//  }
//  
//  private String bucketToString(long position) throws IOException {
//    Trace trace = new Trace();
//    trace.addBucketAddress(position);
//    
//    long positionB4 = directoryFile.getFilePointer();
//    directoryFile.seek(position);
//    int size = directoryFile.readInt();
//    boolean directoryPointers = directoryFile.readBoolean();
//    
//    StringBuilder sb = new StringBuilder();
//    sb.append("Bucket @" + position + " (size: " + size + ", directoryPointers: " + Boolean.toString(directoryPointers) + ") [");
//    
//    //read bucket
//    K lastKey = null;
//    boolean problem = false;
//    for (int i = 0; i < size; ++i) {
//      long targetAddress = directoryFile.readLong();
//      ByteBuffer buffer = directoryFile.getChannel().map(MapMode.READ_ONLY, directoryFile.getFilePointer(), keySerializer.getConstantByteSize());
//      directoryFile.seek(directoryFile.getFilePointer() + keySerializer.getConstantByteSize());
//      
//      K aKey = this.keySerializer.fromByteBuffer(buffer);
//      sb.append("Key " + aKey.toString() + " @" + (directoryFile.getFilePointer() - this.keySerializer.getConstantByteSize()) + " -> @" + targetAddress + ", ");
//      if (lastKey != null && lastKey.equals(aKey)) {
//        problem = true;
//      }
//      lastKey = aKey;
//    }
//    
//    if (problem) {
//      System.out.println(sb.toString());
//      throw new RuntimeException("Problem detected: two keys are equal!");
//    }
//    
//    if (directoryPointers) {
//      long address = directoryFile.readLong();
//      sb.append("-> @" + address);
//    }
//    
//    sb.append("]");
//    directoryFile.seek(positionB4);
//    return sb.toString();
//  }
  
  /**
   * Creates bucket on the next free place and sets the pointer
   * to point to the first byte of the bucket
   * 
   * @param size
   * @return
   * @throws IOException
   */
  private long createBucket(boolean directoryAddresses) throws IOException {
    long bucketPosition = nextBucketPosition;
    
    directoryRandomAccessFile.setLength(nextBucketPosition + this.bucketByteSize);
    directoryRandomAccessFile.seek(nextBucketPosition);
    directoryRandomAccessFile.writeInt(0); //0 size
    directoryRandomAccessFile.writeBoolean(directoryAddresses);
    directoryRandomAccessFile.seek(nextBucketPosition);
    
    this.nextBucketPosition += bucketByteSize;
    return bucketPosition;
  }
  
  public void close() throws IOException {
//    this.directoryFileLock.release();
//    this.dataFileLock.release();
    
    this.directoryRandomAccessFile.close();
    this.dataRandomAccessFile.close();
  }

}
