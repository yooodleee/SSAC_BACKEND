package com.ssac.tool;

import com.ssac.ssacbackend.common.exception.ErrorCode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ErrorCode Enum과 contract/error-contract.yml 간 정합성 검증 도구.
 *
 * <p>검증 항목:
 * <ul>
 *   <li>Enum에 존재하지만 Contract에 없는 ErrorCode → 빌드 실패</li>
 *   <li>Contract에 존재하지만 Enum에 없는 ErrorCode → 빌드 실패</li>
 *   <li>Enum과 Contract 간 status / message 불일치 → 빌드 실패</li>
 * </ul>
 *
 * <p>실행: ./gradlew validateErrorContract
 */
public class ErrorContractValidator {

    private static final Path CONTRACT_PATH = Paths.get("contract", "error-contract.yml");

    public static void main(String[] args) throws IOException {
        if (!Files.exists(CONTRACT_PATH)) {
            System.out.println("[ERROR] Contract 파일을 찾을 수 없습니다: " + CONTRACT_PATH);
            System.out.println("-> contract/error-contract.yml 파일을 생성한 후 다시 실행하세요.");
            System.exit(1);
        }

        String content = Files.readString(CONTRACT_PATH, StandardCharsets.UTF_8);
        Map<String, ContractEntry> contractMap = parseContract(content);
        Map<String, EnumEntry> enumMap = loadEnumEntries();

        List<String> errors = new ArrayList<>();

        for (String code : enumMap.keySet()) {
            if (!contractMap.containsKey(code)) {
                errors.add("Enum에 존재하지만 Contract에 없음: " + code);
            }
        }

        for (String code : contractMap.keySet()) {
            if (!enumMap.containsKey(code)) {
                errors.add("Contract에 존재하지만 Enum에 없음: " + code);
            }
        }

        for (Map.Entry<String, EnumEntry> entry : enumMap.entrySet()) {
            String code = entry.getKey();
            if (!contractMap.containsKey(code)) {
                continue;
            }
            EnumEntry enumEntry = entry.getValue();
            ContractEntry contractEntry = contractMap.get(code);
            if (enumEntry.status != contractEntry.status) {
                errors.add("status 불일치 (" + code + "): Enum=" + enumEntry.status
                    + ", Contract=" + contractEntry.status);
            }
            if (!enumEntry.message.equals(contractEntry.message)) {
                errors.add("message 불일치 (" + code + "): Enum=\"" + enumEntry.message
                    + "\", Contract=\"" + contractEntry.message + "\"");
            }
        }

        if (!errors.isEmpty()) {
            System.out.println("[ERROR] ErrorCode Contract 불일치 발견");
            for (String error : errors) {
                System.out.println("- " + error);
            }
            System.out.println("-> /contract/error-contract.yml을 갱신한 후 다시 실행하세요.");
            System.exit(1);
        }

        System.out.println("[OK] ErrorCode Contract 정합성 검증 통과 (" + enumMap.size() + "개)");
    }

    private static Map<String, EnumEntry> loadEnumEntries() {
        Map<String, EnumEntry> entries = new LinkedHashMap<>();
        for (ErrorCode errorCode : ErrorCode.values()) {
            EnumEntry entry = new EnumEntry();
            entry.code = errorCode.getCode();
            entry.status = errorCode.getStatus();
            entry.message = errorCode.getMessage();
            entries.put(entry.code, entry);
        }
        return entries;
    }

    private static Map<String, ContractEntry> parseContract(String content) {
        Map<String, ContractEntry> entries = new LinkedHashMap<>();
        String[] lines = content.split("\r?\n");
        ContractEntry current = null;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("- code:")) {
                current = new ContractEntry();
                current.code = unquote(trimmed.substring("- code:".length()).trim());
                entries.put(current.code, current);
            } else if (current != null && trimmed.startsWith("status:")) {
                current.status = Integer.parseInt(trimmed.substring("status:".length()).trim());
            } else if (current != null && trimmed.startsWith("message:")) {
                current.message = unquote(trimmed.substring("message:".length()).trim());
            }
        }
        return entries;
    }

    private static String unquote(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static class EnumEntry {

        String code;
        int status;
        String message;
    }

    private static class ContractEntry {

        String code;
        int status;
        String message;
    }
}
