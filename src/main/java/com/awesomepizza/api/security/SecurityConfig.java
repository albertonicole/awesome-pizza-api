package com.awesomepizza.api.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final CustomAuthenticationEntryPoint authenticationEntryPoint;
	private final CustomAccessDeniedHandler accessDeniedHandler;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.requestMatchers("/actuator/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/orders").hasRole("CLIENTE")
						.requestMatchers(HttpMethod.GET, "/api/orders/queue").hasRole("PIZZAIOLO")
						.requestMatchers(HttpMethod.POST, "/api/orders/next").hasRole("PIZZAIOLO")
						.requestMatchers(HttpMethod.PUT, "/api/orders/*/complete").hasRole("PIZZAIOLO")
						.requestMatchers(HttpMethod.GET, "/api/orders/*/status").hasAnyRole("CLIENTE", "PIZZAIOLO")
						.requestMatchers(HttpMethod.GET, "/api/orders/*").hasAnyRole("CLIENTE", "PIZZAIOLO")
						.anyRequest().authenticated()
				)
				.httpBasic(basic -> basic.authenticationEntryPoint(authenticationEntryPoint))
				.exceptionHandling(ex -> ex.accessDeniedHandler(accessDeniedHandler));

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public InMemoryUserDetailsManager userDetailsService() {
		UserDetails cliente = User.builder()
				.username("cliente")
				.password(passwordEncoder().encode("cliente123"))
				.roles("CLIENTE")
				.build();

		UserDetails pizzaiolo = User.builder()
				.username("pizzaiolo")
				.password(passwordEncoder().encode("pizzaiolo123"))
				.roles("PIZZAIOLO")
				.build();

		return new InMemoryUserDetailsManager(cliente, pizzaiolo);
	}
}
