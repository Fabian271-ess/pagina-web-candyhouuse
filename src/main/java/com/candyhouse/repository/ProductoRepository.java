package com.candyhouse.repository;

import com.candyhouse.model.Producto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @Query("SELECT p FROM Producto p WHERE " +
            "LOWER(p.nombrePro) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
            "LOWER(p.categoria) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Producto> buscar(@Param("q") String q, Pageable pageable);
}