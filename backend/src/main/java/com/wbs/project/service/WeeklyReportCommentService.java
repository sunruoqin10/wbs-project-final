package com.wbs.project.service;

import com.wbs.project.entity.WeeklyReportComment;
import com.wbs.project.mapper.WeeklyReportCommentMapper;
import com.wbs.project.mapper.WeeklyReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WeeklyReportCommentService {

    private final WeeklyReportCommentMapper commentMapper;
    private final WeeklyReportMapper reportMapper;

    public List<WeeklyReportComment> getCommentsByReportId(String reportId) {
        return commentMapper.selectByReportId(reportId);
    }

    @Transactional
    public WeeklyReportComment createComment(WeeklyReportComment comment) {
        if (comment.getId() == null || comment.getId().isEmpty()) {
            comment.setId(UUID.randomUUID().toString());
        }
        comment.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(comment);
        return comment;
    }

    @Transactional
    public void deleteComment(String id) {
        WeeklyReportComment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        commentMapper.deleteById(id);
    }

    @Transactional
    public void deleteCommentsByReportId(String reportId) {
        commentMapper.deleteByReportId(reportId);
    }

    public int countByReportId(String reportId) {
        return commentMapper.countByReportId(reportId);
    }
}
