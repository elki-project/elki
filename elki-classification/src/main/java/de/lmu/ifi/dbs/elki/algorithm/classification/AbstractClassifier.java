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
package de.lmu.ifi.dbs.elki.algorithm.classification;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Abstract base class for algorithms.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @param <O> Input type
 * @param <R> Result type
 */
public abstract class AbstractClassifier<O, R extends Result> extends AbstractAlgorithm<R> implements Classifier<O> {
  @Override
  @Deprecated
  public R run(Database database) {
    throw new AbortException("Classifiers cannot auto-run on a database, but need to be trained and can then predict.");
  }

  /**
   * Align the labels for a label query.
   * 
   * @param l1 List of reference labels
   * @param d1 Probabilities for these labels
   * @param l2 List of requested labels
   * @return Probabilities in desired output order
   */
  protected double[] alignLabels(List<ClassLabel> l1, double[] d1, Collection<ClassLabel> l2) {
    assert (l1.size() == d1.length);
    if(l1 == l2) {
      return d1.clone();
    }
    double[] d2 = new double[l2.size()];
    Iterator<ClassLabel> i2 = l2.iterator();
    for(int i = 0; i2.hasNext();) {
      ClassLabel l = i2.next();
      int idx = l1.indexOf(l);
      if(idx < 0 && getLogger().isDebuggingFiner()) {
        getLogger().debugFiner("Label not found: " + l);
      }
      d2[i] = (idx >= 0) ? d1[idx] : 0.; // Default to 0 for unknown labels!
    }
    return d2;
  }
}
