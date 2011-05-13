package experimentalcode.frankenb.model;

import de.lmu.ifi.dbs.elki.math.MathUtil;
import experimentalcode.frankenb.algorithms.partitioning.DBIDPartition;

public class PartitionPairing {

  private final DBIDPartition partitionOne, partitionTwo;

  private int storageId = 0;

  private boolean hasResult = false;

  public PartitionPairing(DBIDPartition partitionOne, DBIDPartition partitionTwo) {
    this.partitionOne = partitionOne;
    this.partitionTwo = partitionTwo;
  }

  /**
   * This is used by the PackageDescriptor to identify this partition pairing
   * within it's internal representation
   * 
   * @param storageId
   */
  protected void setStorageId(int storageId) {
    this.storageId = storageId;
  }

  protected int getStorageId() {
    return this.storageId;
  }

  public DBIDPartition getPartitionOne() {
    return this.partitionOne;
  }

  public DBIDPartition getPartitionTwo() {
    return this.partitionTwo;
  }

  public boolean isSelfPairing() {
    return (this.partitionOne != null && this.partitionOne.equals(this.partitionTwo));
  }

  public boolean hasResult() {
    return this.hasResult;
  }

  protected void setHasResult(boolean hasResult) {
    this.hasResult = hasResult;
  }

  /**
   * Returns the estimated amount of unique ids within the partition. This is
   * just a guess as we assume that two different packages have completely
   * disjunct ids.
   * 
   * @return
   */
  public int getEstimatedUniqueIdsAmount() {
    if(this.isSelfPairing()) {
      return this.getPartitionOne().getSize();
    }
    else {
      return this.getPartitionOne().getSize() + this.getPartitionTwo().getSize();
    }
  }

  /**
   * Amount of at least necessary calculations
   * 
   * @return
   */
  public long getCalculations() {
    long totalCalculations = 0;
    if(this.getPartitionOne() != null && this.getPartitionTwo() != null) {
      if(this.isSelfPairing()) {
        totalCalculations = MathUtil.sumFirstIntegers(this.getPartitionOne().getSize() - 1);
      }
      else {
        totalCalculations = this.getPartitionOne().getSize() * this.getPartitionTwo().getSize();
      }
    }
    return totalCalculations;
  }

  @Override
  public String toString() {
    if(partitionOne == null || partitionTwo == null) {
      return super.toString();
    }
    return String.format("[ (%,d) <=> (%,d) ]", partitionOne.getSize(), partitionTwo.getSize());
    //return String.format("[ %05d (%,d) <=> %05d (%,d) ]", partitionOne.getId(), partitionOne.getSize(), partitionTwo.getId(), partitionTwo.getSize());
  }
}