package br.com.bugnacio.techos.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// Uma linha por (OS, peça). Se a mesma peça for adicionada de novo, soma a quantidade.
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "os_id", "item_id" }))
public class PecaUsada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "os_id")
    private OrdemServico os;

    @ManyToOne(optional = false)
    @JoinColumn(name = "item_id")
    private ItemEstoque item;

    private Integer quantidade = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public OrdemServico getOs() { return os; }
    public void setOs(OrdemServico os) { this.os = os; }

    public ItemEstoque getItem() { return item; }
    public void setItem(ItemEstoque item) { this.item = item; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
}