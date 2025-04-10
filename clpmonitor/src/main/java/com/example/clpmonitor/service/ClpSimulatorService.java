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
import com.example.clpmonitor.plc.PlcConnector;

import jakarta.annotation.PostConstruct;

// Define que esta classe é um componente de serviço do Spring (fica disponível para injeção com @Autowired).
// Contém a lógica de negócio: neste caso, a simulação de dados dos CLPs e envio via SSE.
@Service
public class ClpSimulatorService {

    public static byte[] indexColorEst = new byte[28];
    public Integer indexExpedition;

    public PlcConnector plcEstDb9;
    public PlcConnector plcExpDb9;

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
        sendClp1Update();
        sendClp4Update();
        executor.scheduleAtFixedRate(this::sendClp1Update, 0, 10000, TimeUnit.MILLISECONDS);

        // Agendamento para CLPs 2 a 4 (1 segundo)
        executor.scheduleAtFixedRate(this::sendClp2to4Updates, 0, 3, TimeUnit.SECONDS);

        executor.scheduleAtFixedRate(this::sendClp4Update, 0, 10000, TimeUnit.MILLISECONDS);
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

        plcEstDb9 = new PlcConnector("10.74.241.10", 102);
        try {
            plcEstDb9.connect();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            indexColorEst = plcEstDb9.readBlock(9, 68, 28);
        } catch (Exception e1) {
            e1.printStackTrace();
            System.out.println("Falha");

        }

        List<Integer> byteArray = new ArrayList<>();
        for (int i = 0; i < 28; i++) {
            int color = (int) indexColorEst[i];
            byteArray.add(color); // 0 a 3
        }

        // Cria um ClpData com id = 1 e envia com o evento "clp1-data".
        ClpData clp1 = new ClpData(1, byteArray);
        sendToEmitters("clp1-data", clp1);
    }

    private void sendClp4Update() {

        int values[] = new int[12];

        plcExpDb9 = new PlcConnector("10.74.241.40", 102);
        try {
            plcExpDb9.connect();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            int j = 0;
            for (int i = 6; i <= 28; i += 2) {
                values[j] = plcExpDb9.readInt(9, i);
                j++;
            }
        } catch (Exception e1) {
            e1.printStackTrace();
            System.out.println("Falha");
        }
        // Gera uma lista de 28 inteiros entre 0 e 3.
        // Gera uma lista de 28 inteiros entre 0 e 3.;
        List<Integer> byteArray = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            byteArray.add(values[i]); // 0 a 3
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
