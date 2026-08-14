package mx.sgfte.core.audit.web;
import jakarta.servlet.ServletException;
import mx.sgfte.core.shared.web.Csv;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mx.sgfte.core.audit.AuditLog;
import mx.sgfte.core.audit.AuditLogDao;
import mx.sgfte.core.audit.AuditLogService;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
@WebServlet("/admin/logs.csv")
public class LogsExportServlet extends HttpServlet{
    private final AuditLogDao auditLogDao = new AuditLogDao();

    private final AuditLogService auditLogService = new AuditLogService();
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String search = req.getParameter("search");
        String severity = req.getParameter("severity");
        String moduleFilter = req.getParameter("moduleFilter");

        List<AuditLog> rows = auditLogDao.find(search, severity, moduleFilter,0,4000);

        Csv csv = new Csv();
        csv.row("Fecha","Hora","Nivel","Acción","Usuario","Módulo","Origen");
        for(AuditLog auditLog : rows){
            csv.row(auditLog.getDateLabel(), auditLog.getDateLabel(), auditLog.getSeverity(), auditLog.getAction(), auditLog.getActor(),auditLog.getModule(),auditLog.getIpAddress());
        }
        String fileName = "Export_Today.csv";
        resp.setContentType("text/csv");
        resp.setCharacterEncoding("UTF-8");   // antes de getWriter(), o no aplica
        resp.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        resp.setHeader("Cache-Control", "no-store");

        try (PrintWriter out = resp.getWriter()) {
            out.print("\uFEFF");
            out.print(csv);
        }
    }

}
