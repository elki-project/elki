package de.lmu.ifi.dbs.elki.utilities.optionhandling;
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

import de.lmu.ifi.dbs.elki.utilities.InspectionUtilFrequentlyScanned;

/**
 * Interface to define the required methods for command line interaction.
 * 
 * <b>Important note:</b>
 * 
 * <p>
 * Although <em>this cannot be specified in a Java interface</em>, any class
 * implementing this interface <em>must</em> also have a constructor that takes
 * a single
 * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization}
 * as option, which is used to set the class parameters.
 * </p>
 * 
 * <p>
 * Alternatively, a constructor with no options is also allowed.
 * </p>
 * 
 * <p>
 * This means, each class implementing Parameterizable
 * <em>must have a constructor that either is</em> <blockquote>
 * 
 * <pre>
 * @code
 * public Class(Parameterizable config) { ... }
 * }
 * </pre>
 * 
 * </blockquote> or <blockquote>
 * 
 * <pre>
 * @code
 * public Class() { ... }
 * }
 * </pre>
 * 
 * </blockquote>
 * </p>
 * 
 * <p>
 * Constructors <em>MUST</em> not do expensive operations or allocations, since
 * they will also be called just to determine and validate parameters.
 * </p>
 * 
 * <p>
 * For <em>documentation</em>, the classes should also be annotated with
 * {@link de.lmu.ifi.dbs.elki.utilities.documentation.Title}
 * {@link de.lmu.ifi.dbs.elki.utilities.documentation.Description} and
 * {@link de.lmu.ifi.dbs.elki.utilities.documentation.Reference} (where
 * possible).
 * </p>
 * 
 * <p>
 * Please check the <em>package documentation</em> for full information on this
 * interface.
 * </p>
 * 
 * <p>
 * The application
 * {@link de.lmu.ifi.dbs.elki.application.internal.CheckParameterizables} can be
 * used to check this class contracts.
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @apiviz.exclude
 * @apiviz.excludeSubtypes
 * @apiviz.has de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter oneway - n
 * @apiviz.uses de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization
 *              .Parameterization oneway
 */
public interface Parameterizable extends InspectionUtilFrequentlyScanned {
  // Empty marker interface - the \@Description / \@Title / \@Reference and
  // constructor requirements cannot be specified in Java!
}