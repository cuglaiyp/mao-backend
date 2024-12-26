package org.example.manager;

import org.example.pojo.GameInfo;
import org.example.pojo.SceneInfo;
import org.pyj.yeauty.pojo.Session;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class InfoManager {

    public final static GameInfo gameInfo = new GameInfo();
    public final static SceneInfo sceneInfo = new SceneInfo();
    public final static ConcurrentHashMap<String, Session> player2Session = new ConcurrentHashMap<>();
    public final static List<String> xiWords = new LinkedList<>();

    static {
        xiWords.add("鸿运当头");
        xiWords.add("事事顺心");
        xiWords.add("好运连连<br>笑口常开");
        xiWords.add("平安喜乐");
        xiWords.add("吃嘛嘛香<br>身体倍棒");
        xiWords.add("财源滚滚<br>福星高照");
        xiWords.add("喜上眉头<br>甜上心头");
        xiWords.add("阖家欢乐");
        xiWords.add("心想事成<br>步步高升");
        xiWords.add("顺遂无虞<br>皆得所愿");
        xiWords.add("年长乐<br>岁无忧");
        xiWords.add("财运亨通");
    }

    public static float getProgress() {
        if (sceneInfo.getTotalPointCnt() == 0 || gameInfo.getPlayer2Score().isEmpty()) {
            return 0;
        }
        float progress = (float) sceneInfo.getTotalPointCnt() / gameInfo.getPlayer2Score().size();
        return progress < 100 ? progress : 100;
    }
}
