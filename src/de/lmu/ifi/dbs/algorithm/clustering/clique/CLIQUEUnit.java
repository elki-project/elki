package de.lmu.ifi.dbs.algorithm.clustering.clique;

import de.lmu.ifi.dbs.utilities.HyperBoundingBox;

/**
 * Represents an unit in the CLIQUE algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class CLIQUEUnit extends HyperBoundingBox {

  /**
   * Creates a new unit for the given hyper points.
   *
   * @param min - the coordinates of the minimum hyper point
   * @param max - the coordinates of the maximum hyper point
   */
  public CLIQUEUnit(double[] min, double[] max) {
    super(min, max);
  }
}
