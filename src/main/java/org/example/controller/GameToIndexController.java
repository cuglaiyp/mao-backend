package org.example.controller;

import org.example.handler.WebSocketHandler;
import org.pyj.http.NettyHttpRequest;
import org.pyj.http.annotation.NettyHttpHandler;
import org.pyj.http.handler.IFunctionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.example.manager.InfoManager.gameInfo;
import static org.example.manager.InfoManager.sceneInfo;

@NettyHttpHandler(path = "/toIndex", method = "GET")
public class GameToIndexController implements IFunctionHandler<Void> {

    private static final Logger log = LoggerFactory.getLogger(GameToIndexController.class);

    @Override
    public Void execute(NettyHttpRequest request) {
        toIndex();
        return null;
    }

    public void toIndex() {
        sceneInfo.toIndex();
        gameInfo.toIndex();
        WebSocketHandler.broadcastToIndexMessage();
        log.info("游戏回退首页成功");
    }
}