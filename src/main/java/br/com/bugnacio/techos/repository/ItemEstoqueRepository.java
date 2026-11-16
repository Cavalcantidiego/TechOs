package br.com.bugnacio.techos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.bugnacio.techos.model.ItemEstoque;

public interface ItemEstoqueRepository extends JpaRepository<ItemEstoque, Long> {

    // Busca por nome, código interno OU código de barras (ignora maiúscula/minúscula)
    List<ItemEstoque> findByNomeContainingIgnoreCaseOrCodigoContainingIgnoreCaseOrCodigoBarrasContainingIgnoreCase(
            String nome, String codigo, String codigoBarras);

    // Acha UM item pela leitura da pistola: código interno OU código de barras
    @Query("select i from ItemEstoque i where upper(i.codigo) = upper(:cod) or i.codigoBarras = :cod")
    Optional<ItemEstoque> buscarPorLeitura(@Param("cod") String cod);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);
}