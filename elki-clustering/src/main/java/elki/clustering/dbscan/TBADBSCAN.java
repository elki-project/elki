package elki.clustering.dbscan;

import elki.Algorithm;
import elki.clustering.ClusteringAlgorithm;
import elki.clustering.kmeans.initialization.KMeansPlusPlus;
import elki.clustering.kmedoids.initialization.BUILD;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.ClusterModel;
import elki.data.model.Model;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.datastore.memory.MapIntegerDBIDIntegerStore;
import elki.database.ids.*;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.distance.minkowski.EuclideanDistance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.progress.IndefiniteProgress;
import elki.logging.statistics.LongStatistic;
import elki.result.Metadata;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.constraints.CommonConstraints;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;
import elki.utilities.optionhandling.parameters.EnumParameter;
import elki.utilities.optionhandling.parameters.IntParameter;
import elki.utilities.optionhandling.parameters.ObjectParameter;
import elki.utilities.random.RandomFactory;

import java.util.*;

public class TBADBSCAN<O> implements ClusteringAlgorithm<Clustering<Model>> {
  /**
   * The logger for this class
   */
  private static final Logging LOG = Logging.getLogger(TBADBSCAN.class);

  /**
   * Distance function used.
   */
  protected Distance<? super O> distance;

  /**
   * Holds the epsilon radius threshold
   */
  protected double epsilon;

  /**
   * Holds the minimum cluster size
   */
  protected int minpts;

  /**
   * Holds the number of reference points to be used.
   */
  protected int nRefPoints;

  /**
   * Holds the possible modes for reference point selection.
   * Default mode is the k-Means++-Seeding
   */
  public enum RefPointMode {
    RANDOM,
    KPP,
    QUANTIL,
    PAM,
  }
  protected RefPointMode refPointMode = RefPointMode.KPP;

  /**
   * @param distance Distance funtion
   * @param epsilon Epsilon value
   * @param minpts Minpts parameter
   * @param nRefPoints Number of reference points
   * @param mode Reference point selection mode
   */
  public TBADBSCAN(Distance<? super O> distance, double epsilon, int minpts, int nRefPoints, RefPointMode mode){
    super();
    this.distance = distance;
    this.epsilon = epsilon;
    this.minpts = minpts;
    this.nRefPoints = nRefPoints;
    this.refPointMode = mode;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(distance.getInputTypeRestriction());
  }

  /**
   * Performs the TBA-DBSCAN algorithm on the given database
   *
   * @param relation Given Database
   * @return Computed clustering
   */
  public Clustering<Model> run(Relation<O> relation){
    final int datasetSize = relation.size();
    if(datasetSize < minpts) {
      Clustering<Model> result = new Clustering<>();
      Metadata.of(result).setLongName("TBA-DBSCAN Clustering");
      result.addToplevelCluster(new Cluster<>(relation.getDBIDs(), true, ClusterModel.CLUSTER));
      return result;
    }

    Instance tbaDBSCAN = new Instance();
    tbaDBSCAN.run(relation);

    Clustering<Model> result = new Clustering<>();
    Metadata.of(result).setLongName("TBA-DBSCAN Clustering");
    for(ModifiableDBIDs res: tbaDBSCAN.resultList) {
      result.addToplevelCluster(new Cluster<>(res, ClusterModel.CLUSTER));
    }
    result.addToplevelCluster(new Cluster<>(tbaDBSCAN.noise, true, ClusterModel.CLUSTER));
    return result;
  }

  /**
   * Instance for a single data set.
   *
   * @author Felix Krause
   */
  private class Instance {
    /**
     * Holds a list of clusters found.
     */
    protected List<ModifiableDBIDs> resultList;

    /**
     * Holds a set of noise.
     */
    protected ModifiableDBIDs noise;

    /**
     * Holds a set of processed ids.
     */
    protected ModifiableDBIDs processedIDs;

    /**
     * Progress for objects (may be null).
     */
    protected FiniteProgress objprog;

    /**
     * Progress for clusters (may be null).
     */
    protected IndefiniteProgress clusprog;

    /**
     * Holds distance function query object.
     */
    protected DistanceQuery<? super O> distanceQuery;

    /**
     * Holds distances to reference points
     */
    protected ModifiableDoubleDBIDList[] refDists;

    /**
     * List of maps from ids to index in refDists lists.
     */
    protected MapIntegerDBIDIntegerStore[] refDistsOffsetMap;

    /**
     * Number of distance calculations
     */
    protected long nDistCalcs;


    /**
     * Run the TBA-DBSCAN algorithm
     *
     * @param relation Data relation
     */
    protected void run(Relation<O> relation){
      final int size = relation.size();
      this.objprog = LOG.isVerbose() ? new FiniteProgress("Processing objects", size, LOG) : null;
      this.clusprog = LOG.isVerbose() ? new IndefiniteProgress("Number of clusters", LOG) : null;
      this.distanceQuery = distance.instantiate(relation);

      //Instantiate necessary objects
      resultList = new ArrayList<>();
      noise = DBIDUtil.newHashSet();
      processedIDs = DBIDUtil.newHashSet(size);
      nDistCalcs = 0;

      // Instatiate reference point lists
      refDists = new ModifiableDoubleDBIDList[nRefPoints];
      refDistsOffsetMap = new MapIntegerDBIDIntegerStore[nRefPoints];
      for(int i=0; i<nRefPoints; i++){
        refDists[i] = DBIDUtil.newDistanceDBIDList(relation.size());
        refDistsOffsetMap[i] = new MapIntegerDBIDIntegerStore(relation.size());
      }

      // Calculate reference points with selected mode
      switch (refPointMode){
        case KPP:
          generateKMPPRefPoints(relation);
          break;
        case RANDOM:
          generateRandomRefPoints(relation);
          break;
        case QUANTIL:
          generateQuantilRefPoints(relation);
          break;
        case PAM:
          generatePAMRefPoints(relation);
          break;
      }
      //Outputs the number of distance calculations used in initialization.
      //Always number of reference points * number of objects in dataset.
      //Additional distance calculation may be used depending on used selection mode.
      LOG.statistics(new LongStatistic(TBADBSCAN.class.getName() + ".initialization-distance-computations", nDistCalcs));

      //Start Clustering here
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()){
        if(!processedIDs.contains(iditer)){
          expandCluster(iditer);
        }

        if(objprog != null && clusprog != null) {
          objprog.setProcessed(processedIDs.size(), LOG);
          clusprog.setProcessed(resultList.size(), LOG);
        }
        if(processedIDs.size() == size) {
          break;
        }
      }

      //Output number of total used distance calculations.
      LOG.statistics(new LongStatistic(TBADBSCAN.class.getName() + ".distance-computations", nDistCalcs));
      // Finish progress logging
      LOG.ensureCompleted(objprog);
      LOG.setCompleted(clusprog);
    }

    /**
     * TBA-DBSCAN-funtion expandCluster.
     * <p>
     * Border-Objects become members of the first possible cluster.
     *
     * @param startObjectID potential seed of a new potential cluster
     */
    protected void expandCluster(DBIDRef startObjectID){
      ArrayModifiableDBIDs seeds = DBIDUtil.newArray();
      ModifiableDBIDs neighbors = getNeighbors(startObjectID);

      processedIDs.add(startObjectID);
      LOG.incrementProcessed(objprog);

      //No core continue
      if(neighbors.size() < minpts){
        noise.add(startObjectID);
        return;
      }

      ModifiableDBIDs currentCluster = DBIDUtil.newArray(neighbors.size());
      currentCluster.add(startObjectID);
      processNeighbors(neighbors, currentCluster, seeds);

      DBIDVar o = DBIDUtil.newVar();
      while (!seeds.isEmpty()){
        ModifiableDBIDs curSeeds = getNeighbors(seeds.pop(o));
        if (curSeeds.size() >= minpts){
          processNeighbors(curSeeds, currentCluster, seeds);
        }
        LOG.incrementProcessed(objprog);
      }
      resultList.add(currentCluster);
      LOG.incrementProcessed(clusprog);
    }

    /**
     * Process a single core point.
     *
     * @param neighbors Neighbors
     * @param currentCluster Current cluster
     * @param seeds Seed set
     */
    private void processNeighbors(ModifiableDBIDs neighbors, ModifiableDBIDs currentCluster, ArrayModifiableDBIDs seeds) {
      for(DBIDIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()){
        if(processedIDs.add(neighbor)){
          if(!seeds.contains(neighbor)) {
            seeds.add(neighbor);
          }
        } else if(!noise.remove(neighbor)){
          continue;
        }
        currentCluster.add(neighbor);
      }
    }


    // NEIGHBOR FINDING

    /**
     * Query neighbors for given point.
     *
     * @param point Given point
     * @return Set of neighbors
     */
    protected ModifiableDBIDs getNeighbors(DBIDRef point){
      ModifiableDBIDs neighborhoodCandidates = getCombinedNeighborhoodCandidates(point);

      ModifiableDBIDs neighbors = DBIDUtil.newArray();
      for(DBIDMIter neighborIter = neighborhoodCandidates.iter(); neighborIter.valid(); neighborIter.advance()){
        nDistCalcs++;
        if(distanceQuery.distance(point, neighborIter) <= epsilon){
          neighbors.add(neighborIter);
        }
      }
      return neighbors;
    }

    /**
     * Intersection of all neighborhood candidates for a given point.
     *
     * @param point Given point.
     * @return Set of neighborhood candidates.
     */
    protected ModifiableDBIDs getCombinedNeighborhoodCandidates(DBIDRef point){
      ModifiableDBIDs neighboorhoodCandidates = getNeighborhoodCandidates(0, point);

      // if requested neighborhood does not satisfy core point requirements stop
      if(neighboorhoodCandidates.size() < minpts) {
        return DBIDUtil.newArray();
      }

      // intersection of all neighborhood candidates
      if (nRefPoints > 0) {
        for (int i = 1; i < nRefPoints; i++) {
          ModifiableDBIDs nextNeighboorhoodCandidates = getNeighborhoodCandidates(i, point);
          // if requested neighborhood does not satisfy core point requirements stop
          if (nextNeighboorhoodCandidates.size() < minpts) {
            return DBIDUtil.newArray();
          }
          neighboorhoodCandidates = DBIDUtil.intersection(neighboorhoodCandidates, nextNeighboorhoodCandidates);
          // if requested neighborhood does not satisfy core point requirements stop
          if (neighboorhoodCandidates.size() < minpts) {
            return DBIDUtil.newArray();
          }
        }
      }
      return neighboorhoodCandidates;
    }


    /**
     * Query neighborhood candidates for a given point and a single reference point.
     *
     * @param refPointIndex Used reference point
     * @param point Requested point.
     * @return Neighborhood candidates of a given point and single reference point
     */
    protected ModifiableDBIDs getNeighborhoodCandidates(int refPointIndex, DBIDRef point){
      int offset = refDistsOffsetMap[refPointIndex].intValue(point);
      ArrayModifiableDBIDs forwardCandidates = getForwardCandidates(refPointIndex, offset);
      ArrayModifiableDBIDs backwardCandidates = getBackwardCandidates(refPointIndex, offset);
      forwardCandidates.addDBIDs(backwardCandidates);
      return forwardCandidates;
    }

    /**
     * Forward search of neighborhood candidates for queried point and reference point.
     *
     * @param refPointIndex Used reference point
     * @param referencePointOffset Index of queried point in distance list
     * @return Forward neighborhood candidates of queried point
     */
    protected ArrayModifiableDBIDs getForwardCandidates(int refPointIndex, int referencePointOffset){
      ArrayModifiableDBIDs forwardCandidates = DBIDUtil.newArray();
      double startDist = refDists[refPointIndex].doubleValue(referencePointOffset);
      double forwardThreshold = startDist + epsilon;
      for(DoubleDBIDListIter distIter = refDists[refPointIndex].iter().seek(referencePointOffset); distIter.valid(); distIter.advance()){
        if(distIter.doubleValue() > forwardThreshold){
          break;
        }
        forwardCandidates.add(distIter);
      }
      return forwardCandidates;
    }

    /**
     * Backward search of neighborhood candidates for queried point and reference point.
     *
     * @param refPointIndex Used reference point
     * @param referencePointOffset Index of queried point in distance list
     * @return Backward neighborhood candidates of queried point
     */
    protected ArrayModifiableDBIDs getBackwardCandidates(int refPointIndex, int referencePointOffset){
      ArrayModifiableDBIDs backwardCandidates = DBIDUtil.newArray();
      double startDist = refDists[refPointIndex].doubleValue(referencePointOffset);
      double backwardThreshold = startDist - epsilon;
      for(DoubleDBIDListIter distIter = refDists[refPointIndex].iter().seek(referencePointOffset); distIter.valid(); distIter.retract()){
        if(distIter.doubleValue() < backwardThreshold){
          break;
        }
        backwardCandidates.add(distIter);
      }
      return backwardCandidates;
    }


    /**
     * Compute distance from reference point to all points in data set
     * and sorting them in regards to distance.
     *
     * @param index Index of reference point
     * @param refPoint Reference point id
     * @param relation Data set
     */
    protected void generateSortedReferenceDistances(int index, DBIDRef refPoint, Relation<O> relation){
      // get distance from reference point to every other point
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()){
        nDistCalcs++;
        double dist = distanceQuery.distance(iditer, refPoint);
        refDists[index].add(dist, iditer);
      }
      refDists[index].sort();
    }


    /**
     * Generating a map from id in data set to index in distance array
     *
     * @param index Index of reference point
     */
    protected void generateOffsetMap(int index){
      for(DoubleDBIDListIter idIter = refDists[index].iter(); idIter.valid(); idIter.advance()){
        refDistsOffsetMap[index].putInt(idIter, idIter.getOffset());
      }
    }

    /**
     * Selection of reference points based on quartil of previous refernce point.
     *
     * @param relation data set
     */
    protected void generateQuantilRefPoints(Relation<O> relation){
      DBIDVar startRefPoint = DBIDUtil.newVar();
      DBIDUtil.randomSample(relation.getDBIDs(), 1, new Random()).pop(startRefPoint);
      generateSortedReferenceDistances(0,startRefPoint, relation);
      generateOffsetMap(0);

      DBIDVar nextRefPoint = DBIDUtil.newVar();
      for(int i=1; i<nRefPoints; i++){
        int qunatilIndex = (int) Math.floor(refDists[i-1].size() * 0.75);
        refDists[i-1].assignVar(qunatilIndex, nextRefPoint);
        generateSortedReferenceDistances(i, nextRefPoint, relation);
        generateOffsetMap(i);
      }
    }

    /**
     * Selection of reference points by chosing random points.
     *
     * @param relation data set.
     */
    protected void generateRandomRefPoints(Relation<O> relation){
      ModifiableDBIDs refPoints = DBIDUtil.randomSample(relation.getDBIDs(), nRefPoints, new Random());
      DBIDVar refPoint = DBIDUtil.newVar();
      for(int i=0; i < nRefPoints; i++){
        refPoints.pop(refPoint);
        generateSortedReferenceDistances(i, refPoint, relation);
        generateOffsetMap(i);
      }
    }

    /**
     * Selection of reference points by using k-Means++-Seeding.
     *
     * @param relation Data set
     */
    protected void generateKMPPRefPoints(Relation<O> relation){
      KMeansPlusPlus kpp = new KMeansPlusPlus<>(new RandomFactory(new Random().nextInt()));
      DBIDs refPoints = kpp.chooseInitialMedoids(nRefPoints, relation.getDBIDs(), distanceQuery);
      int i = 0;
      for(DBIDIter iter = refPoints.iter(); iter.valid(); iter.advance()){
        generateSortedReferenceDistances(i, iter, relation);
        generateOffsetMap(i);
        i++;
      }
    }

    /**
     * Selection of reference points by using PAM-BUILD.
     *
     * @param relation Data set
     */
    protected void generatePAMRefPoints(Relation<O> relation){
      DBIDs pamInit = new BUILD().chooseInitialMedoids(nRefPoints, relation.getDBIDs(), distanceQuery);
      int i = 0;
      for(DBIDIter iter = pamInit.iter(); iter.valid(); iter.advance()){
        generateSortedReferenceDistances(i, iter, relation);
        generateOffsetMap(i);
        i++;
      }
    }
  }


  /**
   * Parameterization class.
   *
   * @author Felix Krause
   */
  public static class Par<O> implements Parameterizer {
    /**
     * Parameter to specify the maximum radius of the neighborhood to be
     * considered, must be suitable to the distance function specified.
     */
    public static final OptionID EPSILON_ID = new OptionID("tbadbscan.epsilon", "The maximum radius of the neighborhood to be considered.");

    /**
     * Parameter to specify the threshold for minimum number of points in the
     * epsilon-neighborhood of a point, must be an integer greater than 0.
     */
    public static final OptionID MINPTS_ID = new OptionID("tbadbscan.minpts", "Threshold for minimum number of points in the epsilon-neighborhood of a point. The suggested value is '2 * dim - 1'.");

    /**
     * Parameter to specify the number of reference points to use for clustering.
     * Must be an integer greater than 0.
     */
    public static final OptionID NREFPOINTS_ID = new OptionID("tbadbscan.nRefPoints", "The number of reference points to use for clustering.");

    /**
     * Parameter to specify the mode how reference points will be chosen.
     */
    public static final OptionID MODE_ID = new OptionID("tbadbscan.refPointMode", "The mode of which the refpoints are chosen.");

    /**
     * Holds the epsilon radius threshold.
     */
    protected double epsilon;

    /**
     * Holds the minimum cluster size.
     */
    protected int minpts;

    /**
     * Holds the number of reference points.
     */
    protected int nRefPoints;

    /**
     * Holds the chosen mode for the reference point search.
     * Default mode is K-Means++-Seeding.
     */
    protected RefPointMode mode = RefPointMode.KPP;


    /**
     * The distance function to use.
     */
    protected Distance<? super O> distance;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<Distance<? super O>>(Utils.DISTANCE_FUNCTION_ID, Distance.class, EuclideanDistance.class) //
          .grab(config, x -> distance = x);
      new DoubleParameter(EPSILON_ID) //
          .addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE) //
          .grab(config, x -> epsilon = x);
      if(new IntParameter(MINPTS_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT) //
          .grab(config, x -> minpts = x) && minpts <= 2) {
        LOG.warning("DBSCAN with minPts <= 2 is equivalent to single-link clustering at a single height. Consider using larger values of minPts.");
      }
      new IntParameter(NREFPOINTS_ID) //
          .addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT)
          .grab(config, x -> nRefPoints = x);
      new EnumParameter<RefPointMode>(MODE_ID, RefPointMode.class, RefPointMode.KPP) //
          .grab(config, x -> mode = x);
    }

    @Override
    public TBADBSCAN<O> make() {
      return new TBADBSCAN<>(distance, epsilon, minpts, nRefPoints, mode);
    }
  }
}
