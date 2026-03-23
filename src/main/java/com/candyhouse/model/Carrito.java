package com.candyhouse.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "carrito")
@Data
public class Carrito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carrito")
    private Long idCarrito;

    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "producto_cod")
    private Long productoCod;

    private Integer cantidad = 1;
}
