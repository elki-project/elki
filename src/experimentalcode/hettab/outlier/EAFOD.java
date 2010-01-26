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
		//double s=-Math.sqrt(database.size()/(Math.pow(phi,k)-1));
		//int k=(int)(Math.log(database.size()/(Math.pow(s, 2)+1))/Math.log(phi));
		//this.k = k ;

		    ranges = new HashMap<Integer, HashMap<Integer, HashSet<Integer>>>();
		    this.calculteDepth(database);
			ArrayList<MySubspace> popuArrayList = this.initialPopulation(database, 10);
			System.out.println(recombine(database, popuArrayList.get(0), popuArrayList.get(1)));
			System.out.println(checkConvergence(database, popuArrayList));
			
		
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
			 for(int dim = 1 ; dim<=database.dimensionality() ; dim ++){
		          for(int i = 0 ; i<rest ; i++){
			           range = new HashSet<Integer>();
			               for(int j = i*f + i ; j<(i+1)*f +i+1 ;j++){
			    	           range.add(dbAxis.get(dim-1).get(j).getId()) ;
			               }
			               rangesAt.put(i+1,range);
		                                   }
		          for(int i = rest ; i<phi ; i++){
			           range = new HashSet<Integer>();
			               for(int j = i*f + rest ; j<(i+1)*f+rest;j++){
			    	           range.add(dbAxis.get(dim-1).get(j).getId()) ;
			  	               }
			               rangesAt.put(i+1,range);
		                                   }
		   
		          
		          ranges.put(dim,rangesAt);
		    }
			
		}
	
	/**
	 * 
	 */
	public boolean checkConvergence(Database<V> database , ArrayList<MySubspace> population)
	{
		boolean convergence = true ;
		//for any individium
		for(int i = 0 ;i<population.size();i++){
			//for any dim
			for(int j = 0 ; j<population.get(0).getIndividium().size();j++){
				int count = 0  ;
				//for any ind-dim
				for(int k = 0 ; k<population.size(); k++){
					if(population.get(i).getIndividium().get(j)== population.get(k).getIndividium().get(j) || i!=k){
						count ++ ;
					}
				}
				if(count<0.95*database.size()){
					return false ;
				}
			}
		}
		return convergence;
	}
	/**
	 * 
	 */
	public ArrayList<MySubspace> initialPopulation(Database<V> database , int popsize)
	{	
		int dDim = database.dimensionality();
		ArrayList<MySubspace> population= new ArrayList<MySubspace>();
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
		  Collections.sort(population);
			return population;	
	}
	
	/**
	 * 
	 */
	public ArrayList<MySubspace> selection(ArrayList<MySubspace> population){
		Vector<Integer> probability = new Vector<Integer>();
		ArrayList<MySubspace> newPopulation = new ArrayList<MySubspace>();
		int populationCount = 0 ;
		int sum = 0 ;
		for(int t = 0 ; t<population.size() ; t++ ){
			sum = sum+ (population.size()-1 - t);
			probability.add(sum);
		}
		while(populationCount<population.size()){
			int z= (int)(Math.random()*sum);
			if(z<sum){
				for(int j = 0 ; j<probability.size()-1;j++){
					if(z>=probability.get(j)&&z<=probability.get(j+1)){
						newPopulation.add(population.get(j));
						populationCount++;
					}
				}
			}
		}
		Collections.sort(newPopulation);
		return newPopulation ;
	}
	
	
	/**
	 * 
	 */
	public ArrayList<MySubspace> Mutation(Database<V> database , ArrayList<MySubspace> pop, double perc1, double perc2)
	{
		ArrayList<MySubspace> mutations = new ArrayList<MySubspace>();
		//Set of Positions which are don´t care in the String
		TreeSet<Integer> Q = new TreeSet<Integer>();
		//Set of Positions which are not don´t care in the String
		TreeSet<Integer> R = new TreeSet<Integer>();
		
		// for each Individuum
		for (int j=0;j<pop.size();j++)
		{
			//clear the Sets
			Q.clear();
			R.clear();
			//Fill the Sets with the Positions
			for (int i=0; i<pop.get(j).getIndividium().size();i++)
			{
				if(pop.get(j).getIndividium().get(i)==0)
				{
					Q.add(i);
				}	
				else
				{
					R.add(i);
				}
					
			}
			//Mutation Variant 1
			if (Math.random() <= perc1)
			{
				//calc Mutation Spot
				Integer [] tmp3= new Integer[Q.size()];
				tmp3=Q.toArray(tmp3);
				int z=tmp3[(int)(Math.random()*tmp3.length)];

				//Mutate don´t Care into 1....phi
				pop.get(j).getIndividium().set(z,(int)(phi*Math.random()+1));
				//update Sets
				Q.remove(z);
				R.add(z);
				//calc new Mutation Spot
				tmp3=new Integer[R.size()];
				tmp3=R.toArray(tmp3);
				z=tmp3[(int)(Math.random()*tmp3.length)];
				//Mutate non don´t care into don´t care
				pop.get(j).getIndividium().set(z,0);
				//update Sets
				Q.add(z);
				R.remove(z);
			}
			//Mutation Variant 2
			if (Math.random() >= perc2)
			{
				//calc Mutation Spot
				Integer [] tmp3= new Integer[R.size()];
				tmp3=R.toArray(tmp3);
				int z = tmp3[(int) (Math.random()*tmp3.length)];
				//calc Mutation Spot
				
				pop.get(j).getIndividium().set( z , (int)(Math.random()*tmp3.length));
				//Mutate 1...phi into another 1...phi
				pop.get(j).getIndividium().set( z , (int)(phi*Math.random()+1));
			}		
			mutations.add(new MySubspace(pop.get(j).getIndividium(),fitness(database,pop.get(j).getIndividium())));
		}
		Collections.sort(mutations);
		return pop;
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
				
			}
			dim++;
		}
		HashSet<Integer> ids = ranges.get(dimNoNull.get(0)).get(noNull.get(0));
		for(int i = 0 ;i<noNull.size();i++){
				ids.retainAll(ranges.get(dimNoNull.get(i)).get(noNull.get(i)));
			
		}
		double f = (double) 1/phi;
		double nD = ids.size() ;
		double fK = Math.pow(f, k) ;
		double n = database.size();
		return (nD-n*fK)/Math.sqrt(n*fK*(1-fK));
		
	}	
	
	/**
	 * 
	 */
	public Vector<MySubspace> recombine(Database<V> database,MySubspace s1,MySubspace s2)
	{
		Vector<MySubspace> recombineVector = new Vector<MySubspace>();
		//Set of Positions in which either s1 or s2 are don´t care
		Vector<Integer> Q = new Vector<Integer>();
		//Set of Positions in which neither s1 or s2 is don´t care
		Vector<Integer> R = new Vector<Integer>();
		// Calculate Positions in the Strings
		for(int i=0;i<s1.getIndividium().size();i++)
		{
			
			if ((s1.getIndividium().get(i)== 0)&&(s2.getIndividium().get(i)!=0))Q.add(i);
			if ((s2.getIndividium().get(i)== 0)&&(s1.getIndividium().get(i)!=0))Q.add(i);
			if ((s2.getIndividium().get(i)!= 0)&&(s1.getIndividium().get(i)!=0))R.add(i);
				
		}	
		    //Enumerate the 2......2(|R|) possibilities for recombining the position in R 
		    HashMap<Integer , Vector<Vector<Integer>>> enumeration = new HashMap<Integer, Vector<Vector<Integer>>>();
			Vector<Vector<Integer>> r0  = new Vector<Vector<Integer>>();
			Vector<Integer> m1 = new Vector<Integer>();
			for(int i=0 ;i<database.dimensionality();i++){
				m1.add(0);
			}
			m1.set(R.get(0),s1.getIndividium().get(R.get(0)));
			Vector<Integer> m2 = new Vector<Integer>();
			for(int i=0 ;i<database.dimensionality();i++){
				m2.add(0);
			}
			m2.set(R.get(0),s2.getIndividium().get(R.get(0)));
			r0.add(m1);
			r0.add(m2);
			enumeration.put(0, r0);
			
			//find Ri
			//Ri = Ri-1 concatination R0
			  for(int i = 1 ; i<R.size();i++){
				  Vector<Vector<Integer>> r  = new Vector<Vector<Integer>>();
				  for(Vector<Integer> ind : enumeration.get(i-1)){
					  Vector<Integer> neuInd1 = new Vector<Integer>(database.dimensionality()); ;
					  for(int j=0 ;j<database.dimensionality();j++){
								neuInd1.add(j,ind.get(j));
						   
						}
					 neuInd1.set(R.get(i),s1.getIndividium().get(R.get(i)));
					  
					  Vector<Integer> neuInd2 = new Vector<Integer>(database.dimensionality()) ;
					  for(int j=0 ;j<database.dimensionality();j++){
						  neuInd2.add(j,ind.get(j));
					}
					  neuInd2.set(R.get(i),s2.getIndividium().get(R.get(i)));
					  
					  r.add(neuInd1);
					  r.add(neuInd2);
				   }
				  enumeration.put(i, r);
				  }
	            
			  //best combination
			  TreeSet<MySubspace> tree = new TreeSet<MySubspace>();
			  for(int i = 0 ; i< enumeration.get(R.size()-1).size(); i++){
				 Vector<Integer> individium = enumeration.get(R.size()-1).get(i);
				 tree.add(new MySubspace(individium,fitness(database,individium)));
			  }
			 
				return recombineVector ;
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
