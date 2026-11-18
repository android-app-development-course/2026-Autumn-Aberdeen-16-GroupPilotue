package com.example.mycoursepractice.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class TaskCriterion(
    val id: String,
    val ownerId: String,
    val title: String,
    val target: Int,
    val achieved: Int,
    val unit: String
) {
    val met: Boolean get() = achieved >= target
}

data class AnalysisResult(val criteria: List<TaskCriterion>, val expectedPercent: Int, val demo: Boolean = true)
data class TaskEntry(val project: Project, val item: WorkItem)
enum class GroupPermission(val label: String) { OWNER("组长"), ADMIN("管理员"), MEMBER("成员") }
data class GroupPerson(val member: Member, val permission: GroupPermission)
data class GroupInvite(val account: String, val permission: GroupPermission)

object TaskInsights {
    /** Legacy MM-dd demo dates belong to the course's declared year, not the device year. */
    fun dueDate(value: String, year: Int = 2026): Calendar? {
        val token = Regex("(?:\\d{4}-)?\\d{1,2}-\\d{1,2}").find(value)?.value ?: return null
        val input = if (token.count { it == '-' } == 1) "$year-$token" else token
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val date = try { parser.parse(input) } catch (_: Exception) { null } ?: return null
        return Calendar.getInstance().apply { time = date }
    }

    fun key(date: Calendar, pattern: String = "yyyy-MM-dd"): String =
        SimpleDateFormat(pattern, Locale.US).format(date.time)

    fun criteria(item: WorkItem): List<TaskCriterion> = item.analysis?.criteria ?: item.criteria

    fun progress(item: WorkItem, member: String? = null): Double? {
        val rows = criteria(item).filter { member == null || it.ownerId == member }
        if (rows.isEmpty()) return if (member == null || item.shareOf(member) > 0) item.accepted.toDouble() else null
        return rows.sumOf { (it.achieved.toDouble() / it.target.coerceAtLeast(1)).coerceIn(0.0, 1.0) } / rows.size * 100
    }

    /** Workload = points × assigned share. Analysis never changes accepted contribution. */
    fun weightedProgress(entries: List<TaskEntry>, member: String? = null): Double? {
        val total = entries.sumOf { weight(it.item, member) }
        if (total <= 0) return null
        return entries.sumOf { weight(it.item, member) * (progress(it.item, member) ?: 0.0) } / total
    }

    fun weight(item: WorkItem, member: String? = null): Double =
        item.points.toDouble().coerceAtLeast(0.0) * (if (member == null) 1.0 else item.shareOf(member))

    fun expected(entries: List<TaskEntry>, member: String): Double? {
        val total = entries.sumOf { weight(it.item, member) }
        return if (total <= 0) null else entries.sumOf { weight(it.item, member) * it.item.expectedPercent } / total
    }

    fun seed(project: Project) {
        project.items.forEach { item ->
            if (item.criteria.isNotEmpty()) return@forEach
            val target = when {
                item.title.contains("PPT") -> 12
                item.title.contains("访谈") -> 6
                item.title.contains("招募") -> 5
                else -> 4
            }
            val unit = when {
                item.title.contains("PPT") -> "页"
                item.title.contains("访谈") -> "场"
                item.title.contains("招募") -> "人"
                else -> "项"
            }
            item.criteria = item.shares.filterValues { it > 0 }.keys.flatMap { owner ->
                val done = if (item.stage == Stage.DONE) target else if (item.stage == Stage.NOT_STARTED) 0 else (target * 0.5).toInt()
                listOf(
                    TaskCriterion("${item.id}-$owner-delivery", owner, "${item.title}：提交本人负责的内容", target, done, unit),
                    TaskCriterion("${item.id}-$owner-review", owner, "附可访问成果及逐项自查记录", 2, if (item.stage == Stage.DONE) 2 else 0, "份")
                )
            }
            item.expectedPercent = if (item.stage == Stage.NOT_STARTED) 40 else if (item.stage == Stage.DOING) 75 else 100
        }
    }
}
