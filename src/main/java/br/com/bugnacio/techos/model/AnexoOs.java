package br.com.bugnacio.techos.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostRemove;

// Foto de uma OS. O arquivo fica na pasta uploads/os; o banco guarda só o nome.
@Entity
public class AnexoOs {

    public static final Path PASTA = Path.of("uploads", "os").toAbsolutePath().normalize();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "os_id")
    private OrdemServico os;

    private String nomeOriginal;
    private String arquivo;   // nome gerado (UUID)
    private String tipoMime;
    private Long tamanho;
    private LocalDateTime dataHora = LocalDateTime.now();

    // Ao apagar o registro, apaga o arquivo do disco também
    @PostRemove
    void apagarArquivo() {
        try { Files.deleteIfExists(PASTA.resolve(arquivo)); } catch (IOException e) { /* ignora */ }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public OrdemServico getOs() { return os; }
    public void setOs(OrdemServico os) { this.os = os; }
    public String getNomeOriginal() { return nomeOriginal; }
    public void setNomeOriginal(String nomeOriginal) { this.nomeOriginal = nomeOriginal; }
    public String getArquivo() { return arquivo; }
    public void setArquivo(String arquivo) { this.arquivo = arquivo; }
    public String getTipoMime() { return tipoMime; }
    public void setTipoMime(String tipoMime) { this.tipoMime = tipoMime; }
    public Long getTamanho() { return tamanho; }
    public void setTamanho(Long tamanho) { this.tamanho = tamanho; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
}