package br.com.bugnacio.techos.controller;
 
import java.util.List;
 
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
 
@Controller
@RequestMapping("/os")
public class OrdemServicoController {
 
    // Endereco: http://localhost:8080/os
    @GetMapping
    public String listar(Model model) {
        // A tela os/lista espera uma lista chamada "ordens".
        model.addAttribute("ordens", List.of());
 
        // Abre: templates/os/lista.html
        return "os/lista";
    }
}