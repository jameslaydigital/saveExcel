import java.io.File;
 
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
 
@WebListener
public class FileLocationContextListener implements ServletContextListener {
 
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        String rootPath = System.getProperty("CATALINA_BASE");
        ServletContext ctx = servletContextEvent.getServletContext();
        String relativePath = ctx.getInitParameter("webapps/ROOT/tmp");
        File file = new File(rootPath + File.separator + relativePath);
        if(!file.exists()) file.mkdirs();
        System.out.println("File Directory created to be used for storing files");
        ctx.setAttribute("FILES_DIR_FILE", file);
        ctx.setAttribute("FILES_DIR", rootPath + File.separator + relativePath);
    }
 
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        //do cleanup if needed
    }
     
}
