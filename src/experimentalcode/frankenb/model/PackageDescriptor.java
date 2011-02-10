/**
 * 
 */
package experimentalcode.frankenb.model;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

import experimentalcode.frankenb.model.datastorage.BufferedDiskBackedDataStorage;
import experimentalcode.frankenb.model.ifaces.IDataStorage;
import experimentalcode.frankenb.model.ifaces.IPartition;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class PackageDescriptor implements Iterable<PartitionPairing> {

  private static final String PARTITION_DAT_FILE_FORMAT = "partition%05d.dat";
  private static final String PAIRING_RESULT_FILE_PREFIX = "pairing_result_%05d";
  
  private int id;
  private int dimensionality;
  private int pairingsQuantity = 0;
  private final IDataStorage dataStorage;
  
  public static final int HEADER_SIZE = 3 * Integer.SIZE / 8;
  public static  final int PAIRING_DATA_SIZE = 3 * Integer.SIZE / 8 + 1 * Byte.SIZE / 8;
  private final File parentDirectory;
  
  private Set<IPartition> partitions = new HashSet<IPartition>();
  
  public PackageDescriptor(int id, int dimensionality, IDataStorage dataStorage) throws IOException {
    this.dataStorage = dataStorage;
    this.id = id;
    this.dimensionality = dimensionality;
    
    if (dataStorage.getSource().exists() && dataStorage.getSource().canRead() && dataStorage.getSource().length() > 0) {
      readHeader();
    } else
      if (id == -1 || dimensionality == -1) {
        throw new IOException("PackageDescriptor file is not existing, not readable or size zero (" + dataStorage.getSource() + ").");
      } else {
        writeHeader();
      }
    parentDirectory = dataStorage.getSource().getParentFile();
  }
  
  private void readHeader() throws IOException {
    dataStorage.seek(0);
    this.id = dataStorage.readInt();
    this.dimensionality = dataStorage.readInt();
    this.pairingsQuantity = dataStorage.readInt();
  }
  
  private void writeHeader() throws IOException {
    dataStorage.seek(0);
    if (pairingsQuantity == 0) {
      dataStorage.setLength(HEADER_SIZE);
    }
    dataStorage.writeInt(this.id);
    dataStorage.writeInt(this.dimensionality);
    dataStorage.writeInt(this.pairingsQuantity);
  }
  
  public int getId() {
    return this.id;
  }
  
  public int getPairings() {
    return this.pairingsQuantity;
  }
  
  public int getOriginalDimensionality() {
    return this.dimensionality;
  }
  
  public void addPartitionPairing(PartitionPairing pairing) throws IOException {
    dataStorage.setLength(HEADER_SIZE + (this.pairingsQuantity + 1) * PAIRING_DATA_SIZE); 
    //dataStorage.seek(HEADER_SIZE + this.pairingsQuantity * PAIRING_DATA_SIZE);
    
    if (!partitions.contains(pairing.getPartitionOne())) {
      File partitionOneFile = new File(parentDirectory, String.format(PARTITION_DAT_FILE_FORMAT, pairing.getPartitionOne().getId()));
      pairing.getPartitionOne().copyTo(partitionOneFile);
      partitions.add(pairing.getPartitionOne());
    }
    
    if (!partitions.contains(pairing.getPartitionTwo())) {
      File partitionTwoFile = new File(parentDirectory, String.format(PARTITION_DAT_FILE_FORMAT, pairing.getPartitionTwo().getId()));
      pairing.getPartitionTwo().copyTo(partitionTwoFile);
      partitions.add(pairing.getPartitionTwo());
    }
    
    pairing.setStorageId(pairingsQuantity);

    writePartitionPairingData(pairing);
    
    this.pairingsQuantity++;
    writeHeader();
  }
  
  private void writePartitionPairingData(PartitionPairing pairing) throws IOException {
    dataStorage.seek(HEADER_SIZE + pairing.getStorageId() * PAIRING_DATA_SIZE);
    
    dataStorage.writeInt(pairing.getPartitionOne().getId());
    dataStorage.writeInt(pairing.getPartitionTwo().getId());
    dataStorage.writeInt(pairing.getStorageId());
    dataStorage.writeBoolean(pairing.hasResult());
  }
  
  /**
   * Returns the {@link DynamicBPlusTree} result for the given pairing. If no result
   * is available a new tree is created and returned. To determine if a result is available
   * you should use {@link #hasResult(PartitionPairing)}. 
   * @param pairing
   * @return
   * @throws IOException
   */
  public DynamicBPlusTree<Integer, DistanceList> getResultTreeFor(PartitionPairing pairing) throws IOException {
    Pair<File, File> resultFiles = getResultFilesFor(pairing);
    if (!pairing.hasResult()) throw new RuntimeException("This pairing has no result.");
    return new DynamicBPlusTree<Integer, DistanceList>(
          new BufferedDiskBackedDataStorage(resultFiles.first),
          new BufferedDiskBackedDataStorage(resultFiles.second),
          new ConstantSizeIntegerSerializer(),
          new DistanceListSerializer()
    );
  }

  public DynamicBPlusTree<Integer, DistanceList> createResultTreeFor(PartitionPairing pairing, int maxKeysPerBucket) throws IOException {
    if (pairing.hasResult()) throw new RuntimeException("This pairing already has a result.");
    Pair<File, File> resultFiles = getResultFilesFor(pairing);
    if (resultFiles.first.exists()) {
      resultFiles.first.delete();
    }
    if (resultFiles.second.exists()) {
      resultFiles.second.delete();
    }
    return new DynamicBPlusTree<Integer, DistanceList>(
        new BufferedDiskBackedDataStorage(resultFiles.first),
        new BufferedDiskBackedDataStorage(resultFiles.second),
        new ConstantSizeIntegerSerializer(),
        new DistanceListSerializer(),
        maxKeysPerBucket
    );
  }
    
  public void setHasResultFor(PartitionPairing pairing) throws IOException {
    pairing.setHasResult(true);
    writePartitionPairingData(pairing);    
  }
  
  private Pair<File, File> getResultFilesFor(PartitionPairing pairing) {
    return new Pair<File, File>(
          new File(parentDirectory, String.format(PAIRING_RESULT_FILE_PREFIX + ".dir", pairing.getStorageId())),
          new File(parentDirectory, String.format(PAIRING_RESULT_FILE_PREFIX + ".dat", pairing.getStorageId()))
        );
  }
  
  public static PackageDescriptor readFromStorage(IDataStorage dataStorage) throws IOException {
    return new PackageDescriptor(-1, -1, dataStorage);
  }

  public void close() throws IOException {
    this.dataStorage.close();
  }
  
  /* (non-Javadoc)
   * @see java.lang.Iterable#iterator()
   */
  @Override
  public Iterator<PartitionPairing> iterator() {
    try {
      this.dataStorage.seek(HEADER_SIZE);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return new Iterator<PartitionPairing>() {

      int position = 0;
      
      @Override
      public boolean hasNext() {
        return position < pairingsQuantity;
      }

      @Override
      public PartitionPairing next() {
        try {
          File partitionOneFile = new File(parentDirectory, String.format(PARTITION_DAT_FILE_FORMAT, dataStorage.readInt()));
          File partitionTwoFile = new File(parentDirectory, String.format(PARTITION_DAT_FILE_FORMAT, dataStorage.readInt()));
          IPartition partitionOne = BufferedDiskBackedPartition.loadFromFile(partitionOneFile);
          IPartition partitionTwo = (partitionOneFile.equals(partitionTwoFile) ? partitionOne : BufferedDiskBackedPartition.loadFromFile(partitionTwoFile));
          
          position++;
          PartitionPairing partitionPairing = new PartitionPairing(partitionOne, partitionTwo);
          partitionPairing.setStorageId(dataStorage.readInt());
          partitionPairing.setHasResult(dataStorage.readBoolean());
          return partitionPairing;
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
      
    };
  }
  
}
