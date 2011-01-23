package experimentalcode.frankenb.model;

import experimentalcode.frankenb.model.ifaces.IPartition;

public class PartitionPairing {

  private final IPartition partitionOne, partitionTwo;
  
  public PartitionPairing(IPartition partitionOne, IPartition partitionTwo) {
    this.partitionOne = partitionOne;
    this.partitionTwo = partitionTwo; 
  }
  
  public IPartition getPartitionOne() {
    return this.partitionOne;
  }
  
  public IPartition getPartitionTwo() {
    return this.partitionTwo;
  }
  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    if (partitionOne == null || partitionTwo == null) return super.toString();
    return String.format("[ %05d <=> %05d ]", partitionOne.getId(), partitionTwo.getId());
  }
  
}
