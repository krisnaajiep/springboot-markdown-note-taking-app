package com.krisnaajiep.markdownnotetaking.service;

import com.krisnaajiep.markdownnotetaking.dto.GrammarCheckResponse;

public interface GrammarCheckService {
    GrammarCheckResponse check(String text, String language);
}
