package ca.bc.gov.educ.api.trax.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * The type Business exception.
 */
@Slf4j
public class BusinessException extends Exception {

  private static final long serialVersionUID = -4128980175291948277L;

  /**
   * Instantiates a new Business exception.
   *
   * @param businessError the business error
   * @param messageArgs   the message args
   */
  public BusinessException(final BusinessError businessError, final String... messageArgs) {
    super(businessError.getCode());
    var finalLogMessage = businessError.getCode();
    if (messageArgs != null) {
      finalLogMessage = getFormattedMessage(finalLogMessage, messageArgs);
    }
    log.error(finalLogMessage);
  }

  /**
   * Gets formatted message.
   *
   * @param msg           the msg
   * @param substitutions the substitutions
   * @return the formatted message
   */
  private static String getFormattedMessage(final String msg, final String... substitutions) {
    final String format = msg.replace("$?", "%s");
    return String.format(format, (Object[]) substitutions);
  }
}

