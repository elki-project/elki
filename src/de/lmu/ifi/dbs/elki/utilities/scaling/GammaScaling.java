package de.lmu.ifi.dbs.elki.utilities.scaling;

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
   * OptionID for {@link #GAMMA_PARAM}.
   */
  public static final OptionID GAMMA_ID = OptionID.getOrCreateOptionID("scaling.gamma", "Gamma value for scaling.");

  /**
   * Gamma value.
   */
	private double gamma;
	
	/**
	 * Default constructor.
	 */
	public GammaScaling(Parameterization config){
		this(getGammaParameter(config));
	}

	/**
	 * Parameterization method.
	 * 
	 * @param config Configuration
	 * @return Gamma value
	 */
	private static double getGammaParameter(Parameterization config) {
	  DoubleParameter param = new DoubleParameter(GAMMA_ID);
	  if (config.grab(param)) {
	    return param.getValue();
	  }
    return 1.0;
  }

  /**
	 * Constructor with Gamma value.
	 * 
	 * @param gamma Gamma value.
	 */
	public GammaScaling(double gamma){
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
}
