/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package elki.outlier.subspace;

import java.util.*;

import elki.data.NumberVector;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDs;
import elki.database.relation.DoubleRelation;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.logging.Logging;
import elki.logging.progress.IndefiniteProgress;
import elki.math.DoubleMinMax;
import elki.result.outlier.InvertedOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.datastructures.heap.Heap;
import elki.utilities.datastructures.heap.TopBoundedHeap;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.pairs.Pair;
import elki.utilities.random.RandomFactory;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;

/**
 * Evolutionary variant (EAFOD) of the high-dimensional outlier detection
 * algorithm by Aggarwal and Yu.
 * <p>
 * Reference:
 * <p>
 * Outlier detection for high dimensional data<br>
 * C. C. Aggarwal, P. S. Yu<br>
 * Proc. 2001 ACM SIGMOD international conference on Management of data
 *
 * @author Ahmed Hettab
 * @author Erich Schubert
 * @since 0.4.0
 *
 * @navhas - runs - EvolutionarySearch
 * @navhas - obtains - Individuum
 *
 * @param <V> the type of FeatureVector handled by this Algorithm
 */
@Title("EAFOD: the evolutionary outlier detection algorithm")
@Description("Outlier detection for high dimensional data")
@Reference(authors = "C. C. Aggarwal, P. S. Yu", //
    title = "Outlier detection for high dimensional data", //
    booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD 2001)", //
    url = "https://doi.org/10.1145/375663.375668", //
    bibkey = "DBLP:conf/sigmod/AggarwalY01")
public class AggarwalYuEvolutionary<V extends NumberVector> extends AbstractAggarwalYuOutlier<V> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(AggarwalYuEvolutionary.class);

  /**
   * Maximum iteration count for evolutionary search.
   */
  protected final static int MAX_ITERATIONS = 1000;

  /**
   * At which gene homogenity do we have convergence?
   */
  protected final static double CONVERGENCE = .85;

  /**
   * Holds the value of {@link Par#M_ID}.
   */
  private int m;

  /**
   * Random generator.
   */
  private RandomFactory rnd;

  /**
   * Constructor.
   *
   * @param k K
   * @param phi Phi
   * @param m M
   * @param rnd Random generator
   */
  public AggarwalYuEvolutionary(int k, int phi, int m, RandomFactory rnd) {
    super(k, phi);
    this.m = m;
    this.rnd = rnd;
  }

  /**
   * Performs the evolutionary algorithm on the given database.
   *
   * @param database Database
   * @param relation Relation
   * @return Result
   */
  public OutlierResult run(Database database, Relation<V> relation) {
    final int dbsize = relation.size();
    ArrayList<ArrayList<DBIDs>> ranges = buildRanges(relation);

    Heap<Individuum>.UnorderedIter individuums = (new EvolutionarySearch(relation, ranges, m, rnd.getSingleThreadedRandom())).run();

    WritableDoubleDataStore outlierScore = DataStoreUtil.makeDoubleStorage(relation.getDBIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_STATIC);
    for(; individuums.valid(); individuums.advance()) {
      DBIDs ids = computeSubspaceForGene(individuums.get().getGene(), ranges);
      double sparsityC = sparsity(ids.size(), dbsize, k, phi);
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        double prev = outlierScore.doubleValue(iter);
        if(Double.isNaN(prev) || sparsityC < prev) {
          outlierScore.putDouble(iter, sparsityC);
        }
      }
    }

    DoubleMinMax minmax = new DoubleMinMax();
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      double val = outlierScore.doubleValue(iditer);
      if(Double.isNaN(val)) {
        outlierScore.putDouble(iditer, val = 0.);
      }
      minmax.put(val);
    }
    DoubleRelation scoreResult = new MaterializedDoubleRelation("AggarwalYuEvolutionary", relation.getDBIDs(), outlierScore);
    OutlierScoreMeta meta = new InvertedOutlierScoreMeta(minmax.getMin(), minmax.getMax(), Double.NEGATIVE_INFINITY, 0.0);
    return new OutlierResult(meta, scoreResult);
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * The inner class to handle the actual evolutionary computation.
   *
   * @author Erich Schubert
   *
   * @navhas - evolves - Individuum
   */
  private class EvolutionarySearch {
    /**
     * Database size.
     */
    final int dbsize;

    /**
     * Database dimensionality.
     */
    final int dim;

    /**
     * Database ranges.
     */
    final ArrayList<ArrayList<DBIDs>> ranges;

    /**
     * m to use.
     */
    final int m;

    /**
     * random generator.
     */
    final private Random random;

    /**
     * Constructor.
     *
     * @param relation Database to use
     * @param ranges DBID ranges to process
     * @param m Population size
     * @param random Random generator
     */
    public EvolutionarySearch(Relation<V> relation, ArrayList<ArrayList<DBIDs>> ranges, int m, Random random) {
      super();
      this.ranges = ranges;
      this.m = m;
      this.dbsize = relation.size();
      this.dim = RelationUtil.dimensionality(relation);
      this.random = random;
    }

    public Heap<Individuum>.UnorderedIter run() {
      ArrayList<Individuum> pop = initialPopulation(m);
      // best Population
      TopBoundedHeap<Individuum> bestSol = new TopBoundedHeap<>(m, Collections.reverseOrder());
      for(Individuum ind : pop) {
        bestSol.add(ind);
      }

      IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("Evolutionary search iterations", LOG) : null;
      int iterations = 0;
      while(!checkConvergence(pop)) {
        Collections.sort(pop);
        // Fitter members are more likely to survive
        pop = rouletteRankSelection(pop);
        // Crossover survivors
        pop = crossoverOptimized(pop);
        // Mutation with probability 0.25 , 0.25
        pop = mutation(pop, 0.25, 0.25);
        // Avoid duplicates
        ind: for(Individuum ind : pop) {
          for(Heap<Individuum>.UnorderedIter it = bestSol.unorderedIter(); it.valid(); it.advance()) {
            if(it.get().equals(ind)) {
              continue ind;
            }
          }
          bestSol.add(ind);
        }
        if(LOG.isDebuggingFinest()) {
          StringBuilder buf = new StringBuilder(1000).append("Top solutions:\n");
          for(Heap<Individuum>.UnorderedIter it = bestSol.unorderedIter(); it.valid(); it.advance()) {
            buf.append(it.get().toString()).append('\n');
          }
          buf.append("Population:\n");
          for(Individuum ind : pop) {
            buf.append(ind.toString()).append('\n');
          }
          LOG.debugFinest(buf.toString());
        }
        iterations++;
        LOG.incrementProcessed(prog);
        if(iterations > MAX_ITERATIONS) {
          LOG.warning("Maximum iterations reached.");
          break;
        }
      }
      if(prog != null) {
        prog.setCompleted(LOG);
      }
      return bestSol.unorderedIter();
    }

    /**
     * check the termination criterion.
     *
     * @param pop Population
     * @return Convergence
     */
    private boolean checkConvergence(Collection<Individuum> pop) {
      if(pop.isEmpty()) {
        return true;
      }

      // Gene occurrence counter
      int[][] occur = new int[dim][phi + 1];
      // Count gene occurrences
      for(Individuum ind : pop) {
        short[] gene = ind.getGene();
        for(int d = 0; d < dim; d++) {
          if(gene[d] == DONT_CARE) {
            occur[d][0] += 1;
            continue;
          }
          int val = gene[d] - GENE_OFFSET;
          if(val < 0 || val >= phi) {
            LOG.warning("Invalid gene value encountered: " + val + " in " + ind.toString());
            continue;
          }
          occur[d][val + 1] += 1;
        }
      }

      int conv = (int) Math.floor(pop.size() * CONVERGENCE);
      if(LOG.isDebuggingFine()) {
        LOG.debugFine("Convergence at " + conv + " of " + pop.size() + " individuums.");
      }
      for(int d = 0; d < dim; d++) {
        boolean converged = false;
        for(int val = 0; val <= phi; val++) {
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
      ArrayList<Individuum> population = new ArrayList<>(popsize);
      // fill population
      for(int i = 0; i < popsize; i++) {
        // Random Individual
        short[] gene = new short[dim];
        // fill don't care ( any dimension == don't care)
        Arrays.fill(gene, DONT_CARE);
        // count of don't care positions
        int countDim = k;
        // fill non don't care positions of the Individual
        while(countDim > 0) {
          int z = random.nextInt(dim);
          if(gene[z] != DONT_CARE) {
            continue;
          }
          gene[z] = (short) (random.nextInt(phi) + GENE_OFFSET);
          countDim--;
        }
        population.add(makeIndividuum(gene));
      }
      // Collections.sort(population);
      return population;
    }

    /**
     * Select surviving individuums weighted by rank.
     *
     * the selection criterion for the genetic algorithm: <br>
     * roulette wheel mechanism: <br>
     * where the probability of sampling an individual of the population was
     * proportional to p - r(i), where p is the size of population and r(i) the
     * rank of i-th individual
     *
     * @param population Population
     * @return Survivors
     */
    private ArrayList<Individuum> rouletteRankSelection(ArrayList<Individuum> population) {
      final int popsize = population.size();
      // Relative weight := popsize - position => sum(1..popsize)
      int totalweight = (popsize * (popsize + 1)) >> 1;
      // Survivors
      ArrayList<Individuum> survivors = new ArrayList<>(popsize);

      // position of selection
      for(int i = 0; i < popsize; i++) {
        int z = random.nextInt(totalweight);
        for(int j = 0, rank = popsize; j < popsize; ++j, --rank) {
          if(z < rank) {
            survivors.add(population.get(j));
            break;
          }
          z -= rank;
        }
      }
      assert (survivors.size() == popsize) : "Selection step failed - implementation error?";
      return survivors;
    }

    /**
     * Apply the mutation algorithm.
     */
    private ArrayList<Individuum> mutation(ArrayList<Individuum> population, double perc1, double perc2) {
      // the Mutations
      ArrayList<Individuum> mutations = new ArrayList<>();
      int[] QR = new int[dim];

      // for each individuum
      for(int j = 0; j < population.size(); j++) {
        short[] gene = population.get(j).getGene().clone();
        // Fill position array for mutation process
        int q = 0, r = dim;
        for(int i = 0; i < dim; i++) {
          QR[(gene[i] == DONT_CARE) ? (q++) : (--r)] = i;
        }
        // Mutation variant 1
        if(q > 0 && r < dim && random.nextDouble() <= perc1) {
          // Random mutation spots:
          int rq = random.nextInt(q), rr = random.nextInt(dim - r) + r;
          int pq = QR[rq], pr = QR[rr];
          // Mutate don't care (position pq) into 1....phi
          gene[pq] = (short) (random.nextInt(phi) + GENE_OFFSET);
          // Mutate non don't care (position pr) into don't care
          gene[pr] = DONT_CARE;
          // update sets, by swapping the position vlaues
          QR[rq] = pr;
          QR[rr] = pq;
        }
        // Mutation Variant 2
        if(random.nextDouble() <= perc2) {
          // calc Mutation Spot
          int pr = random.nextInt(dim - r) + r;
          // Mutate 1...phi into another 1...phi
          gene[QR[pr]] = (short) (random.nextInt(phi) + GENE_OFFSET);
        }
        mutations.add(makeIndividuum(gene));
      }
      return mutations;
    }

    /**
     * Make a new individuum helper, computing sparsity=fitness
     *
     * @param gene Gene to evaluate
     * @return new individuum
     */
    private Individuum makeIndividuum(short[] gene) {
      final DBIDs ids = computeSubspaceForGene(gene, ranges);
      final double fitness = (ids.size() > 0) ? sparsity(ids.size(), dbsize, k, phi) : Double.MAX_VALUE;
      return new Individuum(fitness, gene);
    }

    /**
     * method implements the crossover algorithm
     */
    private ArrayList<Individuum> crossoverOptimized(ArrayList<Individuum> population) {
      // Crossover Set of population Set
      ArrayList<Individuum> crossover = new ArrayList<>();

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
      IntArrayList Q = new IntArrayList(dim);
      // Set of Positions in which neither s1 or s2 is don't care
      IntArrayList R = new IntArrayList(dim);

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
      short[] b = best.getGene();
      int count = k - R.size();
      IntIterator q = Q.iterator();

      while(count > 0) {
        short[] l1 = b.clone(), l2 = b.clone();

        while(q.hasNext()) {
          int next = q.nextInt();
          // pos = next;

          boolean s1Null = (parent1.getGene()[next] == DONT_CARE);
          boolean s2Null = (parent1.getGene()[next] == DONT_CARE);

          l1[next] = parent1.getGene()[next];
          l2[next] = parent2.getGene()[next];

          final double sparsityL1 = sparsity(computeSubspaceForGene(l1, ranges).size(), dbsize, k, phi);
          final double sparsityL2 = sparsity(computeSubspaceForGene(l2, ranges).size(), dbsize, k, phi);

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
        // Q.remove(pos);
      }

      // create the complementary String
      short[] comp = new short[dim];

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
      recombinePair = new Pair<>(i1, i2);

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
    private Individuum combineRecursive(IntArrayList r, int i, short[] current, Individuum parent1, Individuum parent2) {
      if(i == r.size()) {
        return makeIndividuum(current);
      }
      // Position to modify
      int pos = r.getInt(i);
      // Build genes
      short[] gene1 = current.clone();
      short[] gene2 = current; // .clone();
      gene1[pos] = parent1.getGene()[pos];
      gene2[pos] = parent2.getGene()[pos];
      Individuum i1 = combineRecursive(r, i + 1, gene1, parent1, parent2);
      Individuum i2 = combineRecursive(r, i + 1, gene2, parent1, parent2);
      // Return the better result.
      return (i1.getFitness() < i2.getFitness()) ? i1 : i2;
    }
  }

  /**
   * Individuum for the evolutionary search.
   *
   * @author Erich Schubert
   */
  private static class Individuum implements Comparable<Individuum> {
    double fitness;

    short[] gene;

    /**
     * Constructor
     *
     * @param fitness Fitness
     * @param gene Gene information
     */
    public Individuum(double fitness, short[] gene) {
      this.fitness = fitness;
      this.gene = gene;
    }

    /**
     * Get the gene.
     *
     * @return the gene information
     */
    public short[] getGene() {
      return gene;
    }

    /**
     * Get the fitness of this individuum.
     *
     * @return fitness
     */
    public double getFitness() {
      return fitness;
    }

    /**
     * Create a "null" individuum (full space).
     *
     * @param dim Dimensionality
     * @return new individuum
     */
    public static Individuum nullIndividuum(int dim) {
      short[] gene = new short[dim];
      Arrays.fill(gene, DONT_CARE);
      return new Individuum(0.0, gene);
    }

    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(200).append("I(f=").append(fitness).append(",g=");
      for(int i = 0; i < gene.length; i++) {
        if(i > 0) {
          buf.append(',');
        }
        if(gene[i] == DONT_CARE) {
          buf.append('*');
        }
        else {
          buf.append(gene[i]);
        }
      }
      buf.append(')');
      return buf.toString();
    }

    @Override
    public boolean equals(Object obj) {
      if(!(obj instanceof Individuum)) {
        return false;
      }
      Individuum other = (Individuum) obj;
      if(other.gene.length != this.gene.length) {
        return false;
      }
      for(int i = 0; i < this.gene.length; i++) {
        if(other.gene[i] != this.gene[i]) {
          return false;
        }
      }
      return true;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(this);
    }

    @Override
    public int compareTo(Individuum o) {
      return Double.compare(this.fitness, o.fitness);
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<V extends NumberVector> extends AbstractAggarwalYuOutlier.Par {
    /**
     * Parameter to specify the number of solutions must be an integer greater
     * than 1.
     */
    public static final OptionID M_ID = new OptionID("ay.m", "Population size for evolutionary algorithm.");

    /**
     * Parameter to specify the random generator seed.
     */
    public static final OptionID SEED_ID = new OptionID("ay.seed", "The random number generator seed.");

    protected int m = 0;

    protected RandomFactory rnd;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new IntParameter(M_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ONE_INT) //
          .grab(config, x -> m = x);
      new RandomParameter(SEED_ID).grab(config, x -> rnd = x);
    }

    @Override
    public AggarwalYuEvolutionary<V> make() {
      return new AggarwalYuEvolutionary<>(k, phi, m, rnd);
    }
  }
}
