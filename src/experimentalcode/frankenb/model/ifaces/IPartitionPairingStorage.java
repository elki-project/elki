/**
 * 
 */
package experimentalcode.frankenb.model.ifaces;

import experimentalcode.frankenb.model.PartitionPairing;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public interface IPartitionPairingStorage {

  public void setPartitionPairings(int partitionPairings);
  
  public void add(PartitionPairing partitionPairing);
  
}
