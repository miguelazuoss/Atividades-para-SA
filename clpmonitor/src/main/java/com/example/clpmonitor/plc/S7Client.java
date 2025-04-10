package com.example.clpmonitor.plc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class S7Client {

    private final String plcIpAddress;
    private final int port;
    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;

    public S7Client(String plcIpAddress, int port) {
        this.plcIpAddress = plcIpAddress;
        this.port = port;
    }

    public boolean connect() throws Exception {
        try {
            InetAddress address = InetAddress.getByName(plcIpAddress);
            socket = new Socket(address, port);
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
            System.out.println("Conexão estabelecida com o CLP: " + plcIpAddress + ":" + port);
            return true;
        } catch (IOException e) {
            throw new Exception("Falha ao conectar ao CLP: " + e.getMessage(), e);
        }
    }

    private byte[] createConnectionRequest() {
        return new byte[] {
                // TPKT Header
                0x03, 0x00, 0x00, 0x16,

                // ISO 873/X.224 COTP Header
                0x11, (byte) 0xE0, 0x00, 0x00, 0x00, 0x01,
                0x00, (byte) 0xC1, 0x02, 0x01, 0x00, (byte) 0xC2, 0x02, 0x01, 0x01, (byte) 0xC0, 0x01, 0x09
        };
    }

    private byte[] createSetupCommunication() {
        return new byte[] {
                // TPKT Header
                0x03, 0x00, 0x00, 0x19,

                // ISO 873/X.224 COTP Header
                0x02, (byte) 0xF0, (byte) 0x80,

                // S7 Header
                0x32, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00,
                // Parameters
                (byte) 0xF0, 0x00, 0x00, 0x01, 0x00, 0x01, (byte) 0x03, (byte) 0xC0
        };
    }

    public byte[] createReadRequest(int db, int offset, int bit, int size, String type) {

        byte tpSize = 0x00;
        int startAddress = offset;

        if ((size == 1) & (type.toLowerCase().equals("boolean"))) {

            tpSize = 0x01;
            startAddress = (offset << 3) & 0xFFF8 | (bit & 0x07);
        } else {

            tpSize = 0x02;
            startAddress = offset << 3;
        }

        return new byte[] {
                // TPKT + ISO 873/X.224 COTP Header
                0x03, 0x00, 0x00, (byte) (31), 0x02, (byte) 0xF0, (byte) 0x80,
                // S7 Header
                0x32, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0E, 0x00, 0x00,
                // Parameter: Function Code, Item Count
                0x04, 0x01,
                // Item Header: Variable Specification, Length of Following, Syntax ID
                0x12, 0x0A, 0x10,
                // Transport Size, Parameter length, DB Number (6), Area Type (DB)
                tpSize, (byte) ((size >> 8) & 0xFF), (byte) (size & 0xFF),
                (byte) ((db >> 8) & 0xFF), (byte) (db & 0xFF), (byte) 0x84,
                // Address: Bit Address, Byte Offset (16), Reserved
                0x00, (byte) ((startAddress >> 8) & 0xFF), (byte) (startAddress & 0xFF)
        };
    }

    public byte[] createWriteRequest(int db, int offset, int bit, int size, String type, Object value) {

        int lenghtTag;
        if (type.toLowerCase().equals("string")) {
            lenghtTag = size + 2;
        } else {
            lenghtTag = size;
        }

        ByteBuffer buffer = ByteBuffer.allocate(35 + lenghtTag);
        buffer.order(ByteOrder.BIG_ENDIAN);

        byte tpSize;
        int startAddress = offset;

        if ((size == 1) & (type.toLowerCase().equals("boolean"))) {
            tpSize = 0x01;
            startAddress = (offset << 3) & 0xFFF8 | (bit & 0x07);
        } else {
            tpSize = 0x02;
            startAddress = offset << 3;
        }

        int lengthPacket = 35 + lenghtTag;
        int dataLength = 4 + size;

        // TPKT Header
        buffer.put((byte) 0x03); // Version
        buffer.put((byte) 0x00); // Reserved
        buffer.putShort((short) lengthPacket); // Length

        // ISO 873/X.224 COTP Header
        buffer.put((byte) 0x02); // Length
        buffer.put((byte) 0xF0); // PDU Type: DT Data
        buffer.put((byte) 0x80); // Destination reference

        // S7 Header
        buffer.put((byte) 0x32); // Protocol ID
        buffer.put((byte) 0x01); // ROSCTR: Job (1)
        buffer.putShort((short) 0x0000); // Redundancy Indentification (Reserved): 0x0000
        buffer.putShort((short) 0x0000); // Protocol Data Unit Reference: 0
        buffer.putShort((short) 0x000E); // Parameter length: 14
        buffer.putShort((short) dataLength); // Data length

        // Parameter: Function Code, Item Count
        buffer.put((byte) 0x05); // Function: Write Var (0x05)
        buffer.put((byte) 0x01); // Item count: 1

        // Item Header: Variable Specification, Length of Following, Syntax ID
        buffer.put((byte) 0x12); // Variable specification: 0x12
        buffer.put((byte) 0x0A); // Length of Following address specification: 10
        buffer.put((byte) 0x10); // Syntax ID: S7ANY (0x10)

        // Transport Size, Parameter length, DB Number (6), Area Type (DB)
        buffer.put((byte) tpSize); // Transport Size: BIT (1) BYTE (2)
        buffer.putShort((short) size); // Length
        buffer.putShort((short) db); // DB number
        buffer.put((byte) 0x84); // Area: Data blocks (DB) (0x84)

        // Address: Bit Address, Byte Offset (16), Reserved
        buffer.put((byte) ((byte) (startAddress >> 16) & 0xFF));
        buffer.put((byte) ((byte) (startAddress >> 8) & 0xFF));
        buffer.put((byte) ((byte) startAddress & 0xFF));

        // Data
        buffer.put((byte) 0x00); // Return code: Reserved (0x00)
        buffer.put((byte) (tpSize + 2)); // Transport Size: BIT (1)+2 , BYTE (2)+2

        if (type.toLowerCase().equals("boolean")) {

            System.out.println("Aqui Boolean: " + value);

            buffer.putShort((short) lenghtTag); // Length
            buffer.put((byte) ((boolean) value ? 0x01 : 0x00));

        }

        if (type.toLowerCase().equals("integer")) {
            buffer.putShort((short) (lenghtTag << 3)); // Length
            buffer.putShort((short) ((int) value));

        }

        if (type.toLowerCase().equals("byte")) {
            buffer.putShort((short) (lenghtTag << 3)); // Length
            buffer.put((byte) ((byte) value));

        }

        if (type.toLowerCase().equals("string")) {

            buffer.putShort((short) (lenghtTag << 3)); // Length
            buffer.put((byte) size);
            buffer.put((byte) ((String) value).trim().length());
            buffer.put(((String) value).getBytes(StandardCharsets.UTF_8));

        }

        if (type.toLowerCase().equals("block")) {

            buffer.putShort((short) (lenghtTag << 3)); // Length

            buffer.put((byte[]) value);

        }

        if (type.toLowerCase().equals("float")) {

            float valFloat = (float) value;
            ByteBuffer bufferFloat = ByteBuffer.allocate(Float.BYTES); // Aloca espaço suficiente para um float
            bufferFloat.putFloat(valFloat); // Coloca o valor float no buffer
            buffer.putShort((short) (lenghtTag << 3)); // Length
            buffer.put(bufferFloat.array());

        }

        return buffer.array();
    }

    public static byte[] hexStringToByteArray(String hexString) {
        // Verifica se a string é válida
        if (hexString == null || hexString.length() % 2 != 0) {
            throw new IllegalArgumentException("A string hex deve ter um número par de caracteres.");
        }

        // Cria um array de bytes para armazenar o resultado
        byte[] byteArray = new byte[hexString.length() / 2];

        // Para cada par de caracteres hexadecimais na string, converte para um byte
        for (int i = 0; i < hexString.length(); i += 2) {
            // Pega os dois caracteres hexadecimais
            String hexPair = hexString.substring(i, i + 2);
            // Converte o par hexadecimal para um byte
            byteArray[i / 2] = (byte) Integer.parseInt(hexPair, 16);
        }

        return byteArray;
    }

    public void sendConnectionRequest() throws Exception {
        if (outputStream == null) {
            throw new Exception("Conexão não estabelecida. Chame o método connect() primeiro.");
        }

        byte[] packet = createConnectionRequest();

        try {
            outputStream.flush();
            outputStream.write(packet);
            outputStream.flush();

            // Leitura da resposta
            byte[] response = new byte[1024];
            int length = inputStream.read(response);

        } catch (Exception e) {
            throw new Exception("Erro ao enviar a solicitação de conexão: " + e.getMessage(), e);
        }
    }

    public void sendSetupCommunication() throws Exception {
        if (outputStream == null) {
            throw new Exception("Conexão não estabelecida. Chame o método connect() primeiro.");
        }

        byte[] packet = createSetupCommunication();

        try {
            outputStream.flush();
            outputStream.write(packet);
            outputStream.flush();

            // Leitura da resposta
            byte[] response = new byte[1024];
            int length = inputStream.read(response);

        } catch (Exception e) {
            throw new Exception("Erro ao enviar o pacote de configuração: " + e.getMessage(), e);
        }
    }

    private String lastReadValue;
    private Object value;

    public String getValueFromLastRead() {
        return lastReadValue;
    }

    byte[] response = new byte[1024];
    int length = 0;

    public Object sendReadRequest(int db, int offset, int bit, int size, String type) throws Exception {

        if (outputStream == null) {
            throw new Exception("Conexão não estabelecida. Chame o método connect() primeiro.");
        }

        byte[] packet = createReadRequest(db, offset, bit, size, type);

        try {

            outputStream.flush();
            outputStream.write(packet);
            outputStream.flush();

            Thread.sleep(50);
            // Leitura da resposta
            response = new byte[1024];
            length = inputStream.read(response);

            // Interpretação do valor lido
            switch (type.toLowerCase()) {
                case "string" -> value = extractStringFromResponse(response, size);
                case "block" -> value = extractBlockFromResponse(response, size);
                case "integer" -> value = extractIntegerFromResponse(response);
                case "float" -> value = extractFloatFromResponse(response);
                case "byte" -> value = extractByteFromResponse(response);
                case "boolean" -> value = extractBooleanFromResponse(response);
                default -> throw new IllegalArgumentException("Tipo de variável não suportado.");
            }

            System.out.println("Retorno de Leitura: " + value);
            return value;

        } catch (Exception e) {
            throw new Exception("Erro ao enviar o pacote de leitura: " + e.getMessage(), e);
        }
    }

    public boolean sendWriteRequest(int db, int offset, int bit, int size, String type, Object value) throws Exception {

        if (outputStream == null) {
            throw new Exception("Conexão não estabelecida. Chame o método connect() primeiro.");
        }

        System.out.println("Tamanho BOOLEAN: " + size);

        byte[] packet = createWriteRequest(db, offset, bit, size, type, value);

        try {
            outputStream.write(packet);
            outputStream.flush();

            // Leitura da resposta
            response = new byte[50];
            length = inputStream.read(response);

            return response[21] == (byte) 0xFF;

        } catch (

        IOException e) {
            throw new Exception("Erro ao enviar o pacote de leitura: " + e.getMessage(), e);
        }

    }

    // --------------------------------------------------------------------------------------------
    // Funções para extração das variáveis (conversão de BYTES para o tipo de
    // variável desejado
    // --------------------------------------------------------------------------------------------
    private int extractIntegerFromResponse(byte[] response) {

        return ByteBuffer.wrap(response, 25, 2).order(ByteOrder.BIG_ENDIAN).getShort();
    }

    private float extractFloatFromResponse(byte[] response) {
        // System.out.println("\n\nResposta de Leitura Float recebida: " +
        // bytesToHex(response, length));
        // System.out.println(response.length);
        return ByteBuffer.wrap(response, 25, 4).order(ByteOrder.BIG_ENDIAN).getFloat();
    }

    private byte extractByteFromResponse(byte[] response) {

        return ByteBuffer.wrap(response, 25, 1).order(ByteOrder.BIG_ENDIAN).get();
    }

    private byte[] extractBlockFromResponse(byte[] response, int size) {

        // int startIndex = 25;
        return Arrays.copyOfRange(response, 25, (25 + size));
    }

    private boolean extractBooleanFromResponse(byte[] response) {
        return (response[25] & 0x01) == 1;
    }

    private String extractStringFromResponse(byte[] response, int size) {

        // int startIndex = 25;
        return new String(response, 25, size).trim();
    }

    public void disconnect() {
        try {
            if (outputStream != null)
                outputStream.close();
            if (inputStream != null)
                inputStream.close();
            if (socket != null)
                socket.close();
            System.out.println("Conexão com o CLP encerrada.");
        } catch (IOException e) {
            System.err.println("Erro ao encerrar a conexão: " + e.getMessage());
        }
    }

    private String bytesToHex(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        return sb.toString().trim();
    }
}