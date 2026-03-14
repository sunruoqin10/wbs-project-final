export default {
  // 언어 이름
  lang: {
    name: '한국어',
    flag: '🇰🇷'
  },

  // 언어 전환기
  languageSwitcher: {
    selectLanguage: '언어 선택',
    currentLanguage: '현재 언어'
  },

  // 공통
  common: {
    and: ' 및',
    search: '검색',
    searchPlaceholder: '검색...',
    loading: '로딩 중...',
    save: '저장',
    cancel: '취소',
    delete: '삭제',
    edit: '편집',
    create: '생성',
    update: '업데이트',
    confirm: '확인',
    close: '닫기',
    back: '뒤로',
    next: '다음',
    submit: '제출',
    reset: '재설정',
    clear: '지우기',
    filter: '필터',
    export: '내보내기',
    import: '가져오기',
    view: '보기',
    actions: '작업',
    status: '상태',
    priority: '우선순위',
    date: '날짜',
    name: '이름',
    description: '설명',
    owner: '담당자',
    members: '구성원',
    tags: '태그',
    progress: '진행률',
    startDate: '시작일',
    endDate: '종료일',
    createdAt: '생성일',
    updatedAt: '수정일',
    empty: '데이터 없음',
    total: '합계',
    selectAll: '모두 선택',
    deselectAll: '선택 해제'
  },

  // 라우트/페이지 제목
  routes: {
    login: '로그인',
    dashboard: '대시보드',
    simpleDashboard: '대시보드 (간단)',
    projectList: '프로젝트 목록',
    projectNew: '프로젝트 생성',
    projectDetail: '프로젝트 상세',
    taskBoard: '작업 보드',
    ganttView: '간트 차트',
    delayStats: '프로젝트 지연 통계',
    overtime: '프로젝트 연장근무 관리',
    weeklyReports: '주간 보고서 관리',
    weeklyReportNew: '주간 보고서 생성',
    weeklyReportDetail: '주간 보고서 상세',
    weeklyReportEdit: '주간 보고서 편집',
    team: '팀 구성원',
    reports: '보고서',
    settings: '설정',
    test: '테스트 페이지'
  },

  // 애플리케이션 이름
  app: {
    name: 'WBS 프로젝트 관리 시스템',
    shortName: 'WBS 시스템'
  },

  // 로그인 페이지
  login: {
    title: '프로젝트 관리 시스템',
    subtitle: '환영합니다',
    userId: '사용자 ID',
    userIdPlaceholder: '사용자 ID를 입력하세요',
    password: '비밀번호',
    passwordPlaceholder: '비밀번호를 입력하세요',
    rememberMe: '로그인 상태 유지',
    loginButton: '로그인',
    loggingIn: '로그인 중...',
    footerText: '로그인하면 다음 사항에 동의하는 것으로 간주됩니다',
    termsOfService: '서비스 약관',
    privacyPolicy: '개인정보 처리방침',
    testAccount: '테스트 계정',
    validation: {
      userIdRequired: '사용자 ID를 입력하세요',
      passwordRequired: '비밀번호를 입력하세요'
    },
    error: {
      loginFailed: '로그인에 실패했습니다. 사용자 ID와 비밀번호를 확인해주세요'
    }
  },

  // 대시보드
  dashboard: {
    welcome: '환영합니다',
    stats: {
      totalProjects: '총 프로젝트 수',
      activeProjects: '진행 중인 프로젝트 수',
      totalTasks: '총 작업 수',
      totalMembers: '팀 구성원'
    },
    charts: {
      taskStatusDistribution: '작업 상태 분포',
      projectProgressOverview: '프로젝트 진행률 개요',
      noTaskData: '작업 데이터 없음',
      noProjectData: '프로젝트 데이터 없음'
    },
    recentProjects: {
      title: '최근 프로젝트',
      viewAll: '전체 보기',
      projectName: '프로젝트 이름',
      status: '상태',
      progress: '진행률',
      deadline: '마감일'
    },
    upcomingTasks: {
      title: '마감 임박 작업',
      startTime: '시작 시간',
      endTime: '종료 시간',
      assignee: '담당자',
      remainingTime: '남은 시간',
      unassigned: '미배정',
      daysRemaining: {
        overdue: '{days}일 지연',
        today: '오늘 마감',
        tomorrow: '내일 마감',
        days: '{days}일 남음'
      }
    }
  },

  // 프로젝트 목록
  projectList: {
    title: '프로젝트 목록',
    subtitle: '모든 프로젝트를 관리하고 조회하세요',
    newProject: '새 프로젝트',
    searchPlaceholder: '프로젝트 검색...',
    status: '상태',
    clearFilters: '필터 지우기',
    viewModes: {
      card: '카드',
      list: '목록'
    },
    listView: {
      projectName: '프로젝트명',
      status: '상태',
      progress: '진행률',
      members: '멤버',
      dateRange: '기간',
      actions: '작업'
    },
    emptyState: {
      title: '프로젝트 없음',
      noResults: '일치하는 프로젝트를 찾을 수 없습니다',
      createFirst: '첫 번째 프로젝트를 만들어 보세요',
      createButton: '프로젝트 생성'
    },
    statuses: {
      all: '전체',
      planning: '계획 중',
      active: '진행 중',
      completed: '완료',
      onHold: '보류',
      cancelled: '취소됨'
    }
  },

  // 프로젝트 상세
  projectDetail: {
    title: '프로젝트 상세',
    editProject: '편집',
    deleteProject: '삭제',
    information: '프로젝트 정보',
    overview: '프로젝트 개요',
    tasks: '작업',
    settings: '프로젝트 설정',
    members: '팀 구성원',
    projectOwner: '프로젝트 담당자',
    activity: '활동 기록',
    status: '상태',
    priority: '우선순위',
    progress: '진행률',
    estimatedHours: '예상 공수',
    hours: '시간',
    people: '명',
    projectProgress: '프로젝트 진행률',
    startDate: '시작일',
    endDate: '종료일',
    tags: '태그',
    quickActions: '빠른 작업',
    viewTasks: '작업 보기',
    tasksCount: '개 작업',
    ganttChart: '간트 차트',
    timelineView: '타임라인 보기',
    newTask: '새 작업',
    addNewTask: '새 작업 추가',
    emptyState: {
      title: '프로젝트가 존재하지 않습니다',
      message: '프로젝트 ID가 올바른지 확인하세요',
      backToList: '프로젝트 목록으로 돌아가기'
    },
    deleteConfirm: '정말 이 프로젝트를 삭제하시겠습니까?'
  },

  // 프로젝트 폼
  projectForm: {
    title: '프로젝트 정보',
    name: '프로젝트 이름',
    namePlaceholder: '프로젝트 이름을 입력하세요',
    description: '프로젝트 설명',
    descriptionPlaceholder: '프로젝트 설명을 입력하세요',
    startDate: '시작일',
    endDate: '종료일',
    color: '프로젝트 색상',
    status: '프로젝트 상태',
    priority: '우선순위',
    owner: '프로젝트 담당자',
    tags: '프로젝트 태그',
    estimatedHours: '예상 공수',
    createButton: '프로젝트 생성',
    updateButton: '프로젝트 업데이트',
    validation: {
      nameRequired: '프로젝트 이름을 입력해야 합니다',
      startDateRequired: '시작일을 입력해야 합니다',
      endDateRequired: '종료일을 입력해야 합니다',
      endDateInvalid: '종료일은 시작일보다 늦어야 합니다'
    }
  },

  // 작업 보드
  taskBoard: {
    title: '작업 보드',
    newTask: '새 작업',
    addTask: '작업 추가',
    searchPlaceholder: '작업 검색...',
    groupBy: '그룹화 기준',
    filterBy: '필터 조건',
    clearFilters: '필터 지우기',
    delayFilter: '지연 필터',
    showDelayedOnly: '지연된 작업만 표시',
    delayOptions: {
      all: '전체',
      delayed: '지연됨',
      critical: '심각한 지연（≥7일）',
      warning: '중간 지연（3-6일）'
    },
    columns: {
      todo: '할 일',
      inProgress: '진행 중',
      done: '완료'
    },
    taskDetail: '작업 상세',
    emptyState: {
      title: '작업 없음',
      message: '이 목록에 작업이 없습니다'
    },
    dragHint: '상태를 변경하려면 작업 카드를 드래그하세요',
    messages: {
      deleteFailed: '작업 삭제 실패: '
    }
  },

  // 작업 상세
  taskDetail: {
    title: '작업 상세',
    editTask: '편집',
    deleteTask: '삭제',
    information: '작업 정보',
    description: '설명',
    attachments: '첨부파일',
    comments: '댓글',
    subtasks: '하위 작업',
    dependencies: '종속성',
    activity: '활동 기록',
    // 상태 관련
    statusAutoCalculated: '상태는 하위 작업에 의해 자동으로 결정됩니다',
    // 날짜 관련
    startDateAutoCalculated: '시작 시간은 하위 작업에 의해 자동으로 계산됩니다 (가장 빠른 하위 작업의 시작 시간)',
    endDateAutoCalculated: '종료 시간은 하위 작업에 의해 자동으로 계산됩니다 (가장 늦은 하위 작업의 종료 시간)',
    // 진행률 관련
    progressAutoCalculated: '이 작업에는 하위 작업이 있으므로 진행률이 하위 작업에서 자동으로 집계됩니다',
    progressFixedWhenDone: '완료된 작업의 진행률은 100%로 고정됩니다',
    // 공수 관련
    estimatedHours: '예상 공수',
    actualHours: '실제 공수',
    // 하위 작업 관련
    addSubtask: '하위 작업 추가',
    noSubtasks: '하위 작업이 없습니다. 위 버튼을 클릭하여 추가하세요',
    // 첨부파일 관련
    download: '다운로드',
    // 확인 작업
    deleteConfirm: '정말 이 작업을 삭제하시겠습니까?',
    // 날짜 형식
    dateFormat: 'YYYY년MM월DD일',
    dateTimeFormat: 'YYYY-MM-DD HH:mm'
  },

  // 작업 폼
  taskForm: {
    title: '작업 정보',
    name: '작업 이름',
    namePlaceholder: '작업 이름을 입력하세요',
    description: '작업 설명',
    descriptionPlaceholder: '작업 설명을 입력하세요',
    startDate: '시작일',
    endDate: '종료일',
    status: '작업 상태',
    priority: '우선순위',
    assignee: '담당자',
    project: '소속 프로젝트',
    parentTask: '상위 작업',
    estimatedHours: '예상 공수',
    actualHours: '실제 공수',
    progress: '완료 진행률',
    createButton: '작업 생성',
    updateButton: '작업 업데이트',
    validation: {
      nameRequired: '작업 이름을 입력해야 합니다',
      projectRequired: '소속 프로젝트를 선택해야 합니다'
    }
  },

  // 작업 모달
  taskModal: {
    createTitle: '새 작업',
    editTitle: '작업 편집',
    // 폼 필드
    titleLabel: '작업 제목',
    titlePlaceholder: '작업 제목을 입력하세요',
    parentTaskLabel: '상위 작업',
    descriptionLabel: '설명',
    descriptionPlaceholder: '작업 설명을 입력하세요',
    statusLabel: '상태',
    priorityLabel: '우선순위',
    startDateLabel: '시작일',
    endDateLabel: '종료일',
    assigneeLabel: '담당자',
    assigneePlaceholder: '담당자를 선택하세요',
    estimatedHoursLabel: '예상 공수',
    estimatedHoursHint: '（업무일 기준 자동 계산）',
    estimatedHoursDisabledHint: '이 필드는 작업의 시작일과 종료일에 따라 업무일을 자동으로 계산하며(하루 8시간), 수동으로 편집할 수 없습니다',
    progressLabel: '진행률',
    tagsLabel: '태그',
    // 힌트 메시지
    statusAutoCalculated: '이 작업에는 하위 작업이 있으므로 상태가 하위 작업에 의해 자동으로 결정됩니다',
    startDateAutoCalculated: '이 작업에는 하위 작업이 있으므로 시작 시간이 하위 작업에서 자동으로 계산되며 수동으로 수정할 수 없습니다',
    endDateAutoCalculated: '이 작업에는 하위 작업이 있으므로 종료 시간이 하위 작업에서 자동으로 계산되며 수동으로 수정할 수 없습니다',
    progressAutoCalculated: '이 작업에는 하위 작업이 있으므로 진행률이 하위 작업에서 자동으로 계산되며 수동으로 수정할 수 없습니다',
    progressFixedWhenTodo: '할 일 상태의 작업 진행률은 0%로 고정되어 수정할 수 없습니다',
    progressFixedWhenDone: '완료된 작업의 진행률은 100%로 고정되어 수정할 수 없습니다',
    hoursLabel: '시간',
    // 버튼
    cancelButton: '취소',
    saveButton: '저장',
    createButton: '생성',
    // 유효성 검사 오류 메시지
    validation: {
      titleRequired: '작업 제목을 입력하세요',
      statusRequired: '작업 상태를 선택하세요',
      priorityRequired: '우선순위를 선택하세요',
      startDateRequired: '시작일을 선택하세요',
      endDateRequired: '종료일을 선택하세요',
      endDateInvalid: '종료일은 시작일보다 빠를 수 없습니다',
      assigneeRequired: '담당자를 선택하세요',
      estimatedHoursInvalid: '유효한 예상 공수를 입력하세요'
    },
    // 경고 메시지
    warnings: {
      projectIdRequired: '프로젝트 ID가 비어 있을 수 없습니다'
    },
    // 일반 태그
    commonTags: ['프론트엔드', '백엔드', '디자인', '데이터베이스', 'API', '테스트', '문서', '최적화']
  },

  // 간트 차트
  gantt: {
    title: '간트 차트',
    viewMode: '보기 모드',
    day: '일',
    week: '주',
    month: '월',
    quarter: '분기',
    dayView: '일 보기',
    weekView: '주 보기',
    monthView: '월 보기',
    zoomIn: '확대',
    zoomOut: '축소',
    fitToScreen: '화면에 맞추기',
    export: '차트 내보내기',
    // 상태 범례
    status: '상태',
    statusTodo: '할 일',
    statusInProgress: '진행 중',
    statusDone: '완료',
    // 열 이름
    taskName: '작업 이름',
    startDate: '시작일',
    duration: '공수',
    // 날짜 형식
    yearFormat: '%Y년',
    monthFormat: '%m월',
    yearMonthFormat: '%Y년 %m월',
    weekFormat: '제%W주',
    dayFormat: '%d',
    // 도구 설명
    tooltip: {
      assignee: '담당자',
      status: '상태',
      priority: '우선순위',
      startTime: '시작',
      endTime: '종료',
      duration: '공수',
      days: '일',
      estimated: '예상',
      actual: '실제',
      progress: '진행률'
    },
    // dhtmlx gantt locale 설정
    locale: {
      newTask: '새 작업',
      todayButton: '오늘',
      confirmDeleting: '작업을 삭제하시겠습니까?',
      description: '설명',
      timePeriod: '기간',
      type: '유형'
    },
    // 날짜 관련
    months: ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월'],
    days: ['일요일', '월요일', '화요일', '수요일', '목요일', '금요일', '토요일'],
    daysShort: ['일', '월', '화', '수', '목', '금', '토']
  },

  // 팀 구성원
  team: {
    title: '팀 구성원',
    subtitle: '프로젝트 팀 구성원 및 권한 관리',
    addMember: '구성원 추가',
    searchPlaceholder: '구성원 검색...',
    name: '이름',
    email: '이메일',
    role: '역할',
    department: '부서',
    skills: '기술',
    joinedAt: '가입일',
    actions: '작업',
    remove: '제거',
    edit: '편집',
    delete: '삭제',
    allMembers: '모든 구성원',
    workloadDistribution: '작업 부하 분포',
    taskCount: '작업 수',
    editMember: '구성원 편집',
    addNewMember: '새 구성원 추가',
    form: {
      nameLabel: '이름',
      namePlaceholder: '구성원 이름을 입력하세요',
      emailLabel: '이메일',
      emailPlaceholder: 'example@company.com',
      roleLabel: '역할',
      rolePlaceholder: '역할을 선택하세요',
      departmentLabel: '부서',
      departmentPlaceholder: '예: 개발팀',
      skillsLabel: '기술',
      skillsPlaceholder: '쉼표로 구분, 예: Vue, TypeScript, Node.js',
      skillsHint: '여러 기술은 쉼표로 구분하세요'
    },
    buttons: {
      saveChanges: '변경사항 저장',
      addMember: '구성원 추가'
    },
    messages: {
      requiredFields: '모든 필수 항목을 입력하세요!',
      updateSuccess: '구성원 정보가 업데이트되었습니다!',
      createSuccess: '새 구성원이 추가되었습니다!',
      deleteConfirm: '정말 "{name}" 구성원을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.',
      deleteSuccess: '구성원이 삭제되었습니다!',
      operationFailed: '작업에 실패했습니다. 다시 시도해주세요.',
      deleteFailed: '삭제에 실패했습니다. 다시 시도해주세요.'
    },
    emptyState: {
      title: '구성원 없음',
      message: '아직 팀 구성원이 없습니다'
    }
  },

  // 보고서
  reports: {
    title: '보고서',
    subtitle: '프로젝트 데이터 및 통계 보기',
    projectReport: '프로젝트 보고서',
    taskReport: '작업 보고서',
    memberReport: '구성원 보고서',
    exportReport: '보고서 내보내기',
    dateRange: '날짜 범위',
    generate: '보고서 생성',
    stats: {
      totalProjects: '총 프로젝트 수',
      completionRate: '완료율',
      totalTasks: '총 작업 수',
      inProgressTasks: '진행 중인 작업 수',
      delayedTasks: '지연된 작업 수',
      totalDelayedDays: '총 지연 일수',
      delayRate: '지연율',
      criticalDelays: '심각한 지연（≥7일）'
    },
    charts: {
      projectStatusDistribution: '프로젝트 상태 분포',
      taskPriorityDistribution: '작업 우선순위 분포',
      projectCompletionProgress: '프로젝트 완료 진행률',
      teamPerformance: '팀 성과',
      projectStatus: '프로젝트 상태',
      taskPriority: '작업 우선순위',
      progress: '진행률',
      completed: '완료됨',
      inProgress: '진행 중',
      taskCount: '작업 수'
    },
    export: {
      title: '보고서 내보내기',
      projectExcel: '프로젝트 Excel',
      projectExcelDesc: '프로젝트 목록 내보내기',
      statisticsExcel: '통계 Excel',
      statisticsExcelDesc: '통계 데이터 및 각 프로젝트 간트 차트 내보내기',
      comprehensiveReport: '종합 보고서',
      comprehensiveReportDesc: '모든 데이터 포함'
    },
    statuses: {
      planning: '계획 중',
      active: '진행 중',
      completed: '완료',
      onHold: '보류'
    },
    priorities: {
      low: '낮음',
      medium: '보통',
      high: '높음',
      urgent: '긴급'
    },
    messages: {
      noProjectData: '내보낼 프로젝트 데이터가 없습니다!',
      exportSuccess: '내보내기 성공!',
      exportFailed: '내보내기에 실패했습니다. 다시 시도해주세요!',
      noData: '내보낼 데이터가 없습니다!'
    }
  },

  // 설정
  settings: {
    title: '설정',
    subtitle: '시스템 구성 및 환경 설정 관리',
    navigation: {
      profile: '프로필',
      notifications: '알림 설정',
      display: '디스플레이 설정',
      security: '보안 설정',
      account: '계정 설정',
      preferences: '환경 설정'
    },
    // 프로필
    profile: {
      title: '프로필',
      changeAvatar: '아바타 변경',
      avatarHint: 'JPG, PNG 형식 지원, 최대 2MB',
      name: '이름',
      email: '이메일',
      department: '부서',
      role: '역할',
      saveChanges: '변경사항 저장'
    },
    // 알림 설정
    notifications: {
      title: '알림 설정',
      emailNotifications: '이메일 알림',
      emailNotificationsDesc: '프로젝트 업데이트 및 작업 배정에 대한 이메일 알림 수신',
      browserNotifications: '브라우저 알림',
      browserNotificationsDesc: '브라우저에서 데스크톱 알림 표시',
      taskReminders: '작업 알림',
      taskRemindersDesc: '작업 마감일 전 알림',
      saveChanges: '변경사항 저장'
    },
    // 디스플레이 설정
    display: {
      title: '디스플레이 설정',
      theme: '테마',
      language: '언어',
      dateFormat: '날짜 형식',
      saveChanges: '변경사항 저장',
      themes: {
        light: '라이트',
        dark: '다크',
        auto: '자동'
      }
    },
    // 보안 설정
    security: {
      title: '보안 설정',
      changePassword: '비밀번호 변경',
      changePasswordDesc: '정기적으로 비밀번호를 변경하면 계정 보안에 도움이 됩니다',
      currentPassword: '현재 비밀번호',
      newPassword: '새 비밀번호',
      confirmPassword: '새 비밀번호 확인',
      updatePassword: '비밀번호 업데이트',
      twoFactor: '2단계 인증',
      twoFactorDesc: '계정 보호를 위한 추가 보안 레이어',
      enableTwoFactor: '2단계 인증 사용'
    },
    // 공통
    logout: '로그아웃'
  },

  // 헤더 탐색
  header: {
    searchPlaceholder: '검색...',
    notifications: '알림',
    userMenu: {
      profile: '프로필',
      settings: '계정 설정',
      logout: '로그아웃'
    }
  },

  // 사이드바
  sidebar: {
    toggle: '접기',
    expand: '펼치기'
  },

  // 사용자 역할
  roles: {
    admin: '관리자',
    projectManager: '프로젝트 관리자',
    member: '구성원',
    viewer: '관찰자'
  },

  // 우선순위
  priorities: {
    low: '낮음',
    medium: '보통',
    high: '높음',
    urgent: '긴급',
    critical: '매우 긴급'
  },

  // 작업 상태
  taskStatus: {
    todo: '할 일',
    inProgress: '진행 중',
    done: '완료'
  },

  // 프로젝트 상태
  projectStatus: {
    planning: '계획 중',
    active: '진행 중',
    completed: '완료',
    onHold: '보류',
    cancelled: '취소됨'
  },

  // 알림 메시지
  messages: {
    success: {
      create: '생성 완료',
      update: '업데이트 완료',
      delete: '삭제 완료',
      save: '저장 완료',
      submit: '제출 완료',
      approve: '승인 완료',
      reject: '거부됨'
    },
    error: {
      create: '생성 실패',
      update: '업데이트 실패',
      delete: '삭제 실패',
      save: '저장 실패',
      submit: '제출 실패',
      approve: '승인 실패',
      network: '네트워크 오류가 발생했습니다. 다시 시도해주세요',
      unauthorized: '권한이 없습니다. 먼저 로그인하세요',
      forbidden: '이 작업을 수행할 권한이 없습니다',
      notFound: '리소스를 찾을 수 없습니다'
    },
    confirm: {
      delete: '정말 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.',
      leave: '저장하지 않은 변경사항이 있습니다. 떠나시겠습니까?',
      logout: '정말 로그아웃하시겠습니까?'
    }
  },

  // 시간
  time: {
    today: '오늘',
    yesterday: '어제',
    tomorrow: '내일',
    thisWeek: '이번 주',
    nextWeek: '다음 주',
    lastWeek: '지난 주',
    thisMonth: '이번 달',
    nextMonth: '다음 달',
    lastMonth: '지난 달'
  },

  // 연장근무 관리
  overtime: {
    title: '프로젝트 연장근무 관리',
    subtitle: '프로젝트 구성원의 연장근무 상황을 기록하고 관리합니다',
    exportExcel: 'Excel 내보내기',
    addNew: '새 연장근무 추가',
    personalTab: '개인 연장근무 정보',
    teamTab: '담당 구성원 연장근무 정보',
    stats: {
      thisMonthHours: '이번 달 연장근무 시간',
      hours: '시간',
      totalRecords: '연장근무 횟수',
      times: '회',
      totalCompTimeoff: '휴가 누계',
      totalPayHours: '연장근무비 누계',
      thisMonthPeople: '연장근무 인원 수',
      pendingApproval: '승인 대기 수량',
      people: '명'
    },
    charts: {
      trend: '연장근무 추세（최근 30일）',
      projectDistribution: '프로젝트별 연장근무 분포',
      overtimeHours: '연장근무 시간',
      overtimeCount: '연장근무 횟수'
    },
    filters: {
      project: '프로젝트',
      allProjects: '전체 프로젝트',
      status: '상태',
      allStatus: '전체 상태',
      type: '유형',
      allTypes: '전체 유형',
      all: '전체',
      dateRange: '날짜 범위',
      to: '~',
      date: '날짜',
      projectName: '프로젝트',
      projectOwner: '프로젝트 담당자',
      relatedTask: '관련 작업',
      timePeriod: '시간대',
      duration: '시간',
      reason: '연장근무 사유',
      approver: '승인자',
      approvalTime: '승인 시간',
      compensationType: '보상 방식'
    },
    columns: {
      project: '프로젝트',
      relatedTask: '관련 작업',
      timePeriod: '시간대',
      hours: '시간',
      type: '유형',
      compensationType: '보상 방식',
      reason: '연장근무 사유',
      actions: '작업'
    },
    pagination: {
      perPage: '건/페이지',
      itemsPerPage: ['10', '20', '50', '100']
    },
    statuses: {
      pending: '승인 대기',
      approved: '승인됨',
      rejected: '거절됨'
    },
    types: {
      weekday: '평일 연장근무',
      weekend: '주말 연장근무',
      holiday: '휴일 연장근무'
    },
    compensation: {
      timeoff: '휴가',
      pay: '급여 보상'
    },
    empty: {
      noRecords: '연장근무 기록 없음',
      noData: '데이터 없음'
    }
  },

  // 지연 통계
  delayStats: {
    title: '프로젝트 지연 통계',
    subtitle: '개인 및 팀의 작업 지연 상황 통계를 조회합니다',
    refresh: '데이터 새로고침',
    loading: '데이터를 불러오는 중...',
    personalTab: '개인 담당 작업',
    teamTab: '팀 구성원 작업',
    empty: {
      noTasks: '담당 작업 없음',
      noData: '현재 담당하고 있는 작업이 없습니다',
      noTeamMembers: '팀 구성원 없음',
      noTeamMembersDesc: '현재 관리하는 팀 구성원이 없습니다'
    },
    stats: {
      totalTasks: '총 작업 수',
      delayedTasks: '지연된 작업 수',
      delayRate: '지연율',
      totalDelayedDays: '총 지연 일수'
    },
    charts: {
      delayDurationDistribution: '지연 시간 분포',
      projectDelayDistribution: '프로젝트 지연 분포',
      personalDelayDistribution: '개인 지연 분포',
      teamDelayDistribution: '팀 지연 분포',
      delayedTasks: '지연된 작업',
      totalDelayedDays: '총 지연 일수',
      delayRate: '지연율',
      totalTasks: '총 작업 수',
      delayedTasksCount: '지연된 작업 수',
      teamTotalTasks: '팀 총 작업 수',
      teamDelayedTasks: '팀 지연된 작업 수',
      teamDelayRate: '팀 지연율',
      teamMemberCount: '팀 구성원 수',
      memberDelayRateComparison: '구성원 지연율 비교',
      memberDelayedTasksComparison: '구성원 지연된 작업 수 비교',
      teamMemberDelayDetails: '팀 구성원 지연 상세'
    },
    list: {
      delayedTaskList: '지연된 작업 목록',
      taskName: '작업 이름',
      projectName: '소속 프로젝트',
      delayedDays: '지연 일수',
      severity: '심각도',
      endDate: '종료 날짜',
      status: '상태',
      noDelayedTasks: '지연된 작업 없음',
      member: '구성원',
      totalTasks: '총 작업 수',
      delayedTasks: '지연된 작업 수',
      delayRate: '지연율',
      totalDelayedDays: '총 지연 일수',
      actions: '작업',
      viewDetails: '상세 보기'
    },
    details: {
      memberTaskDetails: '구성원 작업 상세'
    },
    severity: {
      severe: '심각한 지연',
      moderate: '중간 지연',
      mild: '약간의 지연'
    },
    status: {
      todo: '대기',
      inProgress: '진행 중',
      completed: '완료'
    },
    common: {
      unknownProject: '알 수 없는 프로젝트',
      unknown: '알 수 없음',
      noData: '데이터 없음',
      noTasks: '작업 없음',
      noTasksForMember: '해당 구성원은 현재 담당하고 있는 작업이 없습니다',
      days: '일',
      viewDetails: '상세 보기'
    },
    tooltip: {
      projectDelay: '지연된 작업 수',
      cumulativeDelayedDays: '총 지연 일수',
      memberDelayRate: '지연율',
      totalTasks: '총 작업 수',
      delayedTasks: '지연된 작업'
    }
  },

  // 주간 보고
  weeklyReports: {
    title: '주간 보고',
    subtitle: '프로젝트 주간 보고 관리',
    createReport: '주간 보고 생성',
    newReport: '새 주간 보고',
    myReports: '내 주간 보고',
    projectReports: '프로젝트 주간 보고',
    currentWeek: '이번 주 주간 보고',
    searchPlaceholder: '주간 보고 검색...',
    status: '상태',
    clearFilters: '필터 지우기',
    confirmDelete: '이 주간 보고를 삭제하시겠습니까?',
    viewModes: {
      card: '카드',
      list: '목록'
    },
    listView: {
      project: '프로젝트',
      week: '주간',
      status: '상태',
      submitter: '작성자',
      date: '날짜',
      actions: '작업'
    },
    statuses: {
      all: '전체',
      draft: '초안',
      submitted: '제출됨',
      approved: '승인됨',
      rejected: '거부됨'
    },
    form: {
      createTitle: '주간 보고 생성',
      editTitle: '주간 보고 편집',
      project: '프로젝트',
      selectProject: '프로젝트를 선택하세요',
      week: '주간',
      weekHint: '월요일에서 일요일까지 자동 설정',
      editHint: '주간 보고 제출 후 프로젝트와 주간은 수정할 수 없습니다',
      completedWork: '이번 주 완료된 작업',
      completedWorkPlaceholder: '이번 주에 완료한 주요 작업을 설명하세요...',
      nextWeekPlan: '다음 주 계획',
      nextWeekPlanPlaceholder: '다음 주의 작업 계획을 설명하세요...',
      problems: '문제 및 제안',
      problemsPlaceholder: '문제와 제안을 기록하세요 (선택 사항)...',
      saveDraft: '초안 저장',
      submit: '제출',
      errors: {
        selectProject: '프로젝트를 선택하세요',
        selectWeek: '주간을 선택하세요',
        enterCompletedWork: '이번 주 완료된 작업을 입력하세요',
        enterNextWeekPlan: '다음 주 계획을 입력하세요'
      }
    },
    detail: {
      title: '주간 보고 상세',
      approve: '승인',
      week: '주간',
      submitter: '작성자',
      submitTime: '제출 시간',
      approveTime: '승인 시간',
      completedWork: '이번 주 완료된 작업',
      nextWeekPlan: '다음 주 계획',
      problems: '문제 및 제안',
      noContent: '내용 없음',
      approvalComment: '승인 의견',
      rejectionComment: '거부 사유',
      approver: '승인자',
      loading: '로딩 중...'
    },
    comments: {
      title: '댓글',
      addComment: '댓글 추가...',
      submit: '제출',
      noComments: '댓글 없음'
    },
    approve: {
      approve: '승인',
      reject: '거부',
      comment: '승인 의견',
      commentPlaceholder: '승인 의견을 작성하세요 (선택 사항)...',
      submitApprove: '승인 제출',
      approveSuccess: '주간 보고가 승인되었습니다',
      rejectSuccess: '주간 보고가 거부되었습니다'
    },
    emptyState: {
      title: '주간 보고 없음',
      noResults: '일치하는 주간 보고를 찾을 수 없습니다',
      createFirst: '첫 번째 주간 보고를 만들어 보세요',
      createButton: '주간 보고 생성'
    }
  },

  // 문서 관리
  documents: {
    subtitle: '모든 프로젝트 문서 관리 및 보기',
    search: '검색',
    searchPlaceholder: '문서 검색...',
    allProjects: '전체 프로젝트',
    allCategories: '전체 분류',
    noDocuments: '문서 없음',
    clearFilters: '필터 지우기',
    activeFilters: '현재 필터',
    download: '다운로드',
    preview: '미리보기',
    edit: '편집',
    delete: '삭제',
    deleteConfirm: '문서 "{name}"을(를) 삭제하시겠습니까?',
    deleteError: '삭제 실패, 다시 시도하세요',
    downloadError: '다운로드 실패, 다시 시도하세요',
    today: '오늘',
    yesterday: '어제',
    daysAgo: '일 전',
    weeksAgo: '주 전',
    uploaded: '다운로드됨',
    times: '회',
    uploading: '업로드 중...',
    updating: '업데이트 중...',
    update: '업데이트',
    previewFailed: '미리보기 실패',
    cannotPreview: '미리보기 불가',
    unsupportedFileType: '이 파일 유형은 온라인 미리보기를 지원하지 않습니다',
    pdfPreview: 'PDF 미리보기',
    name: '문서 이름',
    namePlaceholder: '비워두면 원래 파일 이름 사용',
    category: '분류',
    selectCategory: '분류 선택',
    project: '소속 프로젝트',
    selectProject: '프로젝트 선택',
    description: '문서 설명',
    descriptionPlaceholder: '문서 설명 (선택 사항)',
    dragOrClick: '파일을 여기로 드래그',
    orClickSelect: '또는 클릭하여 파일 선택',
    fileSizeLimit: 'PDF, Word, Excel, PPT, 이미지 형식 지원, 최대 10MB',
    fileSize: '파일 크기',
    createdAt: '생성 시간',
    actions: '작업',
    categories: {
      requirements: '요구사항 문서',
      design: '디자인 문서',
      development: '개발 문서',
      testing: '테스트 문서',
      deployment: '배포 문서',
      documentation: '사용자 매뉴얼',
      other: '기타'
    }
  }
};
