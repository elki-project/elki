package de.lmu.ifi.dbs.elki.algorithm.clustering.optics;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.distance.DoubleDistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.UpdatableHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DistanceParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * OPTICS provides the OPTICS algorithm.
 * <p>
 * Reference: M. Ankerst, M. Breunig, H.-P. Kriegel, and J. Sander: OPTICS:
 * Ordering Points to Identify the Clustering Structure. <br>
 * In: Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99).
 * </p>
 * 
 * @author Elke Achtert
 * @param <O> the type of DatabaseObjects handled by the algorithm
 * @param <D> the type of Distance used to discern objects
 */
@Title("OPTICS: Density-Based Hierarchical Clustering")
@Description("Algorithm to find density-connected sets in a database based on the parameters 'minPts' and 'epsilon' (specifying a volume). These two parameters determine a density threshold for clustering.")
@Reference(authors = "M. Ankerst, M. Breunig, H.-P. Kriegel, and J. Sander", title = "OPTICS: Ordering Points to Identify the Clustering Structure", booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99)", url = "http://dx.doi.org/10.1145/304181.304187")
@Alias({ "OPTICS", "de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS" })
public class OPTICS<O> extends AbstractDistanceBasedAlgorithm<O, DoubleDistance, ClusterOrderResult<DoubleDistanceClusterOrderEntry>> implements OPTICSTypeAlgorithm<DoubleDistanceClusterOrderEntry> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(OPTICS.class);

  /**
   * Hold the value of {@link #EPSILON_ID}.
   */
  private DoubleDistance epsilon;

  /**
   * Holds the value of {@link #MINPTS_ID}.
   */
  private int minpts;

  /**
   * Holds a set of processed ids.
   */
  private ModifiableDBIDs processedIDs;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param epsilon Epsilon value
   * @param minpts Minpts value
   */
  public OPTICS(DistanceFunction<? super O, DoubleDistance> distanceFunction, DoubleDistance epsilon, int minpts) {
    super(distanceFunction);
    this.epsilon = epsilon != null ? epsilon : DoubleDistance.INFINITE_DISTANCE;
    this.minpts = minpts;
  }

  /**
   * Run OPTICS on the database.
   * 
   * @param database Database
   * @param relation Relation
   * @return Result
   */
  public ClusterOrderResult<DoubleDistanceClusterOrderEntry> run(Relation<O> relation) {
    RangeQuery<O, DoubleDistance> rangeQuery = QueryUtil.getRangeQuery(relation, getDistanceFunction(), epsilon);

    int size = relation.size();
    final FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("OPTICS", size, LOG) : null;

    processedIDs = DBIDUtil.newHashSet(size);
    ClusterOrderResult<DoubleDistanceClusterOrderEntry> clusterOrder = new ClusterOrderResult<>("OPTICS Clusterorder", "optics-clusterorder");

    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      if(!processedIDs.contains(iditer)) {
        expandClusterOrderDouble(clusterOrder, relation, rangeQuery, DBIDUtil.deref(iditer), epsilon, progress);
      }
    }
    if(progress != null) {
      progress.ensureCompleted(LOG);
    }

    return clusterOrder;
  }

  /**
   * OPTICS-function expandClusterOrder.
   * 
   * @param clusterOrder Cluster order result to expand
   * @param database the database on which the algorithm is run
   * @param rangeQuery the range query to use
   * @param objectID the currently processed object
   * @param epsilon Query epsilon
   * @param progress the progress object to actualize the current progress if
   *        the algorithm
   */
  protected void expandClusterOrderDouble(ClusterOrderResult<DoubleDistanceClusterOrderEntry> clusterOrder, Relation<O> database, RangeQuery<O, DoubleDistance> rangeQuery, DBID objectID, DoubleDistance epsilon, FiniteProgress progress) {
    UpdatableHeap<DoubleDistanceClusterOrderEntry> heap = new UpdatableHeap<>();
    heap.add(new DoubleDistanceClusterOrderEntry(objectID, null, Double.POSITIVE_INFINITY));

    while(!heap.isEmpty()) {
      final DoubleDistanceClusterOrderEntry current = heap.poll();
      clusterOrder.add(current);
      processedIDs.add(current.getID());

      DistanceDBIDList<DoubleDistance> neighbors = rangeQuery.getRangeForDBID(current.getID(), epsilon);
      if(neighbors.size() >= minpts) {
        final DistanceDBIDPair<DoubleDistance> last = neighbors.get(minpts - 1);
        if(last instanceof DoubleDistanceDBIDPair) {
          double coreDistance = ((DoubleDistanceDBIDPair) last).doubleDistance();

          for(DistanceDBIDListIter<DoubleDistance> neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
            if(processedIDs.contains(neighbor)) {
              continue;
            }
            double reachability = Math.max(((DoubleDistanceDBIDListIter) neighbor).doubleDistance(), coreDistance);
            heap.add(new DoubleDistanceClusterOrderEntry(DBIDUtil.deref(neighbor), current.getID(), reachability));
          }
        }
        else {
          // Actually we have little gains in this situation,
          // Only if we got an optimized result before.
          double coreDistance = last.getDistance().doubleValue();

          for(DistanceDBIDListIter<DoubleDistance> neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
            if(processedIDs.contains(neighbor)) {
              continue;
            }
            double reachability = Math.max(neighbor.getDistance().doubleValue(), coreDistance);
            heap.add(new DoubleDistanceClusterOrderEntry(DBIDUtil.deref(neighbor), current.getID(), reachability));
          }
        }
      }
      if(progress != null) {
        progress.setProcessed(processedIDs.size(), LOG);
      }
    }
  }

  @Override
  public int getMinPts() {
    return minpts;
  }

  @Override
  public Class<? super DoubleDistanceClusterOrderEntry> getEntryType() {
    return DoubleDistanceClusterOrderEntry.class;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, DoubleDistance> {
    /**
     * Parameter to specify the maximum radius of the neighborhood to be
     * considered, must be suitable to the distance function specified.
     */
    public static final OptionID EPSILON_ID = new OptionID("optics.epsilon", "The maximum radius of the neighborhood to be considered.");

    /**
     * Parameter to specify the threshold for minimum number of points in the
     * epsilon-neighborhood of a point, must be an integer greater than 0.
     */
    public static final OptionID MINPTS_ID = new OptionID("optics.minpts", "Threshold for minimum number of points in the epsilon-neighborhood of a point.");

    protected DoubleDistance epsilon = null;

    protected int minpts = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DistanceParameter<DoubleDistance> epsilonP = new DistanceParameter<>(EPSILON_ID, distanceFunction, true);
      if(config.grab(epsilonP)) {
        epsilon = epsilonP.getValue();
      }

      IntParameter minptsP = new IntParameter(MINPTS_ID);
      minptsP.addConstraint(CommonConstraints.GREATER_EQUAL_ONE_INT);
      if(config.grab(minptsP)) {
        minpts = minptsP.intValue();
      }
    }

    @Override
    protected OPTICS<O> makeInstance() {
      return new OPTICS<>(distanceFunction, epsilon, minpts);
    }
  }
}