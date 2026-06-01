package com.krisnaajiep.markdownnotetaking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GrammarCheckResponse {
    private String software;
    private String language;
    private List<Result> results;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Result {
        private String message;
        private List<String> suggestions;
        private int offset;
        private int length;
        private Map<String, Object> context;
    }
}
