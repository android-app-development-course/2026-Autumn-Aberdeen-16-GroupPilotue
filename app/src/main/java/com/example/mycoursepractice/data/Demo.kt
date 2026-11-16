package com.example.mycoursepractice.data

import kotlin.math.roundToInt

/**
 * In-memory demo. No login, no backend, no persistence: restart resets.
 *
 * 这一层现在管「账号」：课程、项目、邀请；以及「当前项目」。
 * 所有计分都在 [Project] 里，这里只是把当前项目的方法转发出去，
 * 这样原先写 `Demo.xxx` 的界面代码不用大改。
 */
object Demo {

    const val VIEWER = "he"
    const val RULE = "规则 v1.2"
    const val SNAPSHOT = "11-14 21:06"

    val me = Member("he", "何嘉俊", "何")

    /** 改显示名：只动这一个对象，成员栏 / 结算单 / 确认记录会一起变。 */
    fun renameViewer(newName: String) {
        val clean = newName.trim().ifEmpty { "我" }
        me.name = clean
        me.initial = clean.take(1)
    }

    private val he = me
    private val wang = Member("wang", "王舜祺", "王")
    private val chen = Member("chen", "陈实", "陈")
    private val li = Member("li", "李文豪", "李")
    private val zhou = Member("zhou", "周楚涵", "周")
    private val zhao = Member("zhao", "赵一鸣", "赵")
    private val sun = Member("sun", "孙可", "孙")

    // ── 课程 ────────────────────────────────────────────────────────────

    val courses: MutableList<Course> = mutableListOf(
        Course("cs4001", "CS4001", "人机交互", "2026 秋"),
        Course("comp3009", "COMP3009", "数据库系统", "2026 秋"),
        Course("soft3001", "SOFT3001", "软件工程", "2026 秋")
    )

    fun course(id: String): Course = courses.first { it.id == id }

    // ── 项目 ────────────────────────────────────────────────────────────

    private fun reqs(due: String, form: String, place: String, extra: String? = null, warn: Boolean = false): List<Req> =
        buildList {
            add(Req("形式", form))
            add(Req("位置", place))
            add(Req("截止", due, warn))
            if (extra != null) add(Req("署名", extra))
        }

    val projects: MutableList<Project> = mutableListOf()

    private fun seedProjects() {
        projects += Project(
            id = "p1",
            courseId = "cs4001",
            name = "交互设计大作业",
            groupLabel = "GROUP 16",
            planVersion = "v3",
            role = Role.CREATOR,
            members = mutableListOf(he, wang, chen, li),
            items = mutableListOf(
                WorkItem(
                    no = 1, id = "w01", title = "需求梳理与竞品调研", stage = Stage.DONE,
                    ownerId = "he", points = 10, accepted = 100,
                    shares = mapOf("he" to 0.8, "wang" to 0.2), due = "10-18",
                    evidence = Evidence(EvidenceKind.FILE, "需求梳理.pdf", "1.8 MB · 10-17 22:04", 3),
                    reqs = reqs("10-18 23:59", "报告 · PDF", "Moodle › 作业区 1"),
                    confirmerId = "wang", confirmedAt = "10-18 09:12"
                ),
                WorkItem(
                    no = 2, id = "w02", title = "访谈提纲与 6 场访谈", stage = Stage.DONE,
                    ownerId = "chen", points = 6, accepted = 100,
                    shares = mapOf("chen" to 1.0), due = "10-21",
                    evidence = Evidence(EvidenceKind.FILE, "访谈记录.zip", "8.4 MB · 10-20 19:40", 3),
                    reqs = reqs("10-21 12:00", "文字记录 · ZIP", "Moodle › 作业区 1"),
                    confirmerId = "he", confirmedAt = "10-21 20:15"
                ),
                WorkItem(
                    no = 3, id = "w03", title = "问卷投放与数据清洗", stage = Stage.DONE,
                    ownerId = "wang", points = 6, accepted = 100,
                    shares = mapOf("wang" to 0.8, "li" to 0.2), due = "10-24",
                    evidence = Evidence(EvidenceKind.FILE, "问卷数据.csv", "240 KB · 10-23 16:02", 3),
                    reqs = reqs("10-24 23:59", "数据 · CSV", "Moodle › 作业区 1"),
                    confirmerId = "chen", confirmedAt = "10-24 21:30"
                ),
                WorkItem(
                    no = 4, id = "w04", title = "交互流程与线框图", stage = Stage.DONE,
                    ownerId = "he", points = 10, accepted = 100,
                    shares = mapOf("he" to 0.6, "wang" to 0.4), due = "10-28",
                    evidence = Evidence(EvidenceKind.FILE, "交互流程图.pdf", "2.4 MB · 10-27 23:11", 3),
                    reqs = reqs("11-21 12:00 (UK)", "报告 · PDF", "Moodle › 作业区 2", "组长提交 + 贡献表", warn = true),
                    confirmerId = "wang", confirmedAt = "10-28 08:55"
                ),
                WorkItem(
                    no = 5, id = "w05", title = "UI 设计系统", stage = Stage.DONE,
                    ownerId = "li", points = 8, accepted = 100,
                    shares = mapOf("li" to 1.0), due = "10-31",
                    evidence = Evidence(EvidenceKind.LINK, "设计系统.fig", "Figma · 34 个组件", 2),
                    reqs = reqs("10-31 23:59", "链接 · Figma", "Moodle › 作业区 2"),
                    confirmerId = "he", confirmedAt = "11-01 10:20"
                ),
                WorkItem(
                    no = 6, id = "w06", title = "看板页高保真", stage = Stage.DONE,
                    ownerId = "wang", points = 12, accepted = 100,
                    shares = mapOf("wang" to 0.6, "he" to 0.4), due = "11-04",
                    evidence = Evidence(EvidenceKind.LINK, "看板页.fig", "Figma · 8 屏", 3),
                    reqs = reqs("11-04 23:59", "链接 · Figma", "Moodle › 作业区 2"),
                    confirmerId = "li", confirmedAt = "11-05 09:40"
                ),
                WorkItem(
                    no = 7, id = "w07", title = "结算页高保真", stage = Stage.DONE,
                    ownerId = "li", points = 8, accepted = 100,
                    shares = mapOf("li" to 0.5, "chen" to 0.5), due = "11-07",
                    evidence = Evidence(EvidenceKind.LINK, "结算页.fig", "Figma · 3 屏", 2),
                    reqs = reqs("11-07 23:59", "链接 · Figma", "Moodle › 作业区 2"),
                    confirmerId = "wang", confirmedAt = "11-08 11:05"
                ),
                WorkItem(
                    no = 8, id = "w08", title = "动效与转场", stage = Stage.PENDING,
                    ownerId = "li", points = 8, accepted = 50,
                    shares = mapOf("li" to 1.0), due = "11-12",
                    evidence = Evidence(EvidenceKind.LINK, "动效演示.mp4", "00:42 · 6 个转场", 2),
                    reqs = reqs("11-12 23:59", "视频 · MP4", "Moodle › 作业区 2"),
                    confirmerId = null, confirmedAt = null
                ),
                WorkItem(
                    no = 9, id = "w09", title = "演示视频剪辑", stage = Stage.PENDING,
                    ownerId = "chen", points = 6, accepted = 50,
                    shares = mapOf("chen" to 1.0), due = "11-13",
                    evidence = Evidence(EvidenceKind.LINK, "演示视频.mp4", "01:25 · 草剪", 1),
                    reqs = reqs("11-13 23:59", "视频 · MP4", "Moodle › 作业区 2"),
                    confirmerId = null, confirmedAt = null
                ),
                WorkItem(
                    no = 10, id = "w10", title = "汇报 PPT", stage = Stage.DOING,
                    ownerId = "he", points = 6, accepted = 0,
                    shares = mapOf("he" to 1.0), due = "11-16",
                    reqs = reqs("11-16 23:59", "文件 · PPTX", "Moodle › 作业区 2"),
                    confirmerId = null, confirmedAt = null
                ),
                WorkItem(
                    no = 11, id = "w11", title = "试点招募与执行", stage = Stage.PENDING,
                    ownerId = "wang", points = 12, accepted = 0,
                    shares = mapOf("wang" to 0.8, "chen" to 0.2), due = "11-17",
                    evidence = Evidence(EvidenceKind.TEXT, "招募邮件与群公告", "5 位试点 · 已约时间", 1),
                    reqs = reqs("11-17 23:59", "文字说明", "Moodle › 作业区 2"),
                    confirmerId = null, confirmedAt = null
                ),
                WorkItem(
                    no = 12, id = "w12", title = "数据回收与复盘", stage = Stage.NOT_STARTED,
                    ownerId = "chen", points = 8, accepted = 0,
                    shares = mapOf("chen" to 1.0), due = "11-20",
                    reqs = reqs("11-20 23:59", "报告 · PDF", "Moodle › 作业区 2"),
                    confirmerId = null, confirmedAt = null
                )
            ),
            inviteCode = "K7M2Q4",
            lastActivity = "2 小时前"
        )

        projects += Project(
            id = "p2",
            courseId = "cs4001",
            name = "可用性测试试点",
            groupLabel = "G16 · 试点",
            planVersion = "v1",
            role = Role.MEMBER,
            members = mutableListOf(he, chen, zhou),
            items = mutableListOf(
                WorkItem(
                    no = 1, id = "a01", title = "试点任务脚本", stage = Stage.DONE,
                    ownerId = "he", points = 8, accepted = 100,
                    shares = mapOf("he" to 1.0), due = "11-09",
                    evidence = Evidence(EvidenceKind.FILE, "试点脚本.pdf", "620 KB · 11-08 21:10", 2),
                    reqs = reqs("11-09 23:59", "文档 · PDF", "Moodle › 试点单元"),
                    confirmerId = "chen", confirmedAt = "11-09 09:02"
                ),
                WorkItem(
                    no = 2, id = "a02", title = "招募 5 位参与者", stage = Stage.PENDING,
                    ownerId = "chen", points = 10, accepted = 0,
                    shares = mapOf("chen" to 1.0), due = "11-12",
                    evidence = Evidence(EvidenceKind.TEXT, "招募名单", "4 人已确认 · 1 人待回复", 1),
                    reqs = reqs("11-12 23:59", "文字说明", "Moodle › 试点单元"),
                    confirmerId = null, confirmedAt = null
                ),
                WorkItem(
                    no = 3, id = "a03", title = "试点执行记录", stage = Stage.DOING,
                    ownerId = "zhou", points = 12, accepted = 0,
                    shares = mapOf("zhou" to 1.0), due = "11-15",
                    reqs = reqs("11-15 23:59", "记录 · ZIP", "Moodle › 试点单元"),
                    confirmerId = null, confirmedAt = null
                ),
                WorkItem(
                    no = 4, id = "a04", title = "复盘简报", stage = Stage.NOT_STARTED,
                    ownerId = "he", points = 10, accepted = 0,
                    shares = mapOf("he" to 1.0), due = "11-18",
                    reqs = reqs("11-18 23:59", "报告 · PDF", "Moodle › 试点单元"),
                    confirmerId = null, confirmedAt = null
                )
            ),
            lastActivity = "昨天"
        )

        projects += Project(
            id = "p3",
            courseId = "comp3009",
            name = "图书借阅系统课程设计",
            groupLabel = "GROUP 04",
            planVersion = "v2",
            role = Role.MEMBER,
            members = mutableListOf(he, li, wang, zhao, sun),
            items = mutableListOf(
                WorkItem(
                    no = 1, id = "b01", title = "需求与 ER 图", stage = Stage.DONE,
                    ownerId = "zhao", points = 10, accepted = 100,
                    shares = mapOf("zhao" to 0.7, "he" to 0.3), due = "10-30",
                    evidence = Evidence(EvidenceKind.FILE, "ER图.pdf", "1.1 MB · 10-29 22:31", 3),
                    reqs = reqs("10-30 23:59", "报告 · PDF", "Blackboard › 课程设计"),
                    confirmerId = "he", confirmedAt = "10-30 08:40"
                ),
                WorkItem(
                    no = 2, id = "b02", title = "表结构与索引设计", stage = Stage.DONE,
                    ownerId = "he", points = 10, accepted = 100,
                    shares = mapOf("he" to 1.0), due = "11-03",
                    evidence = Evidence(EvidenceKind.FILE, "schema.sql", "18 KB · 11-02 16:22", 2),
                    reqs = reqs("11-03 23:59", "SQL · 文本", "Blackboard › 课程设计"),
                    confirmerId = "li", confirmedAt = "11-03 10:15"
                ),
                WorkItem(
                    no = 3, id = "b03", title = "存储过程与触发器", stage = Stage.PENDING,
                    ownerId = "sun", points = 12, accepted = 50,
                    shares = mapOf("sun" to 0.6, "li" to 0.4), due = "11-10",
                    evidence = Evidence(EvidenceKind.FILE, "procedures.sql", "26 KB · 11-09 23:58", 2),
                    reqs = reqs("11-10 23:59", "SQL · 文本", "Blackboard › 课程设计"),
                    confirmerId = null, confirmedAt = null
                ),
                WorkItem(
                    no = 4, id = "b04", title = "前端借阅页", stage = Stage.DOING,
                    ownerId = "wang", points = 10, accepted = 0,
                    shares = mapOf("wang" to 0.6, "sun" to 0.4), due = "11-14",
                    reqs = reqs("11-14 23:59", "仓库 · 链接", "Blackboard › 课程设计"),
                    confirmerId = null, confirmedAt = null
                ),
                WorkItem(
                    no = 5, id = "b05", title = "设计报告", stage = Stage.NOT_STARTED,
                    ownerId = "li", points = 8, accepted = 0,
                    shares = mapOf("li" to 1.0), due = "11-20",
                    reqs = reqs("11-20 23:59", "报告 · PDF", "Blackboard › 课程设计"),
                    confirmerId = null, confirmedAt = null
                )
            ),
            lastActivity = "11-12"
        )
    }

    // ── 邀请 ────────────────────────────────────────────────────────────

    val invitations: MutableList<Invitation> = mutableListOf()

    private fun seedInvitations() {
        invitations += Invitation(
            id = "inv1",
            code = "R3F8K2",
            inviterName = "周楚涵",
            sentAt = "昨天 20:14",
            memberCount = 5,
            project = Project(
                id = "p4",
                courseId = "soft3001",
                name = "迭代 3 · 可用性测试",
                groupLabel = "G07",
                planVersion = "v1",
                role = Role.MEMBER,
                members = mutableListOf(
                    Member("zhou", "周楚涵", "周"),
                    Member("luo", "罗一鸣", "罗"),
                    Member("qin", "秦朗", "秦"),
                    Member("fang", "方可", "方")
                ),
                items = mutableListOf(
                    WorkItem(no = 1, id = "c01", title = "测试方案", stage = Stage.DONE, ownerId = "zhou",
                        points = 10, accepted = 100, shares = mapOf("zhou" to 1.0), due = "11-08"),
                    WorkItem(no = 2, id = "c02", title = "5 场可用性测试", stage = Stage.DOING, ownerId = "luo",
                        points = 12, accepted = 0, shares = mapOf("luo" to 1.0), due = "11-18"),
                    WorkItem(no = 3, id = "c03", title = "问题清单与优先级", stage = Stage.NOT_STARTED, ownerId = "qin",
                        points = 8, accepted = 0, shares = mapOf("qin" to 1.0), due = "11-21")
                ),
                lastActivity = "昨天"
            )
        )
    }

    private val expiredCodes = setOf("X9T2W5")

    init {
        seedProjects()
        seedInvitations()
    }

    // ── 当前项目 ────────────────────────────────────────────────────────

    var currentId: String? = null
        private set

    val current: Project get() = projects.first { it.id == currentId }

    fun openProject(id: String) {
        currentId = id
        bump()
    }

    fun closeProject() {
        currentId = null
        bump()
    }

    /** Bumped whenever the demo state changes, so open screens know to re-read it. */
    var version: Int = 0
        private set

    fun bump() {
        version++
    }

    // ── 账号级查询 ──────────────────────────────────────────────────────

    fun projectsOf(courseId: String): List<Project> = projects.filter { it.courseId == courseId }

    fun coursesWithProjects(): List<Course> = courses.filter { c -> projects.any { it.courseId == c.id } }

    fun pendingInvitations(): List<Invitation> = invitations.toList()

    fun joinByCode(raw: String): JoinResult {
        val code = raw.trim().uppercase()
        if (code.isEmpty()) return JoinResult.NotFound
        if (code in expiredCodes) return JoinResult.Expired
        val inv = invitations.firstOrNull { it.code == code }
        if (inv != null) return JoinResult.Ok(inv.project)
        projects.firstOrNull { it.inviteCode.equals(code, true) }?.let {
            return JoinResult.AlreadyIn(it.name)
        }
        return JoinResult.NotFound
    }

    fun invitationFor(code: String): Invitation? =
        invitations.firstOrNull { it.code.equals(code.trim(), ignoreCase = true) }

    fun acceptInvitation(inv: Invitation): Project {
        invitations.remove(inv)
        val project = inv.project
        project.role = Role.MEMBER
        project.members += me
        projects += project
        bump()
        return project
    }

    fun declineInvitation(inv: Invitation) {
        invitations.remove(inv)
        bump()
    }

    // ── 创建 / 管理 ─────────────────────────────────────────────────────

    private fun nextInviteCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { alphabet.random() }.joinToString("")
    }

    fun addCourse(code: String, name: String, term: String): Course {
        val id = "c" + (courses.size + 1) + System.currentTimeMillis().toString().takeLast(3)
        val course = Course(id, code.trim().uppercase(), name.trim(), term.trim().ifEmpty { "2026 秋" })
        courses += course
        bump()
        return course
    }

    fun createProject(
        courseId: String,
        name: String,
        template: String,
        totalPoints: Double,
        requireApproval: Boolean
    ): Project {
        val id = "p" + (projects.size + 1) + System.nanoTime().toString().takeLast(4)
        val project = Project(
            id = id,
            courseId = courseId,
            name = name.trim(),
            groupLabel = "GROUP " + (10 + projects.size).toString().padStart(2, '0'),
            planVersion = "v1",
            role = Role.CREATOR,
            members = mutableListOf(me),
            items = templateItems(template, totalPoints),
            totalPoints = totalPoints,
            inviteCode = nextInviteCode(),
            requireApproval = requireApproval,
            lastActivity = "刚刚"
        )
        projects += project
        bump()
        return project
    }

    private fun templateItems(template: String, total: Double): MutableList<WorkItem> = when (template) {
        "按周" -> listOf(
            "第 1 周 · 选题与分工" to 0.25,
            "第 2 周 · 调研与方案" to 0.25,
            "第 3 周 · 制作与验证" to 0.25,
            "第 4 周 · 汇报材料" to 0.25
        )
        "按交付物" -> listOf(
            "需求与范围" to 0.2,
            "设计与原型" to 0.3,
            "实现与测试" to 0.3,
            "报告与答辩" to 0.2
        )
        else -> emptyList()
    }.mapIndexed { index, (title, ratio) ->
        WorkItem(
            no = index + 1,
            id = "t%02d".format(index + 1),
            title = title,
            stage = Stage.NOT_STARTED,
            ownerId = VIEWER,
            points = (total * ratio).toInt(),
            accepted = 0,
            shares = mapOf(VIEWER to 1.0),
            reqs = emptyList()
        )
    }.toMutableList()

    fun regenerateCode(project: Project) {
        project.inviteCode = nextInviteCode()
        bump()
    }

    fun archive(project: Project) {
        project.archived = true
        bump()
    }

    fun leave(project: Project) {
        projects.remove(project)
        if (currentId == project.id) currentId = null
        bump()
    }

    fun removeMember(project: Project, memberId: String) {
        if (memberId == VIEWER) return
        project.members.removeAll { it.id == memberId }
        project.items.forEach { item ->
            if (item.shares.containsKey(memberId)) {
                item.shares = item.shares - memberId
            }
        }
        bump()
    }

    // ── 当前项目的门面 ──────────────────────────────────────────────────

    // ── 工作项：新建 / 修改 / 删除 ──────────────────────────────────────────

    private fun stamp(): String =
        java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.US).format(java.util.Date())

    fun allocatedPoints(): Int = current.items.sumOf { it.points }

    fun remainingPoints(): Int = (current.totalPoints - allocatedPoints()).toInt()

    fun nextItemNo(): Int = (current.items.maxOfOrNull { it.no } ?: 0) + 1

    private fun nextItemId(no: Int): String {
        val prefix = current.items.firstOrNull()?.id?.takeWhile { it.isLetter() } ?: "w"
        return prefix + "%02d".format(no)
    }

    /** 发起人，或这项的负责人，才有资格改。 */
    fun canEdit(item: WorkItem): Boolean =
        current.role == Role.CREATOR || item.ownerId == VIEWER

    /** 已确认的任务锁死：改分数只能走修正申请。 */
    fun locked(item: WorkItem): Boolean = item.stage == Stage.DONE || item.accepted >= 100

    fun canEditScoring(item: WorkItem): Boolean = canEdit(item) && !locked(item)

    fun canDelete(item: WorkItem): Boolean =
        canEdit(item) && !locked(item) && item.evidence == null &&
            (item.stage == Stage.NOT_STARTED || item.stage == Stage.DOING)

    fun createItem(
        title: String,
        ownerId: String,
        points: Int,
        shares: Map<String, Double>,
        stage: Stage,
        due: String,
        form: String,
        location: String,
        note: String
    ): WorkItem {
        val no = nextItemNo()
        val reqList = mutableListOf<Req>()
        if (form.isNotBlank()) reqList += Req("形式", form)
        if (location.isNotBlank()) reqList += Req("位置", location)
        if (due.isNotBlank()) reqList += Req("截止", due)
        val item = WorkItem(
            no = no,
            id = nextItemId(no),
            title = title.trim(),
            stage = stage,
            ownerId = ownerId,
            points = points,
            accepted = 0,
            shares = shares,
            due = due,
            form = form,
            location = location,
            note = note,
            reqs = reqList,
            fromMaterial = false
        )
        current.items += item
        current.lastActivity = "刚刚"
        bump()
        return item
    }

    fun updateItem(
        item: WorkItem,
        title: String,
        ownerId: String,
        points: Int,
        shares: Map<String, Double>,
        stage: Stage,
        due: String,
        form: String,
        location: String,
        note: String
    ) {
        val at = stamp()
        val by = me.name

        fun log(field: String, from: String, to: String) {
            if (from != to) item.history += Edit(at, by, field, from, to)
        }

        log("标题", item.title, title.trim())
        log("负责人", nameOf(item.ownerId), nameOf(ownerId))
        log("分值", item.points.toString(), points.toString())
        log("分摊", shareText(item.shares), shareText(shares))
        log("阶段", item.stage.label, stage.label)
        log("截止", item.due, due)
        log("交付形式", item.form, form)
        log("位置", item.location, location)
        log("说明", item.note, note)

        item.title = title.trim()
        item.ownerId = ownerId
        item.points = points
        item.shares = shares
        item.stage = stage
        item.due = due
        item.form = form
        item.location = location
        item.note = note
        item.reqs = mutableListOf<Req>().apply {
            if (form.isNotBlank()) add(Req("形式", form))
            if (location.isNotBlank()) add(Req("位置", location))
            if (due.isNotBlank()) add(Req("截止", due))
        }
        item.updatedBy = by
        item.updatedAt = at
        bump()
    }

    fun deleteItem(item: WorkItem) {
        current.items.removeAll { it.id == item.id }
        bump()
    }

    fun shareText(shares: Map<String, Double>): String =
        shares.entries.sortedByDescending { it.value }
            .joinToString(" / ") { "${nameOf(it.key)} ${(it.value * 100).roundToInt()}" }

    val members: List<Member> get() = current.members

    fun member(id: String): Member = current.member(id)

    fun nameOf(id: String?): String = current.nameOf(id)

    val items: MutableList<WorkItem> get() = current.items

    fun item(id: String): WorkItem = current.item(id)

    val courseLabel: String get() = course(current.courseId).label

    val groupLabel: String get() = current.groupLabel

    val planLabel: String get() = "计划 ${current.planVersion}"

    val projectName: String get() = current.name

    fun scoreOf(memberId: String): Double = current.scoreOf(memberId)

    fun total(): Double = current.total()

    fun ratioOf(memberId: String): Double = current.ratioOf(memberId)

    fun itemsOf(stage: Stage): List<WorkItem> = current.itemsOf(stage)

    fun pendingForViewer(): List<WorkItem> = current.pendingForViewer(VIEWER)

    fun confirmedCount(): Int = current.confirmedCount()

    fun fullyConfirmedCount(): Int = current.fullyConfirmedCount()

    fun pendingCount(): Int = current.pendingCount()

    fun openCount(): Int = current.openCount()

    fun shareSummary(item: WorkItem): String = current.shareSummary(item)

    fun formula(item: WorkItem, memberId: String): String = current.formula(item, memberId)

    fun confirmerFor(item: WorkItem): String = current.confirmerFor(item, VIEWER)

    fun unconfirmedCount(): Int = current.unconfirmedCount()

    fun shareItemsOf(memberId: String): List<WorkItem> = current.shareItemsOf(memberId)

    fun shareOfTotal(): Double = current.shareOfTotal()

    fun confirmedShareOfTotal(): Double = current.confirmedShareOfTotal()
}
