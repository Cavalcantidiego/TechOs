package br.com.bugnacio.techos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.bugnacio.techos.model.Cliente;

// JpaRepository<Cliente, Long> já traz pronto: save, findAll, findById, deleteById...
// Cliente = a entidade (tabela); Long = o tipo do id.
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // O Spring monta a query lendo o NOME do método, então a escrita tem que ser exata.
    // Busca clientes cujo nome CONTÉM o texto, ignorando maiúscula/minúscula.
    List<Cliente> findByNomeContainingIgnoreCase(String nome);

    // Retorna true se já existe um cliente com esse telefone.
    boolean existsByTelefone(String telefone);

    // Retorna true se existe um cliente com esse telefone e id DIFERENTE do informado
    // (usado na edição, para não acusar duplicado o próprio cliente).
    boolean existsByTelefoneAndIdNot(String telefone, Long id);
}