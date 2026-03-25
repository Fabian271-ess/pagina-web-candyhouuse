package com.candyhouse.repository;

import com.candyhouse.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByIdUsuario(Long idUsuario);
    Optional<Cliente> findByClienteDoc(Long clienteDoc);
    Optional<Cliente> findByTelefono(Long telefono);
}