package experimentalcode.lisa.scale;

import experimentalcode.shared.outlier.scaling.StaticScalingFunction;

public class GammaFunction implements StaticScalingFunction {

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
	public double getScaled(double d) {
		return Math.pow(d, gamma);
	}
}
