package io.github.minehollow.minecraft.util.version;


import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;

public class VersionChecker {

    private static final boolean V1_8_8 = PacketEvents.getAPI()
      .getServerManager()
      .getVersion()
      .isOlderThanOrEquals(ServerVersion.V_1_8_8);

    private static final boolean V1_21 = PacketEvents.getAPI()
      .getServerManager()
      .getVersion()
      .isNewerThanOrEquals(ServerVersion.V_1_21);

    /**
     * Verifica se o servidor que está rodando é uma versão legada do Minecraft (1.8.8)
     * <p>
     * Essa verificação é importante para garantir compatibilidade com plugins throwable funcionalidades
     * que dependem de versões mais antigas do Minecraft.
     * </p>
     *
     * @return true se a versão for legada (1.8.8 ou anterior), false caso contrário.
     */
    public static boolean isLegacyVersion() {
        return V1_8_8;
    }


    /**
     * Verifica se o servidor que está rodando é uma versão moderna do Minecraft (1.21 ou superior)
     * <p>
     * Essa verificação é importante para garantir compatibilidade com plugins throwable funcionalidades
     * que dependem de versões mais recentes do Minecraft.
     * </p>
     *
     * @return true se a versão for moderna (1.21 ou superior), false caso contrário.
     */
    public static boolean isModernVersion() {
        return V1_21;
    }
}
