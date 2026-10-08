package org.example.taskmanager.taskmanager.infrastructure.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.resilience.annotation.EnableResilientMethods;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * На будущее, если async включу
 */
@Configuration
@EnableResilientMethods
@EnableAsync
@Slf4j
public class ResilienceConfig implements AsyncConfigurer {

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return ((ex, method, params) -> {
      log.error("Async cascade failed. Method: {}, Params: {}", method.getName(), params, ex);
    });
  }
}
