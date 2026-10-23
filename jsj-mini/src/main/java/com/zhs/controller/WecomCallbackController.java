package com.zhs.controller;
import com.zhs.common.NoLoginRequest;
import com.zhs.service.acq.flow.WecomCallbackAdapter;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController @NoLoginRequest @RequestMapping("/api/wecom/callback")
public class WecomCallbackController {
    @Resource private WecomCallbackAdapter adapter;
    @GetMapping("/{sid}") public ResponseEntity<String> verify(@PathVariable("sid") String sid,@RequestParam("msg_signature") String signature,@RequestParam("timestamp") String timestamp,@RequestParam("nonce") String nonce,@RequestParam("echostr") String echo){try{return ResponseEntity.ok(adapter.verify(sid,signature,timestamp,nonce,echo));}catch(RuntimeException e){return ResponseEntity.status(403).body("verification failed");}}
    @PostMapping("/{sid}") public ResponseEntity<String> event(@PathVariable("sid") String sid,@RequestParam("msg_signature") String signature,@RequestParam("timestamp") String timestamp,@RequestParam("nonce") String nonce,@RequestBody String body){try{adapter.accept(sid,signature,timestamp,nonce,body);return ResponseEntity.ok("success");}catch(RuntimeException e){return ResponseEntity.status(500).body("processing failed");}}
}
