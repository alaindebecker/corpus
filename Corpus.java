import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.concurrent.Executors;
import java.util.*;

public class Corpus implements HttpHandler{

    static int port = 8421;
    static Statement db;
    
    @Override
    public void handle(HttpExchange e) throws IOException {
        System.out.println(e.getRequestURI());
        String response = "";
        Properties map = new Properties();
        map.load(new StringReader(e.getRequestURI().getQuery()));
        if(map.containsKey("word"))
            response = searchWord(map.getProperty("word"));
        if(map.containsKey("char"))
            response = searchChar(map.getProperty("char"));
        if(map.containsKey("doc"))
            response = searchDoc(map.getProperty("doc"));
        System.out.println("send response");
        
        Headers responseHeaders = e.getResponseHeaders();
        responseHeaders.set("Content-Type", "text/plain; charset=utf-8");
        responseHeaders.set("Access-Control-Allow-Origin", "*");
        e.sendResponseHeaders(200, 0);
        OutputStream out = e.getResponseBody();
        out.write(response.getBytes());
        out.close();
    }

    /** Returns the list of (id, filename) containing the given word. **/ 
    public String searchWord(String word){
        String res = "";
        try{
            String sql = "SELECT id, rubrique, filename FROM doc JOIN doc_word ON doc.id=doc WHERE word='"+word+"';";
            ResultSet rs = db.executeQuery(sql);
            while(rs.next()){
                String path = rs.getString(3);
                int p = path.lastIndexOf("/");
                String file = path.substring(p+1); 
                if(!res.isEmpty())
                    res += ",";
                res += "{\"id\" : "+rs.getInt(1)+", \"file\" : \""+rs.getString(2)+" - "+file+"\" }";
            }
        }catch(SQLException err){}
        return "["+res+"]";
    } 
    
    /** Returns the list of (id, filename) containing the given character. **/ 
    public String searchChar(String ch){
        String res = "";
        try{
            String sql = "SELECT id, filename FROM doc JOIN doc_char ON doc.id=doc WHERE ch='"+ch+"';";
            System.out.println(sql);
            ResultSet rs = db.executeQuery(sql);
            while(rs.next()){
                String path = rs.getString(2);
                int p = path.lastIndexOf("/");
                String file = path.substring(p+1); 
                if(!res.isEmpty())
                    res += ",";
                res += "{\"id\" : "+rs.getInt(1)+", \"file\" : \""+file+"\" }";
            }
        }catch(SQLException err){}
        return "["+res+"]";
    } 
    
    /** Returns the filename of a given doc_id containing the given word. **/ 
    public String searchDoc(String doc){
        String res = "";
        try{
            String sql = "SELECT filename FROM doc WHERE id="+doc+";";
            ResultSet rs = db.executeQuery(sql);
            if(rs.next())
                res = rs.getString(1);
        }catch(SQLException err){}
        return res;
    } 
    
    public static void main(String[] args) {
        InetSocketAddress addr = new InetSocketAddress(8421);
        try {
//            db = DriverManager.getConnection("jdbc:mysql://localhost:3306/corpus", "root", "").createStatement();
            db = DriverManager.getConnection("jdbc:sqlserver://localhost:1433;databaseName=corpus;integratedSecurity=true").createStatement();
            
            HttpServer server = HttpServer.create(addr, 0);
            server.createContext("/", new Corpus());
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            System.out.println("Server is listening on localhost:"+port);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }    
}
