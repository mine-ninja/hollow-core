package net.warcane.lugin.core.minecraft.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;

/**
 * Classe para gerenciar arquivos de configuração customizados
 */
public class ConfigFile extends YamlConfiguration {

    private final Plugin plugin;
    private final File file;
    private final String fileName;

    private ConfigFile(Plugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.file = new File(plugin.getDataFolder(), fileName);
    }

    /**
     * Cria ou carrega um arquivo de configuração
     *
     * @param plugin   Instância do plugin
     * @param fileName Nome do arquivo (ex: "missions.yml")
     * @return Instância do ConfigFile
     */
    public static ConfigFile create(Plugin plugin, String fileName) {
        ConfigFile config = new ConfigFile(plugin, fileName);
        config.load();
        return config;
    }

    /**
     * Carrega o arquivo de configuração do disco
     */
    private void load() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            if (!file.exists()) {
                copyDefault();
            }

            super.load(file);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE,
              "Erro ao carregar o arquivo " + fileName, e);
        }
    }

    /**
     * Copia o arquivo padrão do resources se existir
     */
    private void copyDefault() {
        try (InputStream input = plugin.getResource(fileName)) {
            if (input != null) {
                Files.copy(input, file.toPath());
                plugin.getLogger().info("Arquivo " + fileName + " criado com valores padrão");
            } else {
                file.createNewFile();
                plugin.getLogger().info("Arquivo " + fileName + " criado (vazio)");
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE,
              "Erro ao criar o arquivo " + fileName, e);
        }
    }

    /**
     * Salva as alterações no arquivo
     *
     * @return true se salvou com sucesso
     */
    public boolean save() {
        try {
            super.save(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE,
              "Erro ao salvar o arquivo " + fileName, e);
            return false;
        }
    }

    /**
     * Recarrega o arquivo do disco
     */
    public void reload() {
        load();
    }

    /**
     * Obtém o arquivo físico
     */
    public File getFile() {
        return file;
    }

    /**
     * Obtém o nome do arquivo
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Deleta o arquivo do disco
     */
    public boolean delete() {
        return file.exists() && file.delete();
    }

    /**
     * Verifica se o arquivo existe
     */
    public boolean exists() {
        return file.exists();
    }
}
