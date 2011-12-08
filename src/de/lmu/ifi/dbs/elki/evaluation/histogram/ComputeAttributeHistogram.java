package de.lmu.ifi.dbs.elki.evaluation.histogram;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.histograms.FlexiHistogram;
import de.lmu.ifi.dbs.elki.result.DataDistributionResult;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.iterator.IterableIterator;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;

/**
 * Compute attribute-wise histograms for the data set.
 * 
 * @author Erich Schubert
 */
public class ComputeAttributeHistogram implements Evaluator {
  /**
   * Number of bins
   */
  int bins = 20;

  /**
   * Constructor.
   * 
   * @param bins Number of bins
   */
  public ComputeAttributeHistogram(int bins) {
    super();
    this.bins = bins;
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    ResultHierarchy hier = baseResult.getHierarchy();
    IterableIterator<Relation<?>> i = ResultUtil.filteredResults(newResult, Relation.class);
    for(Relation<?> rel : i) {
      if(TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(rel.getDataTypeInformation())) {
        @SuppressWarnings("unchecked")
        final Relation<? extends NumberVector<?, ?>> nrel = (Relation<? extends NumberVector<?, ?>>) rel;
        processRelation(hier, nrel);
      }
    }
  }

  /**
   * Process a single relation.
   * 
   * @param hier Result hierarchy
   * @param relation Relation to analyze
   */
  private void processRelation(ResultHierarchy hier, Relation<? extends NumberVector<?, ?>> relation) {
    final int dim = DatabaseUtil.dimensionality(relation);
    final double frac = 1. / relation.size();
    // Create data storage
    ArrayList<DoubleMinMax> minmax = new ArrayList<DoubleMinMax>(dim);
    ArrayList<FlexiHistogram<Double, Double>> histograms = new ArrayList<FlexiHistogram<Double, Double>>(dim);
    ArrayList<LinearScale> scales = new ArrayList<LinearScale>(dim);
    for(int i = 0; i < dim; i++) {
      minmax.add(new DoubleMinMax());
      histograms.add(FlexiHistogram.DoubleSumHistogram(bins));
    }

    // Iterate over the data set
    for(DBID id : relation.iterDBIDs()) {
      NumberVector<?, ?> vec = relation.get(id);
      for(int d = 0; d < dim; d++) {
        double pos = vec.doubleValue(d + 1);
        minmax.get(d).put(pos);
        histograms.get(d).aggregate(pos, frac);
      }
    }
    // Assign scales
    for(int i = 0; i < dim; i++) {
      DoubleMinMax mm = minmax.get(i);
      scales.add(new LinearScale(mm.getMin(), mm.getMax()));
    }

    DataDistributionResult r = new DataDistributionResult(minmax, scales, histograms);
    hier.add(relation, r);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Number of bins for automatically generated histograms.
     */
    public static final OptionID BINS_ID = OptionID.getOrCreateOptionID("histogram.bins", "Number of bins for the distribution histogram.");

    /**
     * Number of bins to use.
     */
    int bins = 20;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter binsP = new IntParameter(BINS_ID, 20);
      binsP.addConstraint(new GreaterConstraint(0));
      if(config.grab(binsP)) {
        bins = binsP.getValue();
      }
    }

    @Override
    protected ComputeAttributeHistogram makeInstance() {
      return new ComputeAttributeHistogram(bins);
    }
  }
}