package br.com.bugnacio.techos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
public class OrdemServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Qual equipamento (o cliente vem dele)
    @NotNull(message = "Selecione o equipamento.")
    @ManyToOne
    @JoinColumn(name = "equipamento_id")
    private Equipamento equipamento;

    // O que o cliente relatou
    @NotBlank(message = "Descreva o defeito relatado pelo cliente.")
    @Column(length = 1000)
    private String defeito;

    // O que o técnico encontrou e fez
    @Column(length = 2000)
    private String laudo;

    // Texto livre até existir o cadastro de técnicos
    private String tecnicoNome;

    @Enumerated(EnumType.STRING)
    private StatusOS status = StatusOS.RECEBIDO;

    private LocalDateTime dataEntrada = LocalDateTime.now();
    private LocalDateTime dataConclusao;

    // Valor do serviço (mão de obra). Peças não são cobradas.
    @DecimalMin(value = "0.00", message = "O valor não pode ser negativo.")
    private BigDecimal valor;

    // true = as peças já saíram do estoque (OS Concluída/Entregue)
    private boolean baixado = false;

    // Peças só podem ser mexidas enquanto não foram baixadas e a OS não foi cancelada
    public boolean isEditavelPecas() {
        return !baixado && status != StatusOS.CANCELADA;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Equipamento getEquipamento() { return equipamento; }
    public void setEquipamento(Equipamento equipamento) { this.equipamento = equipamento; }

    public String getDefeito() { return defeito; }
    public void setDefeito(String defeito) { this.defeito = defeito; }

    public String getLaudo() { return laudo; }
    public void setLaudo(String laudo) { this.laudo = laudo; }

    public String getTecnicoNome() { return tecnicoNome; }
    public void setTecnicoNome(String tecnicoNome) { this.tecnicoNome = tecnicoNome; }

    public StatusOS getStatus() { return status; }
    public void setStatus(StatusOS status) { this.status = status; }

    public LocalDateTime getDataEntrada() { return dataEntrada; }
    public void setDataEntrada(LocalDateTime dataEntrada) { this.dataEntrada = dataEntrada; }

    public LocalDateTime getDataConclusao() { return dataConclusao; }
    public void setDataConclusao(LocalDateTime dataConclusao) { this.dataConclusao = dataConclusao; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public boolean isBaixado() { return baixado; }
    public void setBaixado(boolean baixado) { this.baixado = baixado; }
}