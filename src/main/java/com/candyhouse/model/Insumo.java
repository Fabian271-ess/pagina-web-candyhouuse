package com.candyhouse.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "insumos")
@Data
public class Insumo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "insumo_cod")
    private Long insumoCod;

    @Column(name = "nombre_ins", nullable = false, length = 30)
    private String nombreIns;

    @Column(nullable = false, length = 30)
    private String categoria;

    @Column(nullable = false, length = 50)
    private String marca;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal costo;

    @Column(length = 150)
    private String descripcion;

    @Column(name = "existencia_ins", nullable = false)
    private Integer existenciaIns;
}