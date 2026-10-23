package com.zhs.controller;

import com.zhs.common.NoLoginRequest;
import com.zhs.common.Result;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.acq.flow.*;
import jakarta.annotation.Resource;
import jakarta.servlet.http.*;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.Map;

/** Same-origin anonymous API. The mobile-account authentication remains unchanged. */
@RestController
@RequestMapping("/api/h5")
@NoLoginRequest
public class H5Controller {
    private static final String COOKIE="acq_session";
    @Resource private H5FlowService flow;
    @Resource private H5CatalogService catalog;
    @Resource private PaymentApplicationService payments;
    @Resource private OfficialAccountOAuthService oauth;
    @PostMapping("/orders/{no}/oauth") public Result<?> oauth(@PathVariable("no") String no,HttpServletRequest request){sameOrigin(request);return Result.ok(Map.of("url",oauth.begin(token(request),request.getServerName(),no)));}
    @PostMapping("/orders/{no}/jsapi") public Result<?> jsapi(@PathVariable("no") String no,HttpServletRequest request){sameOrigin(request);return Result.ok(payments.jsapi(token(request),request.getServerName(),no));}
    @GetMapping("/oauth/callback") public org.springframework.http.ResponseEntity<Void> oauthCallback(@RequestParam("state") String state,@RequestParam(name="code",required=false) String code,HttpServletRequest request){
        String path=oauth.callback(token(request),request.getServerName(),state,code);
        if(path==null||!path.matches("/orders/[a-zA-Z0-9_-]{1,32}"))throw new BusinessException("订单返回地址不正确");
        return org.springframework.http.ResponseEntity.status(302).header("Cache-Control","no-store").header("Referrer-Policy","no-referrer").location(URI.create(path)).build();
    }
    @PostMapping("/orders/{no}/payment") public Result<?> payment(@PathVariable("no") String no,HttpServletRequest request){sameOrigin(request);return Result.ok(Map.of("url",payments.start(token(request),request.getServerName(),no,request.getRemoteAddr())));}
    @PostMapping("/orders/{no}/query") public Result<?> query(@PathVariable("no") String no,HttpServletRequest request){sameOrigin(request);return Result.ok(Map.of("paid",payments.query(token(request),request.getServerName(),no)));}
    @GetMapping("/pages/{id}") public Result<?> page(@PathVariable("id") Long id,HttpServletRequest request){return Result.ok(catalog.publicPage(id,request.getServerName()));}
    @PostMapping("/session") public Result<?> session(HttpServletRequest request,HttpServletResponse response){
        sameOrigin(request);String token=flow.session(request.getServerName(),token(request));
        response.addHeader("Set-Cookie",ResponseCookie.from(COOKIE,token).httpOnly(true).secure(request.isSecure()).sameSite("Lax").path("/api/h5").maxAge(30L*24*60*60).build().toString());
        response.setHeader("Cache-Control","no-store");return Result.ok();
    }
    @PostMapping("/visits") public Result<?> visit(@RequestBody H5Commands.Visit input,HttpServletRequest request){sameOrigin(request);return Result.ok(Map.of("id",flow.visit(token(request),request.getServerName(),input)));}
    @PostMapping("/leads") public Result<?> lead(@RequestBody H5Commands.Lead input,HttpServletRequest request){sameOrigin(request);return Result.ok(Map.of("id",flow.lead(token(request),request.getServerName(),input)));}
    @PostMapping("/orders") public Result<?> order(@RequestBody H5Commands.Order input,HttpServletRequest request){sameOrigin(request);return Result.ok(Map.of("orderNo",flow.order(token(request),request.getServerName(),input)));}
    @GetMapping("/orders/{no}") public Result<?> status(@PathVariable("no") String no,HttpServletRequest request,HttpServletResponse response){
        response.setHeader("Cache-Control","no-store");var o=flow.ownedOrder(token(request),request.getServerName(),no);
        return Result.ok(Map.of("orderNo",o.getOrderNo(),"amountFen",o.getAmountFen(),"payStatus",o.getPayStatus(),"title",o.getPageTitle()==null?"付费报告":o.getPageTitle(),"tradeType",o.getTradeType(),"oauthReady",flow.oauthReady(token(request),request.getServerName(),no)));
    }
    @GetMapping("/orders/{no}/report") public Result<?> report(@PathVariable("no") String no,HttpServletRequest request,HttpServletResponse response){
        response.setHeader("Cache-Control","no-store");return Result.ok(flow.report(token(request),request.getServerName(),no));
    }
    @GetMapping("/orders/{no}/entry") public Result<?> entry(@PathVariable("no") String no,HttpServletRequest request,HttpServletResponse response){response.setHeader("Cache-Control","no-store");return Result.ok(flow.entry(token(request),request.getServerName(),no));}
    static String token(HttpServletRequest request){if(request.getCookies()!=null)for(var c:request.getCookies())if(COOKIE.equals(c.getName()))return c.getValue();return null;}
    static void sameOrigin(HttpServletRequest request){
        String raw=request.getHeader("Origin");
        try{var origin=URI.create(raw==null?"":raw);int port=origin.getPort()==-1?("https".equals(origin.getScheme())?443:80):origin.getPort();
            if(!request.getScheme().equals(origin.getScheme())||!request.getServerName().equalsIgnoreCase(origin.getHost())||request.getServerPort()!=port||origin.getRawUserInfo()!=null)throw new IllegalArgumentException();
        }catch(IllegalArgumentException e){throw new BusinessException("请求来源不匹配");}
        if(!"1".equals(request.getHeader("X-H5-Request")))throw new BusinessException("请求校验缺失");
    }
}
