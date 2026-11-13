package br.com.bugnacio.techos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.bugnacio.techos.model.Equipamento;

public interface EquipamentoRepository extends JpaRepository<Equipamento, Long> {

    List<Equipamento> findByMarcaContainingIgnoreCaseOrModeloContainingIgnoreCase(String marca, String modelo);

    // Cadastro novo: existe algum equipamento com esse número de série?
    boolean existsByNumeroSerieIgnoreCase(String numeroSerie);

    // Edição: existe OUTRO equipamento (id diferente) com esse número de série?
    boolean existsByNumeroSerieIgnoreCaseAndIdNot(String numeroSerie, Long id);
}