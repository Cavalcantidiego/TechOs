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

/*
 * Controller responsável pelos anexos (fotos) de uma Ordem de Serviço.
 * Todas as rotas começam com:
 * /os/{osId}/anexos
 */
@Controller
@RequestMapping("/os/{osId}/anexos")
public class AnexoOsController {

    private final AnexoOsRepository anexoRepo;
    private final OrdemServicoRepository osRepo;
    private final AnexoOsService anexoService;

    // Injeção de dependências
    public AnexoOsController(
            AnexoOsRepository anexoRepo,
            OrdemServicoRepository osRepo,
            AnexoOsService anexoService) {

        this.anexoRepo = anexoRepo;
        this.osRepo = osRepo;
        this.anexoService = anexoService;
    }

    /*
     * Recebe os arquivos enviados pelo formulário
     * e vincula à OS informada.
     */
    @PostMapping
    public String enviar(
            @PathVariable("osId") Long osId,
            @RequestParam("arquivos") List<MultipartFile> arquivos,
            RedirectAttributes redirect) {

        // Busca a OS no banco
        OrdemServico os = osRepo.findById(osId).orElse(null);

        // Se a OS não existir, retorna para a listagem
        if (os == null) {
            redirect.addFlashAttribute("erro", "OS não encontrada.");
            return "redirect:/os";
        }

        try {

            // Delega toda a validação e gravação para o Service
            AnexoOsService.Resultado r = anexoService.salvar(os, arquivos);

            // Mensagem de sucesso
            if (r.salvos() > 0) {
                redirect.addFlashAttribute(
                        "mensagem",
                        r.salvos() + " foto(s) enviada(s).");
            }

            // Informa quais arquivos foram recusados
            if (!r.recusados().isEmpty()) {
                redirect.addFlashAttribute(
                        "erro",
                        "Não enviado(s): "
                                + String.join(", ", r.recusados())
                                + ". Aceitamos JPG, PNG, WEBP e GIF (máximo "
                                + AnexoOsService.MAX_POR_OS
                                + " fotos por OS).");
            }

        } catch (IllegalStateException ex) {

            // Erros de regra de negócio
            redirect.addFlashAttribute("erro", ex.getMessage());
        }

        // Volta para a tela da OS
        return "redirect:/os/" + osId;
    }

    /*
     * Exibe uma foto anexada à OS.
     * Exemplo:
     * /os/10/anexos/5
     */
    @GetMapping("/{id}")
    public ResponseEntity<Resource> ver(
            @PathVariable("osId") Long osId,
            @PathVariable("id") Long id) {

        // Busca o anexo garantindo que pertence à OS informada
        AnexoOs a = anexoRepo.findByIdAndOsId(id, osId).orElse(null);

        if (a == null) {
            return ResponseEntity.notFound().build();
        }

        // Monta o caminho físico do arquivo
        Path arquivo = AnexoOs.PASTA
                .resolve(a.getArquivo())
                .normalize();

        /*
         * Segurança:
         * - verifica se o arquivo continua dentro da pasta permitida
         * - verifica se existe fisicamente no disco
         */
        if (!arquivo.startsWith(AnexoOs.PASTA) || !Files.exists(arquivo)) {
            return ResponseEntity.notFound().build();
        }

        // Retorna a imagem para o navegador
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(a.getTipoMime()))
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(arquivo));
    }

    /*
     * Remove um anexo da OS.
     */
    @PostMapping("/{id}/excluir")
    public String excluir(
            @PathVariable("osId") Long osId,
            @PathVariable("id") Long id,
            RedirectAttributes redirect) {

        // Exclui apenas se o anexo pertencer à OS informada
        anexoRepo.findByIdAndOsId(id, osId)
                .ifPresent(anexoRepo::delete);

        redirect.addFlashAttribute("mensagem", "Foto removida.");

        return "redirect:/os/" + osId;
    }
}