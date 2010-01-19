package experimentalcode.hettab.outlier;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.Vector;
import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import experimentalcode.hettab.AxisPoint;
import experimentalcode.hettab.MySubspace;


public class EAFOD<V extends DoubleVector> extends
		AbstractAlgorithm<V, MultiResult> {

	/**
	 * OptionID for {@link #M_PARAM}
	 */
	public static final OptionID M_ID = OptionID.getOrCreateOptionID("eafod.m",
			"number of projection");
	/**
	 * Parameter to specify the number of projection must be an integer greater
	 * than 1.
	 * <p>
	 * <p>
	 * Key: {@code -eafod.m}
	 * </p>
	 */
	private final IntParameter M_PARAM = new IntParameter(M_ID,
			new GreaterConstraint(2));
	/**
	 * Holds the value of {@link #M_PARAM}.
	 */
	private int m;
	/**
	 * OptionID for {@link #PHI_PARAM}
	 */
	public static final OptionID PHI_ID = OptionID.getOrCreateOptionID(
			"eafod.phi", "the dimensoinality of projection");
	/**
	 * Parameter to specify the equi-depth ranges must be an integer greater
	 * than 1.
	 * <p>
	 * Key: {@code -eafod.k}
	 * </p>
	 */
	private final IntParameter PHI_PARAM = new IntParameter(PHI_ID,
			new GreaterConstraint(0));
	/**
	 * Holds the value of {@link #PHI_PARAM}.
	 */
	private int phi;
	/**
	 * OptionID for {@link #K_PARAM}
	 */
	public static final OptionID K_ID = OptionID.getOrCreateOptionID("eafod.k",
			"the dimensoinality of projection");

	/**
	 * Parameter to specify the dimensionality of projection must be an integer
	 * greater than 1.
	 * <p>
	 * Key: {@code -eafod.k}
	 * </p>
	 */
	private final IntParameter K_PARAM = new IntParameter(K_ID,
			new GreaterConstraint(0));

	/**
	 * Holds the value of {@link #K_PARAM}.
	 */
	private int k;
	
	/**
	 * 
	 */
	private HashMap<Integer, HashMap<Integer, HashSet<Integer>>> ranges;
	
	/**
	 * 
	 */
	public EAFOD() {
		addOption(K_PARAM);
		addOption(M_PARAM);
		addOption(PHI_PARAM);
	}

	@Override
	protected MultiResult runInTime(Database<V> database)
			throws IllegalStateException {
		ranges = new HashMap<Integer, HashMap<Integer,HashSet<Integer>>>();
		this.calculteDepth(database);
		Vector<Integer> individium  = new Vector<Integer>();
		for(int i = 0 ; i<database.dimensionality() ; i++){
			individium.add(i);
		}
		System.out.println(fitness(database, individium));
		return null ;
	}
	
	
    /**
	 * 
	 * @param database
	 */
	public void calculteDepth(Database<V> database){
			ArrayList<ArrayList<AxisPoint>> dbAxis = new ArrayList<ArrayList<AxisPoint>>(database.dimensionality());
			HashSet<Integer> range = new HashSet<Integer>();;
			HashMap<Integer, HashSet<Integer>> rangesAt = new HashMap<Integer, HashSet<Integer>>(); ;
			for (int i = 0; i < database.dimensionality(); i++) {
				ArrayList<AxisPoint> axis = new ArrayList<AxisPoint>(database
						.size());
				dbAxis.add(i, axis);
			}
			for (Integer id : database) {
				for (int index = 0; index < database.dimensionality(); index++) {
					double value = database.get(id).getValue(index + 1);
					AxisPoint point = new AxisPoint(id, value);
					dbAxis.get(index).add(point);
				}
			}
			for (int index = 0; index < database.dimensionality(); index++) {
				Collections.sort(dbAxis.get(index));
			}
			
			int rest = database.size()%phi ;
			int f = database.size()/phi ;
			 for(int dim = 0 ; dim<database.dimensionality() ; dim ++){
		          for(int i = 0 ; i<rest ; i++){
			           range = new HashSet<Integer>();
			               for(int j = i*f + i ; j<(i+1)*f +i+1 ;j++){
			    	           range.add(dbAxis.get(dim).get(j).getId()) ;
			               }
			               rangesAt.put(i+1,range);
		                                   }
		          for(int i = rest ; i<phi ; i++){
			           range = new HashSet<Integer>();
			               for(int j = i*f + rest ; j<(i+1)*f+rest;j++){
			    	           range.add(dbAxis.get(dim).get(j).getId()) ;
			  	               }
			               rangesAt.put(i+1,range);
		                                   }
		   
		          
		          ranges.put(dim+1,rangesAt);
		    }
			
		}
	
	/**
	 * 
	 */
	public TreeSet<MySubspace> initialPopulation(Database<V> database , int popsize)
	{	
		int dDim = database.dimensionality();
		TreeSet<MySubspace> population= new TreeSet<MySubspace>();
		//fill population
		for (int i=0; i<popsize;i++)
		{
			Vector<Integer> individium = new Vector<Integer>();
			int[] tmp= new int[dDim] ;
			int tmp2=k;
			// fill "non don´t care" positions of the String
			while(tmp2>0)
			{
				int z= (int)(Math.random()*dDim);
				if(tmp[z]==0)
				{
					tmp[z]=(int)(Math.random()*phi+1);
					tmp2--;
				}
			}	
			//fill rest with don´t cares	
			for (int j=0;j<dDim;j++)
			{
				if (tmp[j]==0)tmp[j]=0;
			}
			for(int j = 0 ; j<tmp.length ; j++){
				individium.add(tmp[j]);
			}
			MySubspace subspace = new MySubspace(individium,fitness(database, individium));
			population.add(subspace);
			}	
			return population;	
	}

	/**
	 * 
	 * @param individium
	 * @return
	 */
	public double fitness(Database<V> database , Vector<Integer> individium){
		Vector<Integer> noNull = new Vector<Integer>();
		Vector<Integer> dimNoNull = new Vector<Integer>();
		int dim = 1 ;
		for(Integer depth : individium){
			if(depth!=0){
				dimNoNull.add(dim);
				noNull.add(depth);
				dim++ ;
			}
		}
		HashSet<Integer> ids = ranges.get(dimNoNull.get(0)).get(noNull.get(0));
		for(int i = 0 ;i<noNull.size();i++){
				ids.retainAll(ranges.get(noNull.get(i)).get(dimNoNull.get(i)));
			
		}
		double nD = ids.size() ;
		System.out.println(nD+" :"+"N(D)");
		double fK = Math.pow(phi, k) ;
		System.out.println(fK+" :"+"fK");
		double n = database.size();
		return (nD-n*fK/Math.sqrt(n*fK*(1-fK)));
		
	}		
	/**
	 * 
	 */    
	@Override
	public Description getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MultiResult getResult() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Calls the super method and additionally sets the values of the parameters
	 * {@link #K_PARAM} and {@link #M_PARAM} and {@link #PHI_PARAM}
	 */
	@Override
	public List<String> setParameters(List<String> args)
			throws ParameterException {
		List<String> remainingParameters = super.setParameters(args);
		k = K_PARAM.getValue();
		m = M_PARAM.getValue();
		phi = PHI_PARAM.getValue();
		return remainingParameters;
	}

}
