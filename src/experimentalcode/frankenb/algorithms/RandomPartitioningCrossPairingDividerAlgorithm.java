package experimentalcode.frankenb.algorithms;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import experimentalcode.frankenb.algorithms.pairing.CrossPartitionPairing;
import experimentalcode.frankenb.algorithms.partitioning.RandomPartitioning;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class RandomPartitioningCrossPairingDividerAlgorithm extends AbstractDividerAlgorithm {
  /**
   * Logger
   */
  public static Logging logger = Logging.getLogger(RandomPartitioningCrossPairingDividerAlgorithm.class);

  public RandomPartitioningCrossPairingDividerAlgorithm(Parameterization config) {
    this.setPartitioning(new RandomPartitioning(config));
    this.setPairing(new CrossPartitionPairing(config));
  }

  @Override
  Logging getLogger() {
    return logger;
  }
}
