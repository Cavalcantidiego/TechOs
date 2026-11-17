package br.com.bugnacio.techos.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.bugnacio.techos.model.OrdemServico;
import br.com.bugnacio.techos.model.StatusOS;

public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long> {

    // As 5 últimas OS (painel)
    List<OrdemServico> findTop5ByOrderByDataEntradaDesc();

    // OS que estão em qualquer um dos status informados (painel)
    List<OrdemServico> findByStatusIn(Collection<StatusOS> status);
}