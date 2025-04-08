package com.example.clpmonitor.util;

public class TagValueParser {

    public static Object parseValue(String value, String type) {
        try {
            return switch (type.toUpperCase()) {
                case "INTEGER" -> Integer.parseInt(value);
                case "FLOAT"   -> Float.parseFloat(value);
                case "BYTE"    -> Byte.parseByte(value);
                case "BIT"     -> Integer.parseInt(value); // ou Boolean.parseBoolean(value)
                case "STRING", "BLOCK" -> value;
                default -> throw new IllegalArgumentException("Tipo de dado não suportado: " + type);
            };
        } catch (Exception e) {
            throw new RuntimeException("Erro ao converter valor: " + value + " para o tipo: " + type, e);
        }
    }
}
