package com.itheima.web.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.itheima.domain.Book;
import com.itheima.domain.Category;
import com.itheima.domain.Customer;
import com.itheima.domain.Orders;
import com.itheima.domain.OrdersItem;
import com.itheima.service.BookService;
import com.itheima.service.CategoryService;
import com.itheima.service.CustomerService;
import com.itheima.service.OrdersService;
import com.itheima.service.impl.BookServiceImpl;
import com.itheima.service.impl.CategoryServiceImpl;
import com.itheima.service.impl.CustomerServiceImpl;
import com.itheima.service.impl.OrdersServiceImpl;
import com.itheima.utils.IDGenerator;
import com.itheima.utils.PageBean;
import com.itheima.utils.SendMailTreadUtils;
import com.itheima.utils.WebUtils;
import com.itheima.web.form.Cart;
import com.itheima.web.form.CartItem;

public class ClientServlet extends HttpServlet {
	
	private BookService bs = new BookServiceImpl();
	private CategoryService cs = new CategoryServiceImpl();
	private CustomerService customerservice = new CustomerServiceImpl();
	private OrdersService os = new OrdersServiceImpl();
	
	
	private static String SHOWINDEX = "showIndex";
	private static String BUYBOOK = "buyBook";
	private static String SHOWCATEGORYPAGERECORDS = "showCategoryPageRecords";
	private static String LOGIN = "login";
	private static String LOGOUT = "logout";
	private static String REGIST = "regist";
	private static String ACTIVED = "actived";
	private static String GENORDERS = "genOrders";
	private static String SHOWORDERS = "showOrders";
	private static String PAYUI = "payUI";
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
			request.setCharacterEncoding("UTF-8");
			response.setContentType("text/html;charset=UTF-8");
			
			String op = request.getParameter("op");
			
			if(SHOWINDEX.equals(op)){
				showIndex(request,response);
			}else if(SHOWCATEGORYPAGERECORDS.equals(op)){
				showCategoryPageRecords(request,response);
			}else if(BUYBOOK.equals(op)){
				buyBook(request,response);
			}else if(LOGIN.equals(op)){
				login(request,response);
			}else if(LOGOUT.equals(op)){
				logout(request,response);
			}else if(REGIST.equals(op)){
				regist(request,response);
			}else if(ACTIVED.equals(op)){
				actived(request,response);
			}else if(GENORDERS.equals(op)){
				genOrders(request,response);
			}else if(SHOWORDERS.equals(op)){
				showOrders(request,response);
			}else if(PAYUI.equals(op)){
				payUI(request,response);
			}
	}

	/**
	 * 查看订单中的未付款中的购买按钮
	 * @param request
	 * @param response
	 * @throws IOException 
	 * @throws ServletException 
	 */
	private void payUI(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//获取参数
		String ordernum = request.getParameter("ordernum");
		Orders o = os.getOrdersByOrdernum(ordernum);
		//把订单保存到reqest并转发到pay.jsp去支付
		request.setAttribute("o", o);
		request.getRequestDispatcher("/pay.jsp").forward(request, response);
		
	}

	/**
	 * 首页查看购买成功订单
	 * @param request
	 * @param response
	 * @throws IOException 
	 * @throws ServletException 
	 */
	private void showOrders(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		//1.在session中获取用户
		Customer c = (Customer) request.getSession().getAttribute("user");
		if(c==null){
			response.getWriter().write("您没有登陆，2秒后调转到登陆页");
			response.setHeader("Refresh", "2;URL="+request.getContextPath()+"/login.jsp");
			return ;
		}
		//2.取出用户所有订单的数据(订单集合)
		List<Orders> list = os.getAllOrdersByCustomerId(c.getId());
		request.setAttribute("list", list);
		
		//3.转发给页面(显示所有)
		request.getRequestDispatcher("/listOrders.jsp").forward(request, response);
	}

	/**
	 * 去结算下的生成订单：生成当前登陆下的订单
	 * @param request
	 * @param response
	 * @throws IOException 
	 * @throws ServletException 
	 */
	private void genOrders(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		//1.如果用户没登陆，不能生成订单
		HttpSession session = request.getSession();
		Customer c = (Customer) session.getAttribute("user");
		if(c==null){
			response.getWriter().write("您没有登陆，2秒后调转到登陆页");
			response.setHeader("Refresh", "2;URL="+request.getContextPath()+"/login.jsp");
			return ;
		}
		
		//2.如果登陆了，生成订单
		//要生成订单，就必须拿到购物车
		Cart cart = (Cart) session.getAttribute("cart");
		//购物车对应订单（Orders）   CartItem对应（OrdersItem）
		Orders o = new Orders();
		o.setId(IDGenerator.genId());
		o.setC(c);
		o.setNum(cart.getTotleNum());//总数量
		o.setPrice(cart.getTotlePrice());//总价格
		o.setOrdernum(IDGenerator.genCode());
		o.setStatus(0);//0代表未付款
		
		//生成订单明细(Orders setItems(list);)
		HashMap<String,CartItem> map = (HashMap<String, CartItem>) cart.getMap();
		List<OrdersItem> list = new ArrayList<OrdersItem>();
		for(Map.Entry<String, CartItem> item:map.entrySet()){
			CartItem ci = item.getValue();//得到一个明细
			OrdersItem oi = new OrdersItem();//生成一个订单明细
			oi.setBook(ci.getBook());
			oi.setNum(ci.getNum());
			oi.setPrice((float) ci.getTotalPrice());
			oi.setId(IDGenerator.genId());
			list.add(oi);
		}
		
		//为订单添加订单明细
		o.setItems(list);
		
		//保存订单及订单明细
		boolean flag = os.saveOrders(o);
		
		if(flag){
			//清除购物车中原有的数据
			request.getSession().removeAttribute("cart");
			request.setAttribute("o", o);//存储订单给jsp使用
			//转发到支付页面（pay.jsp）
			request.getRequestDispatcher("/pay.jsp").forward(request, response);
		}else{
			response.getWriter().write("订单生成失败，2秒后回到主页");
			response.setHeader("Refresh", "2;URL="+request.getContextPath());
		}
	}

	/**
	 * 注册用户时激活用户
	 * @param request
	 * @param response
	 * @throws IOException 
	 */
	private void actived(HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		//1.得到激活码
		String code = request.getParameter("code");
		//2.根据激活码查询数据库完之后告诉用户是否注册成功
		boolean flag = customerservice.active(code);
		if(flag){
			//激活成功
			//2.提示用户激活并进入到主页面
			response.getWriter().write("激活成功，2秒后跳转到登录面");
			response.setHeader("Refresh", "2;URL="+request.getContextPath()+"/login.jsp");
		}else{
			//激活失败
			response.getWriter().write("激活失败，请确认激活码并重新激活");
			response.setHeader("Refresh", "2;URL="+request.getContextPath());
		}
	}

	/**
	 * 页面注册
	 * @param request
	 * @param response
	 * @throws IOException 
	 */
	private void regist(HttpServletRequest request, HttpServletResponse response) throws IOException {
		//1.封装注册页面的参数
		Customer c = WebUtils.fillBean(request, Customer.class);//注意里面的值不全。一部分值还要手动赋值
		
		//2.设置其他信息
		c.setId(IDGenerator.genId());
		c.setCode(IDGenerator.genCode());
		c.setActived(0);//未激活
		c.setRole(0);//普通用户
		
		Boolean flag = customerservice.regist(c);
		
		if(flag){
			//注册成功
			//1.启动发送邮件的线程
			SendMailTreadUtils sendMain = new SendMailTreadUtils(c);
			sendMain.start();
			
			//2.提示用户激活并进入到主页面
			response.getWriter().write("注册成功，请登录邮箱进行激活，2秒后跳转到登录面");
			response.setHeader("Refresh", "2;URL="+request.getContextPath()+"/login.jsp");
		}else{
			//注册失败
			response.getWriter().write("注册失败，用户名可能存在，2秒后跳转到注册页面重新注册");
			response.setHeader("Refresh", "2;URL="+request.getContextPath()+"/regist.jsp");
		}
	}

	/**
	 * 注销
	 * @param request
	 * @param response
	 * @throws IOException 
	 */
	private void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
		//清除session
		request.getSession().removeAttribute("user");
		response.sendRedirect(request.getContextPath());
	}

	/**
	 * 用户登陆
	 * @param request
	 * @param response
	 * @throws IOException 
	 */
	private void login(HttpServletRequest request, HttpServletResponse response) throws IOException {
		//1.获取参数
		String username = request.getParameter("username");
		String password = request.getParameter("password");
		//2.调用业务逻辑，实现查询操作
		Customer c = customerservice.login(username,password);
		//3.判断结果
		if(c==null){
			//5.如果失败，返回登陆页，继续登陆
			response.getWriter().write("登陆失败，可能是用户名，密码，未激活原因引起的");
			response.setHeader("Refresh", "2;URL="+request.getContextPath()+"/login.jsp");
			return ;
		}else{
			//4.如果正确，将用户信息保存到session,user的名字要与header.jsp中保持一致
			request.getSession().setAttribute("user", c);
			response.sendRedirect(request.getContextPath());//回到首页
		}
	}

	/**
	 * 点击购买，页面不动，用户可以手动点击购物车去查看记录，这样会使用到无刷新技术
	 * @param request
	 * @param response
	 * @throws ServletException
	 * @throws IOException
	 */
	private void buyBook(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		//1.得到书的id，转化为一个Book对象
		String bookId = request.getParameter("bookId");
		Book book = bs.findOne(bookId);
		//2.将书添加到购物车
		HttpSession session = request.getSession();
		Cart cart = (Cart)session.getAttribute("cart");//第一次使用时，不存在
		if(cart == null){
			cart = new Cart();
			session.setAttribute("cart", cart);
		}
		//3.将book对象放入购物车
		cart.addBooks2Cart(book, 1);
	
		//4.再调用查询所有书籍的操作
		showIndex(request, response);
	}

	private void showCategoryPageRecords(HttpServletRequest request,
			HttpServletResponse response) {
		
	}

	private void showIndex(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		//1.得到分页信息列表
		List<Category> list = cs.findAllCategorys();
		//2.得到图书列表，要分页
		PageBean pb = new PageBean();
		String pageNo = request.getParameter("pageNo");
		if(pageNo!=null && !"".equals(pageNo)){
			pb.setPageNo(Integer.valueOf(pageNo));
		}
		
		//3.分页查询
		bs.findBooksByPageBean(pb);
		pb.setUrl(request.getContextPath()+"/servlet/ClientServlet?op=showIndex");
		
		//4.放到域中
		request.setAttribute("cs", list);
		request.setAttribute("page", pb);
		
		//5.转发
		request.getRequestDispatcher("/listBooks.jsp").forward(request, response);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		this.doGet(request, response);
	}

}
