package net.warcane.lugin.core.punish.data;

import net.warcane.lugin.core.util.Tuple;

import java.util.ArrayList;

/**
 * @author Rok, Pedro Lucas nmm. Created on 26/06/2025
 */
public record PunishmentInfo(@Deprecated int id,
                             String modernId,
                             String title,
                             String description,
                             String mustHavePermission,
                             boolean reportable,
                             ArrayList<Tuple<PunishTime, PunishmentType>> punishments) {

    private static PunishmentsBuilder builder() {
        return new PunishmentsBuilder();
    }

    public static final ArrayList<PunishmentInfo> PUNISHMENTS = new ArrayList<>();

    static {
        // TODO: Think about transferring this to a database later...
        PUNISHMENTS.add(builder()
            .id(1)
            .modernId("antijogo")
            .title("Antijogo")
            .description("Prejudicar o jogo alheio para diversão própria ou obter benefícios.")
            .addPunishment(PunishTime.THREE_DAYS, PunishmentType.TEMP)
            .addPunishment(PunishTime.ONE_WEEK, PunishmentType.TEMP)
            .addPunishment(PunishTime.HALF_MONTH, PunishmentType.TEMP)
            .addPunishment(PunishTime.ONE_MONTH, PunishmentType.TEMP)
            .addNeededPermissions("moderator")
            .reportable()
            .build());

        PUNISHMENTS.add(builder()
            .id(2)
            .title("Ameaça")
            .modernId("ameaca")
            .description("Ameaçar qualquer jogador para obter vantagens.")
            .addPunishment(PunishTime.THREE_DAYS, PunishmentType.MUTE)
            .addPunishment(PunishTime.ONE_WEEK, PunishmentType.TEMP)
            .addPunishment(PunishTime.HALF_MONTH, PunishmentType.TEMP)
            .addNeededPermissions("helper")
            .reportable()
            .build());

        PUNISHMENTS.add(builder()
            .id(3)
            .title("Discurso de Ódio")
            .modernId("discurso_de_odio")
            .addPunishment(PunishTime.ONE_MONTH, PunishmentType.TEMP)
            .addPunishment(PunishTime.TWO_MONTHS, PunishmentType.TEMP)
            .addPunishment(PunishTime.THREE_MONTHS, PunishmentType.TEMP)
            .addPunishment(PunishTime.PERMANENT, PunishmentType.PERM)
            .addNeededPermissions("admin")
            .build());

        PUNISHMENTS.add(builder()
            .id(4)
            .title("Comércio")
            .modernId("comercio")
            .description("Oferecer itens ou produtos em troca de benefícios.")
            .addPunishment(PunishTime.TWELVE_HOURS, PunishmentType.MUTE)
            .addPunishment(PunishTime.ONE_DAY, PunishmentType.MUTE)
            .addPunishment(PunishTime.THREE_DAYS, PunishmentType.TEMP)
            .addPunishment(PunishTime.ONE_WEEK, PunishmentType.TEMP)
            .addNeededPermissions("helper")
            .build());

        PUNISHMENTS.add(builder()
            .id(5)
            .title("Desinformação")
            .modernId("desinformacao")
            .description("Disseminar informações falsas acerca da equipe, do servidor ou de situações da vida real.")
            .addPunishment(PunishTime.ONE_DAY, PunishmentType.MUTE)
            .addPunishment(PunishTime.THREE_DAYS, PunishmentType.TEMP)
            .addPunishment(PunishTime.ONE_WEEK, PunishmentType.TEMP)
            .addPunishment(PunishTime.HALF_MONTH, PunishmentType.TEMP)
            .addNeededPermissions("moderator")
            .build());

        PUNISHMENTS.add(builder()
            .id(6)
            .title("Divulgação grave")
            .modernId("divulgacao_grave")
            .description("Divulgar qualquer link ou informação que não tenha relação com o servido")
            .addPunishment(PunishTime.ONE_WEEK, PunishmentType.MUTE)
            .addPunishment(PunishTime.HALF_MONTH, PunishmentType.TEMP)
            .addPunishment(PunishTime.ONE_MONTH, PunishmentType.TEMP)
            .addPunishment(PunishTime.PERMANENT, PunishmentType.PERM)
            .addNeededPermissions("moderator")
            .build());

        PUNISHMENTS.add(builder()
            .id(7)
            .title("Divulgação simples")
            .modernId("divulgacao_simples")
            .description("Divulgar qualquer link ou informação que não tenha relação com o servido")
            .addPunishment(PunishTime.TWELVE_HOURS, PunishmentType.MUTE)
            .addPunishment(PunishTime.ONE_DAY, PunishmentType.MUTE)
            .addPunishment(PunishTime.THREE_DAYS, PunishmentType.TEMP)
            .addPunishment(PunishTime.ONE_WEEK, PunishmentType.TEMP)
            .addNeededPermissions("helper")
            .build());

        PUNISHMENTS.add(builder()
            .id(8)
            .title("Estorno de Pagamento")
            .modernId("estorno_de_pagamento")
            .description("Solicitar reembolso de uma compra após ela já ter sido aprovada e ativada.")
            .addPunishment(PunishTime.PERMANENT, PunishmentType.PERM)
            .addNeededPermissions("gerente")
            .build());

        PUNISHMENTS.add(builder()
            .id(9)
            .title("Falsificação de provas")
            .modernId("falsificacao_de_provas")
            .description("Falsificar provas de denúncias para prejudicar outros jogadores.")
            .addPunishment(PunishTime.PERMANENT, PunishmentType.PERM)
            .addNeededPermissions("admin")
            .build());

        PUNISHMENTS.add(builder()
            .id(10)
            .title("Assédio")
            .modernId("assedio")
            .description("Realizar abordagens invasivas, incômodas ou ofensivas a outros jogadores.")
            .addPunishment(PunishTime.THREE_MONTHS, PunishmentType.TEMP)
            .addPunishment(PunishTime.HALF_YEAR, PunishmentType.TEMP)
            .addPunishment(PunishTime.PERMANENT, PunishmentType.PERM)
            .addNeededPermissions("admin")
            .reportable()
            .build());

        PUNISHMENTS.add(builder()
            .id(11)
            .title("Spam/Flood")
            .modernId("spam_flood")
            .description("Envio de mensagens repetidas a fim de pouluir o chat.")
            .addPunishment(PunishTime.SIX_HOURS, PunishmentType.MUTE)
            .addPunishment(PunishTime.TWELVE_HOURS, PunishmentType.MUTE)
            .addPunishment(PunishTime.ONE_DAY, PunishmentType.TEMP)
            .addPunishment(PunishTime.THREE_DAYS, PunishmentType.TEMP)
            .addNeededPermissions("helper")
            .reportable()
            .build());

        PUNISHMENTS.add(builder()
            .id(12)
            .title("Uso de trapaças")
            .modernId("uso_de_trapacas")
            .description("Recorrer a clientes externos visando obter vantagens.")
            .addPunishment(PunishTime.HALF_YEAR, PunishmentType.TEMP)
            .addPunishment(PunishTime.PERMANENT, PunishmentType.TEMP)
            .addNeededPermissions("moderator")
            .reportable()
            .build());

        PUNISHMENTS.add(builder()
            .id(13)
            .title("Nickname inapropriado (original)")
            .modernId("nickname_inapropriado_original")
            .description("Utilizar um nickname que inclua nome de figuras explícitas, assassinos, ditadores, etc.")
            .addPunishment(PunishTime.ONE_MONTH, PunishmentType.TEMP)
            .addNeededPermissions("moderator")
            .build());

        PUNISHMENTS.add(builder()
            .id(14)
            .title("Nickname inapropriado (pirata)")
            .modernId("nickname_inapropriado_pirata")
            .description("Utilizar um nickname que inclua nome de figuras explícitas, assassinos, ditadores, etc.")
            .addPunishment(PunishTime.PERMANENT, PunishmentType.PERM)
            .addNeededPermissions("moderator")
            .build());

        PUNISHMENTS.add(builder()
            .id(15)
            .title("Ofensa a jogador")
            .modernId("ofensa_a_jogador")
            .description("Ofender um jogador específico de qualquer maneira.")
            .addPunishment(PunishTime.TWELVE_HOURS, PunishmentType.MUTE)
            .addPunishment(PunishTime.ONE_DAY, PunishmentType.TEMP)
            .addPunishment(PunishTime.ONE_WEEK, PunishmentType.TEMP)
            .addPunishment(PunishTime.HALF_MONTH, PunishmentType.TEMP)
            .addNeededPermissions("helper")
            .reportable()
            .build());

        PUNISHMENTS.add(builder()
            .id(16)
            .title("Ofensa à equipe/servidor")
            .modernId("ofensa_a_equipe_servidor")
            .description("Ofender diretamente o servidor ou um membro da equipe.")
            .addPunishment(PunishTime.ONE_WEEK, PunishmentType.TEMP)
            .addPunishment(PunishTime.HALF_MONTH, PunishmentType.TEMP)
            .addPunishment(PunishTime.ONE_MONTH, PunishmentType.TEMP)
            .addPunishment(PunishTime.PERMANENT, PunishmentType.PERM)
            .addNeededPermissions("moderator")
            .build());

        PUNISHMENTS.add(builder()
            .id(17)
            .title("Skin inapropriada")
            .modernId("skin_inapropriada")
            .description("Utilizar skins que retratem assassinos, ditadores e símbolos históricos\n" +
                "associados a atos negativos. Válido também para nudez explícita.")
            .addPunishment(PunishTime.ONE_DAY, PunishmentType.TEMP)
            .addPunishment(PunishTime.ONE_WEEK, PunishmentType.TEMP)
            .addPunishment(PunishTime.HALF_MONTH, PunishmentType.TEMP)
            .addPunishment(PunishTime.PERMANENT, PunishmentType.PERM)
            .addNeededPermissions("moderator")
            .reportable()
            .build());

        PUNISHMENTS.add(builder()
            .id(18)
            .title("Formação de times")
            .modernId("formacao_de_times")
            .description("Formar alianças com outros jogadores a fim de prejudicar o jogo alheio.")
            .addPunishment(PunishTime.ONE_DAY, PunishmentType.TEMP)
            .addPunishment(PunishTime.THREE_DAYS, PunishmentType.TEMP)
            .addPunishment(PunishTime.ONE_WEEK, PunishmentType.TEMP)
            .addNeededPermissions("moderator")
            .reportable()
            .build());
        PUNISHMENTS.add(builder()
            .id(19)
            .title("Abuso de Bugs")
            .modernId("abuso_de_bugs")
            .description("Abusar de erros ou bugs para obter vantagens.")
            .addPunishment(PunishTime.HALF_MONTH, PunishmentType.TEMP)
            .addPunishment(PunishTime.PERMANENT, PunishmentType.PERM)
            .addNeededPermissions("admin")
            .reportable()
            .build());
        PUNISHMENTS.add(builder()
            .id(20)
            .title("Conta fake")
            .modernId("conta_fake")
            .description("Recorrer a uma conta fake para violar as regras.\n" +
                "\n" +
                "Essa punição deverá ser aplicada na conta fake.")
            .addPunishment(PunishTime.PERMANENT, PunishmentType.PERM)
            .addNeededPermissions("admin")
            .build());

        PUNISHMENTS.add(builder()
            .id(21)
            .title("Conta fake para atos ilícitos")
            .modernId("conta_fake_para_atos_ilicitos")
            .description("Recorrer a uma conta fake para violar as regras.\n" +
                "\n" +
                "Essa punição deverá ser aplicada na conta principal.")
            .addPunishment(PunishTime.HALF_MONTH, PunishmentType.TEMP)
            .addPunishment(PunishTime.PERMANENT, PunishmentType.PERM)
            .addNeededPermissions("admin")
            .build());

        PUNISHMENTS.add(PunishmentInfo.builder()
            .id(22)
            .title("Conteúdo sexualmente explícito")
            .modernId("conteudo_sexualmente_explicito")
            .description("Enviar mensagens que remetem a atos sexuais de maneira explícita e falas ou ações com teor sexual explícito.")
            .addPunishment(PunishTime.TWELVE_HOURS, PunishmentType.MUTE)
            .addPunishment(PunishTime.ONE_DAY, PunishmentType.MUTE)
            .addPunishment(PunishTime.THREE_DAYS, PunishmentType.TEMP)
            .addPunishment(PunishTime.ONE_WEEK, PunishmentType.TEMP)
            .addNeededPermissions("moderator")
            .reportable()
            .build());
    }

    public static PunishmentInfo getPunishmentById(int id) {
        if (id < 1 || id > PUNISHMENTS.size()) {
            throw new IllegalArgumentException("Id invalido");
        }
        return PUNISHMENTS.get(id - 1);
    }

    public static PunishmentInfo getPunishmentByModernId(String id) {
        for (PunishmentInfo punishment : PUNISHMENTS) {
            if (punishment.modernId().equalsIgnoreCase(id)) {
                return punishment;
            }
        }
        throw new IllegalArgumentException("Id invalido");
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
        private boolean reportable = false;
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

        public PunishmentsBuilder reportable() {
            this.reportable = true;
            return this;
        }

        public PunishmentInfo build() {
            return new PunishmentInfo(id, modernId == null ? "punishment_" + id : modernId, title, description, mustHavePermission, reportable, punishments);
        }
    }
}
