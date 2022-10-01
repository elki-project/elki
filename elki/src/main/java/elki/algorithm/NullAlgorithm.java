/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.algorithm;

import elki.Algorithm;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Title;

/**
 * Null algorithm, which does nothing. Can be used to, e.g., visualize a data set.
 * 
 * @author Erich Schubert
 * @since 0.2
 */
@Title("Null Algorithm")
@Description("Algorithm which does nothing, just return a null object.")
public class NullAlgorithm implements Algorithm {
  /**
   * Constructor.
   */
  public NullAlgorithm() {
    super();
  }

  @Override
  public Void autorun(Database database) {
    return null;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array();
  }
}
