/**
 * 
 */
package experimentalcode.frankenb.algorithms;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import experimentalcode.frankenb.algorithms.pairing.SlidingWindowPartitionPairing;
import experimentalcode.frankenb.algorithms.partitioning.ZCurvePartitioning;
import experimentalcode.frankenb.algorithms.projection.RandomProjection;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class RandomProjectionZCurvePartitionerSlidingWindowPairingDividerAlgorithm extends AbstractDividerAlgorithm {

  public RandomProjectionZCurvePartitionerSlidingWindowPairingDividerAlgorithm(Parameterization config) {
    this.addProjection(new RandomProjection(config));
    this.setPartitioning(new ZCurvePartitioning(config));
    this.setPairing(new SlidingWindowPartitionPairing(config));
  }
  
}
