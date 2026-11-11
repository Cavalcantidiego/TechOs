package br.com.bugnacio.techos.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// @Entity: esta classe vira uma tabela no banco (equipamento).
@Entity
public class Equipamento {

    // @Id: chave primária. @GeneratedValue: o banco gera o número sozinho.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // O tipo é um enum (lista fixa de opções).
    // @Enumerated(EnumType.STRING): grava o nome ("NOTEBOOK") e não o número.
    // @NotNull: obrigatório (para enum não se usa @NotBlank).
    @NotNull(message = "Selecione o tipo do equipamento")
    @Enumerated(EnumType.STRING)
    private TipoEquipamento tipo;

    // @NotBlank: não aceita vazio nem só espaços.
    @NotBlank(message = "A marca é obrigatória")
    private String marca;

    @NotBlank(message = "O modelo é obrigatório")
    private String modelo;

    // Opcional: sem validação.
    private String numeroSerie;

    // @ManyToOne: muitos equipamentos pertencem a UM cliente.
    // @JoinColumn: cria a coluna cliente_id (chave estrangeira) na tabela.
    @ManyToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    // Construtor padrão: o JPA precisa dele.
    public Equipamento() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoEquipamento getTipo() {
        return tipo;
    }

    public void setTipo(TipoEquipamento tipo) {
        this.tipo = tipo;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getNumeroSerie() {
        return numeroSerie;
    }

    public void setNumeroSerie(String numeroSerie) {
        this.numeroSerie = numeroSerie;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }
}