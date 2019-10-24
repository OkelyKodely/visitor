package org.o7planning.springmvcshoppingcart.dao.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.o7planning.springmvcshoppingcart.dao.OrderDAO;
import org.o7planning.springmvcshoppingcart.dao.ProductDAO;
import org.o7planning.springmvcshoppingcart.entity.Order;
import org.o7planning.springmvcshoppingcart.entity.OrderDetail;
import org.o7planning.springmvcshoppingcart.entity.Product;
import org.o7planning.springmvcshoppingcart.model.CartInfo;
import org.o7planning.springmvcshoppingcart.model.CartLineInfo;
import org.o7planning.springmvcshoppingcart.model.CustomerInfo;
import org.o7planning.springmvcshoppingcart.model.OrderDetailInfo;
import org.o7planning.springmvcshoppingcart.model.OrderInfo;
import org.o7planning.springmvcshoppingcart.model.PaginationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;

import com.nimbusds.oauth2.sdk.Message;
import com.nimbusds.openid.connect.sdk.claims.Address;

import net.authorize.Environment;
import net.authorize.api.contract.v1.ANetApiResponse;
import net.authorize.api.contract.v1.CreateTransactionRequest;
import net.authorize.api.contract.v1.CreateTransactionResponse;
import net.authorize.api.contract.v1.CreditCardType;
import net.authorize.api.contract.v1.CustomerDataType;
import net.authorize.api.contract.v1.MerchantAuthenticationType;
import net.authorize.api.contract.v1.MessageTypeEnum;
import net.authorize.api.contract.v1.PaymentType;
import net.authorize.api.contract.v1.TransactionRequestType;
import net.authorize.api.contract.v1.TransactionResponse;
import net.authorize.api.contract.v1.TransactionTypeEnum;
import net.authorize.api.controller.CreateTransactionController;
import net.authorize.api.controller.base.ApiOperationBase;

//Transactional for Hibernate
@Transactional
public class OrderDAOImpl implements OrderDAO {
 

    private static void sendFromGMail(String from, String pass, String[] to, String subject, String body) {
        Properties props = System.getProperties();
        props.put("mail.smtp.host", "smtp.gmail.com");    
        props.put("mail.smtp.socketFactory.port", "465");    
        props.put("mail.smtp.socketFactory.class",    
                  "javax.net.ssl.SSLSocketFactory");    
        props.put("mail.smtp.auth", "true");    
        props.put("mail.smtp.port", "465"); 
        
        javax.mail.Session session = javax.mail.Session.getDefaultInstance(props);
        MimeMessage message = new MimeMessage(session);

        try {
            message.setFrom(new InternetAddress(from));
            InternetAddress[] toAddress = new InternetAddress[to.length];

            // To get the array of addresses
            for( int i = 0; i < to.length; i++ ) {
                toAddress[i] = new InternetAddress(to[i]);
            }

            for( int i = 0; i < toAddress.length; i++) {
                message.addRecipient(javax.mail.Message.RecipientType.TO, toAddress[i]);
            }

            message.setSubject(subject);
            message.setText(body);
            Transport transport = session.getTransport("smtp");
            transport.connect("smtp.gmail.com", from, pass);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
        }
        catch (AddressException ae) {
            ae.printStackTrace();
        }
        catch (MessagingException me) {
            me.printStackTrace();
        }
    }

	@Autowired
    private SessionFactory sessionFactory;
 
    @Autowired
    private ProductDAO productDAO;
 
    private int getMaxOrderNum() {
        String sql = "Select max(o.orderNum) from " + Order.class.getName() + " o ";
        Session session = sessionFactory.getCurrentSession();
        Query query = session.createQuery(sql);
        Integer value = (Integer) query.uniqueResult();
        if (value == null) {
            return 0;
        }
        return value;
    }
 
    public void saveOrder(CartInfo cartInfo, Model model) {

    	Session session = sessionFactory.getCurrentSession();
        int orderNum = getMaxOrderNum() + 1;
        Order order = new Order();
 
        order.setId(UUID.randomUUID().toString());
        order.setOrderNum(orderNum);
        order.setOrderDate(new Date());
        order.setAmount(cartInfo.getAmountTotal());
 
        CustomerInfo customerInfo = cartInfo.getCustomerInfo();
        order.setCustomerName(customerInfo.getName());
        order.setCustomerEmail(customerInfo.getEmail());
        order.setCustomerPhone(customerInfo.getPhone());
        order.setCustomerAddress(customerInfo.getAddress());
        order.setCardnum(customerInfo.getCardnum());
        order.setCardname(customerInfo.getCardname());
        order.setExpire(customerInfo.getExpire());
        order.setCode(customerInfo.getCode());

		// Set the request to operate in either the sandbox or production environment
        ApiOperationBase.setEnvironment(Environment.SANDBOX);
        
        // Create object with merchant authentication details
        MerchantAuthenticationType merchantAuthenticationType  = new MerchantAuthenticationType();
        merchantAuthenticationType.setName("5R2D3Ahbd");
        merchantAuthenticationType.setTransactionKey("52Z2RgcVp27wr986");
        ApiOperationBase.setMerchantAuthentication(merchantAuthenticationType);
        
        // Populate the payment data
        PaymentType paymentType = new PaymentType();
        CreditCardType creditCard = new CreditCardType();
        creditCard.setCardNumber(order.getCardnum());
        creditCard.setExpirationDate(order.getExpire());
        paymentType.setCreditCard(creditCard);

        // Set email address (optional)
        CustomerDataType customer = new CustomerDataType();
        customer.setEmail(order.getCustomerEmail());
        

        // Create the payment transaction object
        TransactionRequestType txnRequest = new TransactionRequestType();
        txnRequest.setTransactionType(TransactionTypeEnum.AUTH_CAPTURE_TRANSACTION.value());
        txnRequest.setPayment(paymentType);
        txnRequest.setCustomer(customer);
        txnRequest.setAmount(new BigDecimal(order.getAmount()).setScale(2, RoundingMode.CEILING));

        // Create the API request and set the parameters for this specific request
        CreateTransactionRequest apiRequest = new CreateTransactionRequest();
        apiRequest.setMerchantAuthentication(merchantAuthenticationType);
        apiRequest.setTransactionRequest(txnRequest);

        // Call the controller
        CreateTransactionController controller = new CreateTransactionController(apiRequest);
        controller.setEnvironment(Environment.SANDBOX);
        controller.setMerchantAuthentication(merchantAuthenticationType);
        controller.execute();

        // Get the response
        CreateTransactionResponse response = new CreateTransactionResponse();
        response = controller.getApiResponse();
        // Parse the response to determine results
        if (response!=null) {
            // If API Response is OK, go ahead and check the transaction response
            if (response.getMessages().getResultCode() == MessageTypeEnum.OK) {
                TransactionResponse result = response.getTransactionResponse();

                if (result.getMessages() != null) {
                	model.addAttribute("auth_response", "Credit card order processed successfully!");
                    System.out.println("Successfully created transaction with Transaction ID: " + result.getTransId());
                    System.out.println("Response Code: " + result.getResponseCode());
                    System.out.println("Message Code: " + result.getMessages().getMessage().get(0).getCode());
                    System.out.println("Description: " + result.getMessages().getMessage().get(0).getDescription());
                    System.out.println("Auth Code: " + result.getAuthCode());

            		String hostName = "localhost:3320";
            		String dbName = "mydb";
            		String userName = "root";
            		String passWord = "";

            		Connection conn = null;
            		
            		PreparedStatement st1 = null;
            		
            		try {

            			try {
                		    Class.forName("com.mysql.jdbc.Driver");
                		    String url = "jdbc:mysql://" + hostName + "/" + dbName;
                		    conn = DriverManager.getConnection(url, userName, passWord);
                		} catch(Exception e) {
                			e.printStackTrace();
                		}

                		st1 = conn.prepareStatement(
                					"insert into orders (id, order_num, order_date, amount, customer_name, customer_email, customer_phone, "
                								+ "customer_address, cardnum, cardname, expire, code) values (?,?,now(),?,?,?,?,?,?,?,?,?)");

                		st1.setString(1, order.getId()+"");
                		st1.setString(2, order.getOrderNum()+"");
                		st1.setString(3, order.getAmount()+"");
                		st1.setString(4, order.getCustomerName()+"");
                		st1.setString(5, order.getCustomerEmail()+"");
                		st1.setString(6, order.getCustomerPhone()+"");
                		st1.setString(7, order.getCustomerAddress()+"");
                		st1.setString(8, order.getCardnum()+"");
                		st1.setString(9, order.getCardname()+"");
                		st1.setString(10, order.getExpire()+"");
                		st1.setString(11, order.getCode()+"");
                		
	            		st1.execute();
	            		st1.close();
	            		conn.close();

	            	    String USER_NAME = "dh.cho428@gmail.com";  // GMail user name (just the part before "@gmail.com")
	            	    String PASSWORD = "Wasada428"; // GMail password
	            	    String RECIPIENT = order.getCustomerEmail();

	            	        String from = USER_NAME;
	            	        String pass = PASSWORD;
	            	        String[] to = { RECIPIENT }; // list of recipient email addresses
	            	        String subject = "OrangeShoppe Sales Order - " + orderNum;
	            	        String body = null;
	                        List<CartLineInfo> lines = cartInfo.getCartLines();
	                        String str = "You have now made a purchase of: \n\n";
	                        for (CartLineInfo line1 : lines) {
	                            OrderDetail detail = new OrderDetail();
	                            detail.setId(UUID.randomUUID().toString());
	                            detail.setOrder(order);
	                            detail.setAmount(line1.getAmount());
	                            detail.setPrice(line1.getProductInfo().getPrice());
	                            detail.setQuanity(line1.getQuantity());
	                            String code = line1.getProductInfo().getCode();
	                            Product product = productDAO.findProduct(code);
	                            detail.setProduct(product);
	                            str += "" + code + " with " + detail.getQuanity() + " qty " + " for $" +
	                            detail.getAmount() * detail.getQuanity() + "\n";
	                        }
	                        str += "\n\n" +
	                        "" + "Total Paid: $" + order.getAmount() + "\n\n\nThank you for your sales, we will do more together again soon!  come again\n\n\nTHE OrageShoppe Team";

	                        body = str;

	            	        OrderDAOImpl.sendFromGMail(from, pass, to, subject, body);
	                    
            		} catch(SQLException sqle) {
            			sqle.printStackTrace();
            		}
                    
                    List<CartLineInfo> lines = cartInfo.getCartLines();
             
                    for (CartLineInfo line1 : lines) {
                        OrderDetail detail = new OrderDetail();
                        detail.setId(UUID.randomUUID().toString());
                        detail.setOrder(order);
                        detail.setAmount(line1.getAmount());
                        detail.setPrice(line1.getProductInfo().getPrice());
                        detail.setQuanity(line1.getQuantity());
             
                        String code = line1.getProductInfo().getCode();
                        Product product = productDAO.findProduct(code);
                        detail.setProduct(product);
             
                        session.persist(detail);
                    }
             
                    // Set OrderNum for report.
                    // Set OrderNum d? thông báo cho ngu?i dùng.
                    cartInfo.setOrderNum(orderNum);
                
                } else {
                	model.addAttribute("auth_response", "Credit card order did not processed successfully!");
                    System.out.println("Failed Transaction.");
                    if (response.getTransactionResponse().getErrors() != null) {
                        System.out.println("Error Code: " + response.getTransactionResponse().getErrors().getError().get(0).getErrorCode());
                        System.out.println("Error message: " + response.getTransactionResponse().getErrors().getError().get(0).getErrorText());
                    }
                }
            } else {
                System.out.println("Failed Transaction.");
            	model.addAttribute("auth_response", "Credit card order did not processed successfully!");
                if (response.getTransactionResponse() != null && response.getTransactionResponse().getErrors() != null) {
                    System.out.println("Error Code: " + response.getTransactionResponse().getErrors().getError().get(0).getErrorCode());
                    System.out.println("Error message: " + response.getTransactionResponse().getErrors().getError().get(0).getErrorText());
                } else {
                    System.out.println("Error Code: " + response.getMessages().getMessage().get(0).getCode());
                    System.out.println("Error message: " + response.getMessages().getMessage().get(0).getText());
                }
            }
        } else {
            // Display the error code and message when response is null 
            ANetApiResponse errorResponse = controller.getErrorResponse();
        	model.addAttribute("auth_response", "Credit card order did not processed successfully!");
            System.out.println("Failed to get response");
            System.out.println("sdfasdfa");
            if (!errorResponse.getMessages().getMessage().isEmpty()) {
                System.out.println("Error: "+errorResponse.getMessages().getMessage().get(0).getCode()+" \n"+ errorResponse.getMessages().getMessage().get(0).getText());
            }
        }
    }
 
    // @page = 1, 2, ...
    public PaginationResult<OrderInfo> listOrderInfo(int page, int maxResult, int maxNavigationPage) {
        String sql = "Select new " + OrderInfo.class.getName()//
                + "(ord.id, ord.orderDate, ord.orderNum, ord.amount, "
                + " ord.customerName, ord.customerAddress, ord.customerEmail, ord.customerPhone, ord.cardnum, ord.cardname, ord.expire, ord.code) " + " from "
                + Order.class.getName() + " ord "//
                + " order by ord.orderNum desc";
        Session session = this.sessionFactory.getCurrentSession();
 
        Query query = session.createQuery(sql);
 
        return new PaginationResult<OrderInfo>(query, page, maxResult, maxNavigationPage);
    }
 
    public Order findOrder(String orderId) {
        Session session = sessionFactory.getCurrentSession();
        Criteria crit = session.createCriteria(Order.class);
        crit.add(Restrictions.eq("id", orderId));
        return (Order) crit.uniqueResult();
    }
 
    public OrderInfo getOrderInfo(String orderId) {
        Order order = this.findOrder(orderId);
        if (order == null) {
            return null;
        }
        return new OrderInfo(order.getId(), order.getOrderDate(), //
                order.getOrderNum(), order.getAmount(), order.getCustomerName(), //
                order.getCustomerAddress(), order.getCustomerEmail(), order.getCustomerPhone(), //
                order.getCardnum(), order.getCardname(), order.getExpire(), order.getCode());
    }
 
    public List<OrderDetailInfo> listOrderDetailInfos(String orderId) {
        String sql = "Select new " + OrderDetailInfo.class.getName() //
                + "(d.id, d.product.code, d.product.name , d.quanity,d.price,d.amount) "//
                + " from " + OrderDetail.class.getName() + " d "//
                + " where d.order.id = :orderId ";
 
        Session session = this.sessionFactory.getCurrentSession();
 
        Query query = session.createQuery(sql);
        query.setParameter("orderId", orderId);
 
        return query.list();
    }
}