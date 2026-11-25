package br.com.bugnacio.techos.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.bugnacio.techos.model.AnexoOs;
import br.com.bugnacio.techos.model.OrdemServico;
import br.com.bugnacio.techos.repository.AnexoOsRepository;
import br.com.bugnacio.techos.repository.OrdemServicoRepository;
import br.com.bugnacio.techos.service.AnexoOsService;

@Controller
@RequestMapping("/os/{osId}/anexos")
public class AnexoOsController {

    private final AnexoOsRepository anexoRepo;
    private final OrdemServicoRepository osRepo;
    private final AnexoOsService anexoService;

    public AnexoOsController(AnexoOsRepository anexoRepo, OrdemServicoRepository osRepo,
            AnexoOsService anexoService) {
        this.anexoRepo = anexoRepo;
        this.osRepo = osRepo;
        this.anexoService = anexoService;
    }

    @PostMapping
    public String enviar(@PathVariable("osId") Long osId,
            @RequestParam("arquivos") List<MultipartFile> arquivos, RedirectAttributes redirect) {

        OrdemServico os = osRepo.findById(osId).orElse(null);
        if (os == null) {
            redirect.addFlashAttribute("erro", "OS não encontrada.");
            return "redirect:/os";
        }
        try {
            AnexoOsService.Resultado r = anexoService.salvar(os, arquivos);
            if (r.salvos() > 0) redirect.addFlashAttribute("mensagem", r.salvos() + " foto(s) enviada(s).");
            if (!r.recusados().isEmpty()) {
                redirect.addFlashAttribute("erro", "Não enviado(s): " + String.join(", ", r.recusados())
                        + ". Aceitamos JPG, PNG, WEBP e GIF (máximo " + AnexoOsService.MAX_POR_OS + " fotos por OS).");
            }
        } catch (IllegalStateException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/os/" + osId;
    }

    // Mostra a foto
    @GetMapping("/{id}")
    public ResponseEntity<Resource> ver(@PathVariable("osId") Long osId, @PathVariable("id") Long id) {
        AnexoOs a = anexoRepo.findByIdAndOsId(id, osId).orElse(null);
        if (a == null) return ResponseEntity.notFound().build();

        Path arquivo = AnexoOs.PASTA.resolve(a.getArquivo()).normalize();
        if (!arquivo.startsWith(AnexoOs.PASTA) || !Files.exists(arquivo)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(a.getTipoMime()))
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(arquivo));
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable("osId") Long osId, @PathVariable("id") Long id,
            RedirectAttributes redirect) {
        anexoRepo.findByIdAndOsId(id, osId).ifPresent(anexoRepo::delete);
        redirect.addFlashAttribute("mensagem", "Foto removida.");
        return "redirect:/os/" + osId;
    }
}