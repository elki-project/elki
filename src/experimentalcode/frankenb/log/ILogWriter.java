package experimentalcode.frankenb.log;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public interface ILogWriter {

  public void putLogLine(LogLevel level, String formattedMessage);
  
}
