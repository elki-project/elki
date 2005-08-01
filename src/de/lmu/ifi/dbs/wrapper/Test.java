package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.parser.Parser;
import de.lmu.ifi.dbs.parser.StandardLabelParser;
import de.lmu.ifi.dbs.database.Database;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 *
 */
public class Test {
  public static void main(String[] args) {
    try {
      File file = new File("test.txt");
      InputStream in = new FileInputStream(file);

      Parser parser = new StandardLabelParser();
      String[] param = {"-database","de.lmu.ifi.dbs.database.RTreeDatabase"};
      parser.setParameters(param);

      Database db = parser.parse(in);
      System.out.println(db);
    }
    catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }
}
