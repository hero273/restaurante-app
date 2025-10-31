package com.wayas.app.config;

import com.wayas.app.repository.IUsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // Importa HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()
                
                // === RESTRICCIONES DE ADMIN ===
                
                // 1. El nuevo CRUD de Usuarios
                .requestMatchers("/usuarios/**").hasAuthority("ADMIN")

                // 2. Todas las acciones de tipo POST (guardar, crear, actualizar)
                .requestMatchers(HttpMethod.POST, 
                    "/calificacion/proveedores/guardar", 
                    "/compras/registrar/guardar", 
                    "/compras/anular/**",
                    "/requerimientos/agregarItem",
                    "/requerimientos/eliminarItem",
                    "/requerimientos/guardar",
                    "/requerimientos/eliminar/**",
                    "/inventario/contrastar/actualizar",
                    "/inventario/insumos/guardar"
                ).hasAuthority("ADMIN")
                
                // 3. Todas las acciones de tipo GET que eliminan (mala práctica, pero así está en tu código)
                .requestMatchers(HttpMethod.GET, 
                    "/calificacion/proveedores/eliminar/**",
                    "/inventario/insumos/eliminar/**"
                ).hasAuthority("ADMIN")

                // === PERMISOS PARA TODOS LOS LOGUEADOS (ADMIN y USER) ===
                // 4. Permitir todo lo demás (Dashboards, vistas GET de reportes, etc.)
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/dashboard", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );
        // http.csrf(csrf -> csrf.disable()); // Descomenta si tienes problemas con formularios POST

        return http.build();
    }
}