package de.lmu.ifi.dbs.elki.properties;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.regex.Pattern;

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

/**
 * Class to convert the old properties file into typical service files.
 * 
 * @author Erich Schubert
 */
public class ConvertPropertiesToServices {
  /**
   * @param args
   */
  public static void main(String[] args) {
    Pattern ignore = Pattern.compile("^[ #]+$");
    Properties prop = Properties.ELKI_PROPERTIES;
    for(String name : prop.getPropertyNames()) {
      String[] props = prop.getProperty(name);
      try {
        PrintStream fo = new PrintStream(new File("src/META-INF/elki", name));
        for(String p : props) {
          if (ignore.matcher(p).matches()) { continue; }
          fo.println(p);
        }
        fo.close();
      }
      catch(FileNotFoundException e) {
        de.lmu.ifi.dbs.elki.logging.LoggingUtil.exception(e);
      }
    }
  }
}