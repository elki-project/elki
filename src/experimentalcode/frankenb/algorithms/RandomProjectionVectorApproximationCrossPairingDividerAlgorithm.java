/**
 * 
 */
package experimentalcode.frankenb.algorithms;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import experimentalcode.frankenb.model.pairer.CrossPartitionPairer;
import experimentalcode.frankenb.model.partitioner.VectorApproximationPartitioner;
import experimentalcode.frankenb.model.projection.RandomProjection;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class RandomProjectionVectorApproximationCrossPairingDividerAlgorithm extends AbstractDividerAlgorithm {

  public RandomProjectionVectorApproximationCrossPairingDividerAlgorithm(Parameterization config) {
    this.addProjection(new RandomProjection(config));
    this.setPartitioning(new VectorApproximationPartitioner(config));
    this.setPairing(new CrossPartitionPairer(config));
  }
  
}
