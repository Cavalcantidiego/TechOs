package br.com.bugnacio.techos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import br.com.bugnacio.techos.model.Cliente;

// Interface (não é classe): não escrevemos o corpo da classe, o Spring Data JPA faz isso para nós.
// A interface JpaRepository já possui métodos prontos para realizar operações de CRUD (Create, Read, Update, Delete) no banco de dados.
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

}
