package de.lmu.ifi.dbs.elki.algorithm.clustering;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.QueryUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDistanceDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceDBIDResult;
import de.lmu.ifi.dbs.elki.database.query.DistanceDBIDResultIter;
import de.lmu.ifi.dbs.elki.database.query.DoubleDistanceDBIDResultIter;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.result.optics.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.result.optics.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.result.optics.DoubleDistanceClusterOrderEntry;
import de.lmu.ifi.dbs.elki.result.optics.GenericClusterOrderEntry;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.UpdatableHeap;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
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
public class OPTICS<O, D extends Distance<D>> extends AbstractDistanceBasedAlgorithm<O, D, ClusterOrderResult<D>> implements OPTICSTypeAlgorithm<D> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(OPTICS.class);

  /**
   * Parameter to specify the maximum radius of the neighborhood to be
   * considered, must be suitable to the distance function specified.
   */
  public static final OptionID EPSILON_ID = OptionID.getOrCreateOptionID("optics.epsilon", "The maximum radius of the neighborhood to be considered.");

  /**
   * Parameter to specify the threshold for minimum number of points in the
   * epsilon-neighborhood of a point, must be an integer greater than 0.
   */
  public static final OptionID MINPTS_ID = OptionID.getOrCreateOptionID("optics.minpts", "Threshold for minimum number of points in the epsilon-neighborhood of a point.");

  /**
   * Hold the value of {@link #EPSILON_ID}.
   */
  private D epsilon;

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
  public OPTICS(DistanceFunction<? super O, D> distanceFunction, D epsilon, int minpts) {
    super(distanceFunction);
    this.epsilon = epsilon;
    this.minpts = minpts;
  }

  /**
   * Run OPTICS on the database.
   * 
   * @param database Database
   * @param relation Relation
   * @return Result
   */
  public ClusterOrderResult<D> run(Database database, Relation<O> relation) {
    // Default value is infinite distance
    if(epsilon == null) {
      epsilon = getDistanceFunction().getDistanceFactory().infiniteDistance();
    }
    RangeQuery<O, D> rangeQuery = QueryUtil.getRangeQuery(relation, getDistanceFunction(), epsilon);

    int size = relation.size();
    final FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("OPTICS", size, logger) : null;

    processedIDs = DBIDUtil.newHashSet(size);
    ClusterOrderResult<D> clusterOrder = new ClusterOrderResult<D>("OPTICS Clusterorder", "optics-clusterorder");

    if(getDistanceFunction() instanceof PrimitiveDoubleDistanceFunction && DoubleDistance.class.isInstance(epsilon)) {
      // Optimized codepath for double-based distances. Avoids Java
      // boxing/unboxing.
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        if(!processedIDs.contains(iditer)) {
          // We need to do some ugly casts to be able to run the optimized version, unfortunately.
          @SuppressWarnings("unchecked")
          final ClusterOrderResult<DoubleDistance> doubleClusterOrder = ClusterOrderResult.class.cast(clusterOrder);
          @SuppressWarnings("unchecked")
          final RangeQuery<O, DoubleDistance> doubleRangeQuery = RangeQuery.class.cast(rangeQuery);
          final DoubleDistance depsilon = DoubleDistance.class.cast(epsilon);
          expandClusterOrderDouble(doubleClusterOrder, database, doubleRangeQuery, DBIDUtil.deref(iditer), depsilon, progress);
        }
      }
    }
    else {
      for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
        if(!processedIDs.contains(iditer)) {
          expandClusterOrder(clusterOrder, database, rangeQuery, DBIDUtil.deref(iditer), epsilon, progress);
        }
      }
    }
    if(progress != null) {
      progress.ensureCompleted(logger);
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
   * @param epsilon Epsilon range value
   * @param progress the progress object to actualize the current progress if
   *        the algorithm
   */
  protected void expandClusterOrder(ClusterOrderResult<D> clusterOrder, Database database, RangeQuery<O, D> rangeQuery, DBID objectID, D epsilon, FiniteProgress progress) {
    UpdatableHeap<ClusterOrderEntry<D>> heap = new UpdatableHeap<ClusterOrderEntry<D>>();
    heap.add(new GenericClusterOrderEntry<D>(objectID, null, getDistanceFunction().getDistanceFactory().infiniteDistance()));

    while(!heap.isEmpty()) {
      final ClusterOrderEntry<D> current = heap.poll();
      clusterOrder.add(current);
      processedIDs.add(current.getID());

      DistanceDBIDResult<D> neighbors = rangeQuery.getRangeForDBID(current.getID(), epsilon);
      if(neighbors.size() >= minpts) {
        final DistanceDBIDPair<D> last = neighbors.get(minpts - 1);
        D coreDistance = last.getDistance();

        for(DistanceDBIDResultIter<D> neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
          if(processedIDs.contains(neighbor)) {
            continue;
          }
          D reachability = DistanceUtil.max(neighbor.getDistance(), coreDistance);
          heap.add(new GenericClusterOrderEntry<D>(DBIDUtil.deref(neighbor), current.getID(), reachability));
        }
      }
      if(progress != null) {
        progress.setProcessed(processedIDs.size(), logger);
      }
    }
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
  protected void expandClusterOrderDouble(ClusterOrderResult<DoubleDistance> clusterOrder, Database database, RangeQuery<O, DoubleDistance> rangeQuery, DBID objectID, DoubleDistance epsilon, FiniteProgress progress) {
    UpdatableHeap<DoubleDistanceClusterOrderEntry> heap = new UpdatableHeap<DoubleDistanceClusterOrderEntry>();
    heap.add(new DoubleDistanceClusterOrderEntry(objectID, null, Double.POSITIVE_INFINITY));

    while(!heap.isEmpty()) {
      final DoubleDistanceClusterOrderEntry current = heap.poll();
      clusterOrder.add(current);
      processedIDs.add(current.getID());

      DistanceDBIDResult<DoubleDistance> neighbors = rangeQuery.getRangeForDBID(current.getID(), epsilon);
      if(neighbors.size() >= minpts) {
        final DistanceDBIDPair<DoubleDistance> last = neighbors.get(minpts - 1);
        if(last instanceof DoubleDistanceDBIDPair) {
          double coreDistance = ((DoubleDistanceDBIDPair) last).doubleDistance();

          for(DistanceDBIDResultIter<DoubleDistance> neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
            if(processedIDs.contains(neighbor)) {
              continue;
            }
            double reachability = Math.max(((DoubleDistanceDBIDResultIter) neighbor).doubleDistance(), coreDistance);
            heap.add(new DoubleDistanceClusterOrderEntry(DBIDUtil.deref(neighbor), current.getID(), reachability));
          }
        }
        else {
          // Actually we have little gains in this situation,
          // Only if we got an optimized result before.
          double coreDistance = last.getDistance().doubleValue();

          for(DistanceDBIDResultIter<DoubleDistance> neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
            if(processedIDs.contains(neighbor)) {
              continue;
            }
            double reachability = Math.max(neighbor.getDistance().doubleValue(), coreDistance);
            heap.add(new DoubleDistanceClusterOrderEntry(DBIDUtil.deref(neighbor), current.getID(), reachability));
          }
        }
      }
      if(progress != null) {
        progress.setProcessed(processedIDs.size(), logger);
      }
    }
  }

  @Override
  public int getMinPts() {
    return minpts;
  }

  @Override
  public D getDistanceFactory() {
    return getDistanceFunction().getDistanceFactory();
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O, D extends Distance<D>> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, D> {
    protected D epsilon = null;

    protected int minpts = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DistanceParameter<D> epsilonP = new DistanceParameter<D>(EPSILON_ID, distanceFunction, true);
      if(config.grab(epsilonP)) {
        epsilon = epsilonP.getValue();
      }

      IntParameter minptsP = new IntParameter(MINPTS_ID, new GreaterConstraint(0));
      if(config.grab(minptsP)) {
        minpts = minptsP.getValue();
      }
    }

    @Override
    protected OPTICS<O, D> makeInstance() {
      return new OPTICS<O, D>(distanceFunction, epsilon, minpts);
    }
  }
}