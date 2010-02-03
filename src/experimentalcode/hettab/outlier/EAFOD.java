package experimentalcode.hettab.outlier;

import java.util.ArrayList;
import java.util.BitSet;
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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import experimentalcode.hettab.AxisPoint;
import experimentalcode.hettab.MySubspace;
import experimentalcode.hettab.Tupel;

/**
 * 
 * @author hettab
 * 
 * @param <V>
 */
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
			new GreaterConstraint(0));
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
	private int dim;

	/**
	 * 
	 */
	private int size;

	/**
	 * 
	 */
	private HashMap<Integer, HashMap<Integer, HashSet<Integer>>> ranges;
	/**
	 * 
	 */
	private Random random;

	/**
	 * 
	 */
	public EAFOD() {

		addOption(K_PARAM);
		addOption(M_PARAM);
		addOption(PHI_PARAM);
		ranges = new HashMap<Integer, HashMap<Integer, HashSet<Integer>>>();

	}

	@Override
	protected MultiResult runInTime(Database<V> database)
			throws IllegalStateException {

		dim = database.dimensionality();
		size = database.size();
		random = new Random();

		// equiDempth sets
		this.calculteDepth(database);

		// System.exit(0);

		ArrayList<MySubspace> pop = this.initialPopulation(10);
		// best Population
		TreeSet<MySubspace> bestSol = new TreeSet<MySubspace>(pop);

		System.out.println(bestSol);

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
			pop = mutation(pop, 0.1, 0.1);
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

			System.out.println("bestSol");
			System.out.println(bestSol);
		}

		return null;
	}

	/**
	 * 
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
		// database.size()/phi +1 , rest..phi => |range| = database.size()/phi )
		int rest = database.size() % phi;
		int f = database.size() / phi;

		BitSet r = new BitSet();
		HashSet<Integer> b = new HashSet<Integer>();
		for (Integer id : database) {
			r.set(id);
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
	 * 
	 */
	public boolean checkConvergence(TreeSet<MySubspace> pop) {

		ArrayList<ArrayList<Tupel>> convDim = new ArrayList<ArrayList<Tupel>>();
		boolean result = true;
		MySubspace[] subspaces = new MySubspace[pop.size()];
		subspaces = pop.toArray(subspaces);

		// init count
		for (int i = 0; i < pop.size(); i++) {
			ArrayList<Tupel> tupels = new ArrayList<Tupel>();
			for (int j = 0; j < dim; j++) {
				tupels.add(j, new Tupel(j, 0));
			}
			convDim.add(i, tupels);

		}
		// calculate count
		for (int i = 0; i < pop.size(); i++) {
			for (int k = 0; k < dim; k++) {
				int count = convDim.get(i).get(k).getSecond();
				for (int j = 0; j < pop.size(); j++) {
					if (subspaces[i].getIndividium()[k] == subspaces[j]
							.getIndividium()[k]) {
						count++;
					}
					convDim.get(i).get(k).setSecond(count);
				}
			}
		}

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
	 * 
	 */
	public ArrayList<MySubspace> initialPopulation(int popsize) {

		// Initial Population
		ArrayList<MySubspace> population = new ArrayList<MySubspace>();

		// fill population

		for (int i = 0; i < popsize; i++) {

			// Random Individium
			int[] individium = new int[dim];

			// fill don't care ( any dimension == don't care)
			for (int j = 0; j < dim; j++) {
				individium[j] = 0;
			}

			// count of don't care positions
			int countDim = k;

			// fill non don't care positions of the Individium
			while (countDim > 0) {
				int z = random.nextInt(dim);
				if (individium[z] == 0) {
					individium[z] = random.nextInt(phi) + 1;
					countDim--;
				}
			}

			population.add(new MySubspace(individium, fitness(individium)));
		}

		Collections.sort(population);
		return population;
	}

	/**
	 * 
	 */
	public ArrayList<MySubspace> selection(ArrayList<MySubspace> population) {

		// probability
		// Roulette weehl
		Vector<Integer> probability = new Vector<Integer>();

		// set of selected individium
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
		Collections.sort(newPopulation);
		return newPopulation;
	}

	/**
	 * 
	 */
	public ArrayList<MySubspace> mutation(ArrayList<MySubspace> population,
			double perc1, double perc2) {
		// the Mutations
		ArrayList<MySubspace> mutations = new ArrayList<MySubspace>();
		// Set of Positions which are don´t care in the String
		TreeSet<Integer> Q = new TreeSet<Integer>();
		// Set of Positions which are not don´t care in the String
		TreeSet<Integer> R = new TreeSet<Integer>();

		// for each Individuum
		for (int j = 0; j < population.size(); j++) {
			// clear the Sets
			Q.clear();
			R.clear();
			// Fill the Sets with the Positions
			for (int i = 0; i < dim; i++) {
				if (population.get(j).getIndividium()[i] == 0) {
					Q.add(i);
				} else {
					R.add(i);
				}

			}
			if (Q.size() != 0)
				// Mutation Variant 1
				if (Math.random() <= perc1) {
					// calc Mutation Spot
					Integer[] pos = new Integer[Q.size()];
					pos = Q.toArray(pos);
					int position = random.nextInt(pos.length);
					int depth = pos[position];
					// Mutate don´t Care into 1....phi
					population.get(j).getIndividium()[depth] = random
							.nextInt(phi) + 1;
					// update Sets
					Q.remove(depth);
					R.add(depth);
					// calc new Mutation Spot
					pos = new Integer[R.size()];
					pos = R.toArray(pos);
					position = random.nextInt(pos.length);
					depth = pos[position];
					// Mutate non don´t care into don´t care
					population.get(j).getIndividium()[depth] = 0;
					// update Sets
					Q.add(depth);
					R.remove(depth);
				}
			if (R.size() != 0)
				// Mutation Variant 2
				if (Math.random() <= perc2) {
					// calc Mutation Spot
					Integer[] pos = new Integer[R.size()];
					pos = R.toArray(pos);
					int position = random.nextInt(pos.length);
					int depth = pos[position];
					// Mutate 1...phi into another 1...phi
					population.get(j).getIndividium()[depth] = random
							.nextInt(phi) + 1;
				}
			int[] individium = population.get(j).getIndividium();
			mutations.add(new MySubspace(individium, fitness(individium)));

		}
		if (mutations.size() == 0) {
			System.exit(0);
		}
		Collections.sort(mutations);
		return mutations;
	}

	/**
	 * 
	 * @param individium
	 * @return
	 */
	public double fitness(int[] individium) {

		HashSet<Integer> m = new HashSet<Integer>(ranges.get(1).get(
				individium[0]));
		for (int i = 2; i <= individium.length; i++) {
			HashSet<Integer> current = new HashSet<Integer>(ranges.get(i).get(
					individium[i - 1]));
			HashSet<Integer> result = retainAll(m, current);
			m.clear();
			m.addAll(result);
		}
		double f = (double) 1 / phi;
		double nD = (double) m.size();
		double fK = Math.pow(f, k);
		double sC = (nD - (size * fK)) / Math.sqrt(size * fK * (1 - fK));
		return sC;
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
	 * Crossover.
	 * 
	 * @param pop
	 *            the pop
	 * 
	 * @return the tree set< Myubspace>
	 */
	public ArrayList<MySubspace> crossover(ArrayList<MySubspace> population) {

		// Crossover Set of population Set
		ArrayList<MySubspace> crossover = new ArrayList<MySubspace>();

		// recombine vector
		Vector<MySubspace> recombine = new Vector<MySubspace>();
		//
		MySubspace[] pop = new MySubspace[population.size()];
		pop = population.toArray(pop);
		int i = 0;

		// Match the Solutions pairwise
		while (i < (population.size() / 2) * 2) {
			recombine = recombine(pop[i], pop[i + 1]);
			i = i + 2;
			// add the Solutions to the new Set
			crossover.add(recombine.get(0));
			crossover.add(recombine.get(1));
		}
		// if the set contains an odd number of Subspaces
		if (pop.length % 2 == 1)
			crossover.add(pop[pop.length - 1]);
		Collections.sort(crossover);
		if (crossover.size() == 0) {
			System.exit(0);
		}
		return crossover;

	}

	/**
	 * 
	 * @param s1
	 * @param s2
	 * @return
	 */
	public Vector<MySubspace> recombine(MySubspace s1, MySubspace s2) {
		// TODO
		Vector<MySubspace> recombineVector = new Vector<MySubspace>();

		// Set of Positions in which either s1 or s2 are don´t care
		TreeSet<Integer> Q = new TreeSet<Integer>();
		// Set of Positions in which neither s1 or s2 is don´t care
		TreeSet<Integer> R = new TreeSet<Integer>();

		for (int i = 0; i < s1.getIndividium().length; i++) {

			if ((s1.getIndividium()[i] == 0) && (s2.getIndividium()[i] != 0))
				Q.add(i);
			if ((s2.getIndividium()[i] == 0) && (s1.getIndividium()[i] != 0))
				Q.add(i);
			if ((s1.getIndividium()[i] != 0) && (s2.getIndividium()[i] != 0)) {
				R.add(i);
			}
		}
		/**
		 * Iterator<Integer> r = R.iterator(); // Selection of the Best
		 * Combination of R Spots TreeSet<MySubspace> tmp2= new
		 * TreeSet<MySubspace>(); tmp2.add(nullSubspace(dim));
		 * 
		 * // For every Position in R while (r.hasNext()) { int index =
		 * r.next(); MySubspace[] tmp4= new MySubspace[tmp2.size()];
		 * tmp4=tmp2.toArray(tmp4); //For every String in the Enumeration for
		 * (int i=0; i< tmp4.length;i++) { //Set the new Positions int[] mod1=
		 * tmp4[i].getIndividium().clone(); int[] mod2=
		 * tmp4[i].getIndividium().clone();
		 * 
		 * mod1[index]=s1.getIndividium()[index];
		 * mod2[index]=s2.getIndividium()[index]; //Add to the Enumeration
		 * 
		 * tmp2.add(new MySubspace(mod1,fitness(mod1))); tmp2.add(new
		 * MySubspace(mod2,fitness(mod2)));
		 * 
		 * //remove the cloned individuum tmp2.remove(tmp4[i]); }
		 * 
		 * }
		 **/
		TreeSet<MySubspace> bestCombi = new TreeSet<MySubspace>();
		ArrayList<int[]> best = comb(R, s1, s2);
		Iterator<int[]> m = best.iterator();

		while (m.hasNext()) {
			int[] next = m.next();
			bestCombi.add(new MySubspace(next, fitness(next)));
		}

		// Select the fittest
		int[] best2R = bestCombi.first().getIndividium();

		// pos of don't care in best2R
		TreeSet<Integer> nullDepth = new TreeSet<Integer>();
		for (int i = 0; i < dim; i++) {
			if (best2R[i] == 0)
				nullDepth.add(i);
		}
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
				if (nullDepth.contains(next))
                 
		          {
					boolean s1Null = (s1.getIndividium()[next] == 0);
					boolean s2Null = (s1.getIndividium()[next] == 0);

					l1[next] = s1.getIndividium()[next];
					l2[next] = s2.getIndividium()[next];

					if (fitness(l1) >= fitness(l2)) {
						b = l1.clone();
						if (s1Null) {
							nullDepth.add(next);
							count--;
						}
					} else {
						b = l2.clone();
						if (s2Null) {
							nullDepth.add(next);
							count--;
						}
					}
				}
				
			}
			Q.remove(pos);
		}

		best2R = b.clone();

		// create the complementary String
		int[] comp = new int[dim];

		for (int i = 0; i < dim; i++) {
			if (best2R[i] == s1.getIndividium()[i])
				comp[i] = s2.getIndividium()[i];
			else
				comp[i] = s2.getIndividium()[i];
		}
		recombineVector.add(new MySubspace(best2R, fitness(best2R)));
		recombineVector.add(new MySubspace(comp, fitness(comp)));

		return recombineVector;
	}

	/**
	 * 
	 */
	public static MySubspace nullSubspace(int dim) {
		int[] individium = new int[dim];
		for (int i = 0; i < dim; i++) {
			individium[i] = 0;
		}
		return new MySubspace(individium);
	}

	/**
	 * 
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
		for (int i = 0; i < comb.get(size).size(); i++) {
			Iterator<Integer> r = R.iterator();
			MySubspace m = nullSubspace(s1.getIndividium().length);
			String s = comb.get(size).get(i);
			for (int pos = 0; pos < s.length(); pos++) {
				int d = r.next();
				if (s.charAt(pos) == '1') {
					m.getIndividium()[d] = s1.getIndividium()[d];
				}
				if (s.charAt(pos) == '2') {
					m.getIndividium()[d] = s2.getIndividium()[d];
				}
			}
			int[] t = m.getIndividium().clone();
			mySubspaces.add(t);
		}
		return mySubspaces;
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
