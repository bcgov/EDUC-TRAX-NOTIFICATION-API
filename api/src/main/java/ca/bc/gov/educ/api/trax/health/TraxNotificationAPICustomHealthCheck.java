package ca.bc.gov.educ.api.trax.health;

import io.nats.client.Connection;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * The type Pen match api custom health check.
 */
@Component
public class TraxNotificationAPICustomHealthCheck implements HealthIndicator {
  /**
   * The Nats connection.
   */
  private final Connection natsConnection;

  /**
   * Instantiates a new Pen match api custom health check.
   *
   * @param natsConnection the nats connection
   */
  public TraxNotificationAPICustomHealthCheck(final Connection natsConnection) {
    this.natsConnection = natsConnection;
  }

  @Override
  public Health getHealth(final boolean includeDetails) {
    return this.healthCheck();
  }


  @Override
  public Health health() {
    return this.healthCheck();
  }

  /**
   * Health check health.
   *
   * @return the health
   */
  private Health healthCheck() {
    if (this.natsConnection.getStatus() == Connection.Status.CLOSED) {
      return Health.down().withDetail("NATS", " Connection is Closed.").build();
    }
    return Health.up().build();
  }
}
