package com.candyhouse.repository;

import com.candyhouse.model.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Todos los pedidos ordenados desc (NotificacionController)
    List<Pedido> findAllByOrderByIdPedidoDesc();

    // Paginado por usuario (mis_pedidos)
    Page<Pedido> findByIdUsuario(Long idUsuario, Pageable pageable);

    // Búsqueda por id de usuario paginada (buscar por nombre en admin)
    Page<Pedido> findByIdUsuarioIn(List<Long> idUsuarios, Pageable pageable);
}