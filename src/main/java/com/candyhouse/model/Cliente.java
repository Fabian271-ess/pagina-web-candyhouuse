package com.candyhouse.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "clientes")
@Data
public class Cliente {

    @Id
    @Column(name = "cliente_doc")
    private Long clienteDoc;

    @Column(name = "tipo_doc", nullable = false, length = 10)
    private String tipoDoc;

    @Column(name = "nombre_cli", nullable = false, length = 50)
    private String nombreCli;

    @Column(name = "apellido_cli", nullable = false, length = 50)
    private String apellidoCli;

    private Long telefono;

    @Column(name = "id_usuario", unique = true)
    private Long idUsuario;
}
