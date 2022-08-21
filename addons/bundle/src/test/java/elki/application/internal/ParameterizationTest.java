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
package elki.application.internal;

import static org.junit.Assert.*;

import java.lang.reflect.Method;

import org.junit.Test;

import elki.utilities.ClassGenericsUtil;
import elki.utilities.ELKIServiceRegistry;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.EmptyParameterization;
import elki.utilities.optionhandling.parameterization.UnParameterization;

/**
 * Parameterization test of all classes supporting the parameterization API.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class ParameterizationTest {
  @Test
  public void testParameterization() throws ReflectiveOperationException {
    for(Class<?> cls : ELKIServiceRegistry.findAllImplementations(Object.class, false)) {
      checkV3Parameterization(cls);
    }
  }

  /** Check for a V3 constructor. */
  private void checkV3Parameterization(Class<?> cls) throws ReflectiveOperationException {
    boolean first = true;
    for(Class<?> inner : cls.getDeclaredClasses()) {
      if(Parameterizer.class.isAssignableFrom(inner)) {
        Class<? extends Parameterizer> pcls = inner.asSubclass(Parameterizer.class);
        pcls.getDeclaredConstructor().newInstance();
        assertTrue("More than one parameterization method in class " + cls.getName(), first);
        checkMakeInstance(cls, pcls);
        // Configure with missing values; must not throw an exception.
        Parameterizer par = ClassGenericsUtil.getParameterizer(cls);
        assertNotNull(par);
        par.configure(new UnParameterization());
        EmptyParameterization ep = new EmptyParameterization();
        par.configure(ep);
        ep.clearErrors(); // Finalizer would report them automatically.
        first = false;
      }
    }
  }

  private void checkMakeInstance(Class<?> cls, Class<? extends Parameterizer> par) {
    try {
      par.getConstructor();
      final Method[] methods = par.getDeclaredMethods();
      boolean hasMakeInstance = false;
      for(int i = 0; i < methods.length; ++i) {
        final Method meth = methods[i];
        if(meth.getName().equals("make")) {
          // Check for empty signature
          if(meth.getParameterTypes().length == 0) {
            // And check for proper return type.
            if(cls.isAssignableFrom(meth.getReturnType())) {
              hasMakeInstance = true;
            }
          }
        }
      }
      assertTrue("No suitable make() for " + cls.getName(), hasMakeInstance);
    }
    catch(Exception e) {
      fail("No proper Par.make() for " + cls.getName() + ": " + e);
    }
  }
}
