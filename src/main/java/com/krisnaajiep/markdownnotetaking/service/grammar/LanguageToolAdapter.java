package com.krisnaajiep.markdownnotetaking.service.grammar;

import com.krisnaajiep.markdownnotetaking.dto.GrammarCheckResponse;
import com.krisnaajiep.markdownnotetaking.dto.LanguageToolCheckResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LanguageToolAdapter implements GrammarCheckService {
    private final LanguageToolService languageToolService;

    @Override
    public GrammarCheckResponse check(String text, String language) {
        LanguageToolCheckResponse languageToolCheckResponse = languageToolService.check(text, language);

        return GrammarCheckResponse.builder()
                .software(languageToolCheckResponse.getSoftware().getName())
                .language(languageToolCheckResponse.getLanguage().getName())
                .results(languageToolCheckResponse.getMatches().stream()
                        .map(match -> GrammarCheckResponse.Result.builder()
                                .message(match.getMessage())
                                .suggestions(match.getReplacements().stream()
                                        .map(replacement -> replacement.get("value").toString())
                                        .toList())
                                .offset(match.getOffset())
                                .length(match.getLength())
                                .context(match.getContext())
                                .build())
                        .toList())
                .build();
    }
}
