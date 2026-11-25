package br.com.bugnacio.techos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.bugnacio.techos.model.ReservaEstoque;

public interface ReservaEstoqueRepository extends JpaRepository<ReservaEstoque, Long> {

    Optional<ReservaEstoque> findByItemIdAndOsId(Long itemId, Long osId);

    // Total reservado de uma peça (todas as OS abertas)
    @Query("select coalesce(sum(r.quantidade), 0) from ReservaEstoque r where r.item.id = :itemId")
    Long totalReservado(@Param("itemId") Long itemId);

    // Total reservado de todas as peças: [itemId, soma]
    @Query("select r.item.id, sum(r.quantidade) from ReservaEstoque r group by r.item.id")
    List<Object[]> totalReservadoPorItem();

    // Total geral reservado (para o painel)
    @Query("select coalesce(sum(r.quantidade), 0) from ReservaEstoque r")
    Long totalGeralReservado();
    void deleteByItemId(Long itemId);
}