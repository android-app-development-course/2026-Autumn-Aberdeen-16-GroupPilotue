import {mkdir,writeFile,readFile,readdir} from 'node:fs/promises';
import {dirname,resolve} from 'node:path';
import {fileURLToPath} from 'node:url';
const dir=dirname(fileURLToPath(import.meta.url));
// Native SVG text, rectangles and paths; no raster text or external resources.
const N=(title, detail='', children=[], kind='part', source='')=>({title,detail,children,kind,source});
const P=(title,source,detail='',children=[])=>N(title,detail,children,'page',source);
const M=(title,detail='',children=[])=>N(title,detail,children,'modal');
const R=(title,detail='')=>N(title,detail,[],'ref');
const maps=[
 {file:'00-app-navigation.svg',title:'GroupPilot · 全部 UI 层级总图',sub:'按当前源码整理 · 21 个 Fragment 页面 · 账号层 / 项目层 / 任务协作层 · 2026-11-18',
 root:'MainActivity · 应用导航容器',rootDetail:'线条表示层级与包含关系；标有 → 的节点表示跳转或复用页面，详图中展开其内部交互。',
 trees:[
 N('01 入口与账号层','登录后进入项目库；账号层不显示项目底部标签',[
 P('登录','LoginFragment','用户名、密码校验；注册按钮仅显示说明'),
 P('项目库','LibraryFragment','课程 → 项目 → 小任务；每个项目可折叠',[
 P('个人信息','ProfileFragment','默认只读 → 编辑 / 保存 / 取消'),
 P('创建项目','CreateProjectFragment','选课程 / 新课程、名称、模板、总分、审批'),
 P('加入项目','JoinProjectFragment','邀请码 → 预览 → 加入；邀请条可预填'),
 P('个人全任务总览','TaskOverviewFragment','列表 / 周课表 / 月历 / 森林；个人工作量浮窗'),
 R('→ 板 · 项目主页','点击深蓝项目标题卡片'),
 R('→ 任务详情','点击项目内的小任务卡片'),
 ]),
 M('应用设置','大 / 中 / 小字号；从有齿轮入口的页面打开'),
 ]),
 N('02 项目协作层','进入项目后显示：板 / 收件箱 / 结算 / 材料',[
 P('板 · 项目主页','BoardFragment','阶段筛选、成员概览、待确认、任务列表',[
 R('→ 任务详情 / 新建任务','点击任务 / 点击 + 新建'),
 R('→ 个人全任务总览','板内树状图按钮'),
 P('项目设置','ProjectSettingsFragment','邀请码、审批开关、成员移除、切换 / 退出'),
 ]),
 P('收件箱','InboxFragment','待确认任务 → 任务详情',[
 P('计划确认','PlanFragment','计划 v4 差异、分摊变化、确认生效'),
 ]),
 P('结算单','ReportFragment','项目总分、成员贡献占比、规则说明',[
 P('成员结算明细','ScoreDetailFragment','已计入 / 尚未计入的工作项与逐项算式'),
 ]),
 P('材料','MaterialsFragment','已解析 / 失败重试；上传按钮尚未接入',[
 P('要求对照','RequirementFragment','要求条目、来源页码与引用原文'),
 ]),
 P('小组总览','GroupOverviewFragment','各项目标签页右上角组名 → 成员 / 邀请 / 权限'),
 ]),
 N('03 任务处理层','由项目库、板、收件箱或全任务总览打开',[
 P('任务详情','ItemDetailFragment','细则、三层完成比例、分摊、证据、贡献、改动记录',[
 P('新建 / 编辑工作项','ItemEditFragment','标题、负责人、分值、分摊、阶段、DDL 等'),
 P('提交成果','SubmitFragment','文件 / 链接 / 文字 → 解析进度 → 分析结果'),
 P('完成确认','ConfirmFragment','0% / 50% / 100%；预览计分影响后确认'),
 P('证据定位','EvidenceFragment','演示材料页高亮定位，不是外部浏览器'),
 P('修正申请','CorrectionFragment','分摊 / 完成度 / 要求对照；提交后未决'),
 R('→ 结算单','已完成任务主按钮；复用 ReportFragment'),
 ]),
 M('修改密码','个人信息入口；旧密码、新密码、确认密码'),
 M('成员管理弹窗','小组总览入口；邀请 / 权限 / 请离确认'),
 ]),
 ],notes:['主导航：登录 → 项目库 → 项目标题 → 板；项目内四个标签彼此切换，返回回到上一层。','任务详情和任务总览是共享页面，不因多个入口重复创建新页面类型。','图仅描述当前 UI；真实后台、AI 分析与文件上传仍以 Demo / 预留接口为主。']},
 {file:'01-profile-and-task-overview.svg',title:'个人信息与任务总览 · 视图森林',sub:'两个账号级页面 · 四种任务视图 · 固定底部个人工作量面板',root:'项目库 · 头像 / 树状图按钮；板 · 树状图按钮',rootDetail:'个人信息从头像进入；个人全任务总览从项目库或板进入。',trees:[
 P('个人信息','ProfileFragment','默认只读；编辑按钮才开启表单',[
 N('只读资料','头像、实名、账号、电话、邮箱'),
 N('编辑个人信息','点击进入可修改状态',[
 N('选择头像','系统相册选择；先作为草稿'),
 N('保存资料','校验账号 / 电话 / 邮箱；保存成功回到只读'),
 N('取消编辑','恢复保存前字段与头像'),
 ]),
 M('修改密码','旧密码 + 新密码 + 再次输入',[
 N('密码校验','检查旧密码、新密码长度、一致性、不能相同'),
 N('保存 / 取消','成功后下次登录使用新密码'),
 ]),
 N('退出登录','清除登录态 → 登录页面'),
 ]),
 P('个人全任务总览','TaskOverviewFragment','默认我的任务；跨项目查看',[
 N('共享筛选栏','我的任务 / 全部成员；全部项目 / 单个项目'),
 N('列表视图','按 DDL、项目、状态、工作量、名称排序',[
 R('→ 任务详情','任务卡展示小任务与本人完成比例'),
 ]),
 N('周课表视图','周一至周日；横向查看整周；前周 / 今天 / 后周',[
 R('→ 任务详情','每一天列出当天截止的任务'),
 ]),
 N('月历视图','前月 / 今天 / 后月；日期格显示任务数',[
 N('日期筛选','点日期列出当天任务；再点恢复整月'),
 R('→ 任务详情','通过日历下面的任务卡片打开'),
 ]),
 N('森林视图','月份 → 大项目 → 小任务 + DDL',[
 N('折叠 / 展开','月份和项目节点均可独立折叠'),
 R('→ 任务详情','点击树上的小任务卡片'),
 ]),
 N('未设置有效截止日期','周 / 月视图下单列，避免任务遗漏'),
 ]),
 N('底部吸附 · 我的工作量','总览内固定面板；不遮盖中间滚动区域',[
 N('收起 / 展开','收起显示本人任务数、完成比例'),
 N('完成情况图','细则实现与期望比例；四种状态的工作量',[
 N('状态分布','待开始 / 进行中 / 待确认 / 已完成'),
 ]),
 N('项目分布图','按大项目展示个人承担工作量占比'),
 N('截止分布图','按日期汇总本人尚未完成的工作量'),
 N('筛选联动','跟随项目和周 / 月范围；切全部成员仍显示本人'),
 N('进度过渡动画','首次 0 → 目标；切换从当前值 → 新值',[
 N('快速连续切换','沿实际已显示进度接续；页面切换淡入'),
 ]),
 N('统计口径','任务分值 × 本人分摊；细则实现 ≠ 已验收贡献'),
 ]),
 ],notes:['月历点选某一天只筛选任务列表，浮窗仍显示该月汇总；列表 / 森林不限定周月范围。','加载中、失败重试、空数据均有页面提示；浮窗中的长图表可以独立滚动。','所有视图共享 TaskOverviewFragment；它们是页内模式，并非四个独立 Fragment。']},
 {file:'02-group-overview-and-permissions.svg',title:'项目主页、小组管理与项目设置',sub:'页面层级 + 权限规则 · 保留当前代码中的新旧两个成员管理入口',root:'项目库 → 深蓝项目标题 → 项目主页',rootDetail:'项目外框保持浅蓝灰；只有项目名称、组名、状态所在的标题小卡片使用深蓝。',trees:[
 P('板 · 项目主页','BoardFragment','项目底部四标签中的“板”',[
 N('项目抬头','回项目库、课程、组名、项目名、计划版本'),
 N('阶段计数 / 筛选','待开始 / 进行中 / 待确认 / 已完成'),
 N('成员概览','姓名与当前贡献占比'),
 N('待你确认','点开第一条待确认的任务详情'),
 N('项目任务列表','按阶段分组；圆角卡片之间留白',[
 N('收起 / 展开项目任务','折叠整个项目的任务列表'),
 R('→ 任务详情','点击任意工作项'),
 R('→ 新建 / 编辑工作项','+ 新建入口；已有项编辑从详情进入'),
 ]),
 R('→ 个人全任务总览','个人跨项目视图与工作量图表'),
 R('→ 项目设置','项目标题下面的设置入口'),
 R('→ 小组总览','右上角组名；其他项目标签页亦有此入口'),
 ]),
 P('小组总览','GroupOverviewFragment','组名、项目、人数、细则实现比例与成员卡片',[
 N('成员卡片','姓名、角色、参与任务数、已确认积分'),
 M('邀请成员','组长 / 管理员可操作',[
 N('账号 / 邮箱输入','校验重复成员和重复邀请'),
 N('待加入邀请列表','演示只在本机记录，尚未实际发送'),
 ]),
 M('权限管理','仅组长可调整其他人的管理员 / 成员身份'),
 M('请离确认','组长可请离其他成员；管理员仅能请离普通成员',[
 N('未完成任务拦截','先转交负责人及分摊，再请离'),
 N('组长 / 本人保护','不能在此请离自己或组长'),
 N('历史记录保留','被请离者转存历史成员，保留成果归属'),
 ]),
 N('普通成员视角','查看成员与进度；无管理按钮'),
 N('加载失败','提示错误并允许重试'),
 ]),
 P('项目设置 · 现有入口','ProjectSettingsFragment','与新“小组总览”目前并存',[
 N('邀请码','发起人可查看、复制、重新生成'),
 N('加入审批开关','发起人设置是否需要确认'),
 N('成员列表','发起人可直接使用旧“移除”操作'),
 R('→ 切换项目','回到项目库'),
 R('→ 退出项目','退出后回到项目库'),
 N('当前实现差异','旧移除调用 Demo.removeMember；新请离调用 UiRepository'),
 N('接口边界','新邀请 / 权限 / 请离使用项目 ID；后端需独立鉴权'),
 ]),
 ],notes:['图记录当前实现：项目设置中的旧成员移除入口，未与新小组权限管理合并。','新小组总览的操作由 DemoUiRepository 校验角色；历史数据保留逻辑属于新请离流程。','项目库也可直接点击小任务进入详情，无须先打开板。']},
 {file:'03-task-material-and-analysis.svg',title:'任务细则、提交解析、验收与结算',sub:'细则实现、解析判断、成员验收是不同口径 · 三者在 UI 中分别呈现',root:'任务详情 · 多入口复用的协作中心',rootDetail:'从项目库小任务、板、收件箱、任务总览或结算待办进入；材料与结算分支另有项目底部标签入口。',trees:[
 P('任务详情','ItemDetailFragment','编号、标题、状态、负责人、分值、分摊',[
 N('三层完成比例','整个大项目 / 整个小任务 / 我的负责部分'),
 N('任务细则','目标数量、实际数量、单位、责任人',[
 N('我的任务细则','本人负责条目；可展开 / 折叠'),
 N('整个任务细则','包含所有成员条目；可展开 / 折叠'),
 ]),
 N('验收与依据','要求对照、成果证据、分摊、贡献算式、改动记录'),
 P('新建 / 编辑工作项','ItemEditFragment','名称、负责人、分值、分摊、阶段、DDL、说明',[
 M('删除工作项确认','已锁定 / 无权限时限制相应编辑操作'),
 ]),
 P('证据定位','EvidenceFragment','详情中的证据入口 → 静态原文高亮页'),
 P('修正申请','CorrectionFragment','分摊 / 完成度 / 要求对照；提交后标记未决'),
 R('→ 提交成果 / 完成确认','主操作随任务状态和当前成员身份变化'),
 ]),
 P('提交成果','SubmitFragment','文件 / 链接 / 文字；可附备注',[
 N('选择提交形式','演示文件列表、外部链接、文字说明'),
 N('解析中','读取材料 → 对照细则 → 生成比例图表',[
 N('双色进度横条','深色已处理 / 浅色剩余；禁用重复提交'),
 N('取消解析','取消请求，恢复输入与提交按钮'),
 N('失败重试','保留输入，允许重新解析'),
 ]),
 N('解析结果','明确标注“演示解析 · 待成员验收”',[
 N('达标 / 未达标明细','本人细则与全任务细则；数量对比与折叠'),
 N('三层进度与期望','大项目 / 小任务 / 本人部分；灰线表示期望'),
 R('→ 返回任务详情','提交状态改为待确认；当前验收分数不变'),
 ]),
 R('→ 总览个人工作量面板','再次进入时读取更新后的解析数据'),
 ]),
 N('项目材料与结算','项目底部标签的两个独立分支',[
 P('材料','MaterialsFragment','已解析材料与解析失败材料',[
 P('要求对照','RequirementFragment','材料要求条目、来源页码和原文引用'),
 N('失败材料行','展开失败说明、重试；演示重试仍失败'),
 N('上传材料按钮','当前仅提示“上传未接入”'),
 ]),
 P('完成确认','ConfirmFragment','任务详情 → 非提交人确认',[
 N('选择验收比例','0% / 50% / 100%；预览积分及待确认数变化'),
 N('确认后生效','更新任务状态、完成度、确认人和确认时间'),
 ]),
 P('结算单','ReportFragment','总分、成员占比、待确认入口、计分规则',[
 P('成员明细','ScoreDetailFragment','已计入 / 尚未计入工作项与逐条公式'),
 ]),
 P('计划确认','PlanFragment','收件箱入口；新旧计划差异与确认'),
 ]),
 ],notes:['计分公式：任务基础分 × 已验收比例 × 成员分摊；解析达标不会自动计入验收贡献。','“材料”标签的课程材料上传尚未接入；“提交成果”的演示解析进度与图表已经实现。','要求对照页展示来源引用；当前未设置跳转到证据定位的点击事件，图中不虚构此跳转。']},
];
const esc=s=>String(s).replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;');
const widthOf=(s,size)=>[...s].reduce((v,c)=>v+size*(c.codePointAt(0)>255?1:0.57),0);
function wrap(s,width,size){const lines=[];let line='';for(const c of s){if(c==='\n'||widthOf(line+c,size)>width){lines.push(line);line=c==='\n'?'':c;}else line+=c;}if(line)lines.push(line);return lines;}
const W=2040,margin=60,laneW=620,laneGap=30,indent=25,gap=12;
const kindStyle={page:['#315d93','#ffffff','页面'],part:['#607283','#f5f7fa','区域'],modal:['#b4472e','#fff3ed','弹窗'],ref:['#177e80','#eef8f5','跳转']};
function render(map){
 const boxes=[],edges=[],lanes=[];const titles=new Set();
 function place(node,lane,depth,y,parent){
  const x=margin+lane*(laneW+laneGap)+16+depth*indent,w=laneW-32-depth*indent;
  const title=wrap(node.title,w-83,20),details=wrap(node.detail,w-32,15),sources=node.source?wrap(node.source,w-32,12):[];
  const h=18+title.length*27+(details.length?8+details.length*23:0)+(sources.length?8+sources.length*18:0)+16;
  const box={...node,x,y,w,h,title,details,sources};boxes.push(box);if(node.source)titles.add(node.source);
  if(parent)edges.push({x1:parent.x+11,y1:parent.y+parent.h,x2:x,y2:y+22});
  let next=y+h+gap;for(const child of node.children)next=place(child,lane,depth+1,next,box);return next;
 }
 const ends=map.trees.map((tree,i)=>{const end=place(tree,i,0,300,null);lanes.push({x:margin+i*(laneW+laneGap),end});return end;});
 const floor=Math.max(...ends)+25,H=floor+210;
 const texts=(lines,x,y,size,color,weight=400,step=23)=>lines.map((t,i)=>`<text x="${x}" y="${y+i*step}" font-size="${size}" fill="${color}" font-weight="${weight}">${esc(t)}</text>`).join('');
 const cards=boxes.map(b=>{const [stroke,fill,badge]=kindStyle[b.kind];const dark=b.source&&b.title[0].includes('项目库');return `<g id="${esc(b.source||'node-'+boxes.indexOf(b))}" aria-label="${esc(b.title.join('')+'：'+b.detail)}"><title>${esc(b.source||b.detail)}</title><rect x="${b.x}" y="${b.y}" width="${b.w}" height="${b.h}" rx="12" fill="${dark?'#315d93':fill}" stroke="${stroke}" stroke-width="1.4" ${b.kind==='ref'?'stroke-dasharray="5 4"':''}/><rect x="${b.x+12}" y="${b.y+15}" width="3" height="23" rx="1.5" fill="${dark?'#ffffff':stroke}"/>${texts(b.title,b.x+24,b.y+34,20,dark?'#fff':'#10283e',600,27)}<text x="${b.x+b.w-14}" y="${b.y+32}" text-anchor="end" font-size="11" fill="${dark?'#fff':stroke}">${badge}</text>${texts(b.details,b.x+16,b.y+18+b.title.length*27+20,15,dark?'#e0e8f2':'#526578',400,23)}${texts(b.sources,b.x+16,b.y+b.h-16,12,dark?'#e0e8f2':'#748494',400,18)}</g>`;}).join('\n');
 const connectors=edges.map(e=>`<path d="M${e.x1},${e.y1} V${e.y2} H${e.x2}"/>`).join('');
 const footer=map.notes.map((t,i)=>texts(wrap('• '+t,W-2*margin,16),margin,floor+58+i*32,16,'#526578')).join('');
 const links=maps.map((m,i)=>`<a href="${m.file}"><text x="${margin+i*465}" y="${H-28}" fill="#315d93" font-size="15">${i===0?'00 总图':i===1?'01 个人信息与任务总览':i===2?'02 小组与项目管理':'03 任务、材料与分析'} ↗</text></a>`).join('');
 const svg=`<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-labelledby="map-title map-desc"><title id="map-title">${esc(map.title)}</title><desc id="map-desc">${esc(map.sub)}</desc><style>text{font-family:'Microsoft YaHei','Noto Sans CJK SC',sans-serif}a:hover text{text-decoration:underline}</style><rect width="100%" height="100%" fill="#f1f3f5"/>
${texts([map.title],margin,70,34,'#10283e',700)}${texts([map.sub],margin,110,17,'#526578')}
<text x="${margin}" y="147" font-size="14" fill="#607283">图例：蓝边 = 独立页面　灰边 = 页内区域 / 模式　赭边 = 弹窗　绿虚框 = 跳转或复用页　树线 = 层级包含关系</text>
<rect x="${margin}" y="176" width="${W-margin*2}" height="82" rx="15" fill="#315d93"/>
${texts([map.root],margin+24,209,22,'#fff',600)}${texts([map.rootDetail],margin+24,237,15,'#e4eaf2')}
${lanes.map(l=>`<path d="M${l.x+laneW/2},258 V300" stroke="#a8b8c8" stroke-width="2"/><rect x="${l.x}" y="284" width="${laneW}" height="${l.end-284+4}" rx="18" fill="#e4eaf2"/>`).join('')}
<g fill="none" stroke="#a8b8c8" stroke-width="1.6">${connectors}</g>${cards}
<path d="M${margin},${floor} H${W-margin}" stroke="#c9d4df"/>
${texts(['当前实现说明'],margin,floor+26,17,'#10283e',600)}${footer}${links}</svg>`;
 return {svg,H,pages:[...titles],boxes};
}
await mkdir(dir,{recursive:true});
const manifests=[];
for(const map of maps){const out=render(map);await writeFile(resolve(dir,map.file),out.svg);manifests.push({file:map.file,width:W,height:out.H,pages:out.pages,nodes:out.boxes.length});}
const screenDir=resolve(dir,'../../app/src/main/java/com/example/mycoursepractice/screens');
const screenFiles=(await readdir(screenDir)).filter(f=>f.endsWith('Fragment.kt')).map(f=>f.replace('.kt',''));
const included=new Set(manifests[0].pages);const missing=screenFiles.filter(f=>!included.has(f));
if(missing.length)throw Error('Missing pages in overall map: '+missing.join(','));
await writeFile(resolve(dir,'manifest.json'),JSON.stringify({source:'app/src/main/java/com/example/mycoursepractice',screenCount:screenFiles.length,missing,diagrams:manifests},null,2));
await writeFile(resolve(dir,'README.md'),`# GroupPilot 当前 UI 层级图\n\n基于 2026-11-18 本地代码（包含未提交修改），共覆盖 ${screenFiles.length} 个 Fragment 页面。\n\n- [00 总图](00-app-navigation.svg)\n- [01 个人信息与任务总览](01-profile-and-task-overview.svg)\n- [02 小组与项目管理](02-group-overview-and-permissions.svg)\n- [03 任务、材料与分析](03-task-material-and-analysis.svg)\n\nSVG 内的文字、卡片、连线均为矢量元素，可在浏览器、Figma、Inkscape 中打开。图底部链接可在各图间跳转。\n\n树线表达包含层级；带“→”的节点表示跳转或复用。灰色框属于同一页面内的区域/状态，不代表单独页面。\n\n说明：项目设置的旧成员移除与新小组总览入口目前并存；材料标签上传仍未接入；提交成果使用演示解析。这些在图中按实际代码标注。\n\n重新生成：node docs/ui-map/generate.mjs\n\nmanifest.json 记录各图尺寸、节点数量和页面覆盖。\n`);
await writeFile(resolve(dir,'index.html'),`<!doctype html><html lang="zh-CN"><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>GroupPilot UI 层级图</title><style>body{margin:0;background:#f1f3f5;font:16px 'Microsoft YaHei',sans-serif;color:#10283e}header{padding:24px;position:sticky;top:0;background:white}nav{display:flex;gap:20px;flex-wrap:wrap}a{color:#315d93}section{padding:24px}img{width:100%;height:auto;border-radius:16px}h2{font-size:20px}</style><header><nav>${maps.map((m,i)=>`<a href="#map${i}">${m.title}</a>`).join('')}</nav></header>${maps.map((m,i)=>`<section id="map${i}"><h2><a href="${m.file}" target="_blank">${m.title} ↗ 原尺寸 SVG</a></h2><img src="${m.file}" alt="${m.title}"></section>`).join('')}</html>`);
console.log(JSON.stringify(manifests));
