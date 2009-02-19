package experimentalcode.thomas.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;;

/**
 * Provides the Longest Common Subsequence distance for NumberVectors.
 * 
 *
 * Adapted for Java, based on Matlab Code by Michalis Vlachos. Original Copyright Notice:
 * 
 *  BEGIN COPYRIGHT NOTICE
 *
 *    lcsMatching code -- (c) 2002 Michalis Vlachos (http://www.cs.ucr.edu/~mvlachos)
 *
 *    This code is provided as is, with no guarantees except that 
 *    bugs are almost surely present.  Published reports of research 
 *    using this code (or a modified version) should cite the 
 *    article that describes the algorithm: 
 *
 *      M. Vlachos, M. Hadjieleftheriou, D. Gunopulos, E. Keogh:  
 *      "Indexing Multi-Dimensional Time-Series with Support for Multiple Distance Measures",
 *      In Proc. of 9th SIGKDD, Washington, DC, 2003
 *      
 *    Comments and bug reports are welcome.  Email to mvlachos@cs.ucr.edu 
 *    I would also appreciate hearing about how you used this code, 
 *    improvements that you have made to it.
 *
 *    You are free to modify, extend or distribute this code, as long 
 *    as this copyright notice is included whole and unchanged.  
 *
 *    END COPYRIGHT NOTICE
 *
 *
 * @author Thomas Bernecker
 * @param <V> the type of NumberVector to compute the distances in between
 */
public class LCSSDistanceFunction<V extends NumberVector<V, ?>>
    extends AbstractEditDistanceFunction<V> {

	/**
     * OptionID for {@link #PDELTA_PARAM}
     */
    public static final OptionID PDELTA_ID = OptionID.getOrCreateOptionID("lcss.pDelta",
        "the allowed deviation in x direction for LCSS alignment (positive number)");
    
    /**
     * OptionID for {@link #PEPSILON_PARAM}
     */
    public static final OptionID PEPSILON_ID = OptionID.getOrCreateOptionID("lcss.pEpsilon",
        "the allowed deviation in y directionfor LCSS alignment (positive number)");

    /**
     * PDELTA parameter
     */
    private final IntParameter PDELTA_PARAM = new IntParameter(PDELTA_ID, new GreaterEqualConstraint(0));

    /**
     * PEPSILON parameter
     */
    private final DoubleParameter PEPSILON_PARAM = new DoubleParameter(PEPSILON_ID, new GreaterEqualConstraint(0));

    /**
     * Keeps the currently set pDelta.
     */
    private int pDelta;
    
    /**
     * Keeps the currently set pEpsilon.
     */
    private double pEpsilon;
	
	/**
     * Provides a Longest Common Subsequence distance function that can compute the Dynamic Time Warping
     * distance (that is a DoubleDistance) for NumberVectors.
     */
    public LCSSDistanceFunction() {
        super();
        addOption(PDELTA_PARAM);
        addOption(PEPSILON_PARAM);
    }

    /**
     * Provides the Longest Common Subsequence distance between the given two vectors.
     *
     * @return the Longest Common Subsequence distance between the given two vectors as an
     *         instance of {@link DoubleDistance DoubleDistance}.
     */
    public DoubleDistance distance(V v1, V v2) {
        
    	final int delta = pDelta;
		final double epsilon = pEpsilon;
		
		int m = -1;
		int n = -1;
		double[] a, b;
		
		
		//put shorter vector first
		if (v1.getDimensionality() < v2.getDimensionality())
		{
			m = v1.getDimensionality();
			n = v2.getDimensionality();
			a = new double[m];
			b = new double[n];
			
			for(int i=0; i < v1.getDimensionality(); i++)
				a[i] = v1.getValue(i + 1).doubleValue();
			for(int j=0; j < v2.getDimensionality(); j++)
				b[j] = v2.getValue(j + 1).doubleValue();
		}
		else
		{
			m = v2.getDimensionality();
			n = v1.getDimensionality();
			a = new double[m];
			b = new double[n];
			
			for(int i=0; i < v2.getDimensionality(); i++)
				a[i] = v2.getValue(i + 1).doubleValue();
			for(int j=0; j < v1.getDimensionality(); j++)
				b[j] = v1.getValue(j + 1).doubleValue();
		}
		
		double[][] matrix = new double[m+1][n+1];
		Step[][] steps = new Step[m+1][n+1];
		
		Step step;
			
		
		for (int i=0;i<m;i++) 
		{
			for (int j=(i-delta);j<=(i+delta);j++)
			{
				if (j<0||j>=n)
				{
					//do nothing;
				}
				else
				{					
					if ( (b[j]+epsilon)>=a[i] & (b[j]-epsilon)<=a[i]) // match
					{
						matrix[i+1][j+1] = matrix[i][j]+1;			
						step = Step.MATCH;
					}
					else if( matrix[i][j+1] > matrix[i+1][j]) // ins
					{					 
						matrix[i+1][j+1] = matrix[i][j+1];		
						step = Step.INS;
					}
					else // del
					{
						matrix[i+1][j+1] = matrix[i+1][j];	
						step = Step.DEL;
					}
					
					steps[i][j] = step;
				}
			}			
		}	
		
		// search for maximum in the last line
		double maxEntry=-1;
		for (int i=1;i<n+1;i++)
		{
			if (matrix[m][i]>maxEntry)
			{
				maxEntry=matrix[m][i];
			}
		}		
		double sim = maxEntry/Math.max(m,n); // FIXME: min instead of max????
		return new DoubleDistance(1 - sim);
    }

    @Override
    public String parameterDescription() {
        StringBuffer description = new StringBuffer();
        description.append(optionHandler.usage("Longest Common Subsequence distance for FeatureVectors.", false));
        description.append('\n');
        return description.toString();
    }

    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingOptions = super.setParameters(args);

        pDelta = PDELTA_PARAM.getValue();
        pEpsilon = PEPSILON_PARAM.getValue();

        return remainingOptions;
    }
}
