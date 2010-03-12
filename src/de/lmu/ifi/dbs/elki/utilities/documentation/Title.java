package de.lmu.ifi.dbs.elki.utilities.documentation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Simple interface to provide a nicer title for the class.
 * 
 * @author Erich Schubert
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.TYPE })
public @interface Title {
  /**
   * Title of the Algorithm
   * 
   * @return Title
   */
  public String value();
}
