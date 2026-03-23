package com.candyhouse.repository;

import com.candyhouse.model.Carrito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CarritoRepository extends JpaRepository<Carrito, Long> {
    List<Carrito> findByIdUsuario(Long idUsuario);
    Optional<Carrito> findByIdUsuarioAndProductoCod(Long idUsuario, Long productoCod);
    void deleteByIdUsuarioAndProductoCod(Long idUsuario, Long productoCod);
}
