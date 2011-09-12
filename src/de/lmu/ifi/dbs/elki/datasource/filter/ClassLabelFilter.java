package de.lmu.ifi.dbs.elki.datasource.filter;

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
import java.util.List;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.SimpleClassLabel;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Class that turns a label column into a class label column.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses LabelList oneway - - «reads»
 * @apiviz.has ClassLabel
 */
public class ClassLabelFilter implements ObjectFilter {
  /**
   * Optional parameter that specifies the index of the label to be used as
   * class label, must be an integer equal to or greater than 0.
   * <p>
   * Key: {@code -dbc.classLabelIndex}
   * </p>
   */
  public static final OptionID CLASS_LABEL_INDEX_ID = OptionID.getOrCreateOptionID("dbc.classLabelIndex", "The index of the label to be used as class label.");

  /**
   * Parameter to specify the class of occurring class labels.
   * <p>
   * Key: {@code -dbc.classLabelClass}
   * </p>
   */
  public static final OptionID CLASS_LABEL_CLASS_ID = OptionID.getOrCreateOptionID("dbc.classLabelClass", "Class label class to use.");

  /**
   * The index of the label to be used as class label, null if no class label is
   * specified.
   */
  private final int classLabelIndex;

  /**
   * The class label class to use.
   */
  private final ClassLabel.Factory classLabelFactory;

  /**
   * Constructor.
   * 
   * @param classLabelIndex The index to convert
   * @param classLabelFactory The class label factory to use
   */
  public ClassLabelFilter(int classLabelIndex, ClassLabel.Factory classLabelFactory) {
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
      if(done || meta.getRestrictionClass() != LabelList.class) {
        bundle.appendColumn(meta, objects.getColumn(i));
        continue;
      }
      done = true;

      // We split the label column into two parts
      List<ClassLabel> clscol = new ArrayList<ClassLabel>(objects.dataLength());
      List<LabelList> lblcol = new ArrayList<LabelList>(objects.dataLength());

      // Split the column
      for(Object obj : objects.getColumn(i)) {
        if(obj != null) {
          LabelList ll = (LabelList) obj;
          try {
            ClassLabel lbl = classLabelFactory.makeFromString(ll.remove(classLabelIndex));
            clscol.add(lbl);
          }
          catch(Exception e) {
            throw new AbortException("Cannot initialize class labels: "+e.getMessage(), e);
          }
          lblcol.add(ll);
          if(ll.size() > 0) {
            keeplabelcol = true;
          }
        }
        else {
          clscol.add(null);
          lblcol.add(null);
        }
      }
      bundle.appendColumn(TypeUtil.CLASSLABEL, clscol);
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
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * The index of the label to be used as class label, null if no class label
     * is specified.
     */
    protected Integer classLabelIndex;

    /**
     * The class label factory to use.
     */
    private ClassLabel.Factory classLabelFactory;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // parameter class label index
      final IntParameter classLabelIndexParam = new IntParameter(CLASS_LABEL_INDEX_ID, new GreaterEqualConstraint(0));
      final ObjectParameter<ClassLabel.Factory> classlabelClassParam = new ObjectParameter<ClassLabel.Factory>(CLASS_LABEL_CLASS_ID, ClassLabel.Factory.class, SimpleClassLabel.Factory.class);

      config.grab(classLabelIndexParam);
      config.grab(classlabelClassParam);
      if(classLabelIndexParam.isDefined() && classlabelClassParam.isDefined()) {
        classLabelIndex = classLabelIndexParam.getValue();
        classLabelFactory = classlabelClassParam.instantiateClass(config);
      }
    }

    @Override
    protected Object makeInstance() {
      return new ClassLabelFilter(classLabelIndex, classLabelFactory);
    }
  }
}