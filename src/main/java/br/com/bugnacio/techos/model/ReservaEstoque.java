package br.com.bugnacio.techos.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// Quanto de uma peça uma OS separou. Existe só enquanto a peça não foi usada.
// Uma linha por (peça, OS).
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "item_id", "os_id" }))
public class ReservaEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "item_id")
    private ItemEstoque item;

    @JoinColumn(name = "os_id")
    private Long osId;

    private Integer quantidade = 0;

    private Long tecnicoId;
    private String tecnicoNome;

    private LocalDateTime dataHora = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ItemEstoque getItem() { return item; }
    public void setItem(ItemEstoque item) { this.item = item; }

    public Long getOsId() { return osId; }
    public void setOsId(Long osId) { this.osId = osId; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public Long getTecnicoId() { return tecnicoId; }
    public void setTecnicoId(Long tecnicoId) { this.tecnicoId = tecnicoId; }

    public String getTecnicoNome() { return tecnicoNome; }
    public void setTecnicoNome(String tecnicoNome) { this.tecnicoNome = tecnicoNome; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
}