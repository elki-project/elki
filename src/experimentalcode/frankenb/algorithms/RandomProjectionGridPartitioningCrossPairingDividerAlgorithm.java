package experimentalcode.frankenb.algorithms;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import experimentalcode.frankenb.algorithms.pairing.CrossPartitionPairing;
import experimentalcode.frankenb.algorithms.partitioning.GridPartitioning;
import experimentalcode.frankenb.algorithms.projection.RandomProjection;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class RandomProjectionGridPartitioningCrossPairingDividerAlgorithm extends AbstractDividerAlgorithm {
  /**
   * Logger
   */
  public static Logging logger = Logging.getLogger(RandomProjectionGridPartitioningCrossPairingDividerAlgorithm.class);

  public RandomProjectionGridPartitioningCrossPairingDividerAlgorithm(Parameterization config) {
    this.addProjection(new RandomProjection(config));
    this.setPartitioning(new GridPartitioning(config));
    this.setPairing(new CrossPartitionPairing(config));
  }

  @Override
  Logging getLogger() {
    return logger;
  }
}
