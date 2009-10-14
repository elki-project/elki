package experimentalcode.lisa.scale;

import experimentalcode.shared.outlier.scaling.StaticScalingFunction;

public class LinearScale implements StaticScalingFunction {
	private double factor;
	
	public LinearScale(){
		this(1.0);
	}
	
	public LinearScale(double factor){
		this.factor = factor;
	}

	@Override
	public double getScaled(double d) {
		return factor*d;
	}
}
