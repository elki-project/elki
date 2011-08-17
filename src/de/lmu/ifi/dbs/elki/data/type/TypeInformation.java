package de.lmu.ifi.dbs.elki.data.type;
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

/**
 * Class wrapping a particular data type.
 * 
 * @author Erich Schubert
 */
public interface TypeInformation {
  /**
   * Test whether this type is assignable from another type.
   * 
   * @param type Other type
   * @return true when the other type is accepted as subtype.
   */
  public boolean isAssignableFromType(TypeInformation type);

  /**
   * Test whether this type is assignable from a given object instance.
   * 
   * @param other Other object
   * @return true when the other type is an acceptable instance.
   */
  public boolean isAssignableFrom(Object other);
}