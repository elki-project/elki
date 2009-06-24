package experimentalcode.lisa.scale;


public class LinearScale implements DoubleScale {
	
	private double factor;
	
	public LinearScale(){
		this(0.1);
	}
	
	public LinearScale(double factor){
		this.factor = factor;
	}

	@Override
	public Double getScaled(Double d) {
		return factor*d;
	}

}
