package org.example.controller;

import cn.hutool.core.util.IdUtil;
import org.pyj.http.NettyHttpRequest;
import org.pyj.http.annotation.NettyHttpHandler;
import org.pyj.http.handler.IFunctionHandler;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.example.manager.InfoManager.sceneInfo;

@NettyHttpHandler(path = "/judgeName", method = "GET", equal = false)
public class JudgeNameController implements IFunctionHandler<Map> {

    @Override
    public Map execute(NettyHttpRequest request) {
        String player = URLDecoder.decode(request.getStringPathValue(2));
        Map res = new HashMap();
        if (1 > player.length() || player.length() > 4) {
            res.put("msg", "名称长度错误，需大于0小于5！");
            res.put("code", 1);
            return res;
        }
        char fc = player.charAt(0);
        if('0' <= fc && fc <= '9') {
            res.put("msg", "名称格式错误，首位不能为数字！");
            res.put("code", 1);
            return res;
        }
        String uuid = request.headers().get("uuid");
        ConcurrentHashMap<String, String> player2Token = sceneInfo.getPlayer2Token();
        if (player2Token.containsKey(player) && !player2Token.get(player).equals(uuid)) {
            res.put("msg", "名称已存在！");
            res.put("code", 1);
            return res;
        }
        uuid = IdUtil.fastSimpleUUID();
        player2Token.put(player, uuid);
        res.put("code", 0);
        res.put("uuid", uuid);
        return res;
    }


}