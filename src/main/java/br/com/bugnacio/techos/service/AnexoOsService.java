package br.com.bugnacio.techos.service;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import br.com.bugnacio.techos.model.AnexoOs;
import br.com.bugnacio.techos.model.OrdemServico;
import br.com.bugnacio.techos.repository.AnexoOsRepository;

// Salva as fotos de uma OS (usado pela tela de nova OS e pela tela de detalhe).
@Service
public class AnexoOsService {

    public static final int MAX_POR_OS = 20;

    // Só imagens: extensão -> tipo servido ao navegador
    private static final Map<String, String> TIPOS = Map.of(
            "jpg", "image/jpeg", "jpeg", "image/jpeg",
            "png", "image/png", "webp", "image/webp", "gif", "image/gif");

    public record Resultado(int salvos, List<String> recusados) {}

    private final AnexoOsRepository anexoRepo;

    public AnexoOsService(AnexoOsRepository anexoRepo) {
        this.anexoRepo = anexoRepo;
    }

    public Resultado salvar(OrdemServico os, List<MultipartFile> arquivos) {
        try {
            Files.createDirectories(AnexoOs.PASTA);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível criar a pasta de arquivos.");
        }

        long total = anexoRepo.countByOsId(os.getId());
        int salvos = 0;
        List<String> recusados = new ArrayList<>();

        for (MultipartFile f : arquivos) {
            if (f.isEmpty()) continue;
            String nome = nomeSeguro(f.getOriginalFilename());
            int p = nome.lastIndexOf('.');
            String ext = p >= 0 ? nome.substring(p + 1).toLowerCase() : "";
            String mime = TIPOS.get(ext);

            if (mime == null || total + salvos >= MAX_POR_OS) {
                recusados.add(nome);
                continue;
            }
            String guardado = UUID.randomUUID() + "." + ext;
            try {
                f.transferTo(AnexoOs.PASTA.resolve(guardado));
            } catch (IOException e) {
                recusados.add(nome);
                continue;
            }
            AnexoOs a = new AnexoOs();
            a.setOs(os);
            a.setNomeOriginal(nome);
            a.setArquivo(guardado);
            a.setTipoMime(mime);
            a.setTamanho(f.getSize());
            anexoRepo.save(a);
            salvos++;
        }
        return new Resultado(salvos, recusados);
    }

    // Tira pastas do nome (alguns navegadores mandam o caminho completo)
    private String nomeSeguro(String original) {
        String n = original == null ? "arquivo" : original.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1);
        return n.length() > 150 ? n.substring(n.length() - 150) : n;
    }
}