package ca.bc.gov.educ.api.trax.helpers;

import ca.bc.gov.educ.api.trax.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public final class LogHelper {
  private static final String EXCEPTION = "Exception ";

  private LogHelper() {

  }


  /**
   * the event is a json string.
   *
   * @param event the json string
   */
  public static void logMessagingEventDetails(final String event) {
    try {
      MDC.putCloseable("messageEvent", event);
      log.info("");
      MDC.clear();
    } catch (final Exception exception) {
      log.error(EXCEPTION, exception);
    }
  }


  public static void logClientHttpReqResponseDetails(@NonNull final HttpMethod method, final String url, final int responseCode, final List<String> correlationID) {
    try {
      final Map<String, Object> httpMap = new HashMap<>();
      httpMap.put("client_http_response_code", String.valueOf(responseCode));
      httpMap.put("client_http_request_method", method.toString());
      httpMap.put("client_http_request_url", url);
      if (correlationID != null) {
        httpMap.put("correlation_id", String.join(",", correlationID));
      }
      MDC.putCloseable("httpEvent", JsonUtil.objectMapper.writeValueAsString(httpMap));
      log.info("");
      MDC.clear();
    } catch (final Exception exception) {
      log.error(EXCEPTION, exception);
    }
  }
}
