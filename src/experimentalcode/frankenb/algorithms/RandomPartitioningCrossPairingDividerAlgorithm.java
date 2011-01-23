/**
 * 
 */
package experimentalcode.frankenb.algorithms;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import experimentalcode.frankenb.model.pairer.CrossPartitionPairer;
import experimentalcode.frankenb.model.partitioner.RandomPartitioner;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class RandomPartitioningCrossPairingDividerAlgorithm extends AbstractDividerAlgorithm {

  public RandomPartitioningCrossPairingDividerAlgorithm(Parameterization config) {
    this.setPartitioning(new RandomPartitioner(config));
    this.setPairing(new CrossPartitionPairer(config));
  }
  
}
