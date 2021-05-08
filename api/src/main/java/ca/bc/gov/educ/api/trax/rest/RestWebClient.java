package ca.bc.gov.educ.api.trax.rest;

import ca.bc.gov.educ.api.trax.properties.ApplicationProperties;
import io.netty.handler.logging.LogLevel;
import lombok.val;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * The type Rest web client.
 */
@Configuration
@Profile("!test")
public class RestWebClient {
  private final DefaultUriBuilderFactory factory;
  private final ClientHttpConnector connector;
  /**
   * The Props.
   */
  private final ApplicationProperties props;

  /**
   * Instantiates a new Rest web client.
   *
   * @param props the props
   */
  public RestWebClient(final ApplicationProperties props) {
    this.props = props;
    this.factory = new DefaultUriBuilderFactory();
    this.factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
    final HttpClient client = HttpClient.create().compress(true)
      .resolver(spec -> spec.queryTimeout(Duration.ofMillis(200)).trace("DNS", LogLevel.TRACE));
    client.warmup()
      .block();
    this.connector = new ReactorClientHttpConnector(client);
  }

  /**
   * Web client web client.
   *
   * @return the web client
   */
  @Bean
  WebClient webClient() {
    val clientRegistryRepo = new InMemoryReactiveClientRegistrationRepository(ClientRegistration
      .withRegistrationId(this.props.getClientID())
      .tokenUri(this.props.getTokenURL())
      .clientId(this.props.getClientID())
      .clientSecret(this.props.getClientSecret())
      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
      .build());
    val clientService = new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistryRepo);
    val authorizedClientManager =
      new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clientRegistryRepo, clientService);
    val oauthFilter = new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
    oauthFilter.setDefaultClientRegistrationId(this.props.getClientID());
    return WebClient.builder()
      .clientConnector(this.connector)
      .uriBuilderFactory(this.factory)
      .filter(oauthFilter)
      .build();
  }

  @Bean
  WebClient chesWebClient() {
    val clientRegistryRepo = new InMemoryReactiveClientRegistrationRepository(ClientRegistration
      .withRegistrationId(this.props.getChesClientID())
      .tokenUri(this.props.getChesTokenURL())
      .clientId(this.props.getChesClientID())
      .clientSecret(this.props.getChesClientSecret())
      .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
      .build());
    val clientService = new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistryRepo);
    val authorizedClientManager =
      new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clientRegistryRepo, clientService);
    val oauthFilter = new ServerOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager);
    oauthFilter.setDefaultClientRegistrationId(this.props.getChesClientID());
    return WebClient.builder()
      .clientConnector(this.connector)
      .uriBuilderFactory(this.factory)
      .filter(oauthFilter)
      .build();
  }
}
