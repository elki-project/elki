package experimentalcode.frankenb.algorithms;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import experimentalcode.frankenb.algorithms.pairing.NearFieldPartitionPairing;
import experimentalcode.frankenb.algorithms.partitioning.GridPartitioning;
import experimentalcode.frankenb.algorithms.projection.RandomProjection;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class RandomProjectionGridPartitioningNearFieldPairingDividerAlgorithm extends AbstractDividerAlgorithm {
  /**
   * Logger
   */
  public static Logging logger = Logging.getLogger(RandomProjectionGridPartitioningNearFieldPairingDividerAlgorithm.class);

  public RandomProjectionGridPartitioningNearFieldPairingDividerAlgorithm(Parameterization config) {
    this.addProjection(new RandomProjection(config));
    this.setPartitioning(new GridPartitioning(config));
    this.setPairing(new NearFieldPartitionPairing(config));
  }

  @Override
  Logging getLogger() {
    return logger;
  }
}
