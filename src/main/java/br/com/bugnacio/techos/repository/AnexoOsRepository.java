package br.com.bugnacio.techos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.bugnacio.techos.model.AnexoOs;

public interface AnexoOsRepository extends JpaRepository<AnexoOs, Long> {

    Optional<AnexoOs> findByIdAndOsId(Long id, Long osId);

    long countByOsId(Long osId);
}