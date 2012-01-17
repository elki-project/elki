package de.lmu.ifi.dbs.elki.logging;

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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;

/**
 * Small experiment to assert the console output logger works as expected.
 * 
 * @author Erich Schubert
 * 
 */
public class TestOutputStreamLogger implements JUnit4Test {
  /**
   * Write a couple of messages to the console output writer and compare the
   * resulting characters.
   * 
   * @throws IOException on errors.
   */
  @Test
  public final void testWriteString() throws IOException {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    Writer wri = new OutputStreamLogger(buf);
    wri.write("Test." + OutputStreamLogger.NEWLINE);
    wri.write("\r123");
    wri.write("\r23");
    wri.write("\r3");
    wri.write("Test.");
    String should = "Test." + OutputStreamLogger.NEWLINE + "\r123\r   \r23\r  \r3" + OutputStreamLogger.NEWLINE + "Test.";
    assertEquals("Output doesn't match requirements.", should, buf.toString());
  }
}
