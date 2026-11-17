package br.com.bugnacio.techos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.bugnacio.techos.model.PecaUsada;

public interface PecaUsadaRepository extends JpaRepository<PecaUsada, Long> {

    List<PecaUsada> findByOsIdOrderByIdAsc(Long osId);

    Optional<PecaUsada> findByOsIdAndItemId(Long osId, Long itemId);
}