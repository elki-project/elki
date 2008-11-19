package de.lmu.ifi.dbs.elki.varianceanalysis.weightfunctions;

/**
 * Inverse Linear Weight Function.
 * 
 * This weight is not particularly reasonable. Instead it serves the purpose
 * of testing the effects of a badly chosen weight function.
 * 
 * This function has increasing weight, from 0.1 to 1.0 at distance == max.
 * 
 * @author Erich Schubert <schube@dbs.ifi.lmu.de>
 */
public final class InverseLinearWeight implements WeightFunction {
  /**
   * Linear increasing weight, from 0.1 to 1.0
   * 
   * NOTE: increasing weights are non-standard, and mostly for testing
   */
  public double getWeight(double distance, double max, @SuppressWarnings("unused") double stddev) {
    if (max <= 0) return 0.1;
    double relativedistance = distance / max;
    return .1 + relativedistance * .9;
  }
}