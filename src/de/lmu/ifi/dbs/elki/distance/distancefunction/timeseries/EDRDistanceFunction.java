package de.lmu.ifi.dbs.elki.distance.distancefunction.timeseries;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Provides the Edit Distance on Real Sequence distance for FeatureVectors.
 * 
 * @author Thomas Bernecker
 */
@Title("Edit Distance on Real Sequence")
@Reference(authors = "L. Chen and M. T. Özsu and V. Oria", title = "Robust and fast similarity search for moving object trajectories", booktitle = "SIGMOD '05: Proceedings of the 2005 ACM SIGMOD international conference on Management of data", url = "http://dx.doi.org/10.1145/1066157.1066213")
public class EDRDistanceFunction extends AbstractEditDistanceFunction {
  /**
   * DELTA parameter
   */
  public static final OptionID DELTA_ID = OptionID.getOrCreateOptionID("edr.delta", "the delta parameter (similarity threshold) for EDR (positive number)");

  /**
   * Keeps the currently set delta.
   */
  private double delta;

  /**
   * Constructor.
   * 
   * @param bandSize Band size
   * @param delta Allowed delta
   */
  public EDRDistanceFunction(double bandSize, double delta) {
    super(bandSize);
    this.delta = delta;
  }

  /**
   * Provides the Edit Distance on Real Sequence distance between the given two
   * vectors.
   * 
   * @return the Edit Distance on Real Sequence distance between the given two
   *         vectors as an instance of {@link DoubleDistance DoubleDistance}.
   */
  @Override
  public DoubleDistance distance(NumberVector<?, ?> v1, NumberVector<?, ?> v2) {
    // Current and previous columns of the matrix
    double[] curr = new double[v2.getDimensionality()];
    double[] prev = new double[v2.getDimensionality()];

    // size of edit distance band
    // bandsize is the maximum allowed distance to the diagonal
    int band = (int) Math.ceil(v2.getDimensionality() * bandSize);

    // System.out.println("len1: " + features1.length + ", len2: " +
    // features2.length + ", band: " + band);
    final double deltaValue = delta;

    for(int i = 0; i < v1.getDimensionality(); i++) {
      // Swap current and prev arrays. We'll just overwrite the new curr.
      {
        double[] temp = prev;
        prev = curr;
        curr = temp;
      }
      int l = i - (band + 1);
      if(l < 0) {
        l = 0;
      }
      int r = i + (band + 1);
      if(r > (v2.getDimensionality() - 1)) {
        r = (v2.getDimensionality() - 1);
      }

      for(int j = l; j <= r; j++) {
        if(Math.abs(i - j) <= band) {
          // compute squared distance
          double val1 = v1.doubleValue(i + 1);
          double val2 = v2.doubleValue(j + 1);
          double diff = (val1 - val2);
          final double d = Math.sqrt(diff * diff);

          final double cost;

          final double subcost = (d <= deltaValue) ? 0 : 1;

          if((i + j) != 0) {
            if((i == 0) || ((j != 0) && (((prev[j - 1] + subcost) > (curr[j - 1] + 1)) && ((curr[j - 1] + 1) < (prev[j] + 1))))) {
              // del
              cost = curr[j - 1] + 1;
            }
            else if((j == 0) || ((i != 0) && (((prev[j - 1] + subcost) > (prev[j] + 1)) && ((prev[j] + 1) < (curr[j - 1] + 1))))) {
              // ins
              cost = prev[j] + 1;
            }
            else {
              // match
              cost = prev[j - 1] + subcost;
            }
          }
          else {
            cost = 0;
          }

          curr[j] = cost;
        }
        else {
          curr[j] = Double.POSITIVE_INFINITY; // outside band
        }
      }
    }

    return new DoubleDistance(curr[v2.getDimensionality() - 1]);
  }

  /**
   * Get parameter delta.
   * 
   * @param config Parameterization
   * @return value
   */
  public static double getParameterDelta(Parameterization config) {
    return 0.0;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractEditDistanceFunction.Parameterizer {
    protected double delta = 0.0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final DoubleParameter deltaP = new DoubleParameter(DELTA_ID, new GreaterEqualConstraint(0), 1.0);
      if(config.grab(deltaP)) {
        delta = deltaP.getValue();
      }
    }

    @Override
    protected EDRDistanceFunction makeInstance() {
      return new EDRDistanceFunction(bandSize, delta);
    }
  }
}