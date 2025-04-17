package com.example.clpmonitor.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.clpmonitor.model.TagWriteRequest;
import com.example.clpmonitor.plc.PlcConnector;
import com.example.clpmonitor.service.ClpSimulatorService;

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
public String writeTag(
        @RequestParam String ip,
        @RequestParam int port,
        @RequestParam String type,
        @RequestParam int db,
        @RequestParam int offset,
        @RequestParam(required = false) Integer size,
        @RequestParam(required = false) Integer bitNumber,
        @RequestParam String value,
        Model model) {

    try {
        PlcConnector plc = new PlcConnector(ip.trim(), port);
        plc.connect();

        boolean success = false;

        switch (type.toUpperCase()) {
            case "STRING":
                success = plc.writeString(db, offset, size, value.trim());
                break;
            case "BLOCK":
                byte[] bytes = PlcConnector.hexStringToByteArray(value.trim());
                success = plc.writeBlock(db, offset, size, bytes);
                break;
            case "FLOAT":
                success = plc.writeFloat(db, offset, Float.parseFloat(value.trim()));
                break;
            case "INTEGER":
                success = plc.writeInt(db, offset, Integer.parseInt(value.trim()));
                break;
            case "BYTE":
                success = plc.writeByte(db, offset, Byte.parseByte(value.trim()));
                break;
            case "BIT":
                if (bitNumber == null) {
                    throw new IllegalArgumentException("Bit Number é obrigatório para tipo BIT");
                }
                success = plc.writeBit(db, offset, bitNumber, Boolean.parseBoolean(value.trim()));
                break;
            default:
                throw new IllegalArgumentException("Tipo não suportado: " + type);
        }

        plc.disconnect();

        if (success) {
            model.addAttribute("mensagem", "Escrita no CLP realizada com sucesso!");
        } else {
            model.addAttribute("erro", "Erro de escrita no CLP!");
        }
    } catch (Exception ex) {
        model.addAttribute("erro", "Erro: " + ex.getMessage());
    }

    return "clp-write-fragment";
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
