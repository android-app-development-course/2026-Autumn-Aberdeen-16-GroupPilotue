package com.example.mycoursepractice.data

import java.util.Locale
import kotlin.math.roundToInt

/** 看板的四个阶段；顺序就是四段刻度条的从左到右。 */
enum class Stage(val label: String) {
    NOT_STARTED("待开始"),
    DOING("进行中"),
    PENDING("待确认"),
    DONE("已完成")
}

data class Member(val id: String, var name: String, var initial: String)

enum class EvidenceKind(val label: String) {
    FILE("文件"),
    LINK("链接"),
    TEXT("文字")
}

/** 提交物：类型 + 标题 + 一行说明 + 证据强度 1..3。 */
data class Evidence(
    val kind: EvidenceKind,
    val title: String,
    val meta: String,
    val strength: Int
)

/** 从材料里读出来的一条要求。warn = 与提交物不符。 */
data class Req(val label: String, val value: String, val warn: Boolean = false)

/** 一条改动记录：谁在什么时候，把哪一格从什么改成了什么。 */
data class Edit(
    val at: String,
    val by: String,
    val field: String,
    val from: String,
    val to: String
)

/**
 * 一个工作项。分数永远由三点算出来，不单独存：
 * 基础分 × 完成度 × 分摊。
 */
class WorkItem(
    val no: Int,
    val id: String,
    var title: String,
    var stage: Stage,
    var ownerId: String,
    var points: Int,
    var accepted: Int,
    var shares: Map<String, Double>,
    var due: String = "",
    var evidence: Evidence? = null,
    var reqs: List<Req> = emptyList(),
    var affected: Boolean = false,
    var confirmerId: String? = null,
    var confirmedAt: String? = null,
    var form: String = "",
    var location: String = "",
    var note: String = "",
    var updatedBy: String? = null,
    var updatedAt: String? = null,
    val fromMaterial: Boolean = true,
    var criteria: List<TaskCriterion> = emptyList(),
    var expectedPercent: Int = 100,
    var analysis: AnalysisResult? = null
) {
    val memberCount: Int get() = shares.size

    /** 每一次编辑都在这里留一行，详情页可展开。 */
    val history: MutableList<Edit> = mutableListOf()

    /** 这个工作项在「完成度」通过后的满分（不含分摊）。 */
    val confirmedPoints: Double get() = points.toDouble() * accepted / 100.0

    fun shareOf(memberId: String): Double = shares[memberId] ?: 0.0

    fun scoreOf(memberId: String): Double = confirmedPoints * shareOf(memberId)

    /** 与材料原文对照用：是否每格都对得上。 */
    val hasWarn: Boolean get() = reqs.any { it.warn }

    fun acceptedLabel(): String = String.format(Locale.US, "%d%%", accepted)
}
