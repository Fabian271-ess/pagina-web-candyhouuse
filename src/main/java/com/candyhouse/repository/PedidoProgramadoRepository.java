package com.candyhouse.repository;

import com.candyhouse.model.PedidoProgramado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PedidoProgramadoRepository extends JpaRepository<PedidoProgramado, Long> {

    Optional<PedidoProgramado> findByIdPedido(Long idPedido);

    List<PedidoProgramado> findByEstado(PedidoProgramado.EstadoProgramado estado);

    Page<PedidoProgramado> findByEstadoOrderByFechaFinAsc(PedidoProgramado.EstadoProgramado estado, Pageable pageable);

    /** Verificar si ya existe programación para un pedido */
    boolean existsByIdPedido(Long idPedido);
}