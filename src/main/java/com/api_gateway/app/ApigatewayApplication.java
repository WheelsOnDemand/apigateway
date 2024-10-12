package com.api_gateway.app;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;

import reactor.core.publisher.Mono;

@SpringBootApplication
@EnableDiscoveryClient
public class ApigatewayApplication {

	@Value("${microservice.payment}")
	String microservicePayment;
	
	@Value("${microservice.inventory}")
	String microserviceInventory;
	
	@Value("${microservice.invoice}")
	String microserviceInvoice;
	
	@Value("${microservice.filter}")
	String microserviceFilter;
	
	@Value("${microservice.rental}")
	String microserviceRental;
	
	public static void main(String[] args) {
		SpringApplication.run(ApigatewayApplication.class, args);
	}

	@Bean
	public RouteLocator wheelsOnDemandRouteConfig(RouteLocatorBuilder routeLocatorBuilder) {
		return routeLocatorBuilder.routes().route(p -> p.path("/wheelsondemand/filter/**").filters(f -> f
				.rewritePath("/wheelsondemand/filter/(?<segment>.*)", "/${segment}")
				.addResponseHeader("X-Response-Time", LocalDateTime.now().toString())
				.circuitBreaker(
						config -> config.setName("filterCircuitBreaker").setFallbackUri("forward:/contactSupport"))
				.requestRateLimiter(config -> config.setRateLimiter(redisRateLimiter()).setKeyResolver(ipKeyResolver()))
				.retry(retryConfig -> retryConfig.setRetries(3).setMethods(HttpMethod.GET)
						.setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, true)))
				.uri(microserviceFilter))
				.route(p -> p.path("/wheelsondemand/inventory/**").filters(f -> f
						.rewritePath("/wheelsondemand/inventory/(?<segment>.*)", "/${segment}")
						.addResponseHeader("X-Response-Time", LocalDateTime.now().toString())
						.circuitBreaker(config -> config.setName("inventoryCircuitBreaker")
								.setFallbackUri("forward:/contactSupport"))
						.requestRateLimiter(
								config -> config.setRateLimiter(redisRateLimiter()).setKeyResolver(ipKeyResolver()))
						.retry(retryConfig -> retryConfig.setRetries(3).setMethods(HttpMethod.GET)
								.setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, true)))
						.uri(microserviceInventory))
				.route(p -> p.path("/wheelsondemand/invoice/**").filters(f -> f
						.rewritePath("/wheelsondemand/invoice/(?<segment>.*)", "/${segment}")
						.addResponseHeader("X-Response-Time", LocalDateTime.now().toString())
						.circuitBreaker(config -> config.setName("invoiceCircuitBreaker")
								.setFallbackUri("forward:/contactSupport"))
						.requestRateLimiter(
								config -> config.setRateLimiter(redisRateLimiter()).setKeyResolver(ipKeyResolver()))
						.retry(retryConfig -> retryConfig.setRetries(3).setMethods(HttpMethod.GET)
								.setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, true)))
						.uri(microserviceInvoice))
				.route(p -> p.path("/wheelsondemand/payment/**")
						.filters(f -> f.rewritePath("/wheelsondemand/payment/(?<segment>.*)", "/${segment}")
								.addResponseHeader("X-Response-Time", LocalDateTime.now().toString())
								.requestRateLimiter(config -> config.setRateLimiter(redisRateLimiter())
										.setKeyResolver(ipKeyResolver())))
						.uri(microservicePayment))
				.route(p -> p.path("/wheelsondemand/rental/**").filters(f -> f
						.rewritePath("/wheelsondemand/rental/(?<segment>.*)", "/${segment}")
						.addResponseHeader("X-Response-Time", LocalDateTime.now().toString())
						.circuitBreaker(config -> config.setName("rentalCircuitBreaker")
								.setFallbackUri("forward:/contactSupport"))
						.requestRateLimiter(
								config -> config.setRateLimiter(redisRateLimiter()).setKeyResolver(ipKeyResolver()))
						.retry(retryConfig -> retryConfig.setRetries(3).setMethods(HttpMethod.GET)
								.setBackoff(Duration.ofMillis(100), Duration.ofMillis(1000), 2, true)))
						.uri(microserviceRental))
				.build();
	}

	@Bean
	public RedisRateLimiter redisRateLimiter() {
		return new RedisRateLimiter(1, 1, 1);
	}

	@Bean
	public KeyResolver ipKeyResolver() {
		return exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() // Extract
																											// the
																											// client's
																											// IP
																											// address
		);
	}

}