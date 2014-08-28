package experimentalcode.erich.approxknn;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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
import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.AbstractDatabase;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.datasource.DatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.cleaning.DropNaNFilter;
import de.lmu.ifi.dbs.elki.datasource.filter.normalization.columnwise.AttributeWiseVarianceNormalization;
import de.lmu.ifi.dbs.elki.datasource.parser.NumberVectorLabelParser;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRStarTreeFactory;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar.RStarTreeFactory;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.bulk.SortTileRecursiveBulkSplit;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.persistent.AbstractPageFileFactory;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Load the image-net data set.
 * 
 * @author Erich Schubert
 */
public class LoadImageNet {
  private static final Logging LOG = Logging.getLogger(LoadImageNet.class);

  private static final String basedir = "/home/schube/experiments/data/";

  // "/nfs/multimedia/images/ImageNet/features/Haralick-1";

  protected static Database loadDatabase(String variant, boolean index) {
    try {
      ListParameterization dbpar = new ListParameterization();
      // Index for full-dimensional query
      if(index) {
        dbpar.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, RStarTreeFactory.class);
        dbpar.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, "10000");
        dbpar.addParameter(AbstractRStarTreeFactory.Parameterizer.BULK_SPLIT_ID, SortTileRecursiveBulkSplit.class);
      }
      dbpar.addParameter(AbstractDatabase.Parameterizer.DATABASE_CONNECTION_ID, new MergingDBC(basedir + variant));
      // Instantiate
      Database db = ClassGenericsUtil.tryInstantiate(Database.class, StaticArrayDatabase.class, dbpar);
      db.initialize();
      return db;
    }
    catch(Exception e) {
      throw new RuntimeException("Cannot load database." + e, e);
    }
  }

  static class MergingDBC implements DatabaseConnection {
    String folder;

    public MergingDBC(String folder) {
      this.folder = folder;
    }

    @Override
    public MultipleObjectsBundle loadData() {
      MultipleObjectsBundle bundle = null;
      for(File child : new File(folder).listFiles()) {
        if(child.getName().endsWith(".gz")) {
          LOG.debugFine("Loading file: " + child);
          NumberVectorLabelParser<DoubleVector> parser = new NumberVectorLabelParser<>(Pattern.compile(" "), "\"'", null, null, DoubleVector.FACTORY);
          MultipleObjectsBundle bun;
          try {
            bun = (new FileBasedDatabaseConnection(null, parser, child)).loadData();
          }
          catch(AbortException e) {
            // Some files are empty.
            if(e.getMessage().contains("No vectors were read")) {
              continue;
            }
            throw e;
          }
          if(bundle == null) {
            bundle = bun;
          }
          else {
            if(bun.metaLength() != bundle.metaLength()) {
              throw new AbortException("Different number of relations! " + child);
            }
            for(int i = 0; i < bundle.metaLength(); i++) {
              if(!bundle.meta(i).isAssignableFromType(bun.meta(i))) {
                throw new AbortException("Incompatible relations: " + bundle.meta(i) + " " + bun.meta(i));
              }
            }
            for(int i = 0; i < bundle.metaLength(); i++) {
              // Merge. Dangerous hack.
              @SuppressWarnings("unchecked")
              final ArrayList<Object> modifiable = (ArrayList<Object>) bundle.getColumn(i);
              modifiable.addAll(bun.getColumn(i));
            }
          }
        }
      }
      bundle = (new DropNaNFilter()).filter(bundle);
      bundle = (new AttributeWiseVarianceNormalization<>()).filter(bundle);
      return bundle;
    }
  }
}
