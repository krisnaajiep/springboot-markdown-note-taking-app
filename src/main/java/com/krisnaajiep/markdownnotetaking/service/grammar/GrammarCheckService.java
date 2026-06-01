package com.krisnaajiep.markdownnotetaking.service.grammar;

import com.krisnaajiep.markdownnotetaking.dto.GrammarCheckResponse;

public interface GrammarCheckService {
    GrammarCheckResponse check(String text, String language);
}
