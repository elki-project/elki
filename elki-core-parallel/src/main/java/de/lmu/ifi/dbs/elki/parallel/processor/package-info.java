/**
 * Processor API of ELKI, and some essential shared processors.
 * 
 * A processor in ELKI is a function, that can be applied in parallel to different objects
 * in the database. It follows the factory design pattern, as it needs to be instantiated
 * for every thread separately.
 * 
 * While this bears some similarity to mappers as used in Map Reduce,
 * this is not an implementation of a map-reduce framework. This is why there
 * is no "reducer" in the ELKI framework.
 * 
 * A key difference is that mappers may be combined into the same thread, and exchange values
 * via the {@link de.lmu.ifi.dbs.elki.parallel.variables.SharedVariable} API.
 * 
 * The other key difference is that ELKI is not (yet?) running in a distributed framework,
 * therefore it is perfectly possible to have a mapper query the database, or write to
 * an output storage. It may be necessary to apply locking in such cases!
 * 
 * As we want to write to some storage at the end, the last processor usually will not have
 * an "output", thus the name "map" is not a good match anymore.
 * 
 * @opt hide .*\.Instance$
 */
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
package de.lmu.ifi.dbs.elki.parallel.processor;