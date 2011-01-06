/**
 * 
 */
package experimentalcode.frankenb.model;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.ByteBufferSerializer;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.frankenb.model.ifaces.ConstantSizeByteBufferSerializer;
import experimentalcode.frankenb.model.ifaces.DataStorage;

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
public class DynamicBPlusTree<K extends Comparable<K>, V> implements Iterable<Pair<K, V>> {
  
  private static final int FIRST_BUCKET_POSITION = 4 * Long.SIZE / 8 + Integer.SIZE / 8;

  private static final Logging LOG = Logging.getLogger(DynamicBPlusTree.class);
  
  private final DataStorage directoryStorage, dataStorage;
  private final ConstantSizeByteBufferSerializer<K> keySerializer;
  private final ByteBufferSerializer<V> valueSerializer;
  
  private long nextBucketPosition = FIRST_BUCKET_POSITION;
  
  private int bucketByteSize;
  private long rootBucketPosition;
  private int maxKeysPerBucket;
  
  private long nextDataPosition = 0;
  private long size = 0;

  private int linkOffset;
  
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
  public DynamicBPlusTree(DataStorage directoryStorage, DataStorage dataStorage, ConstantSizeByteBufferSerializer<K> keySerializer, ByteBufferSerializer<V> valueSerializer, int maxKeysPerBucket) throws IOException {

    this.directoryStorage = directoryStorage;
    this.dataStorage = dataStorage;
    
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
  public DynamicBPlusTree(DataStorage directoryStorage, DataStorage dataStorage, ConstantSizeByteBufferSerializer<K> keySerializer, ByteBufferSerializer<V> valueSerializer) throws IOException {
    this.directoryStorage = directoryStorage;
    
    this.dataStorage = dataStorage;
    this.keySerializer = keySerializer;
    this.valueSerializer = valueSerializer;

    loadHeader();
    initTree();
  }
  
  /**
   * @return the maxKeysPerBucket
   */
  public int getMaxKeysPerBucket() {
    return this.maxKeysPerBucket;
  }
 
  private void initTree() {
    this.bucketByteSize = 
      this.keySerializer.getConstantByteSize() * maxKeysPerBucket //items
      + (maxKeysPerBucket + 1) * (Long.SIZE / 8) //pointers
      + Integer.SIZE / 8 + Byte.SIZE / 8; //size indicator
    this.linkOffset = (bucketByteSize - Long.SIZE / 8);
  }
  
  private void createTree() throws IOException {
    this.rootBucketPosition = createBucket(false);
    
    //set the last link to the next bucket to 0
    this.directoryStorage.seek(this.rootBucketPosition + this.linkOffset);
    this.directoryStorage.writeLong(0);
    
    writeHeader();
  }
  
  private void loadHeader() throws IOException {
    directoryStorage.seek(0);
    this.maxKeysPerBucket = directoryStorage.readInt();
    this.nextBucketPosition = directoryStorage.readLong();
    this.rootBucketPosition = directoryStorage.readLong();
    this.nextDataPosition = directoryStorage.readLong();
    this.size = directoryStorage.readLong();
    
    initTree();
  }
  
  private void writeHeader() throws IOException {
    long lastPosition = directoryStorage.getFilePointer();
    directoryStorage.seek(0);
    
    directoryStorage.writeInt(this.maxKeysPerBucket);
    directoryStorage.writeLong(this.nextBucketPosition);
    directoryStorage.writeLong(this.rootBucketPosition);
    directoryStorage.writeLong(this.nextDataPosition);
    directoryStorage.writeLong(this.size);
    
    directoryStorage.seek(lastPosition);
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
      
      dataStorage.seek(dataAddress);
      long dataSize = dataStorage.readLong();
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
  
  public DataStorage getDirectoryStorage() {
    return this.directoryStorage;
  }
  
  public DataStorage getDataStorage() {
    return this.dataStorage;
  }
  
  private V getData(long address) throws IOException {
    dataStorage.seek(address);
    long size = dataStorage.readLong();
    LOG.debug("Loading data @" + address + "(size: " + size + ")");
    
    ByteBuffer buffer = dataStorage.getReadOnlyByteBuffer(size);
    return this.valueSerializer.fromByteBuffer(buffer);
  }  
  
  private long putData(V value) throws IOException {
    long position = this.nextDataPosition;
    long size = getDataSize(value);
    dataStorage.setLength(position + size + (Long.SIZE / 8));

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
    
    dataStorage.seek(position);
    dataStorage.writeLong(size);
    
    ByteBuffer buffer = dataStorage.getByteBuffer(size);
    this.valueSerializer.toByteBuffer(buffer, value);
  }
  
  private void addToBucket(Trace bucketTrace, K key, long leftAddress, long rightAddress) throws IOException {
    long bucketPosition = bucketTrace.popBucketAddress();
    
    directoryStorage.seek(bucketPosition);
    
    int size = directoryStorage.readInt();
    boolean isDirectoryBucket = directoryStorage.readBoolean();
    
    //now we search for the insert position
    int position = size;
    long insertAddress = this.directoryStorage.getFilePointer() + size * (this.keySerializer.getConstantByteSize() + (Long.SIZE / 8));
    for (int i = 0; i < size; ++i) {
      directoryStorage.readLong();
      
      ByteBuffer buffer = directoryStorage.getReadOnlyByteBuffer(keySerializer.getConstantByteSize());
      directoryStorage.seek(directoryStorage.getFilePointer() + keySerializer.getConstantByteSize());

      K aKey = this.keySerializer.fromByteBuffer(buffer);
      if (aKey.compareTo(key) > 0) {
        insertAddress = directoryStorage.getFilePointer() - (this.keySerializer.getConstantByteSize()) - Long.SIZE / 8;
        position = i;
        break;
      }
    }    
    
    int keyPlusAddressSize = (Long.SIZE / 8) + this.keySerializer.getConstantByteSize();
    
    if (size < this.maxKeysPerBucket) {
      //if there is enough space in the bucket for one more
      if (position < size) {
        directoryStorage.seek(insertAddress);
        byte[] buffer = new byte[((size - position) * keyPlusAddressSize) + (isDirectoryBucket ? Long.SIZE / 8 : 0)];
        long newPosition = insertAddress + keyPlusAddressSize; 
        directoryStorage.read(buffer);
        directoryStorage.seek(newPosition);
        directoryStorage.write(buffer);
      }
      
      //actually write key
      directoryStorage.seek(insertAddress);
      directoryStorage.writeLong(leftAddress);
      ByteBuffer bBuffer = directoryStorage.getByteBuffer(keySerializer.getConstantByteSize());
      this.keySerializer.toByteBuffer(bBuffer, key);
      
      if (isDirectoryBucket) {
        directoryStorage.seek(directoryStorage.getFilePointer() + keySerializer.getConstantByteSize());
        directoryStorage.writeLong(rightAddress);
      }
      
      //update size
      directoryStorage.seek(bucketPosition);
      directoryStorage.writeInt(size + 1);
      
    } else {
      //we have to split this bucket
      //so we take the upper half of this bucket and move them to a new bucket
      long newBucketPosition = createBucket(isDirectoryBucket);

      int firstHalfSize = this.maxKeysPerBucket / 2;
      int secondHalfSize = size - firstHalfSize;
      
      byte[] buffer = new byte[secondHalfSize * keyPlusAddressSize + (isDirectoryBucket ? Long.SIZE / 8 : 0)];
      
      directoryStorage.seek(bucketPosition + ((Integer.SIZE + Byte.SIZE) / 8) + firstHalfSize * keyPlusAddressSize);
      directoryStorage.read(buffer);
      
      directoryStorage.seek(newBucketPosition);
      directoryStorage.writeInt(secondHalfSize);
      directoryStorage.writeBoolean(isDirectoryBucket);
      directoryStorage.write(buffer);
      
      directoryStorage.seek(bucketPosition);
      directoryStorage.writeInt(firstHalfSize);
      
      if (!isDirectoryBucket) {
        //read this bucket's next neighbor
        directoryStorage.seek(bucketPosition + this.linkOffset);
        long linkNextAddress = directoryStorage.readLong();
        directoryStorage.seek(bucketPosition + this.linkOffset);
        directoryStorage.writeLong(newBucketPosition);
        
        //set the new buckets neighbor to this buckets original neighbor
        directoryStorage.seek(newBucketPosition + this.linkOffset);
        directoryStorage.writeLong(linkNextAddress);
      }
      
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
    directoryStorage.seek(bucketPosition);
    
    int size = directoryStorage.readInt();
    directoryStorage.readBoolean();
    
    //now we search for the insert position
    long insertAddress = -1;
    for (int i = 0; i < size; ++i) {
      directoryStorage.readLong();
      
      ByteBuffer buffer = directoryStorage.getReadOnlyByteBuffer(keySerializer.getConstantByteSize());
      directoryStorage.seek(directoryStorage.getFilePointer() + keySerializer.getConstantByteSize());

      K aKey = this.keySerializer.fromByteBuffer(buffer);
      if (aKey.equals(key)) {
        insertAddress = directoryStorage.getFilePointer() - (this.keySerializer.getConstantByteSize()) - Long.SIZE / 8;
        break;
      }
    }
    
    if (insertAddress == -1) throw new IOException("Could not update key in bucket @" + bucketPosition);
    directoryStorage.seek(insertAddress);
    directoryStorage.writeLong(newAddress);
  }

  
  private K getLowestOfBucket(long bucketPosition) throws IOException {
    this.directoryStorage.seek(bucketPosition);
    int size = directoryStorage.readInt();
    directoryStorage.readBoolean();
    
    if (size == 0) return null;
    directoryStorage.readLong();
    ByteBuffer buffer = directoryStorage.getReadOnlyByteBuffer(keySerializer.getConstantByteSize());
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
    
    directoryStorage.seek(startAddress);
    int size = directoryStorage.readInt();
    boolean directoryPointers = directoryStorage.readBoolean();
    
    
    if (size == 0) {
      //this has to be the root
      return trace;
    }
    
    //read bucket
    boolean found = false;
    for (int i = 0; i < size; ++i) {
      long targetAddress = directoryStorage.readLong();
      ByteBuffer buffer = directoryStorage.getReadOnlyByteBuffer(keySerializer.getConstantByteSize());
      directoryStorage.seek(directoryStorage.getFilePointer() + keySerializer.getConstantByteSize());
      
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
      long address = directoryStorage.readLong();
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
    
    directoryStorage.setLength(nextBucketPosition + this.bucketByteSize);
    directoryStorage.seek(nextBucketPosition);
    directoryStorage.writeInt(0); //0 size
    directoryStorage.writeBoolean(directoryAddresses);
    directoryStorage.seek(nextBucketPosition);
    
    this.nextBucketPosition += bucketByteSize;
    return bucketPosition;
  }
  
  public void close() throws IOException {
//    this.directoryFileLock.release();
//    this.dataFileLock.release();
    
    this.directoryStorage.close();
    this.dataStorage.close();
  }

  /* (non-Javadoc)
   * @see java.lang.Iterable#iterator()
   * This iterator is not stable with regard to concurrent access to other functions.
   */
  @Override
  public Iterator<Pair<K, V>> iterator() {
    try {
      this.directoryStorage.seek(FIRST_BUCKET_POSITION);
      
      final int size = directoryStorage.readInt();
      directoryStorage.readBoolean();
      
      return new Iterator<Pair<K, V>>() {
  
        private boolean hasNextEntry = true;
        private int lastSize = size;
        private int position = 0;
        private long bucketPosition = FIRST_BUCKET_POSITION;
        
        @Override
        public boolean hasNext() {
          return hasNextEntry;
        }
  
        @Override
        public Pair<K, V> next() {
          if (!hasNextEntry) return null;
          try {
            long targetAddress = directoryStorage.readLong();
            ByteBuffer buffer = directoryStorage.getReadOnlyByteBuffer(keySerializer.getConstantByteSize());
            directoryStorage.seek(directoryStorage.getFilePointer() + keySerializer.getConstantByteSize());
            
            K aKey = keySerializer.fromByteBuffer(buffer);
            
            if (++position == lastSize) {
              //if we reached the end of this bucket then we jump to the next
              directoryStorage.seek(bucketPosition + linkOffset);
              bucketPosition = directoryStorage.readLong();
              if (bucketPosition > 0) {
                directoryStorage.seek(bucketPosition);
                lastSize = directoryStorage.readInt();
                directoryStorage.readBoolean();
                
                position = 0;
              } else {
                hasNextEntry = false;
              }
            }
            
            return new Pair<K, V>(aKey, getData(targetAddress));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
  
        @Override
        public void remove() {
          throw new UnsupportedOperationException();
        }
        
      };
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
