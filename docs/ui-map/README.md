# GroupPilot 当前 UI 层级图

基于 2026-11-18 本地代码（包含未提交修改），共覆盖 21 个 Fragment 页面。

- [00 总图](00-app-navigation.svg)
- [01 个人信息与任务总览](01-profile-and-task-overview.svg)
- [02 小组与项目管理](02-group-overview-and-permissions.svg)
- [03 任务、材料与分析](03-task-material-and-analysis.svg)

SVG 内的文字、卡片、连线均为矢量元素，可在浏览器、Figma、Inkscape 中打开。图底部链接可在各图间跳转。

树线表达包含层级；带“→”的节点表示跳转或复用。灰色框属于同一页面内的区域/状态，不代表单独页面。

说明：项目设置的旧成员移除与新小组总览入口目前并存；材料标签上传仍未接入；提交成果使用演示解析。这些在图中按实际代码标注。

重新生成：node docs/ui-map/generate.mjs

manifest.json 记录各图尺寸、节点数量和页面覆盖。
