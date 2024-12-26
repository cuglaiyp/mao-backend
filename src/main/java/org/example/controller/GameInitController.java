package org.example.controller;


import cn.hutool.core.util.RandomUtil;
import org.example.manager.InfoManager;
import org.pyj.http.NettyHttpRequest;
import org.pyj.http.annotation.NettyHttpHandler;
import org.pyj.http.handler.IFunctionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@NettyHttpHandler(path = "/init", method = "GET", equal = false)
public class GameInitController implements IFunctionHandler<Map<String, Object>> {

    private static final Logger log = LoggerFactory.getLogger(GameInitController.class);

    @Override
    public Map<String, Object> execute(NettyHttpRequest request) {
        String player = null;
        try {
            player = URLDecoder.decode(request.getStringPathValue(2), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return init(player);
    }

    public Map<String, Object> init(String player) {
        Map<String, Object> res = new HashMap<>();
        String leaderboard = InfoManager.gameInfo.getPlayer2Score().entrySet().stream()
                .sorted((entry1, entry2) -> Integer.compare(entry2.getValue(), entry1.getValue())) // 按分数降序排序
                .limit(10)
                .map(entry -> entry.getKey() + ":" + entry.getValue() + "次")
                .collect(Collectors.joining("<br>"));
        int bound = InfoManager.xiWords.size() - 1;
        InfoManager.sceneInfo.getPlayer2Xi().putIfAbsent(player,
                InfoManager.xiWords.get(RandomUtil.randomInt(0, Math.max(bound, 1))));
        Map gameInfo = new HashMap();
        gameInfo.put("player2Score", leaderboard);
        gameInfo.put("playerScore", InfoManager.gameInfo.getPlayer2Score().get(player));
        gameInfo.put("progress", InfoManager.gameInfo.getProgress());
        Map sceneInfo = new HashMap();
        sceneInfo.put("status", InfoManager.sceneInfo.getStatus());
        sceneInfo.put("startTimestamp", InfoManager.sceneInfo.getStartTimestamp());
        sceneInfo.put("onlineCnt", InfoManager.player2Session.size());
        sceneInfo.put("xiCardWord", InfoManager.sceneInfo.getPlayer2Xi().get(player));
        sceneInfo.put("isValid", InfoManager.sceneInfo.getPlayer2Token().containsKey(player));
        res.put("gameInfo", gameInfo);
        res.put("sceneInfo", sceneInfo);
        log.info("{} 游戏初始化成功", player);
        return res;
    }
}