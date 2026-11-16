package br.com.bugnacio.techos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.bugnacio.techos.model.MovimentoEstoque;

public interface MovimentoEstoqueRepository extends JpaRepository<MovimentoEstoque, Long> {

    List<MovimentoEstoque> findByItemIdOrderByDataHoraDesc(Long itemId);

    List<MovimentoEstoque> findTop100ByOrderByDataHoraDesc();

    List<MovimentoEstoque> findTop5ByOrderByDataHoraDesc();

    // Quanto desta peça a OS já consumiu (saídas menos devoluções).
    // Usado para não aceitar devolução maior que o uso.
    @Query("""
            select coalesce(sum(case
                when m.tipo = br.com.bugnacio.techos.model.TipoMovimento.SAIDA_OS then m.quantidade
                when m.tipo = br.com.bugnacio.techos.model.TipoMovimento.DEVOLUCAO_OS then -m.quantidade
                else 0 end), 0)
            from MovimentoEstoque m
            where m.item.id = :itemId and m.osId = :osId
            """)
    Long usoLiquidoDaOs(@Param("itemId") Long itemId, @Param("osId") Long osId);
}