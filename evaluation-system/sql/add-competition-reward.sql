-- 竞赛证书管理表
DROP TABLE IF EXISTS `competition`;
CREATE TABLE `competition` (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(200) NOT NULL COMMENT '竞赛/证书名称',
  type INT DEFAULT 0 COMMENT '0竞赛 1证书',
  level VARCHAR(20) COMMENT '国家级/省级/校级',
  organizer VARCHAR(200) COMMENT '主办单位',
  award_level VARCHAR(50) COMMENT '奖项级别(特等奖/一等奖/二等奖/三等奖/金奖/银奖/铜奖)',
  bonus_score DECIMAL(10,2) DEFAULT 0 COMMENT '加分分值',
  sort_order INT DEFAULT 0,
  status INT DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '竞赛证书管理表';

-- 奖惩记录表
DROP TABLE IF EXISTS `reward_punish`;
CREATE TABLE `reward_punish` (
  id INT AUTO_INCREMENT PRIMARY KEY,
  student_id INT NOT NULL COMMENT '学生ID',
  batch_id INT COMMENT '批次ID',
  type INT DEFAULT 0 COMMENT '0奖励 1惩罚',
  category VARCHAR(50) COMMENT '类别(竞赛获奖/论文发表/志愿服务/考试作弊/违纪等)',
  title VARCHAR(200) COMMENT '事由标题',
  description TEXT COMMENT '详细说明',
  score_change DECIMAL(10,2) DEFAULT 0 COMMENT '分值变动(奖励为正,惩罚为负)',
  attachment VARCHAR(500) COMMENT '证明材料',
  status INT DEFAULT 0 COMMENT '0待审核 1已通过 2已驳回',
  auditor_id INT COMMENT '审核人ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (student_id) REFERENCES user(id)
) COMMENT '奖惩记录表';

-- 插入竞赛测试数据
INSERT INTO competition (name, type, level, organizer, award_level, bonus_score, sort_order) VALUES
('中国国际大学生创新大赛（原互联网+）', 0, '国家级', '教育部', '金奖', 20, 1),
('中国国际大学生创新大赛（原互联网+）', 0, '国家级', '教育部', '银奖', 16, 2),
('中国国际大学生创新大赛（原互联网+）', 0, '国家级', '教育部', '铜奖', 12, 3),
('挑战杯全国大学生课外学术科技作品竞赛', 0, '国家级', '共青团中央', '特等奖', 20, 4),
('挑战杯全国大学生课外学术科技作品竞赛', 0, '国家级', '共青团中央', '一等奖', 16, 5),
('挑战杯全国大学生课外学术科技作品竞赛', 0, '国家级', '共青团中央', '二等奖', 12, 6),
('ACM-ICPC国际大学生程序设计竞赛', 0, '国家级', 'ACM', '金奖', 20, 7),
('蓝桥杯全国软件和信息技术专业人才大赛', 0, '国家级', '工信部', '一等奖', 16, 8),
('蓝桥杯全国软件和信息技术专业人才大赛', 0, '省级', '工信部', '一等奖', 10, 9),
('全国大学生数学建模竞赛', 0, '国家级', '教育部', '一等奖', 16, 10),
('全国大学生数学建模竞赛', 0, '省级', '教育厅', '一等奖', 10, 11),
('全国大学生英语竞赛(NECCS)', 0, '国家级', '教育部', '特等奖', 20, 12),
('大学英语六级考试(CET-6)', 1, '国家级', '教育部', '通过', 8, 13),
('大学英语四级考试(CET-4)', 1, '国家级', '教育部', '通过', 5, 14),
('全国计算机等级考试二级', 1, '国家级', '教育部', '通过', 5, 15),
('全国计算机等级考试三级', 1, '国家级', '教育部', '通过', 8, 16);
