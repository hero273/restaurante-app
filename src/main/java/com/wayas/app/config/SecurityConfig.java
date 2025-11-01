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
                
                // 1. Gestión de Usuarios (CRUD completo)
                .requestMatchers("/usuarios/**","/usuarios/eliminar/**").hasAuthority("ADMIN")

                // 2. TODAS las acciones de MODIFICACIÓN (POST)
                .requestMatchers(HttpMethod.POST, 
                    // Calificación
                    "/calificacion/proveedores/guardar",
                    "/calificacion/proveedores/eliminar/**", // <-- MOVILIZADO DE GET A POST
                    "/calificacion/guardar",

                    // Compras
                    "/compras/registrar/guardar", 
                    "/compras/anular/**",
                    
                    // Requerimientos
                    "/requerimientos/agregarItem",
                    "/requerimientos/eliminarItem",
                    "/requerimientos/guardar",
                    "/requerimientos/eliminar/**",
                    
                    // Inventario
                    "/inventario/contrastar/actualizar",
                    "/inventario/insumos/guardar",
                    "/inventario/insumos/eliminar/**", // <-- MOVILIZADO DE GET A POST
                    
                    // Recetas (NUEVO MÓDULO)
                    "/recetas/guardar",
                    "/recetas/eliminar/**"
                    
                ).hasAuthority("ADMIN")
                
                // 3. SECCIÓN DE GET PARA ELIMINAR (ELIMINADA)

                // === PERMISOS PARA TODOS LOS LOGUEADOS (ADMIN y USER) ===
                // 4. Vistas principales (GET) permitidas para ambos roles
                .requestMatchers(
                    "/dashboard",
                    "/gestion_compra",
                    "/gestion_inventario",
                    "/gestion_recetas", 
                    "/gestion_calificacion",
                    "/reporte/insumos",
                    "/requerimientos/generar",
                    "/compras/registrar",
                    "/compras/historial",
                    "/calificacion/proveedores",
                    "/calificacion/evaluar",
                    "/calificacion/consultar",
                    "/inventario/ingresar/pendientes",
                    "/inventario/contrastar",
                    "/inventario/insumos",
                    "/recetas/registrar" // (NUEVO MÓDULO)
                ).authenticated()

                // 5. Cualquier otra solicitud (que no coincida) debe estar autenticada
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
            
        // NOTA: Mantenemos CSRF habilitado (NO DESCOMENTAR la línea de abajo)
        // http.csrf(csrf -> csrf.disable()); 

        return http.build();
    }
}
