package br.com.bugnacio.techos.model;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// @Entity: esta classe vira a tabela "item_estoque" no MySQL (criada pelo Hibernate).
@Entity
public class ItemEstoque {

    // Chave primária, gerada automaticamente pelo banco (AUTO_INCREMENT).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @NotBlank: não aceita vazio nem só espaços. A mensagem aparece no formulário.
    @NotBlank(message = "Informe o nome do item.")
    private String nome;

    // unique = true: o banco recusa dois itens com o mesmo código (proteção extra).
    @NotBlank(message = "Informe o código.")
    @Column(unique = true)
    private String codigo;

    // Código de barras de fábrica (EAN). Opcional, mas único quando preenchido.
    @Column(unique = true)
    private String codigoBarras;

    // EnumType.STRING: grava o texto ("PECA") no banco, não o número da posição.
    @NotNull(message = "Selecione a categoria.")
    @Enumerated(EnumType.STRING)
    private Estoque categoria;

    // @Min(0): não aceita quantidade negativa.
    @NotNull(message = "Informe a quantidade.")
    @Min(value = 0, message = "A quantidade não pode ser negativa.")
    private Integer quantidade = 0;

    // Quando a quantidade chega neste valor (ou menos), o item é marcado como "Baixo".
    @NotNull(message = "Informe o estoque mínimo.")
    @Min(value = 0, message = "O estoque mínimo não pode ser negativo.")
    private Integer estoqueMinimo = 1;

    // BigDecimal: tipo correto para dinheiro (double perde precisão nos centavos).
    @NotNull(message = "Informe o preço de custo.")
    @DecimalMin(value = "0.00", message = "O preço não pode ser negativo.")
    private BigDecimal precoCusto = BigDecimal.ZERO;

    @NotNull(message = "Informe o preço de venda.")
    @DecimalMin(value = "0.00", message = "O preço não pode ser negativo.")
    private BigDecimal precoVenda = BigDecimal.ZERO;

    // NOVO: quanto está reservado por OS abertas.
    // @Transient: NÃO vira coluna no banco. O controller preenche na hora de listar.
    @Transient
    private int reservado = 0;

    // ---- Métodos auxiliares usados na tela (o HTML lê como i.zerado, i.baixo) ----

    // Zerado: acabou o estoque.
    public boolean isZerado() {
        return quantidade != null && quantidade == 0;
    }

    // Baixo: ainda tem, mas está no mínimo ou abaixo dele.
    public boolean isBaixo() {
        return quantidade != null && estoqueMinimo != null
                && quantidade > 0 && quantidade <= estoqueMinimo;
    }

    // Valor parado em estoque = custo x quantidade (usado no card do topo).
    public BigDecimal getValorTotal() {
        if (quantidade == null || precoCusto == null) return BigDecimal.ZERO;
        return precoCusto.multiply(BigDecimal.valueOf(quantidade));
    }

    // NOVO: o que de fato pode ser prometido a uma nova OS (físico menos reservado).
    public int getDisponivel() {
        return (quantidade == null ? 0 : quantidade) - reservado;
    }

    // ---- Getters e setters (o Spring/Thymeleaf usam para ler e preencher os campos) ----

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getCodigoBarras() { return codigoBarras; }
    public void setCodigoBarras(String codigoBarras) {
        // vazio vira null, para o banco não achar que "" é duplicado
        this.codigoBarras = (codigoBarras == null || codigoBarras.isBlank()) ? null : codigoBarras.trim();
    }

    public Estoque getCategoria() { return categoria; }
    public void setCategoria(Estoque categoria) { this.categoria = categoria; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public Integer getEstoqueMinimo() { return estoqueMinimo; }
    public void setEstoqueMinimo(Integer estoqueMinimo) { this.estoqueMinimo = estoqueMinimo; }

    public BigDecimal getPrecoCusto() { return precoCusto; }
    public void setPrecoCusto(BigDecimal precoCusto) { this.precoCusto = precoCusto; }

    public BigDecimal getPrecoVenda() { return precoVenda; }
    public void setPrecoVenda(BigDecimal precoVenda) { this.precoVenda = precoVenda; }

    // NOVO
    public int getReservado() { return reservado; }
    public void setReservado(int reservado) { this.reservado = reservado; }
}