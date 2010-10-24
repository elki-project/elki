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
  
  private final RandomAccessFile directoryFile, dataFile;
  private final FileLock directoryFileLock, dataFileLock;
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
   * @param aDirectoryFile
   * @param aDataFile
   * @param keySerializer
   * @param valueSerializer
   * @param maxKeysPerBucket
   * @throws IOException
   */
  public DynamicBPlusTree(File aDirectoryFile, File aDataFile, ConstantSizeByteBufferSerializer<K> keySerializer, ByteBufferSerializer<V> valueSerializer, int maxKeysPerBucket) throws IOException {
    if ((aDirectoryFile.exists() && aDirectoryFile.length() > 0L) || (aDataFile.exists() && aDataFile.length() > 0))
      throw new IOException("The data and/or directory file already exists");

    this.directoryFile = new RandomAccessFile(aDirectoryFile, "rw");
    this.dataFile = new RandomAccessFile(aDataFile, "rw");
    this.directoryFileLock = this.directoryFile.getChannel().lock();
    this.dataFileLock = this.dataFile.getChannel().lock();
    
    this.keySerializer = keySerializer;
    this.valueSerializer = valueSerializer;
    this.maxKeysPerBucket = maxKeysPerBucket;
    
    
    initTree();
    createTree();
  }
  
  /**
   * Uses a already existing tree
   * 
   * @param aDirectoryFile
   * @param aDataFile
   * @param keySerializer
   * @param valueSerializer
   * @throws IOException
   */
  public DynamicBPlusTree(File aDirectoryFile, File aDataFile, ConstantSizeByteBufferSerializer<K> keySerializer, ByteBufferSerializer<V> valueSerializer) throws IOException {
    if (!aDirectoryFile.exists()) throw new IOException("Unable to find directory file");
    
    this.directoryFile = new RandomAccessFile(aDirectoryFile, "rw");
    this.dataFile = new RandomAccessFile(aDataFile, "rw");
    this.directoryFileLock = this.directoryFile.getChannel().lock();
    this.dataFileLock = this.dataFile.getChannel().lock();
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
    directoryFile.seek(0);
    this.maxKeysPerBucket = directoryFile.readInt();
    this.nextBucketPosition = directoryFile.readLong();
    this.rootBucketPosition = directoryFile.readLong();
    this.nextDataPosition = directoryFile.readLong();
    this.size = directoryFile.readLong();
    
    initTree();
  }
  
  private void writeHeader() throws IOException {
    long lastPosition = directoryFile.getFilePointer();
    directoryFile.seek(0);
    
    directoryFile.writeInt(this.maxKeysPerBucket);
    directoryFile.writeLong(this.nextBucketPosition);
    directoryFile.writeLong(this.rootBucketPosition);
    directoryFile.writeLong(this.nextDataPosition);
    directoryFile.writeLong(this.size);
    
    directoryFile.seek(lastPosition);
  }
  
  /**
   * Adds a key value pair to the tree. If the pair
   * was added true is returned, if the key already
   * exists, false is returned
   *  
   * @param key
   * @param value
   * @throws IOException 
   */
  public boolean put(K key, V value) throws IOException {
    Trace trace = findBucket(this.rootBucketPosition, key);
    if (trace.hasTargetAddress()) return false;
    
    long dataAddress = putData(value);
    addToBucket(trace, key, dataAddress, 0L);

    this.size++;
    this.writeHeader();
    
    return true;
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
  
  private V getData(long address) throws IOException {
    dataFile.seek(address);
    long size = dataFile.readLong();
    LOG.debug("Loading data @" + address + "(size: " + size + ")");
    
    ByteBuffer buffer = dataFile.getChannel().map(MapMode.READ_ONLY, this.dataFile.getFilePointer(), size);
    return this.valueSerializer.fromByteBuffer(buffer);
  }  
  
  private long putData(V value) throws IOException {
    long position = this.nextDataPosition;
    long size = valueSerializer.getByteSize(value);
    
    dataFile.seek(position);
    dataFile.setLength(position + size + (Long.SIZE / 8));
    dataFile.writeLong(size);
    
    ByteBuffer buffer = dataFile.getChannel().map(MapMode.READ_WRITE, this.dataFile.getFilePointer(), size);
    this.valueSerializer.toByteBuffer(buffer, value);
    
    this.nextDataPosition += (size + (Long.SIZE / 8));
    this.writeHeader();
    
    return position;
  }
  
  private void addToBucket(Trace bucketTrace, K key, long leftAddress, long rightAddress) throws IOException {
    long bucketPosition = bucketTrace.popBucketAddress();
    directoryFile.seek(bucketPosition);
    
    int size = directoryFile.readInt();
    boolean isDirectoryBucket = directoryFile.readBoolean();
    
    //now we search for the insert position
    int position = size;
    long insertAddress = this.directoryFile.getFilePointer() + size * (this.keySerializer.getConstantByteSize() + (Long.SIZE / 8));
    for (int i = 0; i < size; ++i) {
      directoryFile.readLong();
      
      ByteBuffer buffer = directoryFile.getChannel().map(MapMode.READ_ONLY, directoryFile.getFilePointer(), keySerializer.getConstantByteSize());
      directoryFile.seek(directoryFile.getFilePointer() + keySerializer.getConstantByteSize());

      K aKey = this.keySerializer.fromByteBuffer(buffer);
      if (aKey.compareTo(key) > 0) {
        insertAddress = directoryFile.getFilePointer() - (this.keySerializer.getConstantByteSize()) - Long.SIZE / 8;
        position = i;
        break;
      }
    }    
    
    int keyPlusAddressSize = (Long.SIZE / 8) + this.keySerializer.getConstantByteSize();
    
    if (size < this.maxKeysPerBucket) {
      //if there is enough space in the bucket for one more
      if (position < size) {
        directoryFile.seek(insertAddress);
        byte[] buffer = new byte[((size - position) * keyPlusAddressSize) + Long.SIZE / 8];
        long newPosition = insertAddress + keyPlusAddressSize; 
        directoryFile.read(buffer);
        directoryFile.seek(newPosition);
        directoryFile.write(buffer);
      }
      
      //actually write key
      directoryFile.seek(insertAddress);
      directoryFile.writeLong(leftAddress);
      ByteBuffer bBuffer = directoryFile.getChannel().map(MapMode.READ_WRITE, directoryFile.getFilePointer(), keySerializer.getConstantByteSize());
      this.keySerializer.toByteBuffer(bBuffer, key);
      
      if (isDirectoryBucket) {
        directoryFile.seek(directoryFile.getFilePointer() + keySerializer.getConstantByteSize());
        directoryFile.writeLong(rightAddress);
      }
      
      directoryFile.seek(bucketPosition);
      directoryFile.writeInt(size + 1);
      
    } else {
      //we have to split this bucket
      //so we take the upper half of this bucket and move them to a new bucket
      long newBucketPosition = createBucket(isDirectoryBucket);

      int firstHalfSize = this.maxKeysPerBucket / 2;
      int secondHalfSize = size - firstHalfSize;
      
      byte[] buffer = new byte[secondHalfSize * keyPlusAddressSize + (isDirectoryBucket ? Long.SIZE / 8 : 0)];
      
      directoryFile.seek(bucketPosition + ((Integer.SIZE + Byte.SIZE) / 8) + firstHalfSize * keyPlusAddressSize);
      directoryFile.read(buffer);
      
      directoryFile.seek(newBucketPosition);
      directoryFile.writeInt(secondHalfSize);
      directoryFile.writeBoolean(isDirectoryBucket);
      directoryFile.write(buffer);
      
      directoryFile.seek(bucketPosition);
      directoryFile.writeInt(firstHalfSize);
      
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
  

  
  private K getLowestOfBucket(long bucketPosition) throws IOException {
    this.directoryFile.seek(bucketPosition);
    int size = directoryFile.readInt();
    directoryFile.readBoolean();
    
    if (size == 0) return null;
    directoryFile.readLong();
    ByteBuffer buffer = directoryFile.getChannel().map(MapMode.READ_ONLY, directoryFile.getFilePointer(), keySerializer.getConstantByteSize());
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
    
    directoryFile.seek(startAddress);
    int size = directoryFile.readInt();
    boolean directoryPointers = directoryFile.readBoolean();
    
    
    if (size == 0) {
      //this has to be the root
      return trace;
    }
    
    //read bucket
    boolean found = false;
    for (int i = 0; i < size; ++i) {
      long targetAddress = directoryFile.readLong();
      ByteBuffer buffer = directoryFile.getChannel().map(MapMode.READ_ONLY, directoryFile.getFilePointer(), keySerializer.getConstantByteSize());
      directoryFile.seek(directoryFile.getFilePointer() + keySerializer.getConstantByteSize());
      
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
      long address = directoryFile.readLong();
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
    
    directoryFile.setLength(nextBucketPosition + this.bucketByteSize);
    directoryFile.seek(nextBucketPosition);
    directoryFile.writeInt(0); //0 size
    directoryFile.writeBoolean(directoryAddresses);
    directoryFile.seek(nextBucketPosition);
    
    this.nextBucketPosition += bucketByteSize;
    return bucketPosition;
  }
  
  public void close() throws IOException {
    this.directoryFileLock.release();
    this.dataFileLock.release();
    
    this.directoryFile.close();
    this.dataFile.close();
  }

}
