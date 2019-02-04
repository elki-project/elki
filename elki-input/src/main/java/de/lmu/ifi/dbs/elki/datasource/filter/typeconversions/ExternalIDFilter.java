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

import de.lmu.ifi.dbs.elki.data.ExternalID;
import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Class that turns a label column into an external ID column.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @navassoc - reads - LabelList
 * @navhas - produces - ExternalID
 */
@Alias("de.lmu.ifi.dbs.elki.datasource.filter.ExternalIDFilter")
public class ExternalIDFilter implements ObjectFilter {
  /**
   * The index of the label to be used as external Id.
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
      List<ExternalID> eidcol = new ArrayList<>(objects.dataLength());
      List<LabelList> lblcol = new ArrayList<>(objects.dataLength());

      // Split the column
      ArrayList<String> lbuf = new ArrayList<>();
      for(Object obj : objects.getColumn(i)) {
        if(obj != null) {
          LabelList ll = (LabelList) obj;
          int off = externalIdIndex >= 0 ? externalIdIndex : (ll.size() - externalIdIndex);
          eidcol.add(new ExternalID(ll.get(off)));
          lbuf.clear();
          for(int j = 0; j < ll.size(); j++) {
            if(j == off) {
              continue;
            }
            lbuf.add(ll.get(j));
          }
          lblcol.add(LabelList.make(lbuf));
          if(ll.size() > 0) {
            keeplabelcol = true;
          }
        }
        else {
          eidcol.add(null);
          lblcol.add(null);
        }
      }

      bundle.appendColumn(TypeUtil.EXTERNALID, eidcol);
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
     * Parameter that specifies the index of the label to be used as external
     * Id, starting at 0. Negative numbers are counted from the end.
     */
    public static final OptionID EXTERNALID_INDEX_ID = new OptionID("dbc.externalIdIndex", "The index of the label to be used as external Id. The first label is 0; negative indexes are relative to the end.");

    int externalIdIndex = -1;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final IntParameter externalIdIndexParam = new IntParameter(EXTERNALID_INDEX_ID);
      if(config.grab(externalIdIndexParam)) {
        externalIdIndex = externalIdIndexParam.intValue();
      }
    }

    @Override
    protected ExternalIDFilter makeInstance() {
      return new ExternalIDFilter(externalIdIndex);
    }
  }
}
