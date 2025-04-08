package com.example.clpmonitor.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.clpmonitor.model.ClpData;

import jakarta.annotation.PostConstruct;

// Define que esta classe é um componente de serviço do Spring (fica disponível para injeção com @Autowired).
// Contém a lógica de negócio: neste caso, a simulação de dados dos CLPs e envio via SSE.
@Service
public class ClpSimulatorService {

    // emitters – Lista de clientes conectados via SSE
    // Guarda todos os clientes que estão conectados e escutando eventos via SSE.
    // CopyOnWriteArrayList é usada para permitir acesso concorrente com
    // segurança (vários threads atualizando a lista).
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    // executor – Agendamento das tarefas de simulações
    // Cria uma pool de threads agendadas (com 2 threads).
    // É usada para executar tarefas repetidamente com um intervalo fixo (ex: a cada 1 segundo).
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

    // @PostConstruct – Inicialização automática
    @PostConstruct
    // Esse método é chamado automaticamente após a construção do bean.
    // Define os dois agendamentos de envio de dados simulados:
    public void startSimulation() {
        // Agendamento separado para CLP 1 (800ms)
        executor.scheduleAtFixedRate(this::sendClp1Update, 0, 3800, TimeUnit.MILLISECONDS);

        // Agendamento para CLPs 2 a 4 (1 segundo)
        executor.scheduleAtFixedRate(this::sendClp2to4Updates, 0, 3, TimeUnit.SECONDS);

        executor.scheduleAtFixedRate(this::sendClp4Update, 0, 3800, TimeUnit.MILLISECONDS);
    }

    // subscribe() – Adiciona cliente à lista de ouvintes SSE
    // Esse método é chamado quando o frontend conecta-se à URL /clp-data-stream.
    public SseEmitter subscribe() {
        // Cria um novo SseEmitter com timeout infinito (0L).
        SseEmitter emitter = new SseEmitter(0L);

        // Adiciona esse emitter à lista emitters.
        emitters.add(emitter);

        // Remove o cliente se ele desconectar ou der timeout.
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));

        return emitter;
    }

    // sendClp1Update() – Gera 28 bytes (valores de 0 a 3) para o CLP 1
    private void sendClp1Update() {
        // Gera uma lista de 28 inteiros entre 0 e 3.
        // Gera uma lista de 28 inteiros entre 0 e 3.
        Random rand = new Random();
        List<Integer> byteArray = new ArrayList<>();
        for (int i = 0; i < 28; i++) {
            byteArray.add(rand.nextInt(4)); // 0 a 3
        }

        // Cria um ClpData com id = 1 e envia com o evento "clp1-data".
        ClpData clp1 = new ClpData(1, byteArray);
        sendToEmitters("clp1-data", clp1);
    }

        private void sendClp4Update() {
        // Gera uma lista de 28 inteiros entre 0 e 3.
        // Gera uma lista de 28 inteiros entre 0 e 3.
        Random rand = new Random();
        List<Integer> byteArray = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            byteArray.add(rand.nextInt(201)); // 0 a 3
        }

        // Cria um ClpData com id = 1 e envia com o evento "clp1-data".
        ClpData clp4 = new ClpData(4, byteArray);
        sendToEmitters("clp4-data", clp4);
    }

    // sendClp2to4Updates() – Gera valores inteiros simples
    // Simula os valores para os CLPs 2, 3 e 4 com números aleatórios de 0 a 99.
    private void sendClp2to4Updates() {
        Random rand = new Random();

        sendToEmitters("clp2-data", new ClpData(2, rand.nextInt(100)));
        sendToEmitters("clp3-data", new ClpData(3, rand.nextInt(100)));
    }

    // sendToEmitters() – Envia um evento SSE para todos os clientes
    private void sendToEmitters(String eventName, ClpData clpData) {
        // Percorre todos os SseEmitters conectados.
        for (SseEmitter emitter : emitters) {
            try {
                // Envia um evento com:
                //      eventName → nome do evento no frontend (ex: clp1-data, clp2-data, etc).
                //      data(clpData) → dados a serem enviados (convertidos para JSON automaticamente).
                emitter.send(SseEmitter.event().name(eventName).data(clpData));
            } catch (IOException e) {
                // Se algum cliente tiver erro de conexão, ele é removido da lista.
                emitters.remove(emitter);
            }
        }
    }

}