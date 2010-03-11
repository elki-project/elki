package experimentalcode.hettab.outlier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.MinMax;
import de.lmu.ifi.dbs.elki.result.AnnotationFromHashMap;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.result.OrderingFromHashMap;
import de.lmu.ifi.dbs.elki.result.OrderingResult;
import de.lmu.ifi.dbs.elki.result.outlier.InvertedOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.IntIntPair;
import experimentalcode.hettab.AxisPoint;


/**
 * BruteForce provides a naive brute force algorithm in which all k-subsets of dimensions are exammined
 * and calculates the sparsity coefficient to find outliers
 * @author Ahmed Hettab
 *<p>Reference:
 * <br>Outlier detection for high dimensional data
 * Outlier detection for high dimensional data
 * <br>International Conference on Management of Data
 * Proceedings of the 2001 ACM SIGMOD international conference on Management of data
 * 2001 , Santa Barbara, California, United States 
 * </p>
 * @param <V>
 */
// FIXME: Add Reference, Title, Description
public class BruteForce<V extends DoubleVector> extends
AbstractAlgorithm<V, MultiResult> {
	
	
	/**
	 * OptionID for {@link #PHI_PARAM}
	 */
	public static final OptionID PHI_ID = OptionID.getOrCreateOptionID(
			"bf.phi", "the dimensoinality of projection");
	/**
	 * Parameter to specify the equi-depth ranges must be an integer greater
	 * than 1.
	 * <p>
	 * Key: {@code -eafod.k}
	 * </p>
	 */
	private final IntParameter PHI_PARAM = new IntParameter(PHI_ID,
			new GreaterConstraint(1));
	/**
	 * Holds the value of {@link #PHI_PARAM}.
	 */
	private int phi;
	/**
	 * OptionID for {@link #K_PARAM}
	 */
	public static final OptionID K_ID = OptionID.getOrCreateOptionID("bf.k",
			"the dimensoinality of projection");

	/**
	 * Parameter to specify the dimensionality of projection must be an integer
	 * greater than 1.
	 * <p>
	 * Key: {@code -eafod.k}
	 * </p>
	 */
	private final IntParameter K_PARAM = new IntParameter(K_ID,
			new GreaterConstraint(1));

	/**
	 * Holds the value of {@link #K_PARAM}.
	 */
	private int k;

	/**
	 * Holds the value of database dimensionality
	 */
	private int dim;

	/**
	 * Holds the value of database size
	 */
	private int size;

	/**
	  * Provides the result of the algorithm.
	  */
	 MultiResult result;

	/**
	 * Holds the value of equi-depth
	 */
	private HashMap<Integer, HashMap<Integer, HashSet<Integer>>> ranges;
	
	/**
	  * The association id to associate the BF_SCORE of an object for the BruteForce
	  * algorithm.
	  */
	  public static final AssociationID<Double> BF_SCORE = AssociationID.getOrCreateAssociationID("bf", Double.class);
	
	
	/**
     * Provides the BruteForce algorithm,
     * adding parameters
     * {@link #K_PARAM}
     * {@link #PHI_PARAM}
     * to the option handler additionally to parameters of super class.
     */
	public BruteForce(Parameterization config) {
	  super(config);
		if (config.grab(K_PARAM)) {
		  k = K_PARAM.getValue();
		}
		if (config.grab(PHI_PARAM)) {
		  phi = PHI_PARAM.getValue();
		}
		ranges = new HashMap<Integer, HashMap<Integer, HashSet<Integer>>>();
	
	}
	


	@Override
	protected MultiResult runInTime(Database<V> database)
			throws IllegalStateException {
		//
		dim = database.dimensionality();
		size = database.size();
		ranges = new HashMap<Integer, HashMap<Integer, HashSet<Integer>>>();
		this.calculteDepth(database) ;	
		
		for(int i = 1 ; i<= dim ; i++){
			for(int j = 1 ; j<=phi; j++){
				    ArrayList<Integer> list = new ArrayList<Integer>(ranges.get(i).get(j));
				    MinMax<Double> minmax = new MinMax<Double>();
				    for(int t = 0 ; t<list.size();t++){
					minmax.put(database.get(list.get(t)).getValue(i));
				    }
				    logger.verbose("Dim : "+i+" depth : "+j);
				    logger.verbose("Min :"+minmax.getMin());
				    logger.verbose("Max :"+minmax.getMax());
			}
		}
		
		HashMap<Integer , ArrayList<Vector<IntIntPair>>> subspaces = new HashMap<Integer , ArrayList<Vector<IntIntPair>>>();
	
		
		//Set of all dim*phi ranges
		ArrayList<Vector<IntIntPair>> q = new ArrayList<Vector<IntIntPair>>();
	
		for(int i = 1 ; i<=database.dimensionality() ; i++){
			
			for(int j = 1 ;j<=phi ; j++){
				Vector<IntIntPair> v = new Vector<IntIntPair>();
				
				v.add(new IntIntPair(i, j));
			    q.add(v)	;
			}
		}
		subspaces.put(1, q);
		
		//claculate Ri
		for(int i = 2 ; i<= k ; i++){
			ArrayList<Vector<IntIntPair>> Ri = new ArrayList<Vector<IntIntPair>>();
			ArrayList<Vector<IntIntPair>> oldR = subspaces.get(i-1);
			
			for(int j = 0 ; j < oldR.size() ; j++){
				Vector<IntIntPair> c = oldR.get(j);
				for(int l = 0 ; l< q.size();l++){
					int count = 0 ;
					Vector<IntIntPair> neu = new Vector<IntIntPair>(c);
					IntIntPair pair =  q.get(l).get(0);
					for(int t = 0 ; t<neu.size();t++){
						if(neu.get(t).first == pair.first) count ++ ;
					 }
					if(count == 0){
						neu.add(pair);
					Ri.add(neu);
					}
				}
			}
			subspaces.put(i, Ri);
		}
			
		HashMap<Integer, Double> sparsity = new HashMap<Integer, Double>();
		MinMax<Double> minmax = new MinMax<Double>();
		//set Of all k-subspaces
		ArrayList<Vector<IntIntPair>> s = subspaces.get(k);
		
		//calculate the sparsity coefficient
		for(Vector<IntIntPair> sub : s){	
			double sparsityC = fitness(sub);
			HashSet<Integer> ids = getIDs(sub);
			logger.verbose(printSubspace(sub));
			logger.verbose(ids.toString());
			for(Integer id : ids){
				sparsity.put( id, sparsityC );
			}
			minmax.put(sparsityC);
		}	
		
		    AnnotationResult<Double> scoreResult = new AnnotationFromHashMap<Double>(BF_SCORE, sparsity);
		    OrderingResult orderingResult = new OrderingFromHashMap<Double>(sparsity, false);
		    OutlierScoreMeta scoreMeta = new InvertedOutlierScoreMeta(minmax.getMin(), minmax.getMax(),minmax.getMin(),Double.POSITIVE_INFINITY,minmax.getMin()/2);
		    this.result = new OutlierResult(scoreMeta, scoreResult, orderingResult);
		    return result;
		 
		
	}
	
	/**
	 * grid discretization of the data :
	 * <br>each attribute of data is divided into phi equi-depth ranges .
	 * <br>each range contains a fraction f=1/phi of the records .
	 * @param database
	 */
	public void calculteDepth(Database<V> database) {
		// sort dimension
		ArrayList<ArrayList<AxisPoint>> dbAxis = new ArrayList<ArrayList<AxisPoint>>(
				dim);

		HashSet<Integer> range = new HashSet<Integer>();
		HashMap<Integer, HashSet<Integer>> rangesAt = new HashMap<Integer, HashSet<Integer>>();

		for (int i = 0; i < dim; i++) {
			ArrayList<AxisPoint> axis = new ArrayList<AxisPoint>(size);
			dbAxis.add(i, axis);
		}
		for (Integer id : database) {
			for (int dim = 1; dim <= database.dimensionality(); dim++) {
				double value = database.get(id).getValue(dim);
				AxisPoint point = new AxisPoint(id, value);
				dbAxis.get(dim-1).add(point);
			}
		}
		//
		for (int index = 0; index < database.dimensionality(); index++) {
			Collections.sort(dbAxis.get(index));
		}

		// equi-depth
		// if range = 0 => |range| = database.size();
		// if database.size()%phi == 0 |range|=database.size()/phi
		// if database.size()%phi == rest (1..rest => |range| =
		// database.size()/phi +1 , rest..phi => |range| = database.size()/phi 
		int rest = database.size() % phi;
		int f = database.size() / phi;
		
		HashSet<Integer> b = new HashSet<Integer>();
		for (Integer id : database) {
			b.add(id);
		}
		// if range = 0 => |range| = database.size();
		for (int dim = 1; dim <= database.dimensionality(); dim++) {
			rangesAt = new HashMap<Integer, HashSet<Integer>>();
			ranges.put(dim, rangesAt);
		}
	
		for (int dim = 1; dim <= database.dimensionality(); dim++) {
			ArrayList<AxisPoint> axis = dbAxis.get(dim - 1);

			for (int i = 0; i < rest; i++) {
				// 1..rest => |range| = database.size()/phi +1
				range = new HashSet<Integer>();
				for (int j = i * f + i; j < (i + 1) * f + i + 1; j++) {
					range.add(axis.get(j).getId());
				}
				ranges.get(dim).put(i + 1, range);
			}

			// rest..phi => |range| = database.size()/phi
			for (int i = rest; i < phi; i++) {
				range = new HashSet<Integer>();
				for (int j = i * f + rest; j < (i + 1) * f + rest; j++) {
					range.add(axis.get(j).getId());
				}
				ranges.get(dim).put(i + 1, range);
			}

		}

	}
    
	/**
	 * methode calculte the sparsity coefficient of
	 * @param subspace
	 * @return
	 * sparsity coefficient
	 */
	public double fitness(Vector<IntIntPair> subspace) {

		HashSet<Integer> ids = new HashSet<Integer>(ranges.get(subspace.get(0).getFirst()).get(subspace.get(0).getSecond()));
		
		//intersect
		for (int i = 1 ; i < subspace.size(); i++) {
			HashSet<Integer> current = ranges.get(subspace.get(i).getFirst()).get(subspace.get(i).getSecond());
			HashSet<Integer> result = EAFOD.retainAll(current,ids);
			ids.clear();
			ids.addAll(result);
		}
		//calculate sparsity c
		double f = (double) 1 / phi;
		double nD = (double) ids.size();
		double fK = Math.pow(f, k);
		double sC = (nD - (size * fK)) / Math.sqrt(size * fK * (1 - fK));
		return sC;
	}
	
	/**
	 * methode calculate the ids of
	 * @param subspace
	 * @return
	 * ids
	 */
	public HashSet<Integer> getIDs(Vector<IntIntPair> subspace) {

		HashSet<Integer> ids = new HashSet<Integer>(ranges.get(subspace.get(0).getFirst()).get(subspace.get(0).getSecond()));
		//intersect
		for (int i = 1; i < subspace.size(); i++) {
			HashSet<Integer> current = ranges.get(subspace.get(i).getFirst()).get(subspace.get(i).getSecond());
			HashSet<Integer> result = EAFOD.retainAll(current,ids);
			ids.clear();
			ids.addAll(result);
		}
		return ids ;
	}

  /**
   * get the algorithm result
   */
	public MultiResult getResult() {
		return result;
	}
	
	/**
	 * return a presentation string of
	 * @param subspace
	 * @return
	 * presentation string
	 */
	public String printSubspace(Vector<IntIntPair> subspace){
		String s ="Subspace :";
		for(IntIntPair pair : subspace){
			s = s+" Dim:"+pair.first+" phi:"+pair.second+" ";
		}
		return s ;
	}
}