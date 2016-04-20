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
    /** FileUploadServlet processes a multipart mime .xlsx upload and converts
     *  it to a bar-delimited csv before saving it to the local server.
     *  (currently doesn't support any other file format) */

    private static HashMap<Integer, ArrayList<String>> _rows = new HashMap<Integer, ArrayList<String>>();
    private static HashMap<Integer, Object> _zipcodeMap = new HashMap<Integer, Object>();
    private static final String _filePath = "/var/lib/tomcat7/webapps/ROOT/tmp/";        

    private static class _FIELDS {
        //constants used to access fields by their column number
        private static int zip = 0;
        private static int type = 1;
        private static int city = 2;
        private static int acceptableCity = 3;
        private static int state = 4;
        private static int county = 5;
    }

    private static class Zipcode
    {
        //Zipcode represents a row in the spreadsheet
        //TODO: Perhaps consider renaming to Row?
        //EDIT: Row is already an object in POI.
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

    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException, NullPointerException
    {

        /** doPost: save .xlsx multipart upload as bar delimited file at
         * "/tmp". */

        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.write("<html><head></head><body>");

        String fileName  = null; //the name of the file as uploaded.
        String shortName = null; //the basename of output file
        String fullName  = null; //the pathname + basename of output file

        //Get all the parts from request and write it to the file on server
        //_rows = [str(cell) for cell in [row for row in [part for part in request.getParts()]]]
        for (Part part : request.getParts())
        {
            fileName = getFileName(part);
            InputStream inpSt = part.getInputStream();

            //try stmt in case they up'd wrong ftype
            try {
                XSSFWorkbook workbook = new XSSFWorkbook(inpSt); 

                Sheet firstSheet = workbook.getSheetAt(0);
         
                for (Iterator<Row> rit = firstSheet.rowIterator(); rit.hasNext();)
                {
                    ArrayList<String> cells = new ArrayList<String>();
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
                                cells.add(place);        
                                break;
                                case Cell.CELL_TYPE_BLANK:
                                String value = "";
                                cells.add(value); 
                                break;
                                case Cell.CELL_TYPE_NUMERIC:
                                String number = cell.toString();
                                cells.add(number); 
                                break;
                            }
                        }
                        _rows.put(count, cells);
                    }

                    workbook.close();

                    for(int key : _rows.keySet())
                    {
                        ArrayList<String> fields = _rows.get(key);
                        _zipcodeMap = getZipcodeMap(fields);   
                    }
                }
                  
                for(int Key : _zipcodeMap.keySet())
                {
                    Object zipcode = _zipcodeMap.get(Key);

                    List<String> list = new ArrayList<String>();
                    String Zipcode = zipcode.toString();
                    list.add(Zipcode); 

                    String[] name = fileName.split("\\.");
                    shortName = name[0] + ".txt";	
                    fullName = _filePath + name[0] + ".txt";

                    useBufferedWriter(list, fullName);
                }
            
                _zipcodeMap.clear();
                _rows.clear();    
                File file = new File(request.getServletContext().getAttribute("FILES_DIR")+File.separator+shortName);
                out.write("File "+shortName+ " uploaded successfully.");
                out.write("<br>");
                out.write("<a href=\"/tmp/"+shortName+"\">Download "+shortName+"</a>");

            } catch(org.apache.poi.POIXMLException e) {
                out.write("<div> Only .xlsx files are supported at this time. </div>");
            }
            out.write("</body></html>");
        }
    }
  
    private String getFileName(Part part) throws NullPointerException {
        /** getFileName: get file name from HTTP header content-disposition.  */

        String contentDisp = part.getHeader("content-disposition");
        System.out.println("content-disposition header= "+contentDisp);
        String[] tokens = contentDisp.split(";");
        for (String token : tokens) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf("=") + 2, token.length()-1);
            }
        }
        throw new NullPointerException("Request header did not contain a file name.");
    }

    public static HashMap<Integer, Object> getZipcodeMap(ArrayList<String> fieldArr)
    {
        int count = 1; //start at 1 to skip the header. TODO is there still a header?

        String zip  = fieldArr.get(_FIELDS.zip);
        String type = fieldArr.get(_FIELDS.type);
        String city = fieldArr.get(_FIELDS.city);
        String state  = fieldArr.get(_FIELDS.state);
        String county = fieldArr.get(_FIELDS.county);
        String acceptableCity = fieldArr.get(_FIELDS.acceptableCity);

        String fullZip = fiveDigit(zip);

        //processes lines with only one city for given zip
        if(type.equals("STANDARD") && acceptableCity.equals("") && !county.equals(""))
        {
            Zipcode zpcd = new Zipcode(fullZip, city, state, county);
            _zipcodeMap.put(count, zpcd);
        }
        //this block processes lines that have only one addt'l city for given zip
        else if(type.equals("STANDARD") && !acceptableCity.equals("") && !acceptableCity.contains(",") && !county.equals(""))
        {
            //passes primary city into map
            Zipcode zpcd = new Zipcode(fullZip, city, state, county);
            _zipcodeMap.put(count, zpcd);
            city = acceptableCity; //acceptable name swapped in place of primary name
            count++;
            //addit'l city passed to map
            Zipcode zipcode = new Zipcode(fullZip, city, state, county);
            _zipcodeMap.put(count, zipcode);
        }
        else if(type.equals("STANDARD") && !acceptableCity.equals("") && acceptableCity.contains(",") && !county.equals(""))
        {
            multiCity(fieldArr);
        }
        return _zipcodeMap;	
    }
	
	public static HashMap<Integer,Object> multiCity(ArrayList<String> fieldArr)
	{
        /* multiCity: processes lines with multiple, comma-delimited acceptable
         * cities. */

		int count = _zipcodeMap.size();
		
		String zip  = fieldArr.get(_FIELDS.zip);
 	  //String type = fieldArr.get(_FIELDS.type); //why is this line commented out?
		String city = fieldArr.get(_FIELDS.city);
		String state  = fieldArr.get(_FIELDS.state);
		String county = fieldArr.get(_FIELDS.county);
		String acceptableCity = fieldArr.get(_FIELDS.acceptableCity);
		
		String fullZip = fiveDigit(zip);
	
		//creates hashmap entry for primary city
		Zipcode zpcd = new Zipcode(fullZip, city, state, county);
		_zipcodeMap.put(count, zpcd);
		count++;
	
		String[] cities = acceptableCity.split(",");
		for(String ciudad : cities) {
			//creates entry for each addt'l city name for given zipcode
			Zipcode zipcode = new Zipcode(fullZip, ciudad.trim(), state, county);
			_zipcodeMap.put(count, zipcode);
			//logger.info("acceptable city added: "+ciudad+", "+zip);
			count++;
		}
		return _zipcodeMap;	
	}
 
	public static String fiveDigit(String zip)
	{
        /* fiveDigit: extracts the primary zip code (the 90210 in 90210-1234),
         * discarding the +4 addons. If the remaining zip code is less than 5
         * digits long, it is zero-padded on the left. */
		
		String[] zipSplit = zip.split("\\."); //TODO: is this correct?
		zip= zipSplit[0];
		
        //Perhaps the following could be replaced with this?:
        //while ( zip.length() < 5 ) zip += "0"+zip;
        //return zip;

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
	
	public static void useBufferedWriter(List<String> content, String path)
    {
        /* useBufferedWriter: creates/appends file at `path' with string
         * contents `content'. */

    	File file = new File(path);
    	Writer fileWriter = null;
    	BufferedWriter bufferedWriter = null;
    	
    	try {
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
