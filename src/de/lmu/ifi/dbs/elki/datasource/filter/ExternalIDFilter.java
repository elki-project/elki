package de.lmu.ifi.dbs.elki.datasource.filter;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Class that turns a label column into an external ID column.
 * 
 * @author Erich Schubert
 */
// TODO: use a non-string class for external ids?
public class ExternalIDFilter implements ObjectFilter {
  /**
   * Optional parameter that specifies the index of the label to be used as
   * external Id, must be an integer equal to or greater than 0.
   * <p>
   * Key: {@code -dbc.externalIdIndex}
   * </p>
   */
  public static final OptionID EXTERNALID_INDEX_ID = OptionID.getOrCreateOptionID("dbc.externalIdIndex", "The index of the label to be used as external Id.");

  /**
   * The index of the label to be used as external Id, null if no external id
   * index is specified.
   */
  private final int externalIdIndex;

  /**
   * Constructor.
   * 
   * @param externalIdIndex
   */
  public ExternalIDFilter(int externalIdIndex) {
    super();
    this.externalIdIndex = externalIdIndex;
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
      List<String> eidcol = new ArrayList<String>(objects.dataLength());
      List<LabelList> lblcol = new ArrayList<LabelList>(objects.dataLength());
      bundle.appendColumn(TypeUtil.EXTERNALID, eidcol);
      bundle.appendColumn(meta, lblcol);

      // Split the column
      for(Object obj : objects.getColumn(i)) {
        if(obj != null) {
          LabelList ll = (LabelList) obj;
          eidcol.add(ll.remove(externalIdIndex));
          lblcol.add(ll);
        }
        else {
          eidcol.add(null);
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
    int externalIdIndex = -1;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter externalIdIndexParam = new IntParameter(EXTERNALID_INDEX_ID, new GreaterEqualConstraint(0));
      if(config.grab(externalIdIndexParam)) {
        externalIdIndex = externalIdIndexParam.getValue();
      }
    }

    @Override
    protected Object makeInstance() {
      return new ExternalIDFilter(externalIdIndex);
    }
  }
}