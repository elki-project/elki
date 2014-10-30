package de.lmu.ifi.dbs.elki.algorithm.clustering.optics;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDListIter;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDPair;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * The OPTICS algorithm for density-based hierarchical clustering.
 * 
 * Reference:
 * <p>
 * M. Ankerst, M. Breunig, H.-P. Kriegel, and J. Sander:<br />
 * OPTICS: Ordering Points to Identify the Clustering Structure. <br/>
 * In: Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99).
 * </p>
 * 
 * @author Elke Achtert
 * @param <O> the type of DatabaseObjects handled by the algorithm
 */
@Title("OPTICS: Density-Based Hierarchical Clustering")
@Description("Algorithm to find density-connected sets in a database based on the parameters 'minPts' and 'epsilon' (specifying a volume). These two parameters determine a density threshold for clustering.")
@Reference(authors = "M. Ankerst, M. Breunig, H.-P. Kriegel, and J. Sander", //
title = "OPTICS: Ordering Points to Identify the Clustering Structure", //
booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD '99)", //
url = "http://dx.doi.org/10.1145/304181.304187")
@Alias({ "OPTICS", "de.lmu.ifi.dbs.elki.algorithm.clustering.OPTICS" })
public class OPTICS<O> extends AbstractDistanceBasedAlgorithm<O, ClusterOrderResult<DoubleDistanceClusterOrderEntry>> implements OPTICSTypeAlgorithm<DoubleDistanceClusterOrderEntry> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(OPTICS.class);

  /**
   * Holds the maximum distance to search for objects (performance parameter)
   */
  private double epsilon;

  /**
   * The density threshold, in number of points.
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
  public OPTICS(DistanceFunction<? super O> distanceFunction, double epsilon, int minpts) {
    super(distanceFunction);
    this.epsilon = epsilon;
    this.minpts = minpts;
  }

  /**
   * Run OPTICS on the database.
   * 
   * @param relation Relation
   * @return Result
   */
  public ClusterOrderResult<DoubleDistanceClusterOrderEntry> run(Relation<O> relation) {
    RangeQuery<O> rangeQuery = QueryUtil.getRangeQuery(relation, getDistanceFunction(), epsilon);
    DBIDs ids = relation.getDBIDs();

    int size = ids.size();
    final FiniteProgress progress = LOG.isVerbose() ? new FiniteProgress("OPTICS", size, LOG) : null;

    processedIDs = DBIDUtil.newHashSet(size);
    ClusterOrderResult<DoubleDistanceClusterOrderEntry> clusterOrder = new ClusterOrderResult<>(relation.getDatabase(), ids, "OPTICS Clusterorder", "optics-clusterorder");

    for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
      if(!processedIDs.contains(iditer)) {
        expandClusterOrder(clusterOrder, relation, rangeQuery, DBIDUtil.deref(iditer), epsilon, progress);
      }
    }
    LOG.ensureCompleted(progress);

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
  protected void expandClusterOrder(ClusterOrderResult<DoubleDistanceClusterOrderEntry> clusterOrder, Relation<O> database, RangeQuery<O> rangeQuery, DBID objectID, double epsilon, FiniteProgress progress) {
    UpdatableHeap<DoubleDistanceClusterOrderEntry> heap = new UpdatableHeap<>();
    heap.add(new DoubleDistanceClusterOrderEntry(objectID, null, Double.POSITIVE_INFINITY));

    while(!heap.isEmpty()) {
      final DoubleDistanceClusterOrderEntry current = heap.poll();
      clusterOrder.add(current);
      processedIDs.add(current.getID());

      DoubleDBIDList neighbors = rangeQuery.getRangeForDBID(current.getID(), epsilon);
      if(neighbors.size() >= minpts) {
        final DoubleDBIDPair last = neighbors.get(minpts - 1);
        double coreDistance = last.doubleValue();

        for(DoubleDBIDListIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
          if(processedIDs.contains(neighbor)) {
            continue;
          }
          double reachability = Math.max(neighbor.doubleValue(), coreDistance);
          heap.add(new DoubleDistanceClusterOrderEntry(DBIDUtil.deref(neighbor), current.getID(), reachability));
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
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
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

    protected double epsilon = Double.POSITIVE_INFINITY;

    protected int minpts = 0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      DoubleParameter epsilonP = new DoubleParameter(EPSILON_ID);
      epsilonP.setOptional(true);
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