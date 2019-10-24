package org.o7planning.springmvcshoppingcart.dao;
 
import java.util.List;
 
import org.o7planning.springmvcshoppingcart.model.CartInfo;
import org.o7planning.springmvcshoppingcart.model.OrderDetailInfo;
import org.o7planning.springmvcshoppingcart.model.OrderInfo;
import org.o7planning.springmvcshoppingcart.model.PaginationResult;
import org.springframework.ui.Model;
 
public interface OrderDAO {
 
    public void saveOrder(CartInfo cartInfo, Model model);
 
    public PaginationResult<OrderInfo> listOrderInfo(int page,
            int maxResult, int maxNavigationPage);
    
    public OrderInfo getOrderInfo(String orderId);
    
    public List<OrderDetailInfo> listOrderDetailInfos(String orderId);
 
}