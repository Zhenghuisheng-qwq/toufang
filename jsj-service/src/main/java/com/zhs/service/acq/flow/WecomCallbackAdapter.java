package com.zhs.service.acq.flow;
import com.zhs.common.exception.BusinessException;
import com.zhs.service.mapper.acq.WecomFlowMapper;
import com.zhs.service.model.acq.WecomCorp;
import jakarta.annotation.Resource;
import me.chanjar.weixin.cp.config.impl.WxCpDefaultConfigImpl;
import me.chanjar.weixin.cp.util.crypto.WxCpCryptUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;
import java.io.StringReader;
import org.w3c.dom.Document;

@Component
public class WecomCallbackAdapter {
    @Resource private WecomFlowMapper flow;
    @Resource private PlatformSecrets secrets;
    @Resource private WecomCallbackService callbacks;
    @Value("${acq.wecom.callbacks.enabled:false}") private boolean enabled;
    private WecomCorp corp(String sid){if(!enabled)throw new BusinessException("企微回调未启用");var c=flow.corp(sid);if(c==null||!"SELF_BUILT".equals(c.getAuthMode()))throw new BusinessException("回调企业授权模式不支持");return c;}
    private WxCpCryptUtil crypt(WecomCorp c){var config=new WxCpDefaultConfigImpl();config.setCorpId(c.getCorpId());config.setToken(secrets.resolve(c.getTokenRef()));config.setAesKey(secrets.resolve(c.getEncodingAesKeyRef()));return new CorpBoundWecomCrypt(config);}
    public String verify(String sid,String signature,String time,String nonce,String echo){return crypt(corp(sid)).decryptContent(signature,time,nonce,echo);}
    public void accept(String sid,String signature,String time,String nonce,String body){
        if(body.length()>65536||body.contains("<!DOCTYPE")||body.contains("<!ENTITY"))throw new BusinessException("回调报文不正确");
        var corp=corp(sid);String plain=crypt(corp).decrypt(signature,time,nonce,body);
        try{var factory=DocumentBuilderFactory.newInstance();factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);factory.setFeature("http://xml.org/sax/features/external-general-entities",false);factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);factory.setXIncludeAware(false);factory.setExpandEntityReferences(false);var doc=factory.newDocumentBuilder().parse(new InputSource(new StringReader(plain)));
            if(!"change_external_contact".equals(value(doc,"Event")))return;
            callbacks.accept(corp.getId(),new WecomCallbackService.Event(value(doc,"ToUserName"),value(doc,"ChangeType"),value(doc,"ExternalUserID"),value(doc,"UserID"),value(doc,"State"),Long.parseLong(value(doc,"CreateTime"))));
        }catch(BusinessException e){throw e;}catch(Exception e){throw new BusinessException("企微事件格式不正确");}
    }
    private static String value(Document doc,String tag){var nodes=doc.getElementsByTagName(tag);return nodes.getLength()==1?nodes.item(0).getTextContent():null;}
}
