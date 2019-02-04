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
package de.lmu.ifi.dbs.elki.application.internal;

import static org.junit.Assert.*;

import java.lang.reflect.Method;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.ELKIServiceRegistry;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.EmptyParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.UnParameterization;

/**
 * Parameterization test of all classes supporting the parameterization API.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class ParameterizationTest {
  @Test
  public void testParameterization() {
    for(Class<?> cls : ELKIServiceRegistry.findAllImplementations(Object.class, false, true)) {
      checkV3Parameterization(cls);
    }
  }

  /** Check for a V3 constructor. */
  private void checkV3Parameterization(Class<?> cls) throws NoClassDefFoundError {
    boolean first = true;
    for(Class<?> inner : cls.getDeclaredClasses()) {
      if(AbstractParameterizer.class.isAssignableFrom(inner)) {
        try {
          Class<? extends AbstractParameterizer> pcls = inner.asSubclass(AbstractParameterizer.class);
          pcls.newInstance();
          assertTrue("More than one parameterization method in class " + cls.getName(), first);
          checkMakeInstance(cls, pcls);
          // Configure with missing values; must not throw an exception.
          Parameterizer par = ClassGenericsUtil.getParameterizer(cls);
          assertNotNull(par);
          par.configure(new UnParameterization());
          par.configure(new EmptyParameterization());
        }
        catch(Exception e) {
          fail(e.getMessage());
        }
        first = false;
      }
    }
  }

  private void checkMakeInstance(Class<?> cls, Class<? extends AbstractParameterizer> par) {
    try {
      par.getConstructor();
      final Method[] methods = par.getDeclaredMethods();
      boolean hasMakeInstance = false;
      for(int i = 0; i < methods.length; ++i) {
        final Method meth = methods[i];
        if(meth.getName().equals("makeInstance")) {
          // Check for empty signature
          if(meth.getParameterTypes().length == 0) {
            // And check for proper return type.
            if(cls.isAssignableFrom(meth.getReturnType())) {
              hasMakeInstance = true;
            }
          }
        }
      }
      assertTrue("No suitable makeInstance() for " + cls.getName(), hasMakeInstance);
    }
    catch(Exception e) {
      fail("No proper Parameterizer.makeInstance for " + cls.getName() + ": " + e);
    }
  }
}
