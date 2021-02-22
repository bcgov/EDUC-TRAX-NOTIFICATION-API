package ca.bc.gov.educ.api.trax.support;

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Subscription;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Support class to use for testing.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NatsMessageImpl implements Message {
  private String subject;
  private Subscription subscription;
  private String replyTo;
  private byte[] data;
  private String SID;
  private Connection connection;
}
