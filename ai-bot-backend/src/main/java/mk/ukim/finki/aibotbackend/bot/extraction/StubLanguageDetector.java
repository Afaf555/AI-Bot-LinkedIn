package mk.ukim.finki.aibotbackend.bot.extraction;

import org.springframework.stereotype.Component;

/**
 * Heuristic Macedonian-language detector based on script and letter frequency.
 *
 * <p>Macedonian is written exclusively in Cyrillic and uses the letters
 * ѓ, ќ, ѕ, џ, љ, њ. Crucially, it disqualifies text containing letters that
 * are exclusive to neighbouring Cyrillic languages and never appear in
 * standard Macedonian orthography:</p>
 * <ul>
 *   <li>ъ, щ, ю, я — Bulgarian</li>
 *   <li>ы, э, ё, ъ — Russian</li>
 *   <li>ђ, ћ — Serbian (uses ђ/ћ where Macedonian uses ѓ/ќ)</li>
 * </ul>
 */
@Component
public class StubLanguageDetector implements LanguageDetector {

    private static final String MACEDONIAN_SPECIFIC_LETTERS = "ѓќѕџљњ";
    private static final String MACEDONIAN_EXCLUSIVE_LETTERS = "ѓќ";
    private static final String DISQUALIFYING_LETTERS = "ъщюяыэёђћ";

    @Override
    public double macedonianConfidence(String text) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }

        int letterCount = 0;
        int cyrillicCount = 0;
        int macedonianSpecificCount = 0;
        int macedonianExclusiveCount = 0;
        int disqualifyingCount = 0;

        for (char character : text.toCharArray()) {
            if (!Character.isLetter(character)) {
                continue;
            }
            letterCount++;

            char lower = Character.toLowerCase(character);
            if (isCyrillic(lower)) {
                cyrillicCount++;
            }
            if (MACEDONIAN_SPECIFIC_LETTERS.indexOf(lower) >= 0) {
                macedonianSpecificCount++;
            }
            if (MACEDONIAN_EXCLUSIVE_LETTERS.indexOf(lower) >= 0) {
                macedonianExclusiveCount++;
            }
            if (DISQUALIFYING_LETTERS.indexOf(lower) >= 0) {
                disqualifyingCount++;
            }
        }

        if (letterCount == 0) {
            return 0.0;
        }

        double cyrillicRatio = (double) cyrillicCount / letterCount;

       
        double disqualifyingRatio = (double) disqualifyingCount / letterCount;
        if (disqualifyingRatio > 0.01) {
            return 0.0;
        }

        if (cyrillicRatio < 0.5) {
            return 0.0;
        }


        double exclusiveBoost = Math.min(macedonianExclusiveCount * 0.25, 0.5);
        double sharedBoost = Math.min(macedonianSpecificCount * 0.08, 0.25);

        double confidence = (cyrillicRatio * 0.4) + exclusiveBoost + sharedBoost;
        return Math.min(confidence, 1.0);
    }

    private boolean isCyrillic(char character) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(character);
        return block == Character.UnicodeBlock.CYRILLIC
                || block == Character.UnicodeBlock.CYRILLIC_SUPPLEMENTARY;
    }
}