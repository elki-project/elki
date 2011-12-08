package de.lmu.ifi.dbs.elki.evaluation.clustering;
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

import de.lmu.ifi.dbs.elki.evaluation.clustering.ClusterContingencyTable.Util;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Set matching purity measures
 * 
 * References:
 * <p>
 * Zhao, Y. and Karypis, G.<br />
 * Criterion functions for document clustering: Experiments and analysis<br />
 * University of Minnesota, Department of Computer Science, Technical Report
 * 01-40, 2001
 * </p>
 * <p>
 * Meilă, M<br />
 * Comparing clusterings<br />
 * University of Washington, Seattle, Technical Report 418, 2002
 * </p>
 * <p>
 * Steinbach, M. and Karypis, G. and Kumar, V. and others<br />
 * A comparison of document clustering techniques<br />
 * KDD workshop on text mining, 2000
 * </p>
 * 
 * @author Sascha Goldhofer
 */
@Reference(authors = "Meilă, M", title = "Comparing clusterings", booktitle = "University of Washington, Seattle, Technical Report 418, 2002", url = "http://www.stat.washington.edu/mmp/www.stat.washington.edu/mmp/Papers/compare-colt.pdf")
public class SetMatchingPurity {
  /**
   * Result cache
   */
  protected double smPurity = -1.0, smInversePurity = -1.0;

  /**
   * Constructor.
   * 
   * @param table Contingency table
   */
  protected SetMatchingPurity(ClusterContingencyTable table) {
    super();
    int numobj = table.contingency[table.size1][table.size2];
    {
      smPurity = 0.0;
      // iterate first clustering
      for(int i1 = 0; i1 < table.size1; i1++) {
        double precisionMax = 0.0;
        for(int i2 = 0; i2 < table.size2; i2++) {
          precisionMax = Math.max(precisionMax, (1.0 * table.contingency[i1][i2]));
          // / numobj));
        }
        smPurity += (precisionMax / numobj);
        // * contingency[i1][size2]/numobj;
      }
    }
    {
      smInversePurity = 0.0;
      // iterate second clustering
      for(int i2 = 0; i2 < table.size2; i2++) {
        double recallMax = 0.0;
        for(int i1 = 0; i1 < table.size1; i1++) {
          recallMax = Math.max(recallMax, (1.0 * table.contingency[i1][i2]));
          // / numobj));
        }
        smInversePurity += (recallMax / numobj);
        // * contingency[i1][size2]/numobj;
      }
    }
  }

  /**
   * Get the set matchings purity (first:second clustering) (normalized, 1 =
   * equal)
   * 
   * @return purity
   */
  @Reference(authors = "Zhao, Y. and Karypis, G.", title = "Criterion functions for document clustering: Experiments and analysis", booktitle = "University of Minnesota, Department of Computer Science, Technical Report 01-40, 2001", url = "http://www-users.cs.umn.edu/~karypis/publications/Papers/PDF/vscluster.pdf")
  public double purity() {
    return smPurity;
  }

  /**
   * Get the set matchings inverse purity (second:first clustering)
   * (normalized, 1 = equal)
   * 
   * @return Inverse purity
   */
  public double inversePurity() {
    return smInversePurity;
  }

  /**
   * Get the set matching F1-Measure
   * 
   * <p>
   * Steinbach, M. and Karypis, G. and Kumar, V. and others<br />
   * A comparison of document clustering techniques<br />
   * KDD workshop on text mining, 2000
   * </p>
   * 
   * @return Set Matching F1-Measure
   */
  @Reference(authors = "Steinbach, M. and Karypis, G. and Kumar, V. and others", title = "A comparison of document clustering techniques", booktitle = "KDD workshop on text mining, 2000", url = "http://www-users.itlabs.umn.edu/~karypis/publications/Papers/PDF/doccluster.pdf")
  public double f1Measure() {
    return Util.f1Measure(purity(), inversePurity());
  }
}