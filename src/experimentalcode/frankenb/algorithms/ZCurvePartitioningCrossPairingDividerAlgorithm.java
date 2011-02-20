/**
 * 
 */
package experimentalcode.frankenb.algorithms;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import experimentalcode.frankenb.algorithms.pairing.CrossPartitionPairing;
import experimentalcode.frankenb.algorithms.partitioning.ZCurvePartitioning;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public class ZCurvePartitioningCrossPairingDividerAlgorithm extends AbstractDividerAlgorithm {

  public ZCurvePartitioningCrossPairingDividerAlgorithm(Parameterization config) {
    this.setPartitioning(new ZCurvePartitioning(config));
    this.setPairing(new CrossPartitionPairing(config));
  }

}
