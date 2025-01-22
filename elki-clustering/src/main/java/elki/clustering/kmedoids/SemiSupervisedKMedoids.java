/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2022
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
package elki.clustering.kmedoids;

import java.util.ArrayList;
import java.util.List;

import elki.Algorithm;
import elki.clustering.ClusteringAlgorithm;
import elki.clustering.kmeans.KMeans;
import elki.clustering.kmedoids.initialization.SemiSupervisedKMedoidsInitialization;
import elki.clustering.kmedoids.initialization.UnsupervisedInitialization;
import elki.data.Clustering;
import elki.data.LabelList;
import elki.data.model.MedoidModel;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.query.QueryBuilder;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.logging.statistics.Duration;
import elki.logging.statistics.StringStatistic;
import elki.utilities.exceptions.AbortException;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Interface for clustering algorithms that produce medoids.
 * <p>
 * These may be used to initialize PAMSIL clustering, for example.
 * 
 * @author Erich Schubert
 * @since 0.8.0
 */
public abstract class SemiSupervisedKMedoids<O> implements  ClusteringAlgorithm<Clustering<MedoidModel>> {

  /**
   * The logger for this class.
   */
    private static final Logging LOG = Logging.getLogger(SemiSupervisedKMedoids.class);
  
  /**
   * Distance function used.
   */
  protected Distance<? super O> distance;

  /**
   * The number of clusters to produce.
   */
  protected int k;

  /**
   * The maximum number of iterations.
   */
  protected int maxiter;

  /**
   * Method to choose initial means.
   */
  protected SemiSupervisedKMedoidsInitialization<O> initializer;

/*
 * Constructor
 */
  public SemiSupervisedKMedoids(Distance<? super O> distance, int k, int maxiter, SemiSupervisedKMedoidsInitialization<O> initializer) {
    this.distance = distance;
    this.k = k;
    this.maxiter = maxiter;
    this.initializer = initializer;
  }

  public Clustering<MedoidModel> run(Database database) {
    return run(database.getRelation(TypeUtil.NUMBER_VECTOR_VARIABLE_LENGTH), database.getRelation(TypeUtil.LABELLIST));
  }

  /**
   * Run k-medoids clustering.
   *
   * @param relation relation to use
   * @return result
   */
  public Clustering<MedoidModel> run(Relation<O> relation, Relation<LabelList> labels){
    return run(relation, labels, k, new QueryBuilder<>(relation, distance)
                .precomputed()
                .distanceQuery());
  }

  /**
   * Run k-medoids clustering with a given distance query.<br>
   * Not a very elegant API, but needed for some types of nested k-medoids.
   *
   * @param relation relation to use
   * @param k Number of clusters
   * @param distQ Distance query to use
   * @return result
   */
  public Clustering<MedoidModel> run(Relation<O> relation, Relation<LabelList> labels, int k, DistanceQuery<? super O> distQ){
    DBIDs ids = relation.getDBIDs();
    WritableIntegerDataStore labelsMaps = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, -1);
    List<String> labelList = createLabelMap(labels, labelsMaps);
    return run(relation, labelsMaps, labelList.size()-1, k, distQ);
  }

  /**
   * Run k-medoids clustering with a given distance query.<br>
   * and integer labels.
   *
   * @param relation relation to use
   * @param labels integer labels already formated in a usable way
   * @param k Number of clusters
   * @param distQ Distance query to use
   * @return result
   */
  public Clustering<MedoidModel> run(Relation<O> relation, WritableIntegerDataStore labels, int l, int k, DistanceQuery<? super O> distQ){
    DBIDs ids = relation.getDBIDs();
    ArrayModifiableDBIDs medoids = DBIDUtil.newArray(k); 
    int[] clusterLabels = initialMedoids(distQ, ids, labels, l, k, medoids); // add labels
    // assert that each medoid is unique
    assert checkMedoidUnique(medoids);
    WritableIntegerDataStore assignment = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, -1);
    Duration optd = getLogger().newDuration(getClass().getName() + ".optimization-time").begin();
    instanceWrapper(distQ, ids, assignment, labels, clusterLabels, l).run(medoids, k);
    getLogger().statistics(optd.end());
    return PAM.wrapResult(ids, assignment, medoids, "MED_Clustering");
  }

  protected List<String> createLabelMap(Relation<LabelList> labels, WritableIntegerDataStore labelsMaps){
    List<String> labelList = new ArrayList<>();
    labelList.add("");
    DBIDs ids = labels.getDBIDs();
    for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      String label = labels.get(iter).get(0);
      // TODO check if get(0) can be out of bounds, then also no Label
      int labelIndex = labelList.indexOf(label);
      if (labelIndex == -1) {
        labelIndex = labelList.size();
        labelList.add(label);
      }
      labelsMaps.put(iter, labelIndex);
    }
    return labelList;
  }

  private boolean checkMedoidUnique(ArrayModifiableDBIDs medoids){
    for (int i = 0; i < medoids.size(); i++) {
      for (int j = i+1; j < medoids.size(); j++) {
        if (DBIDUtil.equal(medoids.get(i), medoids.get(j))) {
          return false;
        }
      }
    }
    return true;
  }

    /**
   * Choose the initial medoids.
   *
   * @param distQ Distance query
   * @param ids IDs to choose from
   * @param k Number of medoids to choose from
   * @return Initial medoids
   */
  protected int[] initialMedoids(DistanceQuery<? super O> distQ, DBIDs ids, WritableIntegerDataStore labels, int l, int k, ArrayModifiableDBIDs medoids) {
    if(getLogger().isStatistics()) {
      getLogger().statistics(new StringStatistic(getClass().getName() + ".initialization", initializer.toString()));
    }
    Duration initd = getLogger().newDuration(getClass().getName() + ".initialization-time").begin();
    int[] clusterLabels = initializer.chooseInitialMedoids(k, l, ids, labels, distQ, medoids);
    getLogger().statistics(initd.end());
    if(medoids.size() != k) {
      throw new AbortException("Initializer " + initializer.toString() + " did not return " + k + " means, but " + medoids.size());
    }
    return clusterLabels;
  }


  abstract Instance instanceWrapper(DistanceQuery<?> distQ, DBIDs ids, WritableIntegerDataStore assignment, WritableIntegerDataStore labelsMaps, int[] clusterLabel, int noLabels);

  protected static abstract class Instance {

    /**
     * Ids to process.
     */
    DBIDs ids;

    /**
     * Distance function to use.
     */
    DistanceQuery<?> distQ;

    /**
     * Distance to the nearest medoid of each point.
     */
    WritableDoubleDataStore nearest;

    /**
     * Distance to the second nearest medoid.
     */
    WritableDoubleDataStore second;

    /**
     * Cluster mapping.
     */
    WritableIntegerDataStore assignment;

    protected final WritableIntegerDataStore pointLabelMap;

    protected final int numberOfLabels;

    protected final int[] clusterLabels;

    /**
         * Constructor.
         *
         * @param distQ Distance query
         * @param ids IDs to process
         * @param assignment Cluster assignment
         */
        public Instance(DistanceQuery<?> distQ, DBIDs ids, WritableIntegerDataStore assignment, WritableIntegerDataStore labelsMaps, int[] clusterLabel, int numberOfLabels) {
          this.ids = ids;
          this.distQ = distQ;
          this.nearest = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
          this.second = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
          this.assignment = assignment;
          this.pointLabelMap = labelsMaps;
          this.numberOfLabels = numberOfLabels;
          this.clusterLabels = clusterLabel;

        }


    protected abstract double run(ArrayModifiableDBIDs medoids, int maxiter);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distance.getInputTypeRestriction());
  }

  /**
   * Get the logger for this class.
   *
   * @return Logger
   */
  protected Logging getLogger(){
    return LOG;
  }


  /**
   * Parameterization class.
   * 
   * @author Andreas Lang
   */
  public static abstract class Par<O> implements Parameterizer {
    /**
     * The number of clusters to produce.
     */
    protected int k;

    /**
     * The maximum number of iterations.
     */
    protected int maxiter;
    
    /**
     * Method to choose initial means.
     */
    protected SemiSupervisedKMedoidsInitialization<O> initializer;

    /**
     * The distance function to use.
     */
    protected Distance<? super O> distance;

      @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(Algorithm.Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new IntParameter(KMeans.K_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> k = x);
      new ObjectParameter<SemiSupervisedKMedoidsInitialization<O>>(KMeans.INIT_ID, SemiSupervisedKMedoidsInitialization.class) //
          .grab(config, x -> initializer = x);
      new IntParameter(KMeans.MAXITER_ID, 0) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT) //
          .grab(config, x -> maxiter = x);
    }

    /**
     * Default initialization method.
     *
     * @return Initialization method
     */
    @SuppressWarnings("rawtypes")
    protected Class<? extends SemiSupervisedKMedoidsInitialization> defaultInitializer() {
      return UnsupervisedInitialization.class;
    }

    @Override
    public abstract SemiSupervisedKMedoids<O> make();

  }
}
