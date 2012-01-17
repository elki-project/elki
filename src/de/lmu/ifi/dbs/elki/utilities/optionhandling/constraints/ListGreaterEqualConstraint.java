package de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints;

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

import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

import java.util.List;

/**
 * Represents a Greater-Equal-Than-Number parameter constraint for a list of number values.
 * All values of the list parameter ({@link de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ListParameter})
 * tested have to be greater than or equal to the specified constraint value.
 *
 * @author Elke Achtert
 * @param <N> Number type
 */
public class ListGreaterEqualConstraint<N extends Number> extends AbstractNumberConstraint<List<N>> {
    /**
     * Creates a Greater-Equal-Than-Number parameter constraint.
     * <p/>
     * That is, all values of the list parameter
     * tested have to be greater than or equal to the specified constraint value.
     *
     * @param constraintValue parameter constraint value
     */
    public ListGreaterEqualConstraint(N constraintValue) {
        super(constraintValue);
    }

    /**
     * Checks if all number values of the specified list parameter
     * are greater than or equal to the constraint value.
     * If not, a parameter exception is thrown.
     *
     */
    @Override
    public void test(List<N> t) throws ParameterException {
        for (Number n : t) {
            if (n.doubleValue() < constraintValue.doubleValue()) {
                throw new WrongParameterValueException("Parameter Constraint Error: \n"
                    + "The parameter values specified have to be greater than or equal to " + constraintValue.toString()
                    + ". (current value: " + t + ")\n");
            }
        }
    }

    @Override
    public String getDescription(String parameterName) {
        return "all elements of " + parameterName + " < " + constraintValue;
    }

}
