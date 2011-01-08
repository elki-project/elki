package experimentalcode.frankenb.model;

import experimentalcode.frankenb.model.ifaces.IPartition;

public class PartitionPairing {

  private final IPartition partitionOne, partitionTwo;
  private DynamicBPlusTree<Integer, DistanceList> result;
  
  public PartitionPairing(IPartition partitionOne, IPartition partitionTwo) {
    this(partitionOne, partitionTwo, null);
  }
  
  public PartitionPairing(IPartition partitionOne, IPartition partitionTwo, DynamicBPlusTree<Integer, DistanceList> result) {
    this.partitionOne = partitionOne;
    this.partitionTwo = partitionTwo; 
    this.result = result;
  }
  
  public IPartition getPartitionOne() {
    return this.partitionOne;
  }
  
  public IPartition getPartitionTwo() {
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
