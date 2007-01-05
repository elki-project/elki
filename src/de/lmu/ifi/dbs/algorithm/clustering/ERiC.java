package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.algorithm.result.clustering.HierarchicalCorrelationCluster;
import de.lmu.ifi.dbs.algorithm.result.clustering.HierarchicalCorrelationClusters;
import de.lmu.ifi.dbs.algorithm.result.clustering.PartitionClusteringResults;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.data.SimpleClassLabel;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.distancefunction.PCABasedCorrelationDistanceFunction;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.varianceanalysis.FirstNEigenPairFilter;
import de.lmu.ifi.dbs.varianceanalysis.GlobalPCA;
import de.lmu.ifi.dbs.varianceanalysis.LinearLocalPCA;
import de.lmu.ifi.dbs.varianceanalysis.LocalPCA;

import java.util.*;

/**
 * Performs the COPAC algorithm on the data and builds
 * a hierarchy of correlation clusters that allows multiple inheritance from the clustering result.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ERiC extends AbstractAlgorithm<RealVector> {
  /**
   * The COPAC clustering algorithm.
   */
  private COPAC copacAlgorithm;

  /**
   * Holds the result.
   */
  private HierarchicalCorrelationClusters<RealVector> result;

  /**
   * Performs the COPAC algorithm on the data and builds
   * a hierarchy of correlation clusters that allows multiple inheritance from the clustering result.
   */
  public ERiC() {
    this.debug = true;
  }

  /**
   * The run method encapsulated in measure of runtime. An extending class
   * needs not to take care of runtime itself.
   *
   * @param database the database to run the algorithm on
   * @throws IllegalStateException if the algorithm has not been initialized properly (e.g. the
   *                               setParameters(String[]) method has been failed to be called).
   */
  protected void runInTime(Database<RealVector> database) throws IllegalStateException {
    try {
      int dimensionality = database.dimensionality();

      if (isVerbose()) {
        verbose("Step 1: Run COPAC algorithm...");
      }
      copacAlgorithm.run(database);

// extract correlation clusters
      SortedMap<Integer, List<HierarchicalCorrelationCluster>> clusterMap = extractCorrelationClusters(database, dimensionality);
      if (this.debug) {
        StringBuffer msg = new StringBuffer("\n\nStep 2: extract correlation clusters");
        for (Integer corrDim : clusterMap.keySet()) {
          List<HierarchicalCorrelationCluster> correlationClusters = clusterMap.get(corrDim);
          msg.append("\n\ncorrDim " + corrDim);
          for (HierarchicalCorrelationCluster cluster : correlationClusters) {
            msg.append("\n  cluster " + cluster + ", ids: " + cluster.getIDs().size() + ", level: " + cluster.getLevel() + ", index: " + cluster.getLevelIndex());
//          msg.append("\n  basis " + cluster.getPCA().getWeakEigenvectors().toString("    ", NF) + "  ids " + cluster.getIDs().size());
          }
        }
        debugFine(msg.toString());
      }

// build hierarchy
      buildHierarchy(dimensionality, clusterMap);
      if (this.debug) {
        StringBuffer msg = new StringBuffer("\n\nStep 3: build hierarchy");
        for (Integer corrDim : clusterMap.keySet()) {
          List<HierarchicalCorrelationCluster> correlationClusters = clusterMap.get(corrDim);
          for (HierarchicalCorrelationCluster cluster : correlationClusters) {
            msg.append("\n  cluster " + cluster + ", ids: " + cluster.getIDs().size() + ", level: " + cluster.getLevel() + ", index: " + cluster.getLevelIndex());
            for (int i = 0; i < cluster.getParents().size(); i++) {
              msg.append("\n   parent " + cluster.getParents().get(i));
            }
            for (int i = 0; i < cluster.numChildren(); i++) {
              msg.append("\n   child " + cluster.getChild(i));
            }
          }
        }
        debugFine(msg.toString());
      }

      //todo
      HierarchicalCorrelationCluster rootCluster = clusterMap.get(dimensionality).get(0);
      result = new HierarchicalCorrelationClusters<RealVector>(rootCluster, database);
    }
    catch (ParameterException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Returns the result of the algorithm.
   *
   * @return the result of the algorithm
   */
  public Result<RealVector> getResult() {
    return result;
  }


  /**
   * Returns a description of the algorithm.
   *
   * @return a description of the algorithm
   */
  public Description getDescription() {
    return new Description(
        "XXX",
        "Hierarchical COPAC",
        "Performs the COPAC algorithm on the data and builds " +
        "a hierarchy that allows multiple inheritance from the clustering result.",
        "unpublished");
  }

  /**
   * Passes remaining parameters to the clustering algorithm.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // copac algorithm
    copacAlgorithm = new COPAC();
    remainingParameters = copacAlgorithm.setParameters(remainingParameters);
    copacAlgorithm.setTime(isTime());
    copacAlgorithm.setVerbose(isVerbose());
    setParameters(args, remainingParameters);

    return remainingParameters;
  }

  /**
   * Extracts the correlation clusters and noise from the copac result
   * and returns a mapping of correlation dimension to maps of clusters within
   * this correlation dimension. Each cluster is defined by the basis vectors defining
   * the subspace in which the cluster appears.
   *
   * @param database       the database containing the objects
   * @param dimensionality the dimensionality of the feature space
   * @return a mapping of correlation dimension to maps of clusters
   */
  private SortedMap<Integer, List<HierarchicalCorrelationCluster>> extractCorrelationClusters(Database<RealVector> database,
                                                                                              int dimensionality) {
    try {
      // result
      SortedMap<Integer, List<HierarchicalCorrelationCluster>> clusterMap = new TreeMap<Integer, List<HierarchicalCorrelationCluster>>();

      // result of COPAC algorithm
      PartitionClusteringResults<RealVector> copacResult = (PartitionClusteringResults<RealVector>) copacAlgorithm.getResult();
      Integer noiseDimension = copacResult.getNoiseID();
      // noise cluster containing all noise objects over all partitions
      List<Integer> noiseIDs = new ArrayList<Integer>();

      // iterate over correlation dimensions
      for (Iterator<Integer> it = copacResult.partitionsIterator(); it.hasNext();) {
        Integer correlationDimension = it.next();
        ClusteringResult<RealVector> clusteringResult = copacResult.getResult(correlationDimension);

        // clusters and noise within one corrDim
        if (noiseDimension == null || correlationDimension != noiseDimension) {
          List<HierarchicalCorrelationCluster> correlationClusters = new ArrayList<HierarchicalCorrelationCluster>();
          clusterMap.put(correlationDimension, correlationClusters);
          Map<SimpleClassLabel, Database<RealVector>> clustering = clusteringResult.clustering(SimpleClassLabel.class);
          // clusters
          for (Database<RealVector> db : clustering.values()) {
            // run pca
            LocalPCA pca = new LinearLocalPCA();
            pca.setParameters(pcaParameters(correlationDimension));
            pca.run(Util.getDatabaseIDs(db), db);
//            System.out.println("\ncorrDIM "+corrDim);
//            System.out.println("ew "+ Util.format(pca.getEigenvalues(), ",",6));
//            System.out.println("ev "+ pca.getEigenvectors().toString(NF));
//            System.out.println("strong ev "+ pca.getStrongEigenvectors().toString(NF));
//            System.out.println("weak ev "+ pca.getWeakEigenvectors().toString(NF));
            // create a new correlation cluster
            List<Integer> ids = new ArrayList<Integer>(Util.getDatabaseIDs(db));
            HierarchicalCorrelationCluster correlationCluster = new HierarchicalCorrelationCluster(pca, ids,
                                                                                                   "[" + correlationDimension + "_" + correlationClusters.size() + "]",
                                                                                                   dimensionality - correlationDimension,
                                                                                                   correlationClusters.size());
            // put cluster to result
            correlationClusters.add(correlationCluster);
          }
          // noise
          Database<RealVector> noiseDB = clusteringResult.noise();
          noiseIDs.addAll(Util.getDatabaseIDs(noiseDB));
        }

        // partition containing noise
        else {
          // clusters in noise partition
          Map<SimpleClassLabel, Database<RealVector>> clustering = clusteringResult.clustering(SimpleClassLabel.class);
          for (Database<RealVector> db : clustering.values()) {
            noiseIDs.addAll(Util.getDatabaseIDs(db));
          }
          // noise in noise partition
          Database<RealVector> noiseDB = clusteringResult.noise();
          noiseIDs.addAll(Util.getDatabaseIDs(noiseDB));
        }
      }

      // create noise cluster containing all noise objects over all partitions
      LocalPCA pca = new LinearLocalPCA();
      pca.setParameters(pcaParameters(dimensionality));
      pca.run(noiseIDs, database);
      List<HierarchicalCorrelationCluster> noiseClusters = new ArrayList<HierarchicalCorrelationCluster>();
      HierarchicalCorrelationCluster noiseCluster = new HierarchicalCorrelationCluster(pca,
                                                                                       noiseIDs,
                                                                                       "[noise]", 0, 0);

      noiseClusters.add(noiseCluster);
      clusterMap.put(dimensionality, noiseClusters);


      return clusterMap;
    }

    catch (ParameterException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Returns the parameters for the PCA for the specified correlation dimension.
   *
   * @param correlationDimension the correlation dimension
   * @return the parameters for the PCA for the specified correlation dimension
   */
  private String[] pcaParameters(int correlationDimension) {
    List<String> parameters = new ArrayList<String>();

    // eigenpair filter
    parameters.add(OptionHandler.OPTION_PREFIX + GlobalPCA.EIGENPAIR_FILTER_P);
    parameters.add(FirstNEigenPairFilter.class.getName());

    // n
    parameters.add(OptionHandler.OPTION_PREFIX + FirstNEigenPairFilter.N_P);
    parameters.add(Integer.toString(correlationDimension));

    return parameters.toArray(new String[parameters.size()]);
  }

  private void buildHierarchy(int dimensionality,
                              SortedMap<Integer, List<HierarchicalCorrelationCluster>> clusterMap) throws ParameterException {

    StringBuffer msg = new StringBuffer();
    PCABasedCorrelationDistanceFunction distanceFunction = new PCABasedCorrelationDistanceFunction();
    distanceFunction.setParameters(new String[0]);
    for (Integer childCorrDim : clusterMap.keySet()) {
      List<HierarchicalCorrelationCluster> children = clusterMap.get(childCorrDim);
      SortedMap<Integer, List<HierarchicalCorrelationCluster>> parentMap = clusterMap.tailMap(childCorrDim + 1);
      if (debug) {
        msg.append("\n\ncorrdim " + childCorrDim);
        msg.append("\nparents " + parentMap.keySet());
      }

      for (HierarchicalCorrelationCluster child : children) {
        for (Integer parentCorrDim : parentMap.keySet()) {
          List<HierarchicalCorrelationCluster> parents = parentMap.get(parentCorrDim);
          for (HierarchicalCorrelationCluster parent : parents) {
            int subspaceDim_parent = dimensionality - parent.getLevel();
            System.out.println("\nsubspaceDim_parent(" + parent + ") = " + subspaceDim_parent);
            System.out.println("subspaceDim_child(" + child + ") = " + (dimensionality - child.getLevel()));
            int corrDist = distanceFunction.correlationDistance(child.getPCA(), parent.getPCA(), dimensionality);
            System.out.println("corrDist(" + child + " - " + parent + ") = " + corrDist);
            if (corrDist == subspaceDim_parent && (child.getParents().isEmpty() || ! isParent(dimensionality, distanceFunction, parent, child.getParents())))
            {
              parent.addChild(child);
              child.addParent(parent);
              if (debug) {
                msg.append("\n" + parent + " is parent of " + child);
              }
            }
          }
        }
      }
    }
    if (debug) {
      debugFiner(msg.toString());
    }
  }

  /**
   * Returns true, if the specified parent cluster is a parent of one child of the children clusters.
   *
   * @param dimensionality   the dimensionality of the data
   * @param distanceFunction the distance function for distance computation between the clusters
   * @param parent           the parent to be tested
   * @param children         the list of children to be tested
   * @return true, if the specified parent cluster is a parent of one child of the children clusters,
   *         false otherwise
   */
  private boolean isParent(int dimensionality,
                           PCABasedCorrelationDistanceFunction distanceFunction,
                           HierarchicalCorrelationCluster parent,
                           List<HierarchicalCorrelationCluster> children) {

    StringBuffer msg = new StringBuffer();
    int subspaceDim_parent = dimensionality - parent.getLevel();

    for (HierarchicalCorrelationCluster child : children) {
      int corrDist = distanceFunction.correlationDistance(child.getPCA(), parent.getPCA(), dimensionality);
      if (debug) {
        msg.append("\ncorrDist(" + child + " - " + parent + ") = " + corrDist);

      }
      if (corrDist == subspaceDim_parent) {
        if (debug) {
          debugFine(msg.toString());
        }
        return true;
      }
    }

    if (debug) {
      debugFiner(msg.toString());
    }
    return false;
  }

}
