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
package de.lmu.ifi.dbs.elki.algorithm.clustering;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.PrototypeModel;
import de.lmu.ifi.dbs.elki.data.model.SimplePrototypeModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DoubleDBIDList;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.logging.statistics.LongStatistic;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Leader clustering algorithm.
 * <p>
 * Reference:
 * <p>
 * J. A. Hartigan<br>
 * Clustering algorithms, Chapter 3, Quick Partition Algorithms
 * <p>
 * This implementation does not use the linear process described, but uses index
 * structures. This may or may not be faster.
 * <p>
 * TODO: when no index is available, fall back to the original approach.
 * 
 * @author Erich Schubert
 * @since 0.7.5
 *
 * @param <O> Object type
 */
@Reference(authors = "J. A. Hartigan", //
    title = "Chapter 3: Quick Partition Algorithms, 3.2 Leader Algorithm", //
    booktitle = "Clustering algorithms", // )
    url = "http://dl.acm.org/citation.cfm?id=540298", //
    bibkey = "books/wiley/Hartigan75/C3")
public class Leader<O> extends AbstractDistanceBasedAlgorithm<O, Clustering<PrototypeModel<O>>> implements ClusteringAlgorithm<Clustering<PrototypeModel<O>>> {
  /**
   * Maximum distance from leading object,
   */
  private double threshold;

  /**
   * Constructor.
   *
   * @param distanceFunction Distance function
   * @param threshold Maximum distance from leading object
   */
  public Leader(DistanceFunction<? super O> distanceFunction, double threshold) {
    super(distanceFunction);
    this.threshold = threshold;
  }

  /**
   * Run the leader clustering algorithm.
   * 
   * @param relation Data set
   * @return Clustering result
   */
  public Clustering<PrototypeModel<O>> run(Relation<O> relation) {
    RangeQuery<O> rq = relation.getRangeQuery(getDistanceFunction(), threshold);

    ModifiableDBIDs seen = DBIDUtil.newHashSet(relation.size());
    Clustering<PrototypeModel<O>> clustering = new Clustering<>("Prototype clustering", "prototype-clustering");

    int queries = 0;
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Leader clustering", relation.size(), LOG) : null;
    for(DBIDIter it = relation.iterDBIDs(); it.valid() && seen.size() < relation.size(); it.advance()) {
      if(seen.contains(it)) {
        continue;
      }
      DoubleDBIDList res = rq.getRangeForDBID(it, threshold);
      ++queries;
      ModifiableDBIDs ids = DBIDUtil.newArray(res.size());
      for(DBIDIter cand = res.iter(); cand.valid(); cand.advance()) {
        if(seen.add(cand)) {
          LOG.incrementProcessed(prog);
          ids.add(cand);
        }
      }
      assert (ids.size() > 0 && ids.contains(it));
      PrototypeModel<O> mod = new SimplePrototypeModel<>(relation.get(it));
      clustering.addToplevelCluster(new Cluster<>(ids, mod));
    }
    LOG.statistics(new LongStatistic(this.getClass().getName() + ".queries", queries));
    LOG.ensureCompleted(prog);
    return clustering;
  }

  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(Leader.class);

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
   * @hidden
   *
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Option ID of threshold parameter.
     */
    public static final OptionID THRESHOLD_ID = new OptionID("leader.threshold", "Maximum distance from leading object.");

    /**
     * Maximum distance from leading object,
     */
    private double threshold;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DoubleParameter thresholdP = new DoubleParameter(THRESHOLD_ID);
      if(config.grab(thresholdP)) {
        threshold = thresholdP.doubleValue();
      }
    }

    @Override
    protected Leader<O> makeInstance() {
      return new Leader<O>(distanceFunction, threshold);
    }
  }
}
