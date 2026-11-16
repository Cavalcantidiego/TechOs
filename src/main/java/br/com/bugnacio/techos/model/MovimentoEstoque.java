package br.com.bugnacio.techos.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

// Histórico: cada linha é uma movimentação. Nunca é editada nem apagada.
@Entity
public class MovimentoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Qual peça
    @ManyToOne(optional = false)
    @JoinColumn(name = "item_id")
    private ItemEstoque item;

    @Enumerated(EnumType.STRING)
    private TipoMovimento tipo;

    private Integer quantidade;

    // Quanto ficou no estoque físico depois desta movimentação
    private Integer saldoApos;

    private LocalDateTime dataHora = LocalDateTime.now();

    private String motivo;

    // Em qual OS (null em entradas). Só um número: a OS ainda não existe.
    private Long osId;

    // Técnico responsável (opcional, até existir o cadastro de técnicos)
    private Long tecnicoId;
    private String tecnicoNome;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ItemEstoque getItem() { return item; }
    public void setItem(ItemEstoque item) { this.item = item; }

    public TipoMovimento getTipo() { return tipo; }
    public void setTipo(TipoMovimento tipo) { this.tipo = tipo; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public Integer getSaldoApos() { return saldoApos; }
    public void setSaldoApos(Integer saldoApos) { this.saldoApos = saldoApos; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public Long getOsId() { return osId; }
    public void setOsId(Long osId) { this.osId = osId; }

    public Long getTecnicoId() { return tecnicoId; }
    public void setTecnicoId(Long tecnicoId) { this.tecnicoId = tecnicoId; }

    public String getTecnicoNome() { return tecnicoNome; }
    public void setTecnicoNome(String tecnicoNome) { this.tecnicoNome = tecnicoNome; }
}