package experimentalcode.shared.outlier.scaling;

/**
 * Simple linear scaling function.
 * 
 * @author Erich Schubert
 */
public class LinearScaling implements StaticScalingFunction {
  /**
   * Scaling factor
   */
	private double factor;
	
	/**
	 * Shift
	 */
	private double shift;
	
	/**
	 * Constructor with defaults resulting in identity.
	 */
	public LinearScaling(){
		this(1.0, 0.0);
	}
	
  /**
   * Constructor with scaling only.
   * 
   * @param factor Scaling factor
   */
  public LinearScaling(double factor){
    this(factor, 0.0);
  }

	/**
	 * Full constructor.
	 * 
	 * @param factor Scaling factor
	 * @param shift Shift value
	 */
	public LinearScaling(double factor, double shift){
		this.factor = factor;
		this.shift = shift;
	}

	@Override
	public double getScaled(double d) {
		return factor*d + shift;
	}
}
