package com.huobi.demo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.util.HashMap;
import java.util.Map;


@Slf4j
public class WebSocket extends WebSocketClient {

  private URI uri;
  private String accessKey;
  private String secretKey;

  public WebSocket(URI uri, String accessKey, String secretKey) {
    super(uri, new Draft_17());
    this.uri = uri;
    this.accessKey = accessKey;
    this.secretKey = secretKey;
  }


  @Override
  public void onOpen(ServerHandshake shake) {
    addAuth();
  }

  @Override
  public void onMessage(String arg0) {
    if (arg0 != null) {
      log.info("receive message " + arg0);
    }

  }

  @Override
  public void onError(Exception arg0) {
    String message = "";
    try {
      message = new String(arg0.getMessage().getBytes(), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    log.info("has error ,the message is :" + message);
    arg0.printStackTrace();
  }

  @Override
  public void onClose(int arg0, String arg1, boolean arg2) {

    log.info("connection close ");
    log.info(arg0 + "   " + arg1 + "  " + arg2);
  }

  @Override
  public void onMessage(ByteBuffer bytes) {

    try {

      String message = new String(ZipUtil.decompress(bytes.array()), "UTF-8");
      JSONObject jsonObject = JSONObject.parseObject(message);
      String op = jsonObject.getString("op");
      Integer errCode = jsonObject.getInteger("err-code");

      if ("ping".equals(op)) {
        String pong = jsonObject.toString();
        send(pong.replace("ping", "pong"));
      } else if ("auth".equals(op)) {
        //鉴权结果

        if (errCode != 0) {
          log.info(message);
        } else {
          //鉴权成功发送sub 请求
          sendSub("accounts", "12123");

        }

      } else {
        log.info(message);
      }
    } catch (CharacterCodingException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * 请求鉴权
   */
  public void addAuth() {
    Map<String, String> map = new HashMap<>();
    ApiSignature as = new ApiSignature();
    try {

      String theHost = uri.getHost() + ":" + uri.getPort();
      //组合签名map
      as.createSignature(accessKey, secretKey, "GET", theHost, uri.getPath(), map);
    } catch (Exception e) {
      e.printStackTrace();
    }
    map.put(ApiSignature.op, ApiSignature.opValue);
    map.put("cid", "111");

    String req = JSON.toJSONString(map);
    send(req);
  }

  /**
   * 发送sub请求
   * @param topic
   * @param cid
   */
  public void sendSub(String topic, String cid) {
    JSONObject req = new JSONObject();
    req.put("op", "sub");
    if (cid != null) {
      req.put("cid", cid);
    }
    req.put("topic", topic);
    send(req.toString());
  }


  /**
   * 取消订阅发送unsub请求
   * @param topic
   * @param cid
   */
  public void sendWsUnSubSuccess(String topic, String cid) {


    JSONObject req = new JSONObject();
    req.put("op", "sub");
    req.put("cid", cid);
    req.put("topic", topic);
    send(req.toString());
  }


  /**
   * 发送req请求
   * @param topic
   * @param orderId
   * @param symbol
   * @param states
   * @param cid
   */
  public void sendWsReq(String topic, String orderId, String symbol, String states, String cid) {
    JSONObject req = new JSONObject();
    req.put("op", "req");
    req.put("cid", cid);
    req.put("topic", topic);
    if (orderId != null) {
      req.put("order-id", orderId);
    }
    if (symbol != null) {
      req.put("symbol", symbol);
    }
    if (symbol != null) {
      req.put("states", states);
    }
    send(req.toString());
  }

}
