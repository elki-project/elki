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

import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.datasource.bundle.BundleMeta;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * This filter assigns static DBIDs, based on the sequence the objects appear in
 * the bundle by adding a column of DBID type to the bundle.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has DBID oneway - - «produces»
 */
public class FixedDBIDsFilter extends AbstractStreamFilter {
  /**
   * Optional parameter to specify the first object ID to use.
   * <p>
   * Key: {@code -dbc.startid}
   * </p>
   */
  public static final OptionID IDSTART_ID = OptionID.getOrCreateOptionID("dbc.startid", "Object ID to start counting with");

  /**
   * The filtered meta
   */
  BundleMeta meta;

  /**
   * The next ID to assign
   */
  int curid = 0;

  /**
   * Constructor.
   * 
   * @param startid ID to start enumerating with.
   */
  public FixedDBIDsFilter(int startid) {
    super();
    this.curid = startid;
  }

  @Override
  public BundleMeta getMeta() {
    return meta;
  }

  @Override
  public Event nextEvent() {
    Event ev = source.nextEvent();
    if(ev == Event.META_CHANGED) {
      if(meta == null) {
        meta = new BundleMeta();
        meta.add(TypeUtil.DBID);
      }
      BundleMeta origmeta = source.getMeta();
      // Note -1 for the injected DBID column
      for(int i = meta.size() - 1; i < origmeta.size(); i++) {
        meta.add(origmeta.get(i));
      }
    }
    return ev;
  }

  @Override
  public Object data(int rnum) {
    if(rnum == 0) {
      DBID ret = DBIDUtil.importInteger(curid);
      curid++;
      return ret;
    }
    return source.data(rnum - 1);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    int startid = -1;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter startidParam = new IntParameter(IDSTART_ID);
      if(config.grab(startidParam)) {
        startid = startidParam.getValue();
      }
    }

    @Override
    protected Object makeInstance() {
      return new FixedDBIDsFilter(startid);
    }
  }
}