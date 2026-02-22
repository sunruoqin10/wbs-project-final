<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>WBS项目管理系统</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
            line-height: 1.6;
            color: #333;
            margin: 0;
            padding: 0;
            background-color: #f5f5f5;
        }
        .email-container {
            max-width: 600px;
            margin: 0 auto;
            padding: 20px;
            background-color: #ffffff;
        }
        .header {
            background: linear-gradient(135deg, #10b981 0%, #3b82f6 100%);
            padding: 30px;
            text-align: center;
            border-radius: 8px 8px 0 0;
        }
        .header h1 {
            color: #ffffff;
            margin: 0;
            font-size: 24px;
            font-weight: 600;
        }
        .content {
            padding: 30px;
            background-color: #ffffff;
            border-left: 1px solid #e5e7eb;
            border-right: 1px solid #e5e7eb;
        }
        .footer {
            padding: 20px;
            text-align: center;
            background-color: #f9fafb;
            border-radius: 0 0 8px 8px;
            border: 1px solid #e5e7eb;
            border-top: none;
        }
        .footer p {
            margin: 0;
            color: #6b7280;
            font-size: 14px;
        }
        .info-box {
            background-color: #ecfdf5;
            border-left: 4px solid #10b981;
            padding: 15px 20px;
            margin: 20px 0;
            border-radius: 4px;
        }
    </style>
</head>
<body>
    <div class="email-container">
        <div class="header">
            <h1>WBS项目管理系统</h1>
        </div>
        <div class="content">
            <p>您好，${userName}：</p>
            
            <p>您项目中的任务已完成！</p>
            
            <div class="info-box">
                <p><strong>任务标题：</strong>${taskTitle}</p>
                <p><strong>任务负责人：</strong>${assigneeName}</p>
            </div>
            
            <p>请登录系统查看项目进度。</p>
            
            <p style="margin-top: 30px;">此致</p>
            <p>WBS项目管理系统</p>
        </div>
        <div class="footer">
            <p>此邮件由系统自动发送，请勿直接回复</p>
            <p style="margin-top: 10px;">&copy; 2024 WBS项目管理系统. All rights reserved.</p>
        </div>
    </div>
</body>
</html>
