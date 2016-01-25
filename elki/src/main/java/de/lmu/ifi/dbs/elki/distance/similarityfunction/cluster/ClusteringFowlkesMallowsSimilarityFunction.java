package de.lmu.ifi.dbs.elki.distance.similarityfunction.cluster;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.database.query.DistanceSimilarityQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceSimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.AbstractPrimitiveSimilarityFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.NormalizedSimilarityFunction;
import de.lmu.ifi.dbs.elki.evaluation.clustering.ClusterContingencyTable;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Measure the similarity of clusters via the Fowlkes-Mallows Index.
 *
 * Reference:
 * <p>
 * Fowlkes, E.B. and Mallows, C.L.<br />
 * A method for comparing two hierarchical clusterings<br />
 * In: Journal of the American Statistical Association, Vol. 78 Issue 383
 * </p>
 *
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
@Reference(authors = "Fowlkes, E.B. and Mallows, C.L.", //
title = "A method for comparing two hierarchical clusterings", //
booktitle = "Journal of the American Statistical Association, Vol. 78 Issue 383")
public class ClusteringFowlkesMallowsSimilarityFunction extends AbstractPrimitiveSimilarityFunction<Clustering<?>>implements ClusteringDistanceSimilarityFunction, NormalizedSimilarityFunction<Clustering<?>> {
  /**
   * Static instance.
   */
  public static final ClusteringFowlkesMallowsSimilarityFunction STATIC = new ClusteringFowlkesMallowsSimilarityFunction();

  /**
   * Constructor - use the static instance {@link #STATIC}!
   */
  public ClusteringFowlkesMallowsSimilarityFunction() {
    super();
  }

  @Override
  public double similarity(Clustering<?> o1, Clustering<?> o2) {
    ClusterContingencyTable ct = new ClusterContingencyTable(false, true);
    ct.process(o1, o2);
    return ct.getPaircount().fowlkesMallows();
  }

  @Override
  public double distance(Clustering<?> o1, Clustering<?> o2) {
    ClusterContingencyTable ct = new ClusterContingencyTable(false, true);
    ct.process(o1, o2);
    return 1. - ct.getPaircount().fowlkesMallows();
  }

  @Override
  public boolean isMetric() {
    return false;
  }

  @Override
  public <T extends Clustering<?>> DistanceSimilarityQuery<T> instantiate(Relation<T> relation) {
    return new PrimitiveDistanceSimilarityQuery<>(relation, this, this);
  }

  @Override
  public SimpleTypeInformation<? super Clustering<?>> getInputTypeRestriction() {
    return new SimpleTypeInformation<>(Clustering.class);
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   *
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected ClusteringFowlkesMallowsSimilarityFunction makeInstance() {
      return STATIC;
    }
  }
}