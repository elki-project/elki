package de.lmu.ifi.dbs.elki.logging;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * @author Arthur Zimek
 */
public class ProgressFormatter extends Formatter {
  /**
   * Provides a progress formatter
   * to simply retrieve the progress of an LogRecord.
   */
  public ProgressFormatter() {
      super();
  }

  /**
   * Retrieves the message as it is set in the given LogRecord.
   */
  @Override
  public String format(LogRecord record) {
    if(record instanceof ProgressLogRecord)
    {
      ProgressLogRecord p = (ProgressLogRecord) record;
      if(p.getPercentage()>=100)
      {
        return p.getMessage()+"\n";
      }
    }
    return record.getMessage();    
  }
}
