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
      save: '저장 완료'
    },
    error: {
      create: '생성 실패',
      update: '업데이트 실패',
      delete: '삭제 실패',
      save: '저장 실패',
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
  }
};
