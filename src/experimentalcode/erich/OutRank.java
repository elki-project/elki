package experimentalcode.erich;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OutlierAlgorithm;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * OutRank: ranking outliers in high dimensional data.
 * 
 * Algorithm to score outliers based on a subspace clustering result.
 * 
 * Reference:
 * <p>
 * Emmanuel Müller, Ira Assent, Uwe Steinhausen, Thomas Seidl<br />
 * OutRank: ranking outliers in high dimensional data<br />
 * In Proceedings 24th International Conference on Data Engineering (ICDE)
 * Workshop on Ranking in Databases (DBRank), Cancun, Mexico
 * </p>
 * 
 * @author Erich Schubert
 */
@Title("OutRank: ranking outliers in high dimensional data")
@Description("Ranking outliers in high dimensional data")
@Reference(authors = "Emmanuel Müller, Ira Assent, Uwe Steinhausen, Thomas Seidl", title = "OutRank: ranking outliers in high dimensional data", booktitle = "Proc. 24th Int. Conf. on Data Engineering (ICDE) Workshop on Ranking in Databases (DBRank), Cancun, Mexico", url = "http://dx.doi.org/10.1109/ICDEW.2008.4498387")
public class OutRank extends AbstractAlgorithm<OutlierResult> implements OutlierAlgorithm {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(OutRank.class);

  @Override
  public OutlierResult run(Database database) throws IllegalStateException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    // TODO
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected OutRank makeInstance() {
      return new OutRank();
    }
  }
}