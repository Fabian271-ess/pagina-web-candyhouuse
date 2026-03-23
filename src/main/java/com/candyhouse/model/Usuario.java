package com.candyhouse.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "usuario")
@Data
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(name = "contraseña", nullable = false, length = 225)
    private String contrasena;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoUsuario tipo;

    @Column(unique = true, length = 100)
    private String correo;

    public enum TipoUsuario {
        admin, usuario
    }
}
