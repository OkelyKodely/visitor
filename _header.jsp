<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
 <style>
.logo, .logo a {
overflow: visible;
}

.logo img {
max-height: 150px !important;
} 
 </style>
<div class="header-container">
 
    <div class="logo">
    <img style="position:relative;top:30px;left:50px" src="logo.jpg" class="logo">
    </div>
 
    <div class="header-bar" style="color:white;background-color:#aaa">
        <c:if test="${pageContext.request.userPrincipal.name != null}">
        Hello
           <a href="${pageContext.request.contextPath}/accountInfo">
                ${pageContext.request.userPrincipal.name} </a>
         &nbsp;|&nbsp;
           <a href="${pageContext.request.contextPath}/logout">Logout</a>
 
        </c:if>
        <c:if test="${pageContext.request.userPrincipal.name == null}">
            <a href="${pageContext.request.contextPath}/login">Login</a>
        </c:if>
    </div>
</div>
