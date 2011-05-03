package de.lmu.ifi.dbs.elki.datasource.filter;

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
  private final Class<? extends ClassLabel> classLabelClass;
  
  /**
   * Constructor.
   *
   * @param classLabelIndex The index to convert
   * @param classLabelClass The class label class to use
   */
  public ClassLabelFilter(int classLabelIndex, Class<? extends ClassLabel> classLabelClass) {
    super();
    this.classLabelIndex = classLabelIndex;
    this.classLabelClass = classLabelClass;
  }

  @Override
  public MultipleObjectsBundle filter(MultipleObjectsBundle objects) {
    MultipleObjectsBundle bundle = new MultipleObjectsBundle();
    // Find a labellist column
    boolean done = false;
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
      bundle.appendColumn(TypeUtil.CLASSLABEL, clscol);
      bundle.appendColumn(meta, lblcol);

      // Split the column
      for(Object obj : objects.getColumn(i)) {
        if(obj != null) {
          LabelList ll = (LabelList) obj;
          try {
            ClassLabel lbl = classLabelClass.newInstance();
            lbl.init(ll.remove(classLabelIndex));
            clscol.add(lbl);
          }
          catch(Exception e) {
            throw new AbortException("Cannot initialize class labels.");
          }
          lblcol.add(ll);
        }
        else {
          clscol.add(null);
          lblcol.add(null);
        }
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
     * The index of the label to be used as class label, null if no class label is
     * specified.
     */
    protected Integer classLabelIndex;

    /**
     * The class label class to use.
     */
    private Class<? extends ClassLabel> classLabelClass;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // parameter class label index
      final IntParameter classLabelIndexParam = new IntParameter(CLASS_LABEL_INDEX_ID, new GreaterEqualConstraint(0));
      final ObjectParameter<ClassLabel> classlabelClassParam = new ObjectParameter<ClassLabel>(CLASS_LABEL_CLASS_ID, ClassLabel.class, SimpleClassLabel.class);

      config.grab(classLabelIndexParam);
      config.grab(classlabelClassParam);
      if(classLabelIndexParam.isDefined() && classlabelClassParam.isDefined()) {
        classLabelIndex = classLabelIndexParam.getValue();
        classLabelClass = classlabelClassParam.getValue();
      }
    }

    @Override
    protected Object makeInstance() {
      return new ClassLabelFilter(classLabelIndex, classLabelClass);
    }
  }
}