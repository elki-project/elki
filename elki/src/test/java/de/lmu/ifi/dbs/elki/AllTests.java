package de.lmu.ifi.dbs.elki;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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

import de.lmu.ifi.dbs.elki.utilities.ELKIServiceRegistry;
import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Build a test suite with all tests included with ELKI
 *
 * @author Erich Schubert
 */
public class AllTests extends TestSuite {
  /**
   * Build a test suite with all tests included in ELKI.
   *
   * @return Test suite
   */
  public static Test suite() {
    TestSuite suite = new TestSuite();
    for(Class<?> cls : ELKIServiceRegistry.findAllImplementations(TestCase.class, false, false)) {
      if(cls == AllTests.class) {
        continue;
      }
      Test test = new JUnit4TestAdapter(cls);
      if(test != null) {
        suite.addTest(test);
      }
    }
    for(Class<?> cls : ELKIServiceRegistry.findAllImplementations(JUnit4Test.class, false, false)) {
      if(cls == AllTests.class) {
        continue;
      }
      Test test = new JUnit4TestAdapter(cls);
      if(test != null) {
        suite.addTest(test);
      }
    }
    return suite;
  }
}