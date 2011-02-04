/**
 * 
 */
package experimentalcode.frankenb.algorithms;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import experimentalcode.frankenb.algorithms.pairing.CrossPartitionPairing;
import experimentalcode.frankenb.algorithms.partitioning.GridPartitioning;
import experimentalcode.frankenb.algorithms.projection.RandomProjection;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class RandomProjectionVectorApproximationCrossPairingDividerAlgorithm extends AbstractDividerAlgorithm {

  public RandomProjectionVectorApproximationCrossPairingDividerAlgorithm(Parameterization config) {
    this.addProjection(new RandomProjection(config));
    this.setPartitioning(new GridPartitioning(config));
    this.setPairing(new CrossPartitionPairing(config));
  }
  
}
