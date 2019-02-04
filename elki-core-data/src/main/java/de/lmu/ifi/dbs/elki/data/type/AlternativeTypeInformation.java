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
package de.lmu.ifi.dbs.elki.data.type;

/**
 * Class that combines multiple type restrictions into one using the "or"
 * operator.
 * 
 * @author Erich Schubert
 * @since 0.4.0
 * 
 * @composed - - - TypeInformation
 */
public class AlternativeTypeInformation implements TypeInformation {
  /**
   * The wrapped type restrictions
   */
  private final TypeInformation[] restrictions;

  /**
   * Constructor.
   * 
   * @param restrictions
   */
  public AlternativeTypeInformation(TypeInformation... restrictions) {
    super();
    this.restrictions = restrictions;
  }

  @Override
  public boolean isAssignableFromType(TypeInformation type) {
    for (int i = 0; i < restrictions.length; i++) {
      if (restrictions[i].isAssignableFromType(type)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isAssignableFrom(Object other) {
    for (int i = 0; i < restrictions.length; i++) {
      if (restrictions[i].isAssignableFrom(other)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    for (int i = 0; i < restrictions.length; i++) {
      if (i > 0) {
        buf.append(" OR ");
      }
      buf.append(restrictions[i].toString());
    }
    return buf.toString();
  }
}
