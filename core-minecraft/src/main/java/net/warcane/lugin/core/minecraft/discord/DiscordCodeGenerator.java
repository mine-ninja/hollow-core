package net.warcane.lugin.core.minecraft.discord;

import java.security.SecureRandom;

public class DiscordCodeGenerator {

    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ123456789";
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom random = new SecureRandom();

    public static String generateCode() {
        var codeBuilder = new StringBuilder();

        for (var i = 0; i < CODE_LENGTH; i++) {
            var randomIndex = random.nextInt(CHARACTERS.length());

            codeBuilder.append(CHARACTERS.charAt(randomIndex));
        }

        codeBuilder.insert(CODE_LENGTH / 2, "-");

        return codeBuilder.toString();
    }
}
