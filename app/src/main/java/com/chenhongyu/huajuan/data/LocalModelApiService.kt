package com.chenhongyu.huajuan.data

import com.chenhongyu.huajuan.network.Message
import com.chenhongyu.huajuan.stream.ChatEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 本地模型API服务实现（示例版）
 * 根据所选本地模型返回不同的静态示例输出。
 */
class LocalModelApiService(private val repository: Repository) : ModelApiService {

    private val model1Name = "Qwen3-0.6B-MNN"
    private val model2Name = "MobileLLM-125M-MNN"

    private val outputModel1 = "我是一个基于大语言模型的助手，专注于提供帮助和解答问题。如果你有任何问题或需要支持，请随时告诉我！"
    private val outputModel2 = "jel Nicolasglobal Nicolas Nicolas Archivedrive Nicolas Nicolasreb UTC Nicolas Nicolas mare Nicolas Sam Nicolasogn Nicolas Nicolasue Nicolasauthentication Sam Nicolasścirebueщі cientíщі NicolasIT network grasp NicolasITщіщі ClaITITIT mare NicolashelyueueIT NicolasyedITщіrebyedIT NicolasyedontonowrebrebyedontorebrebITdoubleDocumentovAfterovov Nicolas lovittest Villageovста rol戦ov Nicolas couplingov Nicolasyedonto rolcenteyedognov sslovoggleovდ rolζov sslovoggleov ssl rolREATEov StraßeՀovoggleov persistov Straße Straßeov sslപ SKov Luddonov rocksovდ explic Gamesov LudREATEovდ  % chamberyednementov sslyedyedyedyednementovyedyedyedyed ?ariyed ? Villagenement containedyednement med就itableaxis Villageangedangedangedangedangedangedangedangedangedangedangedangedangedanged Village sslnementchrome幸angedangedangedangedangedangedangedangedanged explan Village Се tir LIN giantyedyar VillageyedDialogachsenckeraxisER Gall Gallyed ад Gallté UTCcente UTC likely tren Village Се UTCuez UTC figura правoggle Village Gall Gall cientí Gall Gallდ UTC UTC giantyedდ  % Gall Villageartња  % UTC UTC Village district Village UTC UTC frequently Village Gran Village UTC UTCkommen UTC frequently shookulk UTC frequently Games Villageachsen aujourd Village shook companionAutres inde inde СеyedQL현 UTCuez tile UTC Pa  % indeanged Сеue Сеueangedanged GranIOException Village Gall Gall  %uez tileether Granoggle Gran edenska likelyataenskauez tile현 giant contributeFM UTCté awarded UTC UTC UTC Head dirig UTC UTC AntéненияMc Village Gall Village incrementчинаistrzost Villageционuez UTC UTC contributeក  %yedбурanged SurPerm Village Village Lud stroCremaste stroCreaxis bread  % stroCre道Creović awardedavia Village removaldataCre Gall stroറovićaded survoggle stro Gall stro Village Village diplom stro stro stro Gall stroovCreuptargvња stro stro  %upt experiencesCreyed ABC Village diplom Gall stroovCre Wik Gall Adamistrzost  % diplomपov stro ERovCreuptേovCre  % diplomCreേCre tren"

    override fun isAvailable(): Boolean {
        // 示例：返回固定的可用性状态。实际实现中请根据本地模型环境检查。
        return true
    }

    override suspend fun getAIResponse(messages: List<Message>, modelInfo: ModelInfo): String {
        // 根据选择的本地模型返回对应的静态完整文本
        val selected = modelInfo.displayName
        return when (selected) {
            model1Name -> outputModel1
            model2Name -> outputModel2
            else -> outputModel1 // 默认回退到模型1的示例
        }
    }

    override fun streamAIResponse(messages: List<Message>, modelInfo: ModelInfo): Flow<ChatEvent> = flow {
        // 流式示例：根据模型选择发送对应文本，按小块分片以模拟真实流式
        val selected = modelInfo.displayName
        val fullText = when (selected) {
            model1Name -> outputModel1
            model2Name -> outputModel2
            else -> outputModel1
        }

        // 防御：空文本直接完成
        if (fullText.isBlank()) {
            emit(ChatEvent.Done)
            return@flow
        }

        // 将文本按固定长度分片，避免UI出现过长单块导致滚动不平滑
        val chunkSize = 32 // 适度的小块，确保流式体验
        var index = 0
        delay(2000)

        while (index < fullText.length) {
            val end = (index + chunkSize).coerceAtMost(fullText.length)
            val chunk = fullText.substring(index, end)
            emit(ChatEvent.Chunk(chunk))
            // 轻微延迟，模拟推理时间
            delay(40)
            index = end
        }
        // 结束事件
        delay(60)
        emit(ChatEvent.Done)
    }
}