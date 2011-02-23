package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import de.lmu.ifi.dbs.elki.algorithm.clustering.AbstractProjectedClustering;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.Subspace;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.model.SubspaceModel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.IndefiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.LongParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.CTriple;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * <p/>
 * Provides the PROCLUS algorithm, an algorithm to find subspace clusters in
 * high dimensional spaces.
 * </p>
 * <p/>
 * Reference: <br>
 * C. C. Aggrawal, C. Procopiuc, J. L. Wolf, P. S. Yu, J. S. Park: Fast
 * Algorithms for Projected Clustering. <br>
 * In: Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99).
 * </p>
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has SubspaceModel
 * 
 * @param <V> the type of NumberVector handled by this Algorithm
 */
@Title("PROCLUS: PROjected CLUStering")
@Description("Algorithm to find subspace clusters in high dimensional spaces.")
@Reference(authors = "C. C. Aggrawal, C. Procopiuc, J. L. Wolf, P. S. Yu, J. S. Park", title = "Fast Algorithms for Projected Clustering", booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99)", url = "http://dx.doi.org/10.1145/304181.304188")
// TODO: make the generics reflect the SubspaceModel
public class PROCLUS<V extends NumberVector<V, ?>> extends AbstractProjectedClustering<V> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(PROCLUS.class);

  /**
   * Parameter to specify the multiplier for the initial number of medoids, must
   * be an integer greater than 0.
   * <p>
   * Default value: {@code 10}
   * </p>
   * <p>
   * Key: {@code -proclus.mi}
   * </p>
   */
  public static final OptionID M_I_ID = OptionID.getOrCreateOptionID("proclus.mi", "The multiplier for the initial number of medoids.");

  /**
   * Holds the value of {@link #M_I_ID}.
   */
  private int m_i;

  /**
   * Parameter to specify the random generator seed.
   */
  public static final OptionID SEED_ID = OptionID.getOrCreateOptionID("proclus.seed", "The random number generator seed.");

  /**
   * Holds the value of {@link #SEED_ID}.
   */
  private Long seed;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public PROCLUS(Parameterization config) {
    super(config);
    config = config.descend(this);
    final IntParameter m_i_param = new IntParameter(M_I_ID, new GreaterConstraint(0), 10);
    if(config.grab(m_i_param)) {
      m_i = m_i_param.getValue();
    }
    final LongParameter seedparam = new LongParameter(SEED_ID, true);
    if(config.grab(seedparam)) {
      seed = seedparam.getValue();
    }
  }

  /**
   * Performs the PROCLUS algorithm on the given database.
   * 
   */
  @Override
  protected Clustering<Model> runInTime(Database<V> database) throws IllegalStateException {
    try {
      DistanceQuery<V, DoubleDistance> distFunc = this.getDistanceQuery(database);
      RangeQuery<V, DoubleDistance> rangeQuery = database.getRangeQuery(distFunc);
      final int dim = getL();
      final int k = getK();
      final int k_i = getK_i();
      final Random random = new Random();
      if (seed != null) {
        random.setSeed(seed);
      }

      if(DatabaseUtil.dimensionality(database) < dim) {
        throw new IllegalStateException("Dimensionality of data < parameter l! " + "(" + DatabaseUtil.dimensionality(database) + " < " + dim + ")");
      }

      // TODO: use a StepProgress!
      // initialization phase
      if(logger.isVerbose()) {
        logger.verbose("1. Initialization phase...");
      }
      int sampleSize = Math.min(database.size(), k_i * k);
      DBIDs sampleSet = database.randomSample(sampleSize, random.nextLong());

      int medoidSize = Math.min(database.size(), m_i * k);
      DBIDs medoids = greedy(distFunc, sampleSet, medoidSize, random);

      if(logger.isDebugging()) {
        StringBuffer msg = new StringBuffer();
        msg.append("\n");
        msg.append("sampleSize ").append(sampleSize).append("\n");
        msg.append("sampleSet ").append(sampleSet).append("\n");
        msg.append("medoidSize ").append(medoidSize).append("\n");
        msg.append("m ").append(medoids).append("\n");
        logger.debugFine(msg.toString());
      }

      // iterative phase
      if(logger.isVerbose()) {
        logger.verbose("2. Iterative phase...");
      }
      double bestObjective = Double.POSITIVE_INFINITY;
      ModifiableDBIDs m_best = null;
      ModifiableDBIDs m_bad = null;
      ModifiableDBIDs m_current = initialSet(medoids, k, random);

      if(logger.isDebugging()) {
        StringBuffer msg = new StringBuffer();
        msg.append("\n");
        msg.append("m_c ").append(m_current).append("\n");
        logger.debugFine(msg.toString());
      }

      IndefiniteProgress cprogress = logger.isVerbose() ? new IndefiniteProgress("Current number of clusters:", logger) : null;

      Map<DBID, PROCLUSCluster> clusters = null;
      int loops = 0;
      while(loops < 10) {
        Map<DBID, Set<Integer>> dimensions = findDimensions(m_current, database, distFunc, rangeQuery);
        clusters = assignPoints(dimensions, database);
        double objectiveFunction = evaluateClusters(clusters, dimensions, database);

        if(objectiveFunction < bestObjective) {
          // restart counting loops
          loops = 0;
          bestObjective = objectiveFunction;
          m_best = m_current;
          m_bad = computeBadMedoids(clusters, (int) (database.size() * 0.1 / getK()));
        }

        m_current = computeM_current(medoids, m_best, m_bad, random);
        loops++;
        if(cprogress != null) {
          cprogress.setProcessed(clusters.size(), logger);
        }
      }

      if(cprogress != null) {
        cprogress.setCompleted(logger);
      }

      // refinement phase
      if(logger.isVerbose()) {
        logger.verbose("3. Refinement phase...");
      }

      List<Pair<V, Set<Integer>>> dimensions = findDimensions(new ArrayList<PROCLUSCluster>(clusters.values()), database);
      List<PROCLUSCluster> finalClusters = finalAssignment(dimensions, database);

      // build result
      int numClusters = 1;
      Clustering<Model> result = new Clustering<Model>("ProClus clustering", "proclus-clustering");
      for(PROCLUSCluster c : finalClusters) {
        Cluster<Model> cluster = new Cluster<Model>(c.objectIDs);
        cluster.setModel(new SubspaceModel<V>(new Subspace<V>(c.getDimensions()), c.centroid));
        cluster.setName("cluster_" + numClusters++);

        result.addCluster(cluster);
      }
      return result;
    }
    catch(Exception e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  /**
   * Returns a piercing set of k medoids from the specified sample set.
   * 
   * @param distFunc the distance function
   * @param sampleSet the sample set
   * @param m the number of medoids to be returned
   * @param random random number generator
   * @return a piercing set of m medoids from the specified sample set
   */
  private ModifiableDBIDs greedy(DistanceQuery<V, DoubleDistance> distFunc, DBIDs sampleSet, int m, Random random) {
    ArrayModifiableDBIDs s = DBIDUtil.newArray(sampleSet);
    ModifiableDBIDs medoids = DBIDUtil.newHashSet();

    // m_1 is random point of S
    DBID m_i = s.remove(random.nextInt(s.size()));
    medoids.add(m_i);
    if(logger.isDebugging()) {
      logger.debugFiner("medoids " + medoids);
    }

    // compute distances between each point in S and m_i
    Map<DBID, DistanceResultPair<DoubleDistance>> distances = new HashMap<DBID, DistanceResultPair<DoubleDistance>>();
    for(DBID id : s) {
      DoubleDistance dist = distFunc.distance(id, m_i);
      distances.put(id, new DistanceResultPair<DoubleDistance>(dist, id));
    }

    for(int i = 1; i < m; i++) {
      // choose medoid m_i to be far from prevois medoids
      List<DistanceResultPair<DoubleDistance>> d = new ArrayList<DistanceResultPair<DoubleDistance>>(distances.values());
      Collections.sort(d);

      m_i = d.get(d.size() - 1).getID();
      medoids.add(m_i);
      s.remove(m_i);
      distances.remove(m_i);

      // compute distances of each point to closest medoid
      for(DBID id : s) {
        DoubleDistance dist_new = distFunc.distance(id, m_i);
        DoubleDistance dist_old = distances.get(id).getFirst();

        DoubleDistance dist = dist_new.compareTo(dist_old) < 0 ? dist_new : dist_old;
        distances.put(id, new DistanceResultPair<DoubleDistance>(dist, id));
      }

      if(logger.isDebugging()) {
        logger.debugFiner("medoids " + medoids);
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
  private ModifiableDBIDs initialSet(DBIDs sampleSet, int k, Random random) {
    ArrayModifiableDBIDs s = DBIDUtil.newArray(sampleSet);
    ModifiableDBIDs initialSet = DBIDUtil.newHashSet();
    while(initialSet.size() < k) {
      DBID next = s.remove(random.nextInt(s.size()));
      initialSet.add(next);
    }
    return initialSet;
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
  private ModifiableDBIDs computeM_current(DBIDs m, DBIDs m_best, DBIDs m_bad, Random random) {
    ArrayModifiableDBIDs m_list = DBIDUtil.newArray(m);
    for(DBID m_i : m_best) {
      m_list.remove(m_i);
    }

    ModifiableDBIDs m_current = DBIDUtil.newHashSet();
    for(DBID m_i : m_best) {
      if(m_bad.contains(m_i)) {
        int currentSize = m_current.size();
        while(m_current.size() == currentSize) {
          DBID next = m_list.remove(random.nextInt(m_list.size()));
          m_current.add(next);
        }
      }
      else {
        m_current.add(m_i);
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
   * @param database the database holding the objects
   * @param distFunc the distance function
   * @return a mapping of the medoid's id to its locality
   */
  private Map<DBID, List<DistanceResultPair<DoubleDistance>>> getLocalities(DBIDs medoids, Database<V> database, DistanceQuery<V, DoubleDistance> distFunc, RangeQuery<V, DoubleDistance> rangeQuery) {
    Map<DBID, List<DistanceResultPair<DoubleDistance>>> result = new HashMap<DBID, List<DistanceResultPair<DoubleDistance>>>();

    for(DBID m : medoids) {
      // determine minimum distance between current medoid m and any other
      // medoid m_i
      DoubleDistance minDist = null;
      for(DBID m_i : medoids) {
        if(m_i == m) {
          continue;
        }
        DoubleDistance currentDist = distFunc.distance(m, m_i);
        if(minDist == null || currentDist.compareTo(minDist) < 0) {
          minDist = currentDist;
        }
      }

      // determine points in sphere centered at m with radius minDist
      assert minDist != null;
      List<DistanceResultPair<DoubleDistance>> qr = rangeQuery.getRangeForDBID(m, minDist);
      result.put(m, qr);
    }

    return result;
  }

  /**
   * Determines the set of correlated dimensions for each medoid in the
   * specified medoid set.
   * 
   * @param medoids the set of medoids
   * @param database the database containing the objects
   * @param distFunc the distance function
   * @return the set of correlated dimensions for each medoid in the specified
   *         medoid set
   */
  private Map<DBID, Set<Integer>> findDimensions(DBIDs medoids, Database<V> database, DistanceQuery<V, DoubleDistance> distFunc, RangeQuery<V, DoubleDistance> rangeQuery) {
    // get localities
    Map<DBID, List<DistanceResultPair<DoubleDistance>>> localities = getLocalities(medoids, database, distFunc, rangeQuery);

    // compute x_ij = avg distance from points in l_i to medoid m_i
    int dim = DatabaseUtil.dimensionality(database);
    Map<DBID, double[]> averageDistances = new HashMap<DBID, double[]>();

    for(DBID m_i : medoids) {
      V medoid_i = database.get(m_i);
      List<DistanceResultPair<DoubleDistance>> l_i = localities.get(m_i);
      double[] x_i = new double[dim];
      for(DistanceResultPair<DoubleDistance> qr : l_i) {
        V o = database.get(qr.getID());
        for(int d = 0; d < dim; d++) {
          x_i[d] += Math.abs(medoid_i.doubleValue(d + 1) - o.doubleValue(d + 1));
        }
      }
      for(int d = 0; d < dim; d++) {
        x_i[d] /= l_i.size();
      }
      averageDistances.put(m_i, x_i);
    }

    Map<DBID, Set<Integer>> dimensionMap = new HashMap<DBID, Set<Integer>>();
    List<CTriple<Double, DBID, Integer>> z_ijs = new ArrayList<CTriple<Double, DBID, Integer>>();
    for(DBID m_i : medoids) {
      Set<Integer> dims_i = new HashSet<Integer>();
      dimensionMap.put(m_i, dims_i);

      double[] x_i = averageDistances.get(m_i);
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
        z_ijs.add(new CTriple<Double, DBID, Integer>((x_i[j] - y_i) / sigma_i, m_i, j + 1));
      }
    }
    Collections.sort(z_ijs);

    int max = Math.max(getK() * getL(), 2);
    for(int m = 0; m < max; m++) {
      CTriple<Double, DBID, Integer> z_ij = z_ijs.get(m);
      Set<Integer> dims_i = dimensionMap.get(z_ij.getSecond());
      dims_i.add(z_ij.getThird());

      if(logger.isDebugging()) {
        StringBuffer msg = new StringBuffer();
        msg.append("\n");
        msg.append("z_ij ").append(z_ij).append("\n");
        msg.append("D_i ").append(dims_i).append("\n");
        logger.debugFiner(msg.toString());
      }
    }
    return dimensionMap;
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
  private List<Pair<V, Set<Integer>>> findDimensions(List<PROCLUSCluster> clusters, Database<V> database) {
    // compute x_ij = avg distance from points in c_i to c_i.centroid
    int dim = DatabaseUtil.dimensionality(database);
    Map<Integer, double[]> averageDistances = new HashMap<Integer, double[]>();

    for(int i = 0; i < clusters.size(); i++) {
      PROCLUSCluster c_i = clusters.get(i);
      double[] x_i = new double[dim];
      for(DBID id : c_i.objectIDs) {
        V o = database.get(id);
        for(int d = 0; d < dim; d++) {
          x_i[d] += Math.abs(c_i.centroid.doubleValue(d + 1) - o.doubleValue(d + 1));
        }
      }
      for(int d = 0; d < dim; d++) {
        x_i[d] /= c_i.objectIDs.size();
      }
      averageDistances.put(i, x_i);
    }

    List<CTriple<Double, Integer, Integer>> z_ijs = new ArrayList<CTriple<Double, Integer, Integer>>();
    for(int i = 0; i < clusters.size(); i++) {
      double[] x_i = averageDistances.get(i);
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
        z_ijs.add(new CTriple<Double, Integer, Integer>((x_i[j] - y_i) / sigma_i, i, j + 1));
      }
    }
    Collections.sort(z_ijs);

    // mapping cluster index -> dimensions
    Map<Integer, Set<Integer>> dimensionMap = new HashMap<Integer, Set<Integer>>();
    int max = Math.max(getK() * getL(), 2);
    for(int m = 0; m < max; m++) {
      CTriple<Double, Integer, Integer> z_ij = z_ijs.get(m);
      Set<Integer> dims_i = dimensionMap.get(z_ij.getSecond());
      if(dims_i == null) {
        dims_i = new HashSet<Integer>();
        dimensionMap.put(z_ij.getSecond(), dims_i);
      }
      dims_i.add(z_ij.getThird());

      if(logger.isDebugging()) {
        StringBuffer msg = new StringBuffer();
        msg.append("\n");
        msg.append("z_ij ").append(z_ij).append("\n");
        msg.append("D_i ").append(dims_i).append("\n");
        logger.debugFiner(msg.toString());
      }
    }

    // mapping cluster -> dimensions
    List<Pair<V, Set<Integer>>> result = new ArrayList<Pair<V, Set<Integer>>>();
    for(int i : dimensionMap.keySet()) {
      Set<Integer> dims_i = dimensionMap.get(i);
      PROCLUSCluster c_i = clusters.get(i);
      result.add(new Pair<V, Set<Integer>>(c_i.centroid, dims_i));
    }
    return result;
  }

  /**
   * Assigns the objects to the clusters.
   * 
   * @param dimensions set of correlated dimensions for each medoid of the
   *        cluster
   * @param database the database containing the objects
   * @return the assignments of the object to the clusters
   */
  private Map<DBID, PROCLUSCluster> assignPoints(Map<DBID, Set<Integer>> dimensions, Database<V> database) {
    Map<DBID, ModifiableDBIDs> clusterIDs = new HashMap<DBID, ModifiableDBIDs>();
    for(DBID m_i : dimensions.keySet()) {
      clusterIDs.put(m_i, DBIDUtil.newHashSet());
    }

    for(Iterator<DBID> it = database.iterator(); it.hasNext();) {
      DBID p_id = it.next();
      V p = database.get(p_id);
      DistanceResultPair<DoubleDistance> minDist = null;
      for(DBID m_i : dimensions.keySet()) {
        V m = database.get(m_i);
        DistanceResultPair<DoubleDistance> currentDist = new DistanceResultPair<DoubleDistance>(manhattanSegmentalDistance(p, m, dimensions.get(m_i)), m_i);
        if(minDist == null || currentDist.compareTo(minDist) < 0) {
          minDist = currentDist;
        }
      }
      // add p to cluster with mindist
      assert minDist != null;
      ModifiableDBIDs ids = clusterIDs.get(minDist.getID());
      ids.add(p_id);
    }

    Map<DBID, PROCLUSCluster> clusters = new HashMap<DBID, PROCLUSCluster>();
    for(DBID m_i : dimensions.keySet()) {
      ModifiableDBIDs objectIDs = clusterIDs.get(m_i);
      if(!objectIDs.isEmpty()) {
        Set<Integer> clusterDimensions = dimensions.get(m_i);
        V centroid = DatabaseUtil.centroid(database, objectIDs);
        clusters.put(m_i, new PROCLUSCluster(objectIDs, clusterDimensions, centroid));
      }
    }

    if(logger.isDebugging()) {
      StringBuffer msg = new StringBuffer();
      msg.append("\n");
      msg.append("clusters ").append(clusters).append("\n");
      logger.debugFine(msg.toString());
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
  private List<PROCLUSCluster> finalAssignment(List<Pair<V, Set<Integer>>> dimensions, Database<V> database) {
    Map<Integer, ModifiableDBIDs> clusterIDs = new HashMap<Integer, ModifiableDBIDs>();
    for(int i = 0; i < dimensions.size(); i++) {
      clusterIDs.put(i, DBIDUtil.newHashSet());
    }

    for(Iterator<DBID> it = database.iterator(); it.hasNext();) {
      DBID p_id = it.next();
      V p = database.get(p_id);
      Pair<DoubleDistance, Integer> minDist = null;
      for(int i = 0; i < dimensions.size(); i++) {
        Pair<V, Set<Integer>> pair_i = dimensions.get(i);
        V c_i = pair_i.first;
        Set<Integer> dimensions_i = pair_i.second;
        DoubleDistance currentDist = manhattanSegmentalDistance(p, c_i, dimensions_i);
        if(minDist == null || currentDist.compareTo(minDist.first) < 0) {
          minDist = new Pair<DoubleDistance, Integer>(currentDist, i);
        }
      }
      // add p to cluster with mindist
      assert minDist != null;
      ModifiableDBIDs ids = clusterIDs.get(minDist.second);
      ids.add(p_id);
    }

    List<PROCLUSCluster> clusters = new ArrayList<PROCLUSCluster>();
    for(int i = 0; i < dimensions.size(); i++) {
      ModifiableDBIDs objectIDs = clusterIDs.get(i);
      if(!objectIDs.isEmpty()) {
        Set<Integer> clusterDimensions = dimensions.get(i).second;
        V centroid = DatabaseUtil.centroid(database, objectIDs);
        clusters.add(new PROCLUSCluster(objectIDs, clusterDimensions, centroid));
      }
    }

    if(logger.isDebugging()) {
      StringBuffer msg = new StringBuffer();
      msg.append("\n");
      msg.append("clusters ").append(clusters).append("\n");
      logger.debugFine(msg.toString());
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
  private DoubleDistance manhattanSegmentalDistance(V o1, V o2, Set<Integer> dimensions) {
    double result = 0;
    for(Integer d : dimensions) {
      result += Math.abs(o1.doubleValue(d) - o2.doubleValue(d));
    }
    result /= dimensions.size();
    return new DoubleDistance(result);
  }

  /**
   * Evaluates the quality of the clusters.
   * 
   * @param clusters the clusters to be evaluated
   * @param dimensions the dimensions associated with each cluster
   * @param database the database holding the objects
   * @return a measure for the cluster quality
   */
  private double evaluateClusters(Map<DBID, PROCLUSCluster> clusters, Map<DBID, Set<Integer>> dimensions, Database<V> database) {
    double result = 0;
    for(DBID m_i : clusters.keySet()) {
      PROCLUSCluster c_i = clusters.get(m_i);
      V centroid_i = c_i.centroid;

      Set<Integer> dims_i = dimensions.get(m_i);
      double w_i = 0;
      for(Integer j : dims_i) {
        w_i += avgDistance(centroid_i, c_i.objectIDs, database, j);
      }

      w_i /= dimensions.keySet().size();
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
  private double avgDistance(V centroid, DBIDs objectIDs, Database<V> database, int dimension) {
    double avg = 0;
    for(DBID objectID : objectIDs) {
      V o = database.get(objectID);
      avg += Math.abs(centroid.doubleValue(dimension) - o.doubleValue(dimension));
    }
    return avg / objectIDs.size();
  }

  /**
   * Computes the bad medoids, where the medoid of a cluster with less than the
   * specified threshold of objects is bad.
   * 
   * @param clusters the clusters
   * @param threshold the threshold
   * @return the bad medoids
   */
  private ModifiableDBIDs computeBadMedoids(Map<DBID, PROCLUSCluster> clusters, int threshold) {
    ModifiableDBIDs badMedoids = DBIDUtil.newHashSet();
    for(DBID m_i : clusters.keySet()) {
      PROCLUSCluster c_i = clusters.get(m_i);
      if(c_i.objectIDs.size() < threshold) {
        badMedoids.add(m_i);
      }
    }
    return badMedoids;
  }

  /**
   * Encapsulates the attributes of a cluster.
   * 
   * @apiviz.exclude
   */
  private class PROCLUSCluster {
    /**
     * The ids of the objects belonging to this cluster.
     */
    ModifiableDBIDs objectIDs;

    /**
     * The correlated dimensions of this cluster.
     */
    Set<Integer> dimensions;

    /**
     * The centroids of this cluster along each dimension.
     */
    V centroid;

    /**
     * Provides a new cluster with the specified parameters.
     * 
     * @param objectIDs the ids of the objects belonging to this cluster
     * @param dimensions the correlated dimensions of this cluster
     * @param centroid the centroid of this cluster
     */
    public PROCLUSCluster(ModifiableDBIDs objectIDs, Set<Integer> dimensions, V centroid) {
      this.objectIDs = objectIDs;
      this.dimensions = dimensions;
      this.centroid = centroid;
    }

    @Override
    public String toString() {
      StringBuffer result = new StringBuffer();
      result.append("Dimensions: [");
      boolean notFirst = false;
      for(Integer d : dimensions) {
        if(notFirst) {
          result.append(",");
        }
        else {
          notFirst = true;
        }
        result.append(d);
      }
      result.append("]");

      result.append("\nCentroid: ").append(centroid);
      return result.toString();
    }

    /**
     * Returns the correlated dimensions of this cluster as BitSet.
     * 
     * @return the correlated dimensions of this cluster as BitSet
     */
    public BitSet getDimensions() {
      BitSet result = new BitSet();
      for(int d : dimensions) {
        result.set(d - 1);
      }
      return result;
    }
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}