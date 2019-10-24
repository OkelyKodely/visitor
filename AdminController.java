package org.o7planning.springmvcshoppingcart.controller;
 
import java.io.ByteArrayInputStream;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import org.hibernate.SessionFactory;
import org.o7planning.springmvcshoppingcart.dao.OrderDAO;
import org.o7planning.springmvcshoppingcart.dao.ProductDAO;
import org.o7planning.springmvcshoppingcart.entity.Product;
import org.o7planning.springmvcshoppingcart.model.OrderDetailInfo;
import org.o7planning.springmvcshoppingcart.model.OrderInfo;
import org.o7planning.springmvcshoppingcart.model.PaginationResult;
import org.o7planning.springmvcshoppingcart.model.ProductInfo;
import org.o7planning.springmvcshoppingcart.validator.ProductInfoValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.commons.CommonsMultipartFile;
import org.springframework.web.multipart.support.ByteArrayMultipartFileEditor;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
 
@Controller
// Enable Hibernate Transaction.
@Transactional
// Need to use RedirectAttributes
@EnableWebMvc
public class AdminController {
 
    @Autowired
    private OrderDAO orderDAO;
 
    @Autowired
    private ProductDAO productDAO;
 
    @Autowired
    private ProductInfoValidator productInfoValidator;
 
    // Configurated In ApplicationContextConfig.
    @Autowired
    private ResourceBundleMessageSource messageSource;
 
    private SessionFactory sessionFactory;

    @InitBinder
    public void myInitBinder(WebDataBinder dataBinder) {
        Object target = dataBinder.getTarget();
        if (target == null) {
            return;
        }
        System.out.println("Target=" + target);
 
        if (target.getClass() == ProductInfo.class) {
            dataBinder.setValidator(productInfoValidator);
            // For upload Image.
            dataBinder.registerCustomEditor(byte[].class, new ByteArrayMultipartFileEditor());
        }
    }
 
    // GET: Show Login Page
    @RequestMapping(value = { "/login" }, method = RequestMethod.GET)
    public String login(Model model) {
 
        return "login";
    }
 
    @RequestMapping(value = { "/accountInfo" }, method = RequestMethod.GET)
    public String accountInfo(Model model) {
 
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        System.out.println(userDetails.getPassword());
        System.out.println(userDetails.getUsername());
        System.out.println(userDetails.isEnabled());
 
        model.addAttribute("userDetails", userDetails);
        return "accountInfo";
    }
 
    @RequestMapping(value = { "/orderList" }, method = RequestMethod.GET)
    public String orderList(Model model, //
            @RequestParam(value = "page", defaultValue = "1") String pageStr) {
        int page = 1;
        try {
            page = Integer.parseInt(pageStr);
        } catch (Exception e) {
        }
        final int MAX_RESULT = 5;
        final int MAX_NAVIGATION_PAGE = 10;
 
        PaginationResult<OrderInfo> paginationResult //
        = orderDAO.listOrderInfo(page, MAX_RESULT, MAX_NAVIGATION_PAGE);
 
        model.addAttribute("paginationResult", paginationResult);
        return "orderList";
    }
 
    // GET: Show product.
    @RequestMapping(value = { "/product" }, method = RequestMethod.GET)
    public String product(Model model, @RequestParam(value = "code", defaultValue = "") String code) {
        ProductInfo productInfo = null;
 
        if (code != null && code.length() > 0) {
            productInfo = productDAO.findProductInfo(code);
        }
        if (productInfo == null) {
            productInfo = new ProductInfo();
            productInfo.setNewProduct(true);
        }
        model.addAttribute("productForm", productInfo);
        return "product";
    }
 
    // POST: Save product
    @RequestMapping(value = { "/product" }, method = RequestMethod.POST)
    // Avoid UnexpectedRollbackException (See more explanations)
    @Transactional(propagation = Propagation.NEVER)
    public String productSave(Model model, //
            @ModelAttribute("productForm") @Validated ProductInfo productInfo, //
            BindingResult result, //
            final RedirectAttributes redirectAttributes) {
 
//        if (1==2&&result.hasErrors()) {
//            return "product";
//        }
        try {
            if (productInfo.getBulkData() != null) {
                	uploadBulkData(productInfo.getBulkData());
            } else {
            	productDAO.save(productInfo);
            }
        } catch (Exception e) {
            // Need: Propagation.NEVER?
            String message = e.getMessage();
            model.addAttribute("message", message);
            // Show product form.
            return "product";
 
        }
        return "redirect:/productList";
    }

    private void uploadBulkData(CommonsMultipartFile file) {

    	String bulkData = file.getFileItem().getString();

    	StringTokenizer st = new StringTokenizer(bulkData, System.getProperty("line.separator"));

		String hostName = "localhost:3320";
		String dbName = "mydb";
		String userName = "root";
		String passWord = "";

		Connection conn = null;

		PreparedStatement st1=null ;

		try {

    		try {
    		    Class.forName("com.mysql.jdbc.Driver");
    		    String url = "jdbc:mysql://" + hostName + "/" + dbName;
    		    conn = DriverManager.getConnection(url, userName, passWord);
    		} catch(Exception e) {
    			e.printStackTrace();
    		}

    		st1 = conn.prepareStatement("insert into products (code, create_date, price, name, image) values (?,now(),?,?,?)");

    		int codeIndex =1;
    		
    		while (true) {
    			
	    		StringTokenizer st2 = new StringTokenizer(st.nextToken(), ",");
	        	String code = st2.nextToken();
	        	String name = st2.nextToken();
	        	Double price = Double.valueOf(st2.nextToken());
	            
	            Product product = null;
	     
	            product = new Product();
	            product.setCreateDate(new Date());
	            product.setCode(code);
	            product.setName(name);
	            product.setPrice(price);
	            
	            String rawDataString = new String("/9j/4AAQSkZJRgABAQEAZABkAAD/2wBDAAQCAwMDAgQDAwMEBAQEBQkGBQUFBQsICAYJDQsNDQ0L\r\n" + 
	            		"DAwOEBQRDg8TDwwMEhgSExUWFxcXDhEZGxkWGhQWFxb/wgALCAQABAABAREA/8QAHAABAQEAAwEB\r\n" + 
	            		"AQAAAAAAAAAAAAgHBAUGAwIB/9oACAEBAAAAAd/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAD8fz6AAAAAAAAAAAAAAAAYJif3uBiWCqm0F5KRW0b+eFlJ21\r\n" + 
	            		"ohk+HeN/HobL7KQ/KDQamAAAAAAAAAAAAAAE74RyL3TthKgd+YxM7ZKbMLnQujtycMPHZXZ/Yw8e\r\n" + 
	            		"NHrgAAAAAAAAAAAAAATvhHIvdMeONJrZMOPNUqslTK+fwK20ll0ovX753PX6WjDx/vKDdl64AAAA\r\n" + 
	            		"AAAAAAAAAATvhHIvdKGXuxuz9RT5ZotdEO9FqeWULvSOPD9rbHZBGHj9PrAAAAAAAAAAAAAAACd8\r\n" + 
	            		"I5F7o+8D2vVWt3MJdj1ntbKdJDfY77OOp1Z1kKfzb6QBGHj9PrAAAAAAAAAAAAAAACd8I5F7or8p\r\n" + 
	            		"pWa0730iaJnfprZZbKXvN5kr0Vu+GjlUGs5Ke89JGHj+V2zYd+AAAAAAAAAAAAAAE74RyL3Qz01C\r\n" + 
	            		"z1s3op3oGfu2ulO2E7TukRf27fESQq/QIQKf2CMPHjb6QAAAAAAAAAAAAAAE74RyL3QN8bKjX2Hq\r\n" + 
	            		"cqr2PvvfCP8AwFD69EHxr7+yAqfTZiy35U/sEYeP0Gjna92AAAAAAAAAAAAAAJ3wjkXvwoN/V9wt\r\n" + 
	            		"wfSc2oomXt9YP4gUZqcQKA39CHX0/sEYeP0+sAAAAAAAAAAAAAAAJ3wjkXv0EP8ANvKT8wbpuMNL\r\n" + 
	            		"n6eMHpf30nC1uo4g6D1Vo/SEOvp/YIw8fp9YAAAAAAAAAAAAAAATvhHIvfxsZdzc2GTmrLQINWv4\r\n" + 
	            		"CZ+ZeP6nDD/U2thM7NY2mSuHT+wRh4/1+0PvsIAAAAAAAAAAAAAAnfCORe+dyJ6W2vERv+rr7CBP\r\n" + 
	            		"zY+M497uxGQS/wDu8frJedBT+wRh48dhd4AAAAAAAAAAAAAAxnJfvXPgJ+9JTnyn3sN3STxaMxjz\r\n" + 
	            		"Wnbk8vM6mfT/ACxjK+h5XqN89hMfmxzazAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAB5fNer9DsX7+GUeT7rXO0PGeKap2jrMqe09oxr8e09p5vO/wC7H9GLtU7Q+edeK+Xp\r\n" + 
	            		"tW+4AAAAAAAAAAAAPxOOJfx2N28GQvGHcWF6VOuFKn1hlUqt2olBHH3+gMnlhWemIAWf7B5aU/Kn\r\n" + 
	            		"7vHlgAAAAAAAAAAAAnrBXee37OmZmxn66lmHH0OvEw482+kE5Ya2amUEcff6AyeWGn1ggBZ/sOpi\r\n" + 
	            		"/pnq+6/ldgAAAAAAAAAAAA6OIfl7mu+W6yGfjvNDYBP63vQyhl729kI78I1eqEEcff6AyeWH0uDu\r\n" + 
	            		"4AWf7Ca8UU9sIAAAAAAAAAAAABiE3rA9+ZXKiyPb+HjhUWux74L6fq8f1B/z+Wj1wgjj7/QGTyx+\r\n" + 
	            		"/wAUJvcC/Kz/AFsKddoldgAAAAAAAAAAAACW8k+96fQwKfV19p08MKA3+KvK+v8AIWJ9409Z5P3F\r\n" + 
	            		"joI4+/0Bk8sew8n3tuQdw7P+0VKO3EAAAAAAAAAAAAASHnne3EJww9fP24kFt0ouGOn2fGKM5U07\r\n" + 
	            		"FjvqLXQRx9/oDJ5Y91ys7r2Weos/q5EVZosXFF6oAAAAAAAAAAAAEg593txCccOXx9+DBzeaGgb4\r\n" + 
	            		"1NLOq9jjNRS73NzII4+/0Bk8sex22Ydd8N5uz+okdV2gwgU/sAAAAAAAAAAAAASplf2vT6mCT2un\r\n" + 
	            		"tuhh5RezQd9Llhf0Pb9RU0b8m9kEcff6AyeWPV2PDLvvK2f840Uzsfmo14VP7AAAAAAAAAAAAAGB\r\n" + 
	            		"T6rrRTLZSWF73PZCVl62IOyu2J/M8zU90itfP2gjj7/QGTyx6i15mxn9fmz/AEsIfLXKjQh19P7A\r\n" + 
	            		"AAAAAAAAAAAAHk4setrjuv51sMfLaqVmvFPtdHnoy724pjxxTuiw6untoI4+/wBAZPLHqLX8bGRZ\r\n" + 
	            		"/sJMzP72N7KEOvp/YAAAAAAAAAAAAAS3kj79/wCkrWY8c/vsPH/zZKbzmRvVWrkEvrb7OEVs+mgj\r\n" + 
	            		"j7/QGTyx6i10aeLWf7Dx0cfDke68D86f2AAAAAAAAAAAAAHFmHKD0NvcCSfCnvK05+Tyx7WyvORH\r\n" + 
	            		"3VyfiBFle1gjj7/QGTyx6i12NzIs/wBgzeYemFQ68AAAAAAAAAAAAA8z4z8em9s/Ge+f9J7/APTp\r\n" + 
	            		"vMdh7Bx/793gP57DsPA/n0HoOp8ryvbON4h7TmHw8L0XJ9T6r9AAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAD8v0H84vLA/nF5YAAAAAAAAAAAAAAAAAMowjyP695Xz+YThfV99ROsH8w3COq72h9bdTF\r\n" + 
	            		"yltRSj4agtlAAAAAAAAAAAAAAAGRS6PZWawWeh+q70JhU6j+1xo3Tww0auUf+Ao/cAAAAAAAAAAA\r\n" + 
	            		"AAAABE3mfZUx9em0nqoe42tbvI/Te9sLrYd4mrb5JXRe4sfp4Yfq3PQx/wCAo/cAAAAAAAAAAAAA\r\n" + 
	            		"AAD4wMpDbxjMzLa9LPWC/q6sxmFavqp/wD+3P/IYN7oSP/AUfuAAAAAAAAAAAAAAAAhXqvV1z3ZM\r\n" + 
	            		"OPdhd7KZWWBk2Mcy8v7l0oq79ZDHJ/fOuGR88o/cAAAAAAAAAAAAAAABgc+OxorZEi516e2GeyEq\r\n" + 
	            		"rHs29FbrwceKk0OGP3qOV1ljOZ0fuAAAAAAAAAAAAAAAA/Ew5Cb9QMf+A9hZ7P5AVPi/gPUWu8PH\r\n" + 
	            		"CoNHhhUstal98mo/cAAAAAAAAAAAAAAAA/mSzVwP1a03Zx6W2mcyMrHGsz9DbzPpBVZ7uGF0xPxN\r\n" + 
	            		"Pyqj9wAAAAAAAAAAAAAAAA8DHyk/IY3zbx/uQy8s3G8W5N6frJpZWR38MLvn7Eez6yj9wAAAAAAA\r\n" + 
	            		"AAAAAAAAA4sFKP5UzLS9bNmJ8m7smmFZvspww/73dwYYXb0sWFH7gAAAAAAAAAAAAAAAJz9hzsgy\r\n" + 
	            		"1Znew9x9D2iYuDrFT9ZDvF0Ha5j6/U6s6eGFz9xHng1H7gAAAAAAAAAAAAAAAcODf4Nnphgk9jnW\r\n" + 
	            		"Z6VhE7jnWT6jp4YXP3GRy4o/cAAAAAAAAAAAAAAADrpw8bxfR67rf9fzFsZ6z2VCetP5jGMdX7Ch\r\n" + 
	            		"PXutlFV3ZceTvhtmqAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAf\r\n" + 
	            		"/8QAMBAAAAUEAAQGAwACAgMAAAAAAgMEBQYAAQcQEhMUNhUWIDQ1YBEXISImI6AksMD/2gAIAQEA\r\n" + 
	            		"AQUC/wDfljEENgDAP7FmZWrTKfFnSkrq6XU6zMtUJU3izpXizpWMVw18R1Oll0ET8WdK8WdKw0uV\r\n" + 
	            		"qV28jrRIIh4s6V4s6UwvbgS9+mVTpA2jdpa/rxGmDMFa/wCLtz88oRMwzzGipy5OJUt8WdK8WdK8\r\n" + 
	            		"WdK8WdKxgvXnzH6xnH3VJPdazgP8rNYQVf46zUq5bFrCYv8AY95uVcKDbAp6xk3kmXDUGehmTdY7\r\n" + 
	            		"2/lqn/eXoxP3t9Yzj7qknutZrH+ZJrFCrpplrNCrmyLWHhcMv3mBVz5bS5KNNasUKeohmspvl2tl\r\n" + 
	            		"1FI4vfj2yAMScA4bGhBb4S1oX3U/7yrHLcjdJL5JjFeSYxTTGGNtW/WM4+6pJ7rWXh8cx00Kbo3W\r\n" + 
	            		"17XtU2VdZLNYqFwzjcnVdbIayci6PWEFP5I1kRxu5SumRAa5uzShTtrf6Z/3lWIe8vrecfdUk91r\r\n" + 
	            		"J4+OcU7JrpjKhKrrYo6qbI2sV7iEiT81FWOh8E11I1XRMFNBHVO2byOJsrD6nky6nlT0bRe/5vWE\r\n" + 
	            		"0VjHL1T/ALyrEPeX1vOPuqSe61OR8yYVPEnKaawuq5sdykq6aGUzpb2x1UMHwSzWXlXIiFY0T9RN\r\n" + 
	            		"MsEc6F1ElPRyaskmXKhGsLlcEYUGWJI/Y7JX7HZK/Y7JUXekr6gqf95UkUqEp3jr3Xjr3WHnBesc\r\n" + 
	            		"vrGcfdUk91qRD5sgrIaPixzWFVXLfs3qfwkro7kYSpgHy33Wb1X5V1hNPxvctI6mMVa/4u0qLLGv\r\n" + 
	            		"LHZOsQ9mu3xe8K9rVP8AvL0YQ+U+sZx91ST3WlI+aoqSIuqglQBT0kxzIo5sqJAI02aJgkY9pMPl\r\n" + 
	            		"qNZOVdVM6w6chSM43RpEBQDlqKxep6mF5RBxwfWGzOOJLixGov14/UeWIk+sK9rVP+8qxmiSr5R5\r\n" + 
	            		"Vj1eVY9TW0NraZ9Yzj7qknuqcR8tvoAbiH+LcL2l6F4TmCJUTdXZdK8dJOsmMzDxRPSMfMSGjCWW\r\n" + 
	            		"vPEqXenCCnibpUnurjesIK7bdvlawr2tU/7yrEPeX1vOPuqSe6qVD5cYpuDxuFZcSdPL9YRScS2R\r\n" + 
	            		"h449qPD5jBPVXRw/QI8+DB5cfqXpFSI+sNKeVKalKC7ZIahbr4PIixhMLp2+VrCva1T/ALyrEPeX\r\n" + 
	            		"1vOPuqSe6rIA+XDKjgeOQ1m5JxINYlSdND3APGg1Ch8yI5qVctipuIuqcA2sENZuTcLnUJU9JLaz\r\n" + 
	            		"IzXMI1Bpoa0FFTSNjKcBhMX1hXtap/3lUHdiWV9/ZDXX7Ia6TZEbDlH1jOPuqSe6rKg+CD1DQ8Us\r\n" + 
	            		"rIqTrIdQbXEJnS2RNV7WvYVrhFWOB8yE5oVc2RVjRN1M01mdNzY1QBCANAfZShUFFnkTuLnsar04\r\n" + 
	            		"V7Wqf95ehp+V+sZVYnR4UeSZPSeFyYKiskt65zjfkmT1EYm/o5LR5YTSf1odTTjwaV00shUjur8k\r\n" + 
	            		"yesfoljdFZhGJI5SbyTJ6xfG3Nqd9TRAY5xjyTJ68kyeoaQsTRmjiyzSpFjxKoGvhciSiEwvlrkR\r\n" + 
	            		"l/OpBAH4+8KY/AGipfE39bJfJMnryTJ68kyevJMnpvhkkLX/APTUfJC0NNLMlIwiDkw38tuRms4Y\r\n" + 
	            		"BWGClBpRBLvkJpTCFktTxtuSEZg2pxROSbcukiSP2/ZDXX7Ia6ZlxTk16eFpTa1/shrr9kNdRGSJ\r\n" + 
	            		"ZBszIzWAz9kNdRCSJZDqUPJDG2/shrq2SGr8liCYXr9kNdfshrpmWluTXswYCwO05YEQlGS7fkrJ\r\n" + 
	            		"gvy3ZDZjhJji1Cf6cMQQAm85OPMFe4haZ03WO1v5ZyWJ0CGYSRa/K9sbosaV8VeU7406zgP8rdYd\r\n" + 
	            		"Vc6KazAq5ET1g8f4WaV+6rBmsydo6xU59fFvRAOzdSp+RsKGSyFyeztlhEMxCQFMi+nZbkQrmaZm\r\n" + 
	            		"dydTLY/kHLhMYdUMzrL71dU6UQWYccHHr7dGrIOSqaxo8Xa5HrNQ/wAybWElXC6azcq4nDWEx/iQ\r\n" + 
	            		"aV+6rBmsydo6xO59DKPRAOzafHAhra35zUu7npsjL6vAODSUIY0wOJMx+nSNwA1Mh5phx9QNgE/O\r\n" + 
	            		"yJMQkS6eFYW9qUGjPPrC7UEZlZpaw2DqKLvEo5WXh8Uy1jpV0ky1kpV1cz1hsfDLtK/dVgzWZO0d\r\n" + 
	            		"FDEUbHV4XRk3AOzazK7XOc9Y9iBKFN9RzYtuW2axo3Wb4nvMCrkRLWOU1ksMrIyeymGawyo5sXrJ\r\n" + 
	            		"w+OcUaARYk5oiVCQ0KhKeYEolYcJSrAAQw1igfDNtK/dVgzWZO0aCG4rVhVz/JdHhsA6oB2aYIIA\r\n" + 
	            		"OyoS50rGDYFylP1LMx/MlNJSrnqSgBLK3nE3/DTIXyWapAXzWHWDTP5U4HzJfUxTXTq6xsq6uG5C\r\n" + 
	            		"VdJDqbU3+o1jcfBNtK/dVgzWZO0agiay2RGgEWZFHG7VIA3sILgHgX1AOzZuf08R1hBPwtv1LKgv\r\n" + 
	            		"zOai1rCk3ozf8nogNwE0vDxodYPv/wCdUhHzX+sppLgaawir4kGa1PLY6sjuThuoSPly7Sv3VYM1\r\n" + 
	            		"mTtGsa3uGb5Qb+gl1YxcvEYo/h4H2oB2blQXDB9YfDa0Pc73C2+OvdeOvdeOvdYiVqlkb+m5Ste0\r\n" + 
	            		"6qLisGTejN9r+JaT3uJPTne4W7WDrX6ilA+YfWT0XMgdYeVciW5rU8b9UxRdHiio6PlyDSv3VYM1\r\n" + 
	            		"mTtGsf3vaZ5kbupYaw649NIZiHgllQDs3K9r+SNYh7Odvi94V7W+m5hJ5cvpMZclQSMJpO84lf8A\r\n" + 
	            		"DphM5zHUoM5Ua1g8r8IHMfLbqKDxmvqXrmW/8vEVPSSfJKjqJpEknXSbI4eOE0iHy1mlfuqwZrMn\r\n" + 
	            		"aNQYXDL3FMWtQOCYxEub1JiNdNTS1EnqAdm5DJ58L1hQ/ij4whGDy4w05BCBxrCva303N6P/AA1i\r\n" + 
	            		"9zs4RXeX0vPiGsaKrKoZWUVNk0L1iRN08NlY+XGKZg8bvUzSdDKbXva7ioEscMMJOdI5yHiiGk4+\r\n" + 
	            		"YRSv3VYM1mTtGoV3dWZWzp3mhCuK9QDs1eQFUhPLGSfWHHCyaRadvlawr2t9Nm7Z4tGtQ18OYXZk\r\n" + 
	            		"dUDsk08pAr2o8sZJ9YeeQJV1ZjdwKV9JyhnqGlKFC15BHy4ZUXDxyWs0JOVINYYScmOycPHGtMI+\r\n" + 
	            		"Yx0r91WDNZk7RqFd3VPWvxaMbgHZtZWbLoJRSY4xOohsgTPzfTt8rWFe1vp2VI+Jvc9Jzjk5vmJ+\r\n" + 
	            		"4cduCkyc1l1lukdat/LhlchCjFe4hViFkEqdKysPhhFQcPFL6zMk50b1EEfQRh2DxtWoaLjidK/d\r\n" + 
	            		"VgzWZO0ahXd2sgNnhUo1AOzam7IF8ZFJJqc+kp56Y8qbyQBRoxGG1hXtb6csTkq0sxhK1tM3EjuR\r\n" + 
	            		"J6c0adwQzGMLWJTuHRZc+HtqROgQ1mQfBEax0GwprUuSddGajqTr32jA8Zesdi44XSv3VYM1mTtG\r\n" + 
	            		"oV3drMjZ1LLqAdm6nESTPgHlqcGpR6MLWvaK/UHePMzmI7HjCMReOmIN2uJsCAzRgQjA6QiPrB2x\r\n" + 
	            		"u0cbXC4+iGG1gh09tSB3TeSYxTXFmJuXa5BFBKKDfQoVGbi8kximtEmbkNChcZELyTGKY2RsZ9PL\r\n" + 
	            		"aidUnkmMUiiMeSK9LCClSXyTGK8kxikCUhEj2eUUeUvhUdVCFjljvcjHzAXSCMsKO9rWtb/otCvY\r\n" + 
	            		"Ng3sK3pv/LWUprj9d/5aylNcf2yYTdG1DdZS/OAjBjMEUYYUOOyqQJlmr/y0xn1iTHJyXuBlND06\r\n" + 
	            		"tg4PNCHcfomM9CnNcnNwcB00vLo2jg80JdR6fhCCx+LOleLOlYodhuMf1kpwXkTXxZ0rDalSpbPr\r\n" + 
	            		"2VJENsRejHybqplrLciEX6QiuEWOX7xtl1lqRiIt6A3uEWNn670zVIfgNYuc/DpTrKPfdYQ+K+vT\r\n" + 
	            		"Fddxk1QpgNf3NuicfRlrItH1IY7D0jNIae1pba0rDzVSusZRgp4NcoyxrEUgbTml3rG7ldtlVO6w\r\n" + 
	            		"tvbFyg1YsrGcZLeT3CMsStFJGw1neax253bJVUh+A0G9wiizjZ1YKyj33WEPivrp4rhI1hG5fhPo\r\n" + 
	            		"zQtuSxah6GzdGazcitY+g3uETMp61ozOt5Me1CUNm+LVm5FbVv5dhVdcySH4DeFXP8DrKPfdYQ+K\r\n" + 
	            		"+vPqMTe81EHw9hdWJ2Qu6LeaVHMkdNRPUumsxE8yI6xgdzoTms/jfqbyeoX2/lqy+VYyHaxYdzYT\r\n" + 
	            		"IfgKR2AJW4JxpF8dXja3ssYTC8qB/E5rCHxX17MLEIe2xcrblcNnSdeLWVR8U4qFh45brKdrXg2s\r\n" + 
	            		"OiveIZZHxTWogDjlWsmh4oPrDIuKJyH4CixcBmXW/pJRWKXLrotlsP4mlYQ+K+vGBCME+hpzaPeL\r\n" + 
	            		"ZSO5lZR77qAd5ayj2JrDfaOUe+6hXd2skdk6wr2rIfgNZUQXXRCsSOXRSfMoeGW1hD4r7BL4Mjca\r\n" + 
	            		"c0SpvWUAVwjhzn4vHMrl8E3qGi4ZZrKw+GEaxGDhhmWwXDNKiQ+XKdZRHwQfWHwXDD5D8BogsCtk\r\n" + 
	            		"d0Zje5pzRkKMqKQL1VYQ+K+w5CYynhi1hI64mbNRHBIabTuncbf21ZnO4IvrHxHTwzNhHC+UiN5C\r\n" + 
	            		"wN7CDWZDuXE9Y5I6eFyH4DTT8Xmhs5TjSlWI5srCHxX2FacWmR6wgVezVmlFzmLUHX2cYtWal1jX\r\n" + 
	            		"akZA1KtKUEhNmdHzo7qAr7OMUrNi6xjjSYoZ6lEQFMjkPwGme9hNE2bPFo1vCHxX14c/XI3tHkRk\r\n" + 
	            		"MsdPY6AE2mah6I1AG27XFnxCByaFZJiZTUBk42BS4z5iJROSs9evrETVdZIaeUZbi1Lk5qRZUBko\r\n" + 
	            		"2BY4T5iKROqw9wcaxK13XSSpD8BqPfAVkhs8MlWsIfFfXXA+yZAK97i9GMYsYrVay1HBGeltRqF6\r\n" + 
	            		"6KNBLIzayzHBneluSKF62IsxTGy1IfgNR74CswNnVx/WEPivrrsksvbHTHjuQJRGJATcEffR3boP\r\n" + 
	            		"IVQo1Am5CO38tq/9tLYCQsNXxd/SDLZngwTNBHxYOKx1vYk/olsCTrTF8WkCQZbM8GCZ4K+rBxSO\r\n" + 
	            		"N7CRp4KGe0eSZPXkmT0zlDIaKVElqEymDyMCjyTJ6xY0OLQg/wDjLf/EAFMQAAECAwEFEQwIBAYC\r\n" + 
	            		"AwAAAAECAwQQEQAFEhMhMSAiMkFRUmFxcnOBkaGiscHRFCMzNUJgdJOywtLhNENTYoKDkqMVJGNk\r\n" + 
	            		"RFSUs+LwoPEwsMD/2gAIAQEABj8C/wDvy6qUANm2cWlVNQ+cUB3NFPM3yF1wbhTXJbxlGevVZsG6\r\n" + 
	            		"UXoh9eqcAiGiHGS4tZODUU1pTtt4yjPXqt4yjPXqsyp1xTjrS1NrUo1Jx16CJxsQhRSvB3iSDjBV\r\n" + 
	            		"i67eMoz16reMoz16rRzcTEvPd7SRhHCqmPMRTjayhxdG0FJocZ7K28ZRnr1W8ZRnr1WhHXboRKm0\r\n" + 
	            		"vpv0qeUQRXHmlQ8CkRkQMtD3tPDp2N/HrZRrGM4OTHa+cWpZ1VGtqjLYGGulEJp5JXfJ4jitCuRR\r\n" + 
	            		"GHWykuUFM9THKPbaj4lCEu4kpeUALeMoz16reMoz16reMoz16reMoz16rQ7b8bEOIKF51bpI0J82\r\n" + 
	            		"bnbhzqk3uxO57etQs8ZHZOOgSda6kch6pw0IDjffvtsJHzE4lGrCE85PbmIKCB8I4XDwCnvZiEiq\r\n" + 
	            		"1LzCVHbpmHLk3McKWU5155J8J90bHTmYWE+3eSjjNqCV0N+zMPuF+yfNm524c6pN7sTh0a2FB5yp\r\n" + 
	            		"sorQRCFNHp6RNmFBxQ7GPbJ7KTprodY6MxgRkhmUo4TnuuTN/wDXMh0bRkwkmph1qbPHXoM+5Ydd\r\n" + 
	            		"ImMzoI8lOmeqdIcYNlJ74+vQp7TYd0h2LXpla70cQtQ3LTwOLHXaHulCOPIwKicEo3wOKd0N+kmF\r\n" + 
	            		"jmcK0W1G9viOi3iz99z4reLP33PismLgoLBPJqArCrPSfNm524c6pN7sTUn7NlCevrnDRY+odSvi\r\n" + 
	            		"NqjIZR74NRhilJ2E53qnCjXJcHMOYjYqtQ4+op2q4uSVyMVKXPQ0dtP/ALlHwZ8lSXE8OI9AnErC\r\n" + 
	            		"qtsnAt7SfnWTEAxo3l0rqDTNm4KFRetNCg2dk5q6G/SRvK/Ny524c6pN7sTjdi8HMEmcVA9Dtujh\r\n" + 
	            		"EoB+tTgQlW2nOnotERZyMNKXxCxUTUnLaMe0mGgeErSO2UAf6hHGkicZF1oWmFFO3TFyyhYb7Z5C\r\n" + 
	            		"OM2gYn7N5SP1CvuyDP8AmWVI97qlFRf2DKl8QtU5ZRceoeAbDaNtX/rlzd0N+kjeV+blztw51Sb3\r\n" + 
	            		"YndFX9wocWKVwIr7W5yU8WP3pPQpOOHfxbSvnW0SAcb5S0OPHyAyuxGU0TzLfEf+Qlc04/pSBi2T\r\n" + 
	            		"SamQccS6lHvdUoMUxNkuHgB66WfXpsOIc5adcoGI0kvpvto4jKPUNNCU8agOubzum5EniAHzst1V\r\n" + 
	            		"aNpKjS30W6Hq0fFb6LdD1aPit9Fuh6tHxWXFwjbyEIcwZDoANaA6ROrK6G/SwsK+6w5kv21lJ5Le\r\n" + 
	            		"Obof6pfbbxzdD/VL7bRiYyOiYgJaBAddKqY9nzZuduHOqTe7E45zXxTh5xlcp5I+ioaH4SinZKJh\r\n" + 
	            		"CcT7FeFJ+ZtAwYOjWpw8GIdJk4KZ54B4+tHVSUE5rYls84TgYIHQIU6obeIdBlFxVPBMXv6j/wAb\r\n" + 
	            		"R7OmqHXTbpilUZbQ0WPr2Ur4xaI3aPaE0b8u0TvKujMP+mK9hErob9mY7eU9Pmzc7cOdUm92Jrc1\r\n" + 
	            		"6iZPwmiUIQEbJSKjolAOVoFO4M/izvXZLOkxDpHCansslpAqpZoBaJhE6FiGSkfhp2SbcrS9UDOK\r\n" + 
	            		"oc6zRocAx8tZRTsRFw7K3X6UW6AaAfM2KVXSg6EUPf02W3Wt4oio05QuPPM3zZ4Di5KWjdVN4rni\r\n" + 
	            		"ak/ZxKk8gPXZ5pOVbZSOK2jg/WnsstpWiQopMn/TFewiV0N+kmHjGEvNYJRvVW8Uw/FbxTD8VlLg\r\n" + 
	            		"YNthSxRRTp+bNztw51Sb3Yk+5rGlHkkEjKTS17QUyUtFQf2DykDarZDyNE2oKFo2JSahTlBwCnVa\r\n" + 
	            		"CQRVLa8Kr8OPppa6Q/tVnkm05Wt8gGynFmiUips9Eq0TzilnhNc1HQn2bqXBwinu2jocZVw6r3bp\r\n" + 
	            		"inHQJOPOupHIeqcTvyumT/pivYRK6G/SRvK/Ny524c6pN7sSugvUhHPZMmEHynUjlkt0DFFNJc93\r\n" + 
	            		"qnGxxHg0BpJ28Z6BaPRroVwc0zgXK1voZs1/CLR7taEtYMfizvXMLTcmLKVCoOCNvFEZ6o2wMXDu\r\n" + 
	            		"MOUrerTQyWwckQwQNsUPbKLgqUDbhvNycY5JMRiq4Kt47uT/ANrwWS4hQUlQqkjTEonfldMn/TFe\r\n" + 
	            		"wiV0N+kjeV+blztw51Sb3YldBX9KnGaSgEDyopsc4Sgo4DwbhbVwivuzbcIoqJcU51dVn0a5tQ5J\r\n" + 
	            		"3ONa/wAsgcQpaGhAcb719wJHzEmIVOV51KBwmlglOIDJKCi6eEaLf6TX3pQD1aDDhJOwrO9cm7ss\r\n" + 
	            		"Ixtd7fprdI/91ZiCjkKfhBoCnRtdotf/AMRCdhTSq9Fn3EGqVuKIPDJ/0xXsIldDfpJjn21uICFJ\r\n" + 
	            		"ojLjt9BjOb22+gxnN7bIZEFFguKCRoe3zZuduHOqTe7Eosa8oHPErmg/5ps86UaimebRhU/hx9FZ\r\n" + 
	            		"BKRUnELQ0GPqGko4hahyGxScolAGtc4RxKIszCg4odjHtk9lJQYORslw8A7aTaiAMbEQOIg/KQWk\r\n" + 
	            		"0KTUGzMSnI82lY4RZbLqAttxN6pJ0xYvMpU5ArOcXrNhWaf9MV7CJXQ37Mw2/J6fNmDVc6FwwaSu\r\n" + 
	            		"/wC+JTTJqm3iz99v4rIUbm4gofXt/FLuS57OFdLySU3wGLht4s/fb+K0HFRUBeMtOXy1YZBpyyW0\r\n" + 
	            		"sVStN6beNkeo+doeKdukhxDLqVlGCpfUOTLN0t3OqgrN6cO3jFdu3iz99v4rQ8HHNYN1orzt8DlU\r\n" + 
	            		"TpbdoyNauffNuOd7OGbxpGIaexbxZ++38Vn4u6ULge83jffEqrU7B2JxcEyi+cWjOCtKkGo6LeLP\r\n" + 
	            		"32/it4s/fb+K0HDR7WDfZReKTfA0ocWTYpJTTqErQoUUlQqDZT1yn+5lH6peNHBpjlt9BwydcysK\r\n" + 
	            		"+dqfweP4IZXZbO3Iihu0XvTbv4Yhh99yp5tbKg+6MOVulxSr290gOqUZFQ0BftOuVQrDIFeW3iz9\r\n" + 
	            		"9v4reLP32/it4s/fb+K3iz99v4rMOLubRKHEknDt6u6/8NW9jIxAc+yTnl8Qt/K3MedH9RwI7bZ6\r\n" + 
	            		"46SNiI+VgmLhX4avlDPpHXyWC0moUKiSnnnEttoFVKUaAWKINp2MUNMZxHGey2duU0E6hePZYJjr\r\n" + 
	            		"nusfebXf9lu6IGJQ8j7pybY0swx3Sy653RfXuDppU7bfQYzm9tvoMZze2zMcyCEPpvgDlE3o54Eo\r\n" + 
	            		"YRfEDKbfQYzm9tvoMZze20R3Mw813Pe32EpjrXsmpBgYvOmnk9tvoMZze20R3Mw813Pe32EpjrXs\r\n" + 
	            		"l3bENOOJKwiiMtvoMZze22OCix+ntslaCFJUKgjTn9BjOb22+gxnN7bMxzSVJQ+m+AVlGYK3FBKR\r\n" + 
	            		"lJNALFCHlxSh9gmo4zit3i5HCt/5W75ccU+7Ef8AGwTEtREL94pvk8mPksh9lYW24m+QoaY8zyta\r\n" + 
	            		"glKRUk6VlwVxXC0yMSogaJe51BYkmpOUzhoT7d5KOM2oLORkU5eNNCqjarhLcMk96YBxDZOqcwmL\r\n" + 
	            		"gnShQyjSWNQ2TGM51Whdb1itSdz29a2s8o7JmHJxwzyk8Bz3WZ4CuOJeSjgGe6hO6DeuQg8p7Zub\r\n" + 
	            		"syup+T78k+kp6DNDK1VdgzgjufJ5MXBmbn7zPDxJvnFeCZTlWezZtWKeo1XOMIxIT25hKEiqlGgF\r\n" + 
	            		"mYZGhZbCBwDzPNw4NedT9KUNP7s72AhHHqZVZEjhOK19SGrrcLjtCquhBrQ21fLv9Ek4sWMbMhcl\r\n" + 
	            		"lXeYXG5Tyl/LtklppBWtZolKRjJthr6FDlK4HCZ7opZcPENqbdbNFJVpSbQtVIeLIad6jxzh0a2E\r\n" + 
	            		"HtKnGQRPhWg4Pwn/AJTgoIHwbZcPCae7OKb10LXiUO2bm7Mrqfk+/JPpKegzTDqVRqNGDO68ns4c\r\n" + 
	            		"zc/eZPR0Sc40MmuOkLORsUqql5BpIGoJhcNc14oORS84Dx2r3ClWwHkdtrnsR0E8yMOFVUnOm9z2\r\n" + 
	            		"XJpeZ8THr+pRnRqq0hx2W86orW4q+Uo6ZleLvkwrOefWOjhsiHhmktNNiiUpGScRGryMNldNXYst\r\n" + 
	            		"51V8txRUo6pMn7ruprgzgmdg+UejllD3XbTQk4F7Z1p6eScHGE1U40L/AHQxHlBktOsZQOvrnBLr\r\n" + 
	            		"icXgj+LF00nGEHOtKDQ/CKHlrNQro4ZQ5Qeqbm7Mrqfk+/JPpKegzS42q9Ug1SdQ2ho9P1zdVbCt\r\n" + 
	            		"MceYufvMm7ktq73DC/d2Vn5dM27oXSZDkYvPJQoYmf8Al5pQkAk+GcLitpP/AL5Jw9R3yJGGXw5O\r\n" + 
	            		"SmYwIOOJeSjg0XVOBTTG4jCHZvjWUcmmNCMIPwms3GCfARBA2iAe2UdsFA5iZXqhQ0B4xWyHkGim\r\n" + 
	            		"1BSdsWbfRoXUBY4bLdXoUJKjZ2IXonVlZ4TZahkbFTx065Qwro0LHNJ6pubsyup+T78k+kp6DI08\r\n" + 
	            		"kVlE3JcVoe/NdCuqS0jIFESufvNitZolIqTaIjF5X3CvarJsupq1CjDKGrTJy+abbWkzDpHCST2S\r\n" + 
	            		"bZTlcWEjhsltAolAoMxc5jVLijzfnOEZpTBsITzZRrWvhnE80zuk1vahzpXRP9wocWKUEoimHudD\r\n" + 
	            		"r/bCfdlBknPNJLR/CaDkpaOcBxqbwY/FneuV0os6TrLaeMk9UoA1pn1DjSRNzdmV1Pyffkn0lPQZ\r\n" + 
	            		"JgVZIll1va72rHZTaxRSDQjUtDR3ktr75spOI2BBqDkNn0a1xQ5ZXP3m10HP6Ck8eLrnGxdMbjob\r\n" + 
	            		"/SK+95pxg1obHMTK5yTkMW0OeMzAj+irpmhJyhIEnknym1DkndAU+rRj4TKOc18S4ecZXEif7bBK\r\n" + 
	            		"4AKdco2CJ8G4HBwinu2hYUHG8/fbYSPmJKepTDxeF4K3vVK5xr/iUDjNJubsyup+T78k+kp6DKAI\r\n" + 
	            		"1yvYNn1JFG4rvyeHLy1kyFGrsL3lfBk5KWjUUpexLgp+IyufvNosa4tjniYI8p9ZPJaIUkkENKII\r\n" + 
	            		"0sVvHN0P9Uvtt45uh/ql9tvHN0P9Uvts85FxL0QsRagFOrKjS9Tq+Z0bs4P/AG0yuco5BFtHnjMw\r\n" + 
	            		"KtItK6ZoUcpSJRChlDSjyTuidK9b96S3NeomSSE44Qtr93rlgK4ollSOEZ7qNoaFriZYvuFR+Ql3\r\n" + 
	            		"KcRh2ma7d8mvTKBcyXkU2ecJubsyup+T78k+kp6DK59PteqzcegZ+DXntwr50kuBWc5GIxbpOMcl\r\n" + 
	            		"bXSH904edK5+82idhSPaE078u0TvKujMP+mK9hHmdf08MwhXSOqSHhlbUFCyXUGqVpvhmLnP6inE\r\n" + 
	            		"nm9k4N77SHQrmiV0HNbCueyZx7+vcQniB7bRDmsaUeSSUa40tFQdPDMqSNumK1DaAf0kxCa7RNDa\r\n" + 
	            		"NVpIUGxwAC0FC0qFvC+2hjPILR4pXOA84Sac1qweWbm7Mrqfk+/JPpKegyucf7hIs9CPDOPIKFcN\r\n" + 
	            		"noR4d8ZWUK4LMxbJz7KwtPBaKiGVVbeKXEnYKQZXP3m10EajV/8ApIV1TiYfTaiK8BSOw2KFCqVC\r\n" + 
	            		"hFvFEH6oWiEJFEpdUAOGT/pivYR5nQMeBkKmVHlHvTZQVVdhO8rGwNDyZguj/DPJc4ND704M6bQL\r\n" + 
	            		"SuA9lJRQrnniltPCewGba/8AMOrc933bXRXqQjlP0mUKkeU+gcso6HpQB4qTtHGOm1RlFn4teifd\r\n" + 
	            		"U4eE1s7FkYoZnlVi6K2uiD/l1GaHMWeSDik5uzK6n5PvyT6SnoMrneko6ZNXSbTnItN6vdj5U4pV\r\n" + 
	            		"Ua4qSufvNnoZehebKDwillsuCim1FKhsyXBrNExjdBuk4xyX04nfldMn/TFewjzOiYVIq5e37W6H\r\n" + 
	            		"/aTESgFbK8683rh22ERAxCXBpp8pG2JxEEvI+2UbWzZbLqb1bailQ1DJy5UQsJRFG+aJ1+pw9Umr\r\n" + 
	            		"lMKqmGzztNfqcA6ZIZaF8txQSkapNoeDRkYaSiurQWugf6VOM0lc5B8qLaHOEmIsZIlmh20/Iib0\r\n" + 
	            		"WRjiXsW5T862uggeVCOjmmcGvXQ6Di3Ik5uzK6n5PvyT6SnoMrneko6ZRDCU1dQMK1uh/wBIzFz9\r\n" + 
	            		"5kt9I71G99TuvK5cfDJD7KihxtQUhQ0jYLSQiJQO/M6h1RsSid+V0yf9MV7CPM83ShkfysUrPU+r\r\n" + 
	            		"X854Rh1bSxkUhVDan8XjPXG0GuKiXXb+/SS4sqypOrsyF1WUd4i/CU8lz59sqi3couo9g6U0r79W\r\n" + 
	            		"WxJNScpl/Fnk95hcTf3l/KUUNepsc8Hqlc4D/MJMmooDHDPCu0cXTScFC0oUsgqH3jjPKbRKKVvm\r\n" + 
	            		"VCnBO5p/tWxxJk5uzK6n5PvyT6SnoMrneko6ZxDKU0adOFa3J+dRO5+8yVDigfbz7CtRWpw2Ww8h\r\n" + 
	            		"SHGzRSVZQZJeh3ltOJyLQqhFrzu8K2VMpJ6LKcWaqWakyf8ATFewjzPXDRDYcacFFJOnZURc5Koq\r\n" + 
	            		"Ey0GNbe2NPbzFz3dSJRXarJyDim79p0UULKVeqdgye9vgcitQ5hK71TMGDn3iMuwnVNm4OFbvGmk\r\n" + 
	            		"0SJJTr4lI5CeqUAD9oTzTKOhaVK2SUj7wxjlEoSDpieeSFbVcfJJSNcKTgD/AE6cRMnN2ZXU/J9+\r\n" + 
	            		"SfSU9Blc70lHTNu6Tac/CKorcH50nc/eZ90MFLEaBic0nNhXbbAx0Mto6R8lW0cy9UZYxVP0o80S\r\n" + 
	            		"qMue0tZyrGdVxi1UuRjewlwdYtjfjlbBcT8Ng41c9KnE5FOkr6ZlC0hSVYiCMtivuZUOo6bCr3ky\r\n" + 
	            		"W+mxt7qVT2WC+5DELGm+q+5MlgAKAZBNMPdBjDNpVfhN+U4+Dbt4s/fc+KyIyDgcG83W9VhVmmKm\r\n" + 
	            		"mZ+Bb/TaqWkA7mdTczL/AF3Pit4s/fc+KyIODbwbLdb1N8TSprpyKjc3Gf67nxW8WfvufFZ3+HQ2\r\n" + 
	            		"Bw1L/PqVWlaZTsy7mj2cK1fX17fFOPgt4s/fc+KzcTD3PvHWlXyFYZZoeObkM+i/adSUrSdMW8Wf\r\n" + 
	            		"vufFbxZ++58Vm4WGReNNCiE1rTMFp5tDiFZUrTUG193FgVHTZWU8mS1e6I8bAcT8Ns93U7u3ewC1\r\n" + 
	            		"WLlsVGmsX551qAUA/wDBbqogDVNqpII2M1U2vBENX2pfj/4Km14Ihq+1L8edqoWBSIqKTiVjzjZ2\r\n" + 
	            		"dWxwt0HG0nyGTeDktfLUVHVJtfNOKQdVJpZphqNU+FrCQ3EZ8HrnU2VCXEvVqGJUSoVH4Rp7dr+N\r\n" + 
	            		"jHnj99eLikFQUc62B5F9VPFksmCjkpYjDoSNA7tahzK4O4oS4tOJUQrGkbnV27FUbGPPV0lKxcUg\r\n" + 
	            		"qCjnmqeTfVTxZLJgroBLEWcSVDQO9hnGKSSFCHWQRpZ028ZRnr1W8ZRnr1WLD7hW/CLvVFRqSk4w\r\n" + 
	            		"ekcE41pmOiW0JvKJQ6QB3tNvGUZ69VowxMQ68Q6KFxZVTF5vpudBrvYqJTVSxlbR2nMwDdNC7hP0\r\n" + 
	            		"57qmbhwa6FQrEqGprMyFJJBGMEaVqPn+ahs4797UVP8AgsEui1prErByDW5kEGhGQ2vH1Vi4WiXf\r\n" + 
	            		"vjSVKO9Gc9kzaQtVGovvKtvyeXpnHfl/7aZR2/Do834yKJqkulKNyMQkWQvBstC+ecpk2NuwSm5r\r\n" + 
	            		"TpHlPi/J47UXcqHTvYvOi38QhYhxTeCKQ0vKkmmnxyiI5zQsNlVNU6Qs5EvKvnHVFSjsmS46OSTC\r\n" + 
	            		"MqvQj7RXZYw6rnMNYqJW02EqTw2egHsZaViVrhpGTFVUaiDgXOHJy0k/Gu6Fhsq29izsU+q+ceWV\r\n" + 
	            		"KOzJcZHJJhGDS9+0VqbVjDKubDtimdU02EKTwiz0A7jwZzqtcnSModZVRp84F3aPzpKO9Gc9kzCk\r\n" + 
	            		"mhGQ2ho4aJxHfNhQxHllHfl/7aZR2/Do83lqGUJJnGpHhA8L7api68yxBJOOKdqrcp+ZE4OFpRQa\r\n" + 
	            		"BXujjMoK6CRogWl8GMdJkFJNCMhtDRn27KV8YszBg44p3HuU4+m9nBw9KKwQWvdKxmUFdADHjZWe\r\n" + 
	            		"Ue9KotCRlcbzKVHbpjtHejOeycxE3JcVou/NdCuqUd+X/tplHb8OjzfiYJQ8C6Uja0pCJbF+2rOv\r\n" + 
	            		"N64dthEwLwWnyk+UjYIzDEODiZhxxkn5Shof7Z5KOMzv6eBiEq6R1zg65UXyOJRtCw+k1D33CSew\r\n" + 
	            		"SZY+1cSjjNqCSl/ZPoV1dc4UHK2Vo5xtHejOeyZNJc0BWL7arZ6Fc0TLhQeA2ho9H1K6qGqnTHFZ\r\n" + 
	            		"LiDVKhUHVtGHXBs8xMo7fh0eb4u5DIregIiQORXVxTETBPrZcGmnT29WyYS6t5DxBxJd+rX2Tixr\r\n" + 
	            		"EtjmA9crnClf5pB5ZxpOlgyPWJnTWxCx0WiBrEIHNr1yucn+7b9oTjxsIPPTNwayKUOam0d6M57J\r\n" + 
	            		"kletNbd0pGcjEBf4hiPVxyQwtVXYI4I7nyezgs6dc0g8ko7fh0eb5QtIUlQoQdOy465qC5B5VIGN\r\n" + 
	            		"TPyzCLi3Rcvq4oZxXsHqlHfl/wC2mVz9+nHfl/7iZq9JV0C0d+X/ALaZXO9JR0zj9wPaE3/TFewi\r\n" + 
	            		"0d6M57JmiMAq7CUc/CdF1HgkIZR73Gpwf4so7OGyDTRQqTyqlHb8OjzhVE3NvYWKylP1bnZZULGM\r\n" + 
	            		"qadRlSZBSTQg1BGlaGjVeEUm9d3QxG0Sr7RKFc0Dqlc0/wB02OWcUNepsc8Hqm2rXurPLTqs6T5b\r\n" + 
	            		"SCOKnVK5yv7tuv6hONx6K8HPTMK176z0DqtHejOeyZoafFUPw4SsbBTZ+Cd0TDhRt7NkPNG9W2oK\r\n" + 
	            		"SdQi1zbotjOxMCDynFyyjt+HR5xOKCB3VDJK2Vae54ZxkPXE2+FDhHytDxGk7D04QT2iTER9k6lf\r\n" + 
	            		"EbVEmmtN2JHEAflO57eq1f8A6jfddoSJpidh73iP/KTT4+qWFcRsFA1BySS3pvRCRyE9U4BFMakF\r\n" + 
	            		"f6iT12jvRnPZM4beU9FmLqtpzsQMG5uhk5OiULDK/wAMVhJ+6aGnHfSjt+HR5xOxDugaQVq2hOOe\r\n" + 
	            		"0lvBPEPnaHjUjHDO0O5V8wJwkRWqg3g3N0nFKGgEn6O2Vr21fIcsmoZoVW8sITtmzbCNC2gJHBZq\r\n" + 
	            		"MAxwr2PcqxdN7OEevqrQjBObpOL58MoS56T4FBcXtnJ0csm2GxVbqglI2TZqGRoWWwhO0BS0d6M5\r\n" + 
	            		"7JnCqGQsI6LRMKE1cvb9rdDJ2cOYjt+HR5vxbTrDcVCpfWGxoVBNcWO3f2opg7KAoclqpefcOolk\r\n" + 
	            		"9djBQrRh4QnPVOec29ScNDuJvXVjCOjZP/QLREC5kfbKa6h0jx2ch3k3rjSilQ1DJTbyFOQbxqtK\r\n" + 
	            		"cqTqixchXHIl6mdaDZTj2SbOxkSq+deVfKMu7lp71BC+21nJ1mURBO6F9sprqbNnYV9N64ysoUNm\r\n" + 
	            		"SkupU5CPeEQnKk64WLkM45EPUzrWDKceyTZ6NiVVdeVfKkItae8wQv8A8fk9vBKO9Gc9kzgfRm/Z\r\n" + 
	            		"En0pTRqI783w5eWs47fh0ebz0ScjLal8QrYk5TmUXWj2r2GbN8yhQ8KdXambuQTdSB/NJHt5luEh\r\n" + 
	            		"Wyt100SBZuCbzytE6vXq1Zm7cE3VSR/MoAyjXZlEJCNFx1w0SkWRBoopeieXr1SjvRnPZM4H0Zv2\r\n" + 
	            		"RJMc2mrkEqp3By9U47fh0ebz8EpakB9soKk5RWxME8zFI0sd4rlxcts/cmJO4Tf9FqC48dww6hbP\r\n" + 
	            		"QqYdOuecHQMdkvx6+7XRkSRRscGnagnQ2VFXIWiHdVjUyrwZ2tS165ct9ey0nCDkteouVGk+jqsD\r\n" + 
	            		"ENpg2tNTpqr9Itewyb95Q748vRK7BmVRVyVohnTjLSvBq2tS165ct9ey0nCDkteouVGk+jqsMO0m\r\n" + 
	            		"Db01OnHxCxEOm/eWM++vRK7BOKZaFVuMLSkapIt4s/fb+K3iz99v4rQrLootthCVDUIEnId1N826\r\n" + 
	            		"goUNUGy0NQOEQlRCV4ZAvhq5beLP32/itFN3Rh8CpxwFIv0qri2D/wDjLv/EACwQAQABAgMIAwEB\r\n" + 
	            		"AQEBAQEAAAERECEAMVFBYXGBkaGx8CBgwfHR4TCgsMD/2gAIAQEAAT8h/wD3y948lBie1BEx9iNw\r\n" + 
	            		"IPMGeHHrX7hSEZh1ONX80igG2YetfuPWv3D3hcIIJWoybKsQCm8u5Y9a/cetfuCIJt8ItPHt8LMe\r\n" + 
	            		"7RCYcHrX7j1r9w3rWCDgi6T8rERVGu/bbjqYlSGVu3Tc5riT47Qu+GBEDImzBonIjuXZhMBPgSLZ\r\n" + 
	            		"xojDIQEGQOPWv3HrX7j1r9x61+4na4t24X6z6jWn1utc8+T2NWmp6tV28ZQ4p3rwxtscPhNmQflP\r\n" + 
	            		"Lp8JPkH3jPefg5Mm6FtTvfDP4WpYTjQRwAAAMgp3LwfXm3qNafW61geNU1/yrKXY8j2a3ehI0cvZ\r\n" + 
	            		"Xlp/YX58JdskG8Xj0pwFV2jHil2Rd0ezUOicpfz1wcV2VahhgPAPB1jGqXBl3Q91xAp4p2xkREmV\r\n" + 
	            		"IIcyFnblXuXgpMeXzcLXQ1mmgbmx0SGymX1n1GtPrda323Juame10ccE2kJE20vyOtLB6CueeyOL\r\n" + 
	            		"+fCHZNOV2Ck49YGbTOrI3A1HbHHbFhjjJzpFgCmyzk4AvLB24k2rbqK3fl3LwU9FofXPUa0+t1rL\r\n" + 
	            		"7wxnZH6zSdegQz3GkqyGtf0Fj9hTj+YcEiU7XEollMbCdmjMu9j6F6xxJtxvwpbGbLwT9xaLaPBo\r\n" + 
	            		"yw2ht5H69aXIJDnVUwyIqZV20lryB2IqnKPzdy8FPRaH1z1GtPrda7sMzOb/ABRoAhtNw8abvSka\r\n" + 
	            		"EJ2Y2kOdIaBOGhjGUa0rmieTP6rbg5PGXj1pP0t1k4wW4kB3+zpO7By0bwLSVKeQV8KhteNhnfAb\r\n" + 
	            		"0UZkCbfBChQr2DEQ3AQady8FCxQIhF2TdXBgHIevW0SY+s+o1p9brW+43E3tQLryWgfIpsYQu+07\r\n" + 
	            		"Y9n4/hKFBEdksio9NFyEvJnZK9XVCZ6W2k6SDO5F8LFm5SO4nuCjAiBkTZhSJOhj+/Bt6DXHqdfy\r\n" + 
	            		"8dy8H15H1GtPrda3XXNc7s0SKgDoneNJRgOhBbsInbpN4vZwhtLrlYMLrERmxRouIu0zIZrnM4Fi\r\n" + 
	            		"8lBLzF2PDC5TiRIcGcdcGWTKkkGJpdMk3OXyYWYTDX0l7TUJUwSRN30ylRRDEHZDKRj4eO5eCjeI\r\n" + 
	            		"orSFnH9R/uP6j/cRz5Dk0+s+o1p9brTqyMZJpcTEOON33ItGmHUEJBtCB6Rh1IS94yYuSk9xHC/g\r\n" + 
	            		"W4gjsHPCmAxdbm/lbgLvdsk4kIG+gZ4nPu3qn6+UguQuc8MGbMS1Ke4K/wBnGr/T6/h47l4Kei0P\r\n" + 
	            		"rnqNafW60v2DFnW13oSyBCN4pa03SkJfvnRxtV2NX6WuDUFEg31u9hqUxnCPVlllRWkCgR206svB\r\n" + 
	            		"PRO2GkkLHb4exg4QPpydRSamQTbaelsBUjtyJcSnp9fw8dy8FPRaH1z1GtPrdaZ4Xy9x+qMIFCHb\r\n" + 
	            		"NLa5WNInl1rflN1iY9p88Xki6aSq7MybGh+MW6J41vO9KVCyWpfrBsQYGhSI83NA6Rh1x2VGWJEL\r\n" + 
	            		"V21jk0qj/rjkE9C0dsBMkuSOX4xb7K0SKT4eO5eClnnE7uJx/OwfzsCWwhsFY+svUa0+t1priPeN\r\n" + 
	            		"r+UCYQWm4P5SGJcESn2DnR+C4Da4gZiVNqBOGZSEI7cCDCQmjTZGe9tMXehI0cvZTDbl9OlS4xKO\r\n" + 
	            		"hj3pjdIDYmINLVaA/uEBAFkYhHGvMPvjbnbx/wDDx3LwfH0+n6zBKorgsMrRpNCLWXZTRDjlSfMs\r\n" + 
	            		"oM4pMb0uwSHYNpJyYdRIcM9o+LE0CmdgbuCrd2aZghpmuhNWkEukwVDEqUAaGZYUmPYHagFc6P8A\r\n" + 
	            		"VYDHMaCFLYvVNNsGGkkL0mSiJG4AdEcSQ9MbPvYCAmGQB5T4YVqY1J1MGgmNX88HHa+PlH6YLZNk\r\n" + 
	            		"hYwS2pk77aJBsS/Gaaaa92ViAC//ABql0aJ/iHOMRLXngTEnDRB88biPDjIjoWIrSJqNIWUDh3rh\r\n" + 
	            		"+8ax3MGB2oQev+MDAG0Qm9EXnBfJdbj0zFufgyvuAWZ0ymnH87B/OwWn1V0EY2jVxVOQgcXH87B/\r\n" + 
	            		"OwF+EQUQuurAKijRh/OwfhIw4hddCdgV7kWbppj+dgaQZbsWYGHFlwcmv87B/OwWxKWg3x8CK/Mc\r\n" + 
	            		"N7iwD5IjnC5LgaFTZB7H7wG5yzhjrdh045gLngFeFkDEj9PYQBeAGauGwU273F83dhQyJRlWqBC5\r\n" + 
	            		"HoJgAAAZBgnU/fBqrYMIPS4ANenwTCfjjtMEbckMu23jaNWukZja4VZcDDSId6ltZEcw9etW8zk+\r\n" + 
	            		"yw1et1+czdsIbntL4jsflrNRNEf8w27G9th+45I6Da3vwdMELauWO0wOAePp8TGBTnzOHJeRsavt\r\n" + 
	            		"nNHExDrhVh1P8o74Kq8NFDM0DIYb2oseUlWcy5GOLQK/88VkBiSswC5JzOcb8KRWdCqRdQC2Fe2u\r\n" + 
	            		"i1jmL14v+RXLoGb4PW6uxE/vieXWt5m5Hg6vW6/OZqWmHW20uMz8XsflpD0nwzyA3rBiauoN3HdH\r\n" + 
	            		"/a3OgwhaiyThhgIWrd8Jo4Nm3AzLbL9PMgZVNtbnIwyMit2ZVpbmJmEbDvg8hxYzwMVR2s+bQW5m\r\n" + 
	            		"DCXF32iV60P+ptMwnoI50HHlQNEp0FAoyMJjMEI1oIuxOzcrNSeoN3sE91V0S6OUFQyJc7qnrdfn\r\n" + 
	            		"M14gpcwZHENNgDYW5QT4dj8tEVwAHIWHg++hg8QEuDZZ7tmzX6lAJiI7CAedZehncXdny83wV4Sm\r\n" + 
	            		"NErx61spJfeB2SjskNNIPAaqkSE08goR14JFygqCJRyoDsmJtyrRJMdm+USecJ3DjoBLjv1+ovnB\r\n" + 
	            		"k9DkPIUytWPWset1+UzXkzccJ/7TP0UK7GA6y5tJCoRO5p2Py4F4wrYGbifK1nUkOWVDMOqLJAeo\r\n" + 
	            		"eA/U0lcibwdnop3KslGI0AnoBB8HBZchQPNQhQQ0REAp/ToCt0uSTofyggxa33v8UmGofh5FF9zc\r\n" + 
	            		"nJgLHHV08FSQbMXHwFO28k+tnX1uvymaHhdztrHIhhObrs0WTErmxB6kXnGBDAkGRMX0m6awqdj8\r\n" + 
	            		"uGLYYromsOTjv1NUYiwdb9pdfWcPiJvGCxXAVKI3FAk0KRvVXucKaCLHftS6M3WM5SmeqC5vflTs\r\n" + 
	            		"lP7pnh1xbEUz0L0ohECnNhNGQafSN9fW6/KZrkww8kXFoEFvb/jXOmo2LOy/r5hxH6vBsih2Py4c\r\n" + 
	            		"Azzq/lcxjjiz8wud8IVK58MGDAF4iqlxKylev05hSAI30M5NnxDQrGO8H+lXFlJd8UstjuMq23sJ\r\n" + 
	            		"ef8AiiuzNxzuzQHeGG0Il70pcFjDgDt1sIdKe3JntStQmzrG7lS9xcLshq+t1+UzWBI292LrsFG4\r\n" + 
	            		"Hp5aW7Y83W8D0xfHPrE/tOx+XCFBZburMTEtNuuY9Tr+peGy4FdYo+6aQM4jlEuoknwVCWZcRHlU\r\n" + 
	            		"QshuaUBnaDjYrZravGYi7EXfKyaXmiGWkuDak7ppS6xgKAiZjiQWAX7CFxBzIxpOdxxn0GPbuYUM\r\n" + 
	            		"wjO5Z7UuUF/ciA19br8pmoYDPUMfuJYHDwROI6UMZSotuxFQ4+Kce9ymfnTsflwgBMFSAaZ5HCd8\r\n" + 
	            		"RC2EZI7KdETJDIBW+oeEbW4hf51bTu1faOFpxH4IWutbaleHSswLcaTBQsgb0oVqETEWR4w7DF/Y\r\n" + 
	            		"ZKNtrvRIcoE7zSN4MNPzhgm0JImzB2QI6LfrF0h8OjQ7MQcNrDcT+VuMt2rk2p63X5TN9topaEkp\r\n" + 
	            		"sGO/cUmgQJOgQdinY/LjsxrovON2AwAw05j1ibuOqvp9f1DwkLvcYc782ERhISgwABvT3Mz/ALiA\r\n" + 
	            		"Ey9CaZw1h8JM7SW5GHD4FDzBhOtD2BcgCQ8gOYNtDGvXVpWOo/ijTR32iA64fKTPAJ554cWb5e4/\r\n" + 
	            		"VAlEKRvpR2jjRoam2E8WoQd2LJIRJ31sg+Hr0PW6/KZvttFNkJznmwcTqfDsflpCHKSLbA4+DTOu\r\n" + 
	            		"jQTI4O/Mmv3h7HlT0+v6j4z0sJsuZwzHM0qYz9MXMxpPEbfrOGt7Vxs7rcpfrEQWg28BPEoSCImS\r\n" + 
	            		"Y1qQRx5fPChkSjKtES57pZz8M8UoFv2i0RIhbpmfyl4KaaHKrMW49frmO/ayXXJW1ruD8p63X5TN\r\n" + 
	            		"9torovZ2zYOHQV7H5aTfJJ2Bm3ZOjswhNp0CzGjtvlrnDAawEHWTNzxPmWNEqy/UfDW9n2GL42wY\r\n" + 
	            		"emwbnOMIjCQlbmQH4hHstBtTp8miNxxpQqAPQ12fBU5GND0Nht0TcR18uqt11pfCNn50SRS3QJ+U\r\n" + 
	            		"jzf2g8ZSdpe1TVgwSjAsuNct7OzcvynrdflM322it1i2TNQ7dxr2Py1ZRDQWjKD/AKN+L6Yhyd7k\r\n" + 
	            		"PxXUBkjM+o2RENr8Y154ja9fO64kJO5uwws7BVBMkFA8Co+su4GiYDGf4brkcjFwbjV9f84PDG0b\r\n" + 
	            		"4vpgQwIAgCsAJ7JRSbzYutJt5d7pKymS1/kcDSDJAJVNeKWgm3//ALIV0ua0nJrK7aib+2/kUZsq\r\n" + 
	            		"D48TkyYZY7Wk0Tw7TGTDFqMPbaAhKzTZzfZg4svwy7bZxw4QT5gJ+HTCUFdBOuLRlNEnEPbJnlwZ\r\n" + 
	            		"YJmBABY/+Ft+VZpBga0ZKkfkhEAM1wpQGZkOX/ghEAM1wpQGZkOX23xHOERm3HUw5aNTNNp5riQP\r\n" + 
	            		"5ynEYl2ldsSSfYmIBLZyaoRADNcAa6wBfZK25zwkQnJEcMhypvTEjxf4xYJYy5xdDbs0+DhkHJ0r\r\n" + 
	            		"c8luOG6qmWnhkORQeRsZPF3dMbH7aJoeDbs0qi9cIUXBx61+49a/cZrvxWKajawm7hYHXHrX7g1g\r\n" + 
	            		"7ANM/X0QyAxkJNLgO53YfhcIkjdBqWU1XuLLmLu6Da/Fu7yEK1MNAWPcv0hneNU7SIEvkeJd3Rq/\r\n" + 
	            		"FQyJBhHGaUE5/oRfeb/iIsKO7kJu7Dm+yRj5/ED6wDzo/YwMouQ3r9HTBgovxlcnQMLZ5tnnbhVE\r\n" + 
	            		"LopXBzIhEc2l0v0CLsHPCi332pLS4CEk28LoInWeOBS3OJwHnDP2YCB7hxIo52i5tDu5dKaZh8Tc\r\n" + 
	            		"LczBzxv6aMmaRFKyxt7tBE8TFlGG9jA53trOHhnk8TX6HeaWJAk2lgeUnL4iFgKkMxw/hBAbk9Dy\r\n" + 
	            		"j7FGPlJo6YVWVlaOhJPVP9j44xsC1CY9NqlP2SXatLiR96f8aLAVIZjgStpmiCmEja363HdUC+MH\r\n" + 
	            		"/SY5Um35vimSCImSYngPF8IdZ+IjdKB1sHnyfscY8Ah32ldzIaRx/JY4eg2P+422FFk6J9J+CXQp\r\n" + 
	            		"NHnsUGqSQRthP2pDeZKMpKpFqRbou0YcV7JLdqX8DQP3AAAAyCiA2T3rs0lZyQ7JUQZKjw3E4772\r\n" + 
	            		"6H5iciyDbW5yTBUzz5BuOEYiwdL8+wjiTARXgt+7/qu3knZNBkNzgZYQDC75z9uGVWZm03ohpuEI\r\n" + 
	            		"3FntUSJXuDB+tZGyE6L9xd02fssoSdESM7YT8rdyI6Rvyueu1uJ+1EMISjDhiZizyLZH2VGXALN9\r\n" + 
	            		"pdMNyp5Vl/PsI6/iDyBzEwh/z/YO/s26/BWea1x1PWzT4R9i8P8A4xzY/baPk3CImcW/4AidBWQu\r\n" + 
	            		"gFAsLzWkLvnBbEZXttfn2IdwVO0gITeGfedNuEOP/pHabyiZzkIVriYBkD/kTE88ZhUaCcm9pvJ+\r\n" + 
	            		"1C37RapbgOe4GQtQ0umASnYINc5Bh97tNVdMG2+AECl1goSd8ZhkHcbcxfnhpo77RI9cRXsxZM7y\r\n" + 
	            		"KPsg8UgVLoJeCHWKvKQFpbxcYs8uct2ofwqB/MIBBHJKCS2WN5vepGCPPMLmQuOq74FFmkjxuH8w\r\n" + 
	            		"YIEo2lDm8i7j8CquQW74XsPgI9TpxZwmobO5420zprpAodxz+yDueD65QS1gSwfSZ/zhJQyfkET6\r\n" + 
	            		"b1PUW1MxPGJ50nnEQ3UHQaIuCzVIPOOzhmIPGFmo09Ljsqs2XqG0cSKEoeMjtYDvCVG/RCiQecdr\r\n" + 
	            		"2YDx8BDgygOpHFgZ3vPBxvgRGEhPsQ+T9CnMEJG2pzwT1T3Ap7YdGWzF7DviQSgyR3LLDcdaGJOC\r\n" + 
	            		"MLl+HeHQxF+EET/kQeWFpsvsGEoRhXVv23JttphwAyBbgAOE4nMjsU6G4ypc4vzkBDy7BrSyFvCp\r\n" + 
	            		"bkYeWNzwQgxSFS8iw1dsWjbywnaWO5IABwnBoG0MjQNwQHCkoeUUsqw8/MECNyEi0Ozl0I+wjwKy\r\n" + 
	            		"WcoT8YROUldfioobgLhp78JqohB5tCxDcWeA6/H9s6SdAzXDtg5GJc+DYbgrKkaXIMjwLO4HY/H9\r\n" + 
	            		"5HgdDfhpU2u1zeGw3HzECNmYKXYToZdfsI8Vv3YBG3GzAZf8sB8QnuvxEQd0dzAyb7I8nZjPRORP\r\n" + 
	            		"rnbdgAAAZBUCAI5jhhKXGXVF+G5ww+DGuN8zxwwlZ7YZVmwQbrk8YwiXBCOEeA7/AAcWSLtc4Pyn\r\n" + 
	            		"DCiDa478cMJWe2HTYV7d16eMYQFGE5B4DnNb13nCRAl31mmvXecYESTfQ2SR7BCdHA2TTjTaDMkv\r\n" + 
	            		"ek2S8fgIbb/+Mu//2gAIAQEAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAACQCJB0gAAAAAAAAAAAAAACAkCIBAQAAAAAAAAAAAAA\r\n" + 
	            		"AgAICCgYAAAAAAAAAAAAAAITQCBAIAAAAAAAAAAAAAACFClKQCAAAAAAAAAAAAAAAhGBKlwEAAAA\r\n" + 
	            		"AAAAAAAAAAIEqWZCBAAAAAAAAAAAAAACEcEQehgAAAAAAAAAAAAAAiuDBgIgAAAAAAAAAAAAAAIC\r\n" + 
	            		"whNiIAAAAAAAAAAAAAACKCCUMjgAAAAAAAAAAAAAAgJkgAIAAAAAAAAAAAAAAAMYjw4W+AAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAc2HHM84kAAAAAAAAAAAAA5YwQAoCI\r\n" + 
	            		"wAAAAAAAAAAAABx0kkKAkgAAAAAAAAAAAAGSBgACiKgAAAAAAAAAAAAAIhC0QqKIAAAAAAAAAAAA\r\n" + 
	            		"AUBSDQKswAAAAAAAAAAAAABCQiRCgIhAAAAAAAAAAAAAQkAgAoaIQAAAAAAAAAAAAUAQTkKIzUAA\r\n" + 
	            		"AAAAAAAAAAEAQkgCgqhAAAAAAAAAAAAAlQYTIoCgQAAAAAAAAAAAAJw1ASKEpEAAAAAAAAAAAADg\r\n" + 
	            		"DQMChIgAAAAAAAAAAAAABEiExEMNQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAA4NjkAgAAAAAAAAAAAAAAAYEAhAKAAAAAAAAAAAAAAAAyESIKgAAAAAAAAAAAAAADQgIBC\r\n" + 
	            		"oAAAAAAAAAAAAAAAQIQARyAAAAAAAAAAAAAAAEQAEEkgAAAAAAAAAAAAAAAChMIAIAAAAAAAAAAA\r\n" + 
	            		"AAAAKiQSRaAAAAAAAAAAAAAAAAAEkECgAAAAAAAAAAAAAAABkcoHIAAAAAAAAAAAAAAASpxigSAA\r\n" + 
	            		"AAAAAAAAAAAAAEIDIIAgAAAAAAAAAAAAAABIDDYY4AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA\r\n" + 
	            		"AAAAAAAAAAAAAAAAAAAAAAD/xAAtEAEAAQMEAgIDAQACAgIDAAABERAhMQBBUWFxgSCRYKHwsTDB\r\n" + 
	            		"oNHh8UCwwP/aAAgBAQABPxD/APflw4ZiPJ5dba1vViYbYfyJU2hUyNhTEsTy0ZgJDRiRslYZcM28\r\n" + 
	            		"Kkg4vJw1Zs0AiHIShVjytZroqnVIiJEly7arNmiKxOQqDYmEpGHwONzIXkCIgYl7e6s2dyLXk4CI\r\n" + 
	            		"xX+TaDY6eQpQenIsRqZGZQn7EGslySabzlLldB3UhCjCOzpP3jZzzOe9MIbHShsJP/ximAPNZwCB\r\n" + 
	            		"0HxZs2bNwzjVDCuGEEt+N2/43GrSkYVwwHe/1UWMssZkV+vvWBx6VdOHiZ6K3NbmsR4j+W+GZwf2\r\n" + 
	            		"Cl6X4JV1HGWcL2IPZ8IAYyjWKwLgZSfHuwSahFKdoFZ60MBwBABsH5EuOW/43Gpz4PJKG1Taxfdb\r\n" + 
	            		"2bFbKxfYfLULo7WOUeT7qUlI1HEXfwxCrdJLg8wPpRsnMghZYc0tygGWQvgPgKgVWNDgG7KQs2DN\r\n" + 
	            		"Y/7TEWUi8KIeSQZ0XjyXN2rDryNtOAbuQ9g6HmMq6fwcMlnAtn4rs2TVJlY5OJirjmX65OynEpLn\r\n" + 
	            		"43b/AI3GpjBVgREwXnw5qiqWJuOnsE96MWQpIG4lAvzGSRhdKjzU2sZ2Wvev9x8MkIlNqGeg+qWx\r\n" + 
	            		"M0BkSYysJd6MBhyrJ/X7quTi65JXpD9CkUgkpEPROOtC9IgG+Ryibr+TLu1v+NxqxSzeOHY6ue6W\r\n" + 
	            		"VaMWScneW+02plV6ZZSPa73pNyWjvbfdkaTkfNIyr70033mgMup+u0xNymH/AAt3U1TxWDCPtH3S\r\n" + 
	            		"9kuWXN7tokrRrhYPcvqlvcymyYPmD9qW5INSIlG8oEd6TupCVOVd2gKYkJ6Qieoufydd2t/xuNb1\r\n" + 
	            		"DjfYP87YpLwDqynD3CTvHVFuKgY1fZ96Z+ckxjXcl902L5yN39oPgpv6OUOG8b+prgkdG65PEk/+\r\n" + 
	            		"1LnzeJvX9B70Ykat5LGPCvE0JzJxwK0zShDHJP0qxkX8Ehncfs0XRjBZIChMDEp8O/fuqB8VfIA3\r\n" + 
	            		"CZkbYmq4+5Q1kDEHcm9USJlDLfMQAktJ+N2/43GoAgLCBvAO16LwlCJWmWljAhOcTCPM9akYYHwS\r\n" + 
	            		"p82/DSHCQp7HFF4X8K6HdqzNAQNkqfFvypcgZODB8wfC6gHKQvjq9IHdSEKMI7OjBSCWzpG0KI2+\r\n" + 
	            		"Bz+/x/5Hv9d/a5fjdv8AjcaompNzT3Rveg1ILGDh2k90ntUc3egJLfETtpWviKJHsXrTuS+ycHtT\r\n" + 
	            		"SykbBaBG0gKTZ0Oxkd2qrZbe8EpXbmmZIUyEMbOl0KE5CE9XRK8JAJAlkYmSkWKKfIDXu42BRdvt\r\n" + 
	            		"ULEXgISbvn+baVUvriGJdiUoiU1rLmWUO5J8v65Cw8lkbCXKhgxYEuxNkUrafxu3/G40vcFzYDfN\r\n" + 
	            		"sZoUgMViUwX19cbxRZiItGt3ACPZkF71HaYtjJ9hqKgvI/Uw0UrookDt6dAbsQ+EPvmKtnDFDFk+\r\n" + 
	            		"Z0ABgcGVegdZGwSWcS+V8oRZYG6PB4/caVqfBiBFZABFeMlxHX+/yeL/AF3a3/G40CaAMSMQPlBR\r\n" + 
	            		"cCkyAVvuluAVCJQfMle+9EqqyuV1iYGllMXYTdd3WZ8NCAg7vW6e/wARmkbTOjCmwHCHkTeoXarp\r\n" + 
	            		"pJxpBbCI6/sf+tc+xiFECYYb9NBh+XMSsdx9/QERJHI6dDDrvcfLP7mimqQeW7Y3XAboNHj8KMEC\r\n" + 
	            		"yIiJz+TRf67tb/jcaIUmeef+Vv6mkOFqIQAZ80S6pquil6H296pdIubKfpEf+zpzix0TYp/dfAdJ\r\n" + 
	            		"FkwYtneN9EKIzrtw+/6KPYHbKCR3OhzRS4AgPqligKjcRPcP66pPTIo5Cr1ddTQ7CVclc+U3MbNT\r\n" + 
	            		"fJAiCyigl3qkwxGiZzzlxKIcvsO06vMZZLRDCSJZv8v65W029gA2iCL3rjxgqpuIwsYy/jdv+Nxo\r\n" + 
	            		"UlC9ezY5syOF4ovQCdi/UihL5LBKjQ8+wUMYkKVmAO1dSWF2Bl8lFfOjFsCSBsjqajbthhKW1XWE\r\n" + 
	            		"RYPVvrQXR2sco8n3QCBe4m6/0Hus5yna/QLQw/PMdInhDTcrZFCY6jSSNCRYDcRTVv8AP5cxDBcK\r\n" + 
	            		"mAhiQfkv9dFybVZ1MSZMU4vRy+mRmAFbcCiJaKgiws7DM3xRx8YRg9ycXjAtOxIFsH0upnLWmWNH\r\n" + 
	            		"d4HDeRwKBtiaj4KbURBKKQ4PBRwH5gpWacSSs2mNMhNarxkFkIJNwZo5eJJVscLEEljjN6jiz354\r\n" + 
	            		"yQCVC97VccZs7vi2jLLLExtFFYCblIQER4dS8SaknA2eoGwBq9atkniL7HT92R9GQPrTwo2hfeD3\r\n" + 
	            		"pdKcYHQC9I8mlQrys4mEAXWWWxime863CY5hsg/FxxxzNwltogSwDYv/AOGrDfcBaSSSWezF3pSC\r\n" + 
	            		"MT5OQ1CLuVK9v+WpBShbuVYeRo9AjiQJG98NHl45cxAB50xlQxsLWBfkg7LoCQWQZtAAfbTxaFyq\r\n" + 
	            		"CB4l50iBpd0JkRE2B+AOMXib+E4Y7rjxhLRuZZIkBGFuVOdxowwSkJQCWL1x425BRk/TdcmMlVYB\r\n" + 
	            		"AiqS3rTH0lJ/XOvTGShSmw2tWSL99ymMlFBXhuwX6WqUkhCDuIj8ceMD+L2ikSJNtn4K50W3BRAO\r\n" + 
	            		"3TsGQrvAnepslO6y5btGkBZC4Z3skuCT/wBJzsPJOr9NJMB9QYM7Ij+HiwoielCwAKrjU5IQWDDL\r\n" + 
	            		"PFbtQspH8ySZVXKu9ZEIhKyl6AVnaNDAcAQAbBpvHboxIAZQAXVAzpDEVKO8Q5TKsSgDNSVEGVmS\r\n" + 
	            		"zQfjJkRBBXFYFQUYJBIFxMMhRRsTDktRs2T64rc4nvuR4frtSeM+OVnwAHpXjFRleHvZ5a/xufzQ\r\n" + 
	            		"vir7VwPRlDz/AOBNauVkMbTvCQQJAgpjd4B2eDfO3Ygt8MrdKHADyugcCHMf5kPw9mprqLA3gWOV\r\n" + 
	            		"sQNA1igK3DEbwydh0Na5MyF8SefSPmzUSiQDIQVuNc49GElDCG6EcFFrUQYQBdVcaEpAs9bMq/aN\r\n" + 
	            		"SKcB18ifsSyIkjSbKwhHOpiS4qwSMgDcsr6T3W1Qom17jtJ+OlcfYe2RS9hB12rZGwOzcns/21/j\r\n" + 
	            		"c/mhfEKOAM9+yeXzTJnuMvHyaDYmWwunlTrE6xXBPtVSqtCHiC8Yd7paYu8ieITUkVEoFIzKhcz+\r\n" + 
	            		"H7DTHoGdCrsK7akS08bJbqq0fpZkUln7Nc2RkBJMwYb/ALVlVlVVVWpxSGIm3zQHaazEAAUd2paX\r\n" + 
	            		"IrFQoZwhqbFTulGd9wD12RsUJIRIjCOmRzUs3T0nH1dl0Tvd7rbT4LATL7XkKnDUK60H1fut1Vjh\r\n" + 
	            		"NsZ43eq/xufzQulimYFA7EHUXvalM/oo+SbFeFST5kRO+AoFQCVwGgqROfBKUMIVZ2givxFyyv0J\r\n" + 
	            		"BOGc76VNSU+EEeQAMOHZPwT4Hcr48Sf/ANqmCfIXSJ9c6CkYwuiWuk9V6WrAj4ZBmOL/AO40428E\r\n" + 
	            		"QYjgUX3RKQeIksN+Ve9RamHYE+w0q0rMzJf1DTsSi3ZPodchic75eVo76F1yioL68aTbA63eq/xu\r\n" + 
	            		"fyQuyyzwVsC24g+JpcUvoTEPAQgzPzTuLIoQJ+qpsTkaDKnQDqOUvSUL4IQdBSZq5p4ezehyL8TD\r\n" + 
	            		"KzxaTPYKJjLCEsE28ugvGbgQPQHwSO0cbj+ZxVGAK5BFRtjFCSBFBYHfNs1RteBnebxtSxzL6kmV\r\n" + 
	            		"7m/uaNiZEQinbmWixpAnaD6v3psOMsKqR2I9NEDwgHMHnr93VOnaEzYPdnuv8bn8kLpPj2j2FIet\r\n" + 
	            		"ZT+Pko7ETQaMk1YIRuzAdi20EfxJIkRMib66N9xYpjbFUyplJTMFSNxNVsJM5CQHGE8wcfiY/gjO\r\n" + 
	            		"Ub3uX+qDQj0YlC38Pxe5mCVhRLHo+q9RZFIDH1R6wwKEAMd3qAKrzkCLtk+1PGvZlkbZoDIZ6sMO\r\n" + 
	            		"d5/R3TJ8nN1UPQ+3XObRXXE8T/IUebJzAIrqZg2hXe+rar9ScMPdvuv8bn8kLyyBrA2EvyKaxono\r\n" + 
	            		"EqCcT6YeXUurWdcE8jIJ30DEQ2SYqGbyRvVNNclU4P8A4/3Wfa4XD/pOnkEMEYQuIgifBEiREtGn\r\n" + 
	            		"cxYgRExKd38OYmlUwjk9ieqM8kOksCv+fF75RC6SfX2VSS026CtFoECkwGLVYNuywVw9y+qIq0s5\r\n" + 
	            		"klzu3oS6qjbpm8BN9nFFCGyWzMPID20DKBB2yfd90KySPGVeJbTZOHSXA9Wr/G5/JC6KQUjFkU9i\r\n" + 
	            		"mpw4wLyS92FnAuhkcUQAVw8ruUaIRN1SK2TEjFoSCOXglD2VcwSgcpIeLI+/xN7/AEi8hATfPpt5\r\n" + 
	            		"dBrJNMSMfs0KgS9mT6T4G7ITZSeduHd90R1U5W5vF996OhDf6TZJMSwT3VoZHerL/d/pGShXELo8\r\n" + 
	            		"WobDK4JsCf3oQE3tneQmOzSwHQEImyak1OZhAaMiSpXHH3fenbsT5l9BxgESDfW5d6ten3r+QHq1\r\n" + 
	            		"f43P5IXf8QrxN/S0949ASNQ7JkdkNFUK8pNTyUSO4mpHHubJSPIxCbimu4hYCB7CPqqZ+7ZOkHJg\r\n" + 
	            		"b63xU2KpLSB/7D/1oSUGyNCjcRTX9j/1owULwNANgCPxD/iIgtwBP9fdQDCGWAw5ldb8P4SU3Ak2\r\n" + 
	            		"F4lGoWFNG8T7n90jRK65gT+sVzl3oiKeFHvUeb+CGFnkUO8LMFGT90C2+3BBI6DetOLIUhRcTRtz\r\n" + 
	            		"NbKgdDDSw0mPCeGdEZgGleYvqRXfwQpcHJa9qfxuf/AhelhJQLBSdjG5VoXDSSUEHgAOgqm3FLyd\r\n" + 
	            		"sN7LTOVUy0HpGg74vWCEV7AcoN/xOL/izYcXUjdk9nTJCIRIRpD6EQTzMrCqnkbLToKhBQzeVmzE\r\n" + 
	            		"ikNFBBS9jvTth4pHzzQHZozu13crsCeqQ52HITWwmE5AlBSVj5RH7KS7ZlUz7QAEN5QaEUYBEgT7\r\n" + 
	            		"Qrt0gxc1DC3qL+qLyGJXAbHd6ZhwuXT6PqqpkAWIb6frR+EBgI7/AHVF1qzklW6vT+Nz/wCBC9KR\r\n" + 
	            		"6DRKNHc4fKmR+2WAkS98jqgjswIDA7EHVunQtsILKL3ImVz8Ti/7g9bxRww092KJU/XdEfcT717D\r\n" + 
	            		"WBEck9zOn4oBLKDI/wDvFHxgXsK54ETzI2nSwHFEIm46jTiEghECZtvfpI/mSTKq5V3oRwnsiHJM\r\n" + 
	            		"f4WilSm2FS05f8TQ5kqS7G/0qQuplMt9grx/8kQINElGGxmJi3M1djki3+nu7p/G5/8ACheliQAI\r\n" + 
	            		"BXwiDo/FMuyS2ihJsSribkDSE0ssIA4RpKmAgukE/wC9BG0mUiCVe7k8zqw5jkuSAAlWxb8R/wAs\r\n" + 
	            		"67z8lrjuJCIIiDpNsd1iwLibGBhkskIhEhGqXcpGj4Yl5iHMiFwAFxB0pIRk0YgsGLwQncFSpiY4\r\n" + 
	            		"TcXNcnyJjQYqXH5U3RRdSt2nCVd1kbrd6pYECQ7l/sUwBLGxFDaSjxMML9B+tAAAgMBp0CCMgEn9\r\n" + 
	            		"1t8Mmk/16P43P/hQvSG6D954WLsaOB/kmksODiIDdaAAIWQAD9fUPBs5Pwybh8WP4yBFSckiSbj+\r\n" + 
	            		"I3K0LDmUe8dXxesMY+u53etdTiX9p+9O60slKsRuII1VKUI4Qosjw6Rcqtyu4HrJoKVksKOxdJxf\r\n" + 
	            		"6GpxgHw8j7D0EfxIIgAMAbVQ5BhgPNbAYVKHPdqayWZNcYmSEGv8T/1r9ZDWBCprFiBirLYgeqOe\r\n" + 
	            		"yimB4y+6xMFoKTvtjMplbcqOf3oehhxpm8wRrGE6IQwGLExejmQO1LzI4EwiVT1ByD0gRLOREyI1\r\n" + 
	            		"ccjnNavVCdMt1X4GjCAg4Ej7NX7EyE4mwdHWNmCfgkft08c8ewf4NTOxLQ8qY+E0agEOAsAGD/wW\r\n" + 
	            		"4axwASxde3V4gongJ8hgOqIAN10cX5RFa7Kdz7P+AYDqiADddHF+URWuync+z8tW5iko2VpHdyGR\r\n" + 
	            		"QTTiqYd1yI+Z3pzmw7b3W+7otiMY+0OphGwIaNFcAOowHVEAG66HIZ4eyJaDsriwJpPJ14HzR9AK\r\n" + 
	            		"Ig4zHnEtfsXhNENWZHCkXtqbksKoBVgMrqPjMK5Enhj6Ngs6vluGPnj6AoiAhkQdpfs0jGsrsZKq\r\n" + 
	            		"vNRZSlSP9XqoBcRBEqzZv3Y2fpV13HY1yEghSoQJSsF1WjNxt8wSRNgnY/H0xavE1KXQZAFIZaSq\r\n" + 
	            		"rK5X4SYHxkgMXqSeyoysQeBJJcscqwD8SkPWiMiFxG4mpKSbN2G3tEO2sIVTq4IbLC4t5YuRfAI/\r\n" + 
	            		"mQTIiYR30kvwIkDb5COw2AfHNhVhVjMPIZHB+SFkW5MJAazsS5G6d6SbjkVIXCpCbAl7EfrCZnBM\r\n" + 
	            		"rwDrQbGbPdyL44xpbSxicos5EiTcoZpJJiAgXZcexreRBZCdErbahnsDDEIEMqARQJgGnnVLs2FM\r\n" + 
	            		"jeJDuOitTjQANsKolhkmRo/wZEEAfYgU7QWlpf4nfC0QeYDsaWo4+5KDYJgNgDag7BvDEJCEUoIq\r\n" + 
	            		"UxOpgDtIhg4yLZJOGkB9kICwy8SiSWANqMkDRBGT1feFz8cxNdswJkR2R02XGESrIwSDst/yKyJH\r\n" + 
	            		"AlkkkSf5pkhEqsq0xS6uBn1Ye/jOvkTikuptYoi+EKV7mDDwFIhIfIyPMp9KhNdswJkR2R0YgJgR\r\n" + 
	            		"9Gip60qMFksUD7nqpfXsVwXN1F8AbUIHLeYFn1P2osBxRCJuOpwGMizVjiL18cxPAActAB7SA7ef\r\n" + 
	            		"yOyJ903GY3DN4UpvDSJscRTzduL7DI2QougEd83ElfkYlCH4IYZSkfYT++dOtZwlEWWf0qm4LiSE\r\n" + 
	            		"r7FvuOq9KJPAd/8AbqUJNkNjD7FufrW9+M/1T9jQwHAEAGwUN0WT7K8fC4quNoPAjGx/XM1zD8A8\r\n" + 
	            		"lBDuJ0W6W9iVKdN2lUvY3kf0E6ZDoshAHhEdL+iM5Ave5f6/IYl1TBeUOQITYFgRTHveGSDMlMEg\r\n" + 
	            		"jua4ArJVEl+1lWzDZVZ7SOZDaGxKt5d6ZTCJJtLYu9XtWficl3F9B7qyjsibN37WrmtnylGe8vCU\r\n" + 
	            		"LFpjIFCOyHuvJuMTtPuz3USs3la9n1/ua5jQAKwqGP1pFIlEAADu57pi4kJJA8Amjy0Z7kJGwR/3\r\n" + 
	            		"7/IYhcbdPIhZEURzpi3ycGWTJ7Xo4Eqmdot3aIyO5uO+Sf8A4Vi6wvZSmLIBqSIQyexPXwzPNEUg\r\n" + 
	            		"SgwSp1SiG0K2Tq7kLVXI9MJjJe8b7B+RRAREkcjq5ooqt7Zk4YzKWEK+BcjgEj5ERMNE2WMAZAmE\r\n" + 
	            		"SZ0ibikCHVgtJCbB0joAaURi3jB372ilvN5Ef6kVUqU2wqWnL/ia79TzMLj3h6NTHYAiwPa6pNI8\r\n" + 
	            		"wFR9LWFCeKZlB7FWZknCxgd7q/rb4Zi87EsRdTkWgrbaoAgHUA6Gs+0ABDeAOg8gaJi8xYd8/kkR\r\n" + 
	            		"Bm4sJO4QjaTZmjIkwNmoeOPnnSrhw4SJP0P/AHQWWBZ/zdBgOKJEdxoa2fTJUL0PtWUwGN8Ft9JE\r\n" + 
	            		"AMYXnzB8RTu6XJBi5fy0HI+KQkT1QQ737rtzgeXJ5KMCZTkKH6HQfHM94OzcTEc2nTpOn+mw6HI/\r\n" + 
	            		"R4/JInx8DATN6GqxlkcpEO4m8mkgsqYUk+HVc0/Wwh+0HiaAvASsLJ7PCU2nAOsGVuxoDYF3Yv6h\r\n" + 
	            		"qF3CHFknxKxoyzZAE9g8QoEMTZAYDZPwp6e+9pW5Rrc30I/6QHwzQaebccdHj7Uke6I+BaZIRCJC\r\n" + 
	            		"P5FEvCZMcgYADdV0BIKWhekr2dHVRTrEP72gkDAzIB2FBmZQlFqBUAlcBpZcCgvbds71SLAEBMPJ\r\n" + 
	            		"F2ND8s+Fb7A0R4ggkICQmIQkC4hpsysDlifDLPaw6JMchEmA2AAbAFGD4QXsiwwYZ6LoTcJbBHMZ\r\n" + 
	            		"2NMHcPCSh3LSO4jRD+ojFAIESSSEXIadambiFk8mVuJgdD2QCMQJmCAlgFHGTFIxk8j7Dz882abJ\r\n" + 
	            		"tuAQjsQgNuz8hiL7vzMnqNHStOyllfjBtgQii6y0MtnASKisMGkMFugIdoFn8TEcIxy2ylFgFdTm\r\n" + 
	            		"4NMc9SAG4BllaGyD3j4EXUj6QT4nJSDndRsBKqABW2kXNApCIpvEAuCbqvyzZlvKhIkPlh6wfP5D\r\n" + 
	            		"FchvUKkgjZRNxcZ0czLPAbTaaqs5zZMA2UXO2b8Ooj4TPDsmb6A324k5uasjfHu/VUO7lxdDAcAQ\r\n" + 
	            		"AbBUYDoCRHZNKGUEEXRFW+GGA6X1LDxZ2MjmO40bQosy5iVtC+W2hIKeRBYro/PQeRJG7YtJjiJU\r\n" + 
	            		"T8AIiSOR0thVaJ3RVt5BFoLtE41aGORtnmHkNG0KLMuYlbQvltpn4SSck5fSN50IKhhCykWkB6iS\r\n" + 
	            		"BrvRZiLwBKCVDmrjm9FmIvCMBJFOKYvwKK+8mhVkolQzgBACTCDRzfwykUyEvsw//wAZd//Z");

	            // Converting a Base64 String into Image byte array
	            
	            byte[] a = decodeImage(rawDataString);
	            product.setImage(a);
	            ByteArrayInputStream bais = new ByteArrayInputStream(product.getImage());
	            
	            st1.setString(1, product.getCode() + codeIndex + "");
	            ++ codeIndex;

	            st1.setDouble(2, product.getPrice());
	            st1.setString(3, product.getName());
	            st1.setBinaryStream(4, bais, product.getImage().length);

	            try {
	            	st1.executeUpdate();
	            } catch(Exception e) {
	            	e.printStackTrace();
	            }
	  		    
    		}
		} catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	try {
    		st1.close();
    		conn.close();
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    }
    
    /**
     * Decodes the base64 string into byte array
     *
     * @param imageDataString - a {@link java.lang.String}
     * @return byte array
     */
    public static byte[] decodeImage(String imageDataString) {
        return org.apache.commons.codec.binary.Base64.decodeBase64(imageDataString);
    }
    
    @RequestMapping(value = { "/order" }, method = RequestMethod.GET)
    public String orderView(Model model, @RequestParam("orderId") String orderId) {
        OrderInfo orderInfo = null;
        if (orderId != null) {
            orderInfo = this.orderDAO.getOrderInfo(orderId);
        }
        if (orderInfo == null) {
            return "redirect:/orderList";
        }
        List<OrderDetailInfo> details = this.orderDAO.listOrderDetailInfos(orderId);
        orderInfo.setDetails(details);
 
        model.addAttribute("orderInfo", orderInfo);
 
        return "order";
    }
    
}