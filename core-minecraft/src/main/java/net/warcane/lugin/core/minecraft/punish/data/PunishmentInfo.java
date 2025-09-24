package net.warcane.lugin.core.minecraft.punish.data;

import net.warcane.lugin.core.minecraft.util.Tuple;

import java.util.ArrayList;

/**
 * @author Rok, Pedro Lucas nmm. Created on 26/06/2025
 * @project punish
 */
public record PunishmentInfo(@Deprecated  int id,
                             String modernId,
                             String title,
                             String description,
                             String mustHavePermission,
                             ArrayList<Tuple<PunishTime, PunishmentType>> punishments) {

    private static PunishmentsBuilder builder() {
        return new PunishmentsBuilder();
    }

    public static final ArrayList<PunishmentInfo> PUNISHMENTS = new ArrayList<>();

    static {
        // TODO: Think about transferring this to a database later...
        int num = 1;
        PUNISHMENTS.add(builder()
                .id(num++)
                .title("Antijogo")
                .description("Prejudicar o jogo alheio para diversão própria ou obter benefícios.")
                .addPunishment(PunishTime.THREE_DAYS, PunishmentType.TEMP)
                .addPunishment(PunishTime.ONE_WEEK, PunishmentType.TEMP)
                .addPunishment(PunishTime.HALF_MONTH, PunishmentType.TEMP)
                .addPunishment(PunishTime.ONE_MONTH, PunishmentType.TEMP)
                .addNeededPermissions("moderator")
                .build());

        PUNISHMENTS.add(builder()
                .id(num++)
                .title("Ameaça")
                .description("Ameaçar qualquer jogador para obter vantagens.")
                .addPunishment(PunishTime.THREE_DAYS, PunishmentType.MUTE)
                .addPunishment(PunishTime.ONE_WEEK, PunishmentType.TEMP)
                .addPunishment(PunishTime.HALF_MONTH, PunishmentType.TEMP)
                .addNeededPermissions("helper")
                .build());

        PUNISHMENTS.add(builder()
                .id(num++)
                .title("Discurso de Ódio")
                .description("Descrição do discurso de ódio")
                .addPunishment(PunishTime.ONE_MONTH, PunishmentType.TEMP)
                .addPunishment(PunishTime.TWO_MONTHS, PunishmentType.TEMP)
                .addPunishment(PunishTime.THREE_MONTHS, PunishmentType.TEMP)
                .addPunishment(PunishTime.PERMANENT, PunishmentType.PERM)
                .addNeededPermissions("admin")
                .build());

        PUNISHMENTS.add(builder()
                .id(num++)
                .title("Comércio")
                .description("Oferecer itens ou produtos em troca de benefícios.")
                .addPunishment(PunishTime.TWELVE_HOURS, PunishmentType.MUTE)
                .addPunishment(PunishTime.ONE_DAY, PunishmentType.MUTE)
                .addPunishment(PunishTime.THREE_DAYS, PunishmentType.TEMP)
                .addPunishment(PunishTime.ONE_WEEK, PunishmentType.TEMP)
                .addNeededPermissions("helper")
                .build());

        PUNISHMENTS.add(builder()
                .id(num++)
                .title("Desinformação")
                .description("Disseminar informações falsas acerca da equipe, do servidor ou de situações da vida real.")
                .addPunishment(PunishTime.ONE_DAY, PunishmentType.MUTE)
                .addPunishment(PunishTime.THREE_DAYS, PunishmentType.TEMP)
                .addPunishment(PunishTime.ONE_WEEK, PunishmentType.TEMP)
                .addPunishment(PunishTime.HALF_MONTH, PunishmentType.TEMP)
                .addNeededPermissions("moderator")
                .build());

        PUNISHMENTS.add(builder()
                .id(num++)
                .title("Divulgação grave")
                .description("Divulgar qualquer link ou informação que não tenha relação com o servido")
                .addPunishment(PunishTime.ONE_WEEK, PunishmentType.MUTE)
                .addPunishment(PunishTime.HALF_MONTH, PunishmentType.TEMP)
                .addPunishment(PunishTime.ONE_MONTH, PunishmentType.TEMP)
                .addPunishment(PunishTime.PERMANENT, PunishmentType.PERM)
                .addNeededPermissions("moderator")
                .build());

        PUNISHMENTS.add(builder()
                .id(num++)
                .title("Divulgação simples")
                .description("Divulgar qualquer link ou informação que não tenha relação com o servido")
                .addPunishment(PunishTime.TWELVE_HOURS, PunishmentType.MUTE)
                .addPunishment(PunishTime.ONE_DAY, PunishmentType.MUTE)
                .addPunishment(PunishTime.THREE_DAYS, PunishmentType.TEMP)
                .addPunishment(PunishTime.ONE_WEEK, PunishmentType.TEMP)
                .addNeededPermissions("helper")
                .build());

        PUNISHMENTS.add(builder()
                .id(num++)
                .title("Estorno de Pagamento")
                .description("Solicitar reembolso de uma compra após ela já ter sido aprovada e ativada.")
                .addPunishment(PunishTime.PERMANENT, PunishmentType.PERM)
                .addNeededPermissions("gerente")
                .build());

        PUNISHMENTS.add(builder()
                .id(num++)
                .title("Falsificação de provas")
                .description("Falsificar provas de denúncias para prejudicar outros jogadores.")
                .addPunishment(PunishTime.PERMANENT, PunishmentType.PERM)
                .addNeededPermissions("admin")
                .build());

/*        PUNISHMENTS.add(builder()
                .id(num++)
                .title("Linguagem inadequada")
                .description("Descrição da linguagem inadequada")
                .addPunishment(PunishTime.TWO_HOURS, PunishmentType.MUTE)
                .addPunishment(PunishTime.SIX_HOURS, PunishmentType.MUTE)
                .addPunishment(PunishTime.ONE_DAY, PunishmentType.MUTE)
                .addPunishment(PunishTime.THREE_DAYS, PunishmentType.TEMP)
                .addNeededPermissions("helper")
                .build());*/

        PUNISHMENTS.add(builder()
                .id(num++)
                .title("Assédio")
                .description("Realizar abordagens invasivas, incômodas ou ofensivas a outros jogadores.")
                .addPunishment(PunishTime.THREE_MONTHS, PunishmentType.TEMP)
                .addPunishment(PunishTime.HALF_YEAR, PunishmentType.TEMP)
                .addPunishment(PunishTime.PERMANENT, PunishmentType.PERM)
                .addNeededPermissions("admin")
                .build());

        PUNISHMENTS.add(builder()
                .id(num++)
                .title("Spam/Flood")
                .description("Envio de mensagens repetidas a fim de pouluir o chat.")
                .addPunishment(PunishTime.SIX_HOURS, PunishmentType.MUTE)
                .addPunishment(PunishTime.TWELVE_HOURS, PunishmentType.MUTE)
                .addPunishment(PunishTime.ONE_DAY, PunishmentType.TEMP)
                .addPunishment(PunishTime.THREE_DAYS, PunishmentType.TEMP)
                .addNeededPermissions("helper")
                .build());

        PUNISHMENTS.add(builder()
                .id(num++)
                .title("Uso de trapaças")
                .description("Recorrer a clientes externos visando obter vantagens.")
                .addPunishment(PunishTime.HALF_YEAR, PunishmentType.TEMP)
                .addPunishment(PunishTime.PERMANENT, PunishmentType.TEMP)
                .addNeededPermissions("moderator")
                .build());

        PUNISHMENTS.add(builder()
                .id(num++)
                .title("Nickname inapropriado (original)")
                .description("Utilizar um nickname que inclua nome de figuras explícitas, assassinos, ditadores, etc.")
                .addPunishment(PunishTime.ONE_MONTH, PunishmentType.TEMP)
                .addNeededPermissions("moderator")
                .build());

        PUNISHMENTS.add(builder()
                .id(num++)
                .title("Nickname inapropriado (pirata)")
                .description("Utilizar um nickname que inclua nome de figuras explícitas, assassinos, ditadores, etc.")
                .addPunishment(PunishTime.PERMANENT, PunishmentType.PERM)
                .addNeededPermissions("moderator")
                .build());

        PUNISHMENTS.add(builder()
                .id(num++)
                .title("Ofensa a jogador")
                .description("Ofender um jogador específico de qualquer maneira.")
                .addPunishment(PunishTime.TWELVE_HOURS, PunishmentType.MUTE)
                .addPunishment(PunishTime.ONE_DAY, PunishmentType.TEMP)
                .addPunishment(PunishTime.ONE_WEEK, PunishmentType.TEMP)
                .addPunishment(PunishTime.HALF_MONTH, PunishmentType.TEMP)
                .addNeededPermissions("helper")
                .build());

        PUNISHMENTS.add(builder()
                .id(num++)
                .title("Ofensa à equipe/servidor")
                .description("Ofender diretamente o servidor ou um membro da equipe.")
                .addPunishment(PunishTime.ONE_WEEK, PunishmentType.TEMP)
                .addPunishment(PunishTime.HALF_MONTH, PunishmentType.TEMP)
                .addPunishment(PunishTime.ONE_MONTH, PunishmentType.TEMP)
                .addPunishment(PunishTime.PERMANENT, PunishmentType.PERM)
                .addNeededPermissions("moderator")
                .build());

        PUNISHMENTS.add(builder()
                .id(num++)
                .title("Skin inapropriada")
                .description("Utilizar skins que retratem assassinos, ditadores e símbolos históricos\n" +
                             "associados a atos negativos. Válido também para nudez explícita.")
                .addPunishment(PunishTime.ONE_DAY, PunishmentType.TEMP)
                .addPunishment(PunishTime.ONE_WEEK, PunishmentType.TEMP)
                .addPunishment(PunishTime.HALF_MONTH, PunishmentType.TEMP)
                .addPunishment(PunishTime.PERMANENT, PunishmentType.PERM)
                .addNeededPermissions("moderator")
                .build());

        PUNISHMENTS.add(builder()
                .id(num++)
                .title("Formação de times")
                .description("Formar alianças com outros jogadores a fim de prejudicar o jogo alheio.")
                .addPunishment(PunishTime.ONE_DAY, PunishmentType.TEMP)
                .addPunishment(PunishTime.THREE_DAYS, PunishmentType.TEMP)
                .addPunishment(PunishTime.ONE_WEEK, PunishmentType.TEMP)
                .addNeededPermissions("moderator")
                .build());
        PUNISHMENTS.add(builder()
                .id(num++)
                .title("Abuso de Bugs")
                .description("Abusar de erros ou bugs para obter vantagens.")
                .addPunishment(PunishTime.HALF_MONTH, PunishmentType.TEMP)
                .addPunishment(PunishTime.PERMANENT, PunishmentType.PERM)
                .addNeededPermissions("admin")
                .build());
        PUNISHMENTS.add(builder()
                .id(num++)
                .title("Conta fake")
                .description("Recorrer a uma conta fake para violar as regras.\n" +
                             "\n" +
                             "* Essa punição deverá ser aplicada na conta fake.")
                .addPunishment(PunishTime.PERMANENT, PunishmentType.PERM)
                .addNeededPermissions("admin")
                .build());

        PUNISHMENTS.add(builder()
                .id(num++)
                .title("Conta fake para atos ilícitos")
                .description("Recorrer a uma conta fake para violar as regras.\n" +
                             "\n" +
                             "* Essa punição deverá ser aplicada na conta principal.")
                .addPunishment(PunishTime.HALF_MONTH, PunishmentType.TEMP)
                .addPunishment(PunishTime.PERMANENT, PunishmentType.PERM)
                .addNeededPermissions("admin")
                .build());

        PUNISHMENTS.add(PunishmentInfo.builder()
                .id(num++)
                .title("Teste")
                .description("Descrição do teste")
                .addPunishment(PunishTime.ONE_HOUR, PunishmentType.MUTE)
                .addPunishment(PunishTime.ONE_HOUR, PunishmentType.TEMP)
                .addNeededPermissions("admin")
                .build());
    }

    public static PunishmentInfo getPunishmentById(int id) {
        if (id < 1 || id > PUNISHMENTS.size()) {
            throw new IllegalArgumentException("Id invalido");
        }
        return PUNISHMENTS.get(id - 1);
    }

    public Tuple<PunishTime, PunishmentType> getPunishment(int index) {
        return punishments.get(Math.min(index, punishments.size() - 1));
    }

    public static class PunishmentsBuilder {

        private int id;
        private String modernId;
        private String title;
        private String description;
        private String mustHavePermission;
        private final ArrayList<Tuple<PunishTime, PunishmentType>> punishments = new ArrayList<>();

        public PunishmentsBuilder title(String title) {
            this.title = title;
            return this;
        }

        @Deprecated(forRemoval = true)
        public PunishmentsBuilder id(int id) {
            this.id = id;
            return this;
        }

        public PunishmentsBuilder description(String description) {
            this.description = description;
            return this;
        }

        public PunishmentsBuilder modernId(String modernId) {
            this.modernId = modernId;
            return this;
        }

        public PunishmentsBuilder addPunishment(PunishTime time, PunishmentType type) {
            this.punishments.add(new Tuple<>(time, type));
            return this;
        }

        public PunishmentsBuilder addNeededPermissions(String permission) {
            this.mustHavePermission = "lugin." + permission;
            return this;
        }

        public PunishmentInfo build() {
            return new PunishmentInfo(id, modernId == null ? "punishment_" + id : modernId, title, description, mustHavePermission, punishments);
        }
    }
}
