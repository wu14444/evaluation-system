-- ============================================
-- 学生综合素质评价管理系统 数据库初始化脚本
-- Database: student_evaluation
-- ============================================

CREATE DATABASE IF NOT EXISTS student_evaluation DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE student_evaluation;

-- 1. 用户表
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  id INT AUTO_INCREMENT PRIMARY KEY,
  student_no VARCHAR(20) COMMENT '学号/工号',
  name VARCHAR(50) NOT NULL COMMENT '姓名',
  password VARCHAR(100) NOT NULL DEFAULT '123456' COMMENT '密码',
  role INT DEFAULT 0 COMMENT '0学生 1教师 2管理员',
  class_id INT COMMENT '班级ID',
  phone VARCHAR(20) COMMENT '手机号',
  email VARCHAR(100) COMMENT '邮箱',
  status INT DEFAULT 1 COMMENT '0禁用 1启用',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '用户表';

-- 2. 班级表
DROP TABLE IF EXISTS `eval_class`;
CREATE TABLE `eval_class` (
  id INT AUTO_INCREMENT PRIMARY KEY,
  class_name VARCHAR(100) NOT NULL COMMENT '班级名称',
  grade VARCHAR(20) COMMENT '年级',
  major VARCHAR(100) COMMENT '专业',
  college VARCHAR(100) COMMENT '学院',
  advisor_id INT COMMENT '辅导员ID',
  student_count INT DEFAULT 0 COMMENT '学生人数',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '班级表';

-- 3. 评价批次表
DROP TABLE IF EXISTS `batch`;
CREATE TABLE `batch` (
  id INT AUTO_INCREMENT PRIMARY KEY,
  academic_year VARCHAR(20) COMMENT '学年',
  semester VARCHAR(10) COMMENT '学期',
  batch_name VARCHAR(100) NOT NULL COMMENT '批次名称',
  start_time DATETIME COMMENT '开始时间',
  end_time DATETIME COMMENT '结束时间',
  status INT DEFAULT 0 COMMENT '0未开始 1进行中 2已结束',
  peer_mode INT DEFAULT 0 COMMENT '0全员互评 1采样互评',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '评价批次表';

-- 4. 指标大类表
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  id INT AUTO_INCREMENT PRIMARY KEY,
  category_name VARCHAR(50) NOT NULL COMMENT '大类名称',
  sort_order INT DEFAULT 0 COMMENT '排序号',
  status INT DEFAULT 1 COMMENT '0停用 1启用',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '指标大类表';

-- 5. 指标项表
DROP TABLE IF EXISTS `indicator`;
CREATE TABLE `indicator` (
  id INT AUTO_INCREMENT PRIMARY KEY,
  category_id INT NOT NULL COMMENT '所属大类',
  indicator_name VARCHAR(200) NOT NULL COMMENT '指标名称',
  max_score DECIMAL(10,2) DEFAULT 100 COMMENT '满分值',
  weight DECIMAL(5,2) DEFAULT 1.00 COMMENT '权重',
  scoring_standard TEXT COMMENT '评分标准',
  sort_order INT DEFAULT 0,
  status INT DEFAULT 1 COMMENT '0停用 1启用',
  eval_type INT DEFAULT 3 COMMENT '0自评 1教师评 2互评 3通用',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (category_id) REFERENCES category(id)
) COMMENT '指标项表';

-- 6. 学生自评表
DROP TABLE IF EXISTS `self_eval`;
CREATE TABLE `self_eval` (
  id INT AUTO_INCREMENT PRIMARY KEY,
  student_id INT NOT NULL,
  batch_id INT NOT NULL,
  indicator_id INT NOT NULL,
  self_score DECIMAL(10,2) COMMENT '自评分数',
  description TEXT COMMENT '自评说明',
  attachment VARCHAR(500) COMMENT '证明材料路径',
  status INT DEFAULT 0 COMMENT '0暂存 1已提交',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '学生自评记录表';

-- 7. 教师评分表
DROP TABLE IF EXISTS `teacher_eval`;
CREATE TABLE `teacher_eval` (
  id INT AUTO_INCREMENT PRIMARY KEY,
  teacher_id INT NOT NULL,
  student_id INT NOT NULL,
  batch_id INT NOT NULL,
  indicator_id INT NOT NULL,
  score DECIMAL(10,2) COMMENT '评分',
  comment VARCHAR(500) COMMENT '评语',
  status INT DEFAULT 1 COMMENT '0草稿 1已提交',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '教师评分表';

-- 8. 互评记录表
DROP TABLE IF EXISTS `peer_eval`;
CREATE TABLE `peer_eval` (
  id INT AUTO_INCREMENT PRIMARY KEY,
  reviewer_id INT NOT NULL COMMENT '评审员ID',
  target_id INT NOT NULL COMMENT '被评学生ID',
  batch_id INT NOT NULL,
  indicator_id INT NOT NULL,
  score DECIMAL(10,2) COMMENT '评分',
  content VARCHAR(500) COMMENT '评价内容',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '互评记录表';

-- 9. 综合得分表
DROP TABLE IF EXISTS `total_score`;
CREATE TABLE `total_score` (
  id INT AUTO_INCREMENT PRIMARY KEY,
  student_id INT NOT NULL,
  batch_id INT NOT NULL,
  self_total DECIMAL(10,2) DEFAULT 0,
  teacher_total DECIMAL(10,2) DEFAULT 0,
  peer_total DECIMAL(10,2) DEFAULT 0,
  bonus_total DECIMAL(10,2) DEFAULT 0,
  final_score DECIMAL(10,2) DEFAULT 0,
  ranking INT COMMENT '排名',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '综合得分表';

-- 10. 操作日志表
DROP TABLE IF EXISTS `audit_log`;
CREATE TABLE `audit_log` (
  id INT AUTO_INCREMENT PRIMARY KEY,
  user_id INT,
  action_type VARCHAR(50) COMMENT '操作类型',
  action_detail VARCHAR(500) COMMENT '操作内容',
  ip_address VARCHAR(50) COMMENT 'IP地址',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '操作日志表';

-- ============================================
-- 测试数据
-- ============================================

-- 班级
INSERT INTO eval_class (class_name, grade, major, college, advisor_id, student_count) VALUES
('计算机科学与技术2401班', '2024级', '计算机科学与技术', '计算机科学与技术学院', 4, 35),
('计算机科学与技术2402班', '2024级', '计算机科学与技术', '计算机科学与技术学院', 5, 32),
('软件工程2401班', '2024级', '软件工程', '软件学院', 4, 30);

-- 用户（密码均为123456）
INSERT INTO `user` (student_no, name, password, role, class_id, phone, email, status) VALUES
('202401001', '张三', '123456', 0, 1, '13800001001', 'zhangsan@nuc.edu.cn', 1),
('202401002', '李四', '123456', 0, 1, '13800001002', 'lisi@nuc.edu.cn', 1),
('202401003', '王五', '123456', 0, 1, '13800001003', 'wangwu@nuc.edu.cn', 1),
('202401004', '赵六', '123456', 0, 2, '13800001004', 'zhaoliu@nuc.edu.cn', 1),
('202401005', '孙七', '123456', 0, 2, '13800001005', 'sunqi@nuc.edu.cn', 1),
('202401006', '李白', '123456', 0, 2, '13800001006', 'libai@nuc.edu.cn', 1),
('202401007', '杜甫', '123456', 0, 3, '13800001007', 'dufu@nuc.edu.cn', 1),
('202401008', '辛弃疾', '123456', 0, 3, '13800001008', 'xinqiji@nuc.edu.cn', 1),
('202401009', '白居易', '123456', 0, 3, '13800001009', 'baijuyi@nuc.edu.cn', 1),
('202401010', '苏轼', '123456', 0, 1, '13800001010', 'sushi@nuc.edu.cn', 1),
('T001', '张老师', '123456', 1, NULL, '13800002001', 'zhangteacher@nuc.edu.cn', 1),
('T002', '李老师', '123456', 1, NULL, '13800002002', 'liteacher@nuc.edu.cn', 1),
('T003', '马辅导员', '123456', 1, NULL, '13800002003', 'mateacher@nuc.edu.cn', 1),
('A001', '系统管理员', '123456', 2, NULL, '13800003001', 'admin@nuc.edu.cn', 1);

-- 指标大类（德智体美劳）
INSERT INTO category (category_name, sort_order, status) VALUES
('德育素质', 1, 1),
('智育素质', 2, 1),
('体育素质', 3, 1),
('美育素质', 4, 1),
('劳育素质', 5, 1);

-- 指标项
INSERT INTO indicator (category_id, indicator_name, max_score, weight, scoring_standard, sort_order, status, eval_type) VALUES
-- 德育素质
(1, '政治思想表现', 20, 1.0, '政治立场端正，积极向党组织靠拢', 1, 1, 1),
(1, '道德品质修养', 20, 1.0, '诚实守信，尊敬师长，团结同学', 2, 1, 1),
(1, '遵纪守法', 20, 1.0, '遵守校纪校规，无违纪违法行为', 3, 1, 3),
(1, '社会责任感', 20, 1.0, '积极参加社会公益活动，具有奉献精神', 4, 1, 3),
(1, '集体荣誉感', 20, 1.0, '关心集体，积极参加班级学院活动', 5, 1, 2),
-- 智育素质
(2, '学业成绩', 30, 1.5, '按学年平均学分绩点折算', 1, 1, 1),
(2, '学科竞赛', 25, 1.2, '国家级+25/省级+15/校级+8', 2, 1, 3),
(2, '学术论文与专利', 20, 1.0, '发表论文或申请专利加分', 3, 1, 3),
(2, '英语与计算机能力', 15, 0.8, '通过CET-4/6、计算机等级考试等', 4, 1, 3),
(2, '创新实践能力', 10, 0.5, '参与大创项目、实验室课题研究', 5, 1, 3),
-- 体育素质
(3, '体育课程成绩', 30, 1.5, '按体育课成绩折算', 1, 1, 1),
(3, '日常体育锻炼', 30, 1.5, '坚持日常运动，出勤率达标', 2, 1, 3),
(3, '体育竞赛参与', 20, 1.0, '参加院级+10/校级+15/省级+20', 3, 1, 3),
(3, '体质健康测试', 20, 1.0, '体测成绩达标', 4, 1, 3),
-- 美育素质
(4, '艺术鉴赏能力', 30, 1.0, '参加艺术类课程或讲座', 1, 1, 3),
(4, '文化艺术活动', 40, 1.5, '参加校园文化活动、社团表演等', 2, 1, 3),
(4, '艺术创作成果', 30, 1.0, '有艺术创作作品或获奖', 3, 1, 3),
-- 劳育素质
(5, '劳动观念与精神', 30, 1.0, '尊重劳动，热爱劳动', 1, 1, 3),
(5, '劳动实践活动', 40, 1.5, '参加校内劳动、社会实践', 2, 1, 3),
(5, '生活技能', 30, 1.0, '宿舍卫生、个人内务整理', 3, 1, 2);

-- 评价批次
INSERT INTO batch (academic_year, semester, batch_name, start_time, end_time, status, peer_mode) VALUES
('2025-2026', '2', '2025-2026学年第二学期综合素质评价', '2026-03-01 00:00:00', '2026-07-31 23:59:59', 1, 0);

-- 学生自评测试数据
INSERT INTO self_eval (student_id, batch_id, indicator_id, self_score, description, status) VALUES
(1, 1, 1, 18, '思想上积极向党组织靠拢，已提交入党申请书', 1),
(1, 1, 2, 18, '遵守学术道德规范，无违纪', 1),
(1, 1, 3, 20, '无任何违反校规校纪行为', 1),
(1, 1, 4, 16, '本学期参加志愿服务2次', 1),
(1, 1, 6, 25, 'GPA 3.8/4.0', 1),
(1, 1, 7, 15, '参加蓝桥杯省级二等奖', 1),
(1, 1, 8, 10, '', 1),
(1, 1, 11, 28, '体育课成绩85分', 1),
(1, 1, 12, 26, '每周坚持跑步3次', 1),
(1, 1, 18, 35, '参加校园清扫活动', 1),
(2, 1, 1, 16, '', 1),
(2, 1, 2, 17, '', 1),
(2, 1, 3, 19, '', 1),
(2, 1, 6, 22, 'GPA 3.2/4.0', 1),
(2, 1, 11, 26, '', 1),
(3, 1, 1, 14, '', 1),
(3, 1, 6, 18, '', 1),
(3, 1, 7, 20, 'ACM-ICPC省级金奖', 1);

-- 教师评分测试数据
INSERT INTO teacher_eval (teacher_id, student_id, batch_id, indicator_id, score, comment, status) VALUES
(4, 1, 1, 5, 18, '积极参与班级活动', 1),
(4, 1, 1, 9, 12, '计算机二级通过', 1),
(4, 2, 1, 5, 16, '', 1),
(4, 3, 1, 5, 17, '', 1);

-- 综合得分（示例）
INSERT INTO total_score (student_id, batch_id, self_total, teacher_total, peer_total, bonus_total, final_score, ranking) VALUES
(1, 1, 196, 30, 85, 20, 87.50, 1),
(2, 1, 100, 16, 0, 0, 72.30, 2),
(3, 1, 52, 17, 0, 25, 70.10, 3);
