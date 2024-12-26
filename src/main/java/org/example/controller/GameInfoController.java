package org.example.controller;


import org.example.manager.InfoManager;
import org.pyj.http.NettyHttpRequest;
import org.pyj.http.annotation.NettyHttpHandler;
import org.pyj.http.handler.IFunctionHandler;

import java.util.Map;

@NettyHttpHandler(path = "/playerPoint", method = "GET", equal = false)
public class GameInfoController implements IFunctionHandler<Map<String, Integer>> {

    @Override
    public Map<String, Integer> execute(NettyHttpRequest request) {
        return InfoManager.gameInfo.getPlayer2Score();
    }
}