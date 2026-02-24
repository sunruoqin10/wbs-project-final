package com.wbs.project.service;

import com.wbs.project.entity.EmailLog;
import com.wbs.project.mapper.EmailLogMapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import jakarta.annotation.PostConstruct;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailLogMapper emailLogMapper;
    private Configuration freemarkerConfig;

    @Value("${spring.mail.username:noreply@wbs-system.com}")
    private String fromEmail;

    @Value("${spring.mail.display-name:WBS项目管理系统}")
    private String displayName;

    @PostConstruct
    public void init() {
        freemarkerConfig = new Configuration(Configuration.VERSION_2_3_31);
        freemarkerConfig.setClassLoaderForTemplateLoading(
            getClass().getClassLoader(), "templates/email"
        );
        freemarkerConfig.setDefaultEncoding("UTF-8");
        freemarkerConfig.setLocale(Locale.CHINA);
        freemarkerConfig.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        freemarkerConfig.setLogTemplateExceptions(false);
        freemarkerConfig.setWrapUncheckedExceptions(true);
        freemarkerConfig.setFallbackOnNullLoopVariable(false);
    }

    public void sendEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        sendEmail(to, null, subject, templateName, variables);
    }

    public void sendEmail(String to, String cc, String subject, String templateName, Map<String, Object> variables) {
        EmailLog emailLog = new EmailLog();
        emailLog.setId("el" + UUID.randomUUID().toString().substring(0, 8));
        emailLog.setToEmail(to);
        emailLog.setCcEmail(cc);
        emailLog.setSubject(subject);
        emailLog.setTemplateName(templateName);
        emailLog.setCreatedAt(LocalDateTime.now());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, displayName);
            helper.setTo(to);
            if (cc != null && !cc.isEmpty()) {
                helper.setCc(cc);
            }
            helper.setSubject(subject);

            Template template = freemarkerConfig.getTemplate(templateName + ".ftl");
            String htmlContent = FreeMarkerTemplateUtils.processTemplateIntoString(template, variables);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            
            emailLog.setStatus("success");
            emailLog.setSentAt(LocalDateTime.now());
            log.info("Email sent successfully to: {}, cc: {}, subject: {}", to, cc, subject);
        } catch (MessagingException e) {
            emailLog.setStatus("failed");
            emailLog.setErrorMessage(e.getMessage());
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("邮件发送失败", e);
        } catch (Exception e) {
            emailLog.setStatus("failed");
            emailLog.setErrorMessage(e.getMessage());
            log.error("Error sending email to: {}", to, e);
            throw new RuntimeException("邮件发送异常", e);
        } finally {
            try {
                emailLogMapper.insert(emailLog);
            } catch (Exception e) {
                log.error("Failed to save email log", e);
            }
        }
    }

    public void sendSimpleEmail(String to, String subject, String content) {
        EmailLog emailLog = new EmailLog();
        emailLog.setId("el" + UUID.randomUUID().toString().substring(0, 8));
        emailLog.setToEmail(to);
        emailLog.setSubject(subject);
        emailLog.setCreatedAt(LocalDateTime.now());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, displayName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, false);

            mailSender.send(message);
            
            emailLog.setStatus("success");
            emailLog.setSentAt(LocalDateTime.now());
            log.info("Simple email sent successfully to: {}, subject: {}", to, subject);
        } catch (Exception e) {
            emailLog.setStatus("failed");
            emailLog.setErrorMessage(e.getMessage());
            log.error("Failed to send simple email to: {}", to, e);
            throw new RuntimeException("邮件发送失败", e);
        } finally {
            try {
                emailLogMapper.insert(emailLog);
            } catch (Exception e) {
                log.error("Failed to save email log", e);
            }
        }
    }
}
