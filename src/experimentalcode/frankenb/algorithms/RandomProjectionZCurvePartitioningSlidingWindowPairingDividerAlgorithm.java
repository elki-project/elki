package experimentalcode.frankenb.algorithms;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import experimentalcode.frankenb.algorithms.pairing.SlidingWindowPartitionPairing;
import experimentalcode.frankenb.algorithms.partitioning.ZCurvePartitioning;
import experimentalcode.frankenb.algorithms.projection.RandomProjection;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class RandomProjectionZCurvePartitioningSlidingWindowPairingDividerAlgorithm extends AbstractDividerAlgorithm {
  /**
   * Logger
   */
  public static Logging logger = Logging.getLogger(RandomProjectionZCurvePartitioningSlidingWindowPairingDividerAlgorithm.class);

  public RandomProjectionZCurvePartitioningSlidingWindowPairingDividerAlgorithm(Parameterization config) {
    this.addProjection(new RandomProjection(config));
    this.setPartitioning(new ZCurvePartitioning(config));
    this.setPairing(new SlidingWindowPartitionPairing(config));
  }

  @Override
  Logging getLogger() {
    return logger;
  }
}
