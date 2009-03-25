package de.lmu.ifi.dbs.elki.distance.distancefunction.timeseries;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;

/**
 * Provides the Edit Distance With Real Penalty distance for NumberVectors.
 *
 * @author Thomas Bernecker
 * @param <V> the type of NumberVector to compute the distances in between
 */
public class ERPDistanceFunction<V extends NumberVector<V, ?>>
    extends AbstractEditDistanceFunction<V> {

	/**
     * OptionID for {@link #G_PARAM}
     */
    public static final OptionID G_ID = OptionID.getOrCreateOptionID("erp.g",
        "the g parameter ERP (positive number)");

    /**
     * G parameter
     */
    private final DoubleParameter G_PARAM = new DoubleParameter(G_ID, new GreaterEqualConstraint(0), 0.0);
    
    /**
     * Keeps the currently set g.
     */
    private double g;
	
	/**
     * Provides a Edit Distance With Real Penalty distance function that can compute the Dynamic Time Warping
     * distance (that is a DoubleDistance) for NumberVectors.
     */
    public ERPDistanceFunction() {
        super();
        addOption(G_PARAM);
    }

    /**
     * Provides the Edit Distance With Real Penalty distance between the given two vectors.
     *
     * @return the Edit Distance With Real Penalty distance between the given two vectors as an
     *         instance of {@link DoubleDistance DoubleDistance}.
     */
    public DoubleDistance distance(V v1, V v2) {
        
    	double[][] matrix = new double[v1.getDimensionality()][v2.getDimensionality()];
		Step[][] steps = new Step[v1.getDimensionality()][v2.getDimensionality()];
        
		// size of edit distance band
		int band = (int)Math.ceil(v2.getDimensionality() * bandSize);	// bandsize is the maximum allowed distance to the diagonal
		
		// g parameter for local usage
		double gValue = g;
		
		for (int i = 0; i < v1.getDimensionality(); i++)
		{
			int l = i - (band + 1);
			if (l < 0) l = 0;
			int r = i + (band + 1);
			if (r > (v2.getDimensionality() - 1)) r = (v2.getDimensionality() - 1);
			
			for (int j = l; j <= r; j++)
			{
				if (Math.abs(i - j) <= band)
				{
					// compute squared distance of feature vectors
					double val1 = v1.getValue(i + 1).doubleValue();
					double val2 = gValue;
					double diff = (val1 - val2);
					final double d1 =  Math.sqrt(diff * diff);
					
					val1 = gValue;
					val2 = v2.getValue(j + 1).doubleValue();
					diff = (val1 - val2);
					final double d2 =  Math.sqrt(diff * diff);
					
					val1 = v1.getValue(i + 1).doubleValue();
					val2 = v2.getValue(j + 1).doubleValue();
					diff = (val1 - val2);
					final double d12 =  Math.sqrt(diff * diff);
					
					final double dist1 = d1 * d1;
					final double dist2 = d2 * d2;
					final double dist12 = d12 * d12;

					final double cost;
					final Step step;

					if ((i+j) != 0)
					{
						if ((i == 0) || ((j != 0) && (((matrix[i - 1][j - 1] + dist12) > (matrix[i][j - 1] + dist2)) && ((matrix[i][j - 1] + dist2) < (matrix[i - 1][j] + dist1)))))
						{
							// del
							cost = matrix[i][j - 1] + dist2;
							step = Step.DEL;
						}
						else if ((j == 0) || ((i != 0) && (((matrix[i - 1][j - 1] + dist12) > (matrix[i - 1][j] + dist1)) && ((matrix[i - 1][j] + dist1) < (matrix[i][j - 1] + dist2)))))
						{
							// ins
							cost = matrix[i - 1][j] + dist1;
							step = Step.INS;
						}
						else
						{
							// match
							cost = matrix[i - 1][j - 1] + dist12;
							step = Step.MATCH;
						}
					}
					else
					{
						cost = 0;
						step = Step.MATCH;
					}
					
					matrix[i][j] = cost;
					steps[i][j] = step;
				}
				else
				{
					matrix[i][j] = Double.POSITIVE_INFINITY;	// outside band
				}
			}
		}
		
		return new DoubleDistance(Math.sqrt(matrix[v1.getDimensionality() - 1][v2.getDimensionality() - 1]));
    }

    @Override
    public String parameterDescription() {
        StringBuffer description = new StringBuffer();
        description.append(optionHandler.usage("Edit Distance With Real Penalty distance for FeatureVectors.", false));
        description.append('\n');
        return description.toString();
    }

    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingOptions = super.setParameters(args);

        g = G_PARAM.getValue();

        return remainingOptions;
    }
}
