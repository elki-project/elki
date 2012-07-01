/**
 * <p>Distance values, i.e. object storing an actual <em>distance</em> value along with
 * comparison functions and value parsers.</p>
 * 
 * <p>Distances follow a factory pattern. Usually, a class will have a static instance
 * called <code>FACTORY</code> that can be used to obtain e.g. infinity or zero distances
 * as well as parse a string value into a new distance value.</p> 
 *
 * @apiviz.exclude java.io.*
 * @apiviz.exclude java.lang.*
 */
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
package de.lmu.ifi.dbs.elki.distance.distancevalue;
