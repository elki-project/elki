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
package de.lmu.ifi.dbs.elki.datasource.filter.typeconversions;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.SimpleClassLabel;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Class that turns a label column into a class label column.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @navassoc - reads - LabelList
 * @has - - - ClassLabel
 */
@Alias("de.lmu.ifi.dbs.elki.datasource.filter.ClassLabelFilter")
public class ClassLabelFilter implements ObjectFilter {
  /**
   * The index of the label to be used as class label, null if no class label is
   * specified.
   */
  private final int classLabelIndex;

  /**
   * The class label class to use.
   */
  private final ClassLabel.Factory<?> classLabelFactory;

  /**
   * Constructor.
   * 
   * @param classLabelIndex The index to convert
   * @param classLabelFactory The class label factory to use
   */
  public ClassLabelFilter(int classLabelIndex, ClassLabel.Factory<?> classLabelFactory) {
    super();
    this.classLabelIndex = classLabelIndex;
    this.classLabelFactory = classLabelFactory;
  }

  @Override
  public MultipleObjectsBundle filter(MultipleObjectsBundle objects) {
    MultipleObjectsBundle bundle = new MultipleObjectsBundle();
    // Find a labellist column
    boolean done = false;
    boolean keeplabelcol = false;
    for(int i = 0; i < objects.metaLength(); i++) {
      SimpleTypeInformation<?> meta = objects.meta(i);
      // Skip non-labellist columns - or if we already had a labellist
      if(done || !LabelList.class.equals(meta.getRestrictionClass())) {
        bundle.appendColumn(meta, objects.getColumn(i));
        continue;
      }
      done = true;

      // We split the label column into two parts
      List<ClassLabel> clscol = new ArrayList<>(objects.dataLength());
      List<LabelList> lblcol = new ArrayList<>(objects.dataLength());

      ArrayList<String> lbuf = new ArrayList<>();
      // Split the column
      for(Object obj : objects.getColumn(i)) {
        if(obj != null) {
          LabelList ll = (LabelList) obj;
          int off = (classLabelIndex >= 0) ? classLabelIndex : (ll.size() - classLabelIndex);
          try {
            ClassLabel lbl = classLabelFactory.makeFromString(ll.get(off));
            clscol.add(lbl);
          }
          catch(Exception e) {
            throw new AbortException("Cannot initialize class labels: " + e.getMessage(), e);
          }
          lbuf.clear();
          for(int j = 0; j < ll.size(); j++) {
            if(j == off) {
              continue;
            }
            lbuf.add(ll.get(j));
          }
          lblcol.add(LabelList.make(lbuf));
          if(!lbuf.isEmpty()) {
            keeplabelcol = true;
          }
        }
        else {
          clscol.add(null);
          lblcol.add(null);
        }
      }
      bundle.appendColumn(classLabelFactory.getTypeInformation(), clscol);
      // Only add the label column when it's not empty.
      if(keeplabelcol) {
        bundle.appendColumn(meta, lblcol);
      }
    }
    return bundle;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Optional parameter that specifies the index of the label to be used as
     * class label, must be an integer equal to or greater than 0.
     */
    public static final OptionID CLASS_LABEL_INDEX_ID = new OptionID("dbc.classLabelIndex", "The index of the label to be used as class label. The first label is 0, negative indexes are relative to the end.");

    /**
     * Parameter to specify the class of occurring class labels.
     */
    public static final OptionID CLASS_LABEL_CLASS_ID = new OptionID("dbc.classLabelClass", "Class label class to use.");

    /**
     * The index of the label to be used as class label, null if no class label
     * is specified.
     */
    protected int classLabelIndex;

    /**
     * The class label factory to use.
     */
    private ClassLabel.Factory<?> classLabelFactory;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // parameter class label index
      final IntParameter classLabelIndexParam = new IntParameter(CLASS_LABEL_INDEX_ID);
      final ObjectParameter<ClassLabel.Factory<?>> classlabelClassParam = new ObjectParameter<>(CLASS_LABEL_CLASS_ID, ClassLabel.Factory.class, SimpleClassLabel.Factory.class);

      config.grab(classLabelIndexParam);
      config.grab(classlabelClassParam);
      if(classLabelIndexParam.isDefined() && classlabelClassParam.isDefined()) {
        classLabelIndex = classLabelIndexParam.intValue();
        classLabelFactory = classlabelClassParam.instantiateClass(config);
      }
    }

    @Override
    protected ClassLabelFilter makeInstance() {
      return new ClassLabelFilter(classLabelIndex, classLabelFactory);
    }
  }
}
