package org.example.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.concurrent.ConcurrentHashMap;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SceneInfo {
    // 场景状态：0游戏开始前初始化状态，1游戏开始状态，2游戏结束状态
    private int status = 0;
    private long startTimestamp = 0;
    private int onlineCnt = 0;
    ConcurrentHashMap<String, String> player2Token = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, String> player2Xi = new ConcurrentHashMap<>();
    private int totalPointCnt = 0;

    public void reset() {
        this.status = 0;
        player2Xi.clear();
        this.totalPointCnt = 0;
    }
    public void toIndex() {
        reset();
        player2Token.clear();
    }

}
