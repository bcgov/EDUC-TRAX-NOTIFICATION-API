package ca.bc.gov.educ.api.trax;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The type Trax notification api resource application.
 */
@SpringBootApplication
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableCaching
@EnableScheduling
@EnableRetry
@EnableSchedulerLock(defaultLockAtMostFor = "1s")
public class TraxNotificationApiResourceApplication {

  /**
   * The entry point of application.
   *
   * @param args the input arguments
   */
  public static void main(final String[] args) {
    SpringApplication.run(TraxNotificationApiResourceApplication.class, args);
  }

  /**
   * Lock provider lock provider.
   *
   * @param jdbcTemplate       the jdbc template
   * @param transactionManager the transaction manager
   * @return the lock provider
   */
  @Bean
  public LockProvider lockProvider(@Autowired final JdbcTemplate jdbcTemplate, @Autowired final PlatformTransactionManager transactionManager) {
    return new JdbcTemplateLockProvider(jdbcTemplate, transactionManager, "TRAX_NOTIFICATION_SHEDLOCK");
  }

  /**
   * The type Web security configuration.
   */
  @Configuration
  static
  class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {
    /**
     * Instantiates a new Web security configuration.
     */
    public WebSecurityConfiguration() {
      super();
      SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Override
    public void configure(final WebSecurity web) {
      web.ignoring().antMatchers("/v3/api-docs/**",
          "/actuator/health", "/actuator/prometheus",
          "/swagger-ui/**", "/health");
    }

    @Override
    protected void configure(final HttpSecurity http) throws Exception {
      http
          .authorizeRequests()
          .anyRequest().authenticated().and()
          .oauth2ResourceServer().jwt();
    }
  }
}
