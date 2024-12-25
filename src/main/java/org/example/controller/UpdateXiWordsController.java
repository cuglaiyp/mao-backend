package org.example.controller;

import com.alibaba.fastjson2.JSONArray;
import org.example.manager.InfoManager;
import org.pyj.http.NettyHttpRequest;
import org.pyj.http.annotation.NettyHttpHandler;
import org.pyj.http.handler.IFunctionHandler;

@NettyHttpHandler(path = "/updateXiWords", method = "POST")
public class UpdateXiWordsController implements IFunctionHandler<Void> {

    @Override
    public Void execute(NettyHttpRequest request) {
        return null;
        // 获取请求体，待改框架源码
        //转成Json
        //JSONArray newXiWords = new JSONArray();
        //InfoManager.xiWords.addAll(newXiWords.toJavaList(String.class));
        //return null;
    }
}