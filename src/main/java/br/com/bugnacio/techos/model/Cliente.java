package br.com.bugnacio.techos.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// @entity: essa classe representa uma tabela no banco de dados.
@Entity 
public class Cliente {
    // @id: o campo que representa a chave primária da tabela.
    // @GeneratedValue: o valor da chave primária será gerado automaticamente pelo banco de dados.
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //@NotBlank: o nome do cliente nao pode ser vazio 
    //@ size: tamanho maximo. meesage: o texto que aparece do lado 
    
    @NotBlank (message = "O nome do cliente é obrigatório")
    @Size (max = 100, message = "Nome do cliente não pode ter mais de 100 caracteres")

    // Cada atributo vira uma coluna na tabela do banco de dados. O nome da coluna será o mesmo do atributo, mas você pode mudar isso usando a anotação @Column.
    private String nome;

    @NotBlank  (message = "Informe o telefone do cliente")
    private String telefone;

    // @Emanil: confere o formato do email. Se nao for valido, mostra a mensagem 
    // Aceite vazie, mas se for preecinho, precisa ser valido 

    @Email (message = "Informe um email valido")
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
