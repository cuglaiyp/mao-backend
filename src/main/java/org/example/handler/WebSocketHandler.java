package org.example.handler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.timeout.IdleStateEvent;
import org.example.manager.InfoManager;
import org.pyj.yeauty.annotation.*;
import org.pyj.yeauty.pojo.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ServerPath(path = "/mao")
public class WebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(WebSocketHandler.class);

    public static ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor() {{
        setCorePoolSize(4); // 核心线程数
        setMaxPoolSize(8);  // 最大线程数
        setQueueCapacity(5000); // 队列容量
        setThreadNamePrefix("broader-"); // 线程名前缀
        initialize(); // 初始化线程池
    }};

    @BeforeHandshake
    public void handshake(Session session, HttpHeaders headers, @RequestParam String player, @RequestParam MultiValueMap reqMap, @PathVariable String arg, @PathVariable Map pathMap) {
        if (StrUtil.isBlank(player)) {
            session.close();
        }
        session.setSubprotocols("stomp");
    }

    @OnOpen
    public void onOpen(Session session, HttpHeaders headers, @RequestParam String player, @RequestParam MultiValueMap reqMap, @PathVariable String arg, @PathVariable Map pathMap) {
        session.setAttribute("player", player);
        InfoManager.player2Session.put(player, session);
        InfoManager.gameInfo.getPlayer2Score().putIfAbsent(player, 0);
        log.info("{} 连接，total:{}", player, InfoManager.player2Session.size());
    }

    @OnClose
    public void onClose(Session session) throws IOException {
        InfoManager.player2Session.remove(session.getAttribute("player"));
        log.warn("{} close，total:{}", session.getAttribute("player"), InfoManager.player2Session.size());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        InfoManager.player2Session.remove(session.getAttribute("player"));
        log.warn("{} error，total:{}", session.getAttribute("player"), InfoManager.player2Session.size());
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        if (message.equalsIgnoreCase("ping")) {
            return;
        }
        if (InfoManager.sceneInfo.getStatus() != 1) {
            return;
        }
        if (Float.compare(InfoManager.gameInfo.getProgress(), 100) == 0) {
            log.info("游戏结束");
            // 设置状态
            InfoManager.sceneInfo.setStatus(2);
            // 生成喜气卡话术
            generateXiWord();
            // 先广播最后的游戏信息
            log.info("广播最后的游戏状态");
            broadcastGameMessage();
            // 广播游戏结果
            log.info("广播游戏停止状态");
            broadcastSceneMessage();
            // 保存结果
            String time = LocalDateTime.now().toString();
            FileUtil.writeString(JSON.toJSONString(InfoManager.gameInfo), "/var/springboot/data/gameInfo-" + time + ".json", StandardCharsets.UTF_8);
            FileUtil.writeString(JSON.toJSONString(InfoManager.sceneInfo), "/var/springboot/data/sceneInfo" + time + ".json", StandardCharsets.UTF_8);
//            FileUtil.writeString(JSON.toJSONString(InfoManager.gameInfo), "gameInfo.json", StandardCharsets.UTF_8);
//            FileUtil.writeString(JSON.toJSONString(InfoManager.sceneInfo), "sceneInfo.json", StandardCharsets.UTF_8);
            return;
        }
        String player = message;
        InfoManager.sceneInfo.setTotalPointCnt(InfoManager.sceneInfo.getTotalPointCnt() + 1);
        ConcurrentHashMap<String, Integer> player2Score = InfoManager.gameInfo.getPlayer2Score();
        player2Score.put(player, player2Score.getOrDefault(player, 0) + 1);
        InfoManager.gameInfo.setProgress(InfoManager.getProgress());
    }

    private void generateXiWord() {
        Collections.shuffle(InfoManager.xiWords);
        Iterator<Map.Entry<String, Session>> iterator = InfoManager.player2Session.entrySet().iterator();
        for (int i = 0; iterator.hasNext(); i = (i + 1) % InfoManager.xiWords.size()) {
            Map.Entry<String, Session> next = iterator.next();
            InfoManager.sceneInfo.getPlayer2Xi().putIfAbsent(next.getKey(), InfoManager.xiWords.get(i));
        }
    }

    @OnBinary
    public void onBinary(Session session, byte[] bytes) {
    }

    @OnEvent
    public void onEvent(Session session, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            switch (idleStateEvent.state()) {
                case READER_IDLE:
                    log.warn("read idle: {}", session.getAttribute("player").toString());
                    break;
                case WRITER_IDLE:
                    log.warn("write idle: {}", session.getAttribute("player").toString());
                    break;
                case ALL_IDLE:
                    log.warn("all idle: {}", session.getAttribute("player").toString());
                    session.close();
                    break;
                default:
                    break;
            }
        }
    }

    public static String generate10LeaderBoardStr() {
        return InfoManager.gameInfo.getPlayer2Score().entrySet().stream()
                .sorted((entry1, entry2) -> Integer.compare(entry2.getValue(), entry1.getValue())) // 按分数降序排序
                .limit(10)
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(";"));
    }

    public static void broadcastGameMessage() {
        String leaderboard = generate10LeaderBoardStr();
        Iterator<Map.Entry<String, Session>> iterator = InfoManager.player2Session.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Session> next = iterator.next();
            String player = next.getKey();
            Session session = next.getValue();
            if (!session.isActive()) {
                iterator.remove();
                continue;
            }

            String msg = "{" +
                    "\"type\":" + 0 +
                    ",\"player2Score\":" + "\"" + leaderboard + "\"" +
                    ",\"playerScore\":" + InfoManager.gameInfo.getPlayer2Score().get(player) +
                    ",\"progress\":" + InfoManager.gameInfo.getProgress() +
                    "}";
            executor.execute(() -> session.sendText(msg));
        }
    }

    public static void broadcastSceneMessage() {
        Iterator<Map.Entry<String, Session>> iterator = InfoManager.player2Session.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Session> next = iterator.next();
            String player = next.getKey();
            Session session = next.getValue();
            if (!session.isActive()) {
                iterator.remove();
                continue;
            }
            String msg = "{" +
                    "\"type\":" + 1 +
                    ",\"status\":" + InfoManager.sceneInfo.getStatus() +
                    ",\"xiCardWord\":" + "\"" + InfoManager.sceneInfo.getPlayer2Xi().get(player) + "\"" +
                    ",\"isValid\":" + InfoManager.sceneInfo.getPlayer2Token().containsKey(player) +
                    "}";
            executor.execute(() -> session.sendText(msg));
        }
    }

    public static void broadcastOnlineMessage() {
        Iterator<Map.Entry<String, Session>> iterator = InfoManager.player2Session.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Session> next = iterator.next();
            String player = next.getKey();
            Session session = next.getValue();
            if (!session.isActive()) {
                iterator.remove();
                continue;
            }
            String msg = "{" +
                    "\"type\":" + 2 +
                    ",\"onlineCnt\":" + InfoManager.player2Session.size() +
                    "}";
            executor.execute(() -> session.sendText(msg));
        }
    }

    public static void broadcastToIndexMessage() {
        Iterator<Map.Entry<String, Session>> iterator = InfoManager.player2Session.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Session> next = iterator.next();
            String player = next.getKey();
            Session session = next.getValue();
            if (!session.isActive()) {
                iterator.remove();
                continue;
            }
            String msg = "{" +
                    "\"type\":" + 1 +
                    ",\"status\":" + InfoManager.sceneInfo.getStatus() +
                    ",\"isValid\":" + InfoManager.sceneInfo.getPlayer2Token().contains(player) +
                    "}";
            executor.execute(() -> {
                session.sendText(msg);
                session.close();
            });
        }
    }


}