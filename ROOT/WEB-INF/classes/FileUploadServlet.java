import java.io.*;
import java.util.*;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
  

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
  
@WebServlet("/FileUploadServlet")
@MultipartConfig(fileSizeThreshold=1024*1024*10,    // 10 MB 
                 maxFileSize=1024*1024*50,          // 50 MB
                 maxRequestSize=1024*1024*100)      // 100 MB
public class FileUploadServlet extends HttpServlet
{
    private static HashMap<Integer, ArrayList<String>> inputMap = new HashMap<Integer, ArrayList<String>>();
    private static HashMap<Integer, Object> fieldMap = new HashMap<Integer, Object>();
    private static final String FILEPATH = "/var/lib/tomcat7/webapps/ROOT/tmp/";        
    private static final long serialVersionUID = 205242440643911308L;
    

    private static class Zipcode
    {
        private String zip;
        private String city;
        private String state;
        private String county;
        
        public Zipcode(String zip,  String city, String state, String county)
        {
            this.zip = zip;
            this.city = city;
            this.state = state;
            this.county = county;
        }
        
        
        @Override
        public String toString()
        {
            return  zip.trim() +"|"+city.trim()+"|"+state.trim()+"|"+county.trim();
        }
    }
    
    /**
     * Directory where uploaded files will be saved, its relative to
     * the web application directory.
     */
    private static final String UPLOAD_DIR = "tmp";
      
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException 
    {
         response.setContentType("text/html");
	     PrintWriter out = response.getWriter();
        out.write("<html><head></head><body>");
//      fieldMap.clear();
//        inputMap.clear(); 

        String fileName = null;
//	 String[] name = fileName.split("\\.");
        String shortName = null;
	 String fullName = null; // FILEPATH + name[0] + ".txt";
        //Get all the parts from request and write it to the file on server
        for (Part part : request.getParts())
        {
            fileName = getFileName(part);
            InputStream inpSt = part.getInputStream();

            XSSFWorkbook workbook = new XSSFWorkbook(inpSt); 
            Sheet firstSheet = workbook.getSheetAt(0);
     
        	for (Iterator<Row> rit = firstSheet.rowIterator(); rit.hasNext();)
        	{
        	    ArrayList<String> rows = new ArrayList<String>();
        	    int count = 0;
        	    Row row = rit.next();
      
        	   for(int i = 0; i<7; i++)
           	   {
			       Cell cell = row.getCell(i, Row.CREATE_NULL_AS_BLANK);
                   
                   if(i!=4)
                   {
                                switch (cell.getCellType()) 
                                {
                                case Cell.CELL_TYPE_STRING:              
                                        String place = cell.toString();
                                        rows.add(place);        
                                        break;
                                case Cell.CELL_TYPE_BLANK:
                                        String value = "";
                                        rows.add(value); 
                                        break;
                                case Cell.CELL_TYPE_NUMERIC:
                                        String number = cell.toString();
                                        rows.add(number); 
                                        break;
                                }
                //                inputMap.put(count, rows);
                   }
                 inputMap.put(count, rows);
              }
         
       

			  workbook.close();
               
                for(int key : inputMap.keySet())
                {
                
                        ArrayList<String> fields = inputMap.get(key);
                
                        //System.out.println(fields);
                
                        fieldMap = answerMap(fields);   
                }
           } //floater
              
        	for(int Key : fieldMap.keySet())
        	{
                Object zipcode = fieldMap.get(Key);
       
                List<String> list = new ArrayList<String>();
                String Zipcode = zipcode.toString();
                list.add(Zipcode); 
      
                String[] name = fileName.split("\\.");
		shortName = name[0] + ".txt";	
                fullName = FILEPATH + name[0] + ".txt";
      
                useBufferedWriter(list, fullName);
              //  workbook.close();     
        	}
        
           fieldMap.clear();
           inputMap.clear();    
        /*	request.setAttribute("message", fileName + " converted to text and edited");
        	getServletContext().getRequestDispatcher("/response.jsp").forward(
                request, response); */
           File file = new File(request.getServletContext().getAttribute("FILES_DIR")+File.separator+shortName);
                System.out.println("Absolute Path at server="+file.getAbsolutePath());
                //fileItem.write(file);
                out.write("File "+shortName+ " uploaded successfully.");
                out.write("<br>");
                out.write("<a href=\"FileUploadServlet?shortName="+shortName+"\">Download "+shortName+"</a>");
                
        }        
  }
  
   protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String fileName = request.getParameter("shortName");
        if(fileName == null || fileName.equals("")){
            throw new ServletException("File Name can't be null or empty");
        }
        File file = new File(request.getParameter("FILEPATH")+File.separator+fileName);
        if(!file.exists()){
            throw new ServletException("File doesn't exist on server.");
        }
        System.out.println("File location on server::"+file.getAbsolutePath());
        ServletContext ctx = getServletContext();
        InputStream fis = new FileInputStream(file);
        String mimeType = ctx.getMimeType(file.getAbsolutePath());
        response.setContentType(mimeType != null? mimeType:"application/octet-stream");
        response.setContentLength((int) file.length());
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
         
        ServletOutputStream os       = response.getOutputStream();
        byte[] bufferData = new byte[1024];
        int read=0;
        while((read = fis.read(bufferData))!= -1){
            os.write(bufferData, 0, read);
        }
        os.flush();
        os.close();
        fis.close();
        System.out.println("File downloaded at client successfully");
    }
  
    /**
     * Utility method to get file name from HTTP header content-disposition
     */
    private String getFileName(Part part) {
        String contentDisp = part.getHeader("content-disposition");
        System.out.println("content-disposition header= "+contentDisp);
        String[] tokens = contentDisp.split(";");
        for (String token : tokens) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf("=") + 2, token.length()-1);
            }
        }
        return "";
    }

public static HashMap<Integer, Object> answerMap(ArrayList<String> fieldArr)
{
		int count = fieldMap.size()+1; //because header was added above, count == 1, so each addt'l line is count++
	   	 
		String zip = fieldArr.get(0);
		String type = fieldArr.get(1);
		String city = fieldArr.get(2);
		String acceptableCity = fieldArr.get(3);
		String state = fieldArr.get(4);
		String county = fieldArr.get(5);
	
		String fullZip = fiveDigit(zip);
		
		//processes lines with only one city for given zip
		if(type.equals("STANDARD") && acceptableCity.equals("") && !county.equals(""))
		{
			
			Zipcode zpcd = new Zipcode(fullZip, city, state, county);
			fieldMap.put(count, zpcd);
		}
		//this block processes lines that have only one addt'l city for given zip
		else if(type.equals("STANDARD") && !acceptableCity.equals("") && !acceptableCity.contains(",") && !county.equals(""))
		{
			//passes primary city into map
			Zipcode zpcd = new Zipcode(fullZip, city, state, county);
			fieldMap.put(count, zpcd);
		//	logger.info("primary city added: "+city+", "+zip);
			
			city = acceptableCity; //acceptable name swapped in place of primary name
			count++;
			//addit'l city passed to map
			Zipcode zipcode = new Zipcode(fullZip, city, state, county);
			fieldMap.put(count, zipcode);
		//	logger.info("acceptable city added: "+city+", "+zip);
			
		}
		//this block processes lines that have several, comma-delimited acceptable cities
		else if(type.equals("STANDARD") && !acceptableCity.equals("") && acceptableCity.contains(",") && !county.equals(""))
		{
			multiCity(fieldArr);
		}
	   	 
		return fieldMap;	
	   	 
	}
	
	//method processes lines with multiple, comma-delimited acceptable cities
	public static HashMap<Integer,Object> multiCity(ArrayList<String> fieldArr)
	{
		int count = fieldMap.size();
		
		String zip = fieldArr.get(0);
 	//	String type = fieldArr.get(1);
		String city = fieldArr.get(2);
		String acceptableCity = fieldArr.get(3);
		String state = fieldArr.get(4);
		String county = fieldArr.get(5);
		
		String fullZip = fiveDigit(zip);
	
		//creates hashmap entry for primary city
		Zipcode zpcd = new Zipcode(fullZip, city, state, county);
		fieldMap.put(count, zpcd);
		//logger.info("primary city added: "+city+", "+zip);
		count++;
	
		String[] cities = acceptableCity.split(",");
		for(String ciudad : cities)
		{
			//creates entry for each addt'l city name for given zipcode
			Zipcode zipcode = new Zipcode(fullZip, ciudad.trim(), state, county);
			fieldMap.put(count, zipcode);
			//logger.info("acceptable city added: "+ciudad+", "+zip);
			count++;
		
		}
 
	
		return fieldMap;	
	}
 
	public static String fiveDigit(String zip)
	{
		
		String[] zipSplit = zip.split("\\.");
		zip= zipSplit[0];
		
		if(zip.length()==5)
		{
			return zip;
		}
		else if(zip.length()==4)
		{
			zip = "0"+zip;
		}
		else if (zip.length()==3)
		{
			zip = "00"+zip;
		}
 	
		return zip;
	}
	
	public static void useBufferedWriter(List<String> content, String filePath)
    {
    	File file = new File(filePath);
    	Writer fileWriter = null;
    	BufferedWriter bufferedWriter = null;
    	
    	try{
    		
    		fileWriter = new FileWriter(file, true);
    		bufferedWriter = new BufferedWriter(fileWriter);
    		
    			for (String line : content)
    			{
    				line += System.getProperty("line.separator");
    				bufferedWriter.write(line);

    			}
    	
    	 } 
    	catch (IOException e) 
    	{
    		 System.err.println("Error writing the file : ");
    		 e.printStackTrace();
    	 } 
    	finally 
    	{
    		 
    		 if (bufferedWriter != null && fileWriter != null)
    		 {
    			 try 
    			 {
    				bufferedWriter.close();
    				fileWriter.close();
    			 } 
    			 catch(IOException e)
    			 {	 
    				 e.printStackTrace();
    			 }
    		 }
    	 }    		 
    }
}

