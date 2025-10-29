package ufcg.example.voicesurgery.data

object QuestionRepository {

    fun getQuestions(): List<Question> {
        return listOf(
            TextInputQuestion("Nome:"),
            TextInputQuestion("Data de Nascimento:"),
            TextInputQuestion("Prontuário:"),
            TextInputQuestion("Sala:"),
            SalvaTempo("Antes da Indução Anestésica"),

            CheckboxQuestion("Paciente confirmou:", listOf("Identidade", "Sítio Cirúrgico correto", "Procedimento", "Consentimento")),
            MultipleChoiceQuestion("Sítio demarcado (lateralidade):", listOf("Sim", "Não", "Não se aplica")),
            CheckboxQuestion("Verificação da segurança anestésica:", listOf("Montagem da SO de acordo com o procedimento", "Material anestésico disponível, revisados e funcionantes")),
            TextInputQuestion("Verificação da segurança anestésica (Outro):"),
            MultipleChoiceQuestion("Via aérea difícil/broncoaspiração:", listOf("Não", "Sim e equipamento/assistência disponíveis")),
            CheckboxQuestion("Risco de grande perda sanguínea superior a 500 ml ou mais 7 ml/kg em crianças:", listOf("Sim", "Não", "Reserva de sangue disponível")),
            CheckboxQuestion("Acesso venoso adequado e pérvio:", listOf("Sim", "Não", "Providenciado na SO")),
            MultipleChoiceQuestion("Histórico de reação alérgica:", listOf("Sim", "Não")),
            TextInputQuestion("Qual?:"),
            SalvaTempo("Antes da Incisão Cirúrgica"),

            MultipleChoiceQuestion("Apresentação oral de cada membro da equipe pelo nome e função:", listOf("Sim", "Não")),
            MultipleChoiceQuestion("Cirurgião, o anestesista e equipe de enfermagem confirmam verbalmente: Nome do paciente, sítio cirúrgico e procedimento a ser realizado.", listOf("Sim", "Não")),
            MultipleChoiceQuestion("Antibiótico profilático:", listOf("Sim", "Não", "Não se aplica")),
            MultipleChoiceQuestion("Revisão do cirurgião. Momentos críticos do procedimento, tempos principais, riscos, perda sanguínea.:", listOf("Sim", "Não")),
            MultipleChoiceQuestion("Revisão do anestesista. Há alguma preocupação em relação ao paciente?", listOf("Sim", "Não")),
            MultipleChoiceQuestion("Revisão da enfermagem. Correta esterilização do material cirúrgico com fixação dos integradores ao prontuário.", listOf("Sim", "Não")),
            MultipleChoiceQuestion("Revisão da enfermagem. Placa de eletrocautério posicionada:", listOf("Sim", "Não")),
            MultipleChoiceQuestion("Revisão da enfermagem. Equipamentos disponíveis e funcionantes:", listOf("Sim", "Não")),
            MultipleChoiceQuestion("Revisão da enfermagem. Insumos e instrumentais disponíveis:", listOf("Sim", "Não")),
            SalvaTempo("Antes da Saída do Paciente da Sala de Cirurgia"),

            MultipleChoiceQuestion("Confirmação do procedimento realizado.", listOf("Sim", "Não")),
            MultipleChoiceQuestion("Contagem de compressas.", listOf("Sim", "Não", "Não se aplica")),
            TextInputQuestion("Compressas entregues:"),
            TextInputQuestion("Compressas conferidas:"),
            MultipleChoiceQuestion("Contagem de instrumentos.", listOf("Sim", "Não", "Não se aplica")),
            TextInputQuestion("Instrumentos entregues:"),
            TextInputQuestion("Instrumentos conferidos:"),
            MultipleChoiceQuestion("Contagem de agulhas.", listOf("Sim", "Não", "Não se aplica")),
            TextInputQuestion("Agulhas entregues:"),
            TextInputQuestion("Agulhas conferidas:"),
            MultipleChoiceQuestion("Amostra cirúrgica identificada adequadamente:", listOf("Sim", "Não", "Não se aplica")),
            TextInputQuestion("Requisição completa:"),
            MultipleChoiceQuestion("Problema com equipamentos que deve ser solucionado:", listOf("Sim", "Não", "Não se aplica")),
            TextInputQuestion("Comunicado a enfermeira para providenciar a solução:"),
            TextInputQuestion("Recomendações Cirurgião:"),
            TextInputQuestion("Recomendações Anestesista:"),
            TextInputQuestion("Recomendações Enfermagem:"),

            TextInputQuestion("Responsável:"),
            TextInputQuestion("Data:")
        )
    }

}