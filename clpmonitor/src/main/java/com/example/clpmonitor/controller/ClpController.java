package com.example.clpmonitor.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.clpmonitor.model.TagWriteRequest;
import com.example.clpmonitor.service.ClpSimulatorService;
import com.example.clpmonitor.util.TagValueParser;

@Controller
public class ClpController {

    // Injeta automaticamente uma instância da classe ClpSimulatorService.
    // Essa classe é responsável por simular os dados dos CLPs e gerenciar
    // os eventos SSE que serão enviados ao frontend.
    @Autowired
    private ClpSimulatorService simulatorService;

    // Mapeia a URL raiz (http://localhost:8080/) para o método index().
    // Retorna a view index.html, localizada em src/main/resources/templates/index.html (Thymeleaf).
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("tag", new TagWriteRequest());
        return "index";
    }

    // Rota "/clp-data-stream" — Comunicação via SSE (Server-Sent Events)
    // Essa rota é chamada no JavaScript pelo EventSource:
    @GetMapping("/clp-data-stream")

    // Retorna um objeto SseEmitter, que é a classe do Spring para enviar
    // dados do servidor para o cliente continuamente usando Server-Sent Events.
    public SseEmitter streamClpData() {
        // Esse método delega a lógica para simulatorService.subscribe() que:
        //  Cria o SseEmitter.
        //  Armazena ele numa lista de ouvintes (clientes conectados).
        //  Inicia o envio periódico dos dados simulados
        return simulatorService.subscribe();
    }

    @PostMapping("/write-tag")
    public String writeTag(@ModelAttribute TagWriteRequest request, Model model) {
        try {
            Object typedValue = TagValueParser.parseValue(request.getValue(), request.getType());

            // lógica de escrita no CLP aqui...
            // Aqui você escreveria no CLP usando IP, DB, Offset etc.
            // Simulação:
            System.out.println("\nEscrevendo no CLP: " + request.getIp() + "\n"
                    + " | DB: " + request.getDb() + "\n"
                    + " | Offset: " + request.getOffset() + "\n"
                    + " | Type: " + request.getType() + "\n"
                    + " | Valor convertido: " + typedValue + "\n");

            model.addAttribute("message", "Valor escrito com sucesso!");
        } catch (Exception e) {
            model.addAttribute("error", "Erro ao escrever valor: " + e.getMessage());
        }

        // ✅ Adiciona de novo o objeto para o formulário não quebrar na volta
        model.addAttribute("tag", new TagWriteRequest());

        return "index";
    }

    @GetMapping("/fragmento-formulario")
    public String carregarFragmentoFormulario(Model model) {
        model.addAttribute("tag", new TagWriteRequest()); // substitua pelo seu DTO real
        return "fragments/formulario :: clp-write-fragment";
    }

    
    @PostMapping("/updateSimulation")
    public String startSimulation() {
        simulatorService.startSimulation();
        return "redirect:/fragmento-formulario";
    }

    /*
     * Descrição do Funcionamento:
     * 
       1 - O usuário acessa http://localhost:8080/ → o método index() retorna o HTML.

       2 - O HTML carrega o JavaScript (scripts.js), que cria:

            const eventSource = new EventSource('/clp-data-stream');

       3 - O navegador faz uma requisição GET para /clp-data-stream.

       4 - O Spring chama simulatorService.subscribe() e devolve um SseEmitter ao navegador.

       5 - A cada X milissegundos, o ClpSimulatorService envia eventos como:

            clp1-data
            clp2-data
            clp3-data
            clp4-data

       6 - O JavaScript escuta cada evento separadamente e atualiza a interface conforme os dados recebidos.
     */
}
