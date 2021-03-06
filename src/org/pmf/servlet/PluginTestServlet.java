package org.pmf.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XLog;
import org.pmf.eao.PluginEao;
import org.pmf.entity.Invoke;
import org.pmf.entity.Plugin;
import org.pmf.plugin.service.PluginService;

/**
 * Servlet implementation class PluginTestServlet
 */
@WebServlet("/PluginTestServlet")
public class PluginTestServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	@EJB
	private PluginEao pluginEao;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public PluginTestServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		this.processRequest(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		this.processRequest(request, response);
	}
	
	private void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
//		Map<String, Map<String, String>> pluginContext = new HashMap<String, Map<String, String>>();
//		Map<String, String> snhow = new HashMap<String, String>();
//		Map<String, String> alphaplus = new HashMap<String, String>();
//		alphaplus.put("jar", "AlphaPlusMiner_fat.jar");
//		alphaplus.put("service_class", "org.pmf.tools.alphaminer.AlphaPluginService");
//		snhow.put("jar", "HoWMiner_fat.jar");
//		snhow.put("service_class", "org.pmf.tools.snminer.mining.SNHOWPluginService");
//		pluginContext.put("alphaplus", alphaplus);
//		pluginContext.put("snhow", snhow);
		
		String apiKey = request.getParameter("apiKey");
		System.out.println("apiKey="+apiKey);
		
//		String confPath = this.getServletContext().getRealPath("/WEB-INF/conf");
		Plugin plugin = pluginEao.findAvailablePluginByApiKey(apiKey);
		
		if (plugin == null) {
			JSONObject json = new JSONObject();
			json.element("status", "ERROR");
			response.setContentType("application/json; charset=utf-8");		
			PrintWriter out = response.getWriter();
			out.print(json);
			return;
		}
		
//		if (!pluginContext.containsKey(apiKey)) {
//			JSONObject json = new JSONObject();
//			json.element("status", "ERROR");
//			response.setContentType("application/json; charset=utf-8");		
//			PrintWriter out = response.getWriter();
//			out.print(json);
//			return;
//		}
		
		//得到上传文件的保存目录，将上传的文件存放于WEB-INF目录下，不允许外界直接访问，保证上传文件的安全
        String savePath = this.getServletContext().getRealPath("/WEB-INF/upload");
        File saveDir = new File(savePath);
        //判断上传文件的保存目录是否存在
        if (!saveDir.exists() && !saveDir.isDirectory()) {
            System.out.println(savePath+"目录不存在，需要创建");
            //创建目录
            saveDir.mkdir();
        }
        //消息提示
        String message = "";
        String filename = "";
        File file = null;
        Map<String,String> params = new HashMap<String,String>();
        try{
            //使用Apache文件上传组件处理文件上传步骤：
            //1、创建一个DiskFileItemFactory工厂
            DiskFileItemFactory factory = new DiskFileItemFactory();
            //2、创建一个文件上传解析器
            ServletFileUpload upload = new ServletFileUpload(factory);
             //解决上传文件名的中文乱码
            upload.setHeaderEncoding("UTF-8"); 
            //3、判断提交上来的数据是否是上传表单的数据
            if(!ServletFileUpload.isMultipartContent(request)){
                //按照传统方式获取数据
                return;
            }
            //4、使用ServletFileUpload解析器解析上传数据，解析结果返回的是一个List<FileItem>集合，每一个FileItem对应一个Form表单的输入项
            List<FileItem> list = upload.parseRequest(request);
            for(FileItem item : list){
                //如果fileitem中封装的是普通输入项的数据
                if(item.isFormField()){
                    String name = item.getFieldName();
                    //解决普通输入项的数据的中文乱码问题
                    String value = item.getString("UTF-8");
                    params.put(name, value);
                    System.out.println(name + "=" + value);
                }else{//如果fileitem中封装的是上传文件
                    //得到上传的文件名称，
                    filename = item.getName();
                    System.out.println(filename);
                    if(filename==null || filename.trim().equals("")){
                        continue;
                    }
                    //注意：不同的浏览器提交的文件名是不一样的，有些浏览器提交上来的文件名是带有路径的，如：  c:\a\b\1.txt，而有些只是单纯的文件名，如：1.txt
                    //处理获取到的上传文件的文件名的路径部分，只保留文件名部分
                    if (!filename.endsWith(".xes")) {
                    	message= "仅支持.xes文件";
                        JSONObject json = new JSONObject();
            			json.element("status", "ERROR");
            			response.setContentType("application/json; charset=utf-8");		
            			PrintWriter out = response.getWriter();
            			out.print(json);
            			return;
                    }
                    filename = UUID.randomUUID().toString().replaceAll("-", "") + ".xes";
                    file = new File(savePath,filename);
                    item.write(file);
                    //删除处理文件上传时生成的临时文件
                    item.delete();
                    message = "文件上传成功！";
                }
            }
        }catch (Exception e) {
            message= "文件上传失败！";
            e.printStackTrace();
            JSONObject json = new JSONObject();
			json.element("status", "ERROR");
			response.setContentType("application/json; charset=utf-8");		
			PrintWriter out = response.getWriter();
			out.print(json);
			return;
            
        }
        
		XesXmlParser parser =  new XesXmlParser();
		InputStream inputfile;
		try {
			if (file == null) {
				JSONObject json = new JSONObject();
				json.element("status", "ERROR");
				response.setContentType("application/json; charset=utf-8");		
				PrintWriter out = response.getWriter();
				out.print(json);
				return;
			}
			inputfile = new FileInputStream(file);
			List<XLog> logs = parser.parse(inputfile);
			if (logs != null && !logs.isEmpty()) {
				XLog log = logs.get(0);
				String pluginPath = this.getServletContext().getRealPath("/WEB-INF/plugin");
				System.out.println("file:///"+pluginPath+"/"+plugin.getJarName());
				ClassLoader cl = new URLClassLoader(new URL[] {new URL("file:///"+pluginPath+"/"+plugin.getJarName())}, this.getClass().getClassLoader());
				Class<?> c = Class.forName(plugin.getServiceClass(), true, cl);
				Class<? extends PluginService> pluginClass = c.asSubclass(PluginService.class);
//				Object o = c.newInstance();
//				if (o instanceof PluginService) {
//					System.out.println("yes");
//				} else {
//					System.out.println("no");
//				}
				PluginService service = pluginClass.newInstance();
				JSONObject json = service.doPluginService(log, params);
				response.setContentType("application/json; charset=utf-8");
				PrintWriter out = response.getWriter();
				out.print(json);				
				// update plugin
				plugin.addInvoke(new Invoke(new Date()));
				pluginEao.update(plugin);
				return;
			} else {
				JSONObject json = new JSONObject();
				json.element("status", "ERROR");
				response.setContentType("application/json; charset=utf-8");		
				PrintWriter out = response.getWriter();
				out.print(json);
				return;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			JSONObject json = new JSONObject();
			json.element("status", "ERROR");
			response.setContentType("application/json; charset=utf-8");		
			PrintWriter out = response.getWriter();
			out.print(json);
			return;
		}
	}

}
