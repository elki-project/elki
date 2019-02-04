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
package de.lmu.ifi.dbs.elki.evaluation.classification.holdout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Split a data set for holdout evaluation.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public abstract class AbstractHoldout implements Holdout {
  /**
   * Labels in the current data set.
   */
  protected ArrayList<ClassLabel> labels;

  /**
   * Column containing the class labels.
   */
  protected int labelcol;

  /**
   * Input data bundle.
   */
  protected MultipleObjectsBundle bundle;

  @Override
  public void initialize(MultipleObjectsBundle bundle) {
    this.bundle = bundle;
    this.labelcol = findClassLabelColumn(bundle);
    this.labels = allClassLabels(bundle);
  }

  @Override
  public ArrayList<ClassLabel> getLabels() {
    return labels;
  }

  /**
   * Find the class label column in the given data set.
   * 
   * @param bundle Bundle
   * @return Class label column
   */
  public static int findClassLabelColumn(MultipleObjectsBundle bundle) {
    for(int i = 0, l = bundle.metaLength(); i < l; ++i) {
      if(TypeUtil.CLASSLABEL.isAssignableFromType(bundle.meta(i))) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Get an array of all class labels in a given data set.
   * 
   * @param bundle Bundle
   * @return Class labels.
   */
  public static ArrayList<ClassLabel> allClassLabels(MultipleObjectsBundle bundle) {
    int col = findClassLabelColumn(bundle);
    // TODO: automatically infer class labels?
    if(col < 0) {
      throw new AbortException("No class label found (try using ClassLabelFilter).");
    }
    return allClassLabels(bundle, col);
  }

  /**
   * Get an array of all class labels in a given data set.
   * 
   * @param bundle Bundle
   * @param col Column
   * @return Class labels.
   */
  public static ArrayList<ClassLabel> allClassLabels(MultipleObjectsBundle bundle, int col) {
    HashSet<ClassLabel> labels = new HashSet<ClassLabel>();
    for(int i = 0, l = bundle.dataLength(); i < l; ++i) {
      Object o = bundle.data(i, col);
      if(o == null || !(o instanceof ClassLabel)) {
        continue;
      }
      labels.add((ClassLabel) o);
    }
    ArrayList<ClassLabel> ret = new ArrayList<>(labels);
    Collections.sort(ret);
    return ret;
  }
}
