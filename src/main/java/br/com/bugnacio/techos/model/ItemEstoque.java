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
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
public class ItemEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Informe o nome do item.")
    @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres.")
    private String nome;

    @NotBlank(message = "Informe o código.")
    @Size(max = 30, message = "O código pode ter no máximo 30 caracteres.")
    @Pattern(regexp = "^[A-Za-z0-9._-]*$",
             message = "Use apenas letras, números, ponto, hífen e sublinhado (sem espaços).")
    @Column(unique = true)
    private String codigo;

    // Opcional. Se preenchido, 4 a 50 caracteres sem espaços.
    @Pattern(regexp = "^[A-Za-z0-9._-]{4,50}$",
             message = "Código de barras inválido (4 a 50 caracteres, sem espaços).")
    @Column(unique = true)
    private String codigoBarras;

    @NotNull(message = "Selecione a categoria.")
    @Enumerated(EnumType.STRING)
    private Estoque categoria;

    @NotNull(message = "Informe a quantidade.")
    @Min(value = 0, message = "A quantidade não pode ser negativa.")
    @Max(value = 100000, message = "A quantidade máxima é 100000.")
    private Integer quantidade = 0;

    @NotNull(message = "Informe o estoque mínimo.")
    @Min(value = 0, message = "O estoque mínimo não pode ser negativo.")
    @Max(value = 100000, message = "O estoque mínimo máximo é 100000.")
    private Integer estoqueMinimo = 1;

    @NotNull(message = "Informe o preço de custo.")
    @DecimalMin(value = "0.01", message = "O preço de custo deve ser maior que zero.")
    @Digits(integer = 8, fraction = 2, message = "Preço inválido.")
    private BigDecimal precoCusto;

    @NotNull(message = "Informe o preço de venda.")
    @DecimalMin(value = "0.01", message = "O preço de venda deve ser maior que zero.")
    @Digits(integer = 8, fraction = 2, message = "Preço inválido.")
    private BigDecimal precoVenda;

    // true = item arquivado: some das listas e buscas, mas o histórico fica guardado.
    private boolean inativo = false;

    // Reservado por OS abertas. Não vai para o banco: o controller preenche.
    @Transient
    private int reservado = 0;

    public boolean isZerado() {
        return quantidade != null && quantidade == 0;
    }

    public boolean isBaixo() {
        return quantidade != null && estoqueMinimo != null
                && quantidade > 0 && quantidade <= estoqueMinimo;
    }

    public BigDecimal getValorTotal() {
        if (quantidade == null || precoCusto == null) return BigDecimal.ZERO;
        return precoCusto.multiply(BigDecimal.valueOf(quantidade));
    }

    public int getDisponivel() {
        return (quantidade == null ? 0 : quantidade) - reservado;
    }

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

    public boolean isInativo() { return inativo; }
    public void setInativo(boolean inativo) { this.inativo = inativo; }

    public int getReservado() { return reservado; }
    public void setReservado(int reservado) { this.reservado = reservado; }
}