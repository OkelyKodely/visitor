<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
 
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Product</title>
 
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/styles.css">

<link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css" integrity="sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T" crossorigin="anonymous">
 
</head>
<body>
 
   <jsp:include page="_header.jsp" />
   <jsp:include page="_menu.jsp" />
 
   <div class="page-title">Product</div>
  
   <c:if test="${not empty errorMessage }">
     <div class="error-message">
         ${errorMessage}
     </div>
   </c:if>
 
<div style="float:left"> 
   <form:form modelAttribute="productForm" method="POST">
       <table style="text-align:left;">
           <tr>
               <td>Code</td>
               <td style="color:red;">
                  <c:if test="${not empty productForm.code}">
                       <form:hidden path="code"/>
                       ${productForm.code}
                  </c:if>
               </td>
           </tr>
 
           <tr>
               <td>Name</td>
               <td>${productForm.name}</td>
           </tr>
 
           <tr>
               <td>Price</td>
               <td>${productForm.price}</td>
           </tr>
           <tr>
               <td>Description</td>
               <td>${productForm.description}</td>
           </tr>
           <tr>
               <td>Image</td>
               <td>
               <img src="${pageContext.request.contextPath}/productImage?code=${productForm.code}" width="100"/></td>
               <td> </td>
           </tr>
           <tr>
             	<td colspan="2"><a
                   href="${pageContext.request.contextPath}/buyProduct?code=${productForm.code}">
                       Buy Now</a></td>
           </tr>
           
       </table>
   </form:form>
</div>
 
   <br><br><br><br>
   <br><br><br><br>
   <br><br><br><br>
   <br><br><br><br>
   <br><br><br><br>
   <br><br><br><br>
   <br><br><br><br>
   <br><br><br><br>
   <jsp:include page="_footer.jsp" />
 
</body>
</html>