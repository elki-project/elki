package de.lmu.ifi.dbs.elki.algorithm.outlier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeSet;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.result.AnnotationFromDataStore;
import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.result.outlier.InvertedOutlierScoreMeta;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierScoreMeta;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.TopBoundedHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.FCPair;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * EAFOD provides the evolutionary outlier detection algorithm, an algorithm to
 * detect outliers for high dimensional data.
 * <p>
 * Reference: <br />
 * Outlier detection for high dimensional data Outlier detection for high
 * dimensional data <br />
 * C.C. Aggarwal, P. S. Yu <br />
 * Proceedings of the 2001 ACM SIGMOD international conference on Management of
 * data 2001, Santa Barbara, California, United States
 * </p>
 * 
 * @author Ahmed Hettab
 * @author Erich Schubert
 * 
 * @apiviz.has EvolutionarySearch oneway - - runs
 * @apiviz.has Individuum oneway - - obtains
 * 
 * @param <V> the type of FeatureVector handled by this Algorithm
 */
// TODO: progress logging!
@Title("EAFOD: the evolutionary outlier detection algorithm")
@Description("Outlier detection for high dimensional data")
@Reference(authors = "C.C. Aggarwal, P. S. Yu", title = "Outlier detection for high dimensional data", booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD 2001), Santa Barbara, CA, 2001", url = "http://dx.doi.org/10.1145/375663.375668")
public class AggarwalYuEvolutionary<V extends NumberVector<?, ?>> extends AbstractAggarwalYuOutlier<V> {
  /**
   * The logger for this class.
   */
  protected static final Logging logger = Logging.getLogger(AggarwalYuEvolutionary.class);

  /**
   * Parameter to specify the number of solutions must be an integer greater
   * than 1.
   * <p>
   * Key: {@code -eafod.m}
   * </p>
   */
  public static final OptionID M_ID = OptionID.getOrCreateOptionID("ay.m", "Population size for evolutionary algorithm.");

  /**
   * Parameter to specify the random generator seed.
   */
  public static final OptionID SEED_ID = OptionID.getOrCreateOptionID("ay.seed", "The random number generator seed.");

  /**
   * Maximum iteration count for evolutionary search.
   */
  protected final int MAX_ITERATIONS = 1000;

  /**
   * Holds the value of {@link #M_ID}.
   */
  private int m;

  /**
   * Holds the value of {@link #SEED_ID}.
   */
  private Long seed;

  /**
   * Constructor.
   * 
   * @param k K
   * @param phi Phi
   * @param m M
   * @param seed Seed
   */
  public AggarwalYuEvolutionary(int k, int phi, int m, Long seed) {
    super(k, phi);
    this.m = m;
    this.seed = seed;
  }

  /**
   * Performs the evolutionary algorithm on the given database.
   * 
   * @param database Database
   * @param relation Relation
   * @return Result
   * @throws IllegalStateException
   */
  public OutlierResult run(Database database, Relation<V> relation) throws IllegalStateException {
    final int dbsize = relation.size();
    ArrayList<ArrayList<DBIDs>> ranges = buildRanges(relation);

    Collection<Individuum> individuums = (new EvolutionarySearch(relation, ranges, m, seed)).run();

    WritableDataStore<Double> outlierScore = DataStoreUtil.makeStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC, Double.class);
    for(Individuum ind : individuums) {
      DBIDs ids = computeSubspaceForGene(ind.getGene(), ranges);
      double sparsityC = sparsity(ids.size(), dbsize, k);
      for(DBID id : ids) {
        Double prev = outlierScore.get(id);
        if(prev == null || sparsityC < prev) {
          outlierScore.put(id, sparsityC);
        }
      }
    }

    DoubleMinMax minmax = new DoubleMinMax();
    for(DBID id : relation.iterDBIDs()) {
      Double val = outlierScore.get(id);
      if(val == null) {
        outlierScore.put(id, 0.0);
        val = 0.0;
      }
      minmax.put(val);
    }
    AnnotationResult<Double> scoreResult = new AnnotationFromDataStore<Double>("AggarwalYuEvolutionary", "aggarwal-yu-outlier", AGGARWAL_YU_SCORE, outlierScore, relation.getDBIDs());
    OutlierScoreMeta meta = new InvertedOutlierScoreMeta(minmax.getMin(), minmax.getMax(), Double.NEGATIVE_INFINITY, 0.0);
    return new OutlierResult(meta, scoreResult);
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * The inner class to handle the actual evolutionary computation.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has Individuum oneway - - evolves
   */
  private class EvolutionarySearch {
    /**
     * Database size
     */
    final int dbsize;

    /**
     * Database dimensionality
     */
    final int dim;

    /**
     * Database ranges
     */
    final ArrayList<ArrayList<DBIDs>> ranges;

    /**
     * m to use.
     */
    final int m;

    /**
     * random generator
     */
    final private Random random;

    /**
     * Constructor.
     * 
     * @param database Database to use
     * @param m Population size
     * @param seed Random generator seed
     */
    public EvolutionarySearch(Relation<V> database, ArrayList<ArrayList<DBIDs>> ranges, int m, Long seed) {
      super();
      this.ranges = ranges;
      this.m = m;
      this.dbsize = database.size();
      this.dim = DatabaseUtil.dimensionality(database);
      if (seed != null) {
        this.random = new Random(seed);
      } else {
        this.random = new Random();
      }
    }

    public Collection<Individuum> run() {
      ArrayList<Individuum> pop = initialPopulation(m);
      // best Population
      TopBoundedHeap<Individuum> bestSol = new TopBoundedHeap<Individuum>(m, Collections.reverseOrder());
      bestSol.addAll(pop);

      int iterations = 0;
      while(!checkConvergence(pop)) {
        Collections.sort(pop);
        pop = rouletteRankSelection(pop);
        // Crossover
        pop = crossoverOptimized(pop);
        // Mutation with probability 0.25 , 0.25
        pop = mutation(pop, 0.5, 0.5);
        // Avoid duplicates
        for(Individuum ind : pop) {
          if(!bestSol.contains(ind)) {
            bestSol.add(ind);
          }
        }
        if(logger.isDebuggingFinest()) {
          StringBuffer buf = new StringBuffer();
          buf.append("Top solutions:\n");
          for(Individuum ind : bestSol) {
            buf.append(ind.toString()).append("\n");
          }
          buf.append("Population:\n");
          for(Individuum ind : pop) {
            buf.append(ind.toString()).append("\n");
          }
          logger.debugFinest(buf.toString());
        }
        iterations++;
        if(iterations > MAX_ITERATIONS) {
          logger.warning("Maximum iterations reached.");
          break;
        }
      }
      return bestSol;
    }

    /**
     * check the termination criterion
     * 
     */
    private boolean checkConvergence(Collection<Individuum> pop) {
      if(pop.size() == 0) {
        return true;
      }

      // Gene occurrence counter
      int[][] occur = new int[dim][phi + 1];
      // Count gene occurrences
      for(Individuum ind : pop) {
        int[] gene = ind.getGene();
        for(int d = 0; d < dim; d++) {
          int val = gene[d] + DONT_CARE;
          if(val < 0 || val >= phi + 1) {
            logger.warning("Invalid gene value encountered: " + val + " in " + ind.toString());
            continue;
          }
          occur[d][val] += 1;
        }
      }

      int conv = (int) (pop.size() * 0.95);
      if(logger.isDebuggingFine()) {
        logger.debugFine("Convergence at " + conv + " of " + pop.size() + " individuums.");
      }
      for(int d = 0; d < dim; d++) {
        boolean converged = false;

        for(int val = 0; val < phi + 1; val++) {
          if(occur[d][val] >= conv) {
            converged = true;
            break;
          }
        }
        // A single failure to converge is sufficient to continue.
        if(!converged) {
          return false;
        }
      }
      return true;
    }

    /**
     * Produce an initial (random) population.
     * 
     * @param popsize Population size
     * @return Sorted list of Individuums
     */
    private ArrayList<Individuum> initialPopulation(int popsize) {
      // Initial Population
      ArrayList<Individuum> population = new ArrayList<Individuum>(popsize);
      // fill population
      for(int i = 0; i < popsize; i++) {
        // Random Individual
        int[] gene = new int[dim];
        // fill don't care ( any dimension == don't care)
        for(int j = 0; j < dim; j++) {
          gene[j] = DONT_CARE;
        }
        // count of don't care positions
        int countDim = k;
        // fill non don't care positions of the Individual
        while(countDim > 0) {
          int z = random.nextInt(dim);
          if(gene[z] == DONT_CARE) {
            gene[z] = random.nextInt(phi) + 1;
            countDim--;
          }
        }
        population.add(makeIndividuum(gene));
      }
      Collections.sort(population);
      return population;
    }

    /**
     * the selection criterion for the genetic algorithm: <br>
     * roulette wheel mechanism: <br>
     * where the probability of sampling an individual of the population was
     * proportional to p - r(i), where p is the size of population and r(i) the
     * rank of i-th individual
     * 
     * @param population
     */
    private ArrayList<Individuum> rouletteRankSelection(ArrayList<Individuum> population) {
      final int popsize = population.size();
      // Relative weight := popsize - position => sum(1..popsize)
      int totalweight = popsize * (popsize + 1) / 2;
      // Survivors
      ArrayList<Individuum> survivors = new ArrayList<Individuum>(popsize);

      // position of selection
      for(int i = 0; i < popsize; i++) {
        int z = random.nextInt(totalweight);
        for(int j = 0; j < popsize; j++) {
          if(z < popsize - j) {
            // TODO: need clone?
            survivors.add(population.get(j));
            break;
          }
          else {
            // decrement
            z -= (popsize - j);
          }
        }
      }
      if(survivors.size() != popsize) {
        throw new AbortException("Selection step failed - implementation error?");
      }
      // Don't sort, to avoid biasing the crossover!
      // Collections.sort(survivors);
      return survivors;
    }

    /**
     * method implements the mutation algorithm
     */
    private ArrayList<Individuum> mutation(ArrayList<Individuum> population, double perc1, double perc2) {
      // the Mutations
      ArrayList<Individuum> mutations = new ArrayList<Individuum>();
      // Set of Positions which are don't care in the String
      TreeSet<Integer> Q = new TreeSet<Integer>();
      // Set of Positions which are not don't care in the String
      TreeSet<Integer> R = new TreeSet<Integer>();

      // for each individuum
      for(int j = 0; j < population.size(); j++) {
        // clear the Sets
        Q.clear();
        R.clear();
        // Fill the Sets with the Positions
        for(int i = 0; i < dim; i++) {
          if(population.get(j).getGene()[i] == DONT_CARE) {
            Q.add(i);
          }
          else {
            R.add(i);
          }
        }
        //
        double r1 = random.nextDouble();
        if(Q.size() != 0) {
          // Mutation Variant 1
          if(r1 <= perc1) {
            // calc Mutation Spot
            Integer[] pos = new Integer[Q.size()];
            pos = Q.toArray(pos);
            int position = random.nextInt(pos.length);
            int depth = pos[position];
            // Mutate don't care into 1....phi
            population.get(j).getGene()[depth] = random.nextInt(phi) + 1;
            // update Sets
            Q.remove(depth);
            R.add(depth);
            // calc new Mutation Spot
            pos = new Integer[R.size()];
            pos = R.toArray(pos);
            position = random.nextInt(pos.length);
            depth = pos[position];
            // Mutate non don't care into don't care
            population.get(j).getGene()[depth] = DONT_CARE;
            // update Sets
            Q.add(depth);
            R.remove(depth);
          }
        }
        r1 = random.nextDouble();
        // Mutation Variant 2
        if(r1 <= perc2) {
          // calc Mutation Spot
          Integer[] pos = new Integer[R.size()];
          pos = R.toArray(pos);
          int position = random.nextInt(pos.length);
          int depth = pos[position];
          // Mutate 1...phi into another 1...phi
          population.get(j).getGene()[depth] = random.nextInt(phi) + 1;
        }
        int[] gene = population.get(j).getGene();
        mutations.add(makeIndividuum(gene));

      }
      Collections.sort(mutations);
      return mutations;
    }

    /**
     * Make a new individuum helper, computing sparsity=fitness
     * 
     * @param gene Gene to evaluate
     * @return new individuum
     */
    private Individuum makeIndividuum(int[] gene) {
      final DBIDs ids = computeSubspaceForGene(gene, ranges);
      final double fitness = (ids.size() > 0) ? sparsity(ids.size(), dbsize, k) : Double.MAX_VALUE;
      return new Individuum(fitness, gene);
    }

    /**
     * method implements the crossover algorithm
     */
    private ArrayList<Individuum> crossoverOptimized(ArrayList<Individuum> population) {
      // Crossover Set of population Set
      ArrayList<Individuum> crossover = new ArrayList<Individuum>();

      for(int i = 0; i < population.size() - 1; i += 2) {
        Pair<Individuum, Individuum> recombine = recombineOptimized(population.get(i), population.get(i + 1));
        // add the Solutions to the new Set
        crossover.add(recombine.getFirst());
        crossover.add(recombine.getSecond());
      }
      // if the set contains an odd number of Subspaces, retain the last one
      if(population.size() % 2 == 1) {
        crossover.add(population.get(population.size() - 1));
      }
      // Collections.sort(crossover);
      return crossover;
    }

    /**
     * Recombination method.
     * 
     * @param parent1 First parent
     * @param parent2 Second parent
     * @return recombined children
     */
    private Pair<Individuum, Individuum> recombineOptimized(Individuum parent1, Individuum parent2) {
      Pair<Individuum, Individuum> recombinePair;
      // Set of Positions in which either s1 or s2 are don't care
      ArrayList<Integer> Q = new ArrayList<Integer>(dim);
      // Set of Positions in which neither s1 or s2 is don't care
      ArrayList<Integer> R = new ArrayList<Integer>(dim);

      for(int i = 0; i < dim; i++) {
        if((parent1.getGene()[i] == DONT_CARE) && (parent2.getGene()[i] != DONT_CARE)) {
          Q.add(i);
        }
        if((parent1.getGene()[i] != DONT_CARE) && (parent2.getGene()[i] == DONT_CARE)) {
          Q.add(i);
        }
        if((parent1.getGene()[i] != DONT_CARE) && (parent2.getGene()[i] != DONT_CARE)) {
          R.add(i);
        }
      }

      Individuum best = combineRecursive(R, 0, Individuum.nullIndividuum(dim).getGene(), parent1, parent2);

      // Extends gene greedily
      int[] b = best.getGene();
      int count = k - R.size();
      Iterator<Integer> q = Q.iterator();

      while(count > 0) {
        int[] l1 = b.clone();
        int[] l2 = b.clone();

        while(q.hasNext()) {
          int next = q.next();
          // pos = next;

          {
            boolean s1Null = (parent1.getGene()[next] == 0);
            boolean s2Null = (parent1.getGene()[next] == 0);

            l1[next] = parent1.getGene()[next];
            l2[next] = parent2.getGene()[next];

            final double sparsityL1 = sparsity(computeSubspaceForGene(l1, ranges).size(), dbsize, k);
            final double sparsityL2 = sparsity(computeSubspaceForGene(l2, ranges).size(), dbsize, k);

            if(sparsityL1 <= sparsityL2) {
              b = l1.clone();
              if(s1Null) {
                count--;
              }
            }
            else {
              b = l2.clone();
              if(s2Null) {
                count--;
              }
            }
          }
        }
        // Q.remove(pos);
      }

      // create the complementary String
      int[] comp = new int[dim];

      for(int i = 0; i < dim; i++) {
        if(b[i] == parent1.getGene()[i]) {
          comp[i] = parent2.getGene()[i];
        }
        else {
          comp[i] = parent2.getGene()[i];
        }
      }
      final Individuum i1 = makeIndividuum(b);
      final Individuum i2 = makeIndividuum(comp);
      recombinePair = new Pair<Individuum, Individuum>(i1, i2);

      return recombinePair;
    }

    /**
     * Recursive method to build all possible gene combinations using positions
     * in r.
     * 
     * @param r valid positions to use
     * @param i Offset in r to start at.
     * @param current Current gene
     * @param parent1 First parent
     * @param parent2 Second parent
     * @return best gene combination
     */
    private Individuum combineRecursive(ArrayList<Integer> r, int i, int[] current, Individuum parent1, Individuum parent2) {
      if(i == r.size()) {
        return makeIndividuum(current);
      }
      // Position to modify
      int pos = r.get(i);
      // Build genes
      int[] gene1 = current.clone();
      int[] gene2 = current; // .clone();
      gene1[pos] = parent1.getGene()[pos];
      gene2[pos] = parent2.getGene()[pos];
      Individuum i1 = combineRecursive(r, i + 1, gene1, parent1, parent2);
      Individuum i2 = combineRecursive(r, i + 1, gene2, parent1, parent2);
      // Return the better result.
      if(i1.getFitness() < i2.getFitness()) {
        return i1;
      }
      else {
        return i2;
      }
    }
  }

  /**
   * Individuum for the evolutionary search.
   * 
   * @author Erich Schubert
   */
  private static class Individuum extends FCPair<Double, int[]> {
    /**
     * Constructor
     * 
     * @param fitness Fitness
     * @param gene Gene information
     */
    public Individuum(double fitness, int[] gene) {
      super(fitness, gene);
    }

    /**
     * Get the gene.
     * 
     * @return the gene information
     */
    public int[] getGene() {
      return second;
    }

    /**
     * Get the fitness of this individuum.
     * 
     * @return fitness
     */
    public double getFitness() {
      return first;
    }

    /**
     * Create a "null" individuum (full space).
     * 
     * @param dim Dimensionality
     * @return new individuum
     */
    public static Individuum nullIndividuum(int dim) {
      int[] gene = new int[dim];
      Arrays.fill(gene, DONT_CARE);
      return new Individuum(0.0, gene);
    }

    @Override
    public String toString() {
      return "I(f=" + first + ",g=" + FormatUtil.format(second) + ")";
    }

    @Override
    public boolean equals(Object obj) {
      if(!(obj instanceof Individuum)) {
        return false;
      }
      Individuum other = (Individuum) obj;
      if(other.second.length != this.second.length) {
        return false;
      }
      for(int i = 0; i < this.second.length; i++) {
        if(other.second[i] != this.second[i]) {
          return false;
        }
      }
      return true;
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<?, ?>> extends AbstractAggarwalYuOutlier.Parameterizer {
    protected int m = 0;
    
    protected Long seed = null;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter mP = new IntParameter(M_ID, new GreaterEqualConstraint(2));
      if(config.grab(mP)) {
        m = mP.getValue();
      }
      final LongParameter seedP = new LongParameter(SEED_ID, true);
      if(config.grab(seedP)) {
        seed = seedP.getValue();
      }
    }

    @Override
    protected AggarwalYuEvolutionary<V> makeInstance() {
      return new AggarwalYuEvolutionary<V>(k, phi, m, seed);
    }
  }
}