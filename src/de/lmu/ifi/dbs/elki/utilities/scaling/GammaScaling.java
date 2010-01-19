package de.lmu.ifi.dbs.elki.utilities.scaling;

/**
 * Non-linear scaling function using a Gamma curve.
 * 
 * @author Erich Schubert
 * @author Remigius Wojdanowski
 */
public class GammaScaling implements StaticScalingFunction {
  /**
   * Gamma value.
   */
	private double gamma;
	
	/**
	 * Default constructor.
	 */
	public GammaScaling(){
		this(1.0);
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
