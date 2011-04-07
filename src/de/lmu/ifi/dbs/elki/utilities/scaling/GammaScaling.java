package de.lmu.ifi.dbs.elki.utilities.scaling;

import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Non-linear scaling function using a Gamma curve.
 * 
 * @author Erich Schubert
 * @author Remigius Wojdanowski
 */
public class GammaScaling implements StaticScalingFunction {
  /**
   * OptionID for the gamma value.
   */
  public static final OptionID GAMMA_ID = OptionID.getOrCreateOptionID("scaling.gamma", "Gamma value for scaling.");

  /**
   * Gamma value.
   */
  private double gamma;

  /**
   * Constructor without options.
   */
  public GammaScaling() {
    this(1.0);
  }

  /**
   * Constructor with Gamma value.
   * 
   * @param gamma Gamma value.
   */
  public GammaScaling(double gamma) {
    this.gamma = gamma;
  }

  @Override
  public double getScaled(double d) {
    return Math.pow(d, gamma);
  }

  @Override
  public double getMin() {
    return Double.NEGATIVE_INFINITY;
  }

  @Override
  public double getMax() {
    return Double.POSITIVE_INFINITY;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    double gamma = 1.0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter gammaP = new DoubleParameter(GAMMA_ID);
      if(config.grab(gammaP)) {
        gamma = gammaP.getValue();
      }
    }

    @Override
    protected GammaScaling makeInstance() {
      return new GammaScaling(gamma);
    }
  }
}