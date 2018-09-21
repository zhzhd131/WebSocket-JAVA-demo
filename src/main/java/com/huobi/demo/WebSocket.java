package com.huobi.demo;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.util.StringUtils;

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


  public static String KLINE = "market.%s.kline.%s";
  public static String DEPTH = "market.%s.depth.%s";
  public static String TRADE = "market.%s.trade.detail";
  public static String DETAIL = "market.%s.detail";
  public static String TICKERS = "market.tickers";
  public static String PERIOD[] = {"1min", "5min", "15min", "30min", "60min", "4hour", "1day", "1mon", "1week", "1year"};
  public static String TYPE[] = {"step0", "step1", "step2", "step3", "step4", "step5", "percent10"};


  public WebSocket(URI uri, String accessKey, String secretKey) {
    super(uri, new Draft_17());
    this.uri = uri;
    this.accessKey = accessKey;
    this.secretKey = secretKey;
  }


  @Override
  public void onOpen(ServerHandshake shake) {
    //包含v1为账户订单请求，否则是行情请求
    if (uri.getPath().indexOf("v1") > 0) {
      addAuth();
    } else {
      String op = "sub";
      String topic = String.format(KLINE, "ethusdt", PERIOD[0]);
      sendWsMarket(op, topic);
    }


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
      if (!StringUtils.isEmpty(message)) {
        if (message.indexOf("ping") > 0) {
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
      }
    } catch (CharacterCodingException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  /**
   * 发送账户订单请求鉴权
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
   * 发送账户订单sub请求
   *
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
   * 发送账户订单取消订阅发送unsub请求
   *
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
   * 发送账户订单req请求
   *
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

//  Market KLine    market.$symbol.kline.$period	sub/req	K线 数据，包含单位时间区间的开盘价、收盘价、最高价、最低价、成交量、成交额、成交笔数等数据 $period 可选值：{ 1min, 5min, 15min, 30min, 60min, 4hour,1day, 1mon, 1week, 1year }	N
//  Market Depth	market.$symbol.depth.$type	    sub/req	盘口深度，按照不同 step 聚合的买一、买二、买三等和卖一、卖二、卖三等数据 $type 可选值：{ step0, step1, step2, step3, step4, step5, percent10 } （合并深度0-5）；step0时，不合并深度	N
//  Trade Detail	market.$symbol.trade.detail	    sub/req	成交记录，包含成交价格、成交量、成交方向等信息	N
//  Market Detail	market.$symbol.detail	        sub/req	最近24小时成交量、成交额、开盘价、收盘价、最高价、最低价、成交笔数等	N
//  Market Tickers	market.tickers


  /**
   * 行情请求
   *
   * @param op
   * @param topic
   */
  public void sendWsMarket(String op, String topic) {
    JSONObject req = new JSONObject();
    req.put(op, topic);
    req.put("id", "12312312");
    send(req.toString());
  }

}
