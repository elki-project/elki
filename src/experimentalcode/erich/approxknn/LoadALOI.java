package experimentalcode.erich.approxknn;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.datasource.AbstractDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.filter.ClassLabelFilter;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeFactory;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar.RStarTreeFactory;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.bulk.SortTileRecursiveBulkSplit;
import de.lmu.ifi.dbs.elki.persistent.AbstractPageFileFactory;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

public class LoadALOI {
  private static final String folder = "/nfs/multimedia/images/ALOI/ColorHistograms/";

  protected static Database loadALOI(String variant, boolean index) {
    try {
      ListParameterization dbpar = new ListParameterization();
      // Input file
      dbpar.addParameter(FileBasedDatabaseConnection.Parameterizer.INPUT_ID, folder + "aloi-" + variant + ".csv.gz");
      // Index
      if (index) {
        dbpar.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
        dbpar.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, "10000");
        dbpar.addParameter(AbstractRStarTreeFactory.Parameterizer.BULK_SPLIT_ID, SortTileRecursiveBulkSplit.class);
      }
      // Instantiate
      Database db = ClassGenericsUtil.tryInstantiate(Database.class, StaticArrayDatabase.class, dbpar);
      db.initialize();
      return db;
    } catch (Exception e) {
      throw new RuntimeException("Cannot load database." + e, e);
    }
  }

  protected static Database loadALOIOutlier(String variant, boolean index) {
    try {
      ListParameterization dbpar = new ListParameterization();
      // Input file
      dbpar.addParameter(FileBasedDatabaseConnection.Parameterizer.INPUT_ID, folder + "outlier/aloi-" + variant + ".csv.gz");
      ArrayList<Object> filters = new ArrayList<>();
      filters.add(ClassLabelFilter.class);
      dbpar.addParameter(AbstractDatabaseConnection.Parameterizer.FILTERS_ID, filters);
      dbpar.addParameter(ClassLabelFilter.Parameterizer.CLASS_LABEL_INDEX_ID, "2");
      // Index
      if (index) {
        dbpar.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
        dbpar.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, "16384");
        dbpar.addParameter(AbstractRStarTreeFactory.Parameterizer.BULK_SPLIT_ID, SortTileRecursiveBulkSplit.class);
      }
      // Instantiate
      Database db = ClassGenericsUtil.tryInstantiate(Database.class, StaticArrayDatabase.class, dbpar);
      db.initialize();
      return db;
    } catch (Exception e) {
      throw new RuntimeException("Cannot load database." + e, e);
    }
  }

}
