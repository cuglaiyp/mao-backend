package org.example.handler;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.timeout.IdleStateEvent;
import org.example.manager.InfoManager;
import org.pyj.yeauty.annotation.*;
import org.pyj.yeauty.pojo.Session;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ServerPath(path = "/mao")
public class WebSocketHandler {

    private static ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor() {{
        setCorePoolSize(20); // 核心线程数
        setMaxPoolSize(40);  // 最大线程数
        setQueueCapacity(200); // 队列容量
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
        InfoManager.sceneInfo.getPlayer2Xi().putIfAbsent(player,
                InfoManager.xiWords.get(RandomUtil.randomInt(0, InfoManager.xiWords.size() - 1)));
    }

    @OnClose
    public void onClose(Session session) throws IOException {
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        InfoManager.player2Session.remove(session.getAttribute("player"));
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        if (Float.compare(InfoManager.gameInfo.getProgress(), 100) == 0
                && InfoManager.sceneInfo.getStatus() == 1) {
            // 设置状态
            InfoManager.sceneInfo.setStatus(2);
            // 生成喜气卡话术
            generateXiWord();
            // 广播游戏结果
            broadcastSceneMessage();
            // 保存结果
            FileUtil.writeString(JSON.toJSONString(InfoManager.gameInfo), "./gameInfo.json", StandardCharsets.UTF_8);
            FileUtil.writeString(JSON.toJSONString(InfoManager.sceneInfo), "./sceneInfo.json", StandardCharsets.UTF_8);
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
                    System.out.println("read idle");
                    break;
                case WRITER_IDLE:
                    System.out.println("write idle");
                    break;
                case ALL_IDLE:
                    System.out.println("all idle");
                    break;
                default:
                    break;
            }
        }
    }

    public static void broadcastGameMessage() {
        LinkedHashMap<String, Integer> top10Map = InfoManager.gameInfo.getPlayer2Score().entrySet().stream()
                .sorted((entry1, entry2) -> Integer.compare(entry2.getValue(), entry1.getValue())) // 按分数降序排序
                .limit(10)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        Iterator<Map.Entry<String, Session>> iterator = InfoManager.player2Session.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Session> next = iterator.next();
            String player = next.getKey();
            Session session = next.getValue();
            if (!session.isActive()) {
                iterator.remove();
                continue;
            }
            Map msg = new HashMap();
            msg.put("type", 0);
            msg.put("player2Score", top10Map);
            msg.put("playerScore", InfoManager.gameInfo.getPlayer2Score().get(player));
            msg.put("progress", InfoManager.gameInfo.getProgress());
            executor.execute(() -> session.sendText(JSON.toJSONString(msg)));
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
            Map msg = new HashMap();
            msg.put("type", 1);
            msg.put("status", InfoManager.sceneInfo.getStatus());
            msg.put("onlineCnt", InfoManager.player2Session.size());
            msg.put("xiCardWord", InfoManager.sceneInfo.getPlayer2Xi().get(player));
            executor.execute(() -> session.sendText(JSON.toJSONString(msg)));
        }
    }


}