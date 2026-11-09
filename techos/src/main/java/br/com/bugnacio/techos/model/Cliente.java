package br.com.bugnacio.techos.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

// @entity: essa classe representa uma tabela no banco de dados.
@Entity 
public class Cliente {
    // @id: o campo que representa a chave primária da tabela.
    // @GeneratedValue: o valor da chave primária será gerado automaticamente pelo banco de dados.
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Cada atributo vira uma coluna na tabela do banco de dados. O nome da coluna será o mesmo do atributo, mas você pode mudar isso usando a anotação @Column.
    private String nome;
    private String telefone;
    private String email;

    // Construtor padrão (sem argumentos) necessário para o JPA.
    public Cliente() {
        
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
