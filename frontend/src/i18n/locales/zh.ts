export default {
  // 语言名称
  lang: {
    name: '中文',
    flag: '🇨🇳'
  },

  // 语言切换器
  languageSwitcher: {
    selectLanguage: '选择语言',
    currentLanguage: '当前语言'
  },

  // 通用
  common: {
    and: '和',
    search: '搜索',
    searchPlaceholder: '搜索...',
    loading: '加载中...',
    save: '保存',
    cancel: '取消',
    delete: '删除',
    edit: '编辑',
    create: '创建',
    update: '更新',
    confirm: '确认',
    close: '关闭',
    back: '返回',
    next: '下一步',
    submit: '提交',
    reset: '重置',
    clear: '清除',
    filter: '筛选',
    export: '导出',
    import: '导入',
    view: '查看',
    actions: '操作',
    status: '状态',
    priority: '优先级',
    date: '日期',
    name: '名称',
    description: '描述',
    owner: '负责人',
    members: '成员',
    tags: '标签',
    progress: '进度',
    startDate: '开始日期',
    endDate: '结束日期',
    createdAt: '创建时间',
    updatedAt: '更新时间',
    empty: '暂无数据',
    total: '总计',
    selectAll: '全选',
    deselectAll: '取消全选'
  },

  // 路由/页面标题
  routes: {
    login: '登录',
    dashboard: '仪表盘',
    simpleDashboard: '仪表盘（简化版）',
    projectList: '项目列表',
    projectNew: '创建项目',
    projectDetail: '项目详情',
    taskBoard: '任务看板',
    ganttView: '甘特图',
    team: '团队成员',
    reports: '报表统计',
    settings: '设置',
    test: '测试页面'
  },

  // 应用名称
  app: {
    name: 'WBS项目管理系统',
    shortName: 'WBS系统'
  },

  // 登录页
  login: {
    title: '项目管理系统',
    subtitle: '欢迎回来',
    userId: '用户ID',
    userIdPlaceholder: '请输入用户ID',
    password: '密码',
    passwordPlaceholder: '请输入密码',
    rememberMe: '记住我',
    loginButton: '登录',
    loggingIn: '登录中...',
    footerText: '登录即表示您同意我们的',
    termsOfService: '服务条款',
    privacyPolicy: '隐私政策',
    testAccount: '测试账号',
    validation: {
      userIdRequired: '请输入用户ID',
      passwordRequired: '请输入密码'
    },
    error: {
      loginFailed: '登录失败，请检查用户ID和密码'
    }
  },

  // 仪表盘
  dashboard: {
    welcome: '欢迎回来',
    stats: {
      totalProjects: '总项目数',
      activeProjects: '进行中的项目数',
      totalTasks: '总任务数',
      totalMembers: '团队成员'
    },
    charts: {
      taskStatusDistribution: '任务状态分布',
      projectProgressOverview: '项目进度概览',
      noTaskData: '暂无任务数据',
      noProjectData: '暂无项目数据'
    },
    recentProjects: {
      title: '最近项目',
      viewAll: '查看全部',
      projectName: '项目名称',
      status: '状态',
      progress: '进度',
      deadline: '截止日期'
    },
    upcomingTasks: {
      title: '即将到期的任务',
      startTime: '开始时间',
      endTime: '结束时间',
      assignee: '负责人',
      remainingTime: '剩余时间',
      unassigned: '未分配',
      daysRemaining: {
        overdue: '已逾期 {days} 天',
        today: '今天到期',
        tomorrow: '明天到期',
        days: '剩余 {days} 天'
      }
    }
  },

  // 项目列表
  projectList: {
    title: '项目列表',
    subtitle: '管理和查看所有项目',
    newProject: '新建项目',
    searchPlaceholder: '搜索项目...',
    status: '状态',
    clearFilters: '清除筛选',
    emptyState: {
      title: '暂无项目',
      noResults: '没有找到匹配的项目',
      createFirst: '开始创建您的第一个项目吧',
      createButton: '创建项目'
    },
    statuses: {
      all: '全部',
      planning: '计划中',
      active: '进行中',
      completed: '已完成',
      onHold: '已暂停',
      cancelled: '废弃'
    }
  },

  // 项目详情
  projectDetail: {
    title: '项目详情',
    editProject: '编辑',
    deleteProject: '删除',
    information: '项目信息',
    overview: '项目概览',
    tasks: '任务',
    settings: '项目设置',
    members: '团队成员',
    projectOwner: '项目负责人',
    activity: '活动记录',
    status: '状态',
    priority: '优先级',
    progress: '进度',
    estimatedHours: '预估工时',
    hours: '小时',
    people: '人',
    projectProgress: '项目进度',
    startDate: '开始日期',
    endDate: '结束日期',
    tags: '标签',
    quickActions: '快速操作',
    viewTasks: '查看任务',
    tasksCount: '个任务',
    ganttChart: '甘特图',
    timelineView: '时间线视图',
    newTask: '新建任务',
    addNewTask: '添加新任务',
    emptyState: {
      title: '项目不存在',
      message: '请检查项目ID是否正确',
      backToList: '返回项目列表'
    },
    deleteConfirm: '确定要删除这个项目吗？'
  },

  // 项目表单
  projectForm: {
    title: '项目信息',
    name: '项目名称',
    namePlaceholder: '请输入项目名称',
    description: '项目描述',
    descriptionPlaceholder: '请输入项目描述',
    startDate: '开始日期',
    endDate: '结束日期',
    color: '项目颜色',
    status: '项目状态',
    priority: '优先级',
    owner: '项目负责人',
    tags: '项目标签',
    estimatedHours: '预估工时',
    createButton: '创建项目',
    updateButton: '更新项目',
    validation: {
      nameRequired: '项目名称不能为空',
      startDateRequired: '开始日期不能为空',
      endDateRequired: '结束日期不能为空',
      endDateInvalid: '结束日期必须晚于开始日期'
    }
  },

  // 任务看板
  taskBoard: {
    title: '任务看板',
    newTask: '新建任务',
    addTask: '添加任务',
    searchPlaceholder: '搜索任务...',
    groupBy: '分组方式',
    filterBy: '筛选条件',
    clearFilters: '清除筛选',
    delayFilter: '延期筛选',
    showDelayedOnly: '仅显示延期任务',
    delayOptions: {
      all: '全部',
      delayed: '已延期',
      critical: '严重延期（≥7天）',
      warning: '中度延期（3-6天）'
    },
    columns: {
      todo: '待办',
      inProgress: '进行中',
      done: '已完成'
    },
    taskDetail: '任务详情',
    emptyState: {
      title: '暂无任务',
      message: '此列表中没有任务'
    },
    dragHint: '拖拽任务卡片以更改状态',
    messages: {
      deleteFailed: '删除任务失败: '
    }
  },

  // 任务详情
  taskDetail: {
    title: '任务详情',
    editTask: '编辑',
    deleteTask: '删除',
    information: '任务信息',
    description: '描述',
    attachments: '附件',
    comments: '评论',
    subtasks: '子任务',
    dependencies: '依赖关系',
    activity: '活动记录',
    // 状态相关
    statusAutoCalculated: '状态由子任务自动确定',
    // 日期相关
    startDateAutoCalculated: '开始时间由子任务自动计算（最早子任务的开始时间）',
    endDateAutoCalculated: '结束时间由子任务自动计算（最晚子任务的结束时间）',
    // 进度相关
    progressAutoCalculated: '此任务有子任务，进度由子任务自动汇总计算',
    progressFixedWhenDone: '已完成状态的任务进度固定为100%',
    // 工时相关
    estimatedHours: '预估工时',
    actualHours: '实际工时',
    // 子任务相关
    addSubtask: '添加子任务',
    noSubtasks: '暂无子任务，点击上方按钮添加',
    // 附件相关
    download: '下载',
    // 确认操作
    deleteConfirm: '确定要删除此任务吗？',
    // 日期格式
    dateFormat: 'YYYY年MM月DD日',
    dateTimeFormat: 'YYYY-MM-DD HH:mm'
  },

  // 任务表单
  taskForm: {
    title: '任务信息',
    name: '任务名称',
    namePlaceholder: '请输入任务名称',
    description: '任务描述',
    descriptionPlaceholder: '请输入任务描述',
    startDate: '开始日期',
    endDate: '结束日期',
    status: '任务状态',
    priority: '优先级',
    assignee: '负责人',
    project: '所属项目',
    parentTask: '父任务',
    estimatedHours: '预估工时',
    actualHours: '实际工时',
    progress: '完成进度',
    createButton: '创建任务',
    updateButton: '更新任务',
    validation: {
      nameRequired: '任务名称不能为空',
      projectRequired: '请选择所属项目'
    }
  },

  // 任务模态框
  taskModal: {
    createTitle: '新建任务',
    editTitle: '编辑任务',
    // 表单字段
    titleLabel: '任务标题',
    titlePlaceholder: '请输入任务标题',
    parentTaskLabel: '父任务',
    descriptionLabel: '描述',
    descriptionPlaceholder: '请输入任务描述',
    statusLabel: '状态',
    priorityLabel: '优先级',
    startDateLabel: '开始日期',
    endDateLabel: '结束日期',
    assigneeLabel: '负责人',
    assigneePlaceholder: '请选择负责人',
    estimatedHoursLabel: '预估工时',
    estimatedHoursHint: '（根据工作日自动计算）',
    estimatedHoursDisabledHint: '此字段根据任务的开始日期和结束日期自动计算工作日（每天8小时），无法手动编辑',
    progressLabel: '进度',
    tagsLabel: '标签',
    // 提示信息
    statusAutoCalculated: '此任务有子任务，状态由子任务自动确定',
    startDateAutoCalculated: '此任务有子任务，开始时间将由子任务自动计算，无法手动修改',
    endDateAutoCalculated: '此任务有子任务，结束时间将由子任务自动计算，无法手动修改',
    progressAutoCalculated: '此任务有子任务，进度将由子任务自动计算，无法手动修改',
    progressFixedWhenTodo: '待办状态的任务进度固定为0%，无法修改',
    progressFixedWhenDone: '已完成状态的任务进度固定为100%，无法修改',
    hoursLabel: '小时',
    // 按钮
    cancelButton: '取消',
    saveButton: '保存',
    createButton: '创建',
    // 验证错误消息
    validation: {
      titleRequired: '请输入任务标题',
      statusRequired: '请选择任务状态',
      priorityRequired: '请选择优先级',
      startDateRequired: '请选择开始日期',
      endDateRequired: '请选择结束日期',
      endDateInvalid: '结束日期不能早于开始日期',
      assigneeRequired: '请选择负责人',
      estimatedHoursInvalid: '请输入有效的预估工时'
    },
    // 警告消息
    warnings: {
      projectIdRequired: '项目ID不能为空'
    },
    // 常用标签
    commonTags: ['前端', '后端', '设计', '数据库', 'API', '测试', '文档', '优化']
  },

  // 甘特图
  gantt: {
    title: '甘特图',
    viewMode: '视图模式',
    day: '日',
    week: '周',
    month: '月',
    quarter: '季度',
    dayView: '日视图',
    weekView: '周视图',
    monthView: '月视图',
    zoomIn: '放大',
    zoomOut: '缩小',
    fitToScreen: '适应屏幕',
    export: '导出图表',
    // 状态图例
    status: '状态',
    statusTodo: '待办',
    statusInProgress: '进行中',
    statusDone: '已完成',
    // 列名
    taskName: '任务名称',
    startDate: '开始时间',
    duration: '工期',
    // 日期格式
    yearFormat: '%Y年',
    monthFormat: '%m月',
    yearMonthFormat: '%Y年 %m月',
    weekFormat: '第%W周',
    dayFormat: '%d',
    // 工具提示
    tooltip: {
      assignee: '负责人',
      status: '状态',
      priority: '优先级',
      startTime: '开始',
      endTime: '结束',
      duration: '工期',
      days: '天',
      estimated: '预估',
      actual: '实际',
      progress: '进度'
    },
    // dhtmlx gantt locale 配置
    locale: {
      newTask: '新任务',
      todayButton: '今天',
      confirmDeleting: '确定删除任务?',
      description: '描述',
      timePeriod: '时间周期',
      type: '类型'
    },
    // 日期相关
    months: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'],
    days: ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'],
    daysShort: ['日', '一', '二', '三', '四', '五', '六']
  },

  // 团队成员
  team: {
    title: '团队成员',
    subtitle: '管理项目团队成员和权限',
    addMember: '添加成员',
    searchPlaceholder: '搜索成员...',
    name: '姓名',
    email: '邮箱',
    role: '角色',
    department: '部门',
    skills: '技能',
    joinedAt: '加入时间',
    actions: '操作',
    remove: '移除',
    edit: '编辑',
    delete: '删除',
    allMembers: '所有成员',
    workloadDistribution: '工作负载分布',
    taskCount: '任务数',
    editMember: '编辑成员',
    addNewMember: '添加新成员',
    form: {
      nameLabel: '姓名',
      namePlaceholder: '请输入成员姓名',
      emailLabel: '邮箱',
      emailPlaceholder: 'example@company.com',
      roleLabel: '角色',
      rolePlaceholder: '请选择角色',
      departmentLabel: '部门',
      departmentPlaceholder: '例如：研发部',
      skillsLabel: '技能',
      skillsPlaceholder: '用逗号分隔，例如：Vue, TypeScript, Node.js',
      skillsHint: '多个技能请用逗号分隔'
    },
    buttons: {
      saveChanges: '保存修改',
      addMember: '添加成员'
    },
    messages: {
      requiredFields: '请填写所有必填项！',
      updateSuccess: '成员信息更新成功！',
      createSuccess: '新成员添加成功！',
      deleteConfirm: '确定要删除成员 "{name}" 吗？此操作不可恢复。',
      deleteSuccess: '成员删除成功！',
      operationFailed: '操作失败，请稍后重试。',
      deleteFailed: '删除失败，请稍后重试。'
    },
    emptyState: {
      title: '暂无成员',
      message: '还没有团队成员'
    },
    taskAssignment: {
      title: '任务分配情况',
      memberName: '成员姓名',
      taskName: '任务名称',
      projectName: '项目名称',
      status: '状态',
      priority: '优先级',
      progress: '进度',
      noData: '暂无任务分配数据',
      previous: '上一页',
      next: '下一页',
      page: '第 {current} 页 / 共 {total} 页',
      itemsPerPage: '每页条数',
      sort: '排序'
    }
  },

  // 报表统计
  reports: {
    title: '报表统计',
    subtitle: '查看项目数据和统计信息',
    projectReport: '项目报表',
    taskReport: '任务报表',
    memberReport: '成员报表',
    exportReport: '导出报表',
    dateRange: '日期范围',
    generate: '生成报表',
    stats: {
      totalProjects: '总项目数',
      completionRate: '完成率',
      totalTasks: '总任务数',
      inProgressTasks: '进行中的任务数',
      delayedTasks: '延期任务数',
      totalDelayedDays: '累计延期天数',
      delayRate: '延期率',
      criticalDelays: '严重延期（≥7天）'
    },
    charts: {
      projectStatusDistribution: '项目状态分布',
      taskPriorityDistribution: '任务优先级分布',
      projectCompletionProgress: '项目完成进度',
      teamPerformance: '团队绩效',
      projectStatus: '项目状态',
      taskPriority: '任务优先级',
      progress: '进度',
      completed: '已完成',
      inProgress: '进行中',
      taskCount: '任务数'
    },
    export: {
      title: '导出报表',
      projectExcel: '项目 Excel',
      projectExcelDesc: '导出项目列表',
      statisticsExcel: '统计 Excel',
      statisticsExcelDesc: '导出统计数据及各项目甘特图',
      comprehensiveReport: '综合报表',
      comprehensiveReportDesc: '包含所有数据'
    },
    statuses: {
      planning: '计划中',
      active: '进行中',
      completed: '已完成',
      onHold: '已暂停'
    },
    priorities: {
      low: '低',
      medium: '中',
      high: '高',
      urgent: '紧急'
    },
    messages: {
      noProjectData: '暂无项目数据可导出！',
      exportSuccess: '导出成功！',
      exportFailed: '导出失败，请稍后重试！',
      noData: '暂无数据可导出！'
    }
  },

  // 设置
  settings: {
    title: '设置',
    subtitle: '管理系统配置和偏好设置',
    navigation: {
      profile: '个人资料',
      notifications: '通知设置',
      display: '显示设置',
      security: '安全设置',
      account: '账号设置',
      preferences: '偏好设置'
    },
    // 个人资料
    profile: {
      title: '个人资料',
      changeAvatar: '更换头像',
      avatarHint: '支持 JPG、PNG 格式，最大 2MB',
      name: '姓名',
      email: '邮箱',
      department: '部门',
      role: '角色',
      saveChanges: '保存更改'
    },
    // 通知设置
    notifications: {
      title: '通知设置',
      emailNotifications: '邮件通知',
      emailNotificationsDesc: '接收项目更新和任务分配的邮件通知',
      browserNotifications: '浏览器通知',
      browserNotificationsDesc: '在浏览器中显示桌面通知',
      taskReminders: '任务提醒',
      taskRemindersDesc: '任务截止日期前的提醒通知',
      saveChanges: '保存更改'
    },
    // 显示设置
    display: {
      title: '显示设置',
      theme: '主题',
      language: '语言',
      dateFormat: '日期格式',
      saveChanges: '保存更改',
      themes: {
        light: '浅色',
        dark: '深色',
        auto: '自动'
      }
    },
    // 安全设置
    security: {
      title: '安全设置',
      changePassword: '修改密码',
      changePasswordDesc: '定期修改密码可以保护您的账户安全',
      currentPassword: '当前密码',
      newPassword: '新密码',
      confirmPassword: '确认新密码',
      updatePassword: '更新密码',
      twoFactor: '两步验证',
      twoFactorDesc: '添加额外的安全层来保护您的账户',
      enableTwoFactor: '启用两步验证'
    },
    // 通用
    logout: '退出系统'
  },

  // 头部导航
  header: {
    searchPlaceholder: '搜索...',
    notifications: '通知',
    userMenu: {
      profile: '个人中心',
      settings: '账号设置',
      logout: '退出系统'
    }
  },

  // 侧边栏
  sidebar: {
    toggle: '收起',
    expand: '展开'
  },

  // 用户角色
  roles: {
    admin: '管理员',
    projectManager: '项目经理',
    member: '成员',
    viewer: '观察者'
  },

  // 优先级
  priorities: {
    low: '低',
    medium: '中',
    high: '高',
    urgent: '紧急',
    critical: '非常紧急'
  },

  // 任务状态
  taskStatus: {
    todo: '待办',
    inProgress: '进行中',
    done: '已完成'
  },

  // 项目状态
  projectStatus: {
    planning: '计划中',
    active: '进行中',
    completed: '已完成',
    onHold: '已暂停',
    cancelled: '已废弃'
  },

  // 通知消息
  messages: {
    success: {
      create: '创建成功',
      update: '更新成功',
      delete: '删除成功',
      save: '保存成功'
    },
    error: {
      create: '创建失败',
      update: '更新失败',
      delete: '删除失败',
      save: '保存失败',
      network: '网络错误，请稍后重试',
      unauthorized: '未授权，请先登录',
      forbidden: '没有权限执行此操作',
      notFound: '资源不存在'
    },
    confirm: {
      delete: '确定要删除吗？此操作不可撤销。',
      leave: '您有未保存的更改，确定要离开吗？',
      logout: '确定要退出登录吗？'
    }
  },

  // 时间
  time: {
    today: '今天',
    yesterday: '昨天',
    tomorrow: '明天',
    thisWeek: '本周',
    nextWeek: '下周',
    lastWeek: '上周',
    thisMonth: '本月',
    nextMonth: '下月',
    lastMonth: '上月'
  }
};
