package com.zhs.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wechat.pay.java.core.notification.RequestParam;
import com.zhs.common.NoLoginRequest;
import com.zhs.service.acq.flow.*;
import com.zhs.service.mapper.acq.TradeOrderMapper;
import com.zhs.service.model.acq.TradeOrder;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@NoLoginRequest
@RequestMapping("/api/payments")
public class PaymentNotificationController {
    @Resource private TradeOrderMapper orders;
    @Resource private WechatPaymentAdapter adapter;
    @Resource private PaymentApplicationService payments;
    @Resource private ObjectMapper json;
    @PostMapping("/notify/{no}") public ResponseEntity<Void> notify(@PathVariable("no") String no,@RequestBody String body,HttpServletRequest request){
        if(body.length()>65536)return ResponseEntity.badRequest().build();
        var order=orders.selectOne(new LambdaQueryWrapper<TradeOrder>().eq(TradeOrder::getOrderNo,no));
        if(order==null)return ResponseEntity.notFound().build();
        try{
            var param=new RequestParam.Builder().serialNumber(request.getHeader("Wechatpay-Serial")).timestamp(request.getHeader("Wechatpay-Timestamp"))
                    .nonce(request.getHeader("Wechatpay-Nonce")).signature(request.getHeader("Wechatpay-Signature")).signType(request.getHeader("Wechatpay-Signature-Type")).body(body).build();
            var tx=adapter.notification(order,param);
            String id=json.readTree(body).path("id").asText();
            payments.confirm(order,tx,PaymentConfirmationService.Source.NOTIFY,id,FlowSecurity.hash(body));
            return ResponseEntity.noContent().build();
        }catch(com.wechat.pay.java.core.exception.ValidationException ex){return ResponseEntity.status(401).build();}
        catch(Exception ex){return ResponseEntity.status(500).build();}
    }
}
