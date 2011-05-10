package experimentalcode.frankenb.algorithms;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import experimentalcode.frankenb.algorithms.pairing.CrossPartitionPairing;
import experimentalcode.frankenb.algorithms.partitioning.ZCurvePartitioning;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class ZCurvePartitioningCrossPairingDividerAlgorithm extends AbstractDividerAlgorithm {
  /**
   * Logger
   */
  public static Logging logger = Logging.getLogger(ZCurvePartitioningCrossPairingDividerAlgorithm.class);

  public ZCurvePartitioningCrossPairingDividerAlgorithm(Parameterization config) {
    this.setPartitioning(new ZCurvePartitioning(config));
    this.setPairing(new CrossPartitionPairing(config));
  }

  @Override
  Logging getLogger() {
    return logger;
  }
}
