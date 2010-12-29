package experimentalcode.frankenb.model;

import experimentalcode.frankenb.model.ifaces.Partition;

public class PartitionPairing {

  private final Partition partitionOne, partitionTwo;
  private DynamicBPlusTree<Integer, DistanceList> result;
  
  public PartitionPairing(Partition partitionOne, Partition partitionTwo) {
    this(partitionOne, partitionTwo, null);
  }
  
  public PartitionPairing(Partition partitionOne, Partition partitionTwo, DynamicBPlusTree<Integer, DistanceList> result) {
    this.partitionOne = partitionOne;
    this.partitionTwo = partitionTwo; 
    this.result = result;
  }
  
  public Partition getPartitionOne() {
    return this.partitionOne;
  }
  
  public Partition getPartitionTwo() {
    return this.partitionTwo;
  }
  
  public void setResult(DynamicBPlusTree<Integer, DistanceList> result) {
    this.result = result;
  }
  
  public DynamicBPlusTree<Integer, DistanceList> getResult() {
    return this.result;
  }
  
  public boolean hasResult() {
    return this.result != null;
  }
  
}
