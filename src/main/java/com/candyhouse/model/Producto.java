package com.candyhouse.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "productos")
@Data
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "producto_cod")
    private Long productoCod;

    @Column(name = "nombre_pro", nullable = false, length = 50)
    private String nombrePro;

    @Column(nullable = false, length = 30)
    private String categoria;

    @Column(nullable = false)
    private Integer precio;

    @Column(name = "existencia_prod", nullable = false)
    private Integer existenciaProd;

    @Column(length = 150)
    private String descripcion;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;
}
