package experimentalcode.remigius.Scales;


public class GammaFunction implements DoubleScale {

	private double gamma;
	
	public GammaFunction(){
		this(1.0);
	}
	
	public GammaFunction(double gamma){
		this.gamma = gamma;
	}
	
	public double getGamma(){
		return this.gamma;
	}
	
	public void setGamma(double gamma){
		this.gamma = gamma;
	}
	
	@Override
	public Double getScaled(Double d) {
		return Math.pow(d, gamma);
	}
}
