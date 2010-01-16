package experimentalcode.hettab.outlier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

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

public class EAFOD <V extends DoubleVector> extends AbstractAlgorithm<V, MultiResult>{
	
	/**
	 * OptionID for {@link #M_PARAM}
	 */
	public static final OptionID M_ID = OptionID.getOrCreateOptionID("eafod.m",
			"number of projection");
	/**
	 * Parameter to specify the number of projection
	 * must be an integer greater than 1.
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
	public static final OptionID PHI_ID = OptionID.getOrCreateOptionID("eafod.phi",
			"the dimensoinality of projection");
	/**
	 * Parameter to specify the equi-depth ranges
	 *  must be an integer greater than 1.
	 * <p>
	 * Key: {@code -eafod.k}
	 * </p>
	 */
	private final IntParameter PHI_PARAM = new IntParameter(PHI_ID,
			new GreaterConstraint(2));
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
	 * Parameter to specify the dimensionality of projection
	 *  must be an integer greater than 1.
	 * <p>
	 * Key: {@code -eafod.k}
	 * </p>
	 */
	private final IntParameter K_PARAM = new IntParameter(K_ID,
			new GreaterConstraint(2));

	/**
	 * Holds the value of {@link #K_PARAM}.
	 */
	private int k;
	
	/**
	 * 
	 */
	private HashMap<Integer, HashMap<Integer, Vector<Integer>>> ranges ;
	
	/**
	 * 
	 */
	public EAFOD(){
		addOption(K_PARAM);
		addOption(M_PARAM);
		addOption(PHI_PARAM);
	}

	@Override
	protected MultiResult runInTime(Database<V> database)
			throws IllegalStateException {
		//
		ranges = new HashMap<Integer, HashMap<Integer,Vector<Integer>>>();
		int n = database.size() ;
		//step 1 equi-depth
		//
		ArrayList<ArrayList<AxisPoint>> dbAxis = new ArrayList<ArrayList<AxisPoint>>(database.dimensionality());
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
		
		//
		ArrayList<AxisPoint> axis = dbAxis.get(0);
		System.out.println(axis.size());
		HashMap<Integer,Vector<Integer>> range = new HashMap<Integer, Vector<Integer>>();
		range.put(0,new Vector<Integer>());
		int f = n/phi ;
		int r = n%phi ;
		System.out.println(f);
		System.out.println(r);
		List<AxisPoint> rangeAt ;
		for(int i = 0 ; i<n%phi;i++){
			rangeAt =  axis.subList(i*f+2*i,f*(i+1)+(2*i+1));
				for(int j = 0 ; j<rangeAt.size() ; j++){
					range.get(0).add(rangeAt.get(0).getId());
				}
				System.out.println(rangeAt.size());	
		}
		for(int i = n%phi ; i<phi;i++){
			rangeAt =  axis.subList(f*i+2*i,f*(i+1)+2*i);
				for(int j = 0 ; j<rangeAt.size() ; j++){
					range.get(0).add(rangeAt.get(0).getId());
				}
				System.out.println(rangeAt.size());
				
		}
		
		
		
		return null;
	}

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
		m=  M_PARAM.getValue() ;
		phi = PHI_PARAM.getValue() ;
		return remainingParameters;
	}


}
