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
package elki.clustering;

import elki.algorithm.AbstractDistanceBasedAlgorithm;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.PrototypeModel;
import elki.data.model.SimplePrototypeModel;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DoubleDBIDList;
import elki.database.ids.ModifiableDBIDs;
import elki.database.query.range.RangeQuery;
import elki.database.relation.Relation;
import elki.distance.distancefunction.Distance;
import elki.logging.Logging;
import elki.logging.progress.FiniteProgress;
import elki.logging.statistics.LongStatistic;
import elki.result.Metadata;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.DoubleParameter;

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
  public Leader(Distance<? super O> distanceFunction, double threshold) {
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
    RangeQuery<O> rq = relation.getRangeQuery(getDistance(), threshold);

    ModifiableDBIDs seen = DBIDUtil.newHashSet(relation.size());
    Clustering<PrototypeModel<O>> clustering = new Clustering<>();
    Metadata.of(clustering).setLongName("Leader Clustering");

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
    return TypeUtil.array(getDistance().getInputTypeRestriction());
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
