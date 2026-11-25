package br.com.bugnacio.techos.controller;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.NumberFormat;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.bugnacio.techos.model.Equipamento;
import br.com.bugnacio.techos.model.OrdemServico;
import br.com.bugnacio.techos.model.StatusOS;
import br.com.bugnacio.techos.repository.EquipamentoRepository;
import br.com.bugnacio.techos.repository.OrdemServicoRepository;
import br.com.bugnacio.techos.repository.PecaUsadaRepository;
import br.com.bugnacio.techos.service.AnexoOsService;
import br.com.bugnacio.techos.service.OrdemServicoService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/os")
public class OrdemServicoController {

    private final OrdemServicoRepository osRepo;
    private final PecaUsadaRepository pecaRepo;
    private final EquipamentoRepository equipamentoRepo;
    private final OrdemServicoService osService;
    private final AnexoOsService anexoService;
    private final String dddPadrao;
    private final String senhaAdmin;

    public OrdemServicoController(OrdemServicoRepository osRepo, PecaUsadaRepository pecaRepo,
            EquipamentoRepository equipamentoRepo, OrdemServicoService osService,
            AnexoOsService anexoService,
            @Value("${techos.ddd-padrao:}") String dddPadrao,
            @Value("${techos.senha-admin:}") String senhaAdmin) {
        this.osRepo = osRepo;
        this.pecaRepo = pecaRepo;
        this.equipamentoRepo = equipamentoRepo;
        this.osService = osService;
        this.anexoService = anexoService;
        this.dddPadrao = dddPadrao == null ? "" : dddPadrao.trim();
        this.senhaAdmin = senhaAdmin == null ? "" : senhaAdmin;
    }

    // ---------- Senha de administrador ----------
    private boolean senhaOk(String informada) {
        if (senhaAdmin.isBlank() || informada == null) return false;
        return MessageDigest.isEqual(senhaAdmin.getBytes(StandardCharsets.UTF_8),
                informada.getBytes(StandardCharsets.UTF_8));
    }

    // Aqui você muda os textos de erro da senha
    private String erroSenha(String informada) {
        if (senhaAdmin.isBlank()) {
            return "Senha de administrador não configurada (techos.senha-admin no application.properties).";
        }
        if (informada == null || informada.isBlank()) {
            return "Digite a senha de administrador para continuar.";
        }
        return "Senha incorreta. Tente novamente.";
    }

    // ---------- LISTA ----------
    @GetMapping
    public String listar(@RequestParam(value = "busca", required = false) String busca,
            @RequestParam(value = "status", required = false) StatusOS status, Model model) {

        List<OrdemServico> lista = osRepo.findAll(Sort.by(Sort.Direction.DESC, "id"));
        if (status != null) {
            lista = lista.stream().filter(o -> o.getStatus() == status).toList();
        }
        if (busca != null && !busca.isBlank()) {
            String b = norm(busca.trim()).replace("#", "");
            lista = lista.stream().filter(o -> corresponde(o, b)).toList();
        }

        model.addAttribute("ordens", lista);
        model.addAttribute("busca", busca);
        model.addAttribute("statusSel", status);
        model.addAttribute("statusLista", StatusOS.values());
        return "os/lista";
    }

    // Minúsculas e sem acento, para "joao" achar "João"
    private static String norm(String t) {
        if (t == null) return "";
        return Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }

    private boolean corresponde(OrdemServico o, String b) {
        Equipamento e = o.getEquipamento();
        return String.valueOf(o.getId()).equals(b)
                || contem(e.getMarca(), b) || contem(e.getModelo(), b) || contem(e.getNumeroSerie(), b)
                || (e.getCliente() != null && contem(e.getCliente().getNome(), b));
    }

    private boolean contem(String texto, String b) {
        return norm(texto).contains(b);
    }

    // ---------- NOVA ----------
    @GetMapping("/nova")
    public String nova(Model model) {
        model.addAttribute("os", new OrdemServico());
        carregarEquipamentos(model);
        return "os/form";
    }

    // ---------- SALVAR (com fotos opcionais) ----------
    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("os") OrdemServico form, BindingResult result,
            @RequestParam(value = "fotos", required = false) List<MultipartFile> fotos,
            Model model, RedirectAttributes redirect) {

        Long equipamentoId = form.getEquipamento() == null ? null : form.getEquipamento().getId();
        Equipamento equipamento = null;
        if (equipamentoId == null) {
            if (!result.hasFieldErrors("equipamento")) {
                result.rejectValue("equipamento", "obrigatorio", "Selecione o equipamento.");
            }
        } else {
            equipamento = equipamentoRepo.findById(equipamentoId).orElse(null);
            if (equipamento == null) {
                result.rejectValue("equipamento", "invalido", "Equipamento não encontrado.");
            }
        }
        if (result.hasErrors()) {
            carregarEquipamentos(model);
            return "os/form";
        }

        boolean nova = form.getId() == null;
        OrdemServico os;
        if (nova) {
            os = new OrdemServico();
        } else {
            Optional<OrdemServico> existente = osRepo.findById(form.getId());
            if (existente.isEmpty()) {
                redirect.addFlashAttribute("erro", "OS não encontrada.");
                return "redirect:/os";
            }
            os = existente.get();
        }

        os.setEquipamento(equipamento);
        os.setDefeito(form.getDefeito().trim());
        os.setLaudo(form.getLaudo() == null || form.getLaudo().isBlank() ? null : form.getLaudo().trim());
        os.setTecnicoNome(form.getTecnicoNome().trim());
        os.setValor(form.getValor());
        os.setGarantiaDias(form.getGarantiaDias());

        OrdemServico salva = osRepo.save(os);

        String msg = nova ? "OS #" + salva.getId() + " aberta. Adicione as peças, se precisar."
                : "OS #" + salva.getId() + " atualizada.";

        // Fotos enviadas junto com o formulário (opcional)
        if (fotos != null && fotos.stream().anyMatch(f -> !f.isEmpty())) {
            try {
                AnexoOsService.Resultado r = anexoService.salvar(salva, fotos);
                if (r.salvos() > 0) msg += " " + r.salvos() + " foto(s) enviada(s).";
                if (!r.recusados().isEmpty()) {
                    redirect.addFlashAttribute("erro", "Não enviado(s): " + String.join(", ", r.recusados())
                            + ". Aceitamos JPG, PNG, WEBP e GIF (máximo " + AnexoOsService.MAX_POR_OS + " fotos por OS).");
                }
            } catch (IllegalStateException ex) {
                redirect.addFlashAttribute("erro", ex.getMessage());
            }
        }
        redirect.addFlashAttribute("mensagem", msg);
        return "redirect:/os/" + salva.getId();
    }

    // ---------- EDITAR ----------
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable("id") Long id, Model model, RedirectAttributes redirect) {
        Optional<OrdemServico> os = osRepo.findById(id);
        if (os.isEmpty()) {
            redirect.addFlashAttribute("erro", "OS não encontrada.");
            return "redirect:/os";
        }
        model.addAttribute("os", os.get());
        carregarEquipamentos(model);
        return "os/form";
    }

    // ---------- DETALHE ----------
    @GetMapping("/{id}")
    public String detalhe(@PathVariable("id") Long id, Model model, RedirectAttributes redirect) {
        Optional<OrdemServico> opt = osRepo.findById(id);
        if (opt.isEmpty()) {
            redirect.addFlashAttribute("erro", "OS não encontrada.");
            return "redirect:/os";
        }
        OrdemServico os = opt.get();

        model.addAttribute("os", os);
        model.addAttribute("pecas", pecaRepo.findByOsIdOrderByIdAsc(id));
        model.addAttribute("statusLista", StatusOS.values());
        model.addAttribute("garantiaAte", os.getGarantiaAte());

        if (os.getStatus().isFinalizada()) {
            model.addAttribute("linkWhatsapp", montarLinkWhatsapp(os));
        }
        return "os/detalhe";
    }

    // ---------- COMPROVANTE ----------
    @GetMapping("/{id}/comprovante")
    public String comprovante(@PathVariable("id") Long id, Model model, RedirectAttributes redirect) {
        Optional<OrdemServico> os = osRepo.findById(id);
        if (os.isEmpty()) {
            redirect.addFlashAttribute("erro", "OS não encontrada.");
            return "redirect:/os";
        }
        model.addAttribute("os", os.get());
        model.addAttribute("pecas", pecaRepo.findByOsIdOrderByIdAsc(id));
        model.addAttribute("garantiaAte", os.get().getGarantiaAte());
        return "os/comprovante";
    }

    // ---------- MUDAR STATUS (reativar uma OS cancelada exige a senha) ----------
    @PostMapping("/{id}/status")
    public String mudarStatus(@PathVariable("id") Long id, @RequestParam("status") StatusOS status,
            @RequestParam(value = "senha", required = false) String senha, RedirectAttributes redirect) {

        Optional<OrdemServico> atual = osRepo.findById(id);
        if (atual.isPresent() && atual.get().getStatus() == StatusOS.CANCELADA
                && status != StatusOS.CANCELADA && !senhaOk(senha)) {
            redirect.addFlashAttribute("erro", "Para reativar uma OS cancelada: " + erroSenha(senha));
            return "redirect:/os/" + id;
        }
        try {
            osService.mudarStatus(id, status);
            String msg = "Status atualizado para " + status.getDescricao() + ".";
            if (status == StatusOS.CONCLUIDA) msg += " Avise o cliente pelo botão de WhatsApp.";
            redirect.addFlashAttribute("mensagem", msg);
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/os/" + id;
    }

    // ---------- EXCLUIR (exige a senha) ----------
    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable("id") Long id,
            @RequestParam(value = "senha", required = false) String senha, RedirectAttributes redirect) {
        if (!senhaOk(senha)) {
            redirect.addFlashAttribute("erro", "Para excluir a OS: " + erroSenha(senha));
            return "redirect:/os/" + id;
        }
        try {
            osService.excluir(id);
            redirect.addFlashAttribute("mensagem", "OS #" + id + " excluída. As peças voltaram ao estoque.");
            return "redirect:/os";
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
            return "redirect:/os/" + id;
        }
    }

    // ---------- PEÇAS ----------
    @PostMapping("/{id}/pecas")
    public String adicionarPeca(@PathVariable("id") Long id, @RequestParam("codigo") String codigo,
            @RequestParam(value = "quantidade", defaultValue = "1") Integer quantidade,
            RedirectAttributes redirect) {
        try {
            osService.adicionarPeca(id, codigo, quantidade == null ? 1 : quantidade);
            redirect.addFlashAttribute("mensagem", "Peça adicionada e reservada.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/os/" + id;
    }

    @PostMapping("/{id}/pecas/{pecaId}/remover")
    public String removerPeca(@PathVariable("id") Long id, @PathVariable("pecaId") Long pecaId,
            RedirectAttributes redirect) {
        try {
            osService.removerPeca(id, pecaId);
            redirect.addFlashAttribute("mensagem", "Peça removida da OS.");
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/os/" + id;
    }

    // ---------- Auxiliares ----------
    private void carregarEquipamentos(Model model) {
        List<Equipamento> equipamentos = equipamentoRepo.findAll(Sort.by("marca")).stream()
                .filter(e -> e.getCliente() != null)
                .toList();
        model.addAttribute("equipamentos", equipamentos);
    }

    // Mensagem de aviso para o WhatsApp
    private String textoAviso(OrdemServico os) {
        String nome = os.getEquipamento().getCliente() == null ? "" : os.getEquipamento().getCliente().getNome();
        String primeiro = nome == null ? "" : nome.trim().split("\\s+")[0];

        StringBuilder msg = new StringBuilder();
        msg.append("Olá, ").append(primeiro).append("! ");
        msg.append("Seu equipamento ").append(os.getEquipamento().getMarca()).append(" ")
                .append(os.getEquipamento().getModelo()).append(" (OS #").append(os.getId())
                .append(") está pronto para retirada na Bugnácio Tech.");
        if (os.getValor() != null && os.getValor().compareTo(BigDecimal.ZERO) > 0) {
            msg.append(" Valor do serviço: ")
                    .append(NumberFormat.getCurrencyInstance(Locale.of("pt", "BR")).format(os.getValor()))
                    .append(".");
        }
        LocalDate ate = os.getGarantiaAte();
        if (ate != null) {
            msg.append(" Garantia de ").append(os.getGarantiaDias()).append(" dias, válida até ")
                    .append(ate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append(".");
        }
        msg.append(" Qualquer dúvida, é só responder esta mensagem.");
        return msg.toString();
    }

    private String montarLinkWhatsapp(OrdemServico os) {
        if (os.getEquipamento().getCliente() == null) return null;
        String fone = telefoneWhatsapp(os.getEquipamento().getCliente().getTelefone());
        if (fone == null) return null;
        String texto = URLEncoder.encode(textoAviso(os), StandardCharsets.UTF_8).replace("+", "%20");
        return "https://wa.me/" + fone + "?text=" + texto;
    }

    private String telefoneWhatsapp(String telefone) {
        if (telefone == null) return null;
        String d = telefone.replaceAll("\\D", "");
        if (d.startsWith("55") && d.length() >= 12) return d;
        if (d.length() == 10 || d.length() == 11) return "55" + d;
        if ((d.length() == 8 || d.length() == 9) && !dddPadrao.isBlank()) return "55" + dddPadrao + d;
        return null;
    }
}