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
package elki.clustering.subspace;

import java.util.*;

import elki.clustering.AbstractProjectedClustering;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.Subspace;
import elki.data.model.SubspaceModel;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.*;
import elki.database.ids.*;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.query.range.RangeSearcher;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.IndefiniteProgress;
import elki.logging.progress.StepProgress;
import elki.math.Mean;
import elki.math.linearalgebra.Centroid;
import elki.result.Metadata;
import elki.utilities.datastructures.BitsUtil;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.io.FormatUtil;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.RandomParameter;
import elki.utilities.pairs.Pair;
import elki.utilities.random.RandomFactory;

/**
 * The PROCLUS algorithm, an algorithm to find subspace clusters in high
 * dimensional spaces.
 * <p>
 * Reference:
 * <p>
 * C. C. Aggarwal, C. Procopiuc, J. L. Wolf, P. S. Yu, J. S. Park<br>
 * Fast Algorithms for Projected Clustering<br>
 * Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99).
 *
 * @author Elke Achtert
 * @since 0.1
 *
 * @navassoc - - - SubspaceModel
 * @has - - - PROCLUSCluster
 */
@Title("PROCLUS: PROjected CLUStering")
@Description("Algorithm to find subspace clusters in high dimensional spaces.")
@Reference(authors = "C. C. Aggarwal, C. Procopiuc, J. L. Wolf, P. S. Yu, J. S. Park", //
    title = "Fast Algorithms for Projected Clustering", //
    booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99)", //
    url = "https://doi.org/10.1145/304181.304188", //
    bibkey = "doi:10.1145/304181.304188")
public class PROCLUS extends AbstractProjectedClustering<Clustering<SubspaceModel>> implements SubspaceClusteringAlgorithm<SubspaceModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(PROCLUS.class);

  /**
   * Multiplier for the initial number of medoids.
   */
  private int m_i;

  /**
   * Random generator
   */
  private RandomFactory rnd;

  /**
   * Java constructor.
   *
   * @param k k Parameter
   * @param k_i k_i Parameter
   * @param l l Parameter
   * @param m_i m_i Parameter
   * @param rnd Random generator
   */
  public PROCLUS(int k, int k_i, int l, int m_i, RandomFactory rnd) {
    super(k, k_i, l);
    this.m_i = m_i;
    this.rnd = rnd;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Performs the PROCLUS algorithm on the given database.
   *
   * @param relation Relation to process
   */
  public <V extends NumberVector> Clustering<SubspaceModel> run(Relation<V> relation) {
    if(RelationUtil.dimensionality(relation) < l) {
      throw new IllegalStateException("Dimensionality of data < parameter l! (" + RelationUtil.dimensionality(relation) + " < " + l + ")");
    }
    final QueryBuilder<V> qb = new QueryBuilder<>(relation, SquaredEuclideanDistance.STATIC);
    DistanceQuery<V> distFunc = qb.distanceQuery();
    RangeSearcher<DBIDRef> rangeQuery = qb.rangeByDBID();
    final Random random = rnd.getSingleThreadedRandom();

    StepProgress stepp = new StepProgress(3);
    // initialization phase
    stepp.beginStep(1, "Initialization phase", LOG);
    int sampleSize = Math.min(relation.size(), k_i * k);
    DBIDs sampleSet = DBIDUtil.randomSample(relation.getDBIDs(), sampleSize, random);

    int medoidSize = Math.min(relation.size(), m_i * k);
    ArrayDBIDs medoids = greedy(distFunc, sampleSet, medoidSize, random);

    if(LOG.isDebugging()) {
      LOG.debugFine(new StringBuilder().append("sampleSize ").append(sampleSize).append('\n') //
          .append("sampleSet ").append(sampleSet).append('\n') //
          .append("medoidSize ").append(medoidSize).append('\n') //
          .append("m ").append(medoids).toString());
    }

    // iterative phase
    stepp.beginStep(2, "Iterative phase", LOG);
    double bestObjective = Double.POSITIVE_INFINITY;
    ArrayDBIDs m_best = null;
    DBIDs m_bad = null;
    ArrayDBIDs m_current = initialSet(medoids, k, random);

    if(LOG.isDebugging()) {
      LOG.debugFine(new StringBuilder().append("m_c ").append(m_current).toString());
    }

    IndefiniteProgress cprogress = LOG.isVerbose() ? new IndefiniteProgress("Current number of clusters:", LOG) : null;

    ArrayList<PROCLUSCluster> clusters = null;
    int loops = 0;
    while(loops < 10) {
      long[][] dimensions = findDimensions(m_current, relation, distFunc, rangeQuery);
      clusters = assignPoints(m_current, dimensions, relation);
      double objectiveFunction = evaluateClusters(clusters, dimensions, relation);

      if(objectiveFunction < bestObjective) {
        // restart counting loops
        loops = 0;
        bestObjective = objectiveFunction;
        m_best = m_current;
        m_bad = computeBadMedoids(m_current, clusters, (int) (relation.size() * 0.1 / k));
      }

      m_current = computeM_current(medoids, m_best, m_bad, random);
      loops++;
      if(cprogress != null) {
        cprogress.setProcessed(clusters.size(), LOG);
      }
    }

    LOG.setCompleted(cprogress);

    // refinement phase
    stepp.beginStep(3, "Refinement phase", LOG);

    List<Pair<double[], long[]>> dimensions = findDimensions(clusters, relation);
    List<PROCLUSCluster> finalClusters = finalAssignment(dimensions, relation);

    stepp.setCompleted(LOG);

    // build result
    int numClusters = 1;
    Clustering<SubspaceModel> result = new Clustering<>();
    Metadata.of(result).setLongName("ProClus Clustering");
    for(PROCLUSCluster c : finalClusters) {
      Cluster<SubspaceModel> cluster = new Cluster<>(c.objectIDs);
      cluster.setModel(new SubspaceModel(new Subspace(c.getDimensions()), c.centroid));
      cluster.setName("cluster_" + numClusters++);
      result.addToplevelCluster(cluster);
    }
    return result;
  }

  /**
   * Returns a piercing set of k medoids from the specified sample set.
   *
   * @param distance the distance function
   * @param sampleSet the sample set
   * @param m the number of medoids to be returned
   * @param random random number generator
   * @return a piercing set of m medoids from the specified sample set
   */
  private ArrayDBIDs greedy(DistanceQuery<? extends NumberVector> distance, DBIDs sampleSet, int m, Random random) {
    ArrayModifiableDBIDs medoids = DBIDUtil.newArray(m);

    ArrayModifiableDBIDs s = DBIDUtil.newArray(sampleSet);
    DBIDArrayIter iter = s.iter();
    DBIDVar mi = DBIDUtil.newVar();
    int size = s.size();

    // Move a random element to the end, then pop()
    s.swap(random.nextInt(size), --size);
    medoids.add(s.pop(mi));
    if(LOG.isDebugging()) {
      LOG.debugFiner("medoids " + medoids.toString());
    }

    // To track the current worst element:
    int worst = -1;
    double worstd = Double.NEGATIVE_INFINITY;

    // compute distances between each point in S and m_i
    WritableDoubleDataStore distances = DataStoreUtil.makeDoubleStorage(s, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
    for(iter.seek(0); iter.getOffset() < size; iter.advance()) {
      final double dist = distance.distance(iter, mi);
      distances.putDouble(iter, dist);
      if(dist > worstd) {
        worstd = dist;
        worst = iter.getOffset();
      }
    }

    for(int i = 1; i < m; i++) {
      // choose medoid m_i to be far from previous medoids
      s.swap(worst, --size);
      medoids.add(s.pop(mi));

      // compute distances of each point to closest medoid; track worst.
      worst = -1;
      worstd = Double.NEGATIVE_INFINITY;
      for(iter.seek(0); iter.getOffset() < size; iter.advance()) {
        double dist_new = distance.distance(iter, mi);
        double dist_old = distances.doubleValue(iter);
        double dist = (dist_new < dist_old) ? dist_new : dist_old;
        distances.putDouble(iter, dist);
        if(dist > worstd) {
          worstd = dist;
          worst = iter.getOffset();
        }
      }

      if(LOG.isDebugging()) {
        LOG.debugFiner("medoids " + medoids.toString());
      }
    }

    return medoids;
  }

  /**
   * Returns a set of k elements from the specified sample set.
   *
   * @param sampleSet the sample set
   * @param k the number of samples to be returned
   * @param random random number generator
   * @return a set of k elements from the specified sample set
   */
  private ArrayDBIDs initialSet(DBIDs sampleSet, int k, Random random) {
    return DBIDUtil.ensureArray(DBIDUtil.randomSample(sampleSet, k, random));
  }

  /**
   * Computes the set of medoids in current iteration.
   *
   * @param m the medoids
   * @param m_best the best set of medoids found so far
   * @param m_bad the bad medoids
   * @param random random number generator
   * @return m_current, the set of medoids in current iteration
   */
  private ArrayDBIDs computeM_current(DBIDs m, DBIDs m_best, DBIDs m_bad, Random random) {
    ArrayModifiableDBIDs m_list = DBIDUtil.newArray(m);
    m_list.removeDBIDs(m_best);
    DBIDArrayMIter it = m_list.iter();

    ArrayModifiableDBIDs m_current = DBIDUtil.newArray();
    for(DBIDIter iter = m_best.iter(); iter.valid(); iter.advance()) {
      if(m_bad.contains(iter)) {
        int currentSize = m_current.size();
        while(m_current.size() == currentSize) {
          m_current.add(it.seek(random.nextInt(m_list.size())));
          it.remove();
        }
      }
      else {
        m_current.add(iter);
      }
    }
    return m_current;
  }

  /**
   * Computes the localities of the specified medoids: for each medoid m the
   * objects in the sphere centered at m with radius minDist are determined,
   * where minDist is the minimum distance between medoid m and any other medoid
   * m_i.
   *
   * @param medoids the ids of the medoids
   * @param distance the distance function
   * @return a mapping of the medoid's id to its locality
   */
  private DataStore<DBIDs> getLocalities(DBIDs medoids, DistanceQuery<? extends NumberVector> distance, RangeSearcher<DBIDRef> rangeQuery) {
    WritableDataStore<DBIDs> result = DataStoreUtil.makeStorage(medoids, DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, DBIDs.class);

    for(DBIDIter iter = medoids.iter(); iter.valid(); iter.advance()) {
      // determine minimum distance between current medoid m and any other
      // medoid m_i
      double minDist = Double.POSITIVE_INFINITY;
      for(DBIDIter iter2 = medoids.iter(); iter2.valid(); iter2.advance()) {
        if(DBIDUtil.equal(iter, iter2)) {
          continue;
        }
        double currentDist = distance.distance(iter, iter2);
        if(currentDist < minDist) {
          minDist = currentDist;
        }
      }

      // determine points in sphere centered at m with radius minDist
      assert minDist != Double.POSITIVE_INFINITY;
      result.put(iter, rangeQuery.getRange(iter, minDist));
    }

    return result;
  }

  /**
   * Determines the set of correlated dimensions for each medoid in the
   * specified medoid set.
   *
   * @param medoids the set of medoids
   * @param relation the relation containing the objects
   * @param distance the distance function
   * @return the set of correlated dimensions for each medoid in the specified
   *         medoid set
   */
  private long[][] findDimensions(ArrayDBIDs medoids, Relation<? extends NumberVector> relation, DistanceQuery<? extends NumberVector> distance, RangeSearcher<DBIDRef> rangeQuery) {
    // get localities
    DataStore<DBIDs> localities = getLocalities(medoids, distance, rangeQuery);

    // compute x_ij = avg distance from points in l_i to medoid m_i
    final int dim = RelationUtil.dimensionality(relation);
    final int numc = medoids.size();
    double[][] averageDistances = new double[numc][];

    for(DBIDArrayIter iter = medoids.iter(); iter.valid(); iter.advance()) {
      NumberVector medoid_i = relation.get(iter);
      DBIDs l_i = localities.get(iter);
      double[] x_i = new double[dim];
      for(DBIDIter qr = l_i.iter(); qr.valid(); qr.advance()) {
        NumberVector o = relation.get(qr);
        for(int d = 0; d < dim; d++) {
          x_i[d] += Math.abs(medoid_i.doubleValue(d) - o.doubleValue(d));
        }
      }
      for(int d = 0; d < dim; d++) {
        x_i[d] /= l_i.size();
      }
      averageDistances[iter.getOffset()] = x_i;
    }

    return computeDimensionMap(computeZijs(averageDistances, dim), dim, numc);
  }

  /**
   * Refinement step that determines the set of correlated dimensions for each
   * cluster centroid.
   *
   * @param clusters the list of clusters
   * @param database the database containing the objects
   * @return the set of correlated dimensions for each specified cluster
   *         centroid
   */
  private List<Pair<double[], long[]>> findDimensions(ArrayList<PROCLUSCluster> clusters, Relation<? extends NumberVector> database) {
    // compute x_ij = avg distance from points in c_i to c_i.centroid
    final int dim = RelationUtil.dimensionality(database);
    final int numc = clusters.size();
    double[][] averageDistances = new double[numc][];

    for(int i = 0; i < numc; i++) {
      PROCLUSCluster c_i = clusters.get(i);
      double[] x_i = new double[dim];
      for(DBIDIter iter = c_i.objectIDs.iter(); iter.valid(); iter.advance()) {
        NumberVector o = database.get(iter);
        for(int d = 0; d < dim; d++) {
          x_i[d] += Math.abs(c_i.centroid[d] - o.doubleValue(d));
        }
      }
      for(int d = 0; d < dim; d++) {
        x_i[d] /= c_i.objectIDs.size();
      }
      averageDistances[i] = x_i;
    }

    List<DoubleIntInt> z_ijs = computeZijs(averageDistances, dim);
    long[][] dimensionMap = computeDimensionMap(z_ijs, dim, numc);

    // mapping cluster -> dimensions
    List<Pair<double[], long[]>> result = new ArrayList<>(numc);
    for(int i = 0; i < numc; i++) {
      long[] dims_i = dimensionMap[i];
      if(dims_i == null) {
        continue;
      }
      result.add(new Pair<>(clusters.get(i).centroid, dims_i));
    }
    return result;
  }

  /**
   * Compute the z_ij values.
   *
   * @param averageDistances Average distances
   * @param dim Dimensions
   * @return z_ij values
   */
  private List<DoubleIntInt> computeZijs(double[][] averageDistances, final int dim) {
    List<DoubleIntInt> z_ijs = new ArrayList<>(averageDistances.length * dim);
    for(int i = 0; i < averageDistances.length; i++) {
      double[] x_i = averageDistances[i];
      // y_i
      double y_i = 0;
      for(int j = 0; j < dim; j++) {
        y_i += x_i[j];
      }
      y_i /= dim;

      // sigma_i
      double sigma_i = 0;
      for(int j = 0; j < dim; j++) {
        double diff = x_i[j] - y_i;
        sigma_i += diff * diff;
      }
      sigma_i /= (dim - 1);
      sigma_i = Math.sqrt(sigma_i);

      for(int j = 0; j < dim; j++) {
        z_ijs.add(new DoubleIntInt((x_i[j] - y_i) / sigma_i, i, j));
      }
    }
    Collections.sort(z_ijs);
    return z_ijs;
  }

  /**
   * Compute the dimension map.
   *
   * @param z_ijs z_ij values
   * @param dim Number of dimensions
   * @param numc Number of clusters
   * @return Bitmap of dimensions used
   */
  private long[][] computeDimensionMap(List<DoubleIntInt> z_ijs, final int dim, final int numc) {
    // mapping cluster index -> dimensions
    long[][] dimensionMap = new long[numc][((dim - 1) >> 6) + 1];
    int max = Math.max(k * l, 2);
    for(int m = 0; m < max; m++) {
      DoubleIntInt z_ij = z_ijs.get(m);
      long[] dims_i = dimensionMap[z_ij.dimi];
      BitsUtil.setI(dims_i, z_ij.dimj);

      if(LOG.isDebugging()) {
        LOG.debugFiner(new StringBuilder().append("z_ij ").append(z_ij).append('\n') //
            .append("D_i ").append(BitsUtil.toString(dims_i)).toString());
      }
    }
    return dimensionMap;
  }

  /**
   * Assigns the objects to the clusters.
   *
   * @param m_current Current centers
   * @param dimensions set of correlated dimensions for each medoid of the
   *        cluster
   * @param database the database containing the objects
   * @return the assignments of the object to the clusters
   */
  private ArrayList<PROCLUSCluster> assignPoints(ArrayDBIDs m_current, long[][] dimensions, Relation<? extends NumberVector> database) {
    ModifiableDBIDs[] clusterIDs = new ModifiableDBIDs[dimensions.length];
    for(int i = 0; i < m_current.size(); i++) {
      clusterIDs[i] = DBIDUtil.newHashSet();
    }

    DBIDArrayIter mi = m_current.iter();
    for(DBIDIter it = database.iterDBIDs(); it.valid(); it.advance()) {
      NumberVector p = database.get(it);
      double minDist = Double.NaN;
      int best = -1, i = 0;
      for(mi.seek(0); mi.valid(); mi.advance(), i++) {
        NumberVector m = database.get(mi);
        double currentDist = manhattanSegmentalDistance(p, m, dimensions[i]);
        if(!(minDist <= currentDist)) {
          minDist = currentDist;
          best = i;
        }
      }
      // add p to cluster with mindist
      assert best >= 0;
      clusterIDs[best].add(it);
    }

    ArrayList<PROCLUSCluster> clusters = new ArrayList<>(m_current.size());
    for(int i = 0; i < dimensions.length; i++) {
      ModifiableDBIDs objectIDs = clusterIDs[i];
      if(!objectIDs.isEmpty()) {
        long[] clusterDimensions = dimensions[i];
        double[] centroid = Centroid.make(database, objectIDs).getArrayRef();
        clusters.add(new PROCLUSCluster(objectIDs, clusterDimensions, centroid));
      }
      else {
        clusters.add(null);
      }
    }

    if(LOG.isDebugging()) {
      LOG.debugFine(new StringBuilder().append("clusters ").append(clusters).toString());
    }
    return clusters;
  }

  /**
   * Refinement step to assign the objects to the final clusters.
   *
   * @param dimensions pair containing the centroid and the set of correlated
   *        dimensions for the centroid
   * @param database the database containing the objects
   * @return the assignments of the object to the clusters
   */
  private List<PROCLUSCluster> finalAssignment(List<Pair<double[], long[]>> dimensions, Relation<? extends NumberVector> database) {
    Map<Integer, ModifiableDBIDs> clusterIDs = new HashMap<>();
    for(int i = 0; i < dimensions.size(); i++) {
      clusterIDs.put(i, DBIDUtil.newHashSet());
    }

    for(DBIDIter it = database.iterDBIDs(); it.valid(); it.advance()) {
      NumberVector p = database.get(it);
      double minDist = Double.POSITIVE_INFINITY;
      int best = -1;
      for(int i = 0; i < dimensions.size(); i++) {
        Pair<double[], long[]> pair_i = dimensions.get(i);
        double currentDist = manhattanSegmentalDistance(p, pair_i.first, pair_i.second);
        if(best < 0 || currentDist < minDist) {
          minDist = currentDist;
          best = i;
        }
      }
      // add p to cluster with mindist
      assert minDist >= 0.;
      clusterIDs.get(best).add(it);
    }

    List<PROCLUSCluster> clusters = new ArrayList<>();
    for(int i = 0; i < dimensions.size(); i++) {
      ModifiableDBIDs objectIDs = clusterIDs.get(i);
      if(!objectIDs.isEmpty()) {
        long[] clusterDimensions = dimensions.get(i).second;
        double[] centroid = Centroid.make(database, objectIDs).getArrayRef();
        clusters.add(new PROCLUSCluster(objectIDs, clusterDimensions, centroid));
      }
    }

    if(LOG.isDebugging()) {
      LOG.debugFine(new StringBuilder().append("clusters ").append(clusters).toString());
    }
    return clusters;
  }

  /**
   * Returns the Manhattan segmental distance between o1 and o2 relative to the
   * specified dimensions.
   *
   * @param o1 the first object
   * @param o2 the second object
   * @param dimensions the dimensions to be considered
   * @return the Manhattan segmental distance between o1 and o2 relative to the
   *         specified dimensions
   */
  private double manhattanSegmentalDistance(NumberVector o1, NumberVector o2, long[] dimensions) {
    double result = 0;
    int card = 0;
    for(int d = BitsUtil.nextSetBit(dimensions, 0); d >= 0; d = BitsUtil.nextSetBit(dimensions, d + 1)) {
      result += Math.abs(o1.doubleValue(d) - o2.doubleValue(d));
      ++card;
    }
    return result / card;
  }

  /**
   * Returns the Manhattan segmental distance between o1 and o2 relative to the
   * specified dimensions.
   *
   * @param o1 the first object
   * @param o2 the second object
   * @param dimensions the dimensions to be considered
   * @return the Manhattan segmental distance between o1 and o2 relative to the
   *         specified dimensions
   */
  private double manhattanSegmentalDistance(NumberVector o1, double[] o2, long[] dimensions) {
    double result = 0;
    int card = 0;
    for(int d = BitsUtil.nextSetBit(dimensions, 0); d >= 0; d = BitsUtil.nextSetBit(dimensions, d + 1)) {
      result += Math.abs(o1.doubleValue(d) - o2[d]);
      ++card;
    }
    return result / card;
  }

  /**
   * Evaluates the quality of the clusters.
   *
   * @param clusters the clusters to be evaluated
   * @param dimensions the dimensions associated with each cluster
   * @param database the database holding the objects
   * @return a measure for the cluster quality
   */
  private double evaluateClusters(ArrayList<PROCLUSCluster> clusters, long[][] dimensions, Relation<? extends NumberVector> database) {
    double result = 0;
    for(int i = 0; i < dimensions.length; i++) {
      PROCLUSCluster c_i = clusters.get(i);
      double[] centroid_i = c_i.centroid;

      long[] dims_i = dimensions[i];
      double w_i = 0;
      for(int d = BitsUtil.nextSetBit(dims_i, 0); d >= 0; d = BitsUtil.nextSetBit(dims_i, d + 1)) {
        w_i += avgDistance(centroid_i, c_i.objectIDs, database, d);
      }

      w_i /= dimensions.length;
      result += c_i.objectIDs.size() * w_i;
    }

    return result / database.size();
  }

  /**
   * Computes the average distance of the objects to the centroid along the
   * specified dimension.
   *
   * @param centroid the centroid
   * @param objectIDs the set of objects ids
   * @param database the database holding the objects
   * @param dimension the dimension for which the average distance is computed
   * @return the average distance of the objects to the centroid along the
   *         specified dimension
   */
  private double avgDistance(double[] centroid, DBIDs objectIDs, Relation<? extends NumberVector> database, int dimension) {
    Mean avg = new Mean();
    for(DBIDIter iter = objectIDs.iter(); iter.valid(); iter.advance()) {
      avg.put(Math.abs(centroid[dimension] - database.get(iter).doubleValue(dimension)));
    }
    return avg.getMean();
  }

  /**
   * Computes the bad medoids, where the medoid of a cluster with less than the
   * specified threshold of objects is bad.
   *
   * @param m_current Current medoids
   * @param clusters the clusters
   * @param threshold the threshold
   * @return the bad medoids
   */
  private DBIDs computeBadMedoids(ArrayDBIDs m_current, ArrayList<PROCLUSCluster> clusters, int threshold) {
    ModifiableDBIDs badMedoids = DBIDUtil.newHashSet(m_current.size());
    int i = 0;
    for(DBIDIter it = m_current.iter(); it.valid(); it.advance(), i++) {
      PROCLUSCluster c_i = clusters.get(i);
      if(c_i == null || c_i.objectIDs.size() < threshold) {
        badMedoids.add(it);
      }
    }
    return badMedoids;
  }

  /**
   * Simple triple.
   *
   * @author Erich Schubert
   */
  private static class DoubleIntInt implements Comparable<DoubleIntInt> {
    protected double first;

    protected int dimi, dimj;

    public DoubleIntInt(double first, int second, int third) {
      this.first = first;
      this.dimi = second;
      this.dimj = third;
    }

    @Override
    public int compareTo(DoubleIntInt o) {
      return this.first < o.first ? -1 : this.first > o.first ? 1 : 0;
    }
  }

  /**
   * Encapsulates the attributes of a cluster.
   */
  private static class PROCLUSCluster {
    /**
     * The ids of the objects belonging to this cluster.
     */
    ModifiableDBIDs objectIDs;

    /**
     * The correlated dimensions of this cluster.
     */
    long[] dimensions;

    /**
     * The centroids of this cluster along each dimension.
     */
    double[] centroid;

    /**
     * Constructor.
     *
     * @param objectIDs the ids of the objects belonging to this cluster
     * @param dimensions the correlated dimensions of this cluster
     * @param centroid the centroid of this cluster
     */
    public PROCLUSCluster(ModifiableDBIDs objectIDs, long[] dimensions, double[] centroid) {
      this.objectIDs = objectIDs;
      this.dimensions = dimensions;
      this.centroid = centroid;
    }

    @Override
    public String toString() {
      StringBuilder result = new StringBuilder(500).append("Dimensions: [");
      boolean notFirst = false;
      for(int d = BitsUtil.nextSetBit(dimensions, 0); d >= 0; d = BitsUtil.nextSetBit(dimensions, d + 1)) {
        if(notFirst) {
          result.append(',');
        }
        notFirst = true;
        result.append(d);
      }
      return FormatUtil.formatTo(result.append("]\nCentroid: "), centroid, ",").toString();
    }

    /**
     * Returns the correlated dimensions of this cluster as BitSet.
     *
     * @return the correlated dimensions of this cluster as BitSet
     */
    public long[] getDimensions() {
      return dimensions;
    }
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par extends AbstractProjectedClustering.Par {
    /**
     * Parameter to specify the multiplier for the initial number of medoids,
     * must be an integer greater than 0.
     */
    public static final OptionID M_I_ID = new OptionID("proclus.mi", "The multiplier for the initial number of medoids.");

    /**
     * Parameter to specify the random generator seed.
     */
    public static final OptionID SEED_ID = new OptionID("proclus.seed", "The random number generator seed.");

    /**
     * Multiplier for the initial number of medoids.
     */
    protected int m_i = -1;

    /**
     * Random generator
     */
    protected RandomFactory rnd;

    @Override
    public void configure(Parameterization config) {
      super.configure(config);
      new IntParameter(K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
      new IntParameter(K_I_ID, 30) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k_i = x);
      new IntParameter(L_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> l = x);
      new IntParameter(M_I_ID, 10) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> m_i = x);
      new RandomParameter(SEED_ID).grab(config, x -> rnd = x);
    }

    @Override
    public PROCLUS make() {
      return new PROCLUS(k, k_i, l, m_i, rnd);
    }
  }
}
