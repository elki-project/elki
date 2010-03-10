package experimentalcode.hettab.outlier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.Vector;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.MultiResult;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.IntIntPair;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.hettab.AxisPoint;
import experimentalcode.hettab.MySubspace;


/**
 * EAFOD provides the evolutionary outlier detection algorithm, an algorithm to detect 
 * outliers for high dimensional data
 * <p>Reference:
 * <br>Outlier detection for high dimensional data
 * Outlier detection for high dimensional data
 * <br>International Conference on Management of Data
 * Proceedings of the 2001 ACM SIGMOD international conference on Management of data
 * 2001 , Santa Barbara, California, United States 
 * </p>
 * @author Ahmed Hettab
 * 
 * @param <V> the type of FeatureVector handled by this Algorithm
 */
public class EAFOD<V extends DoubleVector> extends
		AbstractAlgorithm<V, MultiResult> {

	/**
	 * OptionID for {@link #M_PARAM}
	 */
	public static final OptionID M_ID = OptionID.getOrCreateOptionID("eafod.m",
			"number of solutions");
	/**
	 * Parameter to specify the number of solutions must be an integer greater
	 * than 1.
	 * <p>
	 * Key: {@code -eafod.m}
	 * </p>
	 */
	private final IntParameter M_PARAM = new IntParameter(M_ID,
			new GreaterConstraint(1));
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
			new GreaterConstraint(1));
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
	 * random generator
	 */
	private Random random;
	
	/**
     * Provides the EAFOD algorithm,
     * adding parameters
     * {@link #M_PARAM}
     * {@link #K_PARAM}
     * {@link #PHI_PARAM}
     * to the option handler additionally to parameters of super class.
     */
	public EAFOD(Parameterization config) {
	  super(config);
		if (config.grab(K_PARAM)) {
		  k = K_PARAM.getValue();
		}
		if (config.grab(M_PARAM)) {
		  m = M_PARAM.getValue();
		}
		if (config.grab(PHI_PARAM)) {
		  phi = PHI_PARAM.getValue();
		}
		ranges = new HashMap<Integer, HashMap<Integer, HashSet<Integer>>>();
		random = new Random();
	}

  /**
	 * Performs the EAFOD algorithm on the given database.
	 */
	protected MultiResult runInTime(Database<V> database)
			throws IllegalStateException {
		
		dim = database.dimensionality();
		size = database.size();
		

		// equiDempth sets
		this.calculteDepth(database);

		ArrayList<MySubspace> pop = this.initialPopulation(m);
		// best Population
		TreeSet<MySubspace> bestSol = new TreeSet<MySubspace>();

		while (checkConvergence(bestSol) == false) {

			System.out.println("selection");
			pop = selection(pop);
			System.out.println(pop);

			// Crossover
			System.out.println("crossover");
			pop = crossover(pop);
			System.out.println(pop);
			// Mutation
			System.out.println("mutation");
			pop = mutation(pop, 0.5,0.5);
			System.out.println(pop);
			System.out.println();
			
			bestSol.addAll(pop);
			
			Iterator<MySubspace> tmp = bestSol.iterator();
			TreeSet<MySubspace> tmp2 = new TreeSet<MySubspace>();
			int i = 0;
			while ((i < m) && (tmp.hasNext())) {
				tmp2.add(tmp.next());
				i++;
			}
			//update solution
			bestSol = tmp2 ;
			System.out.println("bestSol");
			System.out.println(bestSol);
		}
	     System.out.println("terminat");
	     System.out.println(bestSol);
	     
	     List<Integer> outliers = new Vector<Integer>();
	     Iterator<MySubspace> mysubspace = bestSol.iterator() ;
	     while(mysubspace.hasNext()){
	    	 outliers.addAll(getIDs(mysubspace.next().getIndividual()));
	     }
	    
	    //TODO 
	     
	     return null ;
		
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
				dbAxis.get(dim - 1).add(point);
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
			rangesAt.put(0, b);
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
	 * check the termination criterion
	 * 
	 */
	public boolean checkConvergence(TreeSet<MySubspace> pop) {
		//
        if(pop.size()==0) return false ;
        
        //
		ArrayList<ArrayList<IntIntPair>> convDim = new ArrayList<ArrayList<IntIntPair>>();
		boolean result = true;
		MySubspace[] subspaces = new MySubspace[pop.size()];
		subspaces = pop.toArray(subspaces);

		// init count
		for (int i = 0; i < pop.size(); i++) {
			ArrayList<IntIntPair> tupels = new ArrayList<IntIntPair>();
			for (int j = 0; j < dim; j++) {
				tupels.add(j, new IntIntPair(j, 0));
			}
			convDim.add(i, tupels);

		}
		// calculate count
		for (int i = 0; i < pop.size(); i++) {
			for (int k = 0; k < dim; k++) {
				int count = convDim.get(i).get(k).getSecond();
				for (int j = 0; j < pop.size(); j++) {
					if (subspaces[i].getIndividual()[k] == subspaces[j]
							.getIndividual()[k]) {
						count++;
					}
					convDim.get(i).get(k).setSecond(count);
				}
			}
		}
		
		// 
		

		for (int i = 0; i < convDim.size(); i++) {
			boolean converged = false;

			// convergence of each dimension
			for (int j = 0; j < convDim.get(i).size(); j++) {
				if (convDim.get(i).get(j).getSecond() >= pop.size() * 0.95)
					converged = true;
			}
			if (!converged)
				result = false;
		}
		return result;
	}
	

	/**
	 * Initial seed population 
	 */
	public ArrayList<MySubspace> initialPopulation(int popsize) {

		// Initial Population
		ArrayList<MySubspace> population = new ArrayList<MySubspace>();

		// fill population
		for (int i = 0; i < popsize; i++) {
			// Random Individual
			int[] Individual = new int[dim];
			// fill don't care ( any dimension == don't care)
			for (int j = 0; j < dim; j++) {
				Individual[j] = 0;
			}
			// count of don't care positions
			int countDim = k;
			// fill non don't care positions of the Individual
			while (countDim > 0) {
				int z = random.nextInt(dim);
				if (Individual[z] == 0) {
					Individual[z] = random.nextInt(phi) + 1;
					countDim--;
				}
			}
			population.add(new MySubspace(Individual, fitness(Individual)));
		}
		Collections.sort(population);
		return population;
	}

	/**
	 * the selection criterion for the genetic algorithm:
	 * <br>
	 * roulette wheel machanism:
	 * <br>
	 *  where the probability of sampling an individual of the population
	 * was proportional to p - r(i), where p is the size of population and r(i) the rank of i-th individual
	 * @param population
	 */
	public ArrayList<MySubspace> selection(ArrayList<MySubspace> population) {
		// probability
		// Roulette weehl
		Vector<Integer> probability = new Vector<Integer>();
		// set of selected individual
		ArrayList<MySubspace> newPopulation = new ArrayList<MySubspace>();
		int sum = 0;
		probability.add(sum);
		// calculate probability
		for (int t = 0; t < population.size(); t++) {
			sum = sum + (population.size() - t);
			probability.add(sum);
		}

		// position of selection
		for (int i = 0; i < population.size(); i++) {
			int z = random.nextInt(sum);
			for (int j = 0; j < probability.size() - 1; j++) {
				if (z >= probability.get(j) && z <= probability.get(j + 1)) {
					// add selected pop
					MySubspace subspace = population.get(j);
					newPopulation.add(subspace);
				}
			}
		}
		if (newPopulation.size() == 0) {
			System.exit(0);
		}
		//sort initialPopulation
		Collections.sort(newPopulation);
		return newPopulation;
	}

	/**
	 * method implements the mutation algorithm
	 */
	public ArrayList<MySubspace> mutation(ArrayList<MySubspace> population,
			double perc1, double perc2) {
		// the Mutations
		ArrayList<MySubspace> mutations = new ArrayList<MySubspace>();
		// Set of Positions which are don�t care in the String
		TreeSet<Integer> Q = new TreeSet<Integer>();
		// Set of Positions which are not don�t care in the String
		TreeSet<Integer> R = new TreeSet<Integer>();

		// for each Individuum
		for (int j = 0; j < population.size(); j++) {
			// clear the Sets
			Q.clear();
			R.clear();
			// Fill the Sets with the Positions
			for (int i = 0; i < dim; i++) {
				if (population.get(j).getIndividual()[i] == 0) {
					Q.add(i);
				} else {
					R.add(i);
				}

			}
			//
			double r1 = random.nextDouble() ;
			if (Q.size() != 0){
				// Mutation Variant 1
				if (r1 <= perc1) {
					// calc Mutation Spot
					Integer[] pos = new Integer[Q.size()];
					pos = Q.toArray(pos);
					int position = random.nextInt(pos.length);
					int depth = pos[position];
					// Mutate don�t Care into 1....phi
					population.get(j).getIndividual()[depth] = random
							.nextInt(phi) + 1;
					// update Sets
					Q.remove(depth);
					R.add(depth);
					// calc new Mutation Spot
					pos = new Integer[R.size()];
					pos = R.toArray(pos);
					position = random.nextInt(pos.length);
					depth = pos[position];
					// Mutate non don�t care into don�t care
					population.get(j).getIndividual()[depth] = 0;
					// update Sets
					Q.add(depth);
					R.remove(depth);
				}
			}
		     r1 = random.nextDouble() ;
				// Mutation Variant 2
				if (r1 <= perc2) {
					// calc Mutation Spot
					Integer[] pos = new Integer[R.size()];
					pos = R.toArray(pos);
					int position = random.nextInt(pos.length);
					int depth = pos[position];
					// Mutate 1...phi into another 1...phi
					population.get(j).getIndividual()[depth] = random
							.nextInt(phi) + 1;
				}
			int[] individual = population.get(j).getIndividual();
			mutations.add(new MySubspace(individual, fitness(individual)));

		}
		Collections.sort(mutations);
		return mutations;
	}

	/**
	 * method calculate the fitness of individual
	 * @param individual
	 * @return sparsity coefficient
	 */
	public double fitness(int[] individual) {

		HashSet<Integer> m = new HashSet<Integer>(ranges.get(1).get(
				individual[0]));
		//intersect
		for (int i = 2; i <= individual.length; i++) {
			HashSet<Integer> current = new HashSet<Integer>(ranges.get(i).get(
					individual[i - 1]));
			HashSet<Integer> result = retainAll(m, current);
			m.clear();
			m.addAll(result);
		}
		//calculate sparsity c
		double f = (double) 1 / phi;
		double nD = (double) m.size();
		double fK = Math.pow(f, k);
		double sC = (nD - (size * fK)) / Math.sqrt(size * fK * (1 - fK));
		return sC;
	}
	
	/**
	 * method get the ids of individual
	 */
      public Vector<Integer> getIDs (int[] individual){
    	  
    	HashSet<Integer> m = new HashSet<Integer>(ranges.get(1).get(
  				individual[0]));
  		//intersect
  		for (int i = 2; i <= individual.length; i++) {
  			HashSet<Integer> current = new HashSet<Integer>(ranges.get(i).get(
  					individual[i - 1]));
  			HashSet<Integer> result = retainAll(m, current);
  			m.clear();
  			m.addAll(result);
  		}
  		Vector<Integer> ids = new Vector<Integer>();
  		ids.addAll(m);
  		
  		return ids ; 
      }

	/**
	 * method implements the crossover algorithm
	 * 
	 */
	public ArrayList<MySubspace> crossover(ArrayList<MySubspace> population) {

		// Crossover Set of population Set
		ArrayList<MySubspace> crossover = new ArrayList<MySubspace>();

		// recombine vector
		Pair<MySubspace , MySubspace> recombine ;
		//
		MySubspace[] pop = new MySubspace[population.size()];
		pop = population.toArray(pop);
		int i = 0;

		// Match the Solutions pairwise
		while (i < (population.size() / 2) * 2) {
			recombine = recombine(pop[i], pop[i + 1]);
			i = i + 2;
			// add the Solutions to the new Set
			crossover.add(recombine.getFirst());
			crossover.add(recombine.getSecond());
		}
		// if the set contains an odd number of Subspaces
		if (pop.length % 2 == 1)
			crossover.add(pop[pop.length - 1]);
		Collections.sort(crossover);
		return crossover;

	}

	/**
	 * 
	 * method implements the recombine algorithm
	 */
	public Pair<MySubspace,MySubspace> recombine(MySubspace s1, MySubspace s2) {
		
		Pair<MySubspace,MySubspace> recombinePair ;
		// Set of Positions in which either s1 or s2 are don�t care
		TreeSet<Integer> Q = new TreeSet<Integer>();
		// Set of Positions in which neither s1 or s2 is don�t care
		TreeSet<Integer> R = new TreeSet<Integer>();

		for (int i = 0; i < s1.getIndividual().length; i++) {

			if ((s1.getIndividual()[i] == 0) && (s2.getIndividual()[i] != 0))
				Q.add(i);
			if ((s2.getIndividual()[i] == 0) && (s1.getIndividual()[i] != 0))
				Q.add(i);
			if ((s1.getIndividual()[i] != 0) && (s2.getIndividual()[i] != 0)) {
				R.add(i);
			}
		}
		

		
		int[] best2R = new int[dim];
		//select the fittest combination of R
		if(R.size()!=0){
			TreeSet<MySubspace> bestCombi = new TreeSet<MySubspace>();
			//best 2R Combination
			ArrayList<int[]> best = comb(R, s1, s2);
			Iterator<int[]> m = best.iterator();
         
			while (m.hasNext()) {
				int[] next = m.next();
				bestCombi.add(new MySubspace(next, fitness(next)));
			      }
		    best2R = bestCombi.first().getIndividual();
		}
		//if R.size() == 0 only extends String
		else{
			for(int i = 0 ;i<dim ; i++){
				best2R[i]= 0 ;
			}
		}

        //Extends String greedily
		int[] b = best2R.clone();
		int count = k - R.size();
		Iterator<Integer> q = Q.iterator();

		while (count > 0) {
            int pos = 0 ;
			int[] l1 = b.clone();
			int[] l2 = b.clone();

			while (q.hasNext()) {

				int next = q.next();
				pos = next ;
                 
		          {
					boolean s1Null = (s1.getIndividual()[next] == 0);
					boolean s2Null = (s1.getIndividual()[next] == 0);

					l1[next] = s1.getIndividual()[next];
					l2[next] = s2.getIndividual()[next];

					if (fitness(l1) >= fitness(l2)) {
						b = l1.clone();
						if (s1Null) count--;
						
					}
					else {
						b = l2.clone();
						if (s2Null)	count--;
			    			}
				}
				
			}
			Q.remove(pos);
		}

		best2R = b.clone();

		// create the complementary String
		int[] comp = new int[dim];

		for (int i = 0; i < dim; i++) {
			if (best2R[i] == s1.getIndividual()[i])
				comp[i] = s2.getIndividual()[i];
			else
				comp[i] = s2.getIndividual()[i];
		}
		recombinePair = new Pair<MySubspace , MySubspace>(new MySubspace(best2R, fitness(best2R)),new MySubspace(comp, fitness(comp)));

		return recombinePair;
	}

	/**
	 * method generate the total subspace
	 */
	public static MySubspace nullSubspace(int dim) {
		int[] individual = new int[dim];
		for (int i = 0; i < dim; i++) {
			individual[i] = 0;
		}
		return new MySubspace(individual);
	}
	
	/**
	 * 
	 * @param set1
	 * @param set2
	 * @return
	 */
	public static HashSet<Integer> retainAll(HashSet<Integer> set1,
			HashSet<Integer> set2) {
		HashSet<Integer> result = new HashSet<Integer>();
		for (Integer id : set1) {
			if (set2.contains(id)) {
				result.add(id);
			}
		}
		for (Integer id : set2) {
			if (set1.contains(id)) {
				result.add(id);
			}
		}
		return result;
	}


	/**
	 * method implements the 
	 */
	public static ArrayList<int[]> comb(TreeSet<Integer> R, MySubspace s1,
			MySubspace s2) {

		ArrayList<int[]> mySubspaces = new ArrayList<int[]>();
		int size = R.size();
		HashMap<Integer, ArrayList<String>> comb = new HashMap<Integer, ArrayList<String>>();
		ArrayList<String> r1 = new ArrayList<String>();
		r1.add(new String("1"));
		r1.add(new String("2"));
		comb.put(1, r1);
		for (int i = 2; i <= size; i++) {
			ArrayList<String> ri = new ArrayList<String>();
			ArrayList<String> rOld = comb.get(i - 1);
			for (int j = 0; j < rOld.size(); j++) {
				String neu1 = rOld.get(j) + "1";
				String neu2 = rOld.get(j) + "2";
				ri.add(neu1);
				ri.add(neu2);
			}
			comb.put(i, ri);
			comb.remove(i - 1);
		}
		
		//
		for (int i = 0; i < comb.get(size).size(); i++) {
			Iterator<Integer> r = R.iterator();
			MySubspace m = nullSubspace(s1.getIndividual().length);
			String s = comb.get(size).get(i);
			for (int pos = 0; pos < s.length(); pos++) {
				int d = r.next();
				if (s.charAt(pos) == '1') {
					m.getIndividual()[d] = s1.getIndividual()[d];
				}
				if (s.charAt(pos) == '2') {
					m.getIndividual()[d] = s2.getIndividual()[d];
				}
			}
			int[] t = m.getIndividual().clone();
			mySubspaces.add(t);
		}
		return mySubspaces;
	}

	/**
	 * 
	 */
	@Override
	public Description getDescription() {
		//TODO
		return new Description("EAFOD", "the evolutionary outlier detection algorithm", "Outlier detection for high dimensional data", "Outlier detection for high dimensional data :");
	}
    
	/**
	 * 
	 */
	@Override
	public MultiResult getResult() {
		// TODO Auto-generated method stub
		return null;
	}

}
